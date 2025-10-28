package com.ads.module.ads.wrapper;

public abstract class ApAdBase {
    protected StatusAd status = StatusAd.AD_INIT;

    public ApAdBase(StatusAd status) {
        this.status = status;
    }

    public ApAdBase() {
    }

    public StatusAd getStatus() {
        return status;
    }

    public void setStatus(StatusAd status) {
        this.status = status;
    }


    abstract boolean isReady();

    public boolean isNotReady() {
        return !isReady();
    }

    public boolean isLoading() {
        return status == StatusAd.AD_LOADING;
    }

    public boolean isLoadFail() {
        return status == StatusAd.AD_LOAD_FAIL;
    }
}
