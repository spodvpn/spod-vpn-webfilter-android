package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class AlertsFragment extends Fragment
{
    private static final String TAG = "AlertsFragment";

    // Required empty public constructor
    public AlertsFragment() {}

    private FragmentStateAdapter adapterViewPager;
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

        ViewPager2 viewPager = view.findViewById(R.id.alerts_fragment_view_pager);
        viewPager.setClipToPadding(false);
        //viewPager.setPageMargin(12);

        //Check if it's already initialized
        if(adapterViewPager != null) {
            //Already initialized: Re-init
            adapterViewPager = null;
            adapterViewPager = new AlertsPagerAdapter(getChildFragmentManager(), getLifecycle());
            viewPager.setAdapter(null);
        } else {
            //Create from scratch
            adapterViewPager = new AlertsPagerAdapter(getChildFragmentManager(), getLifecycle());
        }
        viewPager.setAdapter(adapterViewPager);
        viewPager.setOffscreenPageLimit(3);

        //Set activity's title
        requireActivity().setTitle(getString(R.string.title_alerts));

        return view;
    }

    public static class AlertsPagerAdapter extends FragmentStateAdapter
    {
        private static final int NUM_ITEMS = 4;
        private GlobalMethods globalMethods;
        boolean fullAlertsList = false;

        //Array to hold all 4 adapters (Summary, Trackers, Threats and Sites)
        ArrayList<AlertsGenericRecyclerAdapter> adapters = new ArrayList<>();

        AlertsPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
            adapters.clear();
        }

        @Override
        public int getItemCount() { return NUM_ITEMS; }

        @NonNull @Override
        public Fragment /*/getItem*/createFragment(int position)
        {
            switch (position) {
                default:
                case 0:
                    //Fragment #0 - this will show Web Filter's Activity Summary
                    return AlertsGenericFragment.newInstance(0, 0, this);
                case 1:
                    //Fragment #1 - this will show Trackers
                    return AlertsGenericFragment.newInstance(0, 1, this);
                case 2:
                    //Fragment #2 - this will show Threats
                    return AlertsGenericFragment.newInstance(0, 2, this);
                case 3:
                    //Fragment #3 - this will show Sites
                    return AlertsGenericFragment.newInstance(0, 3, this);
            }
        }

        /*@Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }*/

        void showDetail(FragmentActivity context, String blockType, String hostname, Long timestamp)
        {
            //Make fragment container visible
            Objects.requireNonNull(context.getSupportFragmentManager().findFragmentByTag(TAG)).requireView().findViewById(R.id.alerts_generic_fragment_detail_container).setVisibility(View.VISIBLE);

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
            if (username.getBytes().length > 0) {
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
                showMessageNoAlerts(fragmentActivity);
                if(mSwipeRefresh != null && mSwipeRefresh.isRefreshing()) mSwipeRefresh.setRefreshing(false);
                return;
            }

            //Create POST parameters JSONObject
            JSONObject postData = new JSONObject();
            try {
                postData.put("ListaCompleta", this.fullAlertsList);
            } catch (JSONException exception) {
                Log.v(TAG, "JSONException while trying to fetch alerts: " + exception.getLocalizedMessage());
                exception.printStackTrace();
            }

            //Actually make the request
            globalMethods.APIRequest("https://spod.com.br/services/vpn/pegarAlertas2", postData, response -> {
                //Handle response here
                if(mSwipeRefresh != null && mSwipeRefresh.isRefreshing()) mSwipeRefresh.setRefreshing(false); //Stop refreshing
                JSONObject jsonResponse;
                try {
                    jsonResponse = new JSONObject(response);

                    if (jsonResponse.getString("Status").equals(fragmentActivity.getString(R.string.request_status_success)))
                    {
                        //JSONArray allAlerts = jsonResponse.getJSONArray("Alertas");
                        JSONArray trackersAlerts = jsonResponse.getJSONArray("TrackersAlerts");
                        JSONArray threatsArray = jsonResponse.getJSONArray("ThreatsAlerts");
                        JSONArray blacklistArray = jsonResponse.getJSONArray("BlacklistAlerts");

                        if(trackersAlerts.length() == 0 && threatsArray.length() == 0 && blacklistArray.length() == 0) {
                            //globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.alerts_empty), true);
                            showMessageNoAlerts(fragmentActivity);
                            return;
                        }

                        //Parse alerts
                        ArrayList<Object> blockTrackingArray = new ArrayList<>();
                        ArrayList<Object> blockThreatsArray = new ArrayList<>();
                        ArrayList<Object> blockSitesArray = new ArrayList<>();

                        if(trackersAlerts.length() > 0) {
                            for(int i=0; i<trackersAlerts.length(); i=i+2) {
                                blockTrackingArray.add(trackersAlerts.get(i));
                                blockTrackingArray.add(trackersAlerts.get(i+1));
                            }
                        }
                        if(threatsArray.length() > 0) {
                            for(int i=0; i<threatsArray.length(); i=i+2) {
                                blockThreatsArray.add(threatsArray.get(i));
                                blockThreatsArray.add(threatsArray.get(i+1));
                            }
                        }
                        if(blacklistArray.length() > 0) {
                            for(int i=0; i<blacklistArray.length(); i=i+2) {
                                blockSitesArray.add(blacklistArray.get(i));
                                blockSitesArray.add(blacklistArray.get(i+1));
                            }
                        }

                        int totalTrackers = jsonResponse.getInt("TotalTrackers");
                        int totalThreats = jsonResponse.getInt("TotalThreats");
                        int totalBlacklist = jsonResponse.getInt("TotalBlacklist");
                        int nTotal = totalTrackers + totalThreats + totalBlacklist;
                        ArrayList<Object> totalsArray = new ArrayList<>();
                        totalsArray.add(totalTrackers);
                        totalsArray.add(totalThreats);
                        totalsArray.add(totalBlacklist);

                        //Call reloadData in each adapter's class
                        adapters.get(0).reloadData(totalsArray, nTotal);
                        adapters.get(1).reloadData(blockTrackingArray, jsonResponse.getInt("TotalTrackers"));
                        adapters.get(2).reloadData(blockThreatsArray, jsonResponse.getInt("TotalThreats"));
                        if(adapters.size()>3)
                            adapters.get(3).reloadData(blockSitesArray, jsonResponse.getInt("TotalBlacklist"));

                    } else {
                        globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.error_from_server, jsonResponse.getString(fragmentActivity.getString(R.string.request_message))), true);
                    }
                } catch (JSONException exception) {
                    globalMethods.showAlertWithMessage(fragmentActivity.getString(R.string.error_connecting_to_server), true);

                    Log.v(TAG, "JSONException while fetching alerts:");
                    exception.printStackTrace();
                }
            });
        }

        private void showMessageNoAlerts(FragmentActivity fragmentActivity)
        {
            boolean isConnected = false;
            boolean profileExists;

            //Check if VPN profile already exists
            SharedPreferences sharedPreferences = fragmentActivity.getSharedPreferences(fragmentActivity.getString(R.string.preferences_key), Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(fragmentActivity.getString(R.string.preferences_username), "");

            UUID uuid = null;
            if (username.getBytes().length > 0) {
                uuid = UUID.nameUUIDFromBytes(username.getBytes());
            }
            VpnProfileDataSource mDataSource = new VpnProfileDataSource(fragmentActivity);
            mDataSource.open();
            VpnProfile profile = null;
            if (uuid != null) {
                profile = mDataSource.getVpnProfile(uuid);
            }
            mDataSource.close();

            if(profile == null || profile.getUsername().isEmpty() || profile.getPassword().isEmpty())
                profileExists = false;
            else {
                profileExists = true;
                //Detect current connection status
                MainActivity mainActivity = (MainActivity)fragmentActivity;
                if(mainActivity.server_connected != null && !mainActivity.server_connected.isEmpty()) {
                    isConnected = true;
                }
            }

            try {
                //Make container visible
                FrameLayout frameLayout = fragmentActivity.findViewById(R.id.alerts_fragment_message);
                frameLayout.setVisibility(View.VISIBLE);

                View fixedMessageView = View.inflate(fragmentActivity, R.layout.fragment_fixed_message, frameLayout);
                TextView titleTextView = fixedMessageView.findViewById(R.id.fragment_fixed_message_title);
                TextView messageTextView = fixedMessageView.findViewById(R.id.fragment_fixed_message_text);

                if (!profileExists || !isConnected) {
                    //Unsubscribed and/or disconnected
                    titleTextView.setTextColor(Color.RED);
                    titleTextView.setText(fragmentActivity.getString(R.string.alerts_empty_disconnected_title));
                    messageTextView.setText(fragmentActivity.getString(R.string.alerts_empty_disconnected_text));

                } else {
                    //Subscribed and connected
                    titleTextView.setTextColor(fragmentActivity.getResources().getColorStateList(R.color.connected_green, null));
                    titleTextView.setText(fragmentActivity.getString(R.string.alerts_empty_connected_title));
                    messageTextView.setText(fragmentActivity.getString(R.string.alerts_empty_connected_text));
                }
            } catch (Exception e) {
                Log.v(TAG, "showMessageNoAlerts: Got an exception: "+e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
