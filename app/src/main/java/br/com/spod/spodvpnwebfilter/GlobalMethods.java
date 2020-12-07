package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

class GlobalMethods
{
    private static final String TAG = "GlobalMethods";

    private FragmentActivity mActivity;

    GlobalMethods(FragmentActivity fragmentActivity) {
        this.mActivity = fragmentActivity;
    }

    void showAlertWithMessage(String message, boolean autoDismiss)
    {
        //Hide any pre existing message
        this.closeAlert();

        try {
            //Make container visible
            FrameLayout frameLayout = mActivity.findViewById(R.id.message_container);
            frameLayout.setVisibility(View.VISIBLE);

            //Add message fragment to container
            FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.message_container, MessageFragment.newInstance(message, (autoDismiss ? 5000L : 0L)), "MessageFragment");
            transaction.commitAllowingStateLoss();
        } catch (Exception e) {
            Log.v(TAG, "showAlertWithMessage: Exception!");
            //e.printStackTrace();
        }
    }

    void closeAlert()
    {
        //Find visible message alert and close it!
        try {
            Fragment messageFragment = mActivity.getSupportFragmentManager().findFragmentByTag("MessageFragment");
            if (messageFragment != null && messageFragment.isVisible()) {
                FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                transaction.remove(messageFragment);
                transaction.commit();
            }
        } catch (IllegalStateException exception) {
            Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
        }
    }

    //Generic HTTP POST request method using Volley
    void APIRequest(String url, JSONObject postData, OnAPIResponseListener responseListener)
    {
        RequestQueue queue = Volley.newRequestQueue(this.mActivity);

        StringRequest request = new StringRequest(Request.Method.POST, url, responseListener::onRequestComplete,
        error -> showAlertWithMessage(mActivity.getString(R.string.error_connecting_to_server), true)
        ){
            @Override
            protected Map<String, String> getParams()
            {
                //Get username and password from VPN profile
                SharedPreferences sharedPreferences = mActivity.getSharedPreferences(mActivity.getString(R.string.preferences_key), Context.MODE_PRIVATE);
                String username = sharedPreferences.getString(mActivity.getString(R.string.preferences_username), "");
                VpnProfile profile = null;
                UUID uuid;
                if (username.getBytes().length > 0) {
                    uuid = UUID.nameUUIDFromBytes(username.getBytes());
                    VpnProfileDataSource mDataSource = new VpnProfileDataSource(mActivity);
                    mDataSource.open();
                    profile = mDataSource.getVpnProfile(uuid);
                    mDataSource.close();
                }

                //Add credentials if we have them (not always mandatory)
                if(profile != null && profile.getUsername() != null && profile.getPassword() != null) {
                    try {
                        postData.put("Usuario", profile.getUsername());
                        postData.put("Senha", profile.getPassword());
                    } catch (JSONException exception) {
                        Log.v(TAG, "JSONException while trying create POST params: " + exception.getLocalizedMessage());
                        //exception.printStackTrace();
                    }
                }

                Map<String, String> params = new HashMap<>();
                params.put("q", postData.toString());
                return params;
            }
        };

        //Prevent duplicated requests
        request.setRetryPolicy(new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    //Declare listener for generic callback method
    interface OnAPIResponseListener {
        void onRequestComplete(String response);
    }

    String formatBytes(long bytes)
    {
        String medida = "bytes";
        float n = 0.0f;
        if(bytes > 1000000000) {
            n = (float)bytes/1000000000;
            medida = "GB";
        } else if(bytes > 1000000) {
            n = (float)bytes/1000000;
            medida = "MB";
        } else if(bytes > 1000) {
            n = (float)bytes/1000;
            medida = "kB";
        }

        return String.format(Locale.getDefault(),"%2.2f", n) + " " + medida;
    }
}
