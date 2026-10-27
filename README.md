<h1>⛔ DO NOT download the "ads" module for editing.</h1>

<h1><b> Link Example: https://github.com/Infinity-Technologies-Global/Example-AdLogic-Partner/ </b></h1>

# ERain Studio
- Admob
- Mediation Admob (Facebook, Applovin, Vungle, Pangle, Mintegral)
- Adjust
- Firebase auto log tracking event, tROAS
# Import Module
~~~
        maven { 
            url "https://jitpack.io" 
        }
        maven {
            url 'https://artifact.bytedance.com/repository/pangle/'
        }
        maven {
            url 'https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea'
        }
        maven {
            url = uri("https://android-sdk.is.com/")
        }
    implementation 'com.github.trongluan99:ERain-Studio:2.0'
    implementation 'com.facebook.shimmer:shimmer:0.5.0'
    implementation 'com.google.android.gms:play-services-ads:25.3.0'
    implementation 'androidx.multidex:multidex:2.0.1'
~~~
# Setup environment with id ads for project
~~~    
      productFlavors {
      appDev {
                manifestPlaceholders = [ad_app_id: "ca-app-pub-3940256099942544~3347511713"]
                buildConfigField "String", "inter", "\"ca-app-pub-3940256099942544/1033173712\""
                buildConfigField "String", "banner", "\"ca-app-pub-3940256099942544/6300978111\""
                buildConfigField "String", "native", "\"ca-app-pub-3940256099942544/2247696110\""
                buildConfigField "String", "open_resume", "\"ca-app-pub-3940256099942544/3419835294\""
                buildConfigField "String", "RewardedAd", "\"ca-app-pub-3940256099942544/5224354917\""
                buildConfigField "Boolean", "build_debug", "true"
           }
       appProd {
            // ADS CONFIG BEGIN (required)
                manifestPlaceholders = [ad_app_id: "ca-app-pub-3940256099942544~3347511713"]
                buildConfigField "String", "inter", "\"ca-app-pub-3940256099942544/1033173712\""
                buildConfigField "String", "banner", "\"ca-app-pub-3940256099942544/6300978111\""
                buildConfigField "String", "native", "\"ca-app-pub-3940256099942544/2247696110\""
                buildConfigField "String", "open_resume", "\"ca-app-pub-3940256099942544/3419835294\""
                buildConfigField "String", "RewardedAd", "\"ca-app-pub-3940256099942544/5224354917\""
                buildConfigField "Boolean", "build_debug", "false"
            // ADS CONFIG END (required)
           }
      }
~~~
**AndroidManifest.xml**
~~~
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="${ad_app_id}" />

        // Config SDK Facebook
        <meta-data
            android:name="com.facebook.sdk.ApplicationId"
            android:value="@string/facebook_app_id" />
        <meta-data
            android:name="com.facebook.sdk.ClientToken"
            android:value="@string/facebook_client_token" />

        <meta-data android:name="com.facebook.sdk.AutoInitEnabled"
            android:value="true"/>
        <meta-data android:name="com.facebook.sdk.AutoLogAppEventsEnabled"
            android:value="true"/>

        <meta-data android:name="com.facebook.sdk.AdvertiserIDCollectionEnabled"
            android:value="true"/>
~~~

# Class Application
~~~
public class App extends AdsMultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        initAds();
        initBilling();
    }

    private void initAds() {
        String environment = BuildConfig.DEBUG ? ERainAdConfig.ENVIRONMENT_DEVELOP : ERainAdConfig.ENVIRONMENT_PRODUCTION;
        mERainAdConfig = new ERainAdConfig(this, environment);

        AdjustConfig adjustConfig = new AdjustConfig(true,getString(R.string.adjust_token));
        mERainAdConfig.setAdjustConfig(adjustConfig);
        mERainAdConfig.setFacebookClientToken(getString(R.string.facebook_client_token));
        mERainAdConfig.setAdjustTokenTiktok(getString(R.string.tiktok_token));

        mERainAdConfig.setIdAdResume(BuildConfig.open_resume);

        ERainAd.getInstance().init(this, mERainAdConfig);
        Admob.getInstance().setDisableAdResumeWhenClickAds(true);
        Admob.getInstance().setOpenActivityAfterShowInterAds(true);
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity.class);
    }

    private void initBilling(){
        List<String> listIAP = new ArrayList<>();
        listIAP.add("android.test.purchased");
        List<String> listSub = new ArrayList<>();
        AppPurchase.getInstance().initBilling(this, listIAP, listSub);
    }
}
~~~

