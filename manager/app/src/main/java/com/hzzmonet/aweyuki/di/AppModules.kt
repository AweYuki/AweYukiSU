package com.hzzmonet.aweyuki.di

import coil.ImageLoader
import com.hzzmonet.aweyuki.BuildConfig
import com.hzzmonet.aweyuki.data.AppSettingsRepository
import com.hzzmonet.aweyuki.data.application.ApplicationControlRepository
import com.hzzmonet.aweyuki.data.application.DynamicManagerRepository
import com.hzzmonet.aweyuki.data.count.CountRepository
import com.hzzmonet.aweyuki.data.download.DownloadRepository
import com.hzzmonet.aweyuki.data.file.ModuleFileRepository
import com.hzzmonet.aweyuki.data.flash.FlashRepository
import com.hzzmonet.aweyuki.data.kernel.KernelRepository
import com.hzzmonet.aweyuki.data.kernel.UmountRepository
import com.hzzmonet.aweyuki.data.logging.BugreportRepository
import com.hzzmonet.aweyuki.data.logging.SulogRepository
import com.hzzmonet.aweyuki.data.module.ModuleActionRepository
import com.hzzmonet.aweyuki.data.module.ModuleCatalogRepository
import com.hzzmonet.aweyuki.data.module.ModuleIconRepository
import com.hzzmonet.aweyuki.data.module.ModulePreferencesRepository
import com.hzzmonet.aweyuki.data.module.ModuleReleaseRepository
import com.hzzmonet.aweyuki.data.module.ModuleRepository
import com.hzzmonet.aweyuki.data.module.RepositorySourceStore
import com.hzzmonet.aweyuki.data.module.registry.KernelSuRegistryRepository
import com.hzzmonet.aweyuki.data.network.NetworkRequestRepository
import com.hzzmonet.aweyuki.data.network.NetworkStatusRepository
import com.hzzmonet.aweyuki.data.network.WebResourceRepository
import com.hzzmonet.aweyuki.data.packageinfo.AppIconDataSource
import com.hzzmonet.aweyuki.data.packageinfo.InstalledPackageCache
import com.hzzmonet.aweyuki.data.packageinfo.InstalledPackageRepository
import com.hzzmonet.aweyuki.data.packageinfo.RootServiceRepository
import com.hzzmonet.aweyuki.data.packageinfo.SuperUserRepository
import com.hzzmonet.aweyuki.data.profile.ProfileRepository
import com.hzzmonet.aweyuki.data.profile.ProfileTemplateRepository
import com.hzzmonet.aweyuki.data.script.ScriptRepository
import com.hzzmonet.aweyuki.data.settings.LocaleHelper
import com.hzzmonet.aweyuki.data.settings.LocaleRepository
import com.hzzmonet.aweyuki.data.settings.SettingsPlatformRepository
import com.hzzmonet.aweyuki.data.shell.KsuCliRepository
import com.hzzmonet.aweyuki.data.shell.ShortcutRepository
import com.hzzmonet.aweyuki.data.startup.ApplicationInitializationRepository
import com.hzzmonet.aweyuki.data.startup.StartupRepository
import com.hzzmonet.aweyuki.data.susfs.SuSFSConfigHelper
import com.hzzmonet.aweyuki.data.susfs.SuSFSRepository
import com.hzzmonet.aweyuki.data.system.HomeRuntimeRepository
import com.hzzmonet.aweyuki.data.system.HomeStateRepository
import com.hzzmonet.aweyuki.data.text.HanziToPinyin
import com.hzzmonet.aweyuki.data.theme.MonetCompatColorSource
import com.hzzmonet.aweyuki.data.theme.ThemeRepository
import com.hzzmonet.aweyuki.data.update.ManagerUpdateRepository
import com.hzzmonet.aweyuki.data.webui.WebUiRepository
import com.hzzmonet.aweyuki.domain.text.TextTransliterator
import com.hzzmonet.aweyuki.domain.usecase.AddModuleRepositoryUseCase
import com.hzzmonet.aweyuki.domain.usecase.AddUmountPathUseCase
import com.hzzmonet.aweyuki.domain.usecase.ApplyLanguageUseCase
import com.hzzmonet.aweyuki.domain.usecase.BackupAllowlistUseCase
import com.hzzmonet.aweyuki.domain.usecase.CalculateInstalledModuleSizeUseCase
import com.hzzmonet.aweyuki.domain.usecase.CheckFlashModuleMountUseCase
import com.hzzmonet.aweyuki.domain.usecase.CheckManagerUpdateUseCase
import com.hzzmonet.aweyuki.domain.usecase.CleanSulogUseCase
import com.hzzmonet.aweyuki.domain.usecase.ClearDynamicManagerUseCase
import com.hzzmonet.aweyuki.domain.usecase.ConfigureSuLogUseCase
import com.hzzmonet.aweyuki.domain.usecase.ControlAppUseCase
import com.hzzmonet.aweyuki.domain.usecase.DeleteProfileTemplateUseCase
import com.hzzmonet.aweyuki.domain.usecase.EnableSulogUseCase
import com.hzzmonet.aweyuki.domain.usecase.EnqueueDownloadUseCase
import com.hzzmonet.aweyuki.domain.usecase.EnqueueManagerUpdateUseCase
import com.hzzmonet.aweyuki.domain.usecase.EnsureManagerInstalledUseCase
import com.hzzmonet.aweyuki.domain.usecase.ExecuteFlashOperationUseCase
import com.hzzmonet.aweyuki.domain.usecase.ExecuteModuleActionUseCase
import com.hzzmonet.aweyuki.domain.usecase.ExportProfileTemplatesUseCase
import com.hzzmonet.aweyuki.domain.usecase.ExtractModuleIdUseCase
import com.hzzmonet.aweyuki.domain.usecase.ExtractModuleNameUseCase
import com.hzzmonet.aweyuki.domain.usecase.FetchRemoteTextUseCase
import com.hzzmonet.aweyuki.domain.usecase.GenerateBugreportUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetAppProfileUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetAppSepolicyUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetBooleanPreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetCatalogModuleUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetDefaultUmountModulesUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetHomeBasicInfoUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetInstallEnvironmentUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetKernelFeatureSettingsUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetKernelStatusUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetLastScriptRunUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetManagerRuntimeInfoUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetModuleReadmeUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetModuleReleaseDetailsUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetModuleUpdateChangelogUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetPlatformFeatureStatusUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetProfileTemplateUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetStringPreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetStringSetPreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetSuSFSStatusUseCase
import com.hzzmonet.aweyuki.domain.usecase.GetSuperUserAppGroupUseCase
import com.hzzmonet.aweyuki.domain.usecase.ImportAllowlistUseCase
import com.hzzmonet.aweyuki.domain.usecase.ImportProfileTemplatesUseCase
import com.hzzmonet.aweyuki.domain.usecase.InitializeApplicationUseCase
import com.hzzmonet.aweyuki.domain.usecase.IsCatalogModuleCurrentUseCase
import com.hzzmonet.aweyuki.domain.usecase.IsLateLoadModeUseCase
import com.hzzmonet.aweyuki.domain.usecase.IsModuleUriAccessibleUseCase
import com.hzzmonet.aweyuki.domain.usecase.IsNetworkAvailableUseCase
import com.hzzmonet.aweyuki.domain.usecase.IsSoftRebootPreferredUseCase
import com.hzzmonet.aweyuki.domain.usecase.IsSystemLanguageSettingsUseCase
import com.hzzmonet.aweyuki.domain.usecase.LaunchSystemLanguageSettingsUseCase
import com.hzzmonet.aweyuki.domain.usecase.LoadModuleIconUseCase
import com.hzzmonet.aweyuki.domain.usecase.LoadSettingsPlatformUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveAssetDownloadUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveCatalogModulesUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveDownloadUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveDynamicManagerStateUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveInstalledModulesUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveKernelFlashUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveModuleCatalogRefreshingUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveProfileTemplateOfflineUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveProfileTemplateRefreshingUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveProfileTemplatesUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveRepositorySourcesUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveStartupStateUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveSulogStateUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveSuperUserStateUseCase
import com.hzzmonet.aweyuki.domain.usecase.ObserveUmountStateUseCase
import com.hzzmonet.aweyuki.domain.usecase.PeekOneshotScriptUseCase
import com.hzzmonet.aweyuki.domain.usecase.PrepareScriptUseCase
import com.hzzmonet.aweyuki.domain.usecase.RebootUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshDynamicManagerUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshInstalledModulesUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshModuleCatalogUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshProfileTemplatesUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshSulogUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshSuperUsersUseCase
import com.hzzmonet.aweyuki.domain.usecase.RefreshUmountPathsUseCase
import com.hzzmonet.aweyuki.domain.usecase.ReleaseScriptUseCase
import com.hzzmonet.aweyuki.domain.usecase.RemoveModuleRepositoryUseCase
import com.hzzmonet.aweyuki.domain.usecase.RemovePreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.RemoveUmountPathUseCase
import com.hzzmonet.aweyuki.domain.usecase.RenameModuleRepositoryUseCase
import com.hzzmonet.aweyuki.domain.usecase.ResolveModuleUpdateUseCase
import com.hzzmonet.aweyuki.domain.usecase.RunScriptUseCase
import com.hzzmonet.aweyuki.domain.usecase.SaveModuleActionLogUseCase
import com.hzzmonet.aweyuki.domain.usecase.SaveProfileTemplateUseCase
import com.hzzmonet.aweyuki.domain.usecase.SelectDynamicManagerUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetAppProfileUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetAppSepolicyUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetBooleanPreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetDefaultUmountModulesUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetKernelUmountEnabledUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetManualDynamicManagerUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetModuleEnabledUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetModuleRemovedUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetSelinuxHideEnabledUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetShellSuAdbOnlyEnabledUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetStringPreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetStringSetPreferenceUseCase
import com.hzzmonet.aweyuki.domain.usecase.SetSuEnabledUseCase
import com.hzzmonet.aweyuki.domain.usecase.StartKernelFlashUseCase
import com.hzzmonet.aweyuki.domain.usecase.SuSFSConfigUseCase
import com.hzzmonet.aweyuki.domain.usecase.TakeModuleUriPermissionUseCase
import com.hzzmonet.aweyuki.domain.usecase.TransliterateTextUseCase
import com.hzzmonet.aweyuki.domain.usecase.UpdateAppearanceUseCase
import com.hzzmonet.aweyuki.domain.usecase.UpdateCachedModuleEnabledUseCase
import com.hzzmonet.aweyuki.domain.usecase.UpdatePlatformSettingUseCase
import com.hzzmonet.aweyuki.domain.usecase.ValidateSepolicyUseCase
import com.hzzmonet.aweyuki.ui.activity.util.ThemeUtils
import com.hzzmonet.aweyuki.ui.component.ZipFileDetector
import com.hzzmonet.aweyuki.ui.theme.BackgroundManager
import com.hzzmonet.aweyuki.ui.theme.CardConfig
import com.hzzmonet.aweyuki.ui.theme.ThemeConfig
import com.hzzmonet.aweyuki.ui.util.Shortcut
import com.hzzmonet.aweyuki.ui.viewmodel.AppProfileViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.DynamicManagerViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.ExecuteModuleActionViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.FlashViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.HomeViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.InstallViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.KernelFlashViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.MainIntentViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.ModuleDetailViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.ModuleRepoViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.ModuleViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.RepositorySourcesViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.ScriptRunViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.SettingsViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.SuSFSViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.SulogViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.SuperUserViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.TemplateEditorViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.TemplateViewModel
import com.hzzmonet.aweyuki.ui.viewmodel.UmountManagerScreenViewModel
import com.hzzmonet.aweyuki.ui.webui.MonetColorsProvider
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
