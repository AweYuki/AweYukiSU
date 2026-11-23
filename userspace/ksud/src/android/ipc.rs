use std::{
    collections::HashMap,
    fs::{self, File, OpenOptions, Permissions},
    io::{BufRead, BufReader, Write},
    os::{
        fd::AsRawFd,
        unix::{
            fs::PermissionsExt,
            net::{UnixListener, UnixStream},
            process::CommandExt,
        },
    },
    path::Path,
    process::{Command, Stdio},
    sync::{Arc, Mutex},
    time::Duration,
};

use anyhow::{Context, Result};
use log::{info, warn};

use crate::{
    android::{feature, ksucalls, module, profile, resetprop, sepolicy, umount_config, utils},
    apk_sign, defs,
    ipc_protocol::{
        self, BackendError, BackendStatus, ContractState, FeatureInfo, IpcRequest, IpcResponse,
        KsuBackend, MAX_REQUEST_BYTES, MountEntry, PeerIdentity,
    },
};

const SOCKET_IO_TIMEOUT: Duration = Duration::from_secs(10);

struct AndroidKsuBackend;

impl AndroidKsuBackend {
    fn map_err(e: anyhow::Error) -> BackendError {
        let msg = format!("{e:#}");
        if msg.contains("No such file") || msg.contains("not found") || msg.contains("Unknown") {
            BackendError::NotFound(msg)
        } else if msg.contains("Permission denied") || msg.contains("not allowed") {
            BackendError::PermissionDenied(msg)
        } else {
            BackendError::Internal(msg)
        }
    }
}

impl KsuBackend for AndroidKsuBackend {
    fn helper_version(&self) -> String {
        defs::FULL_VERSION.to_string()
    }

    fn supported_abis(&self) -> Vec<String> {
        let mut abis = Vec::new();
        if let Some(abilist) = utils::getprop("ro.product.cpu.abilist") {
            for abi in abilist.split(',').map(str::trim).filter(|s| !s.is_empty()) {
                abis.push(abi.to_string());
            }
        }
        if abis.is_empty() {
            abis.push(std::env::consts::ARCH.to_string());
        }
        abis
    }

    fn probe_status(&self, fresh_probe: bool) -> BackendStatus {
        let info = ksucalls::get_info();
        let kernel_version = info.version as i32;
        let uapi_matched = if fresh_probe {
            ksucalls::ensure_uapi_version_matched().is_ok()
        } else {
            info.uapi_version == ksucalls::uapi_version()
        };
        let safe_mode = ksucalls::check_kernel_safemode();
        let process_hooked = kernel_version > 0 && ksucalls::mark_get(0).is_ok();
        BackendStatus {
            kernel_version,
            uapi_matched,
            safe_mode,
            is_lkm: ksucalls::is_lkm(),
            is_late_load: ksucalls::is_late_load(),
            process_hooked,
        }
    }

    fn feature_get(&self, id: &str) -> std::result::Result<FeatureInfo, BackendError> {
        feature::query_feature(id).map_err(Self::map_err)
    }

    fn feature_set(&self, id: &str, value: u64) -> std::result::Result<(), BackendError> {
        feature::set_feature_value(id, value).map_err(Self::map_err)
    }

    fn feature_list(&self) -> std::result::Result<Vec<FeatureInfo>, BackendError> {
        Ok(feature::list_features_data())
    }

    fn module_list(&self) -> std::result::Result<Vec<HashMap<String, String>>, BackendError> {
        Ok(module::list_module(defs::MODULE_DIR))
    }

    fn module_enable(&self, id: &str) -> std::result::Result<(), BackendError> {
        module::enable_module(id).map_err(Self::map_err)
    }

    fn module_disable(&self, id: &str) -> std::result::Result<(), BackendError> {
        module::disable_module(id).map_err(Self::map_err)
    }

    fn module_uninstall(&self, id: &str) -> std::result::Result<(), BackendError> {
        module::uninstall_module(id).map_err(Self::map_err)
    }