# Ad Splash Interstitial
~~~   
ERainAd.getInstance().loadSplashInterstitialAds(this, BuildConfig.ad_interstitial_splash, 25000, 5000, new AdCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
~~~   

## Sequential Splash: resume after background

Use the following integration when loading Splash ads sequentially (High first, then Normal if loading fails). Choose either Interstitial or App Open for your Splash screen.

The resume helpers below are part of the current source changes. Use a library build containing these APIs; the dependency version shown above does not by itself confirm their availability.

If the app is in the background when a show is attempted, the library keeps the loaded ad and calls `onAdFailedToShow()` with the message `Show fail in background`. It does not call `onNextAction()` in this case. Call the matching resume helper from `onResume()` to retry when the app returns.

- Navigate to the next screen in `onNextAction()`, not in `onAdFailedToShow()`, so a background interruption can recover. Ad dismissal and SDK show failures already lead to `onNextAction()`.
- Load once after your existing SDK initialization and consent flow. Do not restart loading from `onResume()`.
- The resume helper checks the cached ad after `timeDelay` milliseconds. It does nothing if there is no cached ad or a show is already in progress; it does not load another ad.
- Interstitial recovery also waits until the configured initial `timeDelay` has elapsed. Its load timeout is cancelled when an ad loads successfully, preventing another ready callback from that timeout.

The snippets below belong inside a `SplashActivity` extending `AppCompatActivity`. Both alternatives use this navigation callback (`Intent`, `MainActivity` and `AdCallback` must be available in your app):

```java
private final AdCallback splashNavigation = new AdCallback() {
    @Override
    public void onNextAction() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
};
```

### Interstitial Sequential

Use `com.ads.module.ads.ERainAd` and `com.ads.module.funtion.AdCallback`. Call `loadSequentialInterstitial(idHigh, idNormal)` once with your configured Interstitial ad unit IDs. All durations below are milliseconds: a 25-second load timeout, a 5-second initial delay and a 1-second resume check delay.

```java
private void loadSequentialInterstitial(String idHigh, String idNormal) {
    ERainAd.getInstance().loadInterSplashPriority2Sequentially(
            this, idHigh, idNormal, 25000, 5000, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    ERainAd.getInstance().onShowSplashPrioritySequentially(
                            SplashActivity.this, splashNavigation);
                }

                @Override
                public void onNextAction() {
                    splashNavigation.onNextAction();
                }
            });
}

@Override
protected void onResume() {
    super.onResume();
    ERainAd.getInstance().onCheckShowSplashPrioritySequentiallyWhenFail(
            this, splashNavigation, 1000);
}
```

Use `onCheckShowSplashPrioritySequentiallyWhenFail()` for this flow instead of the older `onCheckShowSplashWhenFail()` helper.

### App Open Sequential

Use `com.ads.module.admob.AppOpenManager` and `com.ads.module.funtion.AdCallback`. Call `loadSequentialAppOpen(idHigh, idNormal)` once with your configured App Open ad unit IDs. This alternative uses a 25-second load timeout and a 1-second resume check delay. `showOpenSplashSequentially()` also has an internal 800 ms delay before checking foreground state and showing the ad.

```java
private void loadSequentialAppOpen(String idHigh, String idNormal) {
    AppOpenManager.getInstance().loadAndShowOpenSplash2Sequentially(
            this, idHigh, idNormal, 25000, new AdCallback() {
                @Override
                public void onAdSplashReady() {
                    AppOpenManager.getInstance().showOpenSplashSequentially(
                            SplashActivity.this, splashNavigation);
                }

                @Override
                public void onNextAction() {
                    splashNavigation.onNextAction();
                }
            });
}

@Override
protected void onResume() {
    super.onResume();
    AppOpenManager.getInstance().onCheckShowOpenSplashSequentiallyWhenFail(
            this, splashNavigation, 1000);
}
```

The [sample SplashActivity](app/src/main/java/com/erain/module/SplashActivity.java) uses the App Open Sequential flow.

To verify the integration, send the app to the background while a Splash ad is loading or waiting to show, then return. A cached ad should show once, and closing it should open the next screen once. Also check normal launch, load failure/timeout and returning from an ad click.

# Ad Banner
~~~   
ERainAd.getInstance().loadBanner(this, BuildConfig.ad_banner);
~~~   

# Ad Collapsible Banner
~~~   
ERainAd.getInstance().loadCollapsibleBanner(this, BuildConfig.ad_banner, AppConstant.CollapsibleGravity.BOTTOM, new AdCallback());
~~~   

# Native: Load And Show
~~~   
ERainAd.getInstance().loadNativeAd(this, BuildConfig.ad_native, R.layout.native_large, frAds, shimmerAds, new AdCallback() {
            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                frAds.removeAllViews();
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                frAds.removeAllViews();
            }
        });
