package com.ads.module.admob;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.ads.module.R;
import com.ads.module.billing.AppPurchase;
import com.ads.module.config.ERainAdConfig;
import com.ads.module.dialog.PrepareLoadingAdsDialog;
import com.ads.module.dialog.ResumeLoadingDialog;
import com.ads.module.event.ERainLogEventManager;
import com.ads.module.funtion.AdCallback;
import com.ads.module.funtion.AdType;
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private static final String TAG = "AppOpenManager";

    private static volatile AppOpenManager INSTANCE;
    private AppOpenAd appResumeAd = null;
    private AppOpenAd splashAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;

    private AppOpenAd.AppOpenAdLoadCallback loadCallbackHigh;
    private AppOpenAd.AppOpenAdLoadCallback loadCallbackMedium;
    private AppOpenAd.AppOpenAdLoadCallback loadCallbackAll;

    private AppOpenAd.AppOpenAdLoadCallback loadCallbackOpen;
    private FullScreenContentCallback fullScreenContentCallback;

    private String appResumeAdId;
    private String appResumeAdIdHigh;
    private boolean isLoadingResume = false;
    private String splashAdId;

    private Activity currentActivity;

    private Application myApplication;

    private static boolean isShowingAd = false;
    private long appResumeLoadTime = 0;
    private long splashLoadTime = 0;
    private int splashTimeout = 0;

    private boolean isInitialized = false;// on  - off ad resume on app
    private boolean isAppResumeEnabled = true;
    private boolean isInterstitialShowing = false;
    private boolean enableScreenContentCallback = false; // default =  true when use splash & false after show splash
    private boolean disableAdResumeByClickAction = false;
    private final List<Class> disabledAppOpenList;
    private Class splashActivity;
    private boolean isTimeout = false;
    private AppOpenAd splashAdHigh = null;
    private AppOpenAd splashAdMedium = null;
    private AppOpenAd splashAdAll = null;

    private AppOpenAd splashAdOpen = null;
    private InterstitialAd splashAdInter = null;

    private int statusHigh = -1;
    private int statusMedium = -1;
    private int statusAll = -1;

    private int statusOpen = -1;
    private int statusInter = -1;

    private final int Type_Loading = 0;
    private final int Type_Load_Success = 1;
    private final int Type_Load_Fail = 2;
    private final int Type_Show_Success = 3;
    private final int Type_Show_Fail = 4;

    private boolean isAppOpenShowed = false;

    private Dialog dialogSplash = null;
    private CountDownTimer timerListenInter = null;
    private long currentTime = 0;
    private long timeRemaining = 0;

    private Handler timeoutHandler;

    public AppOpenAd getSplashAd() {
        return splashAd;
    }

    public void setSplashAd(AppOpenAd splashAd) {
        this.splashAd = splashAd;
    }

    /**
     * Constructor
     */
    private AppOpenManager() {
        disabledAppOpenList = new ArrayList<>();
    }

    public static synchronized AppOpenManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppOpenManager();
        }
        return INSTANCE;
    }

    /**
     * Init AppOpenManager
     *
     * @param application
     */
    public void init(Application application, String appOpenAdId) {
        init(application, null, appOpenAdId);
    }

    public void init(Application application, String appResumeAdIdHigh, String appResumeAdId) {
        isInitialized = true;
        disableAdResumeByClickAction = false;
        this.myApplication = application;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.appResumeAdIdHigh = appResumeAdIdHigh;
        this.appResumeAdId = appResumeAdId;
    }

    public boolean isInitialized() {
        return isInitialized;
    }


    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public void setEnableScreenContentCallback(boolean enableScreenContentCallback) {
        this.enableScreenContentCallback = enableScreenContentCallback;
    }

    public boolean isInterstitialShowing() {
        return isInterstitialShowing;
    }

    public void setInterstitialShowing(boolean interstitialShowing) {
        isInterstitialShowing = interstitialShowing;
    }

    public void disableAdResumeByClickAction() {
        disableAdResumeByClickAction = true;
    }

    public void setDisableAdResumeByClickAction(boolean disableAdResumeByClickAction) {
        this.disableAdResumeByClickAction = disableAdResumeByClickAction;
    }

    public boolean isDisableAdResumeByClickAction() {
        return disableAdResumeByClickAction;
    }

    public boolean isShowingAd() {
        return isShowingAd;
    }

    public void disableAppResumeWithActivity(Class activityClass) {
        Log.d(TAG, "disableAppResumeWithActivity: " + activityClass.getName());
        disabledAppOpenList.add(activityClass);
    }

    public void enableAppResumeWithActivity(Class activityClass) {
        Log.d(TAG, "enableAppResumeWithActivity: " + activityClass.getName());
        disabledAppOpenList.remove(activityClass);
    }

    public void disableAppResume() {
        isAppResumeEnabled = false;
    }

    public void enableAppResume() {
        isAppResumeEnabled = true;
    }

    public void setSplashActivity(Class splashActivity, String adId, int timeoutInMillis) {
        this.splashActivity = splashActivity;
        splashAdId = adId;
        this.splashTimeout = timeoutInMillis;
    }

    public void setAppResumeAdId(String appResumeAdId) {
        this.appResumeAdId = appResumeAdId;
    }

    public void setAppResumeAdId(String appResumeAdIdHigh, String appResumeAdId) {
        this.appResumeAdIdHigh = appResumeAdIdHigh;
        this.appResumeAdId = appResumeAdId;
    }

    public void setAppResumeAdIdHigh(String appResumeAdIdHigh) {
        this.appResumeAdIdHigh = appResumeAdIdHigh;
    }

    public String getAppResumeAdId() {
        return appResumeAdId;
    }

    public String getAppResumeAdIdHigh() {
        return appResumeAdIdHigh;
    }

    public void setFullScreenContentCallback(FullScreenContentCallback callback) {
        this.fullScreenContentCallback = callback;
    }

    public void removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null;
    }

    public void fetchAd(final boolean isSplash) {
        Log.d(TAG, "fetchAd: isSplash = " + isSplash);
        if (isAdAvailable(isSplash)) {
            return;
        }

        if (currentActivity != null && AppPurchase.getInstance().isPurchased(currentActivity)) {
            return;
        }

        if (isSplash) {
            if (currentActivity != null) {
                if (Arrays.asList(currentActivity.getResources().getStringArray(R.array.list_id_test)).contains(splashAdId)) {
                    showTestIdAlert(currentActivity, true, splashAdId);
                }
            }
            loadCallback =
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            Log.d(TAG, "onAppOpenAdLoaded: isSplash = true");
                            AppOpenManager.this.splashAd = ad;
                            AppOpenManager.this.setSplashAd(ad);
                            AppOpenManager.this.splashAd.setOnPaidEventListener(adValue -> {
                                ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                        adValue,
                                        ad.getAdUnitId(),
                                        ad.getResponseInfo().getMediationAdapterClassName());
                            });
                            AppOpenManager.this.splashLoadTime = (new Date()).getTime();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            Log.d(TAG, "onAppOpenAdFailedToLoad: isSplash = true message " + loadAdError.getMessage());
                        }
                    };
            AdRequest request = getAdRequest();
            AppOpenAd.load(myApplication, splashAdId, request, loadCallback);
            return;
        }

        if (isLoadingResume) {
            Log.d(TAG, "fetchAd: app resume is already loading");
            return;
        }

        if (appResumeAdIdHigh != null && !appResumeAdIdHigh.trim().isEmpty()) {
            fetchAdResumeHighFloor();
        } else if (appResumeAdId != null && !appResumeAdId.trim().isEmpty()) {
            fetchAdResumeNormal();
        }
    }

    private void fetchAdResumeHighFloor() {
        if (currentActivity != null) {
            if (Arrays.asList(currentActivity.getResources().getStringArray(R.array.list_id_test)).contains(appResumeAdIdHigh)) {
                showTestIdAlert(currentActivity, false, appResumeAdIdHigh);
            }
        }
        isLoadingResume = true;
        Log.d(TAG, "fetchAdResumeHighFloor: loading " + appResumeAdIdHigh);
        AdRequest request = getAdRequest();
        AppOpenAd.load(myApplication, appResumeAdIdHigh, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.d(TAG, "onAppOpenAdLoaded: appResumeHigh loaded");
                isLoadingResume = false;
                AppOpenManager.this.appResumeAd = ad;
                AppOpenManager.this.appResumeAd.setOnPaidEventListener(adValue -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            ad.getAdUnitId(),
                            ad.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, ad.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                });
                AppOpenManager.this.appResumeLoadTime = (new Date()).getTime();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.d(TAG, "onAppOpenAdFailedToLoad: appResumeHigh failed: " + loadAdError.getMessage() + ", fallback to normal");
                isLoadingResume = false;
                if (appResumeAdId != null && !appResumeAdId.trim().isEmpty()) {
                    fetchAdResumeNormal();
                }
            }
        });
    }

    private void fetchAdResumeNormal() {
        if (currentActivity != null) {
            if (Arrays.asList(currentActivity.getResources().getStringArray(R.array.list_id_test)).contains(appResumeAdId)) {
                showTestIdAlert(currentActivity, false, appResumeAdId);
            }
        }
        isLoadingResume = true;
        Log.d(TAG, "fetchAdResumeNormal: loading " + appResumeAdId);
        AdRequest request = getAdRequest();
        AppOpenAd.load(myApplication, appResumeAdId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.d(TAG, "onAppOpenAdLoaded: appResumeNormal loaded");
                isLoadingResume = false;
                AppOpenManager.this.appResumeAd = ad;
                AppOpenManager.this.appResumeAd.setOnPaidEventListener(adValue -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            ad.getAdUnitId(),
                            ad.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, ad.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                });
                AppOpenManager.this.appResumeLoadTime = (new Date()).getTime();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.d(TAG, "onAppOpenAdFailedToLoad: appResumeNormal failed: " + loadAdError.getMessage());
                isLoadingResume = false;
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void showTestIdAlert(Context context, boolean isSplash, String id) {
        Notification notification = new NotificationCompat.Builder(context, "warning_ads")
                .setContentTitle("Found test ad id")
                .setContentText((isSplash ? "Splash Ads: " : "AppResume Ads: " + id))
                .setSmallIcon(R.drawable.ic_warning)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("warning_ads",
                    "Warning Ads",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(isSplash ? Admob.SPLASH_ADS : Admob.RESUME_ADS, notification);
    }

    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long loadTime, long numHours) {
        long dateDifference = (new Date()).getTime() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public boolean isAdAvailable(boolean isSplash) {
        long loadTime = isSplash ? splashLoadTime : appResumeLoadTime;
        boolean wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4);
        Log.d(TAG, "isAdAvailable: " + wasLoadTimeLessThanNHoursAgo);
        return (isSplash ? splashAd != null : appResumeAd != null)
                && wasLoadTimeLessThanNHoursAgo;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
        Log.d(TAG, "onActivityStarted: " + currentActivity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
        Log.d(TAG, "onActivityResumed: " + currentActivity);
        if (splashActivity == null) {
            if (!activity.getClass().getName().equals(AdActivity.class.getName())) {
                Log.d(TAG, "onActivityResumed 1: with " + activity.getClass().getName());
                fetchAd(false);
            }
        } else {
            if (!activity.getClass().getName().equals(splashActivity.getName()) && !activity.getClass().getName().equals(AdActivity.class.getName())) {
                Log.d(TAG, "onActivityResumed 2: with " + activity.getClass().getName());
                fetchAd(false);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
        Log.d(TAG, "onActivityDestroyed: null");
    }

    public void showAdIfAvailable(final boolean isSplash) {
        if (currentActivity == null || AppPurchase.getInstance().isPurchased(currentActivity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
            return;
        }

        Log.d(TAG, "showAdIfAvailable: " + ProcessLifecycleOwner.get().getLifecycle().getCurrentState());
        Log.d(TAG, "showAd isSplash: " + isSplash);
        if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            Log.d(TAG, "showAdIfAvailable: return");
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }

            return;
        }

        if (!isShowingAd && isAdAvailable(isSplash)) {
            Log.d(TAG, "Will show ad isSplash:" + isSplash);
            if (isSplash) {
                showAdsWithLoading();
            } else {
                showResumeAds();
            }

        } else {
            Log.d(TAG, "Ad is not ready");
            if (!isSplash) {
                fetchAd(false);
            }
            if (isSplash && isShowingAd && isAdAvailable(true)) {
                showAdsWithLoading();
            }
        }
    }

    private void showAdsWithLoading() {
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            dialogSplash = null;
            try {
                dialogSplash = new PrepareLoadingAdsDialog(currentActivity);
                try {
                    dialogSplash.show();
                } catch (Exception e) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback.onAdDismissedFullScreenContent();
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final Dialog finalDialog = dialogSplash;
            new Handler().postDelayed(() -> {
                if (splashAd != null) {
                    splashAd.setFullScreenContentCallback(
                            new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // Set the reference to null so isAdAvailable() returns false.
                                    appResumeAd = null;
                                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                        fullScreenContentCallback.onAdDismissedFullScreenContent();
                                        enableScreenContentCallback = false;
                                    }
                                    isShowingAd = false;
                                    fetchAd(true);
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                        fullScreenContentCallback.onAdFailedToShowFullScreenContent(adError);
                                    }
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                        fullScreenContentCallback.onAdShowedFullScreenContent();
                                    }
                                    isShowingAd = true;
                                    splashAd = null;
                                }


                                @Override
                                public void onAdClicked() {
                                    super.onAdClicked();
                                    if (currentActivity != null) {
                                        ERainLogEventManager.logClickAdsEvent(currentActivity, splashAdId);
                                        if (fullScreenContentCallback != null) {
                                            fullScreenContentCallback.onAdClicked();
                                        }
                                    }
                                }
                            });
                    splashAd.show(currentActivity);
                }
            }, 800);
        }
    }

    Dialog dialog = null;

    private void showResumeAds() {
        if (appResumeAd == null || currentActivity == null || AppPurchase.getInstance().isPurchased(currentActivity)) {
            return;
        }
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {

            try {
                dismissDialogLoading();
                dialog = new ResumeLoadingDialog(currentActivity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback.onAdDismissedFullScreenContent();

                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (appResumeAd != null) {
                final String resumeUnitId = appResumeAd.getAdUnitId();
                appResumeAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        appResumeAd = null;
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            fullScreenContentCallback.onAdDismissedFullScreenContent();
                        }
                        isShowingAd = false;
                        fetchAd(false);

                        dismissDialogLoading();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            fullScreenContentCallback.onAdFailedToShowFullScreenContent(adError);
                        }

                        if (currentActivity != null && !currentActivity.isDestroyed() && dialog != null && dialog.isShowing()) {
                            Log.d(TAG, "dismiss dialog loading ad open: ");
                            try {
                                dialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        appResumeAd = null;
                        isShowingAd = false;
                        fetchAd(false);
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            fullScreenContentCallback.onAdShowedFullScreenContent();
                        }
                        isShowingAd = true;
                        appResumeAd = null;
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (currentActivity != null) {
                            ERainLogEventManager.logClickAdsEvent(currentActivity, resumeUnitId != null ? resumeUnitId : appResumeAdId);
                            if (fullScreenContentCallback != null) {
                                fullScreenContentCallback.onAdClicked();
                            }
                        }
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        if (currentActivity != null) {
                            if (fullScreenContentCallback != null) {
                                fullScreenContentCallback.onAdImpression();
                            }
                        }
                    }
                });
                appResumeAd.show(currentActivity);
            } else {
                dismissDialogLoading();
            }
        }
    }

    public void loadSplashOpenHighFloor(Class splashActivity, Activity activity, String idOpenHigh, String idOpenMedium, String idOpenAll, int timeOutOpen, AdCallback adListener) {
        isAppOpenShowed = false;

        statusHigh = Type_Loading;
        statusMedium = Type_Loading;
        statusAll = Type_Loading;

        if (AppPurchase.getInstance().isPurchased(activity)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (adListener != null && !isAppOpenShowed) {
                    isAppOpenShowed = true;
                    adListener.onNextAction();
                }
            }
        }, timeOutOpen);

        AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenHigh, timeOutOpen);

        // load Open Splash High
        loadCallbackHigh =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        Log.d(TAG, "loadCallbackHigh: onAdLoaded");
                        if (adListener != null) {
                            adListener.onAdLoadedHigh();
                        }

                        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                disableAdResumeByClickAction = true;

                                if (adListener != null) {
                                    adListener.onAdClickedHigh();
                                    adListener.onAdClicked(appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                                }
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                if (adListener != null) {
                                    adListener.onNextAction();
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: High");

                                statusHigh = Type_Load_Fail;

                                if (splashAdHigh != null && statusMedium == Type_Load_Success && !isAppOpenShowed) {
                                    AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenMedium, timeOutOpen);

                                    if (splashAdMedium != null) {
                                        splashAdMedium.show(activity);
                                    }
                                }
                                splashAdHigh = null;

                                if (adListener != null) {
                                    adListener.onAdFailedToShowHigh(adError);
                                }
                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
                                isAppOpenShowed = true;
                                adListener.onAdImpression();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                            }
                        });

                        splashAdHigh = appOpenAd;
                        splashLoadTime = new Date().getTime();
                        appOpenAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                    adValue,
                                    appOpenAd.getAdUnitId(),
                                    appOpenAd.getResponseInfo().getMediationAdapterClassName());

                            ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                            adListener.onAdLogRev(adValue, appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        });

                        if (!isAppOpenShowed) {
                            splashAdHigh.show(currentActivity);
                        }

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "loadCallbackHigh: onAdFailedToLoad");
                        statusHigh = Type_Load_Fail;
                        if (splashAdHigh == null) {
                            if (statusMedium == Type_Load_Success && !isAppOpenShowed) {
                                AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenMedium, timeOutOpen);

                                if (splashAdMedium != null) {
                                    splashAdMedium.show(activity);
                                }
                            }
                        }
                        if (splashAdMedium == null && splashAdAll == null && statusMedium == Type_Load_Fail && statusAll == Type_Load_Fail) {
                            if (adListener != null && !isAppOpenShowed) {
                                isAppOpenShowed = true;
                                adListener.onNextAction();
                            }
                        }
                    }

                };

        // load Open Splash Medium
        loadCallbackMedium =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        Log.d(TAG, "loadCallbackMedium: onAdLoaded");
                        if (adListener != null) {
                            adListener.onAdLoaded();
                        }
                        statusMedium = Type_Load_Success;
                        splashAdMedium = appOpenAd;
                        if ((statusHigh == Type_Load_Fail || statusHigh == Type_Load_Success) && (statusAll == Type_Load_Fail || statusAll == Type_Load_Success || statusAll == Type_Loading) && !isAppOpenShowed) {
                            AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenMedium, timeOutOpen);

                            if (splashAdMedium != null) {
                                splashAdMedium.show(activity);
                            }
                        }

                        splashAdMedium.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                disableAdResumeByClickAction = true;

                                if (adListener != null) {
                                    adListener.onAdClickedMedium();
                                    adListener.onAdClicked(splashAdMedium.getAdUnitId(), splashAdMedium.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                                }
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                if (adListener != null) {
                                    adListener.onNextAction();
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: Medium");

                                splashAdMedium = null;
                                statusMedium = Type_Load_Fail;

                                if (statusAll == Type_Load_Success && !isAppOpenShowed) {
                                    AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen);

                                    if (splashAdAll != null && !isAppOpenShowed) {
                                        splashAdAll.show(activity);
                                    }
                                }

                                if (adListener != null) {
                                    adListener.onAdFailedToShowMedium(adError);
                                }
                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
                                isAppOpenShowed = true;
                                adListener.onAdImpression();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                            }
                        });
                        splashLoadTime = new Date().getTime();
                        appOpenAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                    adValue,
                                    appOpenAd.getAdUnitId(),
                                    appOpenAd.getResponseInfo().getMediationAdapterClassName());
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                            adListener.onAdLogRev(adValue, appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "loadCallbackMedium: onAdFailedToLoad");
                        splashAdMedium = null;
                        statusMedium = Type_Load_Fail;

                        if (splashAdHigh == null && splashAdAll == null && statusHigh == Type_Load_Fail && statusAll == Type_Load_Fail) {
                            if (adListener != null && !isAppOpenShowed) {
                                isAppOpenShowed = true;
                                adListener.onNextAction();
                            }
                        }
                    }

                };

        // load Open Splash All
        loadCallbackAll =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        Log.d(TAG, "loadCallbackAll: onAdLoaded");
                        if (adListener != null) {
                            adListener.onAdLoadedAll();
                        }
                        splashAdAll = appOpenAd;
                        statusAll = Type_Load_Success;

                        if ((statusHigh == Type_Load_Fail || statusHigh == Type_Load_Success) && (statusMedium == Type_Load_Fail || statusMedium == Type_Load_Success) && !isAppOpenShowed) {
                            AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen);

                            if (splashAdAll != null) {
                                splashAdAll.show(activity);
                            }
                        }

                        splashAdAll.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                disableAdResumeByClickAction = true;

                                if (adListener != null) {
                                    adListener.onAdClickedAll();
                                    adListener.onAdClicked(splashAdAll.getAdUnitId(), splashAdAll.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                                }
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                if (adListener != null) {
                                    adListener.onNextAction();
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: All");

                                splashAdAll = null;
                                statusAll = Type_Load_Fail;

                                if (statusHigh == Type_Load_Fail && statusMedium == Type_Load_Fail) {
                                    if (adListener != null && !isAppOpenShowed) {
                                        adListener.onNextAction();
                                    }
                                }

                                if (adListener != null) {
                                    adListener.onAdFailedToShowAll(adError);
                                }
                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
                                isAppOpenShowed = true;
                                adListener.onAdImpression();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                            }
                        });

                        splashLoadTime = new Date().getTime();
                        appOpenAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                    adValue,
                                    appOpenAd.getAdUnitId(),
                                    appOpenAd.getResponseInfo().getMediationAdapterClassName());
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                            adListener.onAdLogRev(adValue, appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "loadCallbackAll: onAdFailedToLoad");
                        splashAdAll = null;
                        statusAll = Type_Load_Fail;

                        if (splashAdHigh == null && splashAdMedium == null && statusHigh == Type_Load_Fail && statusMedium == Type_Load_Fail) {
                            if (adListener != null && !isAppOpenShowed) {
                                isAppOpenShowed = true;
                                adListener.onNextAction();
                            }
                        }

                    }

                };

        AdRequest request = getAdRequest();
        AdRequest request1 = getAdRequest();
        AdRequest request2 = getAdRequest();
        AppOpenAd.load(myApplication, idOpenHigh, request, loadCallbackHigh);
        AppOpenAd.load(myApplication, idOpenMedium, request1, loadCallbackMedium);
        AppOpenAd.load(myApplication, idOpenAll, request2, loadCallbackAll);
    }

    public void loadSplashOpenAndInter(Class splashActivity, AppCompatActivity activity, String idOpen, String idInter, int timeOutOpen, AdCallback adListener) {
        isAppOpenShowed = false;
        statusOpen = Type_Loading;
        statusInter = Type_Loading;

        if (AppPurchase.getInstance().isPurchased(activity)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (adListener != null && !isAppOpenShowed && splashAdOpen == null && splashAdInter == null) {
                    isAppOpenShowed = true;
                    adListener.onNextAction();
                }
            }
        }, timeOutOpen);

        AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpen, timeOutOpen);

        loadCallbackOpen =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        Log.d(TAG, "loadCallbackOpen: onAdLoaded");
                        if (adListener != null) {
                            adListener.onAdLoadedHigh();
                        }

                        appOpenAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                    adValue,
                                    appOpenAd.getAdUnitId(),
                                    appOpenAd.getResponseInfo().getMediationAdapterClassName());
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                            adListener.onAdLogRev(adValue, appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        });

                        splashAdOpen = appOpenAd;
                        splashAdOpen.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                disableAdResumeByClickAction = true;

                                if (adListener != null) {
                                    adListener.onAdClickedHigh();
                                    adListener.onAdClicked(splashAdOpen.getAdUnitId(), splashAdOpen.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                                }
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                if (adListener != null) {
                                    adListener.onNextAction();
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: Open");

                                statusOpen = Type_Load_Fail;
                                splashAdOpen = null;

                                long time = timeOutOpen - (System.currentTimeMillis() - currentTime);

                                if (timerListenInter == null) {
                                    timerListenInter = new CountDownTimer(time, 1000) {
                                        @Override
                                        public void onTick(long l) {
                                            if (statusInter == Type_Load_Success && !isAppOpenShowed) {
                                                isAppOpenShowed = true;
                                                Admob.getInstance().onShowSplash(activity, adListener, splashAdInter);
                                            } else if (statusInter == Type_Load_Fail && !isAppOpenShowed) {
                                                if (adListener != null) {
                                                    isAppOpenShowed = true;
                                                    adListener.onNextAction();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFinish() {
                                            if (!isAppOpenShowed) {
                                                if (adListener != null) {
                                                    isAppOpenShowed = true;
                                                    adListener.onNextAction();
                                                }
                                            }
                                        }
                                    }.start();
                                }
                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
                                isAppOpenShowed = true;
                                adListener.onAdImpression();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                            }
                        });
                        splashLoadTime = new Date().getTime();
                        if (!isAppOpenShowed) {
                            splashAdOpen.show(currentActivity);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, "loadCallbackOpen: onAdFailedToLoad");
                        statusOpen = Type_Load_Fail;
                        splashAdOpen = null;

                        long time = timeOutOpen - (System.currentTimeMillis() - currentTime);

                        if (statusInter != Type_Loading) {
                            if (adListener != null && !isAppOpenShowed) {
                                isAppOpenShowed = true;
                                adListener.onNextAction();
                            }
                        } else {
                            timerListenInter = new CountDownTimer(time, 1000) {
                                @Override
                                public void onTick(long l) {
                                    if (statusInter == Type_Load_Success && !isAppOpenShowed) {
                                        isAppOpenShowed = true;
                                        Admob.getInstance().onShowSplash(activity, adListener, splashAdInter);
                                    } else if (statusInter == Type_Load_Fail && !isAppOpenShowed) {
                                        if (adListener != null) {
                                            isAppOpenShowed = true;
                                            adListener.onNextAction();
                                        }
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    if (!isAppOpenShowed) {
                                        if (adListener != null) {
                                            isAppOpenShowed = true;
                                            adListener.onNextAction();
                                        }
                                    }
                                }
                            }.start();
                        }
                    }
                };

        InterstitialAd.load(activity, idInter, getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        if (adListener != null)
                            adListener.onInterstitialLoad(interstitialAd);

                        statusInter = Type_Load_Success;

                        // Log paid Ads Interstitial
                        interstitialAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(activity,
                                    adValue,
                                    interstitialAd.getAdUnitId(),
                                    interstitialAd.getResponseInfo().getMediationAdapterClassName());
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, interstitialAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                            adListener.onAdLogRev(adValue, interstitialAd.getAdUnitId(), interstitialAd.getResponseInfo().getMediationAdapterClassName(), AdType.INTERSTITIAL);
                        });

                        splashAdInter = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.i(TAG, loadAdError.getMessage());
                        statusInter = Type_Load_Fail;
                        splashAdInter = null;

                        if (statusOpen == Type_Load_Fail) {
                            if (adListener != null && !isAppOpenShowed) {
                                isAppOpenShowed = true;
                                adListener.onNextAction();
                            }
                        }
                    }

                });

        AppOpenAd.load(myApplication, idOpen, getAdRequest(), loadCallbackOpen);
        currentTime = System.currentTimeMillis();
    }

    public void loadAndShowSplashAds(final String aId) {
        loadAndShowSplashAds(aId, 0);
    }

    public void loadAndShowSplashAds(final String adId, long delay) {
        isTimeout = false;
        enableScreenContentCallback = true;
        if (currentActivity != null && AppPurchase.getInstance().isPurchased(currentActivity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                (new Handler()).postDelayed(() -> {
                    fullScreenContentCallback.onAdDismissedFullScreenContent();
                }, delay);
            }
            return;
        }
        loadCallback =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        Log.d(TAG, "onAppOpenAdLoaded: splash");

                        timeoutHandler.removeCallbacks(runnableTimeout);

                        if (isTimeout) {
                            Log.e(TAG, "onAppOpenAdLoaded: splash timeout");
                        } else {
                            AppOpenManager.this.splashAd = appOpenAd;
                            splashLoadTime = new Date().getTime();
                            appOpenAd.setOnPaidEventListener(adValue -> {
                                ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                        adValue,
                                        appOpenAd.getAdUnitId(),
                                        appOpenAd.getResponseInfo().getMediationAdapterClassName());
                            });

                            (new Handler()).postDelayed(() -> {
                                showAdIfAvailable(true);
                            }, delay);
                        }
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "onAppOpenAdFailedToLoad: splash " + loadAdError.getMessage());
                        if (isTimeout) {
                            Log.e(TAG, "onAdFailedToLoad: splash timeout");
                            return;
                        }
                        if (fullScreenContentCallback != null && enableScreenContentCallback) {
                            (new Handler()).postDelayed(() -> {
                                fullScreenContentCallback.onAdDismissedFullScreenContent();
                            }, delay);
                            enableScreenContentCallback = false;
                        }
                    }

                };
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                myApplication, splashAdId, request,
                loadCallback);

        if (splashTimeout > 0) {
            timeoutHandler = new Handler();
            timeoutHandler.postDelayed(runnableTimeout, splashTimeout);
        }
    }

    Runnable runnableTimeout = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "timeout load ad ");
            isTimeout = true;
            enableScreenContentCallback = false;
            if (fullScreenContentCallback != null) {
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
        }
    };

    public void loadAdOpenSplash2id(Class splashActivity, Activity activity, String idOpenHigh, String idOpenAll, int timeOutOpen, AdCallback adListener) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }

        statusHigh = Type_Loading;
        statusAll = Type_Loading;
        isAppOpenShowed = false;

        Runnable actionTimeOut = () -> {
            Log.d("AppOpenSplash", "getAdSplash time out");
            adListener.onNextAction();
            isShowingAd = false;
        };
        Handler handleTimeOut = new Handler();
        handleTimeOut.postDelayed(actionTimeOut, timeOutOpen);
        AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenHigh, timeOutOpen);

        AppOpenAd.load(activity, idOpenHigh, getAdRequest(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                statusHigh = Type_Load_Fail;
                if (statusAll == Type_Load_Success && !isAppOpenShowed && splashAdAll != null) {
                    Log.d("AppOpenSplash", "onAdFailedToLoad: High");
                    AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen);
                    splashAdAll.show(activity);
                }

                if (statusAll == Type_Load_Fail || statusAll == Type_Show_Fail) {
                    Log.d("AppOpenSplash", "onAdFailedToHigh: High");
                    if (adListener != null && !isAppOpenShowed) {
                        adListener.onNextAction();
                    }
                    handleTimeOut.removeCallbacks(actionTimeOut);
                }
            }

            @Override
            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                super.onAdLoaded(appOpenAd);
                handleTimeOut.removeCallbacks(actionTimeOut);
                if (adListener != null) {
                    adListener.onAdLoadedHigh();
                }

                appOpenAd.setOnPaidEventListener(adValue -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            appOpenAd.getAdUnitId(),
                            appOpenAd.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                    adListener.onAdLogRev(adValue, appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                });

                splashAdHigh = appOpenAd;
                statusHigh = Type_Load_Success;

                if (!isAppOpenShowed) {
                    splashAdHigh.show(activity);
                    Log.d("AppOpenSplash", "show High");
                }

                splashAdHigh.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        disableAdResumeByClickAction = true;
                        if (adListener != null) {
                            adListener.onAdClickedHigh();
                            adListener.onAdClicked(splashAdHigh.getAdUnitId(), splashAdHigh.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        if (adListener != null) {
                            adListener.onNextAction();
                            Log.d("AppOpenSplash", "onAdDismissedFullScreenContent: vao 1");
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        if (statusAll == Type_Load_Success && splashAdAll != null && statusHigh != Type_Load_Success) {
                            AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen);
                            splashAdAll.show(activity);
                            Log.d("AppOpenSplash", "onAdFailedToShowFullScreenContent show All");
                        }
                        timeRemaining = timeOutOpen - (System.currentTimeMillis() - currentTime);
                        statusHigh = Type_Show_Fail;
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        isAppOpenShowed = true;
                        statusHigh = Type_Show_Success;
                        adListener.onAdImpression();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                    }
                });
            }
        });

        AppOpenAd.load(activity, idOpenAll,

                getAdRequest(), new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        statusAll = Type_Load_Fail;
                        if (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail) {
                            Log.d("AppOpenSplash", "onAdFailedToLoad: All");
                            if (adListener != null && !isAppOpenShowed) {
                                adListener.onNextAction();
                            }
                            handleTimeOut.removeCallbacks(actionTimeOut);
                        }
                    }

                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                        super.onAdLoaded(appOpenAd);
                        handleTimeOut.removeCallbacks(actionTimeOut);
                        if (adListener != null) {
                            adListener.onAdLoadedAll();
                        }

                        appOpenAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                    adValue,
                                    appOpenAd.getAdUnitId(),
                                    appOpenAd.getResponseInfo().getMediationAdapterClassName());
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                            adListener.onAdLogRev(adValue, appOpenAd.getAdUnitId(), appOpenAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        });

                        splashAdAll = appOpenAd;
                        statusAll = Type_Load_Success;

                        if (!isAppOpenShowed && (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail)) {
                            AppOpenManager.getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen);
                            splashAdAll.show(activity);
                            Log.d("AppOpenSplash", "show All");
                        }

                        splashAdAll.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                disableAdResumeByClickAction = true;
                                if (adListener != null) {
                                    adListener.onAdClickedAll();
                                    adListener.onAdClicked(splashAdAll.getAdUnitId(), splashAdAll.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                                }
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                if (adListener != null) {
                                    adListener.onNextAction();
                                    Log.d("AppOpenSplash", "onAdDismissedFullScreenContent: vao 2");
                                }
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                if (statusHigh == Type_Load_Fail) {
                                    if (timerListenInter == null) {
                                        timerListenInter = new CountDownTimer(timeRemaining, 1000) {
                                            @Override
                                            public void onTick(long l) {
                                                if (isAppOpenShowed) {
                                                    cancel();
                                                }
                                            }

                                            @Override
                                            public void onFinish() {
                                                if (adListener != null && !isAppOpenShowed) {
                                                    if (statusAll != Type_Load_Success && (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail)) {
                                                        adListener.onNextAction();
                                                        Log.d("AppOpenSplash", "onAdFailedToShowFullScreenContentAll: vao 2");
                                                    }
                                                }
                                            }
                                        }.start();
                                    }
                                }
                                statusAll = Type_Show_Fail;
                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
                                isAppOpenShowed = true;
                                statusAll = Type_Load_Success;
                                adListener.onAdImpression();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                            }
                        });
                    }
                });
    }

    public void onCheckShowAppOpenSplashWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            if (!isAppOpenShowed) {
                if (splashAdHigh != null && (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail)) {
                    splashAd = splashAdHigh;
                    showAppOpenSplash(activity, callback);
                    Log.d("AppOpenSplash", "onCheckShowAppOpenSplashWhenFail: vao 1");
                } else if (splashAdAll != null && (statusAll == Type_Load_Fail || statusAll == Type_Show_Fail)) {
                    splashAd = splashAdAll;
                    showAppOpenSplash(activity, callback);
                    Log.d("AppOpenSplash", "onCheckShowAppOpenSplashWhenFail: vao 2");
                }
            }
        }, timeDelay);
    }

    public void showAppOpenSplash(Context context, AdCallback adCallback) {
        if (splashAd == null) {
            adCallback.onNextAction();
            Log.d("AppOpenSplash Failed", "splashAd null: vao 2");
            return;
        }
        new Handler().postDelayed(() -> {
            splashAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            adCallback.onNextAction();
                            isAppOpenShowed = false;
                            Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            adCallback.onAdFailedToShow(adError);
                            isAppOpenShowed = false;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            adCallback.onAdImpression();
                            isAppOpenShowed = true;
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            ERainLogEventManager.logClickAdsEvent(context, splashAdId);
                            adCallback.onAdClicked();
                            adCallback.onAdClicked(splashAd.getAdUnitId(), splashAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adCallback.onAdImpression();
                        }
                    });
            splashAd.show(currentActivity);
        }, 800);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onResume() {
        if (!isAppResumeEnabled) {
            Log.d(TAG, "onResume: app resume is disabled");
            return;
        }

        if (isInterstitialShowing) {
            Log.d(TAG, "onResume: interstitial is showing");
            return;
        }

        if (disableAdResumeByClickAction) {
            Log.d(TAG, "onResume:ad resume disable ad by action");
            disableAdResumeByClickAction = false;
            return;
        }

        for (Class activity : disabledAppOpenList) {
            if (activity.getName().equals(currentActivity.getClass().getName())) {
                Log.d(TAG, "onStart: activity is disabled");
                return;
            }
        }

        if (splashActivity != null && splashActivity.getName().equals(currentActivity.getClass().getName())) {
            String adId = splashAdId;
            if (adId == null) {
                Log.e(TAG, "splash ad id must not be null");
            }
            Log.d(TAG, "onStart: load and show splash ads");
            loadAndShowSplashAds(adId);
            return;
        }

        Log.d(TAG, "onStart: show resume ads :" + currentActivity.getClass().getName());
        showAdIfAvailable(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Log.d(TAG, "onStop: app stop");

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Log.d(TAG, "onPause");
    }

    private void dismissDialogLoading() {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void loadOpenAppAdSplash(final Context context, String idResumeSplash, final long timeDelay, long timeOut, final boolean isShowAdIfReady, final AdCallback adCallback) {
        this.splashAdId = idResumeSplash;
        if (!this.isNetworkConnected(context)) {
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    adCallback.onAdFailedToLoad((LoadAdError) null);
                    adCallback.onNextAction();
                }
            }, timeDelay);
        } else {
            final long currentTimeMillis = System.currentTimeMillis();
            final Runnable timeOutRunnable = () -> {
                Log.d("AppOpenManager", "getAdSplash time out");
                adCallback.onNextAction();
                isShowingAd = false;
            };
            final Handler handler = new Handler();
            handler.postDelayed(timeOutRunnable, timeOut);
            AdRequest adRequest = this.getAdRequest();
            String adUnitId = this.splashAdId;
            AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    handler.removeCallbacks(timeOutRunnable);
                    adCallback.onAdFailedToLoad((LoadAdError) null);
                    adCallback.onNextAction();
                }

                public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                    super.onAdLoaded(appOpenAd);
                    handler.removeCallbacks(timeOutRunnable);
                    AppOpenManager.this.splashAd = appOpenAd;
                    AppOpenManager.this.splashAd.setOnPaidEventListener((adValue) -> {
                    });
                    appOpenAd.setOnPaidEventListener((adValue) -> {
                        ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                adValue,
                                appOpenAd.getAdUnitId(),
                                appOpenAd.getResponseInfo().getMediationAdapterClassName());
                        ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                        adCallback.onAdLogRev(adValue, splashAd.getAdUnitId(), splashAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                    });
                    if (isShowAdIfReady) {
                        long elapsedTime = System.currentTimeMillis() - currentTimeMillis;
                        if (elapsedTime >= timeDelay) {
                            elapsedTime = 0L;
                        }

                        Handler handler1 = new Handler();
                        Context appOpenAdContext = context;
                        Runnable showAppOpenSplashRunnable = () -> {
                            AppOpenManager.this.showAppOpenSplash(appOpenAdContext, adCallback);
                        };
                        handler1.postDelayed(showAppOpenSplashRunnable, elapsedTime);
                    } else {
                        adCallback.onAdSplashReady();
                    }

                }
            };
            AppOpenAd.load(context, adUnitId, adRequest, appOpenAdLoadCallback);
        }

    }

    public void loadOpenAppAdSplashFloor(final Context context, final List<String> listIDResume, final boolean isShowAdIfReady, final AdCallback adCallback) {
        if (!this.isNetworkConnected(context)) {
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    adCallback.onAdFailedToLoad((LoadAdError) null);
                    adCallback.onNextAction();
                }
            }, 3000L);
        } else {
            if (listIDResume == null) {
                adCallback.onAdFailedToLoad((LoadAdError) null);
                adCallback.onNextAction();
                return;
            }

            if (listIDResume.size() > 0) {
                Log.e("AppOpenManager", "load ID :" + (String) listIDResume.get(0));
            }

            if (listIDResume.size() < 1) {
                adCallback.onAdFailedToLoad((LoadAdError) null);
                adCallback.onNextAction();
                return;
            }

            AdRequest adRequest = this.getAdRequest();
            AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    listIDResume.remove(0);
                    if (listIDResume.size() == 0) {
                        adCallback.onAdFailedToLoad((LoadAdError) null);
                        adCallback.onNextAction();
                    } else {
                        AppOpenManager.this.loadOpenAppAdSplashFloor(context, listIDResume, isShowAdIfReady, adCallback);
                    }

                }

                public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                    super.onAdLoaded(appOpenAd);
                    AppOpenManager.this.splashAd = appOpenAd;
                    AppOpenManager.this.splashAd.setOnPaidEventListener((adValue) -> {
                        ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                                adValue,
                                appOpenAd.getAdUnitId(),
                                appOpenAd.getResponseInfo().getMediationAdapterClassName());
                        ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                        adCallback.onAdLogRev(adValue, splashAd.getAdUnitId(), splashAd.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                    });
                    if (isShowAdIfReady) {
                        AppOpenManager.this.showAppOpenSplash(context, adCallback);
                    } else {
                        adCallback.onAdSplashReady();
                    }
                }
            };
            AppOpenAd.load(context, (String) listIDResume.get(0), adRequest, appOpenAdLoadCallback);
        }

    }

    public void onCheckShowSplashWhenFail(final AppCompatActivity activity, final AdCallback callback, int timeDelay) {
        (new Handler(activity.getMainLooper())).postDelayed(new Runnable() {
            public void run() {
                if (AppOpenManager.this.splashAd != null && !AppOpenManager.isShowingAd) {
                    Log.e("AppOpenManager", "show ad splash when show fail in background");
                    AppOpenManager.getInstance().showAppOpenSplash(activity, callback);
                }

            }
        }, (long) timeDelay);
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private boolean isOpenHigh1Failed = false;
    private boolean isOpenHigh2Loaded = false;
    private boolean isOpenHigh3Loaded = false;
    private boolean isOpenNormalLoaded = false;

    public void loadAndShowOpenSplash2SameTime(Context context, String idOpenHigh, String idOpenNormal, long timeOut, AdCallback adCallback) {
        isOpenHigh1Failed = false;
        isOpenHigh2Loaded = true;
        isOpenHigh3Loaded = true;
        isOpenNormalLoaded = false;

        if (!this.isNetworkConnected(context)) {
            (new Handler()).postDelayed(() -> {
                adCallback.onAdFailedToLoad(null);
                adCallback.onNextAction();
            }, 3000L);
        } else {
            // Load Open High
            loadOpenHigh1(context, idOpenHigh, timeOut, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    super.onAdSplashReady();
                    Log.d("LuanDev", "onAdSplashReady: High1");
                    adCallback.onAdSplashHigh1Ready();
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    adCallback.onAdPriorityFailedToLoad(i);
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isOpenHigh1Failed) {
                        if (isOpenNormalLoaded && mOpenSplashNormal != null) {
                            adCallback.onAdSplashNormalReady();
                            isOpenHigh1Failed = true;
                            Log.d("LuanDev", "onAdSplashReady: Normal");
                        } else {
                            // waiting for ads loaded
                            isOpenHigh1Failed = true;
                        }
                    }
                }
            });

            // Load Open Normal
            loadOpenNormal(context, idOpenNormal, timeOut, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    super.onAdSplashReady();
                    if (isOpenHigh1Failed) {
                        adCallback.onAdSplashNormalReady();
                        Log.d("LuanDev", "onAdSplashReady: Normal");
                    } else {
                        isOpenNormalLoaded = true;
                    }
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (isOpenHigh1Failed) {
                        adCallback.onNextAction();
                        Log.d("LuanDev", "onAdSplashReady: nextAction");
                    } else {
                        isOpenNormalLoaded = true;
                    }
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    adCallback.onAdPriorityFailedToLoad(i);
                }
            });
        }
    }

    public void loadAndShowOpenSplash4SameTime(Context context, String idOpenHigh1, String idOpenHigh2, String idOpenHigh3, String idOpenNormal, long timeOut, AdCallback adCallback) {
        isOpenHigh1Failed = false;
        isOpenHigh2Loaded = false;
        isOpenHigh3Loaded = false;
        isOpenNormalLoaded = false;

        if (!this.isNetworkConnected(context)) {
            (new Handler()).postDelayed(() -> {
                adCallback.onAdFailedToLoad(null);
                adCallback.onNextAction();
            }, 3000L);
        } else {
            // Load Open High 1
            loadOpenHigh1(context, idOpenHigh1, timeOut, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    super.onAdSplashReady();
                    Log.d("LuanDev", "onAdSplashReady: 1");
                    adCallback.onAdSplashHigh1Ready();
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    adCallback.onAdPriorityFailedToLoad(i);
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isOpenHigh1Failed) {
                        if (isOpenHigh2Loaded && mOpenSplashHigh2 != null) {
                            adCallback.onAdSplashHigh2Ready();
                            isOpenHigh1Failed = true;
                            Log.d("LuanDev", "onAdSplashReady: 2");
                        } else if (isOpenHigh3Loaded && mOpenSplashHigh3 != null) {
                            adCallback.onAdSplashHigh3Ready();
                            isOpenHigh1Failed = true;
                            Log.d("LuanDev", "onAdSplashReady: 4");
                        } else if (isOpenHigh3Loaded && isOpenNormalLoaded) {
                            adCallback.onAdSplashNormalReady();
                            Log.d("LuanDev", "onAdSplashReady: 7");
                            isOpenHigh1Failed = true;
                        } else {
                            // waiting for ads loaded
                            isOpenHigh1Failed = true;
                        }
                    }
                }
            });

            loadOpenHigh2(context, idOpenHigh2, timeOut, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    super.onAdSplashReady();
                    if (isOpenHigh1Failed) {
                        adCallback.onAdSplashHigh2Ready();
                        Log.d("LuanDev", "onAdSplashReady: 3");
                    } else {
                        isOpenHigh2Loaded = true;
                    }
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (isOpenHigh1Failed && !isOpenHigh2Loaded) {
                        if (isOpenHigh3Loaded && mOpenSplashHigh3 != null) {
                            adCallback.onAdSplashHigh3Ready();
                            isOpenHigh2Loaded = true;
                            Log.d("LuanDev", "onAdSplashReady: 5");
                        } else if (isOpenNormalLoaded && mOpenSplashNormal != null) {
                            adCallback.onAdSplashNormalReady();
                            isOpenHigh2Loaded = true;
                            Log.d("LuanDev", "onAdSplashReady: 8");
                        } else {
                            isOpenHigh2Loaded = true;
                        }
                    } else {
                        isOpenHigh2Loaded = true;
                    }
                }
            });

            loadOpenHigh3(context, idOpenHigh3, timeOut, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    super.onAdSplashReady();
                    if (isOpenHigh1Failed && isOpenHigh2Loaded) {
                        adCallback.onAdSplashHigh3Ready();
                        Log.d("LuanDev", "onAdSplashReady: 6");
                    } else {
                        isOpenHigh3Loaded = true;
                    }
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (isOpenHigh1Failed && isOpenHigh2Loaded && !isOpenHigh3Loaded) {
                        if (isOpenNormalLoaded && mOpenSplashNormal != null) {
                            adCallback.onAdSplashNormalReady();
                            isOpenHigh3Loaded = true;
                            Log.d("LuanDev", "onAdSplashReady: 9");
                        } else {
                            isOpenHigh3Loaded = true;
                        }
                    } else {
                        isOpenHigh3Loaded = true;
                    }
                }
            });

            loadOpenNormal(context, idOpenNormal, timeOut, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    super.onAdSplashReady();
                    if (isOpenHigh1Failed && isOpenHigh2Loaded && isOpenHigh3Loaded) {
                        adCallback.onAdSplashNormalReady();
                        Log.d("LuanDev", "onAdSplashReady: 10");
                    } else {
                        isOpenNormalLoaded = true;
                    }
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (isOpenHigh1Failed && isOpenHigh2Loaded && isOpenHigh3Loaded) {
                        adCallback.onAdSplashNormalReady();
                        isOpenHigh1Failed = true;
                        isOpenHigh2Loaded = true;
                        isOpenHigh3Loaded = true;
                        isOpenNormalLoaded = true;
                        Log.d("LuanDev", "onAdSplashReady: 11");
                    } else {
                        isOpenNormalLoaded = true;
                    }
                }
            });
        }
    }

    private boolean isFailedPriority = false;

    public void showOpenSplash4SameTime(AppCompatActivity activity, AdCallback adListener) {
        isFailedPriority = false;
        if (mOpenSplashHigh1 != null) {
            onShowSplashHigh1(activity, new AdCallback() {
                @Override
                public void onAdFailedToShow(@Nullable AdError i) {
                    super.onAdFailedToShow(i);
                    isFailedPriority = true;
                    onShowSplashHigh2(activity, new AdCallback() {
                        @Override
                        public void onAdFailedToShow(@Nullable AdError i) {
                            super.onAdFailedToShow(i);
                            isFailedPriority = true;
                            onShowSplashHigh3(activity, new AdCallback() {
                                @Override
                                public void onAdFailedToShow(@Nullable AdError i) {
                                    super.onAdFailedToShow(i);
                                    isFailedPriority = true;
                                    onShowSplashNormal(activity, new AdCallback() {
                                        @Override
                                        public void onAdFailedToShow(@Nullable AdError i) {
                                            super.onAdFailedToShow(i);
                                            adListener.onAdFailedToShow(i);
                                        }

                                        @Override
                                        public void onNextAction() {
                                            super.onNextAction();
                                            adListener.onNextAction();
                                            Log.d("LuanDev", "onAdFailedToShow: 41");
                                        }
                                    });
                                }

                                @Override
                                public void onNextAction() {
                                    super.onNextAction();
                                    if (!isFailedPriority) {
                                        Log.d("LuanDev", "onAdFailedToShow: 31");
                                        adListener.onNextAction();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            if (!isFailedPriority) {
                                Log.d("LuanDev", "onAdFailedToShow: 21");
                                adListener.onNextAction();
                            }
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        Log.d("LuanDev", "onAdFailedToShow: 11");
                        adListener.onNextAction();
                    }
                }
            });
        } else if (mOpenSplashHigh2 != null) {
            onShowSplashHigh2(activity, new AdCallback() {
                @Override
                public void onAdFailedToShow(@Nullable AdError i) {
                    super.onAdFailedToShow(i);
                    isFailedPriority = true;
                    onShowSplashHigh3(activity, new AdCallback() {
                        @Override
                        public void onAdFailedToShow(@Nullable AdError i) {
                            super.onAdFailedToShow(i);
                            isFailedPriority = true;
                            onShowSplashNormal(activity, new AdCallback() {
                                @Override
                                public void onAdFailedToShow(@Nullable AdError i) {
                                    super.onAdFailedToShow(i);
                                    adListener.onAdFailedToShow(i);
                                }

                                @Override
                                public void onNextAction() {
                                    super.onNextAction();
                                    adListener.onNextAction();
                                    Log.d("LuanDev", "onAdFailedToShow: 42");
                                }
                            });
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            if (!isFailedPriority) {
                                adListener.onNextAction();
                                Log.d("LuanDev", "onAdFailedToShow: 32");
                            }
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        adListener.onNextAction();
                        Log.d("LuanDev", "onAdFailedToShow: 22");
                    }
                }
            });
        } else if (mOpenSplashHigh3 != null) {
            onShowSplashHigh3(activity, new AdCallback() {
                @Override
                public void onAdFailedToShow(@Nullable AdError i) {
                    super.onAdFailedToShow(i);
                    isFailedPriority = true;
                    onShowSplashNormal(activity, new AdCallback() {
                        @Override
                        public void onAdFailedToShow(@Nullable AdError i) {
                            super.onAdFailedToShow(i);
                            adListener.onAdFailedToShow(i);
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            adListener.onNextAction();
                            Log.d("LuanDev", "onAdFailedToShow: 43");
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        adListener.onNextAction();
                        Log.d("LuanDev", "onAdFailedToShow: 33");
                    }
                }
            });
        } else if (mOpenSplashNormal != null) {
            onShowSplashNormal(activity, new AdCallback() {
                @Override
                public void onAdFailedToShow(@Nullable AdError i) {
                    super.onAdFailedToShow(i);
                    adListener.onAdFailedToShow(i);
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    adListener.onNextAction();
                    Log.d("LuanDev", "onAdFailedToShow: 44");
                }
            });
        } else {
            adListener.onNextAction();
            Log.d("LuanDev", "onAdFailedToShow: 55");
        }
    }

    public void showOpenSplash2SameTime(AppCompatActivity activity, AdCallback adListener) {
        isFailedPriority = false;
        if (mOpenSplashHigh1 != null) {
            onShowSplashHigh1(activity, new AdCallback() {
                @Override
                public void onAdFailedToShow(@Nullable AdError i) {
                    super.onAdFailedToShow(i);
                    isFailedPriority = true;
                    onShowSplashNormal(activity, new AdCallback() {
                        @Override
                        public void onAdFailedToShow(@Nullable AdError i) {
                            super.onAdFailedToShow(i);
                            adListener.onAdFailedToShow(i);
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            adListener.onNextAction();
                            Log.d("LuanDev", "onAdFailedToShow: 2SameTime normal");
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        Log.d("LuanDev", "onAdFailedToShow: 2SameTime high1");
                        adListener.onNextAction();
                    }
                }
            });
        } else if (mOpenSplashNormal != null) {
            onShowSplashNormal(activity, new AdCallback() {
                @Override
                public void onAdFailedToShow(@Nullable AdError i) {
                    super.onAdFailedToShow(i);
                    adListener.onAdFailedToShow(i);
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    adListener.onNextAction();
                    Log.d("LuanDev", "onAdFailedToShow: 2SameTime normal only");
                }
            });
        } else {
            adListener.onNextAction();
            Log.d("LuanDev", "onAdFailedToShow: 2SameTime none");
        }
    }

    private AppOpenAd mOpenSplashSequential;
    private Handler handlerTimeoutOpenSequential;
    private Runnable rdTimeoutOpenSequential;
    private boolean isTimeoutOpenSequential = false;
    private boolean isShowingOpenSequential = false;

    public void loadAndShowOpenSplash2Sequentially(Context context, String idOpenHigh, String idOpenNormal, long timeOut, AdCallback adCallback) {
        List<String> list = new ArrayList<>();
        if (idOpenHigh != null && !idOpenHigh.isEmpty()) list.add(idOpenHigh);
        if (idOpenNormal != null && !idOpenNormal.isEmpty()) list.add(idOpenNormal);
        loadAndShowOpenSplashSequentially(context, list, timeOut, adCallback);
    }

    public void loadAndShowOpenSplashSequentially(Context context, List<String> listIdOpen, long timeOut, AdCallback adCallback) {
        mOpenSplashSequential = null;
        mOpenSplashHigh1 = null;
        isTimeoutOpenSequential = false;
        isShowingOpenSequential = false;

        if (!this.isNetworkConnected(context)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (adCallback != null) {
                    adCallback.onAdFailedToLoad(null);
                    adCallback.onNextAction();
                }
            }, 3000L);
            return;
        }

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        List<String> validIds = new ArrayList<>();
        if (listIdOpen != null) {
            for (String id : listIdOpen) {
                if (id != null && !id.trim().isEmpty()) {
                    validIds.add(id.trim());
                }
            }
        }

        if (validIds.isEmpty()) {
            Log.e("AppOpenManager", "loadAndShowOpenSplashSequentially: listIdOpen is empty");
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        if (timeOut > 0) {
            handlerTimeoutOpenSequential = new Handler(Looper.getMainLooper());
            rdTimeoutOpenSequential = () -> {
                Log.d("AppOpenManager", "loadAndShowOpenSplashSequentially time out");
                isTimeoutOpenSequential = true;
                if (mOpenSplashSequential != null) {
                    if (adCallback != null) {
                        adCallback.onAdSplashReady();
                    }
                    return;
                }
                if (adCallback != null) {
                    adCallback.onNextAction();
                }
            };
            handlerTimeoutOpenSequential.postDelayed(rdTimeoutOpenSequential, timeOut);
        }

        loadOpenSplashSequentialStep(context, validIds, 0, adCallback);
    }

    private void loadOpenSplashSequentialStep(Context context, List<String> ids, int index, AdCallback adCallback) {
        if (isTimeoutOpenSequential) {
            return;
        }

        if (index >= ids.size()) {
            Log.e("AppOpenManager", "loadAndShowOpenSplashSequentially: All ads in list failed to load");
            if (handlerTimeoutOpenSequential != null && rdTimeoutOpenSequential != null) {
                handlerTimeoutOpenSequential.removeCallbacks(rdTimeoutOpenSequential);
            }
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        String currentId = ids.get(index);
        Log.i("AppOpenManager", "loadAndShowOpenSplashSequentially: Loading index " + index + " with ID: " + currentId);

        AdRequest adRequest = this.getAdRequest();
        AppOpenAd.load(context, currentId, adRequest, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                super.onAdLoaded(appOpenAd);
                if (isTimeoutOpenSequential) {
                    return;
                }
                Log.i("AppOpenManager", "loadAndShowOpenSplashSequentially: Loaded successfully at index " + index);
                mOpenSplashSequential = appOpenAd;
                mOpenSplashHigh1 = appOpenAd;

                if (handlerTimeoutOpenSequential != null && rdTimeoutOpenSequential != null) {
                    handlerTimeoutOpenSequential.removeCallbacks(rdTimeoutOpenSequential);
                }

                appOpenAd.setOnPaidEventListener((adValue) -> {
                    Context appContext = myApplication != null ? myApplication.getApplicationContext() : context.getApplicationContext();
                    ERainLogEventManager.logPaidAdImpression(appContext,
                            adValue,
                            appOpenAd.getAdUnitId(),
                            appOpenAd.getResponseInfo() != null ? appOpenAd.getResponseInfo().getMediationAdapterClassName() : "");
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                    if (adCallback != null) {
                        adCallback.onAdLogRev(adValue,
                                appOpenAd.getAdUnitId(),
                                appOpenAd.getResponseInfo() != null ? appOpenAd.getResponseInfo().getMediationAdapterClassName() : "",
                                AdType.APP_OPEN);
                    }
                });

                if (adCallback != null) {
                    if (index == 0) {
                        adCallback.onAdSplashHigh1Ready();
                    } else if (index == 1) {
                        adCallback.onAdSplashNormalReady();
                    }
                    adCallback.onAdSplashReady();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                if (isTimeoutOpenSequential) {
                    return;
                }
                Log.e("AppOpenManager", "loadAndShowOpenSplashSequentially: Failed at index " + index + ": " + loadAdError.getMessage() + " -> try next sequentially");
                if (adCallback != null) {
                    adCallback.onAdPriorityFailedToLoad(loadAdError);
                }
                loadOpenSplashSequentialStep(context, ids, index + 1, adCallback);
            }
        });
    }

    public void showOpenSplashSequentially(AppCompatActivity activity, AdCallback adListener) {
        if (mOpenSplashSequential == null) {
            Log.e("AppOpenManager", "showOpenSplashSequentially: mOpenSplashSequential is null");
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }

        if (handlerTimeoutOpenSequential != null && rdTimeoutOpenSequential != null) {
            handlerTimeoutOpenSequential.removeCallbacks(rdTimeoutOpenSequential);
        }
        isShowingOpenSequential = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (mOpenSplashSequential == null) {
                if (adListener != null) {
                    adListener.onNextAction();
                }
                return;
            }

            if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                // Keep the ad, onCheckShowOpenSplashSequentiallyWhenFail shows it when the app is back
                isShowingOpenSequential = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(new AdError(0, "Show fail in background", "LuanDT"));
                }
                return;
            }

            mOpenSplashSequential.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mOpenSplashSequential = null;
                    mOpenSplashHigh1 = null;
                    if (adListener != null) {
                        adListener.onAdClosed();
                        adListener.onNextAction();
                    }
                    Log.d("AppOpenManager", "onAdDismissedFullScreenContent: sequential open splash");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    mOpenSplashSequential = null;
                    mOpenSplashHigh1 = null;
                    if (adListener != null) {
                        adListener.onAdFailedToShow(adError);
                        adListener.onNextAction();
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    if (adListener != null) {
                        adListener.onAdImpression();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (mOpenSplashSequential != null) {
                        ERainLogEventManager.logClickAdsEvent(activity, mOpenSplashSequential.getAdUnitId());
                        if (adListener != null) {
                            adListener.onAdClicked();
                            adListener.onAdClicked(mOpenSplashSequential.getAdUnitId(),
                                    mOpenSplashSequential.getResponseInfo() != null ? mOpenSplashSequential.getResponseInfo().getMediationAdapterClassName() : "",
                                    AdType.APP_OPEN);
                        }
                    }
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    if (adListener != null) {
                        adListener.onAdImpression();
                    }
                }
            });

            Activity targetActivity = activity != null ? activity : currentActivity;
            if (targetActivity != null && !targetActivity.isFinishing()) {
                mOpenSplashSequential.show(targetActivity);
            } else {
                if (adListener != null) {
                    adListener.onNextAction();
                }
            }
        }, 800);
    }

    public void onCheckShowOpenSplashSequentiallyWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            if (mOpenSplashSequential != null && !isShowingOpenSequential) {
                Log.d("AppOpenManager", "onCheckShowOpenSplashSequentiallyWhenFail: show open splash");
                showOpenSplashSequentially(activity, callback);
            }
        }, timeDelay);
    }

    private AppOpenAd mOpenSplashHigh1;
    private Handler handlerTimeoutHigh1;
    private Runnable rdTimeoutHigh1;

    private void loadOpenHigh1(Context context, String isOpenHigh1, long timeOut, AdCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        rdTimeoutHigh1 = () -> {
            Log.d("AppOpenManager", "getAdSplash time out");
            adCallback.onNextAction();
        };
        handlerTimeoutHigh1 = new Handler();
        handlerTimeoutHigh1.postDelayed(rdTimeoutHigh1, timeOut);

        AdRequest adRequest = this.getAdRequest();
        AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                isOpenHigh1Failed = true;
                if (isOpenHigh2Loaded && isOpenHigh3Loaded && isOpenNormalLoaded) {
                    handlerTimeoutHigh1.removeCallbacks(rdTimeoutHigh1);
                    adCallback.onNextAction();
                }
            }

            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                super.onAdLoaded(appOpenAd);
                mOpenSplashHigh1 = appOpenAd;
                handlerTimeoutHigh1.removeCallbacks(rdTimeoutHigh1);
                appOpenAd.setOnPaidEventListener((adValue) -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            appOpenAd.getAdUnitId(),
                            appOpenAd.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                    adCallback.onAdLogRev(adValue, mOpenSplashHigh1.getAdUnitId(), mOpenSplashHigh1.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                });
                adCallback.onAdSplashReady();
            }
        };
        AppOpenAd.load(context, isOpenHigh1, adRequest, appOpenAdLoadCallback);
    }

    private void onShowSplashHigh1(AppCompatActivity activity, AdCallback adCallback) {
        if (mOpenSplashHigh1 == null) {
            adCallback.onNextAction();
            return;
        }

        if (handlerTimeoutHigh1 != null && rdTimeoutHigh1 != null) {
            handlerTimeoutHigh1.removeCallbacks(rdTimeoutHigh1);
        }

        new Handler().postDelayed(() -> {
            mOpenSplashHigh1.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mOpenSplashHigh1 = null;
                            adCallback.onNextAction();
                            Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            adCallback.onAdFailedToShow(adError);
                            mOpenSplashHigh1 = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            adCallback.onAdImpression();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            ERainLogEventManager.logClickAdsEvent(activity, mOpenSplashHigh1.getAdUnitId());
                            adCallback.onAdClicked();
                            adCallback.onAdClicked(mOpenSplashHigh1.getAdUnitId(), mOpenSplashHigh1.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adCallback.onAdImpression();
                        }
                    });
            mOpenSplashHigh1.show(currentActivity);
        }, 800);
    }

    private AppOpenAd mOpenSplashHigh2;
    private Handler handlerTimeoutHigh2;
    private Runnable rdTimeoutHigh2;

    private void loadOpenHigh2(Context context, String isOpenHigh2, long timeOut, AdCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        rdTimeoutHigh2 = () -> {
            Log.d("AppOpenManager", "getAdSplash time out");
            adCallback.onNextAction();
        };
        handlerTimeoutHigh2 = new Handler();
        handlerTimeoutHigh2.postDelayed(rdTimeoutHigh2, timeOut);

        AdRequest adRequest = this.getAdRequest();
        AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                isOpenHigh2Loaded = true;
                if (isOpenHigh1Failed && isOpenHigh3Loaded && isOpenNormalLoaded) {
                    handlerTimeoutHigh2.removeCallbacks(rdTimeoutHigh2);
                    adCallback.onNextAction();
                }
            }

            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                super.onAdLoaded(appOpenAd);
                mOpenSplashHigh2 = appOpenAd;
                handlerTimeoutHigh2.removeCallbacks(rdTimeoutHigh2);
                appOpenAd.setOnPaidEventListener((adValue) -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            appOpenAd.getAdUnitId(),
                            appOpenAd.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                    adCallback.onAdLogRev(adValue, mOpenSplashHigh2.getAdUnitId(), mOpenSplashHigh2.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                });
                adCallback.onAdSplashReady();
            }
        };
        AppOpenAd.load(context, isOpenHigh2, adRequest, appOpenAdLoadCallback);
    }

    private void onShowSplashHigh2(AppCompatActivity activity, AdCallback adCallback) {
        if (mOpenSplashHigh2 == null) {
            adCallback.onNextAction();
            return;
        }

        if (handlerTimeoutHigh2 != null && rdTimeoutHigh2 != null) {
            handlerTimeoutHigh2.removeCallbacks(rdTimeoutHigh2);
        }

        new Handler().postDelayed(() -> {
            mOpenSplashHigh2.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mOpenSplashHigh2 = null;
                            adCallback.onNextAction();
                            Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            adCallback.onAdFailedToShow(adError);
                            mOpenSplashHigh2 = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            adCallback.onAdImpression();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            ERainLogEventManager.logClickAdsEvent(activity, mOpenSplashHigh2.getAdUnitId());
                            adCallback.onAdClicked();
                            adCallback.onAdClicked(mOpenSplashHigh2.getAdUnitId(), mOpenSplashHigh2.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adCallback.onAdImpression();
                        }
                    });
            mOpenSplashHigh2.show(currentActivity);
        }, 800);
    }

    private AppOpenAd mOpenSplashHigh3;
    private Handler handlerTimeoutHigh3;
    private Runnable rdTimeoutHigh3;

    private void loadOpenHigh3(Context context, String isOpenHigh3, long timeOut, AdCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        rdTimeoutHigh3 = () -> {
            Log.d("AppOpenManager", "getAdSplash time out");
            adCallback.onNextAction();
        };
        handlerTimeoutHigh3 = new Handler();
        handlerTimeoutHigh3.postDelayed(rdTimeoutHigh3, timeOut);

        AdRequest adRequest = this.getAdRequest();
        AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                isOpenHigh3Loaded = true;
                if (isOpenHigh1Failed && isOpenHigh2Loaded && isOpenNormalLoaded) {
                    handlerTimeoutHigh3.removeCallbacks(rdTimeoutHigh3);
                    adCallback.onNextAction();
                }
            }

            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                super.onAdLoaded(appOpenAd);
                mOpenSplashHigh3 = appOpenAd;
                handlerTimeoutHigh3.removeCallbacks(rdTimeoutHigh3);
                appOpenAd.setOnPaidEventListener((adValue) -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            appOpenAd.getAdUnitId(),
                            appOpenAd.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                    adCallback.onAdLogRev(adValue, mOpenSplashHigh3.getAdUnitId(), mOpenSplashHigh3.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                });
                adCallback.onAdSplashReady();
            }
        };
        AppOpenAd.load(context, isOpenHigh3, adRequest, appOpenAdLoadCallback);
    }

    private void onShowSplashHigh3(AppCompatActivity activity, AdCallback adCallback) {
        if (mOpenSplashHigh3 == null) {
            adCallback.onNextAction();
            return;
        }

        if (handlerTimeoutHigh3 != null && rdTimeoutHigh3 != null) {
            handlerTimeoutHigh3.removeCallbacks(rdTimeoutHigh3);
        }

        new Handler().postDelayed(() -> {
            mOpenSplashHigh3.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mOpenSplashHigh3 = null;
                            adCallback.onNextAction();
                            Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            adCallback.onAdFailedToShow(adError);
                            mOpenSplashHigh3 = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            adCallback.onAdImpression();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            ERainLogEventManager.logClickAdsEvent(activity, mOpenSplashHigh3.getAdUnitId());
                            adCallback.onAdClicked();
                            adCallback.onAdClicked(mOpenSplashHigh3.getAdUnitId(), mOpenSplashHigh3.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adCallback.onAdImpression();
                        }
                    });
            mOpenSplashHigh3.show(currentActivity);
        }, 800);
    }

    private AppOpenAd mOpenSplashNormal;
    private Handler handlerTimeoutNormal;
    private Runnable rdTimeoutNormal;

    private void loadOpenNormal(Context context, String isOpenNormal, long timeOut, AdCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adCallback != null) {
                adCallback.onNextAction();
            }
            return;
        }

        rdTimeoutNormal = () -> {
            Log.d("AppOpenManager", "getAdSplash time out");
            adCallback.onNextAction();
        };
        handlerTimeoutNormal = new Handler();
        handlerTimeoutNormal.postDelayed(rdTimeoutNormal, timeOut);

        AdRequest adRequest = this.getAdRequest();
        AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                isOpenNormalLoaded = true;
                if (isOpenHigh1Failed && isOpenHigh2Loaded && isOpenHigh3Loaded) {
                    handlerTimeoutNormal.removeCallbacks(rdTimeoutNormal);
                    adCallback.onNextAction();
                }
            }

            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                super.onAdLoaded(appOpenAd);
                mOpenSplashNormal = appOpenAd;
                handlerTimeoutNormal.removeCallbacks(rdTimeoutNormal);
                appOpenAd.setOnPaidEventListener((adValue) -> {
                    ERainLogEventManager.logPaidAdImpression(myApplication.getApplicationContext(),
                            adValue,
                            appOpenAd.getAdUnitId(),
                            appOpenAd.getResponseInfo().getMediationAdapterClassName());
                    ERainLogEventManager.logPaidAdjustWithToken(adValue, appOpenAd.getAdUnitId(), ERainAdConfig.ADJUST_TOKEN_TIKTOK);
                    adCallback.onAdLogRev(adValue, mOpenSplashNormal.getAdUnitId(), mOpenSplashNormal.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                });
                adCallback.onAdSplashReady();
            }
        };
        AppOpenAd.load(context, isOpenNormal, adRequest, appOpenAdLoadCallback);
    }

    private void onShowSplashNormal(AppCompatActivity activity, AdCallback adCallback) {
        if (mOpenSplashNormal == null) {
            adCallback.onNextAction();
            return;
        }

        if (handlerTimeoutNormal != null && rdTimeoutNormal != null) {
            handlerTimeoutNormal.removeCallbacks(rdTimeoutNormal);
        }

        new Handler().postDelayed(() -> {
            mOpenSplashNormal.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mOpenSplashNormal = null;
                            adCallback.onNextAction();
                            Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            adCallback.onAdFailedToShow(adError);
                            mOpenSplashNormal = null;
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            adCallback.onAdImpression();
                            mOpenSplashNormal = null;
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            ERainLogEventManager.logClickAdsEvent(activity, mOpenSplashNormal.getAdUnitId());
                            adCallback.onAdClicked();
                            adCallback.onAdClicked(mOpenSplashNormal.getAdUnitId(), mOpenSplashNormal.getResponseInfo().getMediationAdapterClassName(), AdType.APP_OPEN);
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adCallback.onAdImpression();
                        }
                    });
            mOpenSplashNormal.show(currentActivity);
        }, 800);
    }
}

