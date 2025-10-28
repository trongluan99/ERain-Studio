package com.ads.module.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharePreferenceUtils {
    private final static String PREF_NAME = "e_rain_ad_pref";

    private final static String KEY_INSTALL_TIME = "KEY_INSTALL_TIME";

    private final static String KEY_CURRENT_TOTAL_REVENUE_AD = "KEY_CURRENT_TOTAL_REVENUE_AD";

    private final static String KEY_CURRENT_TOTAL_REVENUE_001_AD = "KEY_CURRENT_TOTAL_REVENUE_001_AD";

    private final static String KEY_PUSH_EVENT_REVENUE_3_DAY = "KEY_PUSH_EVENT_REVENUE_3_DAY";

    private final static String KEY_PUSH_EVENT_REVENUE_7_DAY = "KEY_PUSH_EVENT_REVENUE_7_DAY";

    private final static String KEY_LAST_IMPRESSION_INTERSTITIAL_TIME = "KEY_LAST_IMPRESSION_INTERSTITIAL_TIME";

    public static long getInstallTime(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pre.getLong(KEY_INSTALL_TIME, 0);
    }

    public static void setInstallTime(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pre.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply();
    }

    public static float getCurrentTotalRevenueAd(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0);
    }

    public static void updateCurrentTotalRevenueAd(Context context, float revenue) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float currentTotalRevenue = pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0);
        currentTotalRevenue += revenue / 1000000.0;
        pre.edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_AD, currentTotalRevenue).apply();
    }

    public static float getCurrentTotalRevenue001Ad(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, 0);
    }

    public static void updateCurrentTotalRevenue001Ad(Context context, float revenue) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pre.edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, revenue).apply();
    }

    public static boolean isPushRevenue3Day(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pre.getBoolean(KEY_PUSH_EVENT_REVENUE_3_DAY, false);
    }

    public static void setPushedRevenue3Day(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pre.edit().putBoolean(KEY_PUSH_EVENT_REVENUE_3_DAY, true).apply();
    }

    public static boolean isPushRevenue7Day(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pre.getBoolean(KEY_PUSH_EVENT_REVENUE_7_DAY, false);
    }

    public static void setPushedRevenue7Day(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pre.edit().putBoolean(KEY_PUSH_EVENT_REVENUE_7_DAY, true).apply();
    }

    public static long getLastImpressionInterstitialTime(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pre.getLong(KEY_LAST_IMPRESSION_INTERSTITIAL_TIME, 0);
    }

    public static void setLastImpressionInterstitialTime(Context context) {
        SharedPreferences pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pre.edit().putLong(KEY_LAST_IMPRESSION_INTERSTITIAL_TIME, System.currentTimeMillis()).apply();
    }
}
