package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MoreFragment extends Fragment implements MoreRecyclerViewAdapter.ItemClickListener, SwipeRefreshLayout.OnRefreshListener
{
    private static final String TAG = "MoreFragment";

    private static final int HELP_SECTION = 14;
    private static final int CHANGE_LOG_ROW = 11;
    private static final int UNBLOCK_LIST_ROW = 12;
    private static final int BLOCK_LIST_ROW = 13;
    private static final int RESET_USAGE_ROW = 7;
    private static final int COPYRIGHT_ROW = 20;
    private static final int SUB_INFO_ROW = 3;

    private VpnProfile profile;
    private GlobalMethods globalMethods;

    private SwipeRefreshLayout mSwipeRefresh;

    //Required empty public constructor
    public MoreFragment() { }

    MoreRecyclerViewAdapter adapter;

    public static MoreFragment newInstance() {
        return new MoreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //Inflate the fragment layout from XML
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        //Data to populate RecyclerView
        ArrayList<String> rowsList = new ArrayList<>();
        //Section 0 : App
        rowsList.add(getString(R.string.more_tab_first_section));
        rowsList.add(getString(R.string.more_tab_first_item));
        rowsList.add(getString(R.string.more_tab_second_item));
        rowsList.add(getString(R.string.more_tab_third_item));
        rowsList.add(getString(R.string.more_tab_fifth_item));
        rowsList.add(getString(R.string.more_tab_sixth_item));
        rowsList.add(getString(R.string.more_tab_seventh_item));
        rowsList.add(getString(R.string.more_tab_eigth_item));
        //Section 1 : Web Filter
        rowsList.add(getString(R.string.more_tab_second_section));
        rowsList.add(getString(R.string.more_tab_ninth_item));
        rowsList.add(getString(R.string.more_tab_tenth_item));
        rowsList.add(getString(R.string.more_tab_eleventh_item));
        rowsList.add(getString(R.string.more_tab_twelfth_item));
        rowsList.add(getString(R.string.more_tab_thirteenth_item));
        //Section 2 : Help
        rowsList.add(getString(R.string.more_tab_third_section));
        rowsList.add(getString(R.string.more_tab_fourteenth_item));
        rowsList.add(getString(R.string.more_tab_fifteenth_item));
        rowsList.add(getString(R.string.more_tab_sixteenth_item));
        rowsList.add(getString(R.string.more_tab_seventeenth_item));
        rowsList.add(getString(R.string.more_tab_eighteenth_item));
        //Copyright
        rowsList.add(getString(R.string.copyright));

        //Set up RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.more_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new MoreRecyclerViewAdapter(getActivity(), rowsList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));

        //Set activity's title
        Objects.requireNonNull(getActivity()).setTitle(getString(R.string.title_more_info));

        //Setup SwipeRefresh
        mSwipeRefresh = view.findViewById(R.id.more_fragment_refresh);
        mSwipeRefresh.setOnRefreshListener(this);

        //Prevent scroll to refresh on other (child) fragments
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                mSwipeRefresh.setEnabled(mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });

        refreshMoreInfo(); //Fetch data from server and update view
        return view;
    }

    @Override
    public void onItemClick(View view, int position)
    {
        if(position == RESET_USAGE_ROW)
        {
            //Open BottomSheetDialog to confirm reset
            String title = getString(R.string.bottom_sheet_title);

            //Read last reset from SharedPreferences
            String subtitle;
            SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            long last_reset_timestamp = sharedPreferences.getLong(getString(R.string.preferences_reset_download_upload), 0L);
            if (last_reset_timestamp == 0) subtitle = getString(R.string.bottom_sheet_last_reset, getString(R.string.never));
            else {
                //Format the timestamp first
                Date date = new Date(last_reset_timestamp);
                DateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy HH:mm", Locale.getDefault());
                String formattedDate = dateFormat.format(date);
                subtitle = getString(R.string.bottom_sheet_last_reset, formattedDate);
            }

            //Confirm and deny buttons
            String confirm = getString(R.string.bottom_sheet_confirm);
            String deny = getString(R.string.bottom_sheet_cancel);

            //Create and present dialog
            ConfirmBottomDialogFragment dialogFragment = ConfirmBottomDialogFragment.newInstance(ConfirmBottomDialogFragment.TYPE_RESET, title, subtitle, confirm, deny);
            dialogFragment.show(getChildFragmentManager(), "ConfirmBottomDialogFragment");

        }
        else if(position == CHANGE_LOG_ROW)
        {
            //Open change log fragment
            FragmentTransaction transaction = Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction();
            String title = String.format("%s: %s", getString(R.string.more_tab_eleventh_item), adapter.firewallVersion);
            transaction.replace(R.id.more_fragment_container, TermsFragment.newInstance(title, adapter.changeLog, TermsFragment.CHANGELOG_TYPE), "ChangelogFragment");
            transaction.addToBackStack(null);
            transaction.commit();
        }
        else if(position == UNBLOCK_LIST_ROW || position == BLOCK_LIST_ROW)
        {
            //Open Unblocked/Blocked Sites
            FragmentTransaction transaction = Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.more_fragment_container, ListFragment.newInstance(position-UNBLOCK_LIST_ROW), "ListFragment");
            transaction.addToBackStack(null);
            transaction.commit();
        }
        else if(position > HELP_SECTION && position < COPYRIGHT_ROW)
        {
            //Open link in the browser
            String[] help_urls = getResources().getStringArray(R.array.help_urls);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(help_urls[position-HELP_SECTION-1]));
            startActivity(browserIntent);
        }
        else if(position == SUB_INFO_ROW) {
            //Open Google Play's subscription management page
            Intent manageSubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"));
            startActivity(manageSubIntent);
        }
    }

    @Override
    public void onRefresh() {
        refreshMoreInfo();
    }

    private void refreshMoreInfo()
    {
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity()); //Init if not already initialized

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("Regiao", getString(R.string.region)); //Region-specific
            Long last_reset_timestamp = Objects.requireNonNull(getActivity()).getSharedPreferences(getActivity().getString(R.string.preferences_key), Context.MODE_PRIVATE).getLong(getString(R.string.preferences_reset_download_upload), 0L);
            postData.put("ResetDownloadUpload", last_reset_timestamp); //Reset download/upload option

        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to load more info: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/moreinfo2", postData, response -> {
            //Handle response here
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);

                if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success)))
                {
                    //Get connected server (if connected)
                    MainActivity mainActivity = (MainActivity)getActivity();
                    if (mainActivity != null) {
                        adapter.connectedServer = (mainActivity.server_connected.length() > 0 ? mainActivity.server_connected : getString(R.string.not_available));
                    }

                    //Get values from JSON
                    adapter.subscriptionType = jsonResponse.optString("SubscriptionType", getString(R.string.not_available));
                    adapter.totalDownload = globalMethods.formatBytes(jsonResponse.optDouble("TotalDownload", 0.00));
                    adapter.totalUpload = globalMethods.formatBytes(jsonResponse.optDouble("TotalUpload", 0.00));
                    adapter.firewallVersion = jsonResponse.optString("FirewallRevision", getString(R.string.not_available));
                    adapter.changeLog = jsonResponse.optString("FirewallUpdateChangelog", getString(R.string.not_available));

                    //Format date
                    Date date = new Date(jsonResponse.optLong("FirewallUpdateDate", 0L) * 1000L);
                    DateFormat format = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
                    adapter.firewallReleaseDate = format.format(date);

                } else {
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }

            } catch (JSONException exception) {
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            } catch (IllegalStateException exception) {
                Log.v(TAG, "IllegalStateException from Volley: This happens when the request is interrupted...");
            }

            if(mSwipeRefresh.isRefreshing()) mSwipeRefresh.setRefreshing(false);

            //Custom method to reload because adapter.notifyDataSetChanged wouldn't reload visible rows
            adapter.prepareForReload();
        });
    }

}
