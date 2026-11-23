#![cfg_attr(not(target_os = "android"), allow(dead_code))]

use std::{
    collections::{HashMap, VecDeque},
    fs,
    io::{self, Write},
    os::unix::fs::PermissionsExt,
    path::Path,
};

use anyhow::{Context, Result, ensure};
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value, json};

pub const API_VERSION: u32 = 1;
pub const MAX_IDEMPOTENCY_ENTRIES: usize = 256;
pub const MAX_REQUEST_BYTES: usize = 1024 * 1024;
pub const ANDROID_USER_UID_STRIDE: u32 = 100_000;

pub const ERR_UNAUTHORIZED: &str = "UNAUTHORIZED";
pub const ERR_UNSUPPORTED_VERSION: &str = "UNSUPPORTED_VERSION";
pub const ERR_STALE_GENERATION: &str = "STALE_GENERATION";
pub const ERR_INVALID_PARAMS: &str = "INVALID_PARAMS";
pub const ERR_NOT_FOUND: &str = "NOT_FOUND";
pub const ERR_PERMISSION_DENIED: &str = "PERMISSION_DENIED";
pub const ERR_INTERNAL_ERROR: &str = "INTERNAL_ERROR";

pub const CODE_UNAUTHORIZED: i32 = -32001;
pub const CODE_UNSUPPORTED_VERSION: i32 = -32002;
pub const CODE_STALE_GENERATION: i32 = -32003;
pub const CODE_NOT_FOUND: i32 = -32004;
pub const CODE_PERMISSION_DENIED: i32 = -32005;
pub const CODE_INVALID_PARAMS: i32 = -32602;
pub const CODE_INTERNAL_ERROR: i32 = -32603;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct CompanionEntry {
    pub package: String,
    pub cert_sha256: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub uid: Option<u32>,
    #[serde(default = "default_true")]
    pub enabled: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
}

const fn default_true() -> bool {
    true
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct CompanionAllowlist {
    #[serde(default = "default_allowlist_version")]
    pub version: u32,
    #[serde(default)]
    pub companions: Vec<CompanionEntry>,
}

const fn default_allowlist_version() -> u32 {
    1
}

impl Default for CompanionAllowlist {
    fn default() -> Self {
        Self {
            version: 1,
            companions: Vec::new(),
        }
    }
}

pub fn normalize_cert_sha256(raw: &str) -> Result<String> {
    let cleaned: String = raw
        .chars()
        .filter(|c| *c != ':' && !c.is_whitespace())
        .collect::<String>()
        .to_ascii_lowercase();
    ensure!(
        cleaned.len() == 64 && cleaned.bytes().all(|b| b.is_ascii_hexdigit()),
        "certSha256 must be 64 hex characters"
    );
    Ok(cleaned)
}

pub fn load_allowlist(path: &Path) -> Result<CompanionAllowlist> {
    match fs::read_to_string(path) {
        Ok(content) => {
            if content.trim().is_empty() {
                return Ok(CompanionAllowlist::default());
            }
            let allowlist: CompanionAllowlist =
                serde_json::from_str(&content).context("invalid companion allowlist JSON")?;
            Ok(allowlist)
        }
        Err(e) if e.kind() == io::ErrorKind::NotFound => Ok(CompanionAllowlist::default()),
        Err(e) => Err(e).with_context(|| format!("failed to read {}", path.display())),
    }
}

pub fn save_allowlist(path: &Path, allowlist: &CompanionAllowlist) -> Result<()> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .with_context(|| format!("failed to create {}", parent.display()))?;
    }
    let json = serde_json::to_string_pretty(allowlist)?;
    let tmp_path = path.with_extension("tmp");
    {
        let mut f = fs::File::create(&tmp_path)
            .with_context(|| format!("failed to create {}", tmp_path.display()))?;
        f.set_permissions(fs::Permissions::from_mode(0o600))?;
        f.write_all(json.as_bytes())?;
        f.sync_all()?;
    }
    fs::rename(&tmp_path, path).with_context(|| {
        format!(
            "failed to rename {} to {}",
            tmp_path.display(),
            path.display()
        )
    })?;
    fs::set_permissions(path, fs::Permissions::from_mode(0o600))?;
    Ok(())
}

