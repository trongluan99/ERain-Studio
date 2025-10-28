package com.ads.module.ads.wrapper;

import com.applovin.mediation.MaxReward;
import com.google.android.gms.ads.rewarded.RewardItem;

public class ApRewardItem {

    private RewardItem admobRewardItem;
    private MaxReward maxRewardItem;

    public ApRewardItem(MaxReward maxRewardItem) {
        this.maxRewardItem = maxRewardItem;
    }

    public ApRewardItem(RewardItem admobRewardItem) {
        this.admobRewardItem = admobRewardItem;
    }

    public RewardItem getAdmobRewardItem() {
        return admobRewardItem;
    }

    public void setAdmobRewardItem(RewardItem admobRewardItem) {
        this.admobRewardItem = admobRewardItem;
    }

    public MaxReward getMaxRewardItem() {
        return maxRewardItem;
    }

    public void setMaxRewardItem(MaxReward maxRewardItem) {
        this.maxRewardItem = maxRewardItem;
    }
}
