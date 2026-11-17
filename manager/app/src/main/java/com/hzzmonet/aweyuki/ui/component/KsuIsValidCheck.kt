package com.hzzmonet.aweyuki.ui.component

import androidx.compose.runtime.Composable
import com.hzzmonet.aweyuki.domain.model.KernelStatus

@Composable
inline fun KsuIsValid(
    status: KernelStatus,
    content: @Composable () -> Unit,
) {
    if (status.isFullFeatured) {
        content()
    }
}
