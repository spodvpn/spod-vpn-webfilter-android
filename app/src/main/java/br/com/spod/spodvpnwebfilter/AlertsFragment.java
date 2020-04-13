package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;


public class AlertsFragment extends Fragment
{
    private static final String TAG = "AlertsFragment";

    // Required empty public constructor
    public AlertsFragment() {}

    private FragmentPagerAdapter adapterViewPager;
    //private GlobalMethods globalMethods;

    public static AlertsFragment newInstance() {
        return new AlertsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        ViewPager viewPager = view.findViewById(R.id.alerts_fragment_view_pager);
        viewPager.setClipToPadding(false);
        viewPager.setPageMargin(12);

        //Check if it's already initialized
        if(adapterViewPager != null) {
            //Already initialized: Re-init
            adapterViewPager = null;
            adapterViewPager = new AlertsPagerAdapter(getChildFragmentManager());
            viewPager.setAdapter(null);
        } else {
            //Create from scratch
            adapterViewPager = new AlertsPagerAdapter(getChildFragmentManager());
        }
        viewPager.setAdapter(adapterViewPager);

        //Set activity's title
        Objects.requireNonNull(getActivity()).setTitle(getString(R.string.title_alerts));

        return view;
    }


    public static class AlertsPagerAdapter extends FragmentPagerAdapter
    {
        private static final int NUM_ITEMS = 3;
        private GlobalMethods globalMethods;

        //Array to hold all 3 adapters (Trackers, Threats and Sites)
        ArrayList<AlertsGenericRecyclerAdapter> adapters = new ArrayList<>();

        AlertsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            adapters.clear();
        }

        @Override
        public int getCount() { return NUM_ITEMS; }

        @Override
        public Fragment getItem(int position)
        {
            switch (position) {
                case 0:
                    //Fragment #0 - this will show Trackers
                    return AlertsGenericFragment.newInstance(0, 0, this);
                case 1:
                    //Fragment #1 - this will show Threats
                    return AlertsGenericFragment.newInstance(0, 1, this);
                case 2:
                    //Fragment #3 - this will show Sites
                    return AlertsGenericFragment.newInstance(0, 2, this);
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

        void showDetail(FragmentActivity context, String blockType, String hostname, Long timestamp)
        {
            //Make fragment container visible
            Objects.requireNonNull(Objects.requireNonNull(context.getSupportFragmentManager().findFragmentByTag(TAG)).getView()).findViewById(R.id.alerts_generic_fragment_detail_container).setVisibility(View.VISIBLE);

            //Add alerts details fragment
            FragmentTransaction transaction = context.getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.alerts_generic_fragment_detail_container, AlertDetailFragment.newInstance(blockType, hostname, timestamp), "AlertDetailFragment");
            transaction.addToBackStack(null);
            transaction.commit();
        }

        void refreshAlerts(FragmentActivity fragmentActivity, SwipeRefreshLayout mSwipeRefresh)
        {
            if(globalMethods == null) this.globalMethods = new GlobalMethods(fragmentActivity);

            //Verify credentials first (Get username and password from VPN profile)
            SharedPreferences sharedPreferences = fragmentActivity.getSharedPreferences(fragmentActivity.getString(R.string.preferences_key), Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(fragmentActivity.getString(R.string.preferences_username), "");

            UUID uuid = null;
            if (username != null) {
                uuid = UUID.nameUUIDFromBytes(username.getBytes());
            }
            VpnProfileDataSource mDataSource = new VpnProfileDataSource(fragmentActivity);
            mDataSource.open();
            VpnProfile profile = null;
            if (uuid != null) {
                profile = mDataSource.getVpnProfile(uuid);
            }
            mDataSource.close();

            if(profile == null || profile.getUsername().isEmpty() || profile.getPassword().isEmpty()) {
                globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.alerts_no_credentials_message), true);
                if(mSwipeRefresh.isRefreshing()) mSwipeRefresh.setRefreshing(false);
                return;
            }

            //Create POST parameters JSONObject
            JSONObject postData = new JSONObject();

            //Actually make the request
            globalMethods.APIRequest("https://spod.com.br/services/vpn/pegarAlertas", postData, response -> {
                //Handle response here
                if(mSwipeRefresh.isRefreshing()) mSwipeRefresh.setRefreshing(false); //Stop refreshing
                JSONObject jsonResponse;
                try {
                    jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("Status").equals(fragmentActivity.getString(R.string.request_status_success)))
                    {
                        JSONArray allAlerts = jsonResponse.getJSONArray("Alertas");
                        if(allAlerts.length() == 0) {
                            globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.alerts_empty), true);
                            return;
                        }

                        //Parse alerts
                        ArrayList<Object> blockTrackingArray = new ArrayList<>();
                        ArrayList<Object> blockThreatsArray = new ArrayList<>();
                        ArrayList<Object> blockSitesArray = new ArrayList<>();

                        for(int i=0; i<allAlerts.length(); i=i+3)
                        {
                            //Add to the correct array
                            if(allAlerts.getString(i).equals("block-tracking")) {
                                blockTrackingArray.add(allAlerts.get(i));
                                blockTrackingArray.add(allAlerts.get(i+1));
                                blockTrackingArray.add(allAlerts.get(i+2));
                            }
                            else if(allAlerts.getString(i).equals("malware-phishing")) {
                                blockThreatsArray.add(allAlerts.get(i));
                                blockThreatsArray.add(allAlerts.get(i+1));
                                blockThreatsArray.add(allAlerts.get(i+2));
                            }
                            else if(allAlerts.getString(i).equals("blacklist")){
                                blockSitesArray.add(allAlerts.get(i));
                                blockSitesArray.add(allAlerts.get(i+1));
                                blockSitesArray.add(allAlerts.get(i+2));
                            }
                        }

                        //Call reloadData in each adapter's class
                        adapters.get(0).reloadData(blockTrackingArray);
                        adapters.get(1).reloadData(blockThreatsArray);
                        adapters.get(2).reloadData(blockSitesArray);
                    } else {
                        globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.error_from_server, jsonResponse.getString(fragmentActivity.getString(R.string.request_message))), true);
                    }
                } catch (JSONException exception) {
                    globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.error_connecting_to_server), true);
                }
            });
        }
    }
}
