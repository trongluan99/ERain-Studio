package com.itg.iaumodule

import android.app.Activity
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.ConsentStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ITGUpdateManager(
    val activity: Activity,
    val requestCode: Int,
    val iUpdateInstanceCallback: IUpdateInstanceCallback
) {
    companion object {
        var canRequestAds: Boolean = false
    }

    private lateinit var consentInformation: ConsentInformation

    private var appUpdateManager: AppUpdateManager? = null

    fun checkUpdateAvailable(): ITGUpdateManager {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity)
        }
        checkForUpdates(activity, iUpdateInstanceCallback)
        return this
    }

    fun checkBelowGeoEEA(isDebug: Boolean): ITGUpdateManager {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity)
        }
        checkGeographyEEA(activity, isDebug)
        return this
    }

    private fun checkForUpdates(
        activity: Activity, iUpdateInstanceCallback: IUpdateInstanceCallback
    ) {
        appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager?.appUpdateInfo!!
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Log.d("InAppUpdateManager", "checkForUpdates:  Success")
            when (iUpdateInstanceCallback.updateAvailableListener(appUpdateInfo)) {
                AppUpdateType.IMMEDIATE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        Log.d("InAppUpdateManager", "checkForUpdates:  IMMEDIATE")
                        appUpdateManager?.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.IMMEDIATE, activity, requestCode
                        )
                    }
                }


                AppUpdateType.FLEXIBLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        Log.d("InAppUpdateManager", "checkForUpdates:  FLEXIBLE")

                        appUpdateManager?.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE, activity, requestCode
                        )
                    }
                }
            }

        }

        appUpdateInfoTask.addOnFailureListener {
            Log.d("InAppUpdateManager", "checkForUpdates: failed $it")
        }
        val listener = InstallStateUpdatedListener { state ->
            // (Optional) Provide a download progress bar.
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                // Show update progress bar.

                Log.v(
                    "InAppUpdateManager",
                    "bytesDownloaded: ${bytesDownloaded} / totalBytesToDownload: ${totalBytesToDownload}"
                )

            }
            if (state.installStatus() == InstallStatus.INSTALLED) {
                Log.v("InAppUpdateManager", "state.installStatus() == InstallStatus.INSTALLED")
            }

            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Log.v("InAppUpdateManager", "state.installStatus() == InstallStatus.DOWNLOADED")

                appUpdateManager?.completeUpdate()
            }
            // Log state or install the update.
        }

        appUpdateManager?.registerListener(listener)
    }

    private fun checkGeographyEEA(activity: Activity, isDebug: Boolean) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .addTestDeviceHashedId("ED3576D8FCF2F8C52AD8E98B4CFA4005")
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA).build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(if (isDebug) debugSettings else null) // Remove for production build
            .setTagForUnderAgeOfConsent(false).build()
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        Log.v("InAppUpdateManager", "canRequestAds 111 :${consentInformation.canRequestAds()}")
        if (isDebug) {
            consentInformation.reset() // Remove for Production build
        }

        UserMessagingPlatform.loadConsentForm(activity, {
            Log.v("InAppUpdateManager", "loadConsentForm :Success")

        }, {
            Log.v("InAppUpdateManager", "loadConsentForm :Failure")
        })
        consentInformation.requestConsentInfoUpdate(activity, params, {
            when(consentInformation.consentStatus){
                ConsentStatus.REQUIRED ->{
                    Log.v("InAppUpdateManager", "ConsentStatus.REQUIRED")
                }
                ConsentStatus.OBTAINED ->{
                    Log.v("InAppUpdateManager", "ConsentStatus.OBTAINED")
                }
            }
            if (consentInformation.consentStatus == ConsentStatus.REQUIRED)
            {

            }else{
                Log.v("InAppUpdateManager", "ConsentStatus ${consentInformation.consentStatus}")
            }


            UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                activity
            ) { loadAndShowError ->
                // Consent gathering failed.
                Log.w(
                    "InAppUpdateManager", "loadAndShowError:" + String.format(
                        "%s: %s", loadAndShowError?.errorCode, loadAndShowError?.message
                    )
                )

                // Consent has been gathered.
                iUpdateInstanceCallback.resultConsentForm(consentInformation.canRequestAds())
                Log.v(
                    "InAppUpdateManager", "ConsentStatus:${
                        consentInformation.consentStatus
                    }"
                )

                if (consentInformation.canRequestAds()) {

                    canRequestAds = true
                    initializeMobileAdsSdk()
                }
            }
        }, { requestConsentError ->
            // Consent gathering failed.
            Log.w(
                "InAppUpdateManager", "requestConsentError: " + String.format(
                    "%s: %s", requestConsentError.errorCode, requestConsentError.message
                )
            )
        })
        Log.v("InAppUpdateManager", "canRequestAds 222 :${consentInformation.canRequestAds()}")
        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        if (consentInformation.canRequestAds()) {
            canRequestAds = true
            initializeMobileAdsSdk()
        }
    }

    /**
     * Initializes the Mobile Ads SDK
     * if (isMobileAdsInitializeCalled.getAndSet(true)) {
     *       return
     *     }
     * MobileAds.initialize(this)
     *
     */
    private fun initializeMobileAdsSdk() {
        iUpdateInstanceCallback.initializeMobileAdsSdk()
    }


}