    fn module_install(&self, zip_path: &str) -> std::result::Result<(), BackendError> {
        module::install_module(zip_path).map_err(Self::map_err)
    }

    fn module_action(&self, id: &str) -> std::result::Result<(), BackendError> {
        module::run_action(id).map_err(Self::map_err)
    }

    fn profile_get(&self, kind: &str, id: &str) -> std::result::Result<String, BackendError> {
        match kind {
            "sepolicy" => profile::read_sepolicy(id).map_err(Self::map_err),
            "template" => profile::read_template(id).map_err(Self::map_err),
            other => Err(BackendError::InvalidParams(format!(
                "unsupported profile kind '{other}'"
            ))),
        }
    }

    fn profile_set(
        &self,
        kind: &str,
        id: &str,
        content: &str,
    ) -> std::result::Result<(), BackendError> {
        match kind {
            "sepolicy" => {
                profile::set_sepolicy(id.to_string(), content.to_string()).map_err(Self::map_err)
            }
            "template" => {
                profile::set_template(id.to_string(), content.to_string()).map_err(Self::map_err)
            }
            other => Err(BackendError::InvalidParams(format!(
                "unsupported profile kind '{other}'"
            ))),
        }
    }

    fn profile_list_templates(
        &self,
        locale: Option<&str>,
    ) -> std::result::Result<Vec<String>, BackendError> {
        profile::collect_templates(false, locale).map_err(Self::map_err)
    }

    fn profile_delete_template(&self, id: &str) -> std::result::Result<(), BackendError> {
        profile::delete_template(id.to_string()).map_err(Self::map_err)
    }

    fn allowlist_get(&self, allow: bool) -> std::result::Result<Vec<u32>, BackendError> {
        ksucalls::get_allow_list(allow).map_err(Self::map_err)
    }

    fn umount_list(&self) -> std::result::Result<Vec<MountEntry>, BackendError> {
        let entries = umount_config::list_umount_entries().map_err(Self::map_err)?;
        Ok(entries
            .into_iter()
            .map(|m| MountEntry {
                path: m.path,
                flags: m.flags,
            })
            .collect())
    }

    fn umount_add(&self, path: &str, flags: u32) -> std::result::Result<(), BackendError> {
        umount_config::add_umount(path, flags).map_err(Self::map_err)?;
        ksucalls::umount_list_add(path, flags).map_err(Self::map_err)
    }

    fn umount_del(&self, path: &str) -> std::result::Result<(), BackendError> {
        umount_config::del_umount(path).map_err(Self::map_err)?;
        ksucalls::umount_list_del(path).map_err(Self::map_err)
    }

    fn resetprop_get(&self, name: &str) -> std::result::Result<Option<String>, BackendError> {
        resetprop::get_property_direct(name).map_err(Self::map_err)
    }

    fn resetprop_set(&self, name: &str, value: &str) -> std::result::Result<(), BackendError> {
        resetprop::set_property_direct(name, value).map_err(Self::map_err)
    }
}

fn get_peer_cred(stream: &UnixStream) -> Result<libc::ucred> {
    let mut cred = libc::ucred {
        pid: 0,
        uid: u32::MAX,
        gid: u32::MAX,
    };
    let mut len = std::mem::size_of::<libc::ucred>() as libc::socklen_t;
    let rc = unsafe {
        libc::getsockopt(
            stream.as_raw_fd(),
            libc::SOL_SOCKET,
            libc::SO_PEERCRED,
            (&raw mut cred).cast::<libc::c_void>(),
            &raw mut len,
        )
    };
    if rc != 0 {
        return Err(std::io::Error::last_os_error()).context("getsockopt(SO_PEERCRED) failed");
    }
    Ok(cred)
}

