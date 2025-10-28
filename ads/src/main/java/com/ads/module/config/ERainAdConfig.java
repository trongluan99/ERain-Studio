package com.ads.module.config;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

public class ERainAdConfig {

    //switch mediation use for app
    public static final int PROVIDER_ADMOB = 0;
    public static final int PROVIDER_MAX = 1;

    public static final String ENVIRONMENT_DEVELOP = "develop";
    public static final String ENVIRONMENT_PRODUCTION = "production";

    public static final String DEFAULT_TOKEN_FACEBOOK_SDK = "client_token";
    public static String ADJUST_TOKEN_TIKTOK = "client_token_adjust_tiktok";

    private boolean isVariantDev = false;

    /**
     * adjustConfig enable adjust and setup adjust token
     */
    private AdjustConfig adjustConfig;

    /**
     * eventNamePurchase push event to adjust when user purchased
     */
    private String eventNamePurchase = "";
    private String idAdResume;
    private List<String> listDeviceTest = new ArrayList();

    private Application application;
    private boolean enableAdResume = false;
    private String facebookClientToken = DEFAULT_TOKEN_FACEBOOK_SDK;

    private String adjustTokenTiktok;

    /**
     * intervalInterstitialAd: time between two interstitial ad impressions
     * unit: seconds
     */
    private int intervalInterstitialAd = 0;

    public ERainAdConfig(Application application) {
        this.application = application;
    }

    public ERainAdConfig(Application application, String environment) {
        this.isVariantDev = environment.equals(ENVIRONMENT_DEVELOP);
        this.application = application;
    }

    /**
     * @param isVariantDev
     */
    @Deprecated
    public void setVariant(Boolean isVariantDev) {
        this.isVariantDev = isVariantDev;
    }

    public void setEnvironment(String environment) {
        this.isVariantDev = environment.equals(ENVIRONMENT_DEVELOP);
    }

    public AdjustConfig getAdjustConfig() {
        return adjustConfig;
    }

    public void setAdjustConfig(AdjustConfig adjustConfig) {
        this.adjustConfig = adjustConfig;
    }

    public String getEventNamePurchase() {
        return eventNamePurchase;
    }

    public Application getApplication() {
        return application;
    }


    public Boolean isVariantDev() {
        return isVariantDev;
    }


    public String getIdAdResume() {
        return idAdResume;
    }

    public List<String> getListDeviceTest() {
        return listDeviceTest;
    }

    public void setListDeviceTest(List<String> listDeviceTest) {
        this.listDeviceTest = listDeviceTest;
    }


    public void setIdAdResume(String idAdResume) {
        this.idAdResume = idAdResume;
        enableAdResume = true;
    }

    public Boolean isEnableAdResume() {
        return enableAdResume;
    }


    public Boolean isEnableAdjust() {
        if (adjustConfig == null)
            return false;
        return adjustConfig.isEnableAdjust();
    }

    public int getIntervalInterstitialAd() {
        return intervalInterstitialAd;
    }

    public void setIntervalInterstitialAd(int intervalInterstitialAd) {
        this.intervalInterstitialAd = intervalInterstitialAd;
    }

    public void setFacebookClientToken(String token) {
        this.facebookClientToken = token;
    }

    public String getFacebookClientToken() {
        return this.facebookClientToken;
    }

    public String getAdjustTokenTiktok() {
        return adjustTokenTiktok;
    }

    public void setAdjustTokenTiktok(String adjustTokenTiktok) {
        ADJUST_TOKEN_TIKTOK = adjustTokenTiktok;
        this.adjustTokenTiktok = adjustTokenTiktok;
    }
}
