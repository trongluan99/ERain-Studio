package com.ads.module.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.ads.module.event.ERainLogEventManager;
import com.ads.module.funtion.BillingListener;
import com.ads.module.funtion.PurchaseListener;
import com.ads.module.funtion.UpdatePurchaseListener;
import com.ads.module.util.AppUtil;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppPurchase {
    private static final String TAG = "PurchaseEG";

    public static final String PRODUCT_ID_TEST = "android.test.purchased";
    @SuppressLint("StaticFieldLeak")
    private static AppPurchase instance;

    @SuppressLint("StaticFieldLeak")
    private String price = "1.49$";
    private String oldPrice = "2.99$";

    @Deprecated
    private String productId;
    private ArrayList<QueryProductDetailsParams.Product> listSubscriptionId;
    private ArrayList<QueryProductDetailsParams.Product> listINAPId;
    private PurchaseListener purchaseListener;
    private UpdatePurchaseListener updatePurchaseListener;
    private BillingListener billingListener;
    private Boolean isInitBillingFinish = false;
    private BillingClient billingClient;
    private List<ProductDetails> skuListINAPFromStore;
    private List<ProductDetails> skuListSubsFromStore;
    final private Map<String, ProductDetails> skuDetailsINAPMap = new HashMap<>();
    final private Map<String, ProductDetails> skuDetailsSubsMap = new HashMap<>();
    private boolean isAvailable;
    private boolean isListGot;
    private boolean isConsumePurchase = false;
    private String idPurchaseCurrent = "";
    private String offerTokenCurrent = "";
    private int typeIap;
    private boolean verifyFinish = false;

    private boolean isVerifyINAP = false;
    private boolean isVerifySUBS = false;
    private boolean isUpdateInapps = false;
    private boolean isUpdateSubs = false;

    private boolean isPurchase = false;//state purchase on app
    private String idPurchased = "";//id purchased
    private List<PurchaseResult> ownerIdSubs = new ArrayList<>();//id sub
    private List<String> ownerIdInapps = new ArrayList<>();//id inapp

    private Handler handlerTimeout;
    private Runnable rdTimeout;

    public void setPurchaseListener(PurchaseListener purchaseListener) {
        this.purchaseListener = purchaseListener;
    }

    public void setUpdatePurchaseListener(UpdatePurchaseListener listener) {
        this.updatePurchaseListener = listener;
    }

    /**
     * Listener init billing app
     * When init available auto call onInitBillingFinish with resultCode = 0
     *
     * @param billingListener
     */
    public void setBillingListener(BillingListener billingListener) {
        this.billingListener = billingListener;
        if (isAvailable && verifyFinish) {
            isInitBillingFinish = true;
            billingListener.onInitBillingFinished(0);
        }
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public Boolean getInitBillingFinish() {
        return isInitBillingFinish;
    }

    public void setEventConsumePurchaseTest(View view) {
        view.setOnClickListener(view1 -> {
            if (AppUtil.VARIANT_DEV) {
                Log.d(TAG, "setEventConsumePurchaseTest: success");
                AppPurchase.getInstance().consumePurchase(PRODUCT_ID_TEST);
            }
        });
    }

    /**
     * Listener init billing app with timeout
     * When init available auto call onInitBillingFinish with resultCode = 0
     *
     * @param billingListener
     * @param timeout
     */
    public void setBillingListener(BillingListener billingListener, int timeout) {
        Log.d(TAG, "setBillingListener: timeout " + timeout);
        this.billingListener = billingListener;
        if (isAvailable && verifyFinish) {
            Log.d(TAG, "setBillingListener: finish");
            isInitBillingFinish = true;
            billingListener.onInitBillingFinished(0);
            return;
        }
        isInitBillingFinish = false;
        handlerTimeout = new Handler(Looper.getMainLooper());
        rdTimeout = () -> {
            Log.d(TAG, "setBillingListener: timeout run ");
            if (!isInitBillingFinish) {
                isInitBillingFinish = true;
                billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.ERROR);
            }
        };
        handlerTimeout.postDelayed(rdTimeout, timeout);
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setConsumePurchase(boolean consumePurchase) {
        isConsumePurchase = consumePurchase;
    }

    public void setOldPrice(String oldPrice) {
        this.oldPrice = oldPrice;
    }

    PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> list) {
            Log.e(TAG, "onPurchasesUpdated code: " + billingResult.getResponseCode());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                for (Purchase purchase : list) {

                    List<String> sku = purchase.getSkus();
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                if (purchaseListener != null)
                    purchaseListener.onUserCancelBilling();
                Log.d(TAG, "onPurchasesUpdated:USER_CANCELED ");
            } else {
                Log.d(TAG, "onPurchasesUpdated:... ");
            }
        }
    };

    BillingClientStateListener purchaseClientStateListener =
            new BillingClientStateListener() {

                @Override
                public void onBillingServiceDisconnected() {
                    isAvailable = false;

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        billingClient.startConnection(purchaseClientStateListener);
                    }, 2000);
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    int code = billingResult.getResponseCode();
                    Log.d(TAG, "onBillingSetupFinished: " + code);

                    isInitBillingFinish = true;

                    if (code != BillingClient.BillingResponseCode.OK) {
                        // retry init
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            billingClient.startConnection(purchaseClientStateListener);
                        }, 2000);
                        return;
                    }

                    // ====== BILLING READY ======
                    isAvailable = true;

                    // 1️⃣ VERIFY PURCHASE (AN TOÀN)
                    verifyPurchased(true);

                    // 2️⃣ LOAD INAPP
                    if (!listINAPId.isEmpty()) {
                        QueryProductDetailsParams paramsINAP =
                                QueryProductDetailsParams.newBuilder()
                                        .setProductList(listINAPId)
                                        .build();

                        billingClient.queryProductDetailsAsync(
                                paramsINAP,
                                (br, result) -> {
                                    List<ProductDetails> list = result.getProductDetailsList();
                                    if (list != null && !list.isEmpty()) {
                                        skuListINAPFromStore = list;
                                        addSkuINAPToMap(list);
                                    }
                                }
                        );
                    }

                    // 3️⃣ LOAD SUBS
                    if (!listSubscriptionId.isEmpty()) {
                        QueryProductDetailsParams paramsSUBS =
                                QueryProductDetailsParams.newBuilder()
                                        .setProductList(listSubscriptionId)
                                        .build();

                        billingClient.queryProductDetailsAsync(
                                paramsSUBS,
                                (br, result) -> {
                                    List<ProductDetails> list = result.getProductDetailsList();
                                    if (list != null && !list.isEmpty()) {
                                        skuListSubsFromStore = list;
                                        addSkuSubsToMap(list);
                                    }
                                }
                        );
                    }
                }
            };


    public static AppPurchase getInstance() {
        if (instance == null) {
            instance = new AppPurchase();
        }
        return instance;
    }

    public List<PurchaseResult> getOwnerIdSubs() {
        return ownerIdSubs;
    }

    public List<String> getOwnerIdInapps() {
        return ownerIdInapps;
    }

    private AppPurchase() {

    }

    public void initBilling(final Application application, List<
            String> listINAPId, List<String> listSubsId) {

        if (AppUtil.VARIANT_DEV) {
            listINAPId.add(PRODUCT_ID_TEST);
        }
        this.listSubscriptionId = listIdToListProduct(listSubsId, BillingClient.ProductType.SUBS);
        this.listINAPId = listIdToListProduct(listINAPId, BillingClient.ProductType.INAPP);

        billingClient = BillingClient.newBuilder(application).setListener(purchasesUpdatedListener).enablePendingPurchases(PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()).build();

        billingClient.startConnection(purchaseClientStateListener);
    }


    private void addSkuSubsToMap(List<ProductDetails> skuList) {
        for (ProductDetails skuDetails : skuList) {
            skuDetailsSubsMap.put(skuDetails.getProductId(), skuDetails);
        }
    }

    private void addSkuINAPToMap(List<ProductDetails> skuList) {
        for (ProductDetails skuDetails : skuList) {
            skuDetailsINAPMap.put(skuDetails.getProductId(), skuDetails);
        }
    }

    public void setPurchase(boolean purchase) {
        isPurchase = purchase;
    }

    public boolean isPurchased() {
        return isPurchase;
    }

    public boolean isPurchased(Context context) {
        return isPurchase;
    }

    public String getIdPurchased() {
        return idPurchased;
    }

    private void addOrUpdateOwnerIdSub(PurchaseResult purchaseResult, String id) {
        boolean isExistId = false;
        for (PurchaseResult p : ownerIdSubs) {
            if (p.getProductId().contains(id)) {
                isExistId = true;
                ownerIdSubs.remove(p);
                ownerIdSubs.add(purchaseResult);
                break;
            }
        }
        if (!isExistId) {
            ownerIdSubs.add(purchaseResult);
        }
    }

    public void verifyPurchased(boolean isCallback) {
        if (billingClient == null || !billingClient.isReady()) {
            Log.e(TAG, "BillingClient is not ready");
            return;
        }

        verifyFinish = false;
        isVerifyINAP = false;
        isVerifySUBS = false;
        ownerIdInapps.clear();
        ownerIdSubs.clear();
        isPurchase = false;
        ArrayList<String> productIdsINAP = getProductIds(listINAPId);
        ArrayList<String> productIdsSUBS = getProductIds(listSubscriptionId);

        if (!productIdsINAP.isEmpty()) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                for (String productId : productIdsINAP) {
                                    if (purchase.getProducts().contains(productId)) {
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            ownerIdInapps.add(productId);
                                            isPurchase = true;
                                            if (!purchase.isAcknowledged()) {
                                                AcknowledgePurchaseParams acknowledgePurchaseParams =
                                                        AcknowledgePurchaseParams.newBuilder()
                                                                .setPurchaseToken(purchase.getPurchaseToken())
                                                                .build();
                                                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult1 -> {
                                                    Log.d(TAG, "verifyPurchased (INAP) acknowledgePurchase: " + billingResult1.getDebugMessage());
                                                });
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        isVerifyINAP = true;
                        checkAndFinishCallback(isCallback, billingResult);
                    }
            );
        } else {
            isVerifyINAP = true;
        }

        if (!productIdsSUBS.isEmpty()) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                for (String productId : productIdsSUBS) {
                                    if (purchase.getProducts().contains(productId)) {
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            PurchaseResult purchaseResult = new PurchaseResult(
                                                    purchase.getPackageName(),
                                                    purchase.getProducts(),
                                                    purchase.getPurchaseState(),
                                                    purchase.isAutoRenewing()
                                            );
                                            addOrUpdateOwnerIdSub(purchaseResult, productId);
                                            isPurchase = true;
                                            if (!purchase.isAcknowledged()) {
                                                AcknowledgePurchaseParams acknowledgePurchaseParams =
                                                        AcknowledgePurchaseParams.newBuilder()
                                                                .setPurchaseToken(purchase.getPurchaseToken())
                                                                .build();
                                                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult1 -> {
                                                    Log.d(TAG, "verifyPurchased (SUBS) acknowledgePurchase: " + billingResult1.getDebugMessage());
                                                });
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        isVerifySUBS = true;
                        checkAndFinishCallback(isCallback, billingResult);
                    }
            );
        } else {
            isVerifySUBS = true;
        }
    }


    private ArrayList<String> getProductIds(ArrayList<QueryProductDetailsParams.Product> productList) {
        ArrayList<String> productIds = new ArrayList<>();
        if (productList != null) {
            for (QueryProductDetailsParams.Product product : productList) {
                productIds.add(product.zza());
            }
        }
        return productIds;
    }

    private void checkAndFinishCallback(boolean isCallback, BillingResult billingResult) {
        if (isVerifyINAP && isVerifySUBS) {
            verifyFinish = true;
            if (billingListener != null && isCallback && !isInitBillingFinish) {
                isInitBillingFinish = true;
                billingListener.onInitBillingFinished(billingResult.getResponseCode());
                if (handlerTimeout != null && rdTimeout != null) {
                    handlerTimeout.removeCallbacks(rdTimeout);
                }
            }
        }
    }


    public void updatePurchaseStatus() {
        isUpdateInapps = false;
        isUpdateSubs = false;
        ownerIdInapps.clear();
        ownerIdSubs.clear();
        isPurchase = false;

        if (listINAPId != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product id : listINAPId) {
                                    if (purchase.getProducts().contains(id.zza())) {
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            if (!ownerIdInapps.contains(id.zza())) {
                                                ownerIdInapps.add(id.zza());
                                            }
                                            isPurchase = true;
                                        }
                                    }
                                }
                            }
                        }
                        isUpdateInapps = true;
                        if (isUpdateSubs) {
                            if (updatePurchaseListener != null) {
                                updatePurchaseListener.onUpdateFinished();
                            }
                        }
                    }
            );
        }

        if (listSubscriptionId != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product id : listSubscriptionId) {
                                    if (purchase.getProducts().contains(id.zza())) {
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            PurchaseResult purchaseResult = new PurchaseResult(
                                                    purchase.getPackageName(),
                                                    purchase.getProducts(),
                                                    purchase.getPurchaseState(),
                                                    purchase.isAutoRenewing()
                                            );
                                            addOrUpdateOwnerIdSub(purchaseResult, id.zza());
                                            isPurchase = true;
                                        }
                                    }
                                }
                            }
                        }
                        isUpdateSubs = true;
                        if (isUpdateInapps) {
                            if (updatePurchaseListener != null) {
                                updatePurchaseListener.onUpdateFinished();
                            }
                        }
                    }
            );
        }
    }

    @Deprecated
    public void purchase(Activity activity) {
        if (productId == null) {
            Toast.makeText(activity, "Product id must not be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        purchase(activity, productId);
    }


    public String purchase(Activity activity, String productId) {
        if (skuListINAPFromStore == null) {
            if (purchaseListener != null)
                purchaseListener.displayErrorMessage("Billing error init");
            return "";
        }
        ProductDetails productDetails = skuDetailsINAPMap.get(productId);
        if (AppUtil.VARIANT_DEV) {
            productId = PRODUCT_ID_TEST;
            PurchaseDevBottomSheet purchaseDevBottomSheet = new PurchaseDevBottomSheet(TYPE_IAP.PURCHASE, productDetails, activity, purchaseListener);
            purchaseDevBottomSheet.show();
            return "";
        }

        if (productDetails == null) {
            return "Product ID invalid";
        }

        idPurchaseCurrent = productId;
        typeIap = TYPE_IAP.PURCHASE;


        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);

        switch (billingResult.getResponseCode()) {

            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing not supported for type of request");
                return "Billing not supported for type of request";

            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "";

            case BillingClient.BillingResponseCode.ERROR:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Error completing request");
                return "Error completing request";

            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Selected item is already owned";

            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";

            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";

            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";

            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Network error.");
                return "Network Connection down";

            case BillingClient.BillingResponseCode.USER_CANCELED:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Request Canceled");
                return "Request Canceled";

            case BillingClient.BillingResponseCode.OK:
                offerTokenCurrent = "";
                return "Subscribed Successfully";
        }
        return "";
    }

    public String subscribe(Activity activity, String SubsId) {
        if (AppUtil.VARIANT_DEV) {
            purchase(activity, PRODUCT_ID_TEST);
            return "Billing test";
        } else {
            if (skuListSubsFromStore == null) {
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing error init");
                return "";
            }
        }
        ProductDetails productDetails = skuDetailsSubsMap.get(SubsId);
        if (productDetails == null) {
            return "Product ID invalid";
        }
        List<ProductDetails.SubscriptionOfferDetails> subsDetail = productDetails.getSubscriptionOfferDetails();
        if (subsDetail == null || subsDetail.isEmpty()) {
            return "No available offers for this subscription";
        }

        String offerToken = getOfferToken(subsDetail);

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        offerTokenCurrent = offerToken;

        switch (billingResult.getResponseCode()) {

            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing not supported for type of request");
                return "Billing not supported for type of request";

            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "";

            case BillingClient.BillingResponseCode.ERROR:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Error completing request");
                return "Error completing request";

            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Selected item is already owned";

            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";

            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";

            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";

            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Network error.");
                return "Network Connection down";

            case BillingClient.BillingResponseCode.USER_CANCELED:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Request Canceled");
                return "Request Canceled";

            case BillingClient.BillingResponseCode.OK:
                return "Subscribed Successfully";
        }
        return "";
    }

    /**
     * Subscribe with specific offer token
     *
     * @param activity   current activity
     * @param SubsId     subscription product ID
     * @param offerToken specific offer token to use
     * @return status message
     */
    public String subscribe(Activity activity, String SubsId, String offerToken) {
        if (AppUtil.VARIANT_DEV) {
            purchase(activity, PRODUCT_ID_TEST);
            return "Billing test";
        } else {
            if (skuListSubsFromStore == null) {
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing error init");
                return "";
            }
        }

        ProductDetails productDetails = skuDetailsSubsMap.get(SubsId);
        if (productDetails == null) {
            return "Product ID invalid";
        }

        if (offerToken == null || offerToken.isEmpty()) {
            return "Offer token is required";
        }

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        offerTokenCurrent = offerToken;

        switch (billingResult.getResponseCode()) {

            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing not supported for type of request");
                return "Billing not supported for type of request";

            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "";

            case BillingClient.BillingResponseCode.ERROR:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Error completing request");
                return "Error completing request";

            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Selected item is already owned";

            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";

            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";

            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";

            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Network error.");
                return "Network Connection down";

            case BillingClient.BillingResponseCode.USER_CANCELED:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Request Canceled");
                return "Request Canceled";

            case BillingClient.BillingResponseCode.OK:
                return "Subscribed Successfully";
        }
        return "";
    }

    private String getOfferToken(List<ProductDetails.SubscriptionOfferDetails> subsDetail) {
        String offerToken = null;
        for (ProductDetails.SubscriptionOfferDetails offer : subsDetail) {
            List<ProductDetails.PricingPhase> pricingPhases = offer.getPricingPhases().getPricingPhaseList();
            for (ProductDetails.PricingPhase phase : pricingPhases) {
                if (phase.getPriceAmountMicros() == 0L) { // Free trial
                    offerToken = offer.getOfferToken();
                    break;
                }
            }
            if (offerToken != null) break;
        }

        if (offerToken == null) {
            offerToken = subsDetail.get(0).getOfferToken();
        }
        offerTokenCurrent = offerToken;
        return offerToken;
    }

    public void consumePurchase() {
        if (productId == null) {
            Log.e(TAG, "Consume Purchase false:productId null ");
            return;
        }
        consumePurchase(productId);
    }

    public void consumePurchase(String productId) {
        if (billingClient == null || !billingClient.isReady()) {
            Log.e(TAG, "BillingClient is not ready");
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build();
        billingClient.queryPurchasesAsync(params, (billingResult, list) -> {
            Purchase pc = null;
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase purchase : list) {
                    if (purchase.getProducts().contains(productId)) {
                        pc = purchase;
                    }
                }
            }

            if (pc == null) {
                Log.e(TAG, "No purchases found to consume.");
                return;
            }

            try {
                ConsumeParams consumeParams =
                        ConsumeParams.newBuilder()
                                .setPurchaseToken(pc.getPurchaseToken())
                                .build();

                billingClient.consumeAsync(consumeParams, (billingResult1, purchaseToken) -> {
                    if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "onConsumeResponse: OK");
                        verifyPurchased(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private List<String> getListInappId() {
        List<String> list = new ArrayList<>();
        for (QueryProductDetailsParams.Product product : listINAPId) {
            list.add(product.zza());
        }
        return list;
    }

    private List<String> getListSubId() {
        List<String> list = new ArrayList<>();
        for (QueryProductDetailsParams.Product product : listSubscriptionId) {
            list.add(product.zza());
        }
        return list;
    }

    private void handlePurchase(Purchase purchase) {
        double price = getPriceWithoutCurrency(idPurchaseCurrent, typeIap, offerTokenCurrent);
        String currency = getCurrency(idPurchaseCurrent, typeIap, offerTokenCurrent);
        ERainLogEventManager.onTrackRevenuePurchase((float) price, currency, idPurchaseCurrent, typeIap);

        if (purchaseListener != null) {
            isPurchase = true;
            purchaseListener.onProductPurchased(purchase.getOrderId(), purchase.getOriginalJson());
        }
        if (isConsumePurchase) {
            ConsumeParams consumeParams =
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

            ConsumeResponseListener listener = new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                    Log.d(TAG, "onConsumeResponse: " + billingResult.getDebugMessage());
                }
            };

            billingClient.consumeAsync(consumeParams, listener);
        } else {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                if (!purchase.isAcknowledged()) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                            Log.d(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getDebugMessage());
                        }
                    });
                }
            }
        }
    }


    @Deprecated
    public String getPrice() {
        return getPrice(productId);
    }

    public String getPrice(String productId) {

        ProductDetails skuDetails = skuDetailsINAPMap.get(productId);
        if (skuDetails == null)
            return "";

        Log.e(TAG, "getPrice: " + skuDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());

        return skuDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    public String getPrice(String productId, int typeIap, String offerToken) {
        if (typeIap == TYPE_IAP.PURCHASE) {
            return getPrice(productId);
        } else {
            return getPriceSub(productId, offerToken);
        }
    }

    public String getPriceSub(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null)
            return "";


        List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
        Log.e(TAG, "getPriceSub: " + pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice());
        return pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice();
    }

    public String getPriceSub(String productId, String offerToken) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null || skuDetails.getSubscriptionOfferDetails() == null)
            return getPriceSub(productId);

        for (ProductDetails.SubscriptionOfferDetails offer : skuDetails.getSubscriptionOfferDetails()) {
            if (offer.getOfferToken().equals(offerToken)) {
                List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
                if (phases != null && !phases.isEmpty()) {
                    // Return the first phase price (introductory price or free trial)
                    return phases.get(0).getFormattedPrice();
                }
            }
        }
        return getPriceSub(productId);
    }

    /**
     * Get all available subscription offers for a product
     *
     * @param productId subscription product ID
     * @return list of SubscriptionOfferDetails or null if not found
     */
    public List<ProductDetails.SubscriptionOfferDetails> getSubscriptionOffers(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null)
            return null;
        return skuDetails.getSubscriptionOfferDetails();
    }

    /**
     * Get Price Pricing Phase List Subs
     *
     * @param productId
     * @return
     */
    public List<ProductDetails.PricingPhase> getPricePricingPhaseList(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null)
            return null;

        List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
        return pricingPhaseList;
    }

    /**
     * Get Formatted Price by country
     * Get final price with id
     *
     * @param productId
     * @return
     */
    public String getIntroductorySubPrice(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null) {
            return "";
        }
        if (skuDetails.getOneTimePurchaseOfferDetails() != null)
            return skuDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
        else if (skuDetails.getSubscriptionOfferDetails() != null) {
            List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
            List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice();
        } else {
            return "";
        }

    }

    /**
     * Get Currency subs or IAP by country
     *
     * @param productId
     * @param typeIAP
     * @return
     */
    public String getCurrency(String productId, int typeIAP) {
        return getCurrency(productId, typeIAP, "");
    }

    public String getCurrency(String productId, int typeIAP, String offerToken) {
        ProductDetails skuDetails = typeIAP == TYPE_IAP.PURCHASE ? skuDetailsINAPMap.get(productId) : skuDetailsSubsMap.get(productId);
        if (skuDetails == null) {
            return "";
        }
        if (typeIAP == TYPE_IAP.PURCHASE)
            return skuDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
        else {
            List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
            if (subsDetail == null || subsDetail.isEmpty()) return "";

            ProductDetails.SubscriptionOfferDetails targetOffer = subsDetail.get(subsDetail.size() - 1);
            if (offerToken != null && !offerToken.isEmpty()) {
                for (ProductDetails.SubscriptionOfferDetails offer : subsDetail) {
                    if (offer.getOfferToken().equals(offerToken)) {
                        targetOffer = offer;
                        break;
                    }
                }
            }

            List<ProductDetails.PricingPhase> pricingPhaseList = targetOffer.getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(0).getPriceCurrencyCode();
        }
    }

    public Map<String, ProductDetails> getSkuDetailsINAPMap() {
        return skuDetailsINAPMap;
    }

    public Map<String, ProductDetails> getSkuDetailsSubsMap() {
        return skuDetailsSubsMap;
    }

    /**
     * Get Price Amount Micros subs or IAP
     * Get final price with id
     *
     * @param productId
     * @param typeIAP
     * @return
     */
    public double getPriceWithoutCurrency(String productId, int typeIAP) {
        return getPriceWithoutCurrency(productId, typeIAP, "");
    }

    public double getPriceWithoutCurrency(String productId, int typeIAP, String offerToken) {
        ProductDetails skuDetails = typeIAP == TYPE_IAP.PURCHASE ? skuDetailsINAPMap.get(productId) : skuDetailsSubsMap.get(productId);
        if (skuDetails == null) {
            return 0;
        }
        if (typeIAP == TYPE_IAP.PURCHASE)
            return skuDetails.getOneTimePurchaseOfferDetails().getPriceAmountMicros();
        else {
            List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
            if (subsDetail == null || subsDetail.isEmpty()) return 0;

            ProductDetails.SubscriptionOfferDetails targetOffer = subsDetail.get(subsDetail.size() - 1);
            if (offerToken != null && !offerToken.isEmpty()) {
                for (ProductDetails.SubscriptionOfferDetails offer : subsDetail) {
                    if (offer.getOfferToken().equals(offerToken)) {
                        targetOffer = offer;
                        break;
                    }
                }
            }
            List<ProductDetails.PricingPhase> pricingPhaseList = targetOffer.getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(0).getPriceAmountMicros();
        }
    }

    /**
     * Format currency and price by country
     *
     * @param price
     * @param currency
     * @return
     */
    private String formatCurrency(double price, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        format.setMaximumFractionDigits(0);
        format.setCurrency(Currency.getInstance(currency));
        return format.format(price);
    }

    /**
     * Check if a subscription has free trial offer
     *
     * @param productId subscription product ID
     * @return true if subscription has free trial, false otherwise
     */
    public boolean hasFreeTrial(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null || skuDetails.getSubscriptionOfferDetails() == null) {
            return false;
        }

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = skuDetails.getSubscriptionOfferDetails();
        for (ProductDetails.SubscriptionOfferDetails offerDetails : offerDetailsList) {
            List<ProductDetails.PricingPhase> pricingPhases = offerDetails.getPricingPhases().getPricingPhaseList();
            // Check if first pricing phase is a free trial (price = 0)
            if (pricingPhases.size() > 1 && pricingPhases.get(0).getPriceAmountMicros() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get free trial period for a subscription
     *
     * @param productId subscription product ID
     * @return free trial period string (e.g., "P7D" for 7 days, "P1M" for 1 month), or empty string if no trial
     */
    public String getFreeTrialPeriod(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null || skuDetails.getSubscriptionOfferDetails() == null) {
            return "";
        }

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = skuDetails.getSubscriptionOfferDetails();
        for (ProductDetails.SubscriptionOfferDetails offerDetails : offerDetailsList) {
            List<ProductDetails.PricingPhase> pricingPhases = offerDetails.getPricingPhases().getPricingPhaseList();
            // Check if first pricing phase is a free trial
            if (pricingPhases.size() > 1 && pricingPhases.get(0).getPriceAmountMicros() == 0) {
                return pricingPhases.get(0).getBillingPeriod();
            }
        }
        return "";
    }

    /**
     * Get formatted free trial period string
     *
     * @param productId subscription product ID
     * @return formatted string (e.g., "7 days free", "1 month free"), or empty string if no trial
     */
    public String getFormattedFreeTrialPeriod(String productId) {
        String period = getFreeTrialPeriod(productId);
        if (period.isEmpty()) {
            return "";
        }

        // Parse ISO 8601 duration format (e.g., P7D, P1M, P1Y)
        if (period.startsWith("P")) {
            period = period.substring(1);
            if (period.endsWith("D")) {
                int days = Integer.parseInt(period.replace("D", ""));
                return days + (days == 1 ? " day free" : " days free");
            } else if (period.endsWith("W")) {
                int weeks = Integer.parseInt(period.replace("W", ""));
                return weeks + (weeks == 1 ? " week free" : " weeks free");
            } else if (period.endsWith("M")) {
                int months = Integer.parseInt(period.replace("M", ""));
                return months + (months == 1 ? " month free" : " months free");
            } else if (period.endsWith("Y")) {
                int years = Integer.parseInt(period.replace("Y", ""));
                return years + (years == 1 ? " year free" : " years free");
            }
        }
        return period;
    }

    /**
     * Get the offer token for free trial subscription
     *
     * @param productId subscription product ID
     * @return offer token string, or null if no free trial available
     */
    public String getFreeTrialOfferToken(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null || skuDetails.getSubscriptionOfferDetails() == null) {
            return null;
        }

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = skuDetails.getSubscriptionOfferDetails();
        for (ProductDetails.SubscriptionOfferDetails offerDetails : offerDetailsList) {
            List<ProductDetails.PricingPhase> pricingPhases = offerDetails.getPricingPhases().getPricingPhaseList();
            // Check if first pricing phase is a free trial
            if (pricingPhases.size() > 1 && pricingPhases.get(0).getPriceAmountMicros() == 0) {
                return offerDetails.getOfferToken();
            }
        }
        return null;
    }

    /**
     * Purchase subscription with free trial if available
     * If free trial is not available, it will use the default subscription offer
     *
     * @param activity  current activity
     * @param productId subscription product ID
     */
    public void subscribeWithFreeTrial(Activity activity, String productId) {
        if (skuDetailsSubsMap.containsKey(productId)) {
            ProductDetails productDetails = skuDetailsSubsMap.get(productId);
            String offerToken = getFreeTrialOfferToken(productId);

            if (offerToken != null) {
                // Has free trial - use the free trial offer token
                subscribe(activity, productId, offerToken);
            } else {
                // No free trial - use default subscription
                subscribe(activity, productId);
            }
        } else {
            Log.e(TAG, "subscribeWithFreeTrial: Product not found - " + productId);
        }
    }

    private double discount = 1;

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDiscount() {
        return discount;
    }

    private ArrayList<QueryProductDetailsParams.Product> listIdToListProduct(List<String> listId, String styleBilling) {
        ArrayList<QueryProductDetailsParams.Product> listProduct = new ArrayList<QueryProductDetailsParams.Product>();
        for (String id : listId) {
            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(styleBilling)
                    .build();
            listProduct.add(product);
        }
        return listProduct;
    }

    @IntDef({TYPE_IAP.PURCHASE, TYPE_IAP.SUBSCRIPTION})
    public @interface TYPE_IAP {
        int PURCHASE = 1;
        int SUBSCRIPTION = 2;
    }
}
