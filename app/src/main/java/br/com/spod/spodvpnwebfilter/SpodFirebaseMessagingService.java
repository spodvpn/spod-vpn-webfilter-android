package br.com.spod.spodvpnwebfilter;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;

public class SpodFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = "SpodFirebase";

    /*@Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.v(TAG, "onMessageReceived");
    }
    */

    @Override
    public void onNewToken(@NonNull String token) {
        Log.v(TAG, "onNewToken");
    }

}
