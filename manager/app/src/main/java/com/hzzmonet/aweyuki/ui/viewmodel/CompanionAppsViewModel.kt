package com.hzzmonet.aweyuki.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzzmonet.aweyuki.domain.model.CompanionAppRecord
import com.hzzmonet.aweyuki.domain.model.CompanionInstalledApp
import com.hzzmonet.aweyuki.domain.usecase.AuthorizeCompanionAppUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveCompanionAppsStateUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshCompanionAppsUseCase
import com.hzzmonet.aweyuki.domain.usecase.RevokeCompanionAppUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetCompanionAppEnabledUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CompanionAppsUiState(
    val trustedApps: List<CompanionAppRecord> = emptyList(),
    val installedApps: List<CompanionInstalledApp> = emptyList(),
    val search: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface CompanionAppsUiAction {
    data object Refresh : CompanionAppsUiAction
    data class Search(val query: String) : CompanionAppsUiAction
    data class AuthorizeInstalled(val app: CompanionInstalledApp) : CompanionAppsUiAction
    data class AuthorizeManual(
        val packageName: String,
        val certSha256: String,
        val label: String,
    ) : CompanionAppsUiAction

    data class Revoke(val packageName: String, val certSha256: String? = null) : CompanionAppsUiAction
    data class SetEnabled(val entry: CompanionAppRecord, val enabled: Boolean) : CompanionAppsUiAction
}

enum class CompanionAppsOperation { Authorize, Revoke, Toggle }

sealed interface CompanionAppsUiEvent {
    data class OperationCompleted(
        val operation: CompanionAppsOperation,
        val success: Boolean,
    ) : CompanionAppsUiEvent
}

class CompanionAppsViewModel(
    observeState: ObserveCompanionAppsStateUseCase,
    private val refreshCompanionApps: RefreshCompanionAppsUseCase,
    private val authorizeCompanionApp: AuthorizeCompanionAppUseCase,
    private val revokeCompanionApp: RevokeCompanionAppUseCase,
    private val setCompanionAppEnabled: SetCompanionAppEnabledUseCase,
) : ViewModel() {
    private val search = MutableStateFlow("")
    private val mutableEvents = MutableSharedFlow<CompanionAppsUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CompanionAppsUiEvent> = mutableEvents.asSharedFlow()

    val uiState: StateFlow<CompanionAppsUiState> = combine(observeState(), search) { source, query ->
        val normalized = query.trim()
        CompanionAppsUiState(
            trustedApps = if (normalized.isEmpty()) {
                source.trustedApps
            } else {
                source.trustedApps.filter { entry ->
                    entry.label.contains(normalized, ignoreCase = true) ||
                        entry.packageName.contains(normalized, ignoreCase = true)
                }
            },
            installedApps = if (normalized.isEmpty()) {
                source.installedApps
            } else {
                source.installedApps.filter { app ->
                    app.label.contains(normalized, ignoreCase = true) ||
                        app.packageName.contains(normalized, ignoreCase = true)
                }
            },
            search = query,
            isLoading = source.isLoading,
            isRefreshing = source.isRefreshing,
            isSubmitting = source.isSubmitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CompanionAppsUiState(),
    )

    fun dispatch(action: CompanionAppsUiAction) {
        when (action) {
            CompanionAppsUiAction.Refresh -> viewModelScope.launch { refresh() }

            is CompanionAppsUiAction.Search -> search.value = action.query

            is CompanionAppsUiAction.AuthorizeInstalled -> submit(CompanionAppsOperation.Authorize) {
                authorizeCompanionApp(
                    packageName = action.app.packageName,
                    certSha256 = action.app.certSha256,
                    uid = action.app.uid,
                    label = action.app.label,
                )
            }

            is CompanionAppsUiAction.AuthorizeManual -> submit(CompanionAppsOperation.Authorize) {
                authorizeCompanionApp(
                    packageName = action.packageName,
                    certSha256 = action.certSha256,
                    uid = null,
                    label = action.label.ifBlank { null },
                )
            }

            is CompanionAppsUiAction.Revoke -> submit(CompanionAppsOperation.Revoke) {
                revokeCompanionApp(action.packageName, action.certSha256)
            }

            is CompanionAppsUiAction.SetEnabled -> submit(CompanionAppsOperation.Toggle) {
                setCompanionAppEnabled(action.entry, action.enabled)
            }
        }
    }

    suspend fun refresh(): Result<Unit> = refreshCompanionApps()

    private fun submit(
        operation: CompanionAppsOperation,
        command: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            val success = command().isSuccess
            mutableEvents.emit(CompanionAppsUiEvent.OperationCompleted(operation, success))
        }
    }
}
