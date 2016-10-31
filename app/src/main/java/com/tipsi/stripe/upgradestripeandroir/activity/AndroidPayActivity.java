package com.tipsi.stripe.upgradestripeandroir.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.tipsi.stripe.upgradestripeandroir.R;

public class AndroidPayActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // You will need to use your live API key even while testing
    public static final String PUBLISHABLE_KEY = "pk_live_XXX";

    // Unique identifiers for asynchronous requests:
    private static final int LOAD_MASKED_WALLET_REQUEST_CODE = 1000;
    private static final int LOAD_FULL_WALLET_REQUEST_CODE = 1001;
    public static final int mEnvironment = WalletConstants.ENVIRONMENT_TEST;
    private static final String TAG = AndroidPayActivity.class.getSimpleName();

    private SupportWalletFragment walletFragment;
    private GoogleApiClient googleApiClient;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .setTheme(WalletConstants.THEME_LIGHT)
                        .build())
                .build();
    }

    public void showAndroidPay() {
        Log.d(TAG, "showAndroidPay: ");
        setContentView(R.layout.android_pay_activity);

        walletFragment =
                (SupportWalletFragment) getSupportFragmentManager().findFragmentById(R.id.wallet_fragment);

        MaskedWalletRequest maskedWalletRequest = MaskedWalletRequest.newBuilder()

                // Request credit card tokenization with Stripe by specifying tokenization parameters:
                .setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters.newBuilder()
                        .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.PAYMENT_GATEWAY)
                        .addParameter("gateway", "stripe")
                        .addParameter("stripe:publishableKey", PUBLISHABLE_KEY)
                        .addParameter("stripe:version", com.stripe.Stripe.VERSION)
                        .build())

                // You want the shipping address:
                .setShippingAddressRequired(true)

                // Price set as a decimal:
                .setEstimatedTotalPrice("20.00")
                .setCurrencyCode("USD")
                .build();

        // Set the parameters:
        WalletFragmentInitParams initParams = WalletFragmentInitParams.newBuilder()
                .setMaskedWalletRequest(maskedWalletRequest)
                .setMaskedWalletRequestCode(LOAD_MASKED_WALLET_REQUEST_CODE)
                .build();

        // Initialize the fragment:
        walletFragment.initialize(initParams);

//        ...
    }

    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    public void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: ");
        if (requestCode == LOAD_MASKED_WALLET_REQUEST_CODE) { // Unique, identifying constant
            if (resultCode == Activity.RESULT_OK) {
                MaskedWallet maskedWallet = data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                FullWalletRequest fullWalletRequest = FullWalletRequest.newBuilder()
                        .setCart(Cart.newBuilder()
                                .setCurrencyCode("USD")
                                .setTotalPrice("20.00")
                                .addLineItem(LineItem.newBuilder() // Identify item being purchased
                                        .setCurrencyCode("USD")
                                        .setQuantity("1")
                                        .setDescription("Premium Llama Food")
                                        .setTotalPrice("20.00")
                                        .setUnitPrice("20.00")
                                        .build())
                                .build())
                        .setGoogleTransactionId(maskedWallet.getGoogleTransactionId())
                        .build();
                Wallet.Payments.loadFullWallet(googleApiClient, fullWalletRequest, LOAD_FULL_WALLET_REQUEST_CODE);
            }
        } else if (requestCode == LOAD_FULL_WALLET_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                FullWallet fullWallet = data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET);
                String tokenJSON = fullWallet.getPaymentMethodToken().getToken();

                //A token will only be returned in production mode,
                //i.e. WalletConstants.ENVIRONMENT_PRODUCTION
                if (mEnvironment == WalletConstants.ENVIRONMENT_PRODUCTION) {
                    com.stripe.model.Token token = com.stripe.model.Token.GSON.fromJson(
                            tokenJSON, com.stripe.model.Token.class);

                    Log.d(TAG, "onActivityResult: Stripe Token: "+token.toString());

                    // TODO: send token to your server
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: ");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected: ");
        Wallet.Payments.isReadyToPay(googleApiClient).setResultCallback(
                new ResultCallback<BooleanResult>() {
                    @Override
                    public void onResult(@NonNull BooleanResult booleanResult) {
                        Log.d(TAG, "onResult: ");
                        if (booleanResult.getStatus().isSuccess()) {
                            Log.d(TAG, "onResult: booleanResult.getStatus().isSuccess()");
                            if (booleanResult.getValue()) {
                                //TODO В Ннашей стране не работает! Как тестить не знаю.
                                Log.d(TAG, "onResult: booleanResult.getValue()");
                                showAndroidPay();
                            } else {
                                Log.d(TAG, "onResult: !booleanResult.getValue()");
                                //TODO: Здесь надо показывать диалог о том что Android Pay не доступна на данном устройстве, но я вызываю showAndroidPay() что бы было видно как она выглядит)
                                showAndroidPay();
                                // Hide Android Pay buttons, show a message that Android Pay
                                // cannot be used yet, and display a traditional checkout button
                            }
                        } else {
                            // Error making isReadyToPay call
                            Log.e(TAG, "isReadyToPay:" + booleanResult.getStatus());
                        }
                    }
                }
        );
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: ");
    }
}
