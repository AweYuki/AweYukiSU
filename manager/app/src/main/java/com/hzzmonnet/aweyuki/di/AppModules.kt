package com.hzzmonnet.aweyuki.di

import coil.ImageLoader
import com.hzzmonnet.aweyuki.BuildConfig
import com.hzzmonnet.aweyuki.data.AppSettingsRepository
import com.hzzmonnet.aweyuki.data.application.ApplicationControlRepository
import com.hzzmonnet.aweyuki.data.application.DynamicManagerRepository
import com.hzzmonnet.aweyuki.data.count.CountRepository
import com.hzzmonnet.aweyuki.data.download.DownloadRepository
import com.hzzmonnet.aweyuki.data.file.ModuleFileRepository
import com.hzzmonnet.aweyuki.data.flash.FlashRepository
import com.hzzmonnet.aweyuki.data.kernel.KernelRepository
import com.hzzmonnet.aweyuki.data.kernel.UmountRepository
import com.hzzmonnet.aweyuki.data.logging.BugreportRepository
import com.hzzmonnet.aweyuki.data.logging.SulogRepository
import com.hzzmonnet.aweyuki.data.module.ModuleActionRepository
import com.hzzmonnet.aweyuki.data.module.ModuleCatalogRepository
import com.hzzmonnet.aweyuki.data.module.ModuleIconRepository
import com.hzzmonnet.aweyuki.data.module.ModulePreferencesRepository
import com.hzzmonnet.aweyuki.data.module.ModuleReleaseRepository
import com.hzzmonnet.aweyuki.data.module.ModuleRepository
import com.hzzmonnet.aweyuki.data.module.RepositorySourceStore
import com.hzzmonnet.aweyuki.data.module.registry.KernelSuRegistryRepository
import com.hzzmonnet.aweyuki.data.network.NetworkRequestRepository
import com.hzzmonnet.aweyuki.data.network.NetworkStatusRepository
import com.hzzmonnet.aweyuki.data.network.WebResourceRepository
import com.hzzmonnet.aweyuki.data.packageinfo.AppIconDataSource
import com.hzzmonnet.aweyuki.data.packageinfo.InstalledPackageCache
import com.hzzmonnet.aweyuki.data.packageinfo.InstalledPackageRepository
import com.hzzmonnet.aweyuki.data.packageinfo.RootServiceRepository
import com.hzzmonnet.aweyuki.data.packageinfo.SuperUserRepository
import com.hzzmonnet.aweyuki.data.profile.ProfileRepository
import com.hzzmonnet.aweyuki.data.profile.ProfileTemplateRepository
import com.hzzmonnet.aweyuki.data.script.ScriptRepository
import com.hzzmonnet.aweyuki.data.settings.LocaleHelper
import com.hzzmonnet.aweyuki.data.settings.LocaleRepository
import com.hzzmonnet.aweyuki.data.settings.SettingsPlatformRepository
import com.hzzmonnet.aweyuki.data.shell.KsuCliRepository
import com.hzzmonnet.aweyuki.data.shell.ShortcutRepository
import com.hzzmonnet.aweyuki.data.startup.ApplicationInitializationRepository
import com.hzzmonnet.aweyuki.data.startup.StartupRepository
import com.hzzmonnet.aweyuki.data.susfs.SuSFSConfigHelper
import com.hzzmonnet.aweyuki.data.susfs.SuSFSRepository
import com.hzzmonnet.aweyuki.data.system.HomeRuntimeRepository
import com.hzzmonnet.aweyuki.data.system.HomeStateRepository
import com.hzzmonnet.aweyuki.data.text.HanziToPinyin
import com.hzzmonnet.aweyuki.data.theme.MonetCompatColorSource
import com.hzzmonnet.aweyuki.data.theme.ThemeRepository
import com.hzzmonnet.aweyuki.data.update.ManagerUpdateRepository
import com.hzzmonnet.aweyuki.data.webui.WebUiRepository
import com.hzzmonnet.aweyuki.domain.text.TextTransliterator
import com.hzzmonnet.aweyuki.domain.usecase.AddModuleRepositoryUseCase
import com.hzzmonnet.aweyuki.domain.usecase.AddUmountPathUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ApplyLanguageUseCase
import com.hzzmonnet.aweyuki.domain.usecase.BackupAllowlistUseCase
import com.hzzmonnet.aweyuki.domain.usecase.CalculateInstalledModuleSizeUseCase
import com.hzzmonnet.aweyuki.domain.usecase.CheckFlashModuleMountUseCase
import com.hzzmonnet.aweyuki.domain.usecase.CheckManagerUpdateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.CleanSulogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ClearDynamicManagerUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ConfigureSuLogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ControlAppUseCase
import com.hzzmonnet.aweyuki.domain.usecase.DeleteProfileTemplateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.EnableSulogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.EnqueueDownloadUseCase
import com.hzzmonnet.aweyuki.domain.usecase.EnqueueManagerUpdateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.EnsureManagerInstalledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ExecuteFlashOperationUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ExecuteModuleActionUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ExportProfileTemplatesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ExtractModuleIdUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ExtractModuleNameUseCase
import com.hzzmonnet.aweyuki.domain.usecase.FetchRemoteTextUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GenerateBugreportUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetAppProfileUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetAppSepolicyUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetBooleanPreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetCatalogModuleUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetDefaultUmountModulesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetHomeBasicInfoUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetInstallEnvironmentUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetKernelFeatureSettingsUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetKernelStatusUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetLastScriptRunUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetManagerRuntimeInfoUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetModuleReadmeUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetModuleReleaseDetailsUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetModuleUpdateChangelogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetPlatformFeatureStatusUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetProfileTemplateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetStringPreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetStringSetPreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetSuSFSStatusUseCase
import com.hzzmonnet.aweyuki.domain.usecase.GetSuperUserAppGroupUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ImportAllowlistUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ImportProfileTemplatesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.InitializeApplicationUseCase
import com.hzzmonnet.aweyuki.domain.usecase.IsCatalogModuleCurrentUseCase
import com.hzzmonnet.aweyuki.domain.usecase.IsLateLoadModeUseCase
import com.hzzmonnet.aweyuki.domain.usecase.IsModuleUriAccessibleUseCase
import com.hzzmonnet.aweyuki.domain.usecase.IsNetworkAvailableUseCase
import com.hzzmonnet.aweyuki.domain.usecase.IsSoftRebootPreferredUseCase
import com.hzzmonnet.aweyuki.domain.usecase.IsSystemLanguageSettingsUseCase
import com.hzzmonnet.aweyuki.domain.usecase.LaunchSystemLanguageSettingsUseCase
import com.hzzmonnet.aweyuki.domain.usecase.LoadModuleIconUseCase
import com.hzzmonnet.aweyuki.domain.usecase.LoadSettingsPlatformUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveAssetDownloadUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveCatalogModulesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveDownloadUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveDynamicManagerStateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveInstalledModulesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveKernelFlashUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveModuleCatalogRefreshingUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveProfileTemplateOfflineUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveProfileTemplateRefreshingUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveProfileTemplatesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveRepositorySourcesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveStartupStateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveSulogStateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveSuperUserStateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ObserveUmountStateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.PeekOneshotScriptUseCase
import com.hzzmonnet.aweyuki.domain.usecase.PrepareScriptUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RebootUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshDynamicManagerUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshInstalledModulesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshModuleCatalogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshProfileTemplatesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshSulogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshSuperUsersUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RefreshUmountPathsUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ReleaseScriptUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RemoveModuleRepositoryUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RemovePreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RemoveUmountPathUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RenameModuleRepositoryUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ResolveModuleUpdateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.RunScriptUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SaveModuleActionLogUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SaveProfileTemplateUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SelectDynamicManagerUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetAppProfileUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetAppSepolicyUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetBooleanPreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetDefaultUmountModulesUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetKernelUmountEnabledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetManualDynamicManagerUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetModuleEnabledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetModuleRemovedUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetSelinuxHideEnabledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetShellSuAdbOnlyEnabledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetStringPreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetStringSetPreferenceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SetSuEnabledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.StartKernelFlashUseCase
import com.hzzmonnet.aweyuki.domain.usecase.SuSFSConfigUseCase
import com.hzzmonnet.aweyuki.domain.usecase.TakeModuleUriPermissionUseCase
import com.hzzmonnet.aweyuki.domain.usecase.TransliterateTextUseCase
import com.hzzmonnet.aweyuki.domain.usecase.UpdateAppearanceUseCase
import com.hzzmonnet.aweyuki.domain.usecase.UpdateCachedModuleEnabledUseCase
import com.hzzmonnet.aweyuki.domain.usecase.UpdatePlatformSettingUseCase
import com.hzzmonnet.aweyuki.domain.usecase.ValidateSepolicyUseCase
import com.hzzmonnet.aweyuki.ui.activity.util.ThemeUtils
import com.hzzmonnet.aweyuki.ui.component.ZipFileDetector
import com.hzzmonnet.aweyuki.ui.theme.BackgroundManager
import com.hzzmonnet.aweyuki.ui.theme.CardConfig
import com.hzzmonnet.aweyuki.ui.theme.ThemeConfig
import com.hzzmonnet.aweyuki.ui.util.Shortcut
import com.hzzmonnet.aweyuki.ui.viewmodel.AppProfileViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.DynamicManagerViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.ExecuteModuleActionViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.FlashViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.HomeViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.InstallViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.KernelFlashViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.MainIntentViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.ModuleDetailViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.ModuleRepoViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.ModuleViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.RepositorySourcesViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.ScriptRunViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.SettingsViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.SuSFSViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.SulogViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.SuperUserViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.TemplateEditorViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.TemplateViewModel
import com.hzzmonnet.aweyuki.ui.viewmodel.UmountManagerScreenViewModel
import com.hzzmonnet.aweyuki.ui.webui.MonetColorsProvider
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationScopeQualifier = named("applicationScope")