fn read_peer_package(pid: i32) -> Option<String> {
    if pid <= 0 {
        return None;
    }
    let raw = fs::read(format!("/proc/{pid}/cmdline")).ok()?;
    let end = raw.iter().position(|&b| b == 0).unwrap_or(raw.len());
    let cmd = std::str::from_utf8(&raw[..end]).ok()?.trim();
    if cmd.is_empty() {
        return None;
    }
    // Strip process suffix like com.pkg:remote -> com.pkg
    Some(cmd.split(':').next().unwrap_or(cmd).to_string())
}

fn find_peer_apk_from_maps(pid: i32) -> Option<String> {
    if pid <= 0 {
        return None;
    }
    let maps = fs::read_to_string(format!("/proc/{pid}/maps")).ok()?;
    for line in maps.lines() {
        let Some(path) = line.split_whitespace().last() else {
            continue;
        };
        if path.ends_with(".apk")
            && (path.starts_with("/data/app/")
                || path.starts_with("/system/")
                || path.starts_with("/product/")
                || path.starts_with("/system_ext/")
                || path.starts_with("/vendor/"))
            && Path::new(path).is_file()
        {
            return Some(path.to_string());
        }
    }
    None
}

fn resolve_peer_identity(cred: libc::ucred) -> PeerIdentity {
    let package = read_peer_package(cred.pid);
    let cert_sha256 = find_peer_apk_from_maps(cred.pid)
        .and_then(|apk| apk_sign::get_apk_signature(&apk).ok())
        .map(|(_, hash)| hash);
    PeerIdentity {
        uid: cred.uid,
        pid: cred.pid,
        gid: cred.gid,
        package,
        cert_sha256,
    }
}

fn get_manager_app_ids() -> Vec<u32> {
    let mut ids = Vec::new();
    if let Ok(appid) = ksucalls::get_manager_appid()
        && appid > 0
    {
        ids.push(appid % ipc_protocol::ANDROID_USER_UID_STRIDE);
    }
    ids
}

fn apply_ipc_sepolicy() {
    const RULES: &str = "\
allow untrusted_app ksu unix_stream_socket connectto
allow priv_app ksu unix_stream_socket connectto
allow system_app ksu unix_stream_socket connectto
allow untrusted_app socket_device sock_file { write getattr }
allow priv_app socket_device sock_file { write getattr }
allow system_app socket_device sock_file { write getattr }";
    if let Err(e) = sepolicy::live_patch(RULES) {
        warn!("failed to apply scoped IPC sepolicy rules: {e:#}");
    }
}

fn write_response(stream: &mut UnixStream, resp: &IpcResponse) {
    if let Ok(mut line) = serde_json::to_string(resp) {
        line.push('\n');
        let _ = stream.write_all(line.as_bytes());
        let _ = stream.flush();
    }
}

