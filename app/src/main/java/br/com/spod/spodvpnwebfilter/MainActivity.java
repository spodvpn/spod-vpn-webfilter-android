package br.com.spod.spodvpnwebfilter;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    private final List<String> skuList = Arrays.asList("spod_vpn_monthly_subscription", "spod_vpn_yearly_subscription");

    private BottomNavigationView bottomNavigation;
    Menu actionBarMenu;

    private CustomSpinner regionSpinner;

    private final NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener
            = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item)
        {
            if(item.getItemId() == R.id.navigation_connect) {
                openFragment(ConnectFragment.newInstance(), true, "ConnectFragment");
                //Check if StoreFragment is visible
                Fragment storeFragment = getSupportFragmentManager().findFragmentByTag("StoreFragment");
                if(storeFragment == null)
                    actionBarMenu.getItem(0).setVisible(true);
                else
                    actionBarMenu.getItem(0).setVisible(!storeFragment.isVisible());
                return true;
            } else if(item.getItemId() == R.id.navigation_alerts) {
                openFragment(AlertsFragment.newInstance(), true, "AlertsFragment");
                actionBarMenu.getItem(0).setVisible(false);
                return true;
            } else if(item.getItemId() == R.id.navigation_more) {
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
        bottomNavigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);

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
        MenuItem item = menu.findItem(R.id.region_spinner);

        if(regionSpinner == null) {
            //Create regionSpinner
            regionSpinner = (CustomSpinner)item.getActionView();
            regionSpinner.removeSpinnerArrow();

            String[] serversHostnames = {"vpn.spod.com.br", "us.vpn.spod.com.br", "eu.vpn.spod.com.br", "in.vpn.spod.com.br"};
            String[] regionsArray = {"", "", "", ""};
            Integer[] flagsArray = {R.drawable.brazil_flag, R.drawable.usa_flag, R.drawable.europe_flag, R.drawable.india_flag};
            RegionSpinnerAdapter spinnerAdapter = new RegionSpinnerAdapter(this, R.layout.region_spinner_layout, regionsArray, flagsArray);
            regionSpinner.setAdapter(spinnerAdapter);

            //ItemSelected listener
            int initialSelectedPosition=regionSpinner.getSelectedItemPosition();
            regionSpinner.setSelection(initialSelectedPosition, false); //clear selection
            regionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    Log.v(TAG, "onItemSelected: Connect to server: "+serversHostnames[position]);
                    ConnectFragment fragment = (ConnectFragment) getSupportFragmentManager().findFragmentByTag("ConnectFragment");
                    if (fragment != null && fragment.isVisible()) fragment.changeRegion(serversHostnames[position]);

                    /*
                    if(mService.getState() == VpnStateService.State.CONNECTING) {
                            return; //Ignore because we're currently connecting!
                    }
                     */
                }

                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });

            //Custom listeners
            regionSpinner.setSpinnerEventsListener(new CustomSpinner.OnSpinnerEventsListener()
            {
                //Add region name next to flag
                @Override public void onSpinnerOpened() {
                    String[] regionsArray = {"South America", "North America", "Central Europe", "Asia Pacific"};
                    spinnerAdapter.setTextArray(regionsArray);
                }

                //Remove region name next to flag
                @Override public void onSpinnerClosed() {
                    String[] regionsArray = {"", "", "", ""};
                    spinnerAdapter.setTextArray(regionsArray);
                }
            });

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

            String serverHostname;
            if(user_profile == null) {
                //No profile found, set region based on user's locale
                Locale locale = getResources().getConfiguration().getLocales().get(0);
                GlobalMethods globalMethods = new GlobalMethods(this);
                serverHostname = globalMethods.getRegionForCountry(locale.getCountry());
            } else {
                //Profile exists, set region based on gateway
                serverHostname = user_profile.getGateway();
            }

            if(serverHostname.startsWith("us.vpn")) regionSpinner.setSelection(1); //North America
            else if(serverHostname.startsWith("eu.vpn")) regionSpinner.setSelection(2); //Central Europe
            else if(serverHostname.startsWith("in.vpn")) regionSpinner.setSelection(3); //Asia Pacific
            else regionSpinner.setSelection(0); //South America (default)

            mDataSource.close();
        }

        return true;
    }

    @Override public void onBackPressed()
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