val coreModule = module {
    single<CoroutineScope>(applicationScopeQualifier) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    single {
        OkHttpClient.Builder()
            .cache(Cache(File(androidApplication().cacheDir, "okhttp"), 10L * 1024L * 1024L))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "AweYukiSU/${BuildConfig.VERSION_CODE}")
                        .header("Accept-Language", Locale.getDefault().toLanguageTag())
                        .build(),
                )
            }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
    single {
        val application = androidApplication()
        val iconSize = application.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        ImageLoader.Builder(application)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(iconSize, false, application))
            }
            .build()
    }
}

val repositoryModule = module {
    single { KsuCliRepository(androidApplication()) }
    singleOf(::CountRepository)
    singleOf(::InstalledPackageCache)
    singleOf(::AppIconDataSource)
    singleOf(::RootServiceRepository)
    singleOf(::InstalledPackageRepository)
    single {
        SuperUserRepository(
            application = get(),
            cache = get(),
            installedPackageRepository = get(),
            profileRepository = get(),
            applicationScope = get(applicationScopeQualifier),
        )
    }
    single {
        AppSettingsRepository(
            context = androidApplication(),
            applicationScope = get(applicationScopeQualifier),
        )
    }
    singleOf(::StartupRepository)
    single {
        ApplicationInitializationRepository(
            application = get(),
            imageLoader = get(),
            applicationScope = get(applicationScopeQualifier),
            flashRepository = get(),
            ksuCliRepository = get(),
        )
    }
    singleOf(::ManagerUpdateRepository)
    singleOf(::ApplicationControlRepository)
    singleOf(::DownloadRepository)
    single { FlashRepository(get(), get(applicationScopeQualifier), get(), get()) }
    singleOf(::KernelRepository)
    singleOf(::HomeRuntimeRepository)
    singleOf(::HomeStateRepository)
    singleOf(::NetworkStatusRepository)
    singleOf(::NetworkRequestRepository)
    singleOf(::DynamicManagerRepository)
    singleOf(::SulogRepository)
    singleOf(::BugreportRepository)
    singleOf(::UmountRepository)
    singleOf(::ModuleCatalogRepository)
    single { ModuleReleaseRepository(get(), get(applicationScopeQualifier)) }
    singleOf(::ModuleIconRepository)
    single { KernelSuRegistryRepository(get(), get(), get(), get()) }
    singleOf(::RepositorySourceStore)
    singleOf(::ModuleRepository)
    singleOf(::ModulePreferencesRepository)
    singleOf(::ModuleActionRepository)
    single { ScriptRepository(androidApplication(), get(), get(applicationScopeQualifier)) }
    singleOf(::WebResourceRepository)
    singleOf(::WebUiRepository)
    singleOf(::ModuleFileRepository)
    singleOf(::ProfileRepository)
    singleOf(::ProfileTemplateRepository)
    singleOf(::SuSFSConfigHelper)
    singleOf(::SuSFSRepository)
    singleOf(::MonetCompatColorSource)
    singleOf(::ThemeRepository)
    single {
        val themeRepository = get<ThemeRepository>()
        ThemeConfig(themeRepository::defaultSeedColor)
    }
    singleOf(::CardConfig)
    singleOf(::BackgroundManager)
    singleOf(::ThemeUtils)
    singleOf(::LocaleHelper)
    singleOf(::LocaleRepository)
    singleOf(::SettingsPlatformRepository)
    singleOf(::ShortcutRepository)
    singleOf(::Shortcut)
    singleOf(::MonetColorsProvider)
    singleOf(::ZipFileDetector)
    single { HanziToPinyin.create() } bind TextTransliterator::class
}

