package com.ecnetwork.iptnet.net;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.iptnet.voipframework.BaseFrame;
import com.iptnet.voipframework.RegisterInfo;
import com.iptnet.voipframework.VOIP;
import com.iptnet.voipframework.VOIPCallbacks;
import com.iptnet.voipframework.VOIPSessionInfo;

import java.io.File;

public class CallSessionActivity extends AppCompatActivity {

    private VOIP voip = null;
    private VOIPSessionInfo vhd = null;
    private boolean isHolding = false;
    private Button buttonHold = null;
    private LinearLayout layoutanswer = null;
    private LinearLayout layoutincoming = null;
    private TextView textViewLoginState = null;
    private TextView textViewLog = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_call);

        SurfaceView selfview = (SurfaceView) findViewById(R.id.surfaceView_localVIew);
        SurfaceView remoteview = (SurfaceView) findViewById(R.id.surfaceView_remoteView);
        textViewLoginState = (TextView)findViewById(R.id.textView_call_loginState);
        textViewLog = (TextView)findViewById(R.id.textView_dialLog);
        buttonHold = (Button)findViewById(R.id.button_hold);
        layoutanswer = (LinearLayout)findViewById(R.id.layout_answer);
        layoutincoming =  (LinearLayout)findViewById(R.id.layout_incooming);
        voip = VOIP.sharedInstance();
        voip.setLocalView(selfview);
        voip.setRemoteView(remoteview);

        Bundle bundle = this.getIntent().getBundleExtra("CallSessionInfo");
        if (bundle != null) {
            boolean isIncoming = bundle.getBoolean("incoming");
            String calleeId = bundle.getString("calleeId");

            if ( isIncoming ){

                layoutincoming.setVisibility(View.VISIBLE);
                layoutanswer.setVisibility(View.INVISIBLE);

                String payload = bundle.getString("payload");
                vhd = voip.createNewSession();
                voip.wakeupWithNotification(vhd,calleeId,payload);
            }else{
                layoutincoming.setVisibility(View.INVISIBLE);
                layoutanswer.setVisibility(View.VISIBLE);

                vhd = voip.createNewSession();
                voip.callWithNotification(vhd,calleeId,getString(R.string.sua_host),getString(R.string.sua_token));
            }
        }

        voip.addOnVOIPSessionListener(mOnVOIPSessionListener);
        voip.addOnLoginListener(mOnLoginListener);

        if ( voip.isLogin() ) {
            textViewLoginState.setBackgroundColor(Color.GREEN);
        }

    }

    @Override
    protected void onDestroy ()
    {   super.onDestroy();
        voip.removeOnVOIPSessionListener(mOnVOIPSessionListener);
        voip.removeOnLoginListener(mOnLoginListener);
    }
    public void onClickHold(View view){
        if ( isHolding == false ) {
            voip.hold(vhd);
            isHolding = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    buttonHold.setText("UnHold");
                }
            });
        }else{
            voip.unhold(vhd);
            isHolding = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    buttonHold.setText("Hold");
                }
            });
        }
    }

    public void onClickAnswer(View view){
        voip.answer(vhd);
        layoutincoming.setVisibility(View.INVISIBLE);
        layoutanswer.setVisibility(View.VISIBLE);

    }

    public void onClickHangup(View view){
        voip.hangUp(vhd);
    }

    private void DisplayLog(final String log){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLog.setText(log);
            }
        });

    }

    private final VOIPCallbacks.OnVOIPSessionListener mOnVOIPSessionListener = new VOIPCallbacks.OnVOIPSessionListener() {
        @Override
        public void onOutgoing(VOIPSessionInfo info) {
            DisplayLog("onOutgoing");
        }

        @Override
        public void onRinging(VOIPSessionInfo info) {
            DisplayLog("onRinging");
        }

        @Override
        public void onIncoming(VOIPSessionInfo info) {
        }

        @Override
        public void onAnswer(VOIPSessionInfo info) {

            DisplayLog("onAnswer");
            voip.startAudioPlayAndRec(CallSessionActivity.this);
            voip.startPlayVideo();
            voip.startRecordVideo();


            // get internal storage
            long time= System.currentTimeMillis()/1000;


            File mp4file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/video_record/"+time+".mp4");
            voip.startRecordLiveViewToMP4(info,mp4file.getPath());
            Log.e("MP4Path","record mp4 path:"+mp4file.getPath());
        }

        @Override
        public void onReceiveAudio(VOIPSessionInfo info, BaseFrame frame) {
            DisplayLog("onReceiveAudio");
        }

        @Override
        public void onReceiveVideo(VOIPSessionInfo info, BaseFrame frame) {
            DisplayLog("onReceiveVideo");
        }

        @Override
        public void onHold(VOIPSessionInfo info) {
            DisplayLog("onHold");
            voip.stopAudioPlayAndRec();
            voip.stopPlayVideo();
            voip.stopRecordVideo();
        }

        @Override
        public void onUnHold(VOIPSessionInfo info) {
            DisplayLog("onUnHold");
            voip.startAudioPlayAndRec(CallSessionActivity.this);
            voip.startPlayVideo();
            voip.startRecordVideo();
        }

        @Override
        public void onTransfer(VOIPSessionInfo info) {
            DisplayLog("onTransfer");
        }

        @Override
        public void onReceiveAlert(VOIPSessionInfo info) {
            DisplayLog("onReceiveAlert");
        }

        @Override
        public void onTerminated(VOIPSessionInfo info) {
            DisplayLog("onTerminated");

            switch (info.message) {
                case VOIP.VOIPMainEvents.VOIP_404_EVENT:
                    DisplayLog("VOIP_404_EVENT");
                    break;
                case VOIP.VOIPMainEvents.VOIP_4XX_EVENT:
                    DisplayLog("VOIP_4XX_EVENT");
                    break;
                case VOIP.VOIPMainEvents.VOIP_NOANSWER_EVENT:
                    DisplayLog("VOIP_NOANSWER_EVENT");
                    break;
                case VOIP.VOIPMainEvents.VOIP_BUSY_EVENT:
                    DisplayLog("VOIP_BUSY_EVENT");
                    break;
                case VOIP.VOIPMainEvents.VOIP_PACKET_LOSS_EVENT:
                    DisplayLog("VOIP_PACKET_LOSS_EVENT");
                    break;
                case VOIP.VOIPMainEvents.VOIP_CANCELED_EVENT:
                    DisplayLog("VOIP_CANCELED_EVENT");
                    break;
                case VOIP.VOIPMainEvents.VOIP_CALL_TERMINATED_EVENT: {
                    DisplayLog("VOIP_CALL_TERMINATED_EVENT");
                    break;
                }
                case VOIP.VOIPMainEvents.VOIP_OUTGOING_ERROR_EVENT: {

                    switch (info.errorNo) {
                        case VOIP.VOIPSubEvents.VOIP_REMOTE_NO_RESP:
                            DisplayLog("VOIP_REMOTE_NO_RESP");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_TIMEOUT:
                            DisplayLog("VOIP_TIMEOUT");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_SRV_DISCONNECT:
                            DisplayLog("VOIP_SRV_DISCONNECT");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_RELAY_NO_RESP:
                            DisplayLog("VOIP_RELAY_NO_RESP");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_RELAY_FAIL:
                            DisplayLog("VOIP_RELAY_FAIL");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_PENDING_TIMEOUT:
                            DisplayLog("VOIP_PENDING_TIMEOUT");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_SRV_NO_RESP:
                            DisplayLog("VOIP_SRV_NO_RESP");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_UNAUTHORIZED:
                            DisplayLog("VOIP_UNAUTHORIZED");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_SOCKET_ERROR:
                            DisplayLog("VOIP_SOCKET_ERROR");
                            break;
                        case VOIP.VOIPSubEvents.VOIP_LOCAL_BUSY:
                            DisplayLog("VOIP_LOCAL_BUSY");
                            break;
                        default:
                            break;
                    }
                    break;
                }
                default:
                    break;

            }

            voip.stopAudioPlayAndRec();
            voip.stopPlayVideo();
            voip.stopRecordVideo();
            voip.stopRecordLiveViewToMP4(info);
            finish();
        }
    };

    private final VOIPCallbacks.OnLoginListener mOnLoginListener = new VOIPCallbacks.OnLoginListener() {
        @Override
        public void onLogining(RegisterInfo info) {
            textViewLoginState.setBackgroundColor(Color.YELLOW);
        }

        @Override
        public void onLoginSuccess(RegisterInfo info) {
            textViewLoginState.setBackgroundColor(Color.GREEN);
        }

        @Override
        public void onLoginFailure(RegisterInfo info) {
            textViewLoginState.setBackgroundColor(Color.RED);
        }

        @Override
        public void onLogoutByServer(RegisterInfo info) {
            textViewLoginState.setBackgroundColor(Color.BLACK);
        }

        @Override
        public void onLogoutDone(RegisterInfo info) {

            textViewLoginState.setBackgroundColor(Color.BLACK);
        }

        @Override
        public void onLogoutFailed(RegisterInfo info) {

        }
    };
}