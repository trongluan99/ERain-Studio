package com.ads.module.application;

import android.app.Application;

import com.ads.module.config.ERainAdConfig;
import com.ads.module.util.AppUtil;
import com.ads.module.util.SharePreferenceUtils;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public abstract class AdsApplication extends Application {

    protected ERainAdConfig mERainAdConfig;
    protected List<String> listTestDevice;

    @Override
    public void onCreate() {
        super.onCreate();
        listTestDevice = new ArrayList<String>();
        mERainAdConfig = new ERainAdConfig(this);
        if (SharePreferenceUtils.getInstallTime(this) == 0) {
            SharePreferenceUtils.setInstallTime(this);
        }
        AppUtil.currentTotalRevenue001Ad = SharePreferenceUtils.getCurrentTotalRevenue001Ad(this);
    }

}
