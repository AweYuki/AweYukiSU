package com.hzzmonet.aweyuki.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AddModerator
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hzzmonet.aweyuki.R
import com.hzzmonet.aweyuki.domain.model.CompanionAppRecord
import com.hzzmonet.aweyuki.domain.model.CompanionInstalledApp
import com.hzzmonet.aweyuki.ui.component.ConfirmResult
import com.hzzmonet.aweyuki.ui.component.DialogHandle
import com.hzzmonet.aweyuki.ui.component.PackageIcon
import com.hzzmonet.aweyuki.ui.component.SearchAppBar
import com.hzzmonet.aweyuki.ui.component.SwipeableSnackbarHost
import com.hzzmonet.aweyuki.ui.component.rememberConfirmDialog
import com.hzzmonet.aweyuki.ui.component.rememberCustomDialog
import com.hzzmonet.aweyuki.ui.component.rememberSearchAppBarScrollBehavior
import com.hzzmonet.aweyuki.ui.component.settings.SegmentedColumn
import com.hzzmonet.aweyuki.ui.component.settings.SettingsBaseWidget
import com.hzzmonet.aweyuki.ui.component.settings.SettingsTextFieldWidget
import com.hzzmonet.aweyuki.ui.component.settings.lazySegmentColumn
import com.hzzmonet.aweyuki.ui.navigation.LocalNavigator
import com.hzzmonet.aweyuki.ui.theme.blurSource
import com.hzzmonet.aweyuki.ui.util.ActivityResumeEffect
import com.hzzmonet.aweyuki.ui.util.LocalSnackbarHost
import com.hzzmonet.aweyuki.ui.util.adaptiveScaffoldWindowInsets
import com.hzzmonet.aweyuki.ui.util.showReplacingSnackbar
import com.hzzmonet.aweyuki.ui.viewmodel.CompanionAppsOperation
import com.hzzmonet.aweyuki.ui.viewmodel.CompanionAppsUiAction
import com.hzzmonet.aweyuki.ui.viewmodel.CompanionAppsUiEvent
import com.hzzmonet.aweyuki.ui.viewmodel.CompanionAppsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompanionAppsScreen() {
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<CompanionAppsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = rememberSearchAppBarScrollBehavior(
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()),
    )
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var isUserRefreshing by remember { mutableStateOf(false) }
    val confirmDialog = rememberConfirmDialog()

    val authorizeTitle = stringResource(R.string.companion_apps_authorize_confirm_title)
    val revokeTitle = stringResource(R.string.companion_apps_revoke_confirm_title)
    val confirmText = stringResource(R.string.confirm)
    val authorizeSuccess = stringResource(R.string.companion_apps_authorize_success)
    val authorizeFailed = stringResource(R.string.companion_apps_authorize_failed)
    val revokeSuccess = stringResource(R.string.companion_apps_revoke_success)
    val revokeFailed = stringResource(R.string.companion_apps_revoke_failed)
    val toggleFailed = stringResource(R.string.companion_apps_toggle_failed)
    val trustedSectionTitle = stringResource(R.string.companion_apps_trusted_section)
    val installedSectionTitle = stringResource(R.string.companion_apps_installed_section)

    val authorizeConfirmFormat = stringResource(R.string.companion_apps_authorize_confirm_message)
    val revokeConfirmFormat = stringResource(R.string.companion_apps_revoke_confirm_message)

    fun runAuthorize(displayName: String, packageName: String, action: CompanionAppsUiAction) {
        scope.launch {
            val result = confirmDialog.awaitConfirm(
                title = authorizeTitle,
                content = String.format(authorizeConfirmFormat, displayName, packageName),
                confirm = confirmText,
            )
            if (result == ConfirmResult.Confirmed) {
                viewModel.dispatch(action)
            }
        }
    }

    fun runRevoke(packageName: String, certSha256: String? = null) {
        scope.launch {
            val result = confirmDialog.awaitConfirm(
                title = revokeTitle,
                content = String.format(revokeConfirmFormat, packageName),
                confirm = confirmText,
            )
            if (result == ConfirmResult.Confirmed) {
                viewModel.dispatch(CompanionAppsUiAction.Revoke(packageName, certSha256))
            }
        }
    }

    val manualDialog = rememberCompanionManualDialog { packageName, certSha256, label ->
        runAuthorize(
            displayName = label.ifBlank { packageName },
            packageName = packageName,
            action = CompanionAppsUiAction.AuthorizeManual(packageName, certSha256, label),
        )
    }

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
        viewModel.events.collectLatest { event ->
            when (event) {
                is CompanionAppsUiEvent.OperationCompleted -> {
                    val message = when (event.operation) {
                        CompanionAppsOperation.Authorize -> if (event.success) authorizeSuccess else authorizeFailed
                        CompanionAppsOperation.Revoke -> if (event.success) revokeSuccess else revokeFailed
                        CompanionAppsOperation.Toggle -> if (!event.success) toggleFailed else null
                    }
                    if (message != null) {
                        snackbarHost.showReplacingSnackbar(message)
                    }
                }
            }
        }
    }

    ActivityResumeEffect {
        viewModel.dispatch(CompanionAppsUiAction.Refresh)
    }

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        topBar = {
            SearchAppBar(
                title = stringResource(R.string.companion_apps_title),
                searchText = uiState.search,
                onSearchTextChange = { viewModel.dispatch(CompanionAppsUiAction.Search(it)) },
                onBackClick = { navigator.pop() },
                scrollBehavior = scrollBehavior,
                searchBarPlaceHolderText = stringResource(R.string.search_apps),
            )
        },
        snackbarHost = { SwipeableSnackbarHost(hostState = snackbarHost) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        } else {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isUserRefreshing,
                onRefresh = {
                    scope.launch {
                        isUserRefreshing = true
                        try {
                            viewModel.refresh()
                        } finally {
                            isUserRefreshing = false
                        }
                    }
                },
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isUserRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = paddingValues.calculateTopPadding()),
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .blurSource(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 5.dp,
                        start = 0.dp,
                        end = 0.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    item {
                        SegmentedColumn(
                            title = stringResource(R.string.companion_apps_title),
                        ) {
                            item {
                                SettingsBaseWidget(
                                    icon = Icons.TwoTone.AddModerator,
                                    title = stringResource(R.string.companion_apps_manual_add),
                                    description = stringResource(R.string.companion_apps_manual_add_summary),
                                    enabled = !uiState.isSubmitting,
                                    onClick = { manualDialog.show() },
                                )
                            }
                            item(visible = uiState.trustedApps.isEmpty()) {
                                SettingsBaseWidget(
                                    icon = Icons.TwoTone.VerifiedUser,
                                    title = stringResource(R.string.companion_apps_none_trusted),
                                    onClick = {},
                                )
                            }
                        }
                    }

                    if (uiState.trustedApps.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        lazySegmentColumn(
                            title = trustedSectionTitle,
                            items = uiState.trustedApps,
                            key = { _, entry -> "${entry.packageName}-${entry.certSha256}" },
                            contentType = { _, _ -> "TrustedCompanionItem" },
                        ) { _, entry ->
                            TrustedCompanionItem(
                                entry = entry,
                                enabled = !uiState.isSubmitting,
                                onToggle = { checked ->
                                    viewModel.dispatch(CompanionAppsUiAction.SetEnabled(entry, checked))
                                },
                                onRevoke = {
                                    runRevoke(entry.packageName, entry.certSha256)
                                },
                            )
                        }
                    }

                    if (uiState.installedApps.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        lazySegmentColumn(
                            title = installedSectionTitle,
                            items = uiState.installedApps,
                            key = { _, app -> "${app.uid}-${app.packageName}" },
                            contentType = { _, _ -> "CompanionInstalledAppItem" },
                        ) { _, app ->
                            CompanionInstalledAppItem(
                                app = app,
                                enabled = !uiState.isSubmitting,
                                onClick = {
                                    if (app.isTrusted) {
                                        runRevoke(app.packageName, app.certSha256)
                                    } else {
                                        runAuthorize(
                                            displayName = app.label,
                                            packageName = app.packageName,
                                            action = CompanionAppsUiAction.AuthorizeInstalled(app),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustedCompanionItem(
    entry: CompanionAppRecord,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onRevoke: () -> Unit,
) {
    val statusText = if (entry.enabled) {
        stringResource(R.string.companion_apps_status_enabled, entry.certSha256.take(12))
    } else {
        stringResource(R.string.companion_apps_status_disabled, entry.certSha256.take(12))
    }
    SettingsBaseWidget(
        enabled = enabled,
        onClick = { onToggle(!entry.enabled) },
        title = entry.label.ifBlank { entry.packageName },
        description = "${entry.packageName}\n$statusText",
        leadingContent = {
            PackageIcon(
                packageName = entry.packageName,
                contentDescription = entry.label.ifBlank { entry.packageName },
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp),
            )
        },
        iconPlaceholder = false,
    ) {
        Switch(
            checked = entry.enabled,
            enabled = enabled,
            onCheckedChange = onToggle,
        )
        IconButton(
            enabled = enabled,
            onClick = onRevoke,
        ) {
            Icon(
                imageVector = Icons.TwoTone.Delete,
                contentDescription = stringResource(R.string.companion_apps_revoke_confirm_title),
            )
        }
    }
}

@Composable
private fun CompanionInstalledAppItem(
    app: CompanionInstalledApp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    SettingsBaseWidget(
        enabled = enabled,
        onClick = onClick,
        title = app.label,
        description = "${app.packageName}\n${app.certSha256.take(16)}...",
        leadingContent = {
            PackageIcon(
                packageName = app.packageName,
                contentDescription = app.label,
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp),
            )
        },
        iconPlaceholder = false,
    ) {
        Checkbox(
            checked = app.isTrusted,
            enabled = enabled,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun rememberCompanionManualDialog(
    onConfirm: (String, String, String) -> Unit,
): DialogHandle = rememberCustomDialog { dismiss ->
    CompanionManualDialog(
        onDismiss = dismiss,
        onConfirm = { packageName, certSha256, label ->
            dismiss()
            onConfirm(packageName, certSha256, label)
        },
    )
}

@Composable
private fun CompanionManualDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    val packageState = rememberTextFieldState()
    val certState = rememberTextFieldState()
    val labelState = rememberTextFieldState()
    val packageName = packageState.text.toString().trim()
    val normalizedCert = certState.text.toString().replace(":", "").trim().lowercase()
    val certValid = normalizedCert.length == 64 && normalizedCert.all { it in '0'..'9' || it in 'a'..'f' }
    val isValid = packageName.isNotEmpty() && certValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.companion_apps_manual_add)) },
        text = {
            SegmentedColumn {
                item {
                    SettingsTextFieldWidget(
                        modifier = Modifier.fillMaxWidth(),
                        renderBackgroundBlur = false,
                        state = packageState,
                        title = stringResource(R.string.companion_apps_package_name),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }

                item {
                    SettingsTextFieldWidget(
                        modifier = Modifier.fillMaxWidth(),
                        renderBackgroundBlur = false,
                        state = certState,
                        title = stringResource(R.string.companion_apps_cert_sha256),
                        error = if (certState.text.isNotEmpty() && !certValid) {
                            stringResource(R.string.hash_must_be_64_chars)
                        } else {
                            ""
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }

                item {
                    SettingsTextFieldWidget(
                        modifier = Modifier.fillMaxWidth(),
                        renderBackgroundBlur = false,
                        state = labelState,
                        title = stringResource(R.string.companion_apps_label_optional),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onConfirm(packageName, normalizedCert, labelState.text.toString().trim())
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
