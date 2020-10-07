package br.com.spod.spodvpnwebfilter;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener  {

    private static final String TAG = "MainActivity";

    String server_connected = "";
    boolean shouldRedirectToStore = false;
    boolean billingSetupFinished;
    String firebaseTokenId = "";
    boolean subscribeToCustomFreeTrial = false;

    BillingClient billingClient;
    private List<String> skuList = Arrays.asList("spod_vpn_monthly_subscription", "spod_vpn_yearly_subscription");

    private BottomNavigationView bottomNavigation;
    Menu actionBarMenu;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_connect:
                    openFragment(ConnectFragment.newInstance(), true, "ConnectFragment");
                    //Check if StoreFragment is visible
                    Fragment storeFragment = getSupportFragmentManager().findFragmentByTag("StoreFragment");
                    if(storeFragment == null)
                        actionBarMenu.getItem(0).setVisible(true);
                    else
                        actionBarMenu.getItem(0).setVisible(!storeFragment.isVisible());
                    return true;
                case R.id.navigation_alerts:
                    openFragment(AlertsFragment.newInstance(), true, "AlertsFragment");
                    actionBarMenu.getItem(0).setVisible(false);
                    return true;
                case R.id.navigation_more:
                    openFragment(MoreFragment.newInstance(), true, "MoreFragment");
                    actionBarMenu.getItem(0).setVisible(false);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupBillingClient();

        bottomNavigation = findViewById(R.id.navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Open default main fragment
        openFragment(ConnectFragment.newInstance(), false, "ConnectFragment");
    }

    @Override
    public void onResume() {
        super.onResume();

        //Clear all alert notifications
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.cancelAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Inflate menu to add button to action bar
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);

        actionBarMenu = menu;
        MenuItem countryButton = menu.getItem(0);

        //If there's a VPN profile installed, use it to determine the correct country
        VpnProfileDataSource mDataSource = new VpnProfileDataSource(this);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");
        VpnProfile user_profile = null;
        UUID uuid;
        mDataSource.open();
        if (username.getBytes().length > 0) {
            uuid = UUID.nameUUIDFromBytes(username.getBytes());
            user_profile = mDataSource.getVpnProfile(uuid);

        }
        if(user_profile == null) {
            //No profile found, use language to set the country
            if(getString(R.string.region).equals("BR")) countryButton.setIcon(R.drawable.brazil_flag);
            else countryButton.setIcon(R.drawable.usa_flag);
        }
        else if(user_profile.getGateway().startsWith("us"))
            countryButton.setIcon(R.drawable.usa_flag);
        else
            countryButton.setIcon((R.drawable.brazil_flag));
        mDataSource.close();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        ConnectFragment fragment = (ConnectFragment) getSupportFragmentManager().findFragmentByTag("ConnectFragment");
        if (fragment != null && fragment.isVisible()) fragment.changeServerLocation();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        //Find current visible fragment and select bottomNavigation's corresponding item
        String[] tags = {"ConnectFragment", "AlertsFragment", "MoreFragment"};
        String[] titles = {getString(R.string.app_name), getString(R.string.title_alerts), getString(R.string.title_more_info)};

        for(int i=0; i< tags.length; i++) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(tags[i]);
            if(currentFragment != null && currentFragment.isVisible()) {
                //Found the visible fragment
                bottomNavigation.getMenu().getItem(i).setChecked(true);

                //Enable action bar only on ConnectFragment
                if (currentFragment.getTag() != null) {
                    if(currentFragment.getTag().equals("ConnectFragment")) {
                        //Check if StoreFragment is visible
                        Fragment storeFragment = getSupportFragmentManager().findFragmentByTag("StoreFragment");
                        if(storeFragment == null || ! storeFragment.isVisible()) {
                            //StoreFragment is not visible, proceed as usual
                            setTitle(titles[i]);
                            actionBarMenu.getItem(0).setVisible(true);
                        }
                    }
                    else {
                        //Hide actionBarMenu
                        actionBarMenu.getItem(0).setVisible(false);
                        setTitle(titles[i]);
                    }
                }

                //Special case for an alert's detail view and/or StoreFragment
                if(tags[i].equals("AlertsFragment")) {
                    currentFragment.requireView().findViewById(R.id.alerts_generic_fragment_detail_container).setVisibility(View.GONE);
                } else if(tags[i].equals("StoreFragment")) {
                    currentFragment.requireView().findViewById(R.id.store_fragment_container).setVisibility(View.GONE);
                }
            }
        }
    }

    private void openFragment(Fragment fragment, boolean addToBackStack, String tag)
    {
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(tag);
        //Check if fragment is not already visible!
        if(currentFragment == null || ! currentFragment.isVisible()) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment, tag);
            if(addToBackStack) transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void setupBillingClient()
    {
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                billingSetupFinished = true;
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK)
                {
                    Log.v(TAG, "setupBillingClient: BILLING | startConnection | RESULT OK");
                    if(billingClient.isReady())
                    {
                        SkuDetailsParams params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(BillingClient.SkuType.SUBS).build();
                        billingClient.querySkuDetailsAsync(params, (result1, skuDetailsList) -> {
                            if (result1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Log.v(TAG, "setupBillingClient: querySkuDetailsAsync, responseCode: "+ result1.getResponseCode());
                                ConnectFragment connectFragment = (ConnectFragment)getSupportFragmentManager().findFragmentByTag("ConnectFragment");
                                if(connectFragment != null) connectFragment.verifyReceipt(); //Everything ready, call verifyReceipt
                            } else {
                                Log.v(TAG, "setupBillingClient: Can't querySkuDetailsAsync, responseCode: "+ result1.getResponseCode());
                            }
                        });
                    } else {
                        Log.v(TAG, "setupBillingClient: Billing Client not ready");
                    }
                } else {
                    Log.v(TAG, "setupBillingClient: BILLING | startConnection | RESULT: " +result.getResponseCode());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.v(TAG, "BILLING | onBillingServiceDisconnected | DISCONNECTED");
            }
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases)
    {
        Log.v(TAG, "onPurchaseUpdated: "+result.getResponseCode());
    }
}