val useCaseModule = module {
    factoryOf(::InitializeApplicationUseCase)
    factoryOf(::GetHomeBasicInfoUseCase)
    factoryOf(::IsNetworkAvailableUseCase)
    factoryOf(::LoadSettingsPlatformUseCase)
    factoryOf(::UpdateAppearanceUseCase)
    factoryOf(::UpdatePlatformSettingUseCase)
    factoryOf(::GetPlatformFeatureStatusUseCase)
    factoryOf(::IsSoftRebootPreferredUseCase)
    factoryOf(::CheckManagerUpdateUseCase)
    factoryOf(::EnsureManagerInstalledUseCase)
    factoryOf(::RebootUseCase)
    factoryOf(::EnqueueDownloadUseCase)
    factoryOf(::EnqueueManagerUpdateUseCase)
    factoryOf(::ObserveDownloadUseCase)
    factoryOf(::ObserveAssetDownloadUseCase)
    factoryOf(::GetKernelStatusUseCase)
    factoryOf(::GetInstallEnvironmentUseCase)
    factoryOf(::ExecuteFlashOperationUseCase)
    factoryOf(::CheckFlashModuleMountUseCase)
    factoryOf(::GetManagerRuntimeInfoUseCase)
    factoryOf(::GetKernelFeatureSettingsUseCase)
    factoryOf(::SetSuEnabledUseCase)
    factoryOf(::SetKernelUmountEnabledUseCase)
    factoryOf(::SetShellSuAdbOnlyEnabledUseCase)
    factoryOf(::ConfigureSuLogUseCase)
    factoryOf(::SetSelinuxHideEnabledUseCase)
    factoryOf(::SetDefaultUmountModulesUseCase)
    factoryOf(::IsLateLoadModeUseCase)
    factoryOf(::GetAppProfileUseCase)
    factoryOf(::SetAppProfileUseCase)
    factoryOf(::GetAppSepolicyUseCase)
    factoryOf(::SetAppSepolicyUseCase)
    factoryOf(::ControlAppUseCase)
    factoryOf(::ValidateSepolicyUseCase)
    factoryOf(::GetDefaultUmountModulesUseCase)
    factoryOf(::GetSuSFSStatusUseCase)
    factoryOf(::SuSFSConfigUseCase)
    factoryOf(::ApplyLanguageUseCase)
    factoryOf(::IsSystemLanguageSettingsUseCase)
    factoryOf(::LaunchSystemLanguageSettingsUseCase)
    factoryOf(::GenerateBugreportUseCase)
    factoryOf(::ObserveStartupStateUseCase)
    factoryOf(::GetSuperUserAppGroupUseCase)
    factoryOf(::ObserveCatalogModulesUseCase)
    factoryOf(::ObserveModuleCatalogRefreshingUseCase)
    factoryOf(::RefreshModuleCatalogUseCase)
    factoryOf(::GetCatalogModuleUseCase)
    factoryOf(::IsCatalogModuleCurrentUseCase)
    factoryOf(::GetModuleReadmeUseCase)
    factoryOf(::GetModuleReleaseDetailsUseCase)
    factoryOf(::LoadModuleIconUseCase)
    factoryOf(::ResolveModuleUpdateUseCase)
    factoryOf(::GetModuleUpdateChangelogUseCase)
    factoryOf(::ObserveRepositorySourcesUseCase)
    factoryOf(::AddModuleRepositoryUseCase)
    factoryOf(::RenameModuleRepositoryUseCase)
    factoryOf(::RemoveModuleRepositoryUseCase)
    factoryOf(::ObserveProfileTemplatesUseCase)
    factoryOf(::ObserveProfileTemplateRefreshingUseCase)
    factoryOf(::ObserveProfileTemplateOfflineUseCase)
    factoryOf(::RefreshProfileTemplatesUseCase)
    factoryOf(::GetProfileTemplateUseCase)
    factoryOf(::SaveProfileTemplateUseCase)
    factoryOf(::DeleteProfileTemplateUseCase)
    factoryOf(::ImportProfileTemplatesUseCase)
    factoryOf(::ExportProfileTemplatesUseCase)
    factoryOf(::GetBooleanPreferenceUseCase)
    factoryOf(::SetBooleanPreferenceUseCase)
    factoryOf(::GetStringPreferenceUseCase)
    factoryOf(::SetStringPreferenceUseCase)
    factoryOf(::GetStringSetPreferenceUseCase)
    factoryOf(::SetStringSetPreferenceUseCase)
    factoryOf(::ObserveDynamicManagerStateUseCase)
    factoryOf(::RefreshDynamicManagerUseCase)
    factoryOf(::SelectDynamicManagerUseCase)
    factoryOf(::SetManualDynamicManagerUseCase)
    factoryOf(::ClearDynamicManagerUseCase)
    factoryOf(::ObserveSulogStateUseCase)
    factoryOf(::RefreshSulogUseCase)
    factoryOf(::EnableSulogUseCase)
    factoryOf(::CleanSulogUseCase)
    factoryOf(::ObserveUmountStateUseCase)
    factoryOf(::RefreshUmountPathsUseCase)
    factoryOf(::AddUmountPathUseCase)
    factoryOf(::RemoveUmountPathUseCase)
    factoryOf(::ObserveKernelFlashUseCase)
    factoryOf(::StartKernelFlashUseCase)
    factoryOf(::RemovePreferenceUseCase)
    factoryOf(::ObserveSuperUserStateUseCase)
    factoryOf(::RefreshSuperUsersUseCase)
    factoryOf(::BackupAllowlistUseCase)
    factoryOf(::ImportAllowlistUseCase)
    factoryOf(::FetchRemoteTextUseCase)
    factoryOf(::IsModuleUriAccessibleUseCase)
    factoryOf(::TakeModuleUriPermissionUseCase)
    factoryOf(::ExtractModuleNameUseCase)
    factoryOf(::ExtractModuleIdUseCase)
    factoryOf(::ObserveInstalledModulesUseCase)
    factoryOf(::RefreshInstalledModulesUseCase)
    factoryOf(::CalculateInstalledModuleSizeUseCase)
    factoryOf(::UpdateCachedModuleEnabledUseCase)
    factoryOf(::ExecuteModuleActionUseCase)
    factoryOf(::PeekOneshotScriptUseCase)
    factoryOf(::PrepareScriptUseCase)
    factoryOf(::ReleaseScriptUseCase)
    factoryOf(::RunScriptUseCase)
    factoryOf(::GetLastScriptRunUseCase)
    factoryOf(::SaveModuleActionLogUseCase)
    factoryOf(::SetModuleEnabledUseCase)
    factoryOf(::SetModuleRemovedUseCase)
    factoryOf(::TransliterateTextUseCase)
}

