package br.com.spod.spodvpnwebfilter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    private List<SkuDetails> skuDetailsList;
    private ProgressBar mProgressBar;

    private List<String> skuList = Arrays.asList("spod_vpn_monthly_subscription", "spod_vpn_yearly_subscription");

    private StoreRecyclerViewAdapter adapter;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_store, container, false);

        //Setup recycler view and click listener
        RecyclerView recyclerView = view.findViewById(R.id.store_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new StoreRecyclerViewAdapter(getActivity(), skuDetailsList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        mProgressBar = view.findViewById(R.id.store_fragment_progress);

        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.shouldRedirectToStore = true;
            mainActivity.setTitle(getString(R.string.store_fragment_title));
        }

        return view;
    }

    private void setupBillingClient()
    {
        billingClient = BillingClient.newBuilder(Objects.requireNonNull(getActivity())).setListener(this).enablePendingPurchases().build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                mProgressBar.setVisibility(View.GONE);
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.v(TAG, "BILLING | startConnection | RESULT OK");

                    //Success, query for products!
                    if(billingClient.isReady()) {
                        Log.v(TAG, "setupBillingClient: billingClint isReady!");
                        SkuDetailsParams params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(BillingClient.SkuType.SUBS).build();

                        billingClient.querySkuDetailsAsync(params, (billingResult, list) -> {
                            if(result.getResponseCode() == BillingClient.BillingResponseCode.OK)
                            {
                                Log.v(TAG, "setupBillingClient: querySkuDetailsAsync returned OK : "+list.toString());
                                skuDetailsList = list; //save list
                                adapter.reloadData(skuDetailsList); //reload recycler view
                            }
                            else
                                {
                                Log.v(TAG, "Can't querySkuDetailsAsync, responseCode: "+result.getResponseCode());
                            }
                        });
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
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases)
    {
        Log.v(TAG, "onPurchaseUpdated: "+result.getResponseCode());

        Purchase purchase;

        if(purchases != null) {
            if(result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) purchase = billingClient.queryPurchases(BillingClient.SkuType.SUBS).getPurchasesList().get(0);
            else if(result.getResponseCode() == BillingClient.BillingResponseCode.OK) purchase = purchases.get(0);
            else {
                Log.v(TAG, "onPurchasesUpdated error: Response code ("+result.getResponseCode()+")");
                return;
            }

            ConnectFragment connectFragment = (ConnectFragment) Objects.requireNonNull(getActivity()).getSupportFragmentManager().findFragmentByTag("ConnectFragment");
            if (connectFragment != null) {
                connectFragment.verifyReceipt();
            }

            MainActivity mainActivity = (MainActivity)getActivity();
            mainActivity.shouldRedirectToStore = false;

            //Set activity's title and show switch country button
            getActivity().setTitle(getString(R.string.app_name));
            mainActivity.actionBarMenu.getItem(0).setVisible(true);

            //Close this fragment
            getActivity().getSupportFragmentManager().popBackStackImmediate();
        }
    }

    @Override
    public void onItemClick(View view, int position)
    {
        SkuDetails skuDetails = adapter.getItem(position);
        selected_row = position;

        String productTitle = skuDetails.getTitle().split("(?> \\(.+?\\))$")[0]; //Remove redundant app name from subscription's name
        String[] help_urls = getResources().getStringArray(R.array.help_urls); //2=terms of use, 3=privacy policy

        String htmlString, duration;
        if(skuDetails.getSku().equals(skuList.get(0))) {
            //Monthly
            duration = getString(R.string.monthly_duration);
            htmlString = String.format(Locale.getDefault(), getString(R.string.terms_monthly_sub_text), duration, skuDetails.getPrice(), productTitle, help_urls[2], help_urls[3]);

        } else {
            //Annual
            duration = getString(R.string.annual_duration);
            htmlString = String.format(Locale.getDefault(), getString(R.string.terms_annual_sub_text), duration, skuDetails.getPrice(), getString(R.string.terms_annual_discount), productTitle, help_urls[2], help_urls[3]);
        }

        FragmentTransaction transaction = Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.store_fragment_terms_container, TermsFragment.newInstance(productTitle, htmlString, TermsFragment.SUB_INFO_TYPE), "TermsFragment");
        transaction.addToBackStack(null);
        transaction.commit();
    }

    //Method for confirming purchase and launching billing flow
    void confirm_purchase()
    {
        BillingFlowParams billingFlowParams = BillingFlowParams
                .newBuilder()
                .setSkuDetails(adapter.getItem(selected_row))
                .build();

        billingClient.launchBillingFlow(getActivity(), billingFlowParams);
    }
}
