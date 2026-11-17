package com.hzzmonet.aweyuki.ui.activity.util

import com.hzzmonet.aweyuki.data.theme.ThemeRepository
import com.hzzmonet.aweyuki.ui.theme.BackgroundManager
import com.hzzmonet.aweyuki.ui.theme.CardConfig
import com.hzzmonet.aweyuki.ui.theme.ThemeConfig
import com.hzzmonet.aweyuki.ui.viewmodel.SettingsUiAction
import com.hzzmonet.aweyuki.ui.viewmodel.SettingsViewModel

class ThemeUtils(
    private val themeConfig: ThemeConfig,
    private val themeRepository: ThemeRepository,
    private val cardConfig: CardConfig,
    private val backgroundManager: BackgroundManager,
) {

    fun initializeThemeSettings(settingsViewModel: SettingsViewModel) {
        settingsViewModel.dispatch(SettingsUiAction.InitializeFirstRun)
        loadThemeSettings()
        settingsViewModel.dispatch(SettingsUiAction.Initialize)
    }

    fun onActivityPause() {
        cardConfig.save()
    }

    fun onActivityResume() {
        loadThemeSettings()
    }

    private fun loadThemeSettings() {
        themeConfig.forceDarkMode = themeRepository.loadThemeMode()
        themeConfig.seedColor = themeRepository.loadSeedColor()
        themeConfig.useDynamicColor = themeRepository.loadDynamicColorState()
        themeConfig.dynamicColorSpec = themeRepository.loadDynamicColorSpec()
        themeConfig.dynamicPaletteStyle = themeRepository.loadDynamicPaletteStyle(
            themeConfig.dynamicColorSpec,
        )
        cardConfig.load()
        backgroundManager.loadCustomBackground()
    }
}
