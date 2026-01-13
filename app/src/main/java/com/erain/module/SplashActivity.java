package com.erain.module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.module.ads.ERainAd;
import com.ads.module.funtion.AdCallback;
import com.ads.module.ump.IAdConsentCallBack;
import com.ads.module.ump.ITGAdConsent;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.FormError;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private boolean canPersonalized = true;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        prefs = EasyPreferences.INSTANCE.defaultPrefs(this);

        if (!prefs.getBoolean("KEY_IS_USER_GLOBAL", false) && !prefs.getBoolean("KEY_CONFIRM_CONSENT", false)) {
            checkNeedToLoadConsent();
        } else {
            loadingRemoteConfig();
        }
    }

    private void checkNeedToLoadConsent() {
        ITGAdConsent.INSTANCE.loadAndShowConsent(true, new IAdConsentCallBack() {
            @Override
            public Activity getCurrentActivity() {
                return SplashActivity.this;
            }

            @Override
            public boolean isDebug() {
                return BuildConfig.DEBUG;
            }

            @Override
            public boolean isUnderAgeAd() {
                return false;
            }

            @Override
            public void onConsentError(FormError formError) {
                canPersonalized = true;
                loadingRemoteConfig();
            }

            @Override
            public void onConsentStatus(int consentStatus) {
                canPersonalized = consentStatus != ConsentInformation.ConsentStatus.REQUIRED;
            }

            @Override
            public void onConsentSuccess(boolean b) {
                canPersonalized = b;
                handleClickConsent(canPersonalized);
            }

            @Override
            public void onNotUsingAdConsent() {
                prefs.edit().putBoolean("KEY_IS_USER_GLOBAL", true).apply();
                canPersonalized = true;
                loadingRemoteConfig();
            }

            @Override
            public void onRequestShowDialog() {
            }

            @Override
            public String testDeviceID() {
                return "ED3576D8FCF2F8C52AD8E98B4CFA4005";
            }
        });
    }

    private void handleClickConsent(boolean canPersonalized) {
        if (canPersonalized) {
            prefs.edit().putBoolean("KEY_CONFIRM_CONSENT", true).apply();
        } else {
            ITGAdConsent.INSTANCE.resetConsentDialog();
        }
        loadingRemoteConfig();
    }

    private void loadingRemoteConfig() {
        ERainAd.getInstance().loadSplashInterstitialAds(this, BuildConfig.ad_interstitial_splash, 25000, 5000, new AdCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
    }

}
