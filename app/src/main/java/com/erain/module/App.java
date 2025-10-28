package com.erain.module;

import com.ads.module.admob.Admob;
import com.ads.module.admob.AppOpenManager;
import com.ads.module.ads.ERainAd;
import com.ads.module.application.AdsMultiDexApplication;
import com.ads.module.config.AdjustConfig;
import com.ads.module.config.ERainAdConfig;

public class App extends AdsMultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        initAds();
    }

    private void initAds() {
        String environment = BuildConfig.DEBUG ? ERainAdConfig.ENVIRONMENT_DEVELOP : ERainAdConfig.ENVIRONMENT_PRODUCTION;
        mERainAdConfig = new ERainAdConfig(this, environment);

        AdjustConfig adjustConfig = new AdjustConfig(true, getString(R.string.adjust_token));
        mERainAdConfig.setAdjustConfig(adjustConfig);
        mERainAdConfig.setFacebookClientToken(getString(R.string.facebook_client_token));
        mERainAdConfig.setAdjustTokenTiktok(getString(R.string.tiktok_token));
        mERainAdConfig.setIdAdResume(BuildConfig.ad_appopen_resume);

        ERainAd.getInstance().init(this, mERainAdConfig);
        Admob.getInstance().setDisableAdResumeWhenClickAds(true);
        Admob.getInstance().setOpenActivityAfterShowInterAds(true);
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity.class);
    }
}
