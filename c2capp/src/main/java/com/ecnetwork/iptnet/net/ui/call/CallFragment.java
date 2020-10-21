package com.ecnetwork.iptnet.net.ui.call;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.ecnetwork.iptnet.net.CallSessionActivity;
import com.ecnetwork.iptnet.net.R;
import com.ecnetwork.iptnet.net.VOIPProfile;
import com.iptnet.voipframework.RegisterInfo;
import com.iptnet.voipframework.VOIP;
import com.iptnet.voipframework.VOIPCallbacks;

public class CallFragment extends Fragment {

    private VOIP voip = null;
    private TextView textViewLoginState = null;
    private EditText editTextCalleeId = null;
    private Button buttonDial = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_call, container, false);
        textViewLoginState = (TextView)root.findViewById(R.id.textView_loginState);

        editTextCalleeId = (EditText)root.findViewById(R.id.editTextText_calleeId);
        buttonDial = (Button)root.findViewById(R.id.button_dial);

        VOIPProfile profile = new VOIPProfile(getContext());
        editTextCalleeId.setText(profile.getCalleeId());

        buttonDial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( editTextCalleeId.getText().length() > 0 && voip.isLogin() == true )
                {
                    VOIPProfile profile = new VOIPProfile(getContext());
                    String realCalleeId = editTextCalleeId.getText()+ "@"+ profile.getServer();
                    profile.save(editTextCalleeId.getText().toString());

                    Bundle bundle = new Bundle();
                    bundle.putBoolean("incoming", false);
                    bundle.putString("calleeId", realCalleeId);
                    Intent intent = new Intent(getContext(), CallSessionActivity.class);
                    intent.putExtra("CallSessionInfo",bundle);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                }
            }
        });
        voip = VOIP.sharedInstance();
        voip.addOnLoginListener(mOnLoginListener);

        if ( voip.isLogin() )
        {
            textViewLoginState.setBackgroundColor(Color.GREEN);
        }
        return root;
    }

    @Override
    public void onDestroyView () {
        super.onDestroyView();
        voip.removeOnLoginListener(mOnLoginListener);
    }

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