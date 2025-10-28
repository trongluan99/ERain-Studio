package com.itg.iaumodule

import android.content.Context
import android.preference.PreferenceManager
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform


object ITGAdConsent {

    private var canPersonalized: Boolean = true
    private var consentInformation: ConsentInformation? = null

    fun getCountryCode(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.networkCountryIso.uppercase()
    }

    fun listEEACountry(): List<String> {
        return listOf(
            "AT",
            "BE",
            "BG",
            "HR",
            "CY",
            "CZ",
            "DK",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "HU",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "PL",
            "PT",
            "RO",
            "SK",
            "SI",
            "ES",
            "SE"
        )
    }

    fun listUKCountry(): List<String> {
        return listOf("GB", "GG", "IM", "JE")
    }

    fun listAdConsentCountry(): List<String> {
        return listEEACountry() + listUKCountry()
    }

    fun isAdConsentCountry(context: Context): Boolean {
        return listAdConsentCountry().contains(getCountryCode(context))
    }

//    fun showConsent(callback: IAdConsentCallBack) {
//        loadConsent(callback)
//    }

    fun loadAndShowConsent(isShowDialog: Boolean,callback: IAdConsentCallBack, ) {

        consentInformation =
            UserMessagingPlatform.getConsentInformation(callback.getCurrentActivity())

        // Set tag for underage of consent. false means users are not underage.
        val params = if (callback.isDebug()) {

            val debugSettings = ConsentDebugSettings.Builder(callback.getCurrentActivity())
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(callback.testDeviceID()).setForceTesting(true)
                .build()

            ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(callback.isUnderAgeAd())
                .setConsentDebugSettings(debugSettings).build()

        } else {
            ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(callback.isUnderAgeAd())
                .build()
        }
//        if (callback.isDebug()) {
//            consentInformation?.reset() // Remove for Production build
//        }

        consentInformation?.requestConsentInfoUpdate(callback.getCurrentActivity(), params, {
            // The consent information state was updated.
            // You are now ready to check if a form is available.


            Log.v("ITGAdConsent", "requestConsentInfoUpdate success")



            if (consentInformation?.isConsentFormAvailable == true) {
                loadForm(consentInformation!!, isShowDialog, callback)
            } else {
                callback.onNotUsingAdConsent()
            }
        }, { formError ->
            // Handle the error.
            callback.onConsentError(formError)
        })
    }


    fun showDialogConsent(callback: IAdConsentCallBack) {
        if (consentInformation?.isConsentFormAvailable == true) {
            loadForm(consentInformation!!, true, callback)
        } else {
            callback.onNotUsingAdConsent()
        }
    }

    private fun loadForm(
        consentInformation: ConsentInformation,
        isShowDialog: Boolean,
        callback: IAdConsentCallBack
    ) {
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(callback.getCurrentActivity(), { consentForm ->
            if (!isShowDialog){
                callback.onConsentStatus(consentInformation.consentStatus)
            }
            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                if (isShowDialog) {
                    callback.onRequestShowDialog()
                    consentForm.show(callback.getCurrentActivity()) { formError ->
//                        if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
//                            // App can start requesting ads.
//
//                        }
                        canPersonalized = canShowPersonalizedAds(callback.getCurrentActivity())
                        callback.onConsentSuccess(canPersonalized)
//                    loadForm(consentInformation, callback)


                    }
                }
            } else if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED) {
                callback.onNotUsingAdConsent()
            }
        }, { formError ->
            // Handle the error.
            callback.onConsentError(formError)
        })
    }

    fun canShowPersonalizedAds(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val purposeConsent: String = prefs.getString("IABTCF_PurposeConsents", "")!!
        val vendorConsent: String = prefs.getString("IABTCF_VendorConsents", "")!!
        val vendorLI: String = prefs.getString("IABTCF_VendorLegitimateInterests", "")!!
        val purposeLI: String = prefs.getString("IABTCF_PurposeLegitimateInterests", "")!!
        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, googleId)
        val indexes: MutableList<Int> = ArrayList()
        indexes.add(1)
        indexes.add(3)
        indexes.add(4)
        val indexesLI: MutableList<Int> = ArrayList()
        indexesLI.add(2)
        indexesLI.add(7)
        indexesLI.add(9)
        indexesLI.add(10)
        return (hasConsentFor(
            indexes, purposeConsent, hasGoogleVendorConsent
        ) && hasConsentOrLegitimateInterestFor(
            indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI
        ))
    }

    private fun hasConsentFor(
        indexes: List<Int>, purposeConsent: String, hasVendorConsent: Boolean
    ): Boolean {
        for (p in indexes) {
            if (!hasAttribute(purposeConsent, p)) {
                Log.e("ITGAdConsent", "hasConsentFor: denied for purpose #$p")
                return false
            }
        }
        return hasVendorConsent
    }


    private fun hasAttribute(input: String?, index: Int): Boolean {
        return if (input == null) false else input.length >= index && input[index - 1] == '1'
    }

    private fun hasConsentOrLegitimateInterestFor(
        indexes: List<Int>,
        purposeConsent: String,
        purposeLI: String,
        hasVendorConsent: Boolean,
        hasVendorLI: Boolean
    ): Boolean {
        for (p in indexes) {
            val purposeAndVendorLI = hasAttribute(purposeLI, p) && hasVendorLI
            val purposeConsentAndVendorConsent = hasAttribute(purposeConsent, p) && hasVendorConsent
            val isOk = purposeAndVendorLI || purposeConsentAndVendorConsent
            if (!isOk) {
                Log.e("ITGAdConsent", "hasConsentOrLegitimateInterestFor: denied for #$p")
                return false
            }
        }
        return true
    }

    fun resetConsentDialog() {
        consentInformation?.reset()
    }

    fun isPersonalizedAd(): Boolean {
        return canPersonalized
    }

}