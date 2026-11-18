# 🚀 Hướng Dẫn & Code Mẫu Tích Hợp ERain-Studio Ads SDK (GMA Next-Gen SDK)

Tài liệu này cung cấp toàn bộ mã nguồn ví dụ (Example Code) chi tiết cho từng định dạng quảng cáo và luồng tích hợp của thư viện **ERain-Studio** theo chuẩn **Google Mobile Ads (GMA) Next-Gen SDK** (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0`).

---

## 📋 Mục Lục

1. [Cấu Hình Dự Án (Gradle & AndroidManifest)](#1-cấu-hình-dự-án-gradle--androidmanifest)
2. [Khởi Tạo Thư Viện Trong Application](#2-khởi-tạo-thư-viện-trong-application)
3. [Màn Hình Chào Splash (Sequential High-Floor Ads)](#3-màn-hình-chào-splash-sequential-high-floor-ads)
4. [Banner Ads (Standard, Collapsible, Inline)](#4-banner-ads-standard-collapsible-inline)
5. [Native Ads (Chuẩn Next-Gen Layout & Preload)](#5-native-ads-chuẩn-next-gen-layout--preload)
6. [Interstitial Ads (Standard & Priority)](#6-interstitial-ads-standard--priority)
7. [Rewarded Ads & Rewarded Interstitial](#7-rewarded-ads--rewarded-interstitial)
8. [Picture-in-Picture (PIP) Ads](#8-picture-in-picture-pip-ads)
9. [Swipeable In-Feed Interstitial Ads](#9-swipeable-in-feed-interstitial-ads)
10. [In-App Purchase & Subscription (Google Play Billing 9.0)](#10-in-app-purchase--subscription-google-play-billing-90)
11. [Công Cụ Debug Ads & Bidding Inspector (ERainAdDebugManager)](#11-công-cụ-debug-ads--bidding-inspector-erainaddebugmanager)

---

## 1. Cấu Hình Dự Án (Gradle & AndroidManifest)

### 1.1. Khai báo Repository (`settings.gradle` hoặc root `build.gradle`)

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven { url "https://artifact.bytedance.com/repository/pangle/" }
        maven { url "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea" }
        maven { url = uri("https://android-sdk.is.com/") }
    }
}
```

### 1.2. Khai báo Dependencies (`app/build.gradle`)

```groovy
dependencies {
    // ERain-Studio Ads Library (Next-Gen)
    implementation project(':ads') // Hoặc implementation 'com.github.trongluan99:ERain-Studio:7.0.0'

    // Google Mobile Ads Next-Gen SDK
    implementation 'com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0'

    // AndroidX & UI
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.google.android.material:material:1.14.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.1'
    implementation 'androidx.viewpager2:viewpager2:1.1.0'
    implementation 'androidx.multidex:multidex:2.0.1'
    implementation 'com.facebook.shimmer:shimmer:0.5.0'

    // In-App Review & Update (tùy chọn)
    implementation 'com.google.android.play:review:2.0.2'
    implementation 'com.google.android.play:review-ktx:2.0.2'
}
```

> [!IMPORTANT]
> **Loại trừ thư viện cũ**: Nếu dự án có dùng các module bên thứ ba kéo theo `com.google.android.gms:play-services-ads`, hãy thêm `exclude group: 'com.google.android.gms', module: 'play-services-ads'` để tránh xung đột class.

### 1.3. Khai báo `AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />

    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.MaterialComponents.Light.NoActionBar">

        <!-- Splash Activity -->
        <activity
            android:name=".SplashActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true" />

        <!-- AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="${ad_app_id}" />

        <!-- Tắt Native Ad Debugger Overlay để tránh che UI -->
        <meta-data
            android:name="com.google.android.gms.ads.flag.NATIVE_AD_DEBUGGER_ENABLED"
            android:value="false" />

        <!-- Facebook SDK (Nếu có dùng) -->
        <meta-data
            android:name="com.facebook.sdk.ApplicationId"
            android:value="@string/facebook_app_id" />
        <meta-data
            android:name="com.facebook.sdk.ClientToken"
            android:value="@string/facebook_client_token" />
        <meta-data
            android:name="com.facebook.sdk.AutoInitEnabled"
            android:value="true" />
    </application>
</manifest>
```

---

## 2. Khởi Tạo Thư Viện Trong Application

Lớp `Application` kế thừa từ `AdsMultiDexApplication`. Toàn bộ quá trình khởi tạo AdMob Next-Gen và các Mediation Adapter chạy hoàn toàn trên **Background Thread**, không làm đơ UI thread.

### Kotlin (`App.kt`):
```kotlin
package com.erain.module

import com.ads.module.admob.Admob
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.application.AdsMultiDexApplication
import com.ads.module.billing.AppPurchase
import com.ads.module.config.AdjustConfig
import com.ads.module.config.ERainAdConfig

class App : AdsMultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        initAds()
        initBilling()
    }

    private fun initAds() {
        val environment = if (BuildConfig.DEBUG) {
            ERainAdConfig.ENVIRONMENT_DEVELOP
        } else {
            ERainAdConfig.ENVIRONMENT_PRODUCTION
        }

        val config = ERainAdConfig(this, environment).apply {
            adjustConfig = AdjustConfig(true, getString(R.string.adjust_token))
            facebookClientToken = getString(R.string.facebook_client_token)
            adjustTokenTiktok = getString(R.string.tiktok_token)
            
            // Thiết lập ID Resume Ad (High Floor & Normal Floor)
            setIdAdResume(BuildConfig.ad_appopen_resume_high, BuildConfig.ad_appopen_resume)
            
            // Bật/tắt thanh công cụ Debug Ads Inspector nổi (chỉ hiện trên bản Debug)
            isShowFloatingDebugButton = true
        }
        mERainAdConfig = config

        // Khởi tạo SDK Next-Gen
        ERainAd.getInstance().init(this, config)

        // Cấu hình hành vi hiển thị
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(false)
        
        // Không hiện Open Resume Ad khi ở SplashActivity
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
    }

    private fun initBilling() {
        val listIAP = arrayListOf("android.test.purchased") // Danh sách ID IAP 1 lần
        val listSub = arrayListOf<String>()                 // Danh sách ID Thuê bao
        AppPurchase.getInstance().initBilling(this, listIAP, listSub)
    }
}
```

---

## 3. Màn Hình Chào Splash (Sequential High-Floor Ads)

Cơ chế tải Splash tuần tự thông minh:
1. Kiểm tra trạng thái VIP (`AppPurchase.isPurchased`): Nếu đã mua -> bỏ qua Splash vào thẳng `MainActivity`.
2. Nếu chưa mua -> Tải tuần tự **High-Floor -> Normal Floor** với timeout an toàn.
3. Khi người dùng bấm Home/ẩn app lúc quảng cáo chưa hiện xong -> Tự động khôi phục trong `onResume()`.

### Kotlin (`SplashActivity.kt`):
```kotlin
package com.erain.module

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ads.module.ads.ERainAd
import com.ads.module.billing.AppPurchase
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.BillingListener
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var isNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Kiểm tra Billing trước khi quyết định tải quảng cáo
        AppPurchase.getInstance().setBillingListener(
            BillingListener { code ->
                runOnUiThread {
                    if (AppPurchase.getInstance().isPurchased) {
                        Log.d("Splash", "User is VIP -> Skip Splash Ads")
                        navigateToMain()
                    } else {
                        Log.d("Splash", "User not VIP -> Load Sequential Splash Ads")
                        loadSplashAds()
                    }
                }
            },
            5000 // Timeout kiểm tra billing tối đa 5s
        )
    }

    private fun loadSplashAds() {
        if (isNavigated || isFinishing || isDestroyed) return

        // Tải Splash Interstitial tuần tự: High Floor -> Normal Floor
        ERainAd.getInstance().loadSplashInterstitialAdsHighFloor(
            this,
            BuildConfig.ad_interstitial_splash, // ID High Floor
            BuildConfig.ad_interstitial_splash, // ID Normal Floor
            30000L,                            // Timeout tải (30s)
            2000L,                             // Delay hiển thị (2s)
            true,                              // Tự động show khi sẵn sàng
            object : AdCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("Splash", "Splash Ad loaded successfully")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    Log.e("Splash", "Splash Ad failed to load: ${i?.message}")
                }

                override fun onAdFailedToShow(adError: FullScreenContentError?) {
                    super.onAdFailedToShow(adError)
                    Log.e("Splash", "Splash Ad failed to show: ${adError?.message}")
                }

                override fun onNextAction() {
                    super.onNextAction()
                    Log.d("Splash", "onNextAction -> Navigate to MainActivity")
                    navigateToMain()
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun navigateToMain() {
        if (isNavigated || isFinishing || isDestroyed) return
        isNavigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

---

## 4. Banner Ads (Standard, Collapsible, Inline)

### 4.1. Standard Adaptive Banner
```kotlin
// Tải và hiển thị banner adaptive tự động vào container có ID fr_banner
ERainAd.getInstance().loadBanner(this, BuildConfig.ad_banner)

// Hoặc tải với callback xử lý sự kiện:
ERainAd.getInstance().loadBanner(this, BuildConfig.ad_banner, object : AdCallback() {
    override fun onAdLoaded() {
        super.onAdLoaded()
        Log.d("Banner", "Banner loaded")
    }

    override fun onAdFailedToLoad(i: LoadAdError?) {
        super.onAdFailedToLoad(i)
        Log.e("Banner", "Banner failed: ${i?.message}")
    }
})
```

### 4.2. Collapsible Banner (Dính đáy / dính đỉnh, tối ưu eCPM)
```kotlin
// Gravity có thể là AppConstant.CollapsibleGravity.BOTTOM hoặc TOP
ERainAd.getInstance().loadCollapsibleBanner(
    this,
    BuildConfig.ad_banner,
    AppConstant.CollapsibleGravity.BOTTOM,
    object : AdCallback() {
        override fun onAdLoaded() {
            super.onAdLoaded()
            Log.d("Banner", "Collapsible banner loaded")
        }
    }
)
```

---

## 5. Native Ads (Chuẩn Next-Gen Layout & Preload)

### 5.1. Định nghĩa file XML Layout theo chuẩn Next-Gen
Trong tất cả các file layout XML Native (`res/layout/native_large.xml`), thẻ bao ngoài cùng **bắt buộc** phải là `com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="8dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <ImageView
                android:id="@+id/ad_app_icon"
                android:layout_width="44dp"
                android:layout_height="44dp" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="8dp"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/ad_headline"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textStyle="bold"
                    android:textSize="14sp" />

                <TextView
                    android:id="@+id/ad_body"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="12sp" />
            </LinearLayout>
        </LinearLayout>

        <!-- MediaView hiển thị hình ảnh / video -->
        <com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
            android:id="@+id/ad_media"
            android:layout_width="match_parent"
            android:layout_height="180dp"
            android:layout_marginTop="8dp" />

        <!-- Nút Call-To-Action (CTA) -->
        <androidx.appcompat.widget.AppCompatButton
            android:id="@+id/ad_call_to_action"
            android:layout_width="match_parent"
            android:layout_height="44dp"
            android:layout_marginTop="8dp"
            android:background="#2563EB"
            android:textColor="#FFFFFF" />
    </LinearLayout>
</com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView>
```

### 5.2. Cách 1: Tải & Hiển thị trực tiếp vào Layout
```kotlin
val frAds: FrameLayout = findViewById(R.id.fr_ads)
val shimmerContainer: ShimmerFrameLayout = findViewById(R.id.shimmer_native)

ERainAd.getInstance().loadNativeAd(
    this,
    BuildConfig.ad_native,
    R.layout.native_large,
    frAds,
    shimmerContainer,
    object : AdCallback() {
        override fun onAdLoaded() {
            super.onAdLoaded()
            Log.d("Native", "Native Ad loaded & rendered")
        }

        override fun onAdFailedToLoad(i: LoadAdError?) {
            super.onAdFailedToLoad(i)
            frAds.removeAllViews() // Xoá placeholder khi không có quảng cáo
        }
    }
)
```

### 5.3. Cách 2: Tải trước (Preload) và gán hiển thị sau
```kotlin
private var preloadedNativeAd: ApNativeAd? = null

// 1. Tải trước quảng cáo Native:
fun preloadNativeAd() {
    ERainAd.getInstance().loadNativeAdResultCallback(
        this,
        BuildConfig.ad_native,
        R.layout.native_large,
        object : AdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                preloadedNativeAd = nativeAd
                Log.d("Native", "Preloaded Native Ad ready")
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                preloadedNativeAd = null
            }
        }
    )
}

// 2. Hiển thị khi người dùng chuyển tab hoặc màn hình mở ra:
fun showPreloadedNativeAd() {
    val ad = preloadedNativeAd
    if (ad != null && ad.isReady) {
        ERainAd.getInstance().populateNativeAdView(
            this,
            ad,
            findViewById(R.id.fr_ads),
            findViewById(R.id.shimmer_native)
        )
    }
}
```

---

## 6. Interstitial Ads (Standard & Priority)

### 6.1. Standard Interstitial (Preload & Show)
```kotlin
private var mInterstitialAd: ApInterstitialAd? = null

// 1. Tải trước Interstitial:
fun loadInterstitial() {
    mInterstitialAd = ERainAd.getInstance().getInterstitialAds(
        this,
        BuildConfig.ad_interstitial_splash,
        object : AdCallback() {
            override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                super.onApInterstitialLoad(apInterstitialAd)
                Log.d("Inter", "Interstitial Ad ready to show")
            }
        }
    )
}

// 2. Hiển thị Interstitial:
fun showInterstitial(onFinished: () -> Unit) {
    val ad = mInterstitialAd
    if (ad != null && ad.isReady) {
        ERainAd.getInstance().forceShowInterstitial(
            this,
            ad,
            object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    // Thực hiện hành động tiếp theo sau khi đóng quảng cáo
                    onFinished()
                }
            },
            true // Tự động reload quảng cáo mới sau khi xem xong
        )
    } else {
        // Chưa có quảng cáo -> Cho người dùng đi tiếp ngay lập tức
        onFinished()
    }
}
```

### 6.2. Priority 2 Interstitial (High Floor + Normal Floor)
```kotlin
val priority2Ad = ApInterstitialPriority2Ad(
    BuildConfig.ad_interstitial_splash, // ID High Floor
    BuildConfig.ad_interstitial_splash  // ID Normal Floor
)

// Tải đồng thời cả 2 floor, ưu tiên floor cao hơn:
ERainAd.getInstance().loadPriority2InterstitialAds(
    this,
    priority2Ad,
    object : AdCallback() {
        override fun onAdLoaded() {
            super.onAdLoaded()
            Log.d("Inter", "Priority 2 Interstitial loaded")
        }
    }
)

// Hiển thị:
ERainAd.getInstance().forceShowInterstitialPriority2(
    this,
    priority2Ad,
    object : AdCallback() {
        override fun onNextAction() {
            super.onNextAction()
            // Tiếp tục luồng
        }
    },
    true
)
```

---

## 7. Rewarded Ads & Rewarded Interstitial

### 7.1. Rewarded Ad
```kotlin
private var mRewardedAd: RewardedAd? = null

// 1. Tải Rewarded Ad:
fun loadRewardAd() {
    ERainAd.getInstance().initRewardAds(this, BuildConfig.ad_reward, object : AdCallback() {
        override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
            super.onRewardAdLoaded(rewardedAd)
            mRewardedAd = rewardedAd
            Log.d("Reward", "Rewarded Ad loaded")
        }
    })
}

// 2. Hiển thị & Trả thưởng:
fun showRewardAd() {
    val ad = mRewardedAd ?: return
    var isUserEarnedReward = false

    ERainAd.getInstance().showRewardAds(this, ad, object : RewardCallback {
        override fun onUserEarnedReward(rewardItem: RewardItem?) {
            // Đánh dấu người dùng đã xem đủ điều kiện nhận thưởng
            isUserEarnedReward = true
            Log.d("Reward", "User earned reward: ${rewardItem?.amount} ${rewardItem?.type}")
        }

        override fun onRewardedAdClosed() {
            if (isUserEarnedReward) {
                // Cộng xu, mở khóa tính năng VIP cho người dùng
            }
        }

        override fun onRewardedAdFailedToShow(codeError: Int) {
            Log.e("Reward", "Failed to show reward ad, code: $codeError")
        }
    })
}
```

---

## 8. Picture-in-Picture (PIP) Ads

Định dạng quảng cáo hình trong hình (PIP) thu nhỏ nổi trên ứng dụng theo chuẩn GMA Next-Gen.

```kotlin
private var mPipAd: ApPipAd? = null

// 1. Tải PIP Ad:
fun loadPipAd() {
    mPipAd = ERainAd.getInstance().getPipAd(
        this,
        BuildConfig.ad_pip,
        object : AdCallback() {
            override fun onPipAdLoaded(pipAd: PictureInPictureAd?) {
                super.onPipAdLoaded(pipAd)
                Log.d("PIP", "PIP Ad loaded")
            }
        }
    )
}

// 2. Hiển thị PIP Ad tại vị trí chỉ định:
fun showPipAd() {
    val pip = mPipAd ?: return
    ERainAd.getInstance().showPipAd(
        activity = this,
        apPipAd = pip,
        position = PictureInPictureAdPosition.BOTTOM_RIGHT, // BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT
        scope = PictureInPictureAdPresentationScope.ACTIVITY,
        adCallback = object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
            }
        }
    )
}

// 3. Ẩn / Huỷ PIP Ad:
fun hidePipAd() {
    ERainAd.getInstance().hidePipAd(mPipAd)
}

fun destroyPipAd() {
    ERainAd.getInstance().destroyPipAd(mPipAd)
}
```

---

## 9. Swipeable In-Feed Interstitial Ads

Định dạng quảng cáo lướt vuốt toàn màn hình tự nhiên (phong cách Short Videos / Reels).

```kotlin
private var mSwipeableAd: ApSwipeableAd? = null

// 1. Tải Swipeable Ad:
fun loadSwipeableAd() {
    mSwipeableAd = ERainAd.getInstance().getSwipeableInterstitialAd(
        this,
        BuildConfig.ad_swipeable_inter,
        object : AdCallback() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d("Swipeable", "Swipeable Interstitial loaded")
            }
        }
    )
}

// 2. Mở Feed Dialog hiển thị:
fun showSwipeableFeedDialog() {
    val swipeable = mSwipeableAd ?: return
    ERainAd.getInstance().showSwipeableInterstitialFeedDialog(
        this,
        swipeable,
        object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                Log.d("Swipeable", "Dialog closed")
            }
        }
    )
}
```

---

## 10. In-App Purchase & Subscription (Google Play Billing 9.0)

Thư viện tích hợp Google Play Billing 9.0 có cơ chế tự động cache trạng thái mua hàng vào SharedPreferences để mở khóa app tức thì:

```kotlin
// 1. Đăng ký nhận sự kiện mua hàng:
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(productId: String?, transactionDetails: String?) {
        Log.d("Billing", "Mua hàng thành công: $productId")
        // Tự động vô hiệu hóa toàn bộ quảng cáo trong phiên làm việc
    }

    override fun displayErrorMessage(errorMsg: String?) {
        Log.e("Billing", "Lỗi mua hàng: $errorMsg")
    }

    override fun onUserCancelBilling() {
        Log.d("Billing", "Người dùng huỷ thanh toán")
    }
})

// 2. Mua gói In-App vĩnh viễn (One-time purchase):
AppPurchase.getInstance().purchase(this, "android.test.purchased")

// 3. Đăng ký gói thuê bao (Subscription theo tuần / tháng / năm):
AppPurchase.getInstance().subscribe(this, "sub_vip_monthly")

// 4. Kiểm tra trạng thái người dùng đã mua VIP chưa:
if (AppPurchase.getInstance().isPurchased) {
    // Ẩn toàn bộ nút mua và quảng cáo
}
```

---

## 11. Công Cụ Debug Ads & Bidding Inspector (ERainAdDebugManager)

Thư viện tích hợp sẵn bảng điều khiển kiểm tra quảng cáo thời gian thực:

* **Tự động kích hoạt**: Khi chạy bản `debug` (`isVariantDev = true`), nút nổi hình `🐞` sẽ tự động hiển thị trên màn hình.
* **Mở Debug Inspector bằng code**:
```kotlin
ERainAd.getInstance().openDebugInspector(this)
```
* **Bật/Tắt chế độ Debug Inspector**:
```kotlin
// Bật debug nhưng ẩn nút nổi:
ERainAd.getInstance().setEnableDebugMode(enable = true, showFloatingButton = false)
```
* **Các thông tin hiển thị trên Inspector**:
  * Trạng thái chi tiết từng vị trí quảng cáo: `WAITING`, `LOADING`, `LOADED`, `SHOWN`, `FAILED`.
  * Thời gian tải (Latency tính bằng mili-giây).
  * Adapter mediation thắng phiên đấu giá Real-Time Bidding (`AppLovin`, `Vungle`, `Meta`, `Pangle`, v.v.).
  * Giá trị eCPM và doanh thu thực nhận trên mỗi lượt hiển thị.