val viewModelModule = module {
    viewModel { parameters -> ScriptRunViewModel(parameters[0], get(), get(), get(), get(), get()) }
    viewModel { parameters ->
        AppProfileViewModel(
            uid = parameters[0],
            packageName = parameters[1],
            getAppGroup = get(),
            getProfile = get(),
            getDefaultUmountModules = get(),
            setProfile = get(),
            getSepolicy = get(),
            setSepolicy = get(),
            controlApp = get(),
            validateSepolicy = get(),
        )
    }
    viewModelOf(::HomeViewModel)
    viewModelOf(::InstallViewModel)
    viewModelOf(::MainIntentViewModel)
    viewModelOf(::KernelFlashViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ModuleViewModel)
    viewModelOf(::SuperUserViewModel)
    viewModelOf(::SuSFSViewModel)
    viewModel { parameters -> ModuleRepoViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), parameters.getOrNull<String>()) }
    viewModel { parameters -> ModuleDetailViewModel(parameters[0], parameters[1], get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::RepositorySourcesViewModel)
    viewModelOf(::TemplateViewModel)
    viewModel { parameters ->
        TemplateEditorViewModel(
            templateId = parameters[0],
            readOnly = parameters[1],
            isCreation = parameters[2],
            getTemplate = get(),
            saveTemplate = get(),
            deleteTemplate = get(),
        )
    }
    viewModelOf(::SulogViewModel)
    viewModelOf(::DynamicManagerViewModel)
    viewModelOf(::FlashViewModel)
    viewModelOf(::UmountManagerScreenViewModel)
    viewModel { parameters ->
        ExecuteModuleActionViewModel(
            moduleId = parameters[0],
            executeModuleAction = get(),
            saveModuleActionLog = get(),
        )
    }
}

val appModules = listOf(coreModule, repositoryModule, useCaseModule, viewModelModule)
