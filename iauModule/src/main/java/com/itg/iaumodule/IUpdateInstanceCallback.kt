package com.itg.iaumodule

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType

interface IUpdateInstanceCallback {
    fun updateAvailableListener(updateAvailability: AppUpdateInfo) : Int{
        return  AppUpdateType.FLEXIBLE
    }
    fun initializeMobileAdsSdk(){

    }
    fun resultConsentForm(canRequestAds: Boolean){

    }
}