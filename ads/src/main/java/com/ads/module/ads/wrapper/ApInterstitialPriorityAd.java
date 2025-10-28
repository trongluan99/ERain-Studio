package com.ads.module.ads.wrapper;

public class ApInterstitialPriorityAd {
    private String high1PriorityId;
    private String high2PriorityId;
    private String high3PriorityId;
    private String normalPriorityId;
    private ApInterstitialAd high1PriorityInterstitialAd;
    private ApInterstitialAd high2PriorityInterstitialAd;
    private ApInterstitialAd high3PriorityInterstitialAd;
    private ApInterstitialAd normalPriorityInterstitialAd;

    public ApInterstitialPriorityAd(String high1PriorityId, String high2PriorityId, String high3PriorityId, String normalPriorityId) {
        this.high1PriorityId = high1PriorityId;
        this.high2PriorityId = high2PriorityId;
        this.high3PriorityId = high3PriorityId;
        this.normalPriorityId = normalPriorityId;
        if (!this.high1PriorityId.isEmpty() && this.high1PriorityInterstitialAd == null) {
            this.high1PriorityInterstitialAd = new ApInterstitialAd();
        }
        if (!this.high2PriorityId.isEmpty() && this.high2PriorityInterstitialAd == null) {
            this.high2PriorityInterstitialAd = new ApInterstitialAd();
        }
        if (!this.high3PriorityId.isEmpty() && this.high3PriorityInterstitialAd == null) {
            this.high3PriorityInterstitialAd = new ApInterstitialAd();
        }
        if (!this.normalPriorityId.isEmpty() && this.normalPriorityInterstitialAd == null) {
            this.normalPriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public void setHigh1PriorityId(String high1PriorityId) {
        this.high1PriorityId = high1PriorityId;
        if (!this.high1PriorityId.isEmpty() && this.high1PriorityInterstitialAd == null) {
            this.high1PriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public void setHigh2PriorityId(String high2PriorityId) {
        this.high2PriorityId = high2PriorityId;
        if (!this.high2PriorityId.isEmpty() && this.high2PriorityInterstitialAd == null) {
            this.high2PriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public void setHigh3PriorityId(String high3PriorityId) {
        this.high3PriorityId = high3PriorityId;
        if (!this.high3PriorityId.isEmpty() && this.high3PriorityInterstitialAd == null) {
            this.high3PriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public void setNormalPriorityId(String normalPriorityId) {
        this.normalPriorityId = normalPriorityId;
        if (!this.normalPriorityId.isEmpty() && this.normalPriorityInterstitialAd == null) {
            this.normalPriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public String getHigh1PriorityId() {
        return high1PriorityId;
    }

    public ApInterstitialAd getHigh1PriorityInterstitialAd() {
        return high1PriorityInterstitialAd;
    }

    public String getHigh2PriorityId() {
        return high2PriorityId;
    }

    public ApInterstitialAd getHigh2PriorityInterstitialAd() {
        return high2PriorityInterstitialAd;
    }

    public String getHigh3PriorityId() {
        return high3PriorityId;
    }

    public ApInterstitialAd getHigh3PriorityInterstitialAd() {
        return high3PriorityInterstitialAd;
    }

    public String getNormalPriorityId() {
        return normalPriorityId;
    }

    public ApInterstitialAd getNormalPriorityInterstitialAd() {
        return normalPriorityInterstitialAd;
    }
}