fn handle_client(mut stream: UnixStream, state: Arc<Mutex<ContractState>>) {
    let _ = stream.set_read_timeout(Some(SOCKET_IO_TIMEOUT));
    let _ = stream.set_write_timeout(Some(SOCKET_IO_TIMEOUT));

    let cred = match get_peer_cred(&stream) {
        Ok(c) => c,
        Err(e) => {
            warn!("IPC SO_PEERCRED failed: {e:#}");
            write_response(
                &mut stream,
                &IpcResponse::unauthorized(None, "failed to verify peer credentials"),
            );
            return;
        }
    };

    let peer = resolve_peer_identity(cred);
    let allowlist =
        ipc_protocol::load_allowlist(Path::new(defs::COMPANION_ALLOWLIST_PATH)).unwrap_or_default();
    let manager_ids = get_manager_app_ids();
    let authorized = ipc_protocol::authorize_peer(&peer, &manager_ids, &allowlist);

    let reader_stream = match stream.try_clone() {
        Ok(s) => s,
        Err(_) => return,
    };
    let mut reader = BufReader::new(reader_stream);
    let mut buf = Vec::new();
    let backend = AndroidKsuBackend;

    loop {
        buf.clear();
        let n = match reader.read_until(b'\n', &mut buf) {
            Ok(0) => break,
            Ok(n) => n,
            Err(_) => break,
        };
        if n > MAX_REQUEST_BYTES {
            write_response(
                &mut stream,
                &IpcResponse::err(
                    None,
                    ipc_protocol::ERR_INVALID_PARAMS,
                    ipc_protocol::CODE_INVALID_PARAMS,
                    "request exceeds max size",
                ),
            );
            break;
        }
        let line = match std::str::from_utf8(&buf) {
            Ok(s) => s.trim(),
            Err(_) => {
                write_response(
                    &mut stream,
                    &IpcResponse::err(
                        None,
                        ipc_protocol::ERR_INVALID_PARAMS,
                        ipc_protocol::CODE_INVALID_PARAMS,
                        "invalid UTF-8",
                    ),
                );
                continue;
            }
        };
        if line.is_empty() {
            continue;
        }
        let req: IpcRequest = match serde_json::from_str(line) {
            Ok(r) => r,
            Err(e) => {
                write_response(
                    &mut stream,
                    &IpcResponse::err(
                        None,
                        ipc_protocol::ERR_INVALID_PARAMS,
                        ipc_protocol::CODE_INVALID_PARAMS,
                        format!("invalid JSON-RPC request: {e}"),
                    ),
                );
                continue;
            }
        };

        if !authorized {
            write_response(
                &mut stream,
                &IpcResponse::unauthorized(req.id, "caller is not in companion allowlist"),
            );
            continue;
        }

        let resp = {
            let mut guard = state.lock().unwrap_or_else(|e| e.into_inner());
            ipc_protocol::dispatch(&backend, &mut guard, req)
        };
        write_response(&mut stream, &resp);
    }
}

fn acquire_lock() -> Result<File> {
    utils::ensure_dir_exists(Path::new(defs::WORKING_DIR))?;
    let lock = OpenOptions::new()
        .create(true)
        .write(true)
        .truncate(false)
        .open(defs::IPC_SERVER_LOCK_PATH)
        .with_context(|| format!("failed to open {}", defs::IPC_SERVER_LOCK_PATH))?;
    let rc = unsafe { libc::flock(lock.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) };
    if rc != 0 {
        anyhow::bail!("ipc-server already running");
    }
    Ok(lock)
}

pub fn run_server(socket_override: Option<&str>) -> Result<()> {
    let _lock = acquire_lock()?;
    let env_socket = std::env::var("KSUD_IPC_SOCKET").ok();
    let socket_path = socket_override
        .or(env_socket.as_deref())
        .unwrap_or(defs::IPC_SOCKET_PATH);

    let sock = Path::new(socket_path);
    if let Some(parent) = sock.parent() {
        utils::ensure_dir_exists(parent)?;
    }
    if sock.exists() {
        let _ = fs::remove_file(sock);
    }

    let listener = UnixListener::bind(sock)
        .with_context(|| format!("failed to bind IPC socket {socket_path}"))?;
    fs::set_permissions(sock, Permissions::from_mode(0o666))
        .with_context(|| format!("failed to chmod {socket_path}"))?;

    apply_ipc_sepolicy();
    info!("ksud IPC server listening on {socket_path}");

    let state = Arc::new(Mutex::new(ContractState::default()));
    for stream in listener.incoming() {
        match stream {
            Ok(s) => {
                let st = Arc::clone(&state);
                std::thread::spawn(move || handle_client(s, st));
            }
            Err(e) => warn!("IPC accept error: {e}"),
        }
    }
    Ok(())
}

pub fn ensure_ipc_server_running() -> Result<()> {
    let exe =
        std::env::current_exe().unwrap_or_else(|_| Path::new(defs::DAEMON_PATH).to_path_buf());
    let mut cmd = Command::new(exe);
    cmd.arg("ipc-server")
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null());
    unsafe {
        cmd.pre_exec(|| {
            libc::setsid();
            Ok(())
        });
    }
    cmd.spawn().context("failed to spawn ksud ipc-server")?;
    Ok(())
}