pub fn upsert_companion(
    path: &Path,
    package: &str,
    cert_sha256: &str,
    uid: Option<u32>,
    enabled: bool,
    label: Option<String>,
) -> Result<CompanionEntry> {
    let package = package.trim();
    ensure!(!package.is_empty(), "package must not be empty");
    let cert_sha256 = normalize_cert_sha256(cert_sha256)?;

    let mut allowlist = load_allowlist(path)?;
    let entry = CompanionEntry {
        package: package.to_string(),
        cert_sha256: cert_sha256.clone(),
        uid,
        enabled,
        label,
    };

    if let Some(existing) = allowlist
        .companions
        .iter_mut()
        .find(|c| c.package == package && c.cert_sha256.eq_ignore_ascii_case(&cert_sha256))
    {
        *existing = entry.clone();
    } else {
        allowlist.companions.push(entry.clone());
    }

    save_allowlist(path, &allowlist)?;
    Ok(entry)
}

pub fn remove_companion(path: &Path, package: &str, cert_sha256: Option<&str>) -> Result<usize> {
    let package = package.trim();
    ensure!(!package.is_empty(), "package must not be empty");
    let normalized_cert = match cert_sha256 {
        Some(c) => Some(normalize_cert_sha256(c)?),
        None => None,
    };

    let mut allowlist = load_allowlist(path)?;
    let before = allowlist.companions.len();
    allowlist.companions.retain(|c| {
        if c.package != package {
            return true;
        }
        match &normalized_cert {
            Some(cert) => !c.cert_sha256.eq_ignore_ascii_case(cert),
            None => false,
        }
    });
    let removed = before.saturating_sub(allowlist.companions.len());
    if removed > 0 {
        save_allowlist(path, &allowlist)?;
    }
    Ok(removed)
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PeerIdentity {
    pub uid: u32,
    pub pid: i32,
    pub gid: u32,
    pub package: Option<String>,
    pub cert_sha256: Option<String>,
}

pub fn authorize_peer(
    peer: &PeerIdentity,
    manager_app_ids: &[u32],
    allowlist: &CompanionAllowlist,
) -> bool {
    if peer.uid == 0 {
        return true;
    }

    let app_id = peer.uid % ANDROID_USER_UID_STRIDE;
    if manager_app_ids.contains(&app_id) {
        return true;
    }

    let Some(peer_cert) = peer.cert_sha256.as_deref() else {
        return false;
    };
    if peer_cert.len() != 64 || !peer_cert.bytes().all(|b| b.is_ascii_hexdigit()) {
        return false;
    }

    allowlist.companions.iter().any(|entry| {
        if !entry.enabled || entry.cert_sha256.len() != 64 {
            return false;
        }
        if !entry.cert_sha256.eq_ignore_ascii_case(peer_cert) {
            return false;
        }
        if let Some(expected_uid) = entry.uid {
            let uid_matches = if expected_uid < ANDROID_USER_UID_STRIDE {
                app_id == expected_uid
            } else {
                peer.uid == expected_uid
            };
            if !uid_matches {
                return false;
            }
        }
        if !entry.package.is_empty()
            && let Some(peer_pkg) = peer.package.as_deref()
            && peer_pkg != entry.package
        {
            return false;
        }
        true
    })
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IpcRequest {
    #[serde(default)]
    pub id: Option<String>,
    pub method: String,
    #[serde(default)]
    pub api_version: Option<u32>,
    #[serde(default = "default_empty_object")]
    pub params: Value,
}

fn default_empty_object() -> Value {
    Value::Object(Map::new())
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IpcError {
    pub code: String,
    pub numeric_code: i32,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IpcResponse {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    pub ok: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<IpcError>,
}

impl IpcResponse {
    pub fn success(id: Option<String>, result: Value) -> Self {
        Self {
            id,
            ok: true,
            result: Some(result),
            error: None,
        }
    }

    pub fn err(
        id: Option<String>,
        code: &str,
        numeric_code: i32,
        message: impl Into<String>,
    ) -> Self {
        Self {
            id,
            ok: false,
            result: None,
            error: Some(IpcError {
                code: code.to_string(),
                numeric_code,
                message: message.into(),
            }),
        }
    }

    pub fn unauthorized(id: Option<String>, message: impl Into<String>) -> Self {
        Self::err(id, ERR_UNAUTHORIZED, CODE_UNAUTHORIZED, message)
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FieldOutcome {
    pub field: String,
    pub written: bool,
    pub verified: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error_code: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ApplyConfigurationResult {
    pub request_id: String,
    pub applied_generation: u64,
    pub success: bool,
    pub outcomes: Vec<FieldOutcome>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error_code: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FeatureInfo {
    pub id: u32,
    pub name: String,
    pub description: String,
    pub value: u64,
    pub supported: bool,
    pub forced: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MountEntry {
    pub path: String,
    pub flags: u32,
}

#[derive(Debug, Clone)]
pub struct BackendStatus {
    pub kernel_version: i32,
    pub uapi_matched: bool,
    pub safe_mode: bool,
    pub is_lkm: bool,
    pub is_late_load: bool,
    pub process_hooked: bool,
}

#[derive(Debug)]
pub enum BackendError {
    NotFound(String),
    PermissionDenied(String),
    InvalidParams(String),
    Internal(String),
}

impl BackendError {
    fn into_response(self, id: Option<String>) -> IpcResponse {
        match self {
            Self::NotFound(m) => IpcResponse::err(id, ERR_NOT_FOUND, CODE_NOT_FOUND, m),
            Self::PermissionDenied(m) => {
                IpcResponse::err(id, ERR_PERMISSION_DENIED, CODE_PERMISSION_DENIED, m)
            }
            Self::InvalidParams(m) => {
                IpcResponse::err(id, ERR_INVALID_PARAMS, CODE_INVALID_PARAMS, m)
            }
            Self::Internal(m) => IpcResponse::err(id, ERR_INTERNAL_ERROR, CODE_INTERNAL_ERROR, m),
        }
    }
}

pub trait KsuBackend {
    fn helper_version(&self) -> String;
    fn supported_abis(&self) -> Vec<String>;
    fn probe_status(&self, fresh_probe: bool) -> BackendStatus;

    fn feature_get(&self, id: &str) -> std::result::Result<FeatureInfo, BackendError>;
    fn feature_set(&self, id: &str, value: u64) -> std::result::Result<(), BackendError>;
    fn feature_list(&self) -> std::result::Result<Vec<FeatureInfo>, BackendError>;

    fn module_list(&self) -> std::result::Result<Vec<HashMap<String, String>>, BackendError>;
    fn module_enable(&self, id: &str) -> std::result::Result<(), BackendError>;
    fn module_disable(&self, id: &str) -> std::result::Result<(), BackendError>;
    fn module_uninstall(&self, id: &str) -> std::result::Result<(), BackendError>;
    fn module_install(&self, zip_path: &str) -> std::result::Result<(), BackendError>;
    fn module_action(&self, id: &str) -> std::result::Result<(), BackendError>;

    fn profile_get(&self, kind: &str, id: &str) -> std::result::Result<String, BackendError>;
    fn profile_set(
        &self,
        kind: &str,
        id: &str,
        content: &str,
    ) -> std::result::Result<(), BackendError>;
    fn profile_list_templates(
        &self,
        locale: Option<&str>,
    ) -> std::result::Result<Vec<String>, BackendError>;
    fn profile_delete_template(&self, id: &str) -> std::result::Result<(), BackendError>;

    fn allowlist_get(&self, allow: bool) -> std::result::Result<Vec<u32>, BackendError>;

    fn umount_list(&self) -> std::result::Result<Vec<MountEntry>, BackendError>;
    fn umount_add(&self, path: &str, flags: u32) -> std::result::Result<(), BackendError>;
    fn umount_del(&self, path: &str) -> std::result::Result<(), BackendError>;

    fn resetprop_get(&self, name: &str) -> std::result::Result<Option<String>, BackendError>;
    fn resetprop_set(&self, name: &str, value: &str) -> std::result::Result<(), BackendError>;
}

pub struct ContractState {
    pub applied_generation: u64,
    pub last_error_code: Option<String>,
    pub idempotency_cache: HashMap<String, ApplyConfigurationResult>,
    pub idempotency_order: VecDeque<String>,
}

impl Default for ContractState {
    fn default() -> Self {
        Self {
            applied_generation: 0,
            last_error_code: None,
            idempotency_cache: HashMap::new(),
            idempotency_order: VecDeque::new(),
        }
    }
}

impl ContractState {
    fn cache_result(&mut self, request_id: String, result: ApplyConfigurationResult) {
        if !self.idempotency_cache.contains_key(&request_id) {
            if self.idempotency_order.len() >= MAX_IDEMPOTENCY_ENTRIES
                && let Some(oldest) = self.idempotency_order.pop_front()
            {
                self.idempotency_cache.remove(&oldest);
            }
            self.idempotency_order.push_back(request_id.clone());
        }
        self.idempotency_cache.insert(request_id, result);
    }
}

pub fn supported_operations() -> Vec<&'static str> {
    vec![
        "getCapabilities",
        "getStatus",
        "applyConfiguration",
        "diagnostics",
        "feature.get",
        "feature.set",
        "feature.list",
        "module.list",
        "module.enable",
        "module.disable",
        "module.uninstall",
        "module.install",
        "module.action",
        "profile.get",
        "profile.set",
        "profile.listTemplates",
        "profile.deleteTemplate",
        "allowlist.get",
        "umount.list",
        "umount.add",
        "umount.del",
        "resetprop.get",
        "resetprop.set",
    ]
}

fn check_api_version(req: &IpcRequest) -> std::result::Result<(), IpcResponse> {
    if let Some(v) = req.api_version
        && v != API_VERSION
    {
        return Err(IpcResponse::err(
            req.id.clone(),
            ERR_UNSUPPORTED_VERSION,
            CODE_UNSUPPORTED_VERSION,
            format!("unsupported apiVersion {v}; expected {API_VERSION}"),
        ));
    }
    Ok(())
}

fn require_str<'a>(params: &'a Value, key: &str) -> std::result::Result<&'a str, BackendError> {
    params
        .get(key)
        .and_then(Value::as_str)
        .filter(|s| !s.is_empty())
        .ok_or_else(|| BackendError::InvalidParams(format!("missing or empty '{key}'")))
}

pub fn dispatch<B: KsuBackend>(
    backend: &B,
    state: &mut ContractState,
    req: IpcRequest,
) -> IpcResponse {
    if let Err(resp) = check_api_version(&req) {
        return resp;
    }

    let id = req.id.clone();
    match req.method.as_str() {
        "getCapabilities" => {
            if req.api_version != Some(API_VERSION) && req.api_version.is_some() {
                return IpcResponse::err(
                    id,
                    ERR_UNSUPPORTED_VERSION,
                    CODE_UNSUPPORTED_VERSION,
                    "unsupported apiVersion",
                );
            }
            IpcResponse::success(
                id,
                json!({
                    "apiVersion": API_VERSION,
                    "helperVersion": backend.helper_version(),
                    "supportedOperations": supported_operations(),
                    "supportedAbi": backend.supported_abis(),
                }),
            )
        }
        "getStatus" => {
            let fresh_probe = req
                .params
                .get("probe")
                .and_then(Value::as_bool)
                .unwrap_or(false);
            let st = backend.probe_status(fresh_probe);
            let module_loaded = st.kernel_version > 0;
            let availability = if !module_loaded {
                "UNAVAILABLE"
            } else if st.safe_mode {
                "DISABLED"
            } else if !st.uapi_matched {
                "ERROR"
            } else {
                "READY"
            };
            let error_code = if !module_loaded {
                Some("KERNEL_UNAVAILABLE")
            } else if !st.uapi_matched {
                Some("UAPI_MISMATCH")
            } else {
                state.last_error_code.as_deref()
            };
            IpcResponse::success(
                id,
                json!({
                    "availability": availability,
                    "moduleLoaded": module_loaded,
                    "processHooked": st.process_hooked,
                    "appliedGeneration": state.applied_generation,
                    "errorCode": error_code,
                    "freshProbe": fresh_probe,
                }),
            )
        }
        "applyConfiguration" => handle_apply_configuration(backend, state, id, &req.params),
        "diagnostics" => {
            let fresh_probe = req
                .params
                .get("freshProbe")
                .and_then(Value::as_bool)
                .unwrap_or(false);
            let st = backend.probe_status(fresh_probe);
            let module_loaded = st.kernel_version > 0;
            let availability = if !module_loaded {
                "UNAVAILABLE"
            } else if st.safe_mode {
                "DISABLED"
            } else if !st.uapi_matched {
                "ERROR"
            } else {
                "READY"
            };
            IpcResponse::success(
                id,
                json!({
                    "apiVersion": API_VERSION,
                    "helperVersion": backend.helper_version(),
                    "availability": availability,
                    "moduleLoaded": module_loaded,
                    "processHooked": st.process_hooked,
                    "appliedGeneration": state.applied_generation,
                    "freshProbe": fresh_probe,
                    "capabilities": supported_operations(),
                    "flags": {
                        "safeMode": st.safe_mode,
                        "uapiMatched": st.uapi_matched,
                        "isLkm": st.is_lkm,
                        "isLateLoad": st.is_late_load,
                    },
                    "errorCode": state.last_error_code,
                }),
            )
        }
        "feature.get" => match require_str(&req.params, "id") {
            Ok(fid) => match backend.feature_get(fid) {
                Ok(info) => {
                    IpcResponse::success(id, serde_json::to_value(info).unwrap_or(Value::Null))
                }
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "feature.set" => {
            let fid = match require_str(&req.params, "id") {
                Ok(v) => v,
                Err(e) => return e.into_response(id),
            };
            let Some(val) = req.params.get("value").and_then(Value::as_u64) else {
                return BackendError::InvalidParams("missing numeric 'value'".into())
                    .into_response(id);
            };
            match backend.feature_set(fid, val) {
                Ok(()) => IpcResponse::success(id, json!({ "id": fid, "value": val })),
                Err(e) => e.into_response(id),
            }
        }
        "feature.list" => match backend.feature_list() {
            Ok(features) => IpcResponse::success(id, json!({ "features": features })),
            Err(e) => e.into_response(id),
        },
        "module.list" => match backend.module_list() {
            Ok(modules) => IpcResponse::success(id, json!({ "modules": modules })),
            Err(e) => e.into_response(id),
        },
        "module.enable" => match require_str(&req.params, "id") {
            Ok(mid) => match backend.module_enable(mid) {
                Ok(()) => IpcResponse::success(id, json!({ "id": mid, "enabled": true })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "module.disable" => match require_str(&req.params, "id") {
            Ok(mid) => match backend.module_disable(mid) {
                Ok(()) => IpcResponse::success(id, json!({ "id": mid, "enabled": false })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "module.uninstall" => match require_str(&req.params, "id") {
            Ok(mid) => match backend.module_uninstall(mid) {
                Ok(()) => IpcResponse::success(id, json!({ "id": mid, "uninstalled": true })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "module.install" => match require_str(&req.params, "zipPath") {
            Ok(zip) => match backend.module_install(zip) {
                Ok(()) => IpcResponse::success(id, json!({ "zipPath": zip, "installed": true })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "module.action" => match require_str(&req.params, "id") {
            Ok(mid) => match backend.module_action(mid) {
                Ok(()) => IpcResponse::success(id, json!({ "id": mid, "executed": true })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "profile.get" => {
            let kind = req
                .params
                .get("kind")
                .and_then(Value::as_str)
                .unwrap_or("sepolicy");
            let pid = match require_str(&req.params, "id") {
                Ok(v) => v,
                Err(e) => return e.into_response(id),
            };
            match backend.profile_get(kind, pid) {
                Ok(content) => {
                    IpcResponse::success(id, json!({ "kind": kind, "id": pid, "content": content }))
                }
                Err(e) => e.into_response(id),
            }
        }
        "profile.set" => {
            let kind = req
                .params
                .get("kind")
                .and_then(Value::as_str)
                .unwrap_or("sepolicy");
            let pid = match require_str(&req.params, "id") {
                Ok(v) => v,
                Err(e) => return e.into_response(id),
            };
            let Some(content) = req.params.get("content").and_then(Value::as_str) else {
                return BackendError::InvalidParams("missing 'content'".into()).into_response(id);
            };
            match backend.profile_set(kind, pid, content) {
                Ok(()) => IpcResponse::success(id, json!({ "kind": kind, "id": pid })),
                Err(e) => e.into_response(id),
            }
        }
        "profile.listTemplates" => {
            let locale = req.params.get("locale").and_then(Value::as_str);
            match backend.profile_list_templates(locale) {
                Ok(templates) => IpcResponse::success(id, json!({ "templates": templates })),
                Err(e) => e.into_response(id),
            }
        }
        "profile.deleteTemplate" => match require_str(&req.params, "id") {
            Ok(tid) => match backend.profile_delete_template(tid) {
                Ok(()) => IpcResponse::success(id, json!({ "id": tid, "deleted": true })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "allowlist.get" => {
            let allow = req
                .params
                .get("allow")
                .and_then(Value::as_bool)
                .unwrap_or(true);
            match backend.allowlist_get(allow) {
                Ok(uids) => IpcResponse::success(id, json!({ "uids": uids, "allow": allow })),
                Err(e) => e.into_response(id),
            }
        }
        "umount.list" => match backend.umount_list() {
            Ok(entries) => IpcResponse::success(id, json!({ "entries": entries })),
            Err(e) => e.into_response(id),
        },
        "umount.add" => {
            let path = match require_str(&req.params, "path") {
                Ok(v) => v,
                Err(e) => return e.into_response(id),
            };
            let flags = req
                .params
                .get("flags")
                .and_then(Value::as_u64)
                .and_then(|v| u32::try_from(v).ok())
                .unwrap_or(0);
            match backend.umount_add(path, flags) {
                Ok(()) => IpcResponse::success(id, json!({ "path": path, "flags": flags })),
                Err(e) => e.into_response(id),
            }
        }
        "umount.del" => match require_str(&req.params, "path") {
            Ok(path) => match backend.umount_del(path) {
                Ok(()) => IpcResponse::success(id, json!({ "path": path })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "resetprop.get" => match require_str(&req.params, "name") {
            Ok(name) => match backend.resetprop_get(name) {
                Ok(value) => IpcResponse::success(id, json!({ "name": name, "value": value })),
                Err(e) => e.into_response(id),
            },
            Err(e) => e.into_response(id),
        },
        "resetprop.set" => {
            let name = match require_str(&req.params, "name") {
                Ok(v) => v,
                Err(e) => return e.into_response(id),
            };
            let Some(value) = req.params.get("value").and_then(Value::as_str) else {
                return BackendError::InvalidParams("missing 'value'".into()).into_response(id);
            };
            match backend.resetprop_set(name, value) {
                Ok(()) => IpcResponse::success(id, json!({ "name": name, "value": value })),
                Err(e) => e.into_response(id),
            }
        }
        other => IpcResponse::err(
            id,
            ERR_NOT_FOUND,
            CODE_NOT_FOUND,
            format!("unknown method '{other}'"),
        ),
    }
}

fn handle_apply_configuration<B: KsuBackend>(
    backend: &B,
    state: &mut ContractState,
    id: Option<String>,
    params: &Value,
) -> IpcResponse {
    let request_id = match require_str(params, "requestId") {
        Ok(v) => v.to_string(),
        Err(e) => return e.into_response(id),
    };
    if let Some(cached) = state.idempotency_cache.get(&request_id) {
        return IpcResponse::success(id, serde_json::to_value(cached).unwrap_or(Value::Null));
    }

    let Some(expected_generation) = params.get("expectedGeneration").and_then(Value::as_u64) else {
        return BackendError::InvalidParams("missing 'expectedGeneration'".into())
            .into_response(id);
    };
    let Some(desired_generation) = params.get("desiredGeneration").and_then(Value::as_u64) else {
        return BackendError::InvalidParams("missing 'desiredGeneration'".into()).into_response(id);
    };

    if expected_generation != state.applied_generation {
        return IpcResponse::err(
            id,
            ERR_STALE_GENERATION,
            CODE_STALE_GENERATION,
            format!(
                "stale expectedGeneration {expected_generation}; current is {}",
                state.applied_generation
            ),
        );
    }
    if desired_generation <= expected_generation {
        return BackendError::InvalidParams(
            "desiredGeneration must be greater than expectedGeneration".into(),
        )
        .into_response(id);
    }

    let Some(config) = params.get("configuration").and_then(Value::as_object) else {
        return BackendError::InvalidParams("missing object 'configuration'".into())
            .into_response(id);
    };

    let mut outcomes = Vec::new();
    let mut all_ok = true;

    if let Some(features) = config.get("features").and_then(Value::as_object) {
        for (fid, raw_val) in features {
            let field_name = format!("features.{fid}");
            let Some(val) = raw_val.as_u64() else {
                all_ok = false;
                outcomes.push(FieldOutcome {
                    field: field_name,
                    written: false,
                    verified: false,
                    error_code: Some(ERR_INVALID_PARAMS.to_string()),
                });
                continue;
            };
            match backend.feature_set(fid, val) {
                Ok(()) => {
                    let verified = backend
                        .feature_get(fid)
                        .map(|f| f.value == val)
                        .unwrap_or(false);
                    if !verified {
                        all_ok = false;
                    }
                    outcomes.push(FieldOutcome {
                        field: field_name,
                        written: true,
                        verified,
                        error_code: (!verified).then(|| "VERIFY_FAILED".to_string()),
                    });
                }
                Err(_) => {
                    all_ok = false;
                    outcomes.push(FieldOutcome {
                        field: field_name,
                        written: false,
                        verified: false,
                        error_code: Some(ERR_INTERNAL_ERROR.to_string()),
                    });
                }
            }
        }
    }

    if let Some(props) = config.get("properties").and_then(Value::as_object) {
        for (prop_name, raw_val) in props {
            let field_name = format!("properties.{prop_name}");
            let Some(val) = raw_val.as_str() else {
                all_ok = false;
                outcomes.push(FieldOutcome {
                    field: field_name,
                    written: false,
                    verified: false,
                    error_code: Some(ERR_INVALID_PARAMS.to_string()),
                });
                continue;
            };
            match backend.resetprop_set(prop_name, val) {
                Ok(()) => {
                    let verified =
                        backend.resetprop_get(prop_name).ok().flatten().as_deref() == Some(val);
                    if !verified {
                        all_ok = false;
                    }
                    outcomes.push(FieldOutcome {
                        field: field_name,
                        written: true,
                        verified,
                        error_code: (!verified).then(|| "VERIFY_FAILED".to_string()),
                    });
                }
                Err(_) => {
                    all_ok = false;
                    outcomes.push(FieldOutcome {
                        field: field_name,
                        written: false,
                        verified: false,
                        error_code: Some(ERR_INTERNAL_ERROR.to_string()),
                    });
                }
            }
        }
    }

    if all_ok {
        state.applied_generation = desired_generation;
        state.last_error_code = None;
    } else {
        state.last_error_code = Some("PARTIAL_APPLY_FAILED".to_string());
    }

    let result = ApplyConfigurationResult {
        request_id: request_id.clone(),
        applied_generation: state.applied_generation,
        success: all_ok,
        outcomes,
        error_code: (!all_ok).then(|| "PARTIAL_APPLY_FAILED".to_string()),
    };

    // Only cache completed/successful applies for idempotency; failed applies stay retryable!
    if all_ok {
        state.cache_result(request_id, result.clone());
    }

    IpcResponse::success(id, serde_json::to_value(result).unwrap_or(Value::Null))
}

#[cfg(test)]
mod tests {
    use std::cell::RefCell;

    use tempfile::TempDir;

    use super::*;

    struct MockBackend {
        features: RefCell<HashMap<String, u64>>,
        props: RefCell<HashMap<String, String>>,
        fail_prop: Option<String>,
    }

    impl MockBackend {
        fn new() -> Self {
            let mut features = HashMap::new();
            features.insert("su_compat".to_string(), 1);
            Self {
                features: RefCell::new(features),
                props: RefCell::new(HashMap::new()),
                fail_prop: None,
            }
        }
    }

    impl KsuBackend for MockBackend {
        fn helper_version(&self) -> String {
            "1.0.0-test".to_string()
        }
        fn supported_abis(&self) -> Vec<String> {
            vec!["arm64-v8a".to_string()]
        }
        fn probe_status(&self, _fresh_probe: bool) -> BackendStatus {
            BackendStatus {
                kernel_version: 12000,
                uapi_matched: true,
                safe_mode: false,
                is_lkm: true,
                is_late_load: false,
                process_hooked: true,
            }
        }
        fn feature_get(&self, id: &str) -> std::result::Result<FeatureInfo, BackendError> {
            let val = self
                .features
                .borrow()
                .get(id)
                .copied()
                .ok_or_else(|| BackendError::NotFound(id.to_string()))?;
            Ok(FeatureInfo {
                id: 0,
                name: id.to_string(),
                description: "mock".to_string(),
                value: val,
                supported: true,
                forced: false,
            })
        }
        fn feature_set(&self, id: &str, value: u64) -> std::result::Result<(), BackendError> {
            self.features.borrow_mut().insert(id.to_string(), value);
            Ok(())
        }
        fn feature_list(&self) -> std::result::Result<Vec<FeatureInfo>, BackendError> {
            Ok(vec![])
        }
        fn module_list(&self) -> std::result::Result<Vec<HashMap<String, String>>, BackendError> {
            Ok(vec![])
        }
        fn module_enable(&self, _id: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn module_disable(&self, _id: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn module_uninstall(&self, _id: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn module_install(&self, _zip_path: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn module_action(&self, _id: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn profile_get(&self, _kind: &str, _id: &str) -> std::result::Result<String, BackendError> {
            Ok(String::new())
        }
        fn profile_set(
            &self,
            _kind: &str,
            _id: &str,
            _content: &str,
        ) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn profile_list_templates(
            &self,
            _locale: Option<&str>,
        ) -> std::result::Result<Vec<String>, BackendError> {
            Ok(vec![])
        }
        fn profile_delete_template(&self, _id: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn allowlist_get(&self, _allow: bool) -> std::result::Result<Vec<u32>, BackendError> {
            Ok(vec![10234])
        }
        fn umount_list(&self) -> std::result::Result<Vec<MountEntry>, BackendError> {
            Ok(vec![])
        }
        fn umount_add(&self, _path: &str, _flags: u32) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn umount_del(&self, _path: &str) -> std::result::Result<(), BackendError> {
            Ok(())
        }
        fn resetprop_get(&self, name: &str) -> std::result::Result<Option<String>, BackendError> {
            Ok(self.props.borrow().get(name).cloned())
        }
        fn resetprop_set(&self, name: &str, value: &str) -> std::result::Result<(), BackendError> {
            if self.fail_prop.as_deref() == Some(name) {
                return Err(BackendError::Internal("injected failure".to_string()));
            }
            self.props
                .borrow_mut()
                .insert(name.to_string(), value.to_string());
            Ok(())
        }
    }

    #[test]
    fn allowlist_auth_checks_root_manager_and_companion() {
        let dir = TempDir::new().unwrap();
        let path = dir.path().join(".companion_allowlist");
        let cert = "a".repeat(64);
        upsert_companion(
            &path,
            "com.kousei.kaorios",
            &cert,
            Some(10234),
            true,
            Some("Kaorios".to_string()),
        )
        .unwrap();
        let allowlist = load_allowlist(&path).unwrap();

        // Root uid=0
        assert!(authorize_peer(
            &PeerIdentity {
                uid: 0,
                pid: 1,
                gid: 0,
                package: None,
                cert_sha256: None
            },
            &[10111],
            &allowlist
        ));

        // Manager appId
        assert!(authorize_peer(
            &PeerIdentity {
                uid: 10111,
                pid: 2,
                gid: 10111,
                package: None,
                cert_sha256: None
            },
            &[10111],
            &allowlist
        ));

        // Valid companion
        assert!(authorize_peer(
            &PeerIdentity {
                uid: 10234,
                pid: 3,
                gid: 10234,
                package: Some("com.kousei.kaorios".to_string()),
                cert_sha256: Some(cert.clone())
            },
            &[10111],
            &allowlist
        ));

        // Wrong cert rejected
        assert!(!authorize_peer(
            &PeerIdentity {
                uid: 10234,
                pid: 3,
                gid: 10234,
                package: Some("com.kousei.kaorios".to_string()),
                cert_sha256: Some("b".repeat(64))
            },
            &[10111],
            &allowlist
        ));

        // Unknown uid rejected
        assert!(!authorize_peer(
            &PeerIdentity {
                uid: 10999,
                pid: 4,
                gid: 10999,
                package: Some("com.evil.app".to_string()),
                cert_sha256: Some(cert)
            },
            &[10111],
            &allowlist
        ));
    }

    #[test]
    fn contract_version_generation_idempotency_and_partial_apply() {
        let mut backend = MockBackend::new();
        let mut state = ContractState::default();

        // Unsupported version
        let resp = dispatch(
            &backend,
            &mut state,
            IpcRequest {
                id: Some("1".into()),
                method: "getCapabilities".into(),
                api_version: Some(99),
                params: json!({}),
            },
        );
        assert!(!resp.ok);
        assert_eq!(resp.error.unwrap().code, ERR_UNSUPPORTED_VERSION);

        // Stale generation
        let resp = dispatch(
            &backend,
            &mut state,
            IpcRequest {
                id: Some("2".into()),
                method: "applyConfiguration".into(),
                api_version: Some(1),
                params: json!({
                    "requestId": "req-stale",
                    "expectedGeneration": 5,
                    "desiredGeneration": 6,
                    "configuration": {}
                }),
            },
        );
        assert!(!resp.ok);
        assert_eq!(resp.error.unwrap().code, ERR_STALE_GENERATION);

        // Valid apply + idempotency
        let apply_req = IpcRequest {
            id: Some("3".into()),
            method: "applyConfiguration".into(),
            api_version: Some(1),
            params: json!({
                "requestId": "req-1",
                "expectedGeneration": 0,
                "desiredGeneration": 1,
                "configuration": {
                    "features": { "su_compat": 0 },
                    "properties": { "ro.test.prop": "ok" }
                }
            }),
        };
        let resp1 = dispatch(&backend, &mut state, apply_req.clone());
        assert!(resp1.ok);
        assert_eq!(state.applied_generation, 1);

        // Repeat same requestId -> idempotent success even though generation is now 1
        let resp2 = dispatch(&backend, &mut state, apply_req);
        assert!(resp2.ok);
        assert_eq!(resp2.result.unwrap()["appliedGeneration"], 1);

        // Partial apply failure retains retry state (does not bump generation)
        backend.fail_prop = Some("ro.fail.prop".to_string());
        let fail_req = IpcRequest {
            id: Some("4".into()),
            method: "applyConfiguration".into(),
            api_version: Some(1),
            params: json!({
                "requestId": "req-2",
                "expectedGeneration": 1,
                "desiredGeneration": 2,
                "configuration": {
                    "properties": { "ro.fail.prop": "x" }
                }
            }),
        };
        let resp3 = dispatch(&backend, &mut state, fail_req);
        assert!(resp3.ok);
        let r3 = resp3.result.unwrap();
        assert_eq!(r3["success"], false);
        assert_eq!(r3["appliedGeneration"], 1);
        assert_eq!(state.applied_generation, 1);

        // Diagnostics contains no sensitive payloads
        let diag = dispatch(
            &backend,
            &mut state,
            IpcRequest {
                id: Some("5".into()),
                method: "diagnostics".into(),
                api_version: Some(1),
                params: json!({ "freshProbe": true }),
            },
        );
        assert!(diag.ok);
        let d = diag.result.unwrap();
        assert_eq!(d["freshProbe"], true);
        assert!(d.get("configuration").is_none());
        assert!(d.get("keybox").is_none());
    }
}
