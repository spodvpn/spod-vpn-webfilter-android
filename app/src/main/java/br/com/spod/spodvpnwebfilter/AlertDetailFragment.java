package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AlertDetailFragment extends Fragment implements AlertDetailRecyclerViewAdapter.ItemClickListener
{
    private static final String TAG = "AlertDetailFragment";

    private static final String ARG1 = "arg1";
    private static final String ARG2 = "arg2";
    private static final String ARG3 = "arg3";

    private static final int UNBLOCK_HOSTNAME_ROW = 3;

    private String blockType;
    private String blockedHostname;
    private Long blockedHostnameTimestamp;
    private GlobalMethods globalMethods;

    private ProgressBar mProgressBar;
    private RecyclerView recyclerView;

    public AlertDetailFragment() {}

    public static AlertDetailFragment newInstance(String blockType, String blockedHostname, Long blockedHostnameTimestamp)
    {
        AlertDetailFragment fragment = new AlertDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG1, blockType);
        args.putString(ARG2, blockedHostname);
        args.putLong(ARG3, blockedHostnameTimestamp);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            blockType = getArguments().getString(ARG1);
            blockedHostname = getArguments().getString(ARG2);
            blockedHostnameTimestamp = getArguments().getLong(ARG3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.alert_detail_fragment, container, false);

        //Static data to populate RecyclerView
        ArrayList<String> rowsList = new ArrayList<>();
        rowsList.add(getString(R.string.alert_detail_first_row));
        rowsList.add(getString(R.string.alert_detail_second_row));
        rowsList.add(getString(R.string.alert_detail_third_row));
        rowsList.add(getString(R.string.alert_detail_fourth_row));

        //Set up RecyclerView
        recyclerView = view.findViewById(R.id.alert_detail_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        AlertDetailRecyclerViewAdapter adapter = new AlertDetailRecyclerViewAdapter(getActivity(), rowsList, blockType, blockedHostname, blockedHostnameTimestamp);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        mProgressBar = view.findViewById(R.id.alert_detail_recycler_view_progress);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        requireActivity().setTitle(String.format(getString(R.string.alert_detail_title), blockType));

        return view;
    }

    @Override
    public void onItemClick(View view, int position)
    {
        if(position == UNBLOCK_HOSTNAME_ROW)
        {
            //Unblock hostname: Check if it's already in the 'Unblocked sites' list first
            SharedPreferences preferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            Set<String> unblocked_list_set = preferences.getStringSet(getString(R.string.preferences_whitelist_key), null);
            Set<String> blocked_list_set = preferences.getStringSet(getString(R.string.preferences_blacklist_key), null);

            if(unblocked_list_set != null && unblocked_list_set.contains(blockedHostname)) {
                //Show alert indicating this hostname is already unblocked
                if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
                globalMethods.showAlertWithMessage(getString(R.string.alert_detail_unblock_already_unblocked), true);
            }
            else if(blockType.equals(getString(R.string.site)) && blocked_list_set != null && ! blocked_list_set.contains(blockedHostname))
            {
                if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
                globalMethods.showAlertWithMessage(getString(R.string.alert_detail_unblock_already_removed), true);
            }
            else {
                //Confirm before adding hostname to unblocked_list
                ConfirmBottomDialogFragment dialogFragment = ConfirmBottomDialogFragment.newInstance(ConfirmBottomDialogFragment.TYPE_UNBLOCK, getString(R.string.bottom_sheet_unblock_title), blockedHostname, getString(R.string.bottom_sheet_unblock_confirm), getString(R.string.bottom_sheet_cancel));
                dialogFragment.show(getChildFragmentManager(), "ConfirmBottomDialogFragment");
            }
        }
    }

    void unblockHostname()
    {
        recyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE); //show loading progress bar

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity()); //Init if not already initialized

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            if(blockType.equals(getString(R.string.site))) {
                //If the hostname is in the 'Blocked Sites' list, simply remove it
                postData.put("Lista", "Blacklist");
                postData.put("Acao", "Remover");
            } else {
                //Otherwise add it to the 'Unblocked Sites' list
                postData.put("Lista", "Whitelist");
                postData.put("Acao", "Adicionar");
            }
            postData.put("Hostname", blockedHostname);
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to unblock hostname: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/atualizarListas", postData, response -> {
            //Handle response here
            mProgressBar.setVisibility(View.GONE); //stop progress bar
            recyclerView.setVisibility(View.VISIBLE);

            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);
                if(jsonResponse.getString("Status").equals(getString(R.string.request_status_success)))
                {
                    SharedPreferences preferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor preferencesEditor = preferences.edit();
                    String list = getString(R.string.preferences_blacklist_key);

                    if(blockType.equals(getString(R.string.site)))
                    {
                        //Remove hostname from 'Blocked Sites' (where it was initially added)
                        Set<String> listSet = preferences.getStringSet(list, null);
                        if (listSet != null) {
                            listSet.remove(blockedHostname);
                        }
                        preferencesEditor.putStringSet(list, listSet);
                        preferencesEditor.apply();
                    }
                    else
                    {
                        //Add hostname to local 'Blocked Sites' list (SharedPreferences)
                        Set<String> listSet = preferences.getStringSet(list, null);
                        if(listSet == null) {
                            //Currently empty list, create a new one
                            listSet = new HashSet<>();
                        }

                        listSet.add(blockedHostname);
                        preferencesEditor.putStringSet(list, listSet);
                        preferencesEditor.apply();
                    }

                    //Show alert with success message
                    globalMethods.showAlertWithMessage(jsonResponse.getString(getString(R.string.request_message)), true);

                } else {
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }
            } catch (JSONException exception) {
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            }
        });
    }
}
