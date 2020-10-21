package com.ecnetwork.iptnet.net.fcm;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.ecnetwork.iptnet.net.CallSessionActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class FcmMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FcmMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            JSONObject json = new JSONObject(data);

            try {
                Log.d(TAG, "Message Notification Body: " +  json.getString("msg_i") );

                Bundle bundle = new Bundle();
                bundle.putBoolean("incoming", true);
                bundle.putString("payload",json.getString("msg_i"));
                bundle.putString("calleeId",new JSONObject(json.getString("msg_i")).getString("peerId"));
                Intent intent = new Intent(FcmMessagingService.this, CallSessionActivity.class);
                intent.putExtra("CallSessionInfo",bundle);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }

}
