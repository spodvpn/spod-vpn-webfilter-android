package br.com.spod.spodvpnwebfilter;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.security.LocalCertificateStore;
import org.strongswan.android.ui.VpnProfileControlActivity;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import static org.strongswan.android.utils.Constants.MTU_MIN;

public class ConnectFragment extends Fragment implements VpnStateService.VpnStateListener, View.OnClickListener
{
    private static final String TAG = "ConnectFragment";

    private TextView blockTrackersButton;
    private ImageButton blockTrackersImageButton;
    private ImageButton blockTrackersCheckmarkButton;

    private TextView blockThreatsButton;
    private ImageButton blockThreatsImageButton;
    private ImageButton blockThreatsCheckmarkButton;

    private TextView settingsAppButton;
    private ImageButton settingsAppImageButton;

    private ProgressBar mProgressBar;
    private GlobalMethods globalMethods;

    private boolean tryingToConnect;
    private List<Long> connectErrorArray;

    private Button statusButton;
    private ImageButton statusImageButton;
    private VpnProfileDataSource mDataSource;
    private VpnStateService mService;
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName componentName) { mService = null; }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = ((VpnStateService.LocalBinder)service).getService();
            if(mDataSource != null) mService.registerListener(ConnectFragment.this);
            stateChanged();
        }
    };

    //Required public constructor
    public ConnectFragment() { }

    public static ConnectFragment newInstance() {
        return new ConnectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Bind service and open profile's DataSource (database)
        Objects.requireNonNull(getActivity()).getApplicationContext().bindService(new Intent(getActivity().getApplicationContext(), VpnStateService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
        mDataSource = new VpnProfileDataSource(getActivity());

        SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");

        //Try to open profile with UUID based on username
        UUID user_uuid;
        if (username != null) {
            user_uuid = UUID.nameUUIDFromBytes(username.getBytes());
            mDataSource.open();
            VpnProfile profile = mDataSource.getVpnProfile(user_uuid);
            if(username.length() == 0 || profile.getGateway() == null || profile.getGateway().length() == 0) {
                //VPN has not been configured yet, show initial message
                if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
                globalMethods.showAlertWithMessage(getString(R.string.new_user_message), false);
            }
            mDataSource.close();
        }

        connectErrorArray = new ArrayList<>(); //Initialize array
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        statusButton = view.findViewById(R.id.statusButton);
        statusButton.setOnClickListener(this::statusButtonClicked);

        statusImageButton = view.findViewById(R.id.statusImageButton);
        statusImageButton.setOnClickListener(this::statusButtonClicked);

        //Setup buttons and onClick listeners
        mProgressBar = view.findViewById(R.id.connect_fragment_progress);

        TextView websiteButton = view.findViewById(R.id.connect_website_button);
        websiteButton.setOnClickListener(this);

        blockTrackersButton = view.findViewById(R.id.connect_block_trackers_button);
        blockTrackersButton.setOnClickListener(this);

        blockTrackersImageButton = view.findViewById(R.id.connect_block_trackers_image_button);
        blockTrackersImageButton.setOnClickListener(this);

        blockTrackersCheckmarkButton = view.findViewById(R.id.connect_block_trackers_checkmark_button);
        blockTrackersCheckmarkButton.setOnClickListener(this);

        blockThreatsButton = view.findViewById(R.id.connect_block_threats_button);
        blockThreatsButton.setOnClickListener(this);

        blockThreatsImageButton = view.findViewById(R.id.connect_block_threats_image_button);
        blockThreatsImageButton.setOnClickListener(this);
        blockThreatsCheckmarkButton = view.findViewById(R.id.connect_block_threats_checkmark_button);
        blockThreatsCheckmarkButton.setOnClickListener(this);

        //Adjust buttons width according to the size of the title
        if(getString(R.string.region).equals("US")) {
            //US: 120dp
            blockTrackersButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
            blockThreatsButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
        } else {
            //BR: 170dp
            blockTrackersButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
            blockThreatsButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
        }

        settingsAppButton = view.findViewById(R.id.connect_toggle_always_on_button);
        settingsAppButton.setOnClickListener(this);
        settingsAppImageButton = view.findViewById(R.id.connect_toggle_always_on_image_button);
        settingsAppImageButton.setOnClickListener(this);

        //Start disconnected
        statusImageButton.setImageResource(R.drawable.status_image_animation_off_on);
        statusImageButton.setAlpha(0.5f);

        enableButtons(false);
        Objects.requireNonNull(getActivity()).setTitle(getString(R.string.app_name));

        return view;
    }

    //Fragment lifecycle methods
    @Override
    public void onStop() {
        super.onStop();
        if (mService != null) mService.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mService != null) stateChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(getActivity()).getApplicationContext().unbindService(mServiceConnection);
    }

    @Override
    public void stateChanged()
    {
        Log.v(TAG, "stateChanged: "+mService.getState().toString());

        //Identify current state
        if(mService.getState() == VpnStateService.State.CONNECTED && (this.tryingToConnect || statusImageButton.getAlpha() == 0.5f || ! statusButton.getText().equals(getString(R.string.connected))))
        {
            //Set connectedServer string
            MainActivity mainActivity = (MainActivity)getActivity();
            if (mainActivity != null) {
                mainActivity.server_connected = mService.getProfile().getGateway();
            }

            //Set status text button
            statusButton.setText(R.string.connected);
            statusButton.setBackgroundTintList(getActivity().getResources().getColorStateList(R.color.connected_green, null));
            statusButton.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()));

            this.tryingToConnect = false;

            //Animation: Off -> On
            Log.v(TAG, "Animation: Off -> On");

            //Step 1: Fade in
            statusImageButton.animate().alpha(1.0f).setDuration(700).setListener(null);

            //Step 2: Change images (only if we were disconnected before)
            if(statusImageButton.getDrawable() != getActivity().getResources().getDrawable(R.drawable.status_image_animation_off_on, null))
                statusImageButton.setImageResource(R.drawable.status_image_animation_off_on);
            AnimationDrawable turnOnAnimation = (AnimationDrawable) statusImageButton.getDrawable();
            turnOnAnimation.start();

            //Step 3: Enable buttons
            enableButtons(true);

        }
        else if((mService.getState() == VpnStateService.State.DISABLED || mService.getState() == VpnStateService.State.DISCONNECTING) && (statusImageButton.getAlpha() == 1.0f || ! statusButton.getText().equals(getString(R.string.disconnected))))
        {
            statusButton.setText(R.string.disconnected);
            statusButton.setBackgroundTintList(Objects.requireNonNull(getActivity()).getResources().getColorStateList(R.color.disconnected_red, null));
            statusButton.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics()));

            //Set connectedServer string
            MainActivity mainActivity = (MainActivity)getActivity();
            mainActivity.server_connected = "";

            //Animation: On -> Off
            Log.v(TAG, "Animation: On -> Off");

            //Step 1: Fade out
            statusImageButton.animate().alpha(0.5f).setDuration(700).setListener(null);

            //Step 2: Change images
            statusImageButton.setImageResource(R.drawable.status_image_animation_on_off);
            AnimationDrawable turnOffAnimation = (AnimationDrawable)statusImageButton.getDrawable();
            turnOffAnimation.start();

            //Step 3: Disable buttons
            enableButtons(false);

            if(this.tryingToConnect) {
                //Was trying to connect but wasn't successful
                if(this.connectErrorArray.size() < 3) {
                    //Lower than 3, save timestamp
                    this.connectErrorArray.add(System.currentTimeMillis());
                } else {
                    //Greater than 3, verify if it's stuck!
                    Long firstErrorTimestamp = this.connectErrorArray.get(0);
                    Long lastErrorTimestamp = this.connectErrorArray.get(this.connectErrorArray.size()-1);
                    if(lastErrorTimestamp - firstErrorTimestamp < 30) {
                        //3 connect errors in less than 30 seconds, cancel connection (most likely due to expired subscription)
                        Log.v(TAG, "3 failed connect attempts is less than 30s, cancel connection!");
                        //Disconnect and call verifyReceipt()!
                        mService.disconnect();
                        verifyReceipt();
                    }
                }
                this.tryingToConnect = false;
            }

        }
        else if(mService.getState() == VpnStateService.State.CONNECTING) {
            this.tryingToConnect = true;
            statusButton.setText(R.string.connecting___);
            statusButton.setBackgroundTintList(Objects.requireNonNull(getActivity()).getResources().getColorStateList(R.color.connecting_orange, null));
            statusButton.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics()));
            animateStatusButton(); //Kickstart the status button animation
        }
        else if(mService.getState() == VpnStateService.State.DISCONNECTING) {
            statusButton.setText(R.string.disconnecting___);
            statusButton.setBackgroundTintList(Objects.requireNonNull(getActivity()).getResources().getColorStateList(R.color.connecting_orange, null));
            statusButton.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics()));
        }
    }

    void statusButtonClicked(View v)
    {
        Log.v(TAG, "statusButtonClicked!");
        //Close any visible alert
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
        globalMethods.closeAlert();

        //Check if we should redirect to store
        MainActivity mainActivity = (MainActivity)getActivity();
        if (mainActivity != null && mainActivity.shouldRedirectToStore) {
            redirectToStore();
            return;
        }

        SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");

        //Try to open profile with UUID based on username
        mDataSource.open();
        UUID user_uuid;
        VpnProfile profile = null;
        if (username != null) {
            user_uuid = UUID.nameUUIDFromBytes(username.getBytes());
            profile = mDataSource.getVpnProfile(user_uuid);
        }

        if(mService.getState() == VpnStateService.State.CONNECTED || mService.getState() == VpnStateService.State.CONNECTING) {
            //Currently connected or connecting: will disconnect
            mService.disconnect();
            final Handler handler = new Handler();
            handler.postDelayed(this::stateChanged, 500);
        }
        else if(profile == null) {
            //No VPN profile found: will setup
            Purchase purchase = null;

            Purchase.PurchasesResult purchasesResult = mainActivity.billingClient.queryPurchases(BillingClient.SkuType.SUBS);
            if(purchasesResult.getPurchasesList() == null) {
                if(this.globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
                globalMethods.showAlertWithMessage(getString(R.string.billing_not_available), true);
                return;
            }
            for (int i = 0; i<purchasesResult.getPurchasesList().size(); i++) {
                if(purchasesResult.getPurchasesList().get(i).getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    purchase = purchasesResult.getPurchasesList().get(i); //Found correct purchase (receipt), send info for server validation
                }
            }

            if(purchase == null) {
                //User has not purchased yet, send to store
                mService.disconnect();
                redirectToStore();
                return;
            }

            //Found purchase, asking credentials from server
            requestCredentials(purchase);
        }
        else if(mService.getState() == VpnStateService.State.DISABLED) {
            //Currently disconnected, will connect
            UUID uuid = UUID.nameUUIDFromBytes(username.getBytes());
            Intent intent = new Intent(getActivity(), VpnProfileControlActivity.class);
            intent.setAction(VpnProfileControlActivity.START_PROFILE);
            intent.putExtra(VpnProfileControlActivity.EXTRA_VPN_PROFILE_ID, uuid.toString());
            startActivity(intent);
            final Handler handler = new Handler();
            handler.postDelayed(this::stateChanged, 500);
        }
        else {
            Log.v(TAG, "About to disconnect!");
            mService.disconnect();
        }

        mDataSource.close();
    }

    private void animateStatusButton()
    {
        VpnStateService.State currentState = mService.getState();
        String currentText = statusButton.getText().toString();

        try {
            if (currentText.equals(getString(R.string.connecting___)))
                currentText = getString(R.string.connecting);
            else if (currentText.equals(getString(R.string.connecting)))
                currentText = getString(R.string.connecting_);
            else if (currentText.equals(getString(R.string.connecting_)))
                currentText = getString(R.string.connecting__);
            else if (currentText.equals(getString(R.string.connecting__)))
                currentText = getString(R.string.connecting___);
        } catch (IllegalStateException exception) {
            Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
        }

        //Only if we're still connecting
        if(currentState == VpnStateService.State.CONNECTING || currentState == VpnStateService.State.DISCONNECTING) {
            statusButton.setText(currentText);

            final Handler handler = new Handler();
            handler.postDelayed(this::animateStatusButton, 500);
        }
        else {
            //Stop animation as we are no longer connecting and show the correct state
            stateChanged();
        }
    }

    private void enableButtons(boolean enabled)
    {
        /* 3 possible states:
            1) Connected + ListEnabled = Visible, Enabled, Alpha=1.0f
            2) Connected + ListDisabled = Visible, Enabled, Alpha=0.5f
            3) Disconnected + ListWhatever = Visible, Disabled, Alpha=0.3f
         */

        //Common
        blockTrackersButton.setEnabled(enabled);
        blockThreatsButton.setEnabled(enabled);
        blockTrackersImageButton.setClickable(enabled);
        blockTrackersCheckmarkButton.setClickable(enabled);
        blockThreatsImageButton.setClickable(enabled);
        blockThreatsCheckmarkButton.setClickable(enabled);

        if(enabled) {
            blockTrackersButton.setAlpha(0.5f);
            blockTrackersImageButton.setAlpha(0.5f);
            blockThreatsButton.setAlpha(0.5f);
            blockThreatsImageButton.setAlpha(0.5f);
            blockTrackersCheckmarkButton.setVisibility(View.GONE);
            blockThreatsCheckmarkButton.setVisibility(View.GONE);
            settingsAppButton.setVisibility(View.VISIBLE);
            settingsAppImageButton.setVisibility(View.VISIBLE);

            //Turn on depending on user config (if enabled, Alpha will be set to 1.0f)!
            SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            Set<String> enabled_lists = sharedPreferences.getStringSet(getString(R.string.preferences_lists_key), null);
            if (enabled_lists != null && enabled_lists.size() > 0) {
                if (enabled_lists.contains(getString(R.string.preferences_block_trackers_list))) {
                    blockTrackersButton.setAlpha(1.0f);
                    blockTrackersImageButton.setAlpha(1.0f);
                    blockTrackersCheckmarkButton.setAlpha(1.0f);
                    blockTrackersCheckmarkButton.setVisibility(View.VISIBLE);
                }
                if (enabled_lists.contains(getString(R.string.preferences_block_threats_list))) {
                    blockThreatsButton.setAlpha(1.0f);
                    blockThreatsImageButton.setAlpha(1.0f);
                    blockThreatsCheckmarkButton.setAlpha(1.0f);
                    blockThreatsCheckmarkButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            //NOT enabled, probably not even connected, set everyone to Alpha=0.3f and hide checkmarks;
            blockTrackersImageButton.setAlpha(0.3f);
            blockTrackersCheckmarkButton.setAlpha(0.3f);
            blockTrackersCheckmarkButton.setVisibility(View.GONE);
            blockThreatsImageButton.setAlpha(0.3f);
            blockThreatsCheckmarkButton.setAlpha(0.3f);
            blockThreatsCheckmarkButton.setVisibility(View.GONE);
            settingsAppButton.setVisibility(View.GONE);
            settingsAppImageButton.setVisibility(View.GONE);

        }
    }

    @Override
    public void onClick(View v)
    {
        String status;

        switch (v.getId()) {
            case R.id.connect_block_trackers_button:
            case R.id.connect_block_trackers_checkmark_button:
            case R.id.connect_block_trackers_image_button:
                status = (v.getAlpha() == 1.0f ? "desligado" : "ligado"); //Switch from enabled to disabled and vice-versa
                updatePreferences(status, "block-tracking");
                break;
            case R.id.connect_block_threats_button:
            case R.id.connect_block_threats_image_button:
            case R.id.connect_block_threats_checkmark_button:
                status = (v.getAlpha() == 1.0f ? "desligado" : "ligado"); //Switch from enabled to disabled and vice-versa
                updatePreferences(status, "malware-phishing");
                break;
            case R.id.connect_website_button:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website_url)));
                startActivity(browserIntent);
                break;
            case R.id.connect_toggle_always_on_button:
            case R.id.connect_toggle_always_on_image_button:
                Intent vpnSettingsIntent = new Intent(Settings.ACTION_VPN_SETTINGS);
                vpnSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(vpnSettingsIntent);
                break;
        }
    }

    private void updatePreferences(String status, String list)
    {
        //Hide buttons and show progress bar!
        mProgressBar.setVisibility(View.VISIBLE);
        blockTrackersButton.setVisibility(View.GONE);
        blockTrackersImageButton.setVisibility(View.GONE);
        blockTrackersCheckmarkButton.setVisibility(View.GONE);
        blockThreatsButton.setVisibility(View.GONE);
        blockThreatsImageButton.setVisibility(View.GONE);
        blockThreatsCheckmarkButton.setVisibility(View.GONE);
        settingsAppButton.setVisibility(View.GONE);
        settingsAppImageButton.setVisibility(View.GONE);

        SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("Lista", list);
            postData.put("Status", status);
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to update preferences: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/atualizarPreferencias", postData, response -> {
            //Hide progress bar and show buttons again!
            mProgressBar.setVisibility(View.GONE);
            blockTrackersButton.setVisibility(View.VISIBLE);
            blockTrackersImageButton.setVisibility(View.VISIBLE);
            blockTrackersCheckmarkButton.setVisibility(View.VISIBLE);
            blockThreatsButton.setVisibility(View.VISIBLE);
            blockThreatsImageButton.setVisibility(View.VISIBLE);
            blockThreatsCheckmarkButton.setVisibility(View.VISIBLE);
            settingsAppButton.setVisibility(View.VISIBLE);
            settingsAppImageButton.setVisibility(View.VISIBLE);

            //Handle response
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);
                if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success))) {

                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

                    JSONArray listsArray = jsonResponse.getJSONArray("Preferencias");
                    //Convert JSONArray to Set<String>
                    Set<String> enabled_lists = new HashSet<>();
                    for(int i=0; i<listsArray.length(); i++) {
                        enabled_lists.add(listsArray.getString(i));
                    }

                    sharedPreferencesEditor.putStringSet(getString(R.string.preferences_lists_key), enabled_lists);
                    sharedPreferencesEditor.apply();
                    enableButtons(true); //refresh UI to reflect changes!

                } else {
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }
            } catch (JSONException exception) {
                Log.v(TAG, "updatePreferences: JSONException: " + exception.getLocalizedMessage());
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            }
        });
    }

    private void requestCredentials(Purchase purchase)
    {
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST request parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("Recibo", purchase.getOriginalJson());
            postData.put("SO", "G"); //'G'oogle
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to request credentials : "+exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/criarUsuario3", postData, response -> {
            //Handle response here
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);

                if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success))) {

                    //Everything is good, store username and password!
                    String username = jsonResponse.getString("Usuario");
                    if(username == null || username.length() != 16) {
                        globalMethods.showAlertWithMessage(getString(R.string.error_request_1), true);
                        return;
                    }

                    String password = jsonResponse.getString("Senha");
                    if(password == null || password.length() != 16) {
                        globalMethods.showAlertWithMessage(getString(R.string.error_request_2), true);
                        return;
                    }

                    //Store credentials in SharedPreferences
                    SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putString(getString(R.string.preferences_username), username);
                    sharedPreferencesEditor.apply();

                    //Create VPN profile with uuid based on username and add let's encrypt CA if necessary
                    Log.v(TAG, "requestCredentials: Start by adding the let's encrypt cert if necessary!");
                    LocalCertificateStore certificateStore = new LocalCertificateStore();
                    if (certificateStore.containsAlias("local:da9b52a8771169d31318a567e1dc9b1f44b5b35c")) {
                        Log.v(TAG, "requestCredentials: Certificate is already trusted, didn't need to import!");
                    } else {
                        ByteArrayInputStream cert_bin = new ByteArrayInputStream((Base64.decode("MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/\n" +
                                "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n" +
                                "DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow\n" +
                                "SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT\n" +
                                "GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                                "AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF\n" +
                                "q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8\n" +
                                "SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0\n" +
                                "Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA\n" +
                                "a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj\n" +
                                "/PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T\n" +
                                "AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG\n" +
                                "CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv\n" +
                                "bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k\n" +
                                "c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw\n" +
                                "VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC\n" +
                                "ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz\n" +
                                "MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu\n" +
                                "Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF\n" +
                                "AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo\n" +
                                "uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/\n" +
                                "wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu\n" +
                                "X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG\n" +
                                "PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6\n" +
                                "KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==", 0)));
                        try {
                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            Certificate lets_encrypt_cert = cf.generateCertificate(cert_bin);
                            certificateStore.addCertificate(lets_encrypt_cert);
                        } catch (Exception e) {
                            Log.v(TAG, "requestCredentials: opped at an exception:" + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }

                    UUID uuid = UUID.nameUUIDFromBytes(username.getBytes());
                    mDataSource.open();

                    //If there's already a VpnProfile installed with this uuid, remove it first!
                    if(mDataSource.getVpnProfile(uuid) != null) {
                        Log.v(TAG, "requestCredentials: Found an existing profile with this UUID, removing it!");
                        mDataSource.deleteVpnProfile(mDataSource.getVpnProfile(uuid));
                    }

                    VpnProfile user_profile = new VpnProfile();
                    user_profile.setUsername(username);
                    user_profile.setPassword(password);
                    user_profile.setGateway(getString(R.string.default_vpn_gateway));
                    user_profile.setId(0);
                    user_profile.setName("Spod VPN");
                    user_profile.setVpnType(VpnType.IKEV2_EAP);
                    user_profile.setEspProposal("aes256gcm16-sha256-modp2048");
                    user_profile.setIkeProposal("aes256gcm16-sha256-modp2048");
                    user_profile.setMTU(MTU_MIN);
                    user_profile.setUUID(uuid);

                    mDataSource.insertProfile(user_profile);
                    mDataSource.close();

                    //Setup list configuration: Start by enabling both 'Block Trackers' and 'Block Threats' options (default)
                    Set<String> enabled_lists = new HashSet<>();
                    enabled_lists.add(getString(R.string.preferences_block_trackers_list));
                    enabled_lists.add(getString(R.string.preferences_block_threats_list));
                    sharedPreferencesEditor.putStringSet(getString(R.string.preferences_lists_key), enabled_lists);
                    sharedPreferencesEditor.apply();

                    Log.v(TAG, "requestCredentials: Finished setting up and saving VPN profile, connecting!");
                    statusButtonClicked(null); //start connection
                } else {
                    Log.v(TAG, "requestCredentials: Error from server: " + jsonResponse.getString(getString(R.string.request_message)));
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }
            } catch (JSONException exception) {
                Log.v(TAG, "requestCredentials: JSONException: " + exception.getLocalizedMessage());
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            }
        });
    }

    void verifyReceipt()
    {
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            MainActivity mainActivity = (MainActivity)getActivity();
            if (mainActivity != null && (!mainActivity.billingSetupFinished || Objects.requireNonNull(Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE).getString(getString(R.string.preferences_username), "")).isEmpty())) {
                Log.v(TAG, "verifyReceipt: Abort because billingClient is NOT ready yet or no username found!");
                return;
            }

            Purchase.PurchasesResult purchasesResult = null;
            if (mainActivity != null) {
                purchasesResult = mainActivity.billingClient.queryPurchases(BillingClient.SkuType.SUBS);
                for (int i = 0; i<purchasesResult.getPurchasesList().size(); i++) {
                    if (purchasesResult.getPurchasesList().get(i).getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        //Found correct purchase (receipt), send info for server validation
                        Purchase purchase = purchasesResult.getPurchasesList().get(i);
                        postData.put("Recibo", purchase.getOriginalJson());
                    }
                }
            }

            if(! postData.has("Recibo")) {
                Log.v(TAG, "verifyReceipt: Did NOT find valid purchase/receipt, redirecting to store...");
                mService.disconnect();
                redirectToStore();
            }
        }
        catch (JSONException exception) {
            Log.v(TAG, "verifyReceipt: JSONException: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/validarRecibo3", postData, response -> {
            //Handle response here
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);

                if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success))) {
                    Log.v(TAG, "verifyReceipt: Success validating receipt!");
                    //Connect to the VPN if not already connected
                    if(mService.getState() == VpnStateService.State.DISABLED)
                        statusButtonClicked(statusButton);

                } else {
                    //silent fail
                    if(jsonResponse.getString("Status").equals("Erro")) {
                        Log.v(TAG, "verifyReceipt: Detected Error from server...");
                        if (jsonResponse.getString("Mensagem").startsWith("Assinatura")) {
                            Log.v(TAG, "verifyReceipt: Error on subscription (invalid/expired), disconnect and send to store!");
                            mService.disconnect();
                            redirectToStore();
                        }
                    }
                }
            } catch (JSONException exception) {
                Log.v(TAG, "verifyReceipt: JSONException: " + exception.getLocalizedMessage());
                exception.printStackTrace();
            } catch (IllegalStateException exception) {
                Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
            }
        });
    }

    private void redirectToStore()
    {
        Log.v(TAG,"redirectToStore: Disconnect and send to store!");

        //Disconnect
        if (mService != null) mService.disconnect();

        //Send to store
        Fragment storeFragment = Objects.requireNonNull(getActivity()).getSupportFragmentManager().findFragmentByTag("StoreFragment");
        if (storeFragment != null && storeFragment.isVisible()) {
            Log.v(TAG, "redirectToStore: Tried to open StoreFragment while it was already opened, ignoring!");
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.runOnUiThread(() -> {
            //Should open store fragment!
            FrameLayout frameLayout = Objects.requireNonNull(getView()).findViewById(R.id.store_fragment_container);
            frameLayout.setVisibility(View.VISIBLE); //make container visible

            MainActivity mainActivity1 = (MainActivity) getActivity();
            mainActivity1.actionBarMenu.getItem(0).setVisible(false); //hide country flag in action bar

            //actually open the fragment
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.store_fragment_container, StoreFragment.newInstance(), "StoreFragment");
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    void changeServerLocation()
    {
        if(mService.getState() == VpnStateService.State.CONNECTING) {
            return; //Ignore because we're currently connecting!
        }

        //Open VPN profile
        SharedPreferences sharedPreferences = Objects.requireNonNull(getContext()).getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");
        UUID uuid = null;
        if (username != null) {
            uuid = UUID.nameUUIDFromBytes(username.getBytes());
        }
        mDataSource.open();
        VpnProfile user_profile = null;
        if (uuid != null) {
            user_profile = mDataSource.getVpnProfile(uuid);
        }

        String message = null;
        int icon;

        if (user_profile != null) {
            if(user_profile.getGateway().startsWith("us")) {
                //Currently connected to USA, switch to Brazil
                user_profile.setGateway("vpn.spod.com.br");
                message = getString(R.string.switch_country_brazil);
                icon = R.drawable.brazil_flag;
            }
            else {
                //Currently connected to Brazil, switch to USA
                user_profile.setGateway("us.vpn.spod.com.br");
                message = getString(R.string.switch_country_usa);
                icon = R.drawable.usa_flag;
            }
            mDataSource.updateVpnProfile(user_profile); //Update profile in database

            //Change button icon (country flag) to reflect new server location
            MainActivity mainActivity = (MainActivity)getActivity();
            if (mainActivity != null) {
                mainActivity.actionBarMenu.getItem(0).setIcon(icon);
            }
        }
        mDataSource.close();

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
        if(message == null) {
            //No VPN profile found, show initial welcome message
            globalMethods.showAlertWithMessage(getString(R.string.new_user_message), false);
            return;
        } else {
            //Show success message
            globalMethods.showAlertWithMessage(message, true);
        }

        mService.reconnect();
        final Handler handler = new Handler();
        handler.postDelayed(this::stateChanged, 500);
    }
}
