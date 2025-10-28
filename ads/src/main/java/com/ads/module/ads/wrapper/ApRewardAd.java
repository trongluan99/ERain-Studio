package com.ads.module.ads.wrapper;

import com.applovin.mediation.ads.MaxRewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;

public class ApRewardAd extends ApAdBase {
    private RewardedAd admobReward;
    private RewardedInterstitialAd admobRewardInter;
    private MaxRewardedAd maxReward;

    public ApRewardAd() {
    }

    public ApRewardAd(StatusAd status) {
        super(status);
    }

    public void setAdmobReward(RewardedAd admobReward) {
        this.admobReward = admobReward;
        status = StatusAd.AD_LOADED;
    }

    public void setAdmobReward(RewardedInterstitialAd admobRewardInter) {
        this.admobRewardInter = admobRewardInter;
    }

    public void setMaxReward(MaxRewardedAd maxReward) {
        this.maxReward = maxReward;
        status = StatusAd.AD_LOADED;
    }

    public ApRewardAd(MaxRewardedAd maxReward) {
        this.maxReward = maxReward;
        status = StatusAd.AD_LOADED;
    }

    public ApRewardAd(RewardedInterstitialAd admobRewardInter) {
        this.admobRewardInter = admobRewardInter;
        status = StatusAd.AD_LOADED;
    }

    public ApRewardAd(RewardedAd admobReward) {
        this.admobReward = admobReward;
        status = StatusAd.AD_LOADED;
    }


    public RewardedAd getAdmobReward() {
        return admobReward;
    }

    public RewardedInterstitialAd getAdmobRewardInter() {
        return admobRewardInter;
    }

    public MaxRewardedAd getMaxReward() {
        return maxReward;
    }

    /**
     * Clean reward when shown
     */
    public void clean() {
        maxReward = null;
        admobReward = null;
        admobRewardInter = null;
    }

    @Override
    public boolean isReady() {
        return admobReward != null || admobRewardInter != null || maxReward != null && maxReward.isReady();
    }

    public boolean isRewardInterstitial() {
        return admobRewardInter != null;
    }
}