~~~   

# Native: Load
~~~   
private ApNativeAd mApNativeAd;
ERainAd.getInstance().loadNativeAdResultCallback(this, BuildConfig.ad_native, R.layout.native_large, new AdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);

                mApNativeAd = nativeAd;
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);

                mApNativeAd = null;
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);

                mApNativeAd = null;
            }
        });
~~~   

# Native: Show
~~~   
if (mApNativeAd != null) {
            ERainAd.getInstance().populateNativeAdView(this, mApNativeAd, frAds, shimmerAds);
        }
~~~

# Reward: Load
~~~
private RewardedAd rewardedAds;
ERainAd.getInstance().initRewardAds(this, BuildConfig.ad_reward, new AdCallback() {
                @Override
                public void onRewardAdLoaded(RewardedAd rewardedAd) {
                    super.onRewardAdLoaded(rewardedAd);
                    rewardedAds = rewardedAd;
                    Toast.makeText(MainActivity.this, "Ads Ready", Toast.LENGTH_SHORT).show();
                }
            });
~~~
# Reward: Show
~~~
private boolean isEarn = false;
btnShowReward.setOnClickListener(v -> {
            isEarn = false;
            ERainAd.getInstance().showRewardAds(MainActivity.this, rewardedAds, new RewardCallback() {
                @Override
                public void onUserEarnedReward(RewardItem var1) {
                    isEarn = true;
                }

                @Override
                public void onRewardedAdClosed() {
                    if (isEarn) {
                        // action intent
                    }
                }

                @Override
                public void onRewardedAdFailedToShow(int codeError) {

                }

                @Override
                public void onAdClicked() {

                }
            });
        });
~~~

# In-App Purchase
~~~
AppPurchase.getInstance().setPurchaseListener(new PurchaseListener() {
            @Override
            public void onProductPurchased(String productId, String transactionDetails) {
                Toast.makeText(MainActivity.this, "onProductPurchased", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void displayErrorMessage(String errorMsg) {
                Toast.makeText(MainActivity.this, "displayErrorMessage", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserCancelBilling() {
                Toast.makeText(MainActivity.this, "onUserCancelBilling", Toast.LENGTH_SHORT).show();
            }
        });

btnIap.setOnClickListener(v -> AppPurchase.getInstance().purchase(MainActivity.this, "android.test.purchased"));
~~~























