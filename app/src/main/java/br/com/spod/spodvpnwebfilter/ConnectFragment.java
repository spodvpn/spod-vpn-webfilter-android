package br.com.spod.spodvpnwebfilter;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.security.LocalCertificateStore;
import org.strongswan.android.ui.VpnProfileControlActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import androidx.core.content.res.ResourcesCompat;
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

    private TextView sendNotificationsButton;
    private ImageButton sendNotificationsImageButton;
    private ImageButton sendNotificationsCheckmarkButton;

    private TextView settingsAppButton;
    private ImageButton settingsAppImageButton;

    private boolean shouldShouldSettingsAppButton;

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
        requireActivity().getApplicationContext().bindService(new Intent(requireActivity().getApplicationContext(), VpnStateService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
        mDataSource = new VpnProfileDataSource(getActivity());

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");

        //Try to open profile with UUID based on username
        UUID user_uuid;
        user_uuid = UUID.nameUUIDFromBytes(username.getBytes());
        mDataSource.open();
        VpnProfile profile = mDataSource.getVpnProfile(user_uuid);
        if(username.length() == 0 || profile.getGateway() == null || profile.getGateway().length() == 0) {
            //VPN has not been configured yet, show initial message
            if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
            globalMethods.showAlertWithMessage(getString(R.string.new_user_message), false);
        }
        mDataSource.close();

        connectErrorArray = new ArrayList<>(); //Initialize array
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        statusButton = view.findViewById(R.id.statusButton);
        statusButton.setOnClickListener(this::statusButtonClicked);
        statusButton.setBackgroundTintList(getResources().getColorStateList(R.color.disconnected_red, null)); //Always start disconnected

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

        sendNotificationsButton = view.findViewById(R.id.connect_send_notifications_button);
        sendNotificationsButton.setOnClickListener(this);
        sendNotificationsImageButton = view.findViewById(R.id.connect_send_notifications_image_button);
        sendNotificationsImageButton.setOnClickListener(this);
        sendNotificationsCheckmarkButton = view.findViewById(R.id.connect_send_notifications_checkmark_button);
        sendNotificationsCheckmarkButton.setOnClickListener(this);
        settingsAppButton = view.findViewById(R.id.connect_toggle_always_on_button);
        settingsAppButton.setOnClickListener(this);
        settingsAppImageButton = view.findViewById(R.id.connect_toggle_always_on_image_button);
        settingsAppImageButton.setOnClickListener(this);

        //Start disconnected
        statusImageButton.setImageResource(R.drawable.status_image_animation_off_on);
        statusImageButton.setAlpha(0.5f);

        enableButtons(false);
        requireActivity().setTitle(getString(R.string.app_name));

        //Make buttons wider for pt-BR
        if(! getString(R.string.region).equals("US")) {
            blockTrackersButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
            blockThreatsButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
            sendNotificationsButton.getLayoutParams().width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, getResources().getDisplayMetrics());
        }

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        boolean sendNotifications = sharedPreferences.getBoolean(getString(R.string.preferences_send_notification), false);
        if(sendNotifications) updateNotificationToken(true);

        //Get display size to determine if settingsAppButton should be displayed
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Display d = requireActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        d.getSize(size);
        int height = size.y;
        this.shouldShouldSettingsAppButton = height / metrics.densityDpi >= 4;

        return view;
    }

    //Fragment lifecycle methods
    @Override
    public void onStop() {
        super.onStop();
        if (mService != null) mService.unregisterListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        //Update VPN profile to disable cert_reqs (v1.3.1)
        try {
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(requireActivity().getString(R.string.preferences_key), Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(requireActivity().getString(R.string.preferences_username), "");
            if (username.length() > 0) {
                UUID uuid = UUID.nameUUIDFromBytes(username.getBytes());
                VpnProfileDataSource mDataSource = new VpnProfileDataSource(getActivity());
                mDataSource.open();
                VpnProfile profile = mDataSource.getVpnProfile(uuid);
                if (profile.getFlags() != VpnProfile.FLAGS_SUPPRESS_CERT_REQS) {
                    profile.setFlags(VpnProfile.FLAGS_SUPPRESS_CERT_REQS);
                    mDataSource.updateVpnProfile(profile);
                }

                //Remove hardcoded certificate from local store (no longer needed - v1.3.2)
                LocalCertificateStore certificateStore = new LocalCertificateStore();
                if (certificateStore.containsAlias("local:da9b52a8771169d31318a567e1dc9b1f44b5b35c")) {
                    certificateStore.deleteCertificate("local:da9b52a8771169d31318a567e1dc9b1f44b5b35c");
                }

                mDataSource.close();
            }
        } catch (Exception e) {
            Log.v(TAG, "onStart: Exception while updating VPN profile...");
            //e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mService != null) stateChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().getApplicationContext().unbindService(mServiceConnection);
    }

    //@SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void stateChanged()
    {
        Log.v(TAG, "stateChanged: "+mService.getState().toString());

        try {
            //Identify current state
            if (mService.getState() == VpnStateService.State.CONNECTED && (this.tryingToConnect || statusImageButton.getAlpha() == 0.5f || !statusButton.getText().equals(getString(R.string.connected)))) {
                //Set connectedServer string
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.server_connected = mService.getProfile().getGateway();

                    //Set button color
                    statusButton.setBackgroundTintList(mainActivity.getResources().getColorStateList(R.color.connected_green, null));

                    //Change images (only if we were disconnected before)
                    //if (statusImageButton.getDrawable() != mainActivity.getResources().getDrawable(R.drawable.status_image_animation_off_on, null))
                    if (statusImageButton.getDrawable() != ResourcesCompat.getDrawable(mainActivity.getResources(), R.drawable.status_image_animation_off_on, null))
                        statusImageButton.setImageResource(R.drawable.status_image_animation_off_on);
                }

                //Set status text button
                statusButton.setText(R.string.connected);
                try {
                    statusButton.setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()));
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
                }

                this.tryingToConnect = false;

                //Animation: Off -> On
                Log.v(TAG, "Animation: Off -> On");

                //Step 1: Fade in
                statusImageButton.animate().alpha(1.0f).setDuration(700).setListener(null);

                //Step 2: Animation
                AnimationDrawable turnOnAnimation = (AnimationDrawable) statusImageButton.getDrawable();
                turnOnAnimation.start();

                //Step 3: Enable buttons
                try {
                    enableButtons(true);
                } catch (NullPointerException exception) {
                    Log.v(TAG, "stateChanged: Received NullPointerException before enableButtons");
                }

            } else if ((mService.getState() == VpnStateService.State.DISABLED || mService.getState() == VpnStateService.State.DISCONNECTING) && (statusImageButton.getAlpha() == 1.0f || !statusButton.getText().equals(getString(R.string.disconnected)))) {
                statusButton.setText(R.string.disconnected);
                statusButton.setBackgroundTintList(requireActivity().getResources().getColorStateList(R.color.disconnected_red, null));
                try {
                    statusButton.setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics()));
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "stateChanged: Got an IllegalStateException, probably running in the background...");
                }

                //Set connectedServer string
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.server_connected = "";
                }

                //Animation: On -> Off
                Log.v(TAG, "Animation: On -> Off");

                //Step 1: Fade out
                statusImageButton.animate().alpha(0.5f).setDuration(700).setListener(null);

                //Step 2: Change images
                statusImageButton.setImageResource(R.drawable.status_image_animation_on_off);
                AnimationDrawable turnOffAnimation = (AnimationDrawable) statusImageButton.getDrawable();
                turnOffAnimation.start();

                //Step 3: Disable buttons
                enableButtons(false);

                if (this.tryingToConnect) {
                    //Was trying to connect but wasn't successful
                    if (this.connectErrorArray.size() < 3) {
                        //Lower than 3, save timestamp
                        this.connectErrorArray.add(System.currentTimeMillis());
                    } else {
                        //Greater than 3, verify if it's stuck!
                        Long firstErrorTimestamp = this.connectErrorArray.get(0);
                        Long lastErrorTimestamp = this.connectErrorArray.get(this.connectErrorArray.size() - 1);
                        if (lastErrorTimestamp - firstErrorTimestamp < 30) {
                            //3 connect errors in less than 30 seconds, cancel connection (most likely due to expired subscription)
                            Log.v(TAG, "3 failed connect attempts is less than 30s, cancel connection!");
                            //Disconnect and call verifyReceipt()!
                            mService.disconnect();
                            verifyReceipt();
                        }
                    }
                    this.tryingToConnect = false;
                }

            } else if (mService.getState() == VpnStateService.State.CONNECTING) {
                this.tryingToConnect = true;
                statusButton.setText(R.string.connecting___);
                try {
                    statusButton.setBackgroundTintList(requireActivity().getResources().getColorStateList(R.color.connecting_orange, null));
                    statusButton.setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics()));
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
                }
                animateStatusButton(); //Kickstart the status button animation
            } else if (mService.getState() == VpnStateService.State.DISCONNECTING) {
                statusButton.setText(R.string.disconnecting___);
                try {
                    statusButton.setBackgroundTintList(requireActivity().getResources().getColorStateList(R.color.connecting_orange, null));
                    statusButton.setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics()));
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
                }
            }
        } catch (IllegalStateException | NullPointerException exception) {
            Log.v(TAG, "stateChanged: Got an exception, probably running in the background...");
        }
    }

    private void statusButtonClicked(View v)
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

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");

        //Try to open profile with UUID based on username
        mDataSource.open();
        UUID user_uuid;
        VpnProfile profile = null;
        if (username.getBytes().length > 0) {
            user_uuid = UUID.nameUUIDFromBytes(username.getBytes());
            profile = mDataSource.getVpnProfile(user_uuid);
        }

        if(mService.getState() == VpnStateService.State.CONNECTED || mService.getState() == VpnStateService.State.CONNECTING) {
            //Currently connected or connecting: will disconnect
            mService.disconnect();
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(this::stateChanged, 500);
        }
        else if(profile == null && mainActivity != null) {
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

            if(purchase == null && ! mainActivity.subscribeToCustomFreeTrial) {
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

            final Handler handler = new Handler(Looper.getMainLooper());
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

            final Handler handler = new Handler(Looper.getMainLooper());
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

        try {
            //Common
            blockTrackersButton.setEnabled(enabled);
            blockThreatsButton.setEnabled(enabled);
            blockTrackersImageButton.setClickable(enabled);
            blockTrackersCheckmarkButton.setClickable(enabled);
            blockThreatsImageButton.setClickable(enabled);
            blockThreatsCheckmarkButton.setClickable(enabled);
            sendNotificationsButton.setEnabled(enabled);
            sendNotificationsImageButton.setClickable(enabled);
            sendNotificationsCheckmarkButton.setClickable(enabled);

            if (enabled) {
                blockTrackersButton.setAlpha(0.5f);
                blockTrackersImageButton.setAlpha(0.5f);
                blockThreatsButton.setAlpha(0.5f);
                blockThreatsImageButton.setAlpha(0.5f);
                blockTrackersCheckmarkButton.setVisibility(View.GONE);
                blockThreatsCheckmarkButton.setVisibility(View.GONE);
                if (this.shouldShouldSettingsAppButton)
                    settingsAppButton.setVisibility(View.VISIBLE);
                if (this.shouldShouldSettingsAppButton)
                    settingsAppImageButton.setVisibility(View.VISIBLE);
                sendNotificationsButton.setAlpha(0.5f);
                sendNotificationsImageButton.setAlpha(0.5f);
                sendNotificationsCheckmarkButton.setVisibility(View.GONE);

                //Turn on depending on user config (if enabled, Alpha will be set to 1.0f)!
                SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
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
                    if (sharedPreferences.getBoolean(getString(R.string.preferences_send_notification), false)) {
                        sendNotificationsButton.setAlpha(1.0f);
                        sendNotificationsImageButton.setAlpha(1.0f);
                        sendNotificationsCheckmarkButton.setAlpha(1.0f);
                        sendNotificationsCheckmarkButton.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                //NOT enabled, probably not even connected, set everyone to Alpha=0.3f and hide checkmarks;
                blockTrackersButton.setAlpha(0.3f);
                blockTrackersImageButton.setAlpha(0.3f);
                blockTrackersCheckmarkButton.setAlpha(0.3f);
                blockTrackersCheckmarkButton.setVisibility(View.GONE);
                blockThreatsButton.setAlpha(0.3f);
                blockThreatsImageButton.setAlpha(0.3f);
                blockThreatsCheckmarkButton.setAlpha(0.3f);
                blockThreatsCheckmarkButton.setVisibility(View.GONE);
                if (this.shouldShouldSettingsAppButton) settingsAppButton.setVisibility(View.GONE);
                if (this.shouldShouldSettingsAppButton)
                    settingsAppImageButton.setVisibility(View.GONE);
                sendNotificationsButton.setAlpha(0.3f);
                sendNotificationsImageButton.setAlpha(0.3f);
                sendNotificationsCheckmarkButton.setAlpha(0.3f);
                sendNotificationsCheckmarkButton.setVisibility(View.GONE);
            }
        } catch (NullPointerException | IllegalStateException ex) {
            Log.v(TAG, "enableButtons: Got an exception, probably running in the background...");
        }
    }

    @Override
    public void onClick(View v)
    {
        String status;

        if(v.getId() == R.id.connect_block_trackers_button || v.getId() == R.id.connect_block_trackers_checkmark_button || v.getId() == R.id.connect_block_trackers_image_button)
        {
            status = (v.getAlpha() == 1.0f ? "desligado" : "ligado"); //Switch from enabled to disabled and vice-versa
            updatePreferences(status, "block-tracking");
        }
        else if(v.getId() == R.id.connect_block_threats_button || v.getId() == R.id.connect_block_threats_checkmark_button || v.getId() == R.id.connect_block_threats_image_button)
        {
            status = (v.getAlpha() == 1.0f ? "desligado" : "ligado"); //Switch from enabled to disabled and vice-versa
            updatePreferences(status, "malware-phishing");
        }
        else if(v.getId() == R.id.connect_website_button)
        {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website_url)));
            startActivity(browserIntent);
        }
        else if(v.getId() == R.id.connect_toggle_always_on_button || v.getId() == R.id.connect_toggle_always_on_image_button)
        {
            Intent vpnSettingsIntent = new Intent(Settings.ACTION_VPN_SETTINGS);
            vpnSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(vpnSettingsIntent);
        }
        else if(v.getId() == R.id.connect_send_notifications_button || v.getId() == R.id.connect_send_notifications_checkmark_button || v.getId() == R.id.connect_send_notifications_image_button)
        {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            boolean sendNotifications = sharedPreferences.getBoolean(getString(R.string.preferences_send_notification), false);
            if(sendNotifications)
                updateNotificationSettings(false, null); //sendNotifications is currently enabled and user clicked the button: disable!
            else
                updateNotificationToken(true); //sendNotifications is currently disabled and user clicked the button: enable!
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
        if(this.shouldShouldSettingsAppButton) settingsAppButton.setVisibility(View.GONE);
        if(this.shouldShouldSettingsAppButton) settingsAppImageButton.setVisibility(View.GONE);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);

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
            try {
                mProgressBar.setVisibility(View.GONE);
                blockTrackersButton.setVisibility(View.VISIBLE);
                blockTrackersImageButton.setVisibility(View.VISIBLE);
                blockTrackersCheckmarkButton.setVisibility(View.VISIBLE);
                blockThreatsButton.setVisibility(View.VISIBLE);
                blockThreatsImageButton.setVisibility(View.VISIBLE);
                blockThreatsCheckmarkButton.setVisibility(View.VISIBLE);
                if (this.shouldShouldSettingsAppButton)
                    settingsAppButton.setVisibility(View.VISIBLE);
                if (this.shouldShouldSettingsAppButton)
                    settingsAppImageButton.setVisibility(View.VISIBLE);
            } catch (IllegalStateException | NullPointerException exception) {
                Log.v(TAG, "updatePreferences: Got exception while modifying UI");
            }

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
                Log.v(TAG, "updatePreferences: Got exception: " + exception.getLocalizedMessage());
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            } catch (IllegalStateException exception) {
                Log.v(TAG, "updatePreferences: Got IllegalStateException: " + exception.getLocalizedMessage());
            }
        });
    }

    private void requestCredentials(Purchase purchase)
    {
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST request parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            if(purchase != null) postData.put("Recibo", purchase.getOriginalJson());
            else {
                postData.put("Recibo", "FreeTrial3dias");
                @SuppressLint("HardwareIds") final String deviceID = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                postData.put("SSAID", deviceID);
                MainActivity mainActivity = (MainActivity)getActivity();
                if(mainActivity != null) postData.put("TokenID", mainActivity.firebaseTokenId);
            }
            postData.put("SO", "G"); //'G'oogle
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to request credentials : "+exception.getLocalizedMessage());
            exception.printStackTrace();
        } catch (Exception exception) {
            Log.v(TAG, "Exception while trying to request credentials: "+exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/criarUsuario4", postData, response -> {
            //Handle response here
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);

                if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success))) {

                    //Everything is good, store username and password!
                    String username = jsonResponse.getString("Usuario");
                    if(username.length() != 16) {
                        globalMethods.showAlertWithMessage(getString(R.string.error_request_1), true);
                        return;
                    }

                    String password = jsonResponse.getString("Senha");
                    if(password.length() != 16) {
                        globalMethods.showAlertWithMessage(getString(R.string.error_request_2), true);
                        return;
                    }

                    //Store credentials in SharedPreferences
                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putString(getString(R.string.preferences_username), username);
                    sharedPreferencesEditor.apply();

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

                    //user_profile.setGateway(getString(R.string.default_vpn_gateway));
                    //Set default gateway based on user's locale
                    Locale locale = getResources().getConfiguration().getLocales().get(0);
                    String serverHostname = globalMethods.getRegionForCountry(locale.getCountry());
                    user_profile.setGateway(serverHostname);

                    user_profile.setId(0);
                    user_profile.setName("Spod VPN");
                    user_profile.setVpnType(VpnType.IKEV2_EAP);
                    user_profile.setEspProposal("aes256gcm16-sha256-modp2048");
                    user_profile.setIkeProposal("aes256gcm16-sha256-modp2048");
                    user_profile.setMTU(MTU_MIN);
                    user_profile.setUUID(uuid);
                    user_profile.setFlags(VpnProfile.FLAGS_SUPPRESS_CERT_REQS);

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
                Log.v(TAG, "requestCredentials: Got exception: " + exception.getLocalizedMessage());
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            } catch (IllegalStateException exception) {
                Log.v(TAG, "requestCredentials: Got IllegalStateException");
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
            if (mainActivity != null && mainActivity.subscribeToCustomFreeTrial) {
                requestCredentials(null);
                return;
            }
            if (mainActivity != null && (!mainActivity.billingSetupFinished || Objects.requireNonNull(requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE).getString(getString(R.string.preferences_username), "")).isEmpty())) {
                Log.v(TAG, "verifyReceipt: Abort because billingClient is NOT ready yet or no username found!");
                return;
            }

            Purchase.PurchasesResult purchasesResult;
            if (mainActivity != null) {
                purchasesResult = mainActivity.billingClient.queryPurchases(BillingClient.SkuType.SUBS);
                if(purchasesResult.getPurchasesList() != null) {
                    for (int i = 0; i < purchasesResult.getPurchasesList().size(); i++) {
                        if (purchasesResult.getPurchasesList().get(i).getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            //Found correct purchase (receipt), send info for server validation
                            Purchase purchase = purchasesResult.getPurchasesList().get(i);
                            postData.put("Recibo", purchase.getOriginalJson());
                        }
                    }
                }
            }
            if(! postData.has("Recibo")) postData.put("Recibo", "FreeTrial3dias");
        }
        catch (JSONException exception) {
            Log.v(TAG, "verifyReceipt: JSONException: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/validarRecibo4", postData, response -> {
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
        Fragment storeFragment = requireActivity().getSupportFragmentManager().findFragmentByTag("StoreFragment");
        if (storeFragment != null && storeFragment.isVisible()) {
            Log.v(TAG, "redirectToStore: Tried to open StoreFragment while it was already opened, ignoring!");
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                //Should open store fragment!
                FrameLayout frameLayout = requireView().findViewById(R.id.store_fragment_container);
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
    }

    void changeRegion(String serverHostname)
    {
        //Open VPN profile
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String username = sharedPreferences.getString(getString(R.string.preferences_username), "");
        UUID uuid = null;
        if (username.getBytes().length > 0) {
            uuid = UUID.nameUUIDFromBytes(username.getBytes());
        }
        mDataSource.open();
        VpnProfile user_profile = null;
        if (uuid != null) {
            user_profile = mDataSource.getVpnProfile(uuid);
        }



        String message = null;
        if (user_profile != null)
        {
            if(mService.getState() == VpnStateService.State.CONNECTING)
            {
                String[] serversHostnames = {"vpn.spod.com.br", "us.vpn.spod.com.br", "eu.vpn.spod.com.br", "in.vpn.spod.com.br"};
                MainActivity mainActivity1 = (MainActivity) getActivity();
                MenuItem item = Objects.requireNonNull(mainActivity1).actionBarMenu.findItem(R.id.region_spinner);
                CustomSpinner regionSpinner = (CustomSpinner) item.getActionView();
                regionSpinner.setSelection(Arrays.asList(serversHostnames).indexOf(user_profile.getGateway()), true); //reset selection to current configured region
                mDataSource.close();
                return; //Ignore because we're currently connecting
            }

            if(user_profile.getGateway().equals(serverHostname)) {
                mDataSource.close();
                return; //Ignore, we're already connected to this region!
            }

            user_profile.setGateway(serverHostname);
            message = getString(R.string.change_region);

            mDataSource.updateVpnProfile(user_profile); //Update profile in database
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
        //final Handler handler = new Handler();
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(this::stateChanged, 500);
    }

    void updateNotificationToken(boolean shouldEnable)
    {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if(!task.isSuccessful()) {
                        Log.v(TAG, "updateNotificationToken: Task failed!");
                        return;
                    }

                    // Get new Instance ID token
                    try {
                        String token = Objects.requireNonNull(task.getResult());
                        MainActivity mainActivity = (MainActivity)getActivity();
                        if(mainActivity != null) mainActivity.firebaseTokenId = token;

                        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                        boolean sendNotifications = sharedPreferences.getBoolean(getString(R.string.preferences_send_notification), false);
                        if(shouldEnable) {
                            if (!sendNotifications) updateNotificationSettings(true, token);
                            else updateNotificationTokenOnServer(token);
                        }

                    } catch (Exception e) {
                        Log.v(TAG, "updateNotificationToken: Got Exception: "+e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                });
    }

    private void updateNotificationTokenOnServer(String token)
    {
        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());
        try {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(getString(R.string.preferences_username), "");
            boolean sendNotifications = sharedPreferences.getBoolean(getString(R.string.preferences_send_notification), false);
            if(! sendNotifications) return;

            UUID uuid = null;
            if (username.getBytes().length > 0) {
                uuid = UUID.nameUUIDFromBytes(username.getBytes());
            }
            VpnProfileDataSource mDataSource = new VpnProfileDataSource(getContext());
            mDataSource.open();
            VpnProfile profile = null;
            if (uuid != null) {
                profile = mDataSource.getVpnProfile(uuid);
            }
            mDataSource.close();

            if (profile == null || profile.getUsername().isEmpty() || profile.getPassword().isEmpty()) {
                return; //Fail silently
            }

            //Create POST parameters JSONObject
            JSONObject postData = new JSONObject();
            try {
                postData.put("TokenID", token);
                postData.put("SO", "G"); //'G'oogle
            } catch (JSONException exception) {
                Log.v(TAG, "JSONException while trying create POST params: " + exception.getLocalizedMessage());
                exception.printStackTrace();
            }

            //Actually make the request
            globalMethods.APIRequest("https://spod.com.br/services/vpn/atualizarTokenId", postData, response -> {
                //Handle response here
                JSONObject jsonResponse;
                try {
                    jsonResponse = new JSONObject(response);
                    if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success))) {
                        Log.v(TAG, "Updated tokenID on server with success!");
                    } else {
                        //Fail silently
                        Log.v(TAG, "Error updating tokenID on server!");
                    }
                } catch (JSONException exception) {
                    Log.v(TAG, "updateNotificationOnServer: JSONException: " + exception.getLocalizedMessage());
                } catch (IllegalStateException exception) {
                    Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNotificationSettings(boolean status, String tokenID)
    {
        //Hide buttons and show progress bar!
        mProgressBar.setVisibility(View.VISIBLE);
        blockTrackersButton.setVisibility(View.GONE);
        blockTrackersImageButton.setVisibility(View.GONE);
        blockTrackersCheckmarkButton.setVisibility(View.GONE);
        blockThreatsButton.setVisibility(View.GONE);
        blockThreatsImageButton.setVisibility(View.GONE);
        blockThreatsCheckmarkButton.setVisibility(View.GONE);
        if(this.shouldShouldSettingsAppButton) settingsAppButton.setVisibility(View.GONE);
        if(this.shouldShouldSettingsAppButton) settingsAppImageButton.setVisibility(View.GONE);
        sendNotificationsButton.setVisibility(View.GONE);
        sendNotificationsImageButton.setVisibility(View.GONE);
        sendNotificationsCheckmarkButton.setVisibility(View.GONE);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);

        if(globalMethods == null) this.globalMethods = new GlobalMethods(getActivity());

        //Create POST parameters JSONObject
        JSONObject postData = new JSONObject();
        try {
            postData.put("SendNotifications", status);
            postData.put("Idioma", getString(R.string.language));

            if(! status) postData.put("TokenID", "Disable");
            else {
                if (tokenID != null && tokenID.length() > 0) postData.put("TokenID", tokenID);
                else postData.put("TokenID", "Disable");
            }
        } catch (JSONException exception) {
            Log.v(TAG, "JSONException while trying to update preferences: " + exception.getLocalizedMessage());
            exception.printStackTrace();
        }

        //Actually make the request
        globalMethods.APIRequest("https://spod.com.br/services/vpn/atualizarNotificacoes", postData, response -> {
            //Hide progress bar and show buttons again!
            mProgressBar.setVisibility(View.GONE);
            blockTrackersButton.setVisibility(View.VISIBLE);
            blockTrackersImageButton.setVisibility(View.VISIBLE);
            blockTrackersCheckmarkButton.setVisibility(View.VISIBLE);
            blockThreatsButton.setVisibility(View.VISIBLE);
            blockThreatsImageButton.setVisibility(View.VISIBLE);
            blockThreatsCheckmarkButton.setVisibility(View.VISIBLE);
            if(this.shouldShouldSettingsAppButton) settingsAppButton.setVisibility(View.VISIBLE);
            if(this.shouldShouldSettingsAppButton) settingsAppImageButton.setVisibility(View.VISIBLE);
            sendNotificationsButton.setVisibility(View.VISIBLE);
            sendNotificationsImageButton.setVisibility(View.VISIBLE);
            sendNotificationsCheckmarkButton.setVisibility(View.VISIBLE);

            //Handle response
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(response);
                if (jsonResponse.getString("Status").equals(getString(R.string.request_status_success))) {
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putBoolean(getString(R.string.preferences_send_notification), jsonResponse.getInt("SendNotifications") == 1);
                    sharedPreferencesEditor.apply();
                    enableButtons(true); //refresh UI to reflect changes!
                } else {
                    globalMethods.showAlertWithMessage(getString(R.string.error_from_server, jsonResponse.getString(getString(R.string.request_message))), true);
                }
            } catch (JSONException exception) {
                Log.v(TAG, "updateNotificationSettings: JSONException: " + exception.getLocalizedMessage());
                globalMethods.showAlertWithMessage(getString(R.string.error_connecting_to_server), true);
            } catch (IllegalStateException exception) {
                Log.v(TAG, "updateNotificationSettings: Got IllegalStateException");
            }
        });
    }
}
