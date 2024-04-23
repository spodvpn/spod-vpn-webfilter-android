package br.com.spod.spodvpnwebfilter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class StoreFragment extends Fragment implements PurchasesUpdatedListener, StoreRecyclerViewAdapter.ProductClickListener
{
    private static final String TAG = "StoreFragment";

    private  BillingClient billingClient;
    private List<ProductDetails> productsList;
    private ProgressBar mProgressBar;
    private StoreRecyclerViewAdapter adapter;

    private GlobalMethods globalMethods;
    private int freeTrialTries = 0;

    private int selected_row;

    //Required public constructor
    public StoreFragment() { }

    public static StoreFragment newInstance() {
        return new StoreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBillingClient();
        checkCustomFreeTrial();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_store, container, false);

        //Setup recycler view and click listener
        RecyclerView recyclerView = view.findViewById(R.id.store_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new StoreRecyclerViewAdapter(getActivity(), productsList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        mProgressBar = view.findViewById(R.id.store_fragment_progress);

        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.shouldRedirectToStore = true;
            mainActivity.setTitle(getString(R.string.store_fragment_title));
        }

        return view;
    }

    private void setupBillingClient()
    {
        billingClient = BillingClient.newBuilder(requireActivity()).setListener(this).enablePendingPurchases().build();

        Log.v(TAG, "setupBillingClient: Begin");
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                //Log.v(TAG, "setupBillingClient: onBillingSetupFinished with ResponseCode: "+result.getResponseCode());
                if(mProgressBar != null) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> mProgressBar.setVisibility(View.GONE), 500);
                }
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.v(TAG, "BILLING | startConnection | RESULT OK");

                    //Success, query for products!
                    if(billingClient.isReady()) {
                        Log.v(TAG, "setupBillingClient: billingClint isReady!");

                        ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                                QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId("spod_vpn_monthly_subscription")
                                        .setProductType(BillingClient.ProductType.SUBS).build(),
                                QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId("spod_vpn_yearly_subscription")
                                        .setProductType(BillingClient.ProductType.SUBS)
                                        .build());

                        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(productList).build();

                        billingClient.queryProductDetailsAsync(
                                params,
                                (billingResult, productDetailsList) -> {
                                    // Process the result
                                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        Log.v(TAG, "setupBillingClient: querySkuDetailsAsync responseCode OK, proceeding...");
                                        productsList = productDetailsList; //save list
                                        adapter.reloadData(productsList, null); //reload recycler view

                                    } else {
                                        Log.v(TAG, "Can't querySkuDetailsAsync, responseCode: "+ billingResult.getResponseCode());
                                    }
                                }
                        );
                    }
                    else Log.v(TAG, "ERROR: billingClient NOT ready!");
                } else {
                    Log.v(TAG, "BILLING | startConnection | RESULT: " +result.getResponseCode());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.v(TAG, "BILLING | onBillingServiceDisconnected | DISCONNECTED");
                GlobalMethods globalMethods = new GlobalMethods(getActivity());
                try {
                    globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
                }
            }
        });
    }

    private void checkCustomFreeTrial()
    {
        try {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                //Check receipt
                QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();

                Log.v(TAG, "checkCustomFreeTrial: Calling queryPurchasesAsync()");
                mainActivity.billingClient.queryPurchasesAsync(params, (billingResult, list) -> {
                    boolean hasProfile = false;
                    boolean hasReceipt = false;

                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getPurchaseState() == Purchase.PurchaseState.PURCHASED)
                            hasReceipt = true;
                    }

                    //Check profile
                    VpnProfileDataSource mDataSource = new VpnProfileDataSource(mainActivity);
                    SharedPreferences sharedPreferences = mainActivity.getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    String username = sharedPreferences.getString(getString(R.string.preferences_username), "");
                    mDataSource.open();
                    if (!username.isEmpty()) hasProfile = true;
                    mDataSource.close();

                    if (!hasProfile && !hasReceipt) {
                        prepareForFreeTrial();
                    }
                });
            }
        } catch (NullPointerException exception) {
            Log.v(TAG, "Got a NullPointerException at checkCustomFreeTrial, probably running in the background...");
        }
    }

    private void prepareForFreeTrial() {
        try {
            MainActivity mainActivity = (MainActivity) getActivity();
            //Purchase.PurchasesResult purchasesResult;
            if (mainActivity != null) {
                //Eligible for custom free trial, get firebaseTokenId
                if (!mainActivity.firebaseTokenId.isEmpty()) {
                    //Already have firebaseTokenId, get SSAID and send to SPOD
                    if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
                    getCustomFreeTrial();
                } else {
                    //No firebaseTokenId yet!
                    if (this.freeTrialTries == 0) {
                        //First time, call connectFragment.updateNotificationToken(false) and try again in 1 second ?
                        ConnectFragment connectFragment = (ConnectFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("ConnectFragment");
                        if (connectFragment != null)
                            connectFragment.updateNotificationToken(false);
                    }

                    this.freeTrialTries++;
                    if (this.freeTrialTries < 5) { //5 is a hardcoded limit!
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(this::checkCustomFreeTrial, 1000);
                    }
                }
            }
        } catch (NullPointerException exception) {
            Log.v(TAG, "Got a NullPointerException at prepareForFreeTrial, probably running in the background...");
        }
    }

    private void getCustomFreeTrial()
    {
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity()); //Init if not already initialized
        MainActivity mainActivity = (MainActivity)getActivity();
        if(mainActivity != null)
        {
            @SuppressLint("HardwareIds") final String deviceID = Settings.Secure.getString(mainActivity.getContentResolver(), Settings.Secure.ANDROID_ID);

            //Create POST parameters JSONObject
            JSONObject postData = new JSONObject();
            try {
                postData.put("Regiao", getString(R.string.region)); //Region-specific
                postData.put("TokenID", mainActivity.firebaseTokenId);
                postData.put("SSAID", deviceID);
                postData.put("SO", "G");
            } catch (JSONException exception) {
                Log.v(TAG, "JSONException while trying to load custom free trial info: " + exception.getLocalizedMessage());
                exception.printStackTrace();
            }

            //Actually make the request
            globalMethods.APIRequest("https://spod.com.br/services/vpn/freeTrialInfo", postData, response -> {
                //Handle response here
                if(mProgressBar != null) mProgressBar.setVisibility(View.GONE);
                JSONObject jsonResponse;
                try {
                    jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success)))
                    {
                        String title = jsonResponse.optString("FreeTrialTitle", getString(R.string.not_available));
                        String subtitle = jsonResponse.optString("FreeTrialSubtitle", getString(R.string.not_available));
                        String price = jsonResponse.optString("FreeTrialPrice", getString(R.string.not_available));
                        String duration = jsonResponse.optString("FreeTrialDuration", getString(R.string.not_available));
                        String description_text = jsonResponse.optString("FreeTrialDescription", getString(R.string.not_available));

                        List<String> freeTrialInfo = new ArrayList<>();
                        freeTrialInfo.add(title);
                        freeTrialInfo.add(subtitle);
                        freeTrialInfo.add(price);
                        freeTrialInfo.add(duration);
                        freeTrialInfo.add(description_text);

                        adapter.reloadData(productsList, freeTrialInfo);

                    }
                } catch (JSONException exception) {
                    //Fail silently
                    Log.v(TAG, "getCustomFreeTrial: Error on request...");
                    exception.printStackTrace();
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "IllegalStateException from Volley: This happens when the request is interrupted...");
                }
            });
        }

    }

    private void returnToMainFragment(boolean subscribeToCustomFreeTrial)
    {
        try {
            ConnectFragment connectFragment = (ConnectFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("ConnectFragment");
            MainActivity mainActivity = (MainActivity)getActivity();
            assert mainActivity != null;
            mainActivity.shouldRedirectToStore = false;
            mainActivity.subscribeToCustomFreeTrial = subscribeToCustomFreeTrial;

            if (connectFragment != null) {
                connectFragment.verifyReceipt();
            }

            //Set activity's title and show switch country button
            getActivity().setTitle(getString(R.string.app_name));
            mainActivity.actionBarMenu.getItem(0).setVisible(true);

            //Close this fragment
            getActivity().getSupportFragmentManager().popBackStackImmediate();
        } catch (Exception exception) {
            Log.v(TAG, "returnToMainFragment: Got exception");
            exception.printStackTrace();
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases)
    {
        Log.v(TAG, "onPurchaseUpdated: "+result.getResponseCode());
        if(purchases != null) {
            if(result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED || result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                returnToMainFragment(false);
            } else {
                Log.v(TAG, "onPurchasesUpdated error: Response code ("+result.getResponseCode()+")");
            }
        }
    }

    @Override
    public void onItemClick(View view, int position)
    {
        Log.v(TAG, "onItemClick: "+position);
        Object productData = adapter.getItem(position);
        selected_row = position;

        String productTitle;
        String htmlString;
        String duration;
        String[] help_urls = getResources().getStringArray(R.array.help_urls); //2=terms of use, 3=privacy policy

        if(productData.getClass().equals(ProductDetails.class))
        {
            ProductDetails productDetails = (ProductDetails)productData;
            productTitle = productDetails.getTitle().split("(?> \\(.+?\\))$")[0]; //Remove redundant app name from subscription's name
            if(productDetails.getProductId().equals("spod_vpn_monthly_subscription")) {
                //Monthly
                duration = getString(R.string.monthly_duration);
                htmlString = String.format(Locale.getDefault(), getString(R.string.terms_monthly_sub_text), duration, Objects.requireNonNull(productDetails.getSubscriptionOfferDetails()).get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice(), productTitle, help_urls[2], help_urls[3]);
            } else {
                //Annual
                duration = getString(R.string.annual_duration);
                htmlString = String.format(Locale.getDefault(), getString(R.string.terms_annual_sub_text), duration, Objects.requireNonNull(productDetails.getSubscriptionOfferDetails()).get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice(), getString(R.string.terms_annual_discount), productTitle, help_urls[2], help_urls[3]);
            }
        } else {
            List<String> freeTrialInfo = (List<String>) productData;
            productTitle = freeTrialInfo.get(0);
            String price = freeTrialInfo.get(2);
            duration = freeTrialInfo.get(3);
            htmlString = String.format(Locale.getDefault(), getString(R.string.terms_free_trial_text), duration, price, help_urls[2], help_urls[3]);
        }

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.store_fragment_terms_container, TermsFragment.newInstance(productTitle, htmlString, TermsFragment.SUB_INFO_TYPE), "TermsFragment");
        transaction.addToBackStack(null);
        transaction.commit();
    }

    //Method for confirming purchase and launching billing flow
    void confirm_purchase()
    {
        Object productData = adapter.getItem(selected_row);
        if(productData.getClass().equals(ProductDetails.class)) {
            BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails((ProductDetails)productData)
                    .setOfferToken(Objects.requireNonNull(((ProductDetails) productData).getSubscriptionOfferDetails()).get(0).getOfferToken())
                    .build();
            BillingFlowParams billingFlowParams = BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(params))
                    .build();
            billingClient.launchBillingFlow(requireActivity(), billingFlowParams);
        }
        else {
            //Subscribe to FreeTrial (3 days)
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> returnToMainFragment(true), 600);
        }
    }
}
