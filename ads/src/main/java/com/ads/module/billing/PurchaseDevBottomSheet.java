package com.ads.module.billing;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ads.module.R;
import com.ads.module.funtion.PurchaseListener;
import com.ads.module.util.AppUtil;
import com.android.billingclient.api.ProductDetails;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class PurchaseDevBottomSheet extends BottomSheetDialog {
    private ProductDetails productDetails;
    private int typeIap;
    private TextView txtTitle;
    private TextView txtDescription;
    private TextView txtId;
    private TextView txtPrice;
    private TextView txtContinuePurchase;
    private PurchaseListener purchaseListener;

    public PurchaseDevBottomSheet(int typeIap, ProductDetails productDetails, @NonNull Context context, PurchaseListener purchaseListener) {
        super(context);
        this.productDetails = productDetails;
        this.typeIap = typeIap;
        this.purchaseListener = purchaseListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_billing_test);
        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtId = findViewById(R.id.txtId);
        txtPrice = findViewById(R.id.txtPrice);
        txtContinuePurchase = findViewById(R.id.txtContinuePurchase);
        if (productDetails == null) {
            if (AppUtil.VARIANT_DEV) {
                txtContinuePurchase.setOnClickListener(v -> {
                    AppPurchase.getInstance().setPurchase(true);
                    if (purchaseListener != null) {
                        purchaseListener.onProductPurchased("android.test.purchased", "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}', parsedJson={\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}, productId='android.test.purchased', productType='inapp', title='Tiêu đề mẫu', productDetailsToken='AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC', subscriptionOfferDetails=null}");
                    }
                    dismiss();
                });
            } else {
                txtContinuePurchase.setOnClickListener(v -> {
                    dismiss();
                });
            }
        } else {
            txtTitle.setText(productDetails.getTitle());
            txtDescription.setText(productDetails.getDescription());
            txtId.setText(productDetails.getProductId());
            String price = "$2.00";
            if (typeIap == AppPurchase.TYPE_IAP.PURCHASE) {
                ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails = productDetails.getOneTimePurchaseOfferDetails();
                if (oneTimePurchaseOfferDetails != null) {
                    price = oneTimePurchaseOfferDetails.getFormattedPrice();
                }
                txtPrice.setText(price);
            } else {
                List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
                if (subscriptionOfferDetails != null && !subscriptionOfferDetails.isEmpty()) {
                    price = subscriptionOfferDetails.get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                }
                txtPrice.setText(price);
            }

            txtContinuePurchase.setOnClickListener(v -> {
                AppPurchase.getInstance().setPurchase(true);
                if (purchaseListener != null) {
                    purchaseListener.onProductPurchased(productDetails.getProductId(), "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}', parsedJson={\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}, productId='android.test.purchased', productType='inapp', title='Tiêu đề mẫu', productDetailsToken='AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC', subscriptionOfferDetails=null}");
                }
                dismiss();
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        int w = ViewGroup.LayoutParams.MATCH_PARENT;
        int h = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setLayout(w, h);
    }
}
