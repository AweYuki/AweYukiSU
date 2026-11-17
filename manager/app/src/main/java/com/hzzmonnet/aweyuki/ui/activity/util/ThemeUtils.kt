package com.hzzmonnet.aweyuki.ui.activity.util

import com.hzzmonnet.aweyuki.data.theme.ThemeRepository
import com.hzzmonnet.aweyuki.ui.theme.BackgroundManager
import com.hzzmonnet.aweyuki.ui.theme.CardConfig
import com.hzzmonnet.aweyuki.ui.theme.ThemeConfig
import com.hzzmonnet.aweyuki.ui.viewmodel.SettingsUiAction
import com.hzzmonnet.aweyuki.ui.viewmodel.SettingsViewModel

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
