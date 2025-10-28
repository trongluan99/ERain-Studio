package com.ads.module.admob;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.ads.module.R;
import com.ads.module.billing.AppPurchase;
import com.ads.module.dialog.PrepareLoadingAdsDialog;
import com.ads.module.event.ERainLogEventManager;
import com.ads.module.funtion.AdCallback;
import com.ads.module.funtion.AdType;
import com.ads.module.funtion.AdmobHelper;
import com.ads.module.funtion.RewardCallback;
import com.ads.module.util.SharePreferenceUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaAspectRatio;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Admob {
    private static final String TAG = "ERainStudio";
    private static Admob instance;
    private int currentClicked = 0;
    private String nativeId;
    private int numShowAds = 3;

    private int maxClickAds = 100;
    private Handler handlerTimeout;
    private Runnable rdTimeout;
    private PrepareLoadingAdsDialog dialog;
    private boolean isTimeout;
    private boolean disableAdResumeWhenClickAds = false;
    private boolean isShowLoadingSplash = false;
    boolean isTimeDelay = false;
    private boolean openActivityAfterShowInterAds = false;
    private Context context;

    public static final String BANNER_INLINE_SMALL_STYLE = "BANNER_INLINE_SMALL_STYLE";
    public static final String BANNER_INLINE_LARGE_STYLE = "BANNER_INLINE_LARGE_STYLE";
    private final int MAX_SMALL_INLINE_BANNER_HEIGHT = 50;

    InterstitialAd mInterstitialSplash;

    private String tokenAdjust;

    public void setMaxClickAdsPerDay(int maxClickAds) {
        this.maxClickAds = maxClickAds;
    }

    public static Admob getInstance() {
        if (instance == null) {
            instance = new Admob();
            instance.isShowLoadingSplash = false;
        }
        return instance;
    }

    private Admob() {

    }

    public void setNumToShowAds(int numShowAds) {
        this.numShowAds = numShowAds;
    }

    public void setNumToShowAds(int numShowAds, int currentClicked) {
        this.numShowAds = numShowAds;
        this.currentClicked = currentClicked;
    }

    public void setDisableAdResumeWhenClickAds(boolean disableAdResumeWhenClickAds) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds;
    }

    public void init(Context context, List<String> testDeviceList, String tokenAdjust) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
        MobileAds.initialize(context, initializationStatus -> {
            Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                if (status != null) {
                    Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status.getDescription(), status.getLatency()));
                }
            }
        });
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(testDeviceList).build());

        this.tokenAdjust = tokenAdjust;
        this.context = context;
    }

    public void init(Context context, String tokenAdjust) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }

        MobileAds.initialize(context, initializationStatus -> {
            Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                if (status != null) {
                    Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status.getDescription(), status.getLatency()));
                }
            }
        });

        this.tokenAdjust = tokenAdjust;
        this.context = context;
    }

    public boolean isShowLoadingSplash() {
        return isShowLoadingSplash;
    }

    private String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }

    public void setOpenActivityAfterShowInterAds(boolean openActivityAfterShowInterAds) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds;
    }

    @SuppressLint("VisibleForTests")
    public AdRequest getAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        return builder.build();
    }

    public boolean interstitialSplashLoaded() {
        return mInterstitialSplash != null;
    }

    public InterstitialAd getInterstitialSplash() {
        return mInterstitialSplash;
    }

    /**
     * Load quảng cáo Full tại màn SplashActivity
     * Sau khoảng thời gian timeout thì load ads và callback về cho View
     *
     * @param context
     * @param id
     * @param timeOut    : thời gian chờ ads, timeout <= 0 tương đương với việc bỏ timeout
     * @param timeDelay  : thời gian chờ show ad từ lúc load ads
     * @param adListener
     */
    public void loadSplashInterstitialAds(final Context context, String id, long timeOut, long timeDelay, AdCallback adListener) {
        isTimeDelay = false;
        isTimeout = false;

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //check delay show ad splash
                if (mInterstitialSplash != null) {
                    onShowSplash((AppCompatActivity) context, adListener);
                    return;
                }
                isTimeDelay = true;
            }
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = new Runnable() {
                @Override
                public void run() {
                    isTimeout = true;
                    if (mInterstitialSplash != null) {
                        onShowSplash((AppCompatActivity) context, adListener);
                        return;
                    }
                    if (adListener != null) {
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }


        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                if (isTimeout)
                    return;
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd;
                    if (isTimeDelay) {
                        onShowSplash((AppCompatActivity) context, adListener);
                    }
                }
            }


            @Override
            public void onAdFailedToLoad(LoadAdError i) {
                super.onAdFailedToLoad(i);
                isShowLoadingSplash = false;
                if (isTimeout)
                    return;
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    adListener.onAdFailedToLoad(i);
                    adListener.onNextAction();
                }
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    adListener.onNextAction();
                }
            }
        });

    }

    /**
     * Load quảng cáo Full tại màn SplashActivity
     * Sau khoảng thời gian timeout thì load ads và callback về cho View
     *
     * @param context
     * @param id
     * @param timeOut           : thời gian chờ ads, timeout <= 0 tương đương với việc bỏ timeout
     * @param timeDelay         : thời gian chờ show ad từ lúc load ads
     * @param showSplashIfReady : auto show ad splash if ready
     * @param adListener
     */
    public void loadSplashInterstitialAds(final Context context, String id, long timeOut, long timeDelay, boolean showSplashIfReady, AdCallback adListener) {
        isTimeDelay = false;
        isTimeout = false;

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mInterstitialSplash != null) {
                    if (showSplashIfReady)
                        onShowSplash((AppCompatActivity) context, adListener);
                    else
                        adListener.onAdSplashReady();
                    return;
                }
                isTimeDelay = true;
            }
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = new Runnable() {
                @Override
                public void run() {
                    isTimeout = true;
                    if (mInterstitialSplash != null) {
                        if (showSplashIfReady)
                            onShowSplash((AppCompatActivity) context, adListener);
                        else
                            adListener.onAdSplashReady();
                        return;
                    }
                    if (adListener != null) {
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }

        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                if (isTimeout)
                    return;
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd;
                    if (isTimeDelay) {
                        if (showSplashIfReady)
                            onShowSplash((AppCompatActivity) context, adListener);
                        else
                            adListener.onAdSplashReady();
                    }
                }
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    adListener.onNextAction();
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isTimeout)
                    return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    adListener.onAdFailedToLoad(i);
                }
            }
        });

    }

    public void onShowSplash(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;

        if (mInterstitialSplash == null) {
            adListener.onNextAction();
            return;
        }

        mInterstitialSplash.setOnPaidEventListener(adValue -> {
            ERainLogEventManager.logPaidAdImpression(context,
                    adValue,
                    mInterstitialSplash.getAdUnitId(),
                    mInterstitialSplash.getResponseInfo()
                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);
            if (tokenAdjust != null) {
                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterstitialSplash.getAdUnitId(), tokenAdjust);
            }
        });

        if (handlerTimeout != null && rdTimeout != null) {
            handlerTimeout.removeCallbacks(rdTimeout);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterstitialSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                AppOpenManager.getInstance().setInterstitialShowing(true);
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppOpenManager.getInstance().setInterstitialShowing(false);
                mInterstitialSplash = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                mInterstitialSplash = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                ERainLogEventManager.logClickAdsEvent(context, mInterstitialSplash.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                    AppOpenManager.getInstance().setInterstitialShowing(true);
                } catch (Exception e) {
                    assert adListener != null;
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterstitialSplash != null) {
                        mInterstitialSplash.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    assert adListener != null;
                    adListener.onAdFailedToShow(new AdError(0, "Show fail in background after show loading ad", "LuanDT"));
                }
            }, 800);

        } else {
            isShowLoadingSplash = false;
        }
    }

    public void onShowSplash(AppCompatActivity activity, AdCallback adListener, InterstitialAd mInter) {
        mInterstitialSplash = mInter;
        isShowLoadingSplash = true;

        if (mInter == null) {
            adListener.onNextAction();
            return;
        }

        mInterstitialSplash.setOnPaidEventListener(adValue -> {
            ERainLogEventManager.logPaidAdImpression(context,
                    adValue,
                    mInterstitialSplash.getAdUnitId(),
                    mInterstitialSplash.getResponseInfo()
                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);

            if (tokenAdjust != null) {
                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterstitialSplash.getAdUnitId(), tokenAdjust);
            }
        });

        if (handlerTimeout != null && rdTimeout != null) {
            handlerTimeout.removeCallbacks(rdTimeout);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterstitialSplash.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                AppOpenManager.getInstance().setInterstitialShowing(true);
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppOpenManager.getInstance().setInterstitialShowing(false);
                mInterstitialSplash = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                mInterstitialSplash = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                ERainLogEventManager.logClickAdsEvent(context, mInterstitialSplash.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                    AppOpenManager.getInstance().setInterstitialShowing(true);
                } catch (Exception e) {
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterstitialSplash != null) {
                        mInterstitialSplash.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    assert adListener != null;
                    adListener.onAdFailedToShow(new AdError(0, "Show fail in background after show loading ad", "LuanDT"));
                }
            }, 800);

        } else {
            isShowLoadingSplash = false;
        }
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (interstitialSplashLoaded() && !isShowLoadingSplash()) {
                    Admob.getInstance().onShowSplash(activity, callback);
                }
            }
        }, timeDelay);
    }

    /**
     * Trả về 1 InterstitialAd và request Ads
     *
     * @param context
     * @param id
     * @return
     */
    public void getInterstitialAds(Context context, String id, AdCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context) || AdmobHelper.getNumClickAdsPerDay(context, id) >= maxClickAds) {
            adCallback.onInterstitialLoad(null);
            return;
        }

        InterstitialAd.load(context, id, getAdRequest(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        if (adCallback != null)
                            adCallback.onInterstitialLoad(interstitialAd);

                        interstitialAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    interstitialAd.getAdUnitId(),
                                    interstitialAd.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);
                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, interstitialAd.getAdUnitId(), tokenAdjust);
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.i(TAG, loadAdError.getMessage());
                        if (adCallback != null)
                            adCallback.onAdFailedToLoad(loadAdError);
                    }

                });

    }


    /**
     * Hiển thị ads  timeout
     * Sử dụng khi reopen app in splash
     *
     * @param context
     * @param mInterstitialAd
     * @param timeDelay
     */
    public void showInterstitialAdByTimes(final Context context, final InterstitialAd mInterstitialAd, final AdCallback callback, long timeDelay) {
        if (timeDelay > 0) {
            handlerTimeout = new Handler();
            rdTimeout = new Runnable() {
                @Override
                public void run() {
                    forceShowInterstitial(context, mInterstitialAd, callback);
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeDelay);
        } else {
            forceShowInterstitial(context, mInterstitialAd, callback);
        }
    }


    /**
     * Hiển thị ads theo số lần được xác định trước và callback result
     * vd: click vào 3 lần thì show ads full.
     * AdmodHelper.setupAdmodData(context) -> kiểm tra xem app đc hoạt động đc 1 ngày chưa nếu YES thì reset lại số lần click vào ads
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    public void showInterstitialAdByTimes(final Context context, InterstitialAd mInterstitialAd, final AdCallback callback) {
        AdmobHelper.setupAdmobData(context);
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback.onNextAction();
            return;
        }
        if (mInterstitialAd == null) {
            if (callback != null) {
                callback.onNextAction();
            }
            return;
        }

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                AppOpenManager.getInstance().setInterstitialShowing(false);
                SharePreferenceUtils.setLastImpressionInterstitialTime(context);
                if (callback != null) {
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction();
                    }
                    callback.onAdClosed();
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                if (callback != null) {
                    callback.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                AppOpenManager.getInstance().setInterstitialShowing(true);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                if (callback != null) {
                    callback.onAdClicked();
                }
                ERainLogEventManager.logClickAdsEvent(context, mInterstitialAd.getAdUnitId());
            }
        });

        if (AdmobHelper.getNumClickAdsPerDay(context, mInterstitialAd.getAdUnitId()) < maxClickAds) {
            showInterstitialAd(context, mInterstitialAd, callback);
            return;
        }
        if (callback != null) {
            callback.onNextAction();
        }
    }


    /**
     * Bắt buộc hiển thị  ads full và callback result
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    public void forceShowInterstitial(Context context, InterstitialAd mInterstitialAd, final AdCallback callback) {
        currentClicked = numShowAds;
        showInterstitialAdByTimes(context, mInterstitialAd, callback);
    }

    /**
     * Kiểm tra và hiện thị ads
     *
     * @param context
     * @param mInterstitialAd
     * @param callback
     */
    private void showInterstitialAd(Context context, InterstitialAd mInterstitialAd, AdCallback callback) {
        currentClicked++;
        if (currentClicked >= numShowAds && mInterstitialAd != null) {
            if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                try {
                    if (dialog != null && dialog.isShowing())
                        dialog.dismiss();
                    dialog = new PrepareLoadingAdsDialog(context);
                    dialog.setCancelable(false);
                    try {
                        callback.onInterstitialShow();
                        dialog.show();
                        AppOpenManager.getInstance().setInterstitialShowing(true);
                    } catch (Exception e) {
                        callback.onNextAction();
                        return;
                    }
                } catch (Exception e) {
                    dialog = null;
                    e.printStackTrace();
                }
                new Handler().postDelayed(() -> {
                    if (((AppCompatActivity) context).getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        if (openActivityAfterShowInterAds && callback != null) {
                            callback.onNextAction();
                            new Handler().postDelayed(() -> {
                                if (dialog != null && dialog.isShowing() && !((Activity) context).isDestroyed())
                                    dialog.dismiss();
                            }, 1500);
                        }
                        mInterstitialAd.show((Activity) context);
                    } else {
                        if (dialog != null && dialog.isShowing() && !((Activity) context).isDestroyed())
                            dialog.dismiss();
                        callback.onAdFailedToShow(new AdError(0, "Show fail in background after show loading ad", "LuanDT"));
                    }
                }, 800);
            }
            currentClicked = 0;
        } else if (callback != null) {
            if (dialog != null) {
                dialog.dismiss();
            }
            callback.onNextAction();
        }
    }

    /**
     * Load quảng cáo Banner Trong Activity
     *
     * @param mActivity
     * @param id
     */
    public void loadBanner(final Activity mActivity, String id) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, false, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load quảng cáo Banner Trong Activity
     *
     * @param mActivity
     * @param id
     */
    public void loadBanner(final Activity mActivity, String id, AdCallback callback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, false, BANNER_INLINE_LARGE_STYLE);
    }


    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @deprecated Using loadInlineBanner()
     */
    @Deprecated
    public void loadBanner(final Activity mActivity, String id, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param inlineStyle
     */
    public void loadInlineBanner(final Activity activity, String id, String inlineStyle) {
        final FrameLayout adContainer = activity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = activity.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, null, true, inlineStyle);
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @param callback
     * @param useInlineAdaptive
     * @deprecated Using loadInlineBanner() with callback
     */
    @Deprecated
    public void loadBanner(final Activity mActivity, String id, final AdCallback callback, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load quảng cáo Banner Trong Activity set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param inlineStyle
     * @param callback
     */
    public void loadInlineBanner(final Activity activity, String id, String inlineStyle, final AdCallback callback) {
        final FrameLayout adContainer = activity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = activity.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, callback, true, inlineStyle);
    }

    /**
     * Load quảng cáo Collapsible Banner Trong Activity
     *
     * @param mActivity
     * @param id
     */
    public void loadCollapsibleBanner(final Activity mActivity, String id, String gravity, final AdCallback callback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback);
    }

    /**
     * Load quảng cáo Collapsible Banner Size Medium Trong Activity
     *
     * @param mActivity
     * @param id
     */
    public void loadCollapsibleBannerSizeMedium(final Activity mActivity, String id, String gravity, AdSize sizeBanner, final AdCallback callback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadCollapsibleAutoSizeMedium(mActivity, id, gravity, sizeBanner, adContainer, containerShimmer, callback);
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment
     *
     * @param mActivity
     * @param id
     * @param rootView
     */
    public void loadBannerFragment(final Activity mActivity, String id, final View rootView) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, false, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment
     *
     * @param mActivity
     * @param id
     * @param rootView
     */
    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, final AdCallback callback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, false, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @param rootView
     * @deprecated Using loadInlineBannerFragment()
     */
    @Deprecated
    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param rootView
     * @param inlineStyle
     */
    public void loadInlineBannerFragment(final Activity activity, String id, final View rootView, String inlineStyle) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, null, true, inlineStyle);
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param mActivity
     * @param id
     * @param rootView
     * @param callback
     * @param useInlineAdaptive
     * @deprecated Using loadInlineBannerFragment() with callback
     */
    @Deprecated
    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, final AdCallback callback, Boolean useInlineAdaptive) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE);
    }

    /**
     * Load Quảng Cáo Banner Trong Fragment set Inline adaptive banners
     *
     * @param activity
     * @param id
     * @param rootView
     * @param inlineStyle
     * @param callback
     */
    public void loadInlineBannerFragment(final Activity activity, String id, final View rootView, String inlineStyle, final AdCallback callback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(activity, id, adContainer, containerShimmer, callback, true, inlineStyle);
    }

    /**
     * Load quảng cáo Collapsible Banner Trong Fragment
     *
     * @param mActivity
     * @param id
     * @param rootView
     * @param gravity
     * @param callback
     */
    public void loadCollapsibleBannerFragment(final Activity mActivity, String id, final View rootView, String gravity, final AdCallback callback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback);
    }

    private void loadBanner(final Activity mActivity, String id,
                            final FrameLayout adContainer, final ShimmerFrameLayout containerShimmer,
                            final AdCallback callback, Boolean useInlineAdaptive, String inlineStyle) {
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }

        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity, useInlineAdaptive, inlineStyle);
            int adHeight;
            if (useInlineAdaptive && inlineStyle.equalsIgnoreCase(BANNER_INLINE_SMALL_STYLE)) {
                adHeight = MAX_SMALL_INLINE_BANNER_HEIGHT;
            } else {
                adHeight = adSize.getHeight();
            }
            containerShimmer.getLayoutParams().height = (int) (adHeight * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);

                    if (callback != null) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                }


                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "Banner adapter class name: " + adView.getResponseInfo().getMediationAdapterClassName());
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                    if (adView != null) {
                        adView.setOnPaidEventListener(adValue -> {
                            Log.d(TAG, "OnPaidEvent banner:" + adValue.getValueMicros());

                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    adView.getAdUnitId(),
                                    adView.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.BANNER);
                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, adView.getAdUnitId(), tokenAdjust);
                            }
                        });
                    }

                    if (callback != null) {
                        callback.onAdLoaded();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    if (callback != null) {
                        callback.onAdClicked();
                        Log.d(TAG, "onAdClicked");
                    }
                    ERainLogEventManager.logClickAdsEvent(context, id);
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    if (callback != null) {
                        callback.onAdImpression();
                    }
                }
            });

            adView.loadAd(getAdRequest());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCollapsibleBanner(final Activity mActivity, String id, String gravity, final FrameLayout adContainer,
                                       final ShimmerFrameLayout containerShimmer, final AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }

        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity, false, "");
            containerShimmer.getLayoutParams().height = (int) (adSize.getHeight() * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity));
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                    if (callback != null) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                }

                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "Banner adapter class name: " + adView.getResponseInfo().getMediationAdapterClassName());
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                    adView.setOnPaidEventListener(adValue -> {
                        Log.d(TAG, "OnPaidEvent banner:" + adValue.getValueMicros());

                        ERainLogEventManager.logPaidAdImpression(context,
                                adValue,
                                adView.getAdUnitId(),
                                adView.getResponseInfo()
                                        .getMediationAdapterClassName(), AdType.BANNER);
                        if (tokenAdjust != null) {
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, adView.getAdUnitId(), tokenAdjust);
                        }
                    });
                    if (callback != null) {
                        callback.onAdLoaded();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    ERainLogEventManager.logClickAdsEvent(context, id);
                    if (callback != null) {
                        callback.onAdClicked();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCollapsibleAutoSizeMedium(final Activity mActivity, String id, String gravity, AdSize sizeBanner, final FrameLayout adContainer,
                                               final ShimmerFrameLayout containerShimmer, final AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }

        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = sizeBanner;
            containerShimmer.getLayoutParams().height = (int) (adSize.getHeight() * Resources.getSystem().getDisplayMetrics().density + 0.5f);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity));
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                    if (callback != null) {
                        callback.onAdFailedToLoad(loadAdError);
                    }
                }

                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "Banner adapter class name: " + adView.getResponseInfo().getMediationAdapterClassName());
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                    adView.setOnPaidEventListener(adValue -> {
                        Log.d(TAG, "OnPaidEvent banner:" + adValue.getValueMicros());

                        ERainLogEventManager.logPaidAdImpression(context,
                                adValue,
                                adView.getAdUnitId(),
                                adView.getResponseInfo()
                                        .getMediationAdapterClassName(), AdType.BANNER);
                        if (tokenAdjust != null) {
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, adView.getAdUnitId(), tokenAdjust);
                        }
                    });
                    if (callback != null) {
                        callback.onAdLoaded();
                    }
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    ERainLogEventManager.logClickAdsEvent(context, id);
                    if (callback != null) {
                        callback.onAdClicked();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInlineAdaptiveBanner(final Activity mActivity, String id, AdCallback adCallback) {
        @SuppressLint("VisibleForTests") AdSize adSize = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(mActivity, 320);
        AdView bannerView = new AdView(mActivity);
        bannerView.setAdUnitId(id);
        bannerView.setAdSize(adSize);
        bannerView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                adCallback.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                adCallback.onAdFailedToLoad(loadAdError);
            }
        });
        bannerView.loadAd(getAdRequest());
    }

    @SuppressLint("VisibleForTests")
    private AdSize getAdSize(Activity mActivity, Boolean useInlineAdaptive, String inlineStyle) {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        if (useInlineAdaptive) {
            if (inlineStyle.equalsIgnoreCase(BANNER_INLINE_LARGE_STYLE)) {
                return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(mActivity, adWidth);
            } else {
                return AdSize.getInlineAdaptiveBannerAdSize(adWidth, MAX_SMALL_INLINE_BANNER_HEIGHT);
            }
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth);

    }

    @SuppressLint("VisibleForTests")
    private AdRequest getAdRequestForCollapsibleBanner(String gravity) {
        AdRequest.Builder builder = new AdRequest.Builder();
        Bundle admobExtras = new Bundle();
        admobExtras.putString("collapsible", gravity);
        admobExtras.putString("collapsible_request_id", UUID.randomUUID().toString());
        builder.addNetworkExtrasBundle(AdMobAdapter.class, admobExtras);
        return builder.build();
    }

    /**
     * load ads big native
     *
     * @param mActivity
     * @param id
     */
    public void loadNative(final Activity mActivity, String id) {
        final FrameLayout frameLayout = mActivity.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size);
    }

    public void loadNativeFragment(final Activity mActivity, String id, View parent) {
        final FrameLayout frameLayout = parent.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = parent.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size);
    }

    public void loadSmallNative(final Activity mActivity, String adUnitId) {
        final FrameLayout frameLayout = mActivity.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admob_medium);
    }

    public void loadSmallNativeFragment(final Activity mActivity, String adUnitId, View parent) {
        final FrameLayout frameLayout = parent.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = parent.findViewById(R.id.shimmer_container_native);
        loadNative(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admob_medium);
    }

    public void loadNativeAd(Context context, String id, final AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(nativeAd -> {
                    callback.onUnifiedNativeAdLoaded(nativeAd);
                    nativeAd.setOnPaidEventListener(adValue -> {
                        ERainLogEventManager.logPaidAdImpression(context,
                                adValue,
                                id,
                                nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                        if (tokenAdjust != null) {
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, id, tokenAdjust);
                        }
                    });
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        callback.onAdFailedToLoad(error);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        if (callback != null) {
                            callback.onAdImpression();
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                        ERainLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAd(getAdRequest());
    }

    public void loadNativeAds(Context context, String id, final AdCallback callback, int countAd) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback.onAdClosed();
            return;
        }
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        callback.onUnifiedNativeAdLoaded(nativeAd);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, id, tokenAdjust);
                            }
                        });
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        callback.onAdFailedToLoad(error);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                        ERainLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAds(getAdRequest(), countAd);
    }

    private void loadNative(final Context context, final ShimmerFrameLayout containerShimmer, final FrameLayout frameLayout, final String id, final int layout) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE);
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();


        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.VISIBLE);
                        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(context)
                                .inflate(layout, null);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, id, tokenAdjust);
                            }
                        });
                        populateUnifiedNativeAdView(nativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }


                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        ERainLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();

        adLoader.loadAd(getAdRequest());
    }

    private void loadNative(final Context context, final ShimmerFrameLayout containerShimmer, final FrameLayout frameLayout, final String id, final int layout, final AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE);
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();


        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.VISIBLE);
                        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(context)
                                .inflate(layout, null);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, id, tokenAdjust);
                            }
                        });
                        populateUnifiedNativeAdView(nativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }

                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.GONE);
                    }


                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                        ERainLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();


        adLoader.loadAd(getAdRequest());
    }

    public void loadNativeAdsFullScreen(Context context, String id, final AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }

        VideoOptions videoOptions =
                new VideoOptions.Builder().setStartMuted(false).build();
        NativeAdOptions adOptions =
                new NativeAdOptions.Builder()
                        .setMediaAspectRatio(MediaAspectRatio.PORTRAIT)
                        .setVideoOptions(videoOptions)
                        .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        callback.onUnifiedNativeAdLoaded(nativeAd);
                        nativeAd.setOnPaidEventListener(adValue -> {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    id,
                                    nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);

                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, id, tokenAdjust);
                            }
                        });
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        callback.onAdFailedToLoad(error);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                        ERainLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAds(getAdRequest(), 5);

    }

    public void loadNativeAdsFullScreen(final Context context, final ShimmerFrameLayout containerShimmer, final FrameLayout frameLayout, final String id, final int layout, final AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE);
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setMediaAspectRatio(MediaAspectRatio.PORTRAIT)
                .setVideoOptions(videoOptions)
                .build();


        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(nativeAd -> {
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    frameLayout.setVisibility(View.VISIBLE);
                    @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(context)
                            .inflate(layout, null);
                    nativeAd.setOnPaidEventListener(adValue -> {

                        ERainLogEventManager.logPaidAdImpression(context,
                                adValue,
                                id,
                                nativeAd.getResponseInfo().getMediationAdapterClassName(), AdType.NATIVE);
                        if (tokenAdjust != null) {
                            ERainLogEventManager.logPaidAdjustWithToken(adValue, id, tokenAdjust);
                        }
                    });
                    populateUnifiedNativeAdView(nativeAd, adView);
                    frameLayout.removeAllViews();
                    frameLayout.addView(adView);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        containerShimmer.stopShimmer();
                        containerShimmer.setVisibility(View.GONE);
                        frameLayout.setVisibility(View.GONE);
                    }


                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        if (disableAdResumeWhenClickAds)
                            AppOpenManager.getInstance().disableAdResumeByClickAction();
                        if (callback != null) {
                            callback.onAdClicked();
                        }
                        ERainLogEventManager.logClickAdsEvent(context, id);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();


        adLoader.loadAds(getAdRequest(), 5);

    }

    public void populateUnifiedNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setMediaView(adView.findViewById(R.id.ad_media));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        try {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        try {
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getCallToAction() == null) {
                Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
            } else {
                Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
                ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getIcon() == null) {
                Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(
                        nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getPrice() == null) {
                Objects.requireNonNull(adView.getPriceView()).setVisibility(View.INVISIBLE);
            } else {
                Objects.requireNonNull(adView.getPriceView()).setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getStarRating() == null) {
                Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) Objects.requireNonNull(adView.getStarRatingView())).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        adView.setNativeAd(nativeAd);

    }


    private RewardedAd rewardedAd;

    /**
     * Khởi tạo ads reward
     *
     * @param context
     * @param id
     */
    public void initRewardAds(Context context, String id) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        this.nativeId = id;
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        RewardedAd.load(context, id, getAdRequest(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                Admob.this.rewardedAd = rewardedAd;
                Admob.this.rewardedAd.setOnPaidEventListener(adValue -> {
                    ERainLogEventManager.logPaidAdImpression(context,
                            adValue,
                            rewardedAd.getAdUnitId(), Admob.this.rewardedAd.getResponseInfo().getMediationAdapterClassName()
                            , AdType.REWARDED);
                    if (tokenAdjust != null) {
                        ERainLogEventManager.logPaidAdjustWithToken(adValue, rewardedAd.getAdUnitId(), tokenAdjust);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
            }
        });
    }

    /**
     * Load ad Reward
     *
     * @param context
     * @param id
     */
    public void initRewardAds(Context context, String id, AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        this.nativeId = id;
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        RewardedAd.load(context, id, getAdRequest(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                callback.onRewardAdLoaded(rewardedAd);
                Admob.this.rewardedAd = rewardedAd;
                Admob.this.rewardedAd.setOnPaidEventListener(adValue -> {
                    ERainLogEventManager.logPaidAdImpression(context,
                            adValue,
                            rewardedAd.getAdUnitId(),
                            Admob.this.rewardedAd.getResponseInfo().getMediationAdapterClassName()
                            , AdType.REWARDED);
                    if (tokenAdjust != null) {
                        ERainLogEventManager.logPaidAdjustWithToken(adValue, rewardedAd.getAdUnitId(), tokenAdjust);
                    }
                });

            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onAdFailedToLoad(loadAdError);
                Admob.this.rewardedAd = null;
            }
        });
    }

    /**
     * Load ad Reward Interstitial
     *
     * @param context
     * @param id
     */
    public void getRewardInterstitial(Context context, String id, AdCallback callback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        this.nativeId = id;
        if (AppPurchase.getInstance().isPurchased(context)) {
            return;
        }
        RewardedInterstitialAd.load(context, id, getAdRequest(), new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedAd) {
                callback.onRewardAdLoaded(rewardedAd);
                rewardedAd.setOnPaidEventListener(adValue -> {
                    ERainLogEventManager.logPaidAdImpression(context,
                            adValue,
                            rewardedAd.getAdUnitId(),
                            rewardedAd.getResponseInfo().getMediationAdapterClassName()
                            , AdType.REWARDED);
                    if (tokenAdjust != null) {
                        ERainLogEventManager.logPaidAdjustWithToken(adValue, rewardedAd.getAdUnitId(), tokenAdjust);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                callback.onAdFailedToLoad(loadAdError);
            }
        });
    }

    public RewardedAd getRewardedAd() {

        return rewardedAd;
    }

    /**
     * Show Reward and callback
     *
     * @param context
     * @param adCallback
     */
    public void showRewardAds(final Activity context, final RewardCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback.onUserEarnedReward(null);
            return;
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId);

            adCallback.onRewardedAdFailedToShow(0);
            return;
        } else {
            Admob.this.rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    if (adCallback != null)
                        adCallback.onRewardedAdClosed();

                    AppOpenManager.getInstance().setInterstitialShowing(false);

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    if (adCallback != null)
                        adCallback.onRewardedAdFailedToShow(adError.getCode());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();

                    AppOpenManager.getInstance().setInterstitialShowing(true);
                    rewardedAd = null;
                }

                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    ERainLogEventManager.logClickAdsEvent(context, rewardedAd.getAdUnitId());
                }
            });
            rewardedAd.show(context, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    if (adCallback != null) {
                        adCallback.onUserEarnedReward(rewardItem);

                    }
                }
            });
        }
    }

    /**
     * Show Reward Interstitial and callback
     *
     * @param activity
     * @param rewardedInterstitialAd
     * @param adCallback
     */
    public void showRewardInterstitial(final Activity activity, RewardedInterstitialAd rewardedInterstitialAd, final RewardCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            adCallback.onUserEarnedReward(null);
            return;
        }
        if (rewardedInterstitialAd == null) {
            initRewardAds(activity, nativeId);

            adCallback.onRewardedAdFailedToShow(0);
            return;
        } else {
            rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    if (adCallback != null)
                        adCallback.onRewardedAdClosed();

                    AppOpenManager.getInstance().setInterstitialShowing(false);

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    if (adCallback != null)
                        adCallback.onRewardedAdFailedToShow(adError.getCode());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();

                    AppOpenManager.getInstance().setInterstitialShowing(true);

                }

                public void onAdClicked() {
                    super.onAdClicked();
                    ERainLogEventManager.logClickAdsEvent(activity, rewardedAd.getAdUnitId());
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            });
            rewardedInterstitialAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    if (adCallback != null) {
                        adCallback.onUserEarnedReward(rewardItem);
                    }
                }
            });
        }
    }


    /**
     * Show quảng cáo reward và nhận kết quả trả về
     *
     * @param context
     * @param adCallback
     */
    public void showRewardAds(final Activity context, RewardedAd rewardedAd, final RewardCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback.onUserEarnedReward(null);
            return;
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId);

            adCallback.onRewardedAdFailedToShow(0);
            return;
        } else {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    if (adCallback != null)
                        adCallback.onRewardedAdClosed();


                    AppOpenManager.getInstance().setInterstitialShowing(false);

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    if (adCallback != null)
                        adCallback.onRewardedAdFailedToShow(adError.getCode());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();

                    AppOpenManager.getInstance().setInterstitialShowing(true);
                    initRewardAds(context, nativeId);
                }

                public void onAdClicked() {
                    super.onAdClicked();
                    if (disableAdResumeWhenClickAds)
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    if (adCallback != null) {
                        adCallback.onAdClicked();
                    }
                    ERainLogEventManager.logClickAdsEvent(context, rewardedAd.getAdUnitId());
                }
            });
            rewardedAd.show(context, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    if (adCallback != null) {
                        adCallback.onUserEarnedReward(rewardItem);

                    }
                }
            });
        }
    }


    @SuppressLint("HardwareIds")
    public String getDeviceId(Activity activity) {
        String android_id = Settings.Secure.getString(activity.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return md5(android_id).toUpperCase();
    }

    private String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public final static int SPLASH_ADS = 0;
    public final static int RESUME_ADS = 1;
    private final static int BANNER_ADS = 2;
    private final static int INTERS_ADS = 3;
    private final static int REWARD_ADS = 4;
    private final static int NATIVE_ADS = 5;


    private boolean isShowInterstitialSplashSuccess = false;
    private boolean isInterHigh1Failed = false;
    private boolean isInterHigh2Loaded = false;
    private boolean isInterHigh3Loaded = false;
    private boolean isInterNormalLoaded = false;

    public void loadInterSplashPriority4SameTime(final Context context,
                                                 String idAdsHigh1,
                                                 String idAdsHigh2,
                                                 String idAdsHigh3,
                                                 String idAdsNormal,
                                                 long timeOut,
                                                 long timeDelay,
                                                 AdCallback adListener) {
        isInterHigh1Failed = false;
        isInterHigh2Loaded = false;
        isInterHigh3Loaded = false;
        isInterNormalLoaded = false;
        loadInterSplashHigh1(context, idAdsHigh1, timeOut, timeDelay, false, new AdCallback() {
            @Override
            public void onAdSplashReady() {
                super.onAdSplashReady();
                adListener.onAdSplashHigh1Ready();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                adListener.onAdPriorityFailedToLoad(i);
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                if (isInterHigh2Loaded && mInterSplashHigh2 != null) {
                    adListener.onAdSplashHigh2Ready();
                } else if (isInterHigh3Loaded && mInterSplashHigh3 != null) {
                    adListener.onAdSplashHigh3Ready();
                } else if (isInterHigh3Loaded && isInterNormalLoaded) {
                    adListener.onAdSplashNormalReady();
                } else {
                    // waiting for ads loaded
                    isInterHigh1Failed = true;
                }
            }
        });

        loadInterSplashHigh2(context, idAdsHigh2, timeOut, timeDelay, new AdCallback() {
            @Override
            public void onAdSplashReady() {
                super.onAdSplashReady();
                if (isInterHigh1Failed) {
                    adListener.onAdSplashHigh2Ready();
                } else {
                    isInterHigh2Loaded = true;
                }
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                if (isInterHigh1Failed) {
                    if (isInterHigh3Loaded && mInterSplashHigh3 != null) {
                        adListener.onAdSplashHigh3Ready();
                    } else if (isInterNormalLoaded && mInterSplashNormal != null) {
                        adListener.onAdSplashNormalReady();
                    } else {
                        isInterHigh2Loaded = true;
                    }
                } else {
                    isInterHigh2Loaded = true;
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                adListener.onAdPriorityFailedToLoad(i);
            }
        });

        loadInterSplashHigh3(context, idAdsHigh3, timeOut, timeDelay, new AdCallback() {
            @Override
            public void onAdSplashReady() {
                super.onAdSplashReady();
                if (isInterHigh1Failed && isInterHigh2Loaded) {
                    adListener.onAdSplashHigh3Ready();
                } else {
                    isInterHigh3Loaded = true;
                }
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                if (isInterHigh1Failed && isInterHigh2Loaded) {
                    if (isInterNormalLoaded && mInterSplashNormal != null) {
                        adListener.onAdSplashNormalReady();
                    } else {
                        isInterHigh3Loaded = true;
                    }
                } else {
                    isInterHigh3Loaded = true;
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                adListener.onAdPriorityFailedToLoad(i);
            }
        });

        loadInterSplashNormal(context, idAdsNormal, timeOut, timeDelay, new AdCallback() {
            @Override
            public void onAdSplashReady() {
                super.onAdSplashReady();
                if (isInterHigh1Failed && isInterHigh2Loaded && isInterHigh3Loaded) {
                    adListener.onAdSplashNormalReady();
                } else {
                    isInterNormalLoaded = true;
                }
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                if (isInterHigh1Failed && isInterHigh2Loaded && isInterHigh3Loaded) {
                    adListener.onNextAction();
                } else {
                    isInterNormalLoaded = true;
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                adListener.onAdPriorityFailedToLoad(i);
            }
        });
    }

    private boolean isFailedPriority = false;

    public void onShowSplashPriority4(AppCompatActivity activity, AdCallback adListener) {
        isFailedPriority = false;
        if (mInterSplashHigh1 != null) {
            onShowSplashHigh1(activity, new AdCallback() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    adListener.onAdClosed();
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    adListener.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    adListener.onAdImpression();
                }

                @Override
                public void onInterstitialShow() {
                    super.onInterstitialShow();
                }

                @Override
                public void onAdFailedToShow(@Nullable AdError adError) {
                    super.onAdFailedToShow(adError);
                    isShowLoadingSplash = false;
                    Log.i(TAG, "onAdFailedToShowPriority: ");
                    adListener.onAdPriorityFailedToShow(adError);
                    isFailedPriority = true;
                    onShowSplashHigh2(activity, new AdCallback() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            adListener.onAdClosed();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            adListener.onAdClicked();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adListener.onAdImpression();
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            isShowLoadingSplash = false;
                            adListener.onAdPriorityFailedToShow(adError);
                            isFailedPriority = true;
                            onShowSplashHigh3(activity, new AdCallback() {
                                @Override
                                public void onAdClosed() {
                                    super.onAdClosed();
                                    adListener.onAdClosed();
                                }

                                @Override
                                public void onAdClicked() {
                                    super.onAdClicked();
                                    adListener.onAdClicked();
                                }

                                @Override
                                public void onAdImpression() {
                                    super.onAdImpression();
                                    adListener.onAdImpression();
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    isShowLoadingSplash = false;
                                    adListener.onAdPriorityFailedToShow(adError);
                                    isFailedPriority = true;
                                    onShowSplashNormal(activity, new AdCallback() {
                                        @Override
                                        public void onAdClosed() {
                                            super.onAdClosed();
                                            adListener.onAdClosed();
                                        }

                                        @Override
                                        public void onAdClicked() {
                                            super.onAdClicked();
                                            adListener.onAdClicked();
                                        }

                                        @Override
                                        public void onAdImpression() {
                                            super.onAdImpression();
                                            adListener.onAdImpression();
                                        }

                                        @Override
                                        public void onAdFailedToShow(@Nullable AdError adError) {
                                            super.onAdFailedToShow(adError);
                                            isShowLoadingSplash = false;
                                            adListener.onAdFailedToShow(adError);
                                        }

                                        @Override
                                        public void onNextAction() {
                                            super.onNextAction();
                                            adListener.onNextAction();
                                        }
                                    });
                                }

                                @Override
                                public void onNextAction() {
                                    super.onNextAction();
                                    if (!isFailedPriority) {
                                        adListener.onNextAction();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            if (!isFailedPriority) {
                                adListener.onNextAction();
                            }
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        adListener.onNextAction();
                    }
                }
            });
        } else if (mInterSplashHigh2 != null) {
            onShowSplashHigh2(activity, new AdCallback() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    adListener.onAdClosed();
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    adListener.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    adListener.onAdImpression();
                }

                @Override
                public void onAdFailedToShow(@Nullable AdError adError) {
                    super.onAdFailedToShow(adError);
                    isShowLoadingSplash = false;
                    adListener.onAdPriorityFailedToShow(adError);
                    isFailedPriority = true;
                    onShowSplashHigh3(activity, new AdCallback() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            adListener.onAdClosed();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            adListener.onAdClicked();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adListener.onAdImpression();
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            isShowLoadingSplash = false;
                            adListener.onAdPriorityFailedToShow(adError);
                            isFailedPriority = true;
                            onShowSplashNormal(activity, new AdCallback() {
                                @Override
                                public void onAdClosed() {
                                    super.onAdClosed();
                                    adListener.onAdClosed();
                                }

                                @Override
                                public void onAdClicked() {
                                    super.onAdClicked();
                                    adListener.onAdClicked();
                                }

                                @Override
                                public void onAdImpression() {
                                    super.onAdImpression();
                                    adListener.onAdImpression();
                                }

                                @Override
                                public void onAdFailedToShow(@Nullable AdError adError) {
                                    super.onAdFailedToShow(adError);
                                    isShowLoadingSplash = false;
                                    adListener.onAdFailedToShow(adError);
                                }

                                @Override
                                public void onNextAction() {
                                    super.onNextAction();
                                    adListener.onNextAction();
                                }
                            });
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            if (!isFailedPriority) {
                                adListener.onNextAction();
                            }
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        adListener.onNextAction();
                    }
                }
            });
        } else if (mInterSplashHigh3 != null) {
            onShowSplashHigh3(activity, new AdCallback() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    adListener.onAdClosed();
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    adListener.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    adListener.onAdImpression();
                }

                @Override
                public void onAdFailedToShow(@Nullable AdError adError) {
                    super.onAdFailedToShow(adError);
                    isShowLoadingSplash = false;
                    adListener.onAdPriorityFailedToShow(adError);
                    isFailedPriority = true;
                    onShowSplashNormal(activity, new AdCallback() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            adListener.onAdClosed();
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            adListener.onAdClicked();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            adListener.onAdImpression();
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            isShowLoadingSplash = false;
                            adListener.onAdFailedToShow(adError);
                        }

                        @Override
                        public void onNextAction() {
                            super.onNextAction();
                            adListener.onNextAction();
                        }
                    });
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    if (!isFailedPriority) {
                        adListener.onNextAction();
                    }
                }
            });
        } else if (mInterSplashNormal != null) {
            onShowSplashNormal(activity, new AdCallback() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    adListener.onAdClosed();
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    adListener.onAdClicked();
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    adListener.onAdImpression();
                }

                @Override
                public void onAdFailedToShow(@Nullable AdError adError) {
                    super.onAdFailedToShow(adError);
                    isShowLoadingSplash = false;
                    adListener.onAdFailedToShow(adError);
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    adListener.onNextAction();
                }
            });
        } else {
            adListener.onNextAction();
        }
    }

    private InterstitialAd mInterSplashHigh1;
    private boolean isTimeDelayHigh1 = false;
    private boolean isTimeoutHigh1 = false;
    private Handler handlerTimeoutHigh1;
    private Runnable rdTimeoutHigh1;

    private void loadInterSplashHigh1(final Context context, String id, long timeOut, long timeDelay, boolean showSplashIfReady, AdCallback adListener) {
        isTimeDelayHigh1 = false;
        isTimeoutHigh1 = false;
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (mInterSplashHigh1 != null) {
                if (showSplashIfReady) {
                    onShowSplashHigh1((AppCompatActivity) context, adListener);
                } else {
                    adListener.onAdSplashReady();
                }
                return;
            }
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeoutHigh1 = new Handler();
            rdTimeoutHigh1 = () -> {
                Log.e(TAG, "loadSplashInterstitialAdsPriority: on timeout");
                isTimeoutHigh1 = true;
                if (mInterSplashHigh1 != null) {
                    Log.i(TAG, "loadSplashInterstitialAdsPriority:show ad on timeout ");
                    if (showSplashIfReady)
                        onShowSplashHigh1((AppCompatActivity) context, adListener);
                    else
                        adListener.onAdSplashReady();
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeoutHigh1.postDelayed(rdTimeoutHigh1, timeOut);
        }
        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                if (isTimeoutHigh1) {
                    return;
                }
                if (interstitialAd != null) {
                    mInterSplashHigh1 = interstitialAd;
                    mInterSplashHigh1.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    mInterSplashHigh1.getAdUnitId(),
                                    mInterSplashHigh1.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);

                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterSplashHigh1.getAdUnitId(), tokenAdjust);
                            }
                        }
                    });

                    if (isTimeDelayHigh1) {
                        if (showSplashIfReady)
                            onShowSplashHigh1((AppCompatActivity) context, adListener);
                        else
                            adListener.onAdSplashReady();
                        Log.i(TAG, "loadSplashInterstitialAdsPriority:show ad on loaded ");
                    }
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isTimeoutHigh1) {
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeoutHigh1 != null && rdTimeoutHigh1 != null) {
                        handlerTimeoutHigh1.removeCallbacks(rdTimeoutHigh1);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitialAdsPriority: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                }
            }
        });
    }

    private void onShowSplashHigh1(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;
        if (mInterSplashHigh1 == null) {
            adListener.onNextAction();
            return;
        }

        if (handlerTimeoutHigh1 != null && rdTimeoutHigh1 != null) {
            handlerTimeoutHigh1.removeCallbacks(rdTimeoutHigh1);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterSplashHigh1.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                isShowInterstitialSplashSuccess = true;
                AppOpenManager.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().disableAppResume();
                isShowLoadingSplash = true;
                mInterSplashHigh1 = null;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                AppOpenManager.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().enableAppResume();
                mInterSplashHigh1 = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                mInterSplashHigh1 = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adListener != null) {
                    adListener.onAdClicked();
                }
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                ERainLogEventManager.logClickAdsEvent(context, mInterSplashHigh1.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                try {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    dialog = null;
                    e.printStackTrace();
                }
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }

            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterSplashHigh1 != null) {
                        Log.i(TAG, "start show InterstitialAd " + activity.getLifecycle().getCurrentState().name() + "/" + ProcessLifecycleOwner.get().getLifecycle().getCurrentState().name());
                        mInterSplashHigh1.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    Log.e(TAG, "onShowSplash:   show fail in background after show loading ad");
                    adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "MiaAd"));
                }
            }, 800);
        } else {
            adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "MiaAd"));
            Log.e(TAG, "onShowSplash: fail on background");
            isShowLoadingSplash = false;
        }

    }

    private InterstitialAd mInterSplashHigh2;
    private boolean isTimeDelayHigh2 = false;
    private boolean isTimeoutHigh2 = false;
    private Handler handlerTimeoutHigh2;
    private Runnable rdTimeoutHigh2;

    private void loadInterSplashHigh2(final Context context, String id, long timeOut, long timeDelay, AdCallback adListener) {
        isTimeDelayHigh2 = false;
        isTimeoutHigh2 = false;
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(() -> {
            //check delay show ad splash
            if (mInterSplashHigh2 != null) {
                Log.i(TAG, "mInterSplashHigh2:show ad on delay ");
                adListener.onAdSplashReady();
                return;
            }
            Log.i(TAG, "mInterSplashHigh2: delay validate");
            isTimeDelayHigh2 = true;
        }, timeDelay);
        if (timeOut > 0) {
            handlerTimeoutHigh2 = new Handler();
            rdTimeoutHigh2 = () -> {
                Log.e(TAG, "loadSplashInterstitialAdsMedium: on timeout");
                isTimeoutHigh2 = true;
                if (mInterSplashHigh2 != null) {
                    adListener.onAdSplashReady();
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeoutHigh2.postDelayed(rdTimeoutHigh2, timeOut);
        }

        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                if (isTimeoutHigh2) {
                    return;
                }
                if (interstitialAd != null) {
                    mInterSplashHigh2 = interstitialAd;
                    mInterSplashHigh2.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    mInterSplashHigh2.getAdUnitId(),
                                    mInterSplashHigh2.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);

                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterSplashHigh2.getAdUnitId(), tokenAdjust);
                            }
                        }
                    });

                    if (isTimeDelayHigh2) {
                        adListener.onAdSplashReady();
                        Log.i(TAG, "mInterSplashHigh2:show ad on loaded ");
                    }
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isTimeoutHigh2)
                    return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeoutHigh2 != null && rdTimeoutHigh2 != null) {
                        handlerTimeoutHigh2.removeCallbacks(rdTimeoutHigh2);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitialAdsMedium: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                }
            }
        });
    }

    private void onShowSplashHigh2(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;
        if (mInterSplashHigh2 == null) {
            isShowLoadingSplash = false;
            adListener.onAdFailedToShow(new AdError(0, "mInterSplashHigh2 null", "MiaAd"));
            adListener.onNextAction();
            return;
        }

        if (handlerTimeoutHigh2 != null && rdTimeoutHigh2 != null) {
            handlerTimeoutHigh2.removeCallbacks(rdTimeoutHigh2);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterSplashHigh2.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                isShowInterstitialSplashSuccess = true;
                AppOpenManager.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().disableAppResume();
                isShowLoadingSplash = false;
                mInterSplashHigh2 = null;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                Log.d(TAG, " Splash:onAdDismissedFullScreenContent ");
                AppOpenManager.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().enableAppResume();
                mInterSplashHigh2 = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                Log.e(TAG, "Splash onAdFailedToShowFullScreenContent: " + adError.getMessage());
                mInterSplashHigh2 = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adListener != null) {
                    adListener.onAdClicked();
                }
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                ERainLogEventManager.logClickAdsEvent(context, mInterSplashHigh2.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                try {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    dialog = null;
                    e.printStackTrace();
                }
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterSplashHigh2 != null) {
                        Log.i(TAG, "start show InterstitialAd " + activity.getLifecycle().getCurrentState().name() + "/" + ProcessLifecycleOwner.get().getLifecycle().getCurrentState().name());
                        mInterSplashHigh2.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    Log.e(TAG, "onShowSplash: show fail in background after show loading ad");
                    adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "AperoAd"));
                }
            }, 800);

        } else {
            adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "AperoAd"));
            Log.e(TAG, "onShowSplash: fail on background");
            isShowLoadingSplash = false;
        }
    }

    private InterstitialAd mInterSplashHigh3;
    private boolean isTimeDelayHigh3 = false;
    private boolean isTimeoutHigh3 = false;
    private Handler handlerTimeoutHigh3;
    private Runnable rdTimeoutHigh3;

    private void loadInterSplashHigh3(final Context context, String id, long timeOut, long timeDelay, AdCallback adListener) {
        isTimeDelayHigh3 = false;
        isTimeoutHigh3 = false;
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(() -> {
            //check delay show ad splash
            if (mInterSplashHigh3 != null) {
                Log.i(TAG, "mInterSplashHigh3:show ad on delay ");
                adListener.onAdSplashReady();
                return;
            }
            Log.i(TAG, "mInterSplashHigh3: delay validate");
            isTimeDelayHigh3 = true;
        }, timeDelay);
        if (timeOut > 0) {
            handlerTimeoutHigh3 = new Handler();
            rdTimeoutHigh3 = () -> {
                Log.e(TAG, "loadSplashInterstitialAdsMedium: on timeout");
                isTimeoutHigh3 = true;
                if (mInterSplashHigh3 != null) {
                    adListener.onAdSplashReady();
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeoutHigh3.postDelayed(rdTimeoutHigh3, timeOut);
        }

        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                if (isTimeoutHigh3) {
                    return;
                }
                if (interstitialAd != null) {
                    mInterSplashHigh3 = interstitialAd;
                    mInterSplashHigh3.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    mInterSplashHigh3.getAdUnitId(),
                                    mInterSplashHigh3.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);

                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterSplashHigh3.getAdUnitId(), tokenAdjust);
                            }
                        }
                    });

                    if (isTimeDelayHigh3) {
                        adListener.onAdSplashReady();
                        Log.i(TAG, "mInterSplashHigh3:show ad on loaded ");
                    }
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isTimeoutHigh3)
                    return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeoutHigh3 != null && rdTimeoutHigh3 != null) {
                        handlerTimeoutHigh3.removeCallbacks(rdTimeoutHigh3);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitialAdsMedium: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                }
            }
        });
    }

    private void onShowSplashHigh3(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;
        if (mInterSplashHigh3 == null) {
            isShowLoadingSplash = false;
            adListener.onAdFailedToShow(new AdError(0, "mInterSplashHigh3 null", "MiaAd"));
            adListener.onNextAction();
            return;
        }

        if (handlerTimeoutHigh3 != null && rdTimeoutHigh3 != null) {
            handlerTimeoutHigh3.removeCallbacks(rdTimeoutHigh3);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterSplashHigh3.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                isShowInterstitialSplashSuccess = true;
                AppOpenManager.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().disableAppResume();
                isShowLoadingSplash = false;
                mInterSplashHigh3 = null;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                Log.d(TAG, " Splash:onAdDismissedFullScreenContent ");
                AppOpenManager.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().enableAppResume();
                mInterSplashHigh3 = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                Log.e(TAG, "Splash onAdFailedToShowFullScreenContent: " + adError.getMessage());
                mInterSplashHigh3 = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adListener != null) {
                    adListener.onAdClicked();
                }
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                ERainLogEventManager.logClickAdsEvent(context, mInterSplashHigh3.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                try {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    dialog = null;
                    e.printStackTrace();
                }
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                } catch (Exception e) {
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterSplashHigh3 != null) {
                        Log.i(TAG, "start show InterstitialAd " + activity.getLifecycle().getCurrentState().name() + "/" + ProcessLifecycleOwner.get().getLifecycle().getCurrentState().name());
                        mInterSplashHigh3.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    Log.e(TAG, "onShowSplash: show fail in background after show loading ad");
                    adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "AperoAd"));
                }
            }, 800);

        } else {
            adListener.onAdFailedToShow(new AdError(0, " show fail in background after show loading ad", "AperoAd"));
            Log.e(TAG, "onShowSplash: fail on background");
            isShowLoadingSplash = false;
        }
    }

    private InterstitialAd mInterSplashNormal;
    private boolean isTimeDelayNormal = false;
    private boolean isTimeoutNormal = false;
    private Handler handlerTimeoutNormal;
    private Runnable rdTimeoutNormal;

    private void loadInterSplashNormal(final Context context, String id, long timeOut, long timeDelay, AdCallback adListener) {
        isTimeDelayNormal = false;
        isTimeoutNormal = false;
        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onNextAction();
            }
            return;
        }
        new Handler().postDelayed(() -> {
            //check delay show ad splash
            if (mInterSplashNormal != null) {
                Log.i(TAG, "loadInterSplashNormal:show ad on delay ");
                adListener.onAdSplashReady();
                return;
            }
            Log.i(TAG, "loadInterSplashNormal: delay validate");
            isTimeDelayNormal = true;
        }, timeDelay);
        if (timeOut > 0) {
            handlerTimeoutNormal = new Handler();
            rdTimeoutNormal = () -> {
                Log.e(TAG, "loadSplashInterstitialAdsMedium: on timeout");
                isTimeoutNormal = true;
                if (mInterSplashNormal != null) {
                    adListener.onAdSplashReady();
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeoutNormal.postDelayed(rdTimeoutNormal, timeOut);
        }

        isShowLoadingSplash = true;
        getInterstitialAds(context, id, new AdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                if (isTimeoutNormal) {
                    return;
                }
                if (interstitialAd != null) {
                    mInterSplashNormal = interstitialAd;
                    mInterSplashNormal.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(@NonNull AdValue adValue) {
                            ERainLogEventManager.logPaidAdImpression(context,
                                    adValue,
                                    mInterSplashNormal.getAdUnitId(),
                                    mInterSplashNormal.getResponseInfo()
                                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);

                            if (tokenAdjust != null) {
                                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterSplashNormal.getAdUnitId(), tokenAdjust);
                            }
                        }
                    });

                    if (isTimeDelayNormal) {
                        adListener.onAdSplashReady();
                        Log.i(TAG, "loadInterSplashNormal:show ad on loaded ");
                    }
                }
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isTimeoutNormal)
                    return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeoutNormal != null && rdTimeoutNormal != null) {
                        handlerTimeoutNormal.removeCallbacks(rdTimeoutNormal);
                    }
                    if (i != null)
                        Log.e(TAG, "loadSplashInterstitialAdsMedium: load fail " + i.getMessage());
                    adListener.onAdFailedToLoad(i);
                }
            }
        });
    }

    public void onShowSplashNormal(AppCompatActivity activity, AdCallback adListener) {
        isShowLoadingSplash = true;

        if (mInterSplashNormal == null) {
            adListener.onNextAction();
            return;
        }

        mInterSplashNormal.setOnPaidEventListener(adValue -> {
            ERainLogEventManager.logPaidAdImpression(context,
                    adValue,
                    mInterSplashNormal.getAdUnitId(),
                    mInterSplashNormal.getResponseInfo()
                            .getMediationAdapterClassName(), AdType.INTERSTITIAL);
            if (tokenAdjust != null) {
                ERainLogEventManager.logPaidAdjustWithToken(adValue, mInterSplashNormal.getAdUnitId(), tokenAdjust);
            }
        });

        if (handlerTimeoutNormal != null && rdTimeoutNormal != null) {
            handlerTimeoutNormal.removeCallbacks(rdTimeoutNormal);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }

        mInterSplashNormal.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                isShowInterstitialSplashSuccess = true;
                AppOpenManager.getInstance().setInterstitialShowing(true);
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AppOpenManager.getInstance().setInterstitialShowing(false);
                mInterSplashNormal = null;
                if (adListener != null) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                isShowLoadingSplash = false;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                mInterSplashNormal = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }

                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (disableAdResumeWhenClickAds)
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                ERainLogEventManager.logClickAdsEvent(context, mInterSplashNormal.getAdUnitId());
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                    AppOpenManager.getInstance().setInterstitialShowing(true);
                } catch (Exception e) {
                    assert adListener != null;
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (mInterSplashNormal != null) {
                        mInterSplashNormal.show(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    assert adListener != null;
                    adListener.onAdFailedToShow(new AdError(0, "Show fail in background after show loading ad", "LuanDT"));
                }
            }, 800);

        } else {
            isShowLoadingSplash = false;
        }
    }

    public void onCheckShowSplashPriority4WhenFail(AppCompatActivity activity, AdCallback callback, int timeDelay) {
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            if (!isShowLoadingSplash() && (mInterSplashHigh1 != null || mInterSplashHigh2 != null || mInterSplashHigh3 != null || mInterSplashNormal != null)) {
                onShowSplashPriority4(activity, new AdCallback() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        Log.i(TAG, "onAdClosed: ");
                        callback.onAdClosed();
                    }

                    @Override
                    public void onAdPriorityFailedToShow(@Nullable AdError adError) {
                        super.onAdPriorityFailedToShow(adError);
                        Log.e(TAG, "onAdPriorityFailedToShow: ");
                        callback.onAdPriorityFailedToShow(adError);
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        Log.e(TAG, "onAdFailedToShow: ");
                        callback.onAdFailedToShow(adError);
                        isShowLoadingSplash = false;
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        Log.i(TAG, "onNextAction: ");
                        callback.onNextAction();
                    }
                });
            } else {
                callback.onNextAction();

            }
        }, timeDelay);
    }
}
