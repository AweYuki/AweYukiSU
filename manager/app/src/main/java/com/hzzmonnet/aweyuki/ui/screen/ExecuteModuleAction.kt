package com.hzzmonnet.aweyuki.ui.screen

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hzzmonnet.aweyuki.R
import com.hzzmonnet.aweyuki.ui.component.ModuleActionOutput
import com.hzzmonnet.aweyuki.ui.navigation.LocalNavigator
import com.hzzmonnet.aweyuki.ui.util.LocalSnackbarHost
import com.hzzmonnet.aweyuki.ui.util.showReplacingSnackbar
import com.hzzmonnet.aweyuki.ui.viewmodel.ExecuteModuleActionUiAction
import com.hzzmonnet.aweyuki.ui.viewmodel.ExecuteModuleActionUiEvent
import com.hzzmonnet.aweyuki.ui.viewmodel.ExecuteModuleActionViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ExecuteModuleActionScreen(moduleId: String, fromShortcut: Boolean) {
    val viewModel = koinViewModel<ExecuteModuleActionViewModel>(
        parameters = { parametersOf(moduleId) },
    )
    val moduleActionState by viewModel.state.collectAsStateWithLifecycle()
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val navigator = LocalNavigator.current

    LaunchedEffect(viewModel, fromShortcut) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ExecuteModuleActionUiEvent.Completed -> {
                    if (event.successful && fromShortcut) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.module_action_success),
                            Toast.LENGTH_SHORT,
                        ).show()
                        activity?.finishAndRemoveTask()
                    }
                }

                is ExecuteModuleActionUiEvent.LogSaved -> {
                    snackBarHost.showReplacingSnackbar("Log saved to ${event.path}")
                }

                is ExecuteModuleActionUiEvent.Error -> {
                    snackBarHost.showReplacingSnackbar(event.message)
                }
            }
        }
    }

    ModuleActionOutput(
        output = moduleActionState.output,
        running = moduleActionState.running,
        title = stringResource(R.string.action),
        onBack = { navigator.pop() },
        onSave = { viewModel.dispatch(ExecuteModuleActionUiAction.SaveLog) },
    )
}
