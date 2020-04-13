package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ListFragment extends Fragment implements ListRecyclerViewAdapter.ItemClickListener
{
    private static final String TAG = "ListFragment";

    private static final String ARG_PARAM1 = "param1";

    private static final int UNBLOCK_LIST_TYPE = 0; //Whitelist
    private static final int BLOCK_LIST_TYPE = 1; //Blacklist

    private EditText addHostnameText;
    private ProgressBar mProgressBar;
    private RecyclerView recyclerView;
    private ImageButton addButton;
    private GlobalMethods globalMethods;

    private int listType;

    ListRecyclerViewAdapter adapter;

    //Required empty public constructor
    public ListFragment() { }

    public static ListFragment newInstance(int listType)
    {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, listType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listType = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        //Set title accordingly
        Objects.requireNonNull(getActivity()).setTitle(listType == UNBLOCK_LIST_TYPE ? getString(R.string.unblock_list_title) : getString(R.string.block_list_title));

        //Setup button
        addButton = view.findViewById(R.id.custom_list_add_button);
        addButton.setOnClickListener(v -> addHostname());

        mProgressBar = view.findViewById(R.id.custom_list_recycler_progress);
        addHostnameText = view.findViewById(R.id.custom_list_add_text);
        addHostnameText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if(actionId == EditorInfo.IME_ACTION_DONE) addHostname();
            return false;
        });

        //Data to populate RecyclerView
        ArrayList<String> rowsList = new ArrayList<>();
        String list = (listType == 0 ? getString(R.string.preferences_whitelist_key) : getString(R.string.preferences_blacklist_key));
        SharedPreferences preferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        Set<String> listSet = preferences.getStringSet(list, null);
        if(listSet != null && listSet.size() > 0) {
            ArrayList<String> listArray = new ArrayList<>(listSet);
            for (int i = 0; i < listArray.size(); i++) {
                if(listArray.get(i).equals("")) continue;
                rowsList.add(listArray.get(i));
            }
        }

        //Set up RecyclerView
        recyclerView = view.findViewById(R.id.custom_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ListRecyclerViewAdapter(getActivity(), rowsList, listType);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        loadCustomList(); //populate list
        return view;
    }

    @Override
    public void onItemClick(View view, int position) {
        removeHostname(position);
    }

    //Method to show and hide loading progress
    private void showLoadingProgress(boolean shouldShow) {
        if(shouldShow) {
            mProgressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            addButton.setClickable(false);
        } else {
            mProgressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            addButton.setClickable(true);
        }
    }

    //Refresh and populate list
    private void loadCustomList()
    {
        showLoadingProgress(true);

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("Lista", (listType == 0 ? "Whitelist" : "Blacklist"));
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to load custom list: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/carregarLista", postData, response -> {
            //Handle response here
            showLoadingProgress(false);

            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);

                if(jsonResponse.getString("Status").equals(getString(R.string.request_status_success)))
                {
                    //Populate SharedPreferences and call adapter.reloadData to update screen
                    String list = (listType == 0 ? getString(R.string.preferences_whitelist_key) : getString(R.string.preferences_blacklist_key));
                    JSONArray jsonArray = jsonResponse.getJSONArray("Lista");
                    Set<String> listSet = new HashSet<>();
                    for(int i=0; i<jsonArray.length(); i++) listSet.add(jsonArray.getString(i));

                    SharedPreferences preferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor preferencesEditor = preferences.edit();
                    preferencesEditor.putStringSet(list, listSet);
                    preferencesEditor.apply();

                    adapter.reloadData(listSet);
                } else {
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }
            } catch (JSONException exception) {
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            }
        });
    }

    //Method for including hostname in custom list
    private void addHostname()
    {
        //First step is to check for a valid hostname!
        if(addHostnameText.getText().toString().equals("")) return; //Empty field, ignore
        if(! addHostnameText.getText().toString().matches("^(([a-zA-Z\\d]|[a-zA-Z\\d][a-zA-Z\\d\\-]*[a-zA-Z\\d])\\.)*([A-Za-z\\d]|[A-Za-z\\d][A-Za-z\\d\\-]*[A-Za-z\\d])$") || addHostnameText.getText().toString().contains(":") || addHostnameText.getText().toString().contains("/") || addHostnameText.getText().toString().contains(",")) {
            //Invalid hostname
            Log.v(TAG, "Invalid hostname, show an error dialog/alert !");
            return;
        }

        showLoadingProgress(true); //show loading progress bar

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("Lista", (listType == 0 ? "Whitelist" : "Blacklist"));
            postData.put("Hostname", addHostnameText.getText().toString());
            postData.put("Acao", "Adicionar");
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to add hostname to custom list: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/atualizarListas", postData, response -> {
            //Handle response here
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);
                showLoadingProgress(false); //Hide progress bar
                if(jsonResponse.getString("Status").equals(getString(R.string.request_status_success)))
                {
                    //Add hostname to local list (SharedPreferences)
                    String list = (listType == 0 ? getString(R.string.preferences_whitelist_key) : getString(R.string.preferences_blacklist_key));
                    SharedPreferences preferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    Set<String> listSet = preferences.getStringSet(list, null);
                    if(listSet == null) {
                        //Currently empty list, create a new one
                        listSet = new HashSet<>();
                    }

                    listSet.add(addHostnameText.getText().toString());
                    SharedPreferences.Editor preferencesEditor = preferences.edit();
                    preferencesEditor.putStringSet(list, listSet);
                    preferencesEditor.apply();

                    addHostnameText.setText(""); //clear input text field
                    adapter.reloadData(listSet);

                    //Show alerts with success message
                    globalMethods.showAlertWithMessage(jsonResponse.getString(getString(R.string.request_message)), true);

                } else {
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }

            } catch (JSONException exception) {
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            }
        });
    }

    //Method for removing hostname from custom list
    private void removeHostname(int position)
    {
        showLoadingProgress(true); //show loading progress bar

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("Lista", (listType == 0 ? "Whitelist" : "Blacklist"));
            postData.put("Hostname", adapter.getItem(position));
            postData.put("Acao", "Remover");
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to remove hostname from custom list: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/atualizarListas", postData, response -> {
            //Handle response here
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);
                showLoadingProgress(false); //hide progress bar

                if(jsonResponse.getString("Status").equals(getString(R.string.request_status_success)))
                {
                    //Remove hostname from local list (SharedPreferences)
                    String list = (listType == 0 ? getString(R.string.preferences_whitelist_key) : getString(R.string.preferences_blacklist_key));
                    SharedPreferences preferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    Set<String> listSet = preferences.getStringSet(list, null);
                    if (listSet != null) {
                        listSet.remove(adapter.getItem(position));
                    }
                    SharedPreferences.Editor preferencesEditor = preferences.edit();
                    preferencesEditor.putStringSet(list, listSet);
                    preferencesEditor.apply();
                    adapter.reloadData(listSet);

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
