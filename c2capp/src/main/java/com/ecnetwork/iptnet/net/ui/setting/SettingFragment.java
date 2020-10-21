package com.ecnetwork.iptnet.net.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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

import com.ecnetwork.iptnet.net.R;
import com.ecnetwork.iptnet.net.VOIPProfile;
import com.iptnet.voipframework.RegisterInfo;
import com.iptnet.voipframework.VOIP;
import com.iptnet.voipframework.VOIPCallbacks;

public class SettingFragment extends Fragment {

    private VOIP voip = null;

    private TextView textViewLog = null;
    private EditText editText_account = null;
    private EditText editText_password = null;
    private EditText editText_server = null;
    private EditText editText_username = null;
    private EditText editText_photo = null;
    private Button buttonLoginAction = null;


    private static boolean mLoginActionState = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_setting, container, false);

        buttonLoginAction = (Button) root.findViewById(R.id.button_loginaction);
        textViewLog = (TextView) root.findViewById(R.id.textView_log);
        textViewLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        editText_account = (EditText) root.findViewById(R.id.editText_account);
        editText_password = (EditText) root.findViewById(R.id.editText_password);
        editText_server = (EditText) root.findViewById(R.id.editText_server);
        editText_username = (EditText) root.findViewById(R.id.editText_username);
        editText_photo = (EditText) root.findViewById(R.id.editText_photo);

        if (mLoginActionState == true) {
            buttonLoginAction.setText("Logout");
        }

        buttonLoginAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLoginActionState == false) {
                    String realAccount = editText_account.getText().toString()+"@"+editText_server.getText().toString();
                    voip.login(editText_server.getText().toString(),realAccount , editText_password.getText().toString());
                    voip.setUserProfile(editText_username.getText().toString(), editText_photo.getText().toString());

                    VOIPProfile profile = new VOIPProfile(getActivity());

                    profile.save(editText_account.getText().toString(),
                            editText_password.getText().toString(),
                            editText_server.getText().toString(),
                            editText_username.getText().toString(),
                            editText_photo.getText().toString());

                    mLoginActionState = true;
                    buttonLoginAction.setText("Logout");

                } else {
                    voip.logout();
                    mLoginActionState = false;
                    buttonLoginAction.setText("Login");
                }
            }
        });
        VOIPProfile profile = new VOIPProfile(getContext());

        voip = VOIP.sharedInstance();
        voip.addOnLoginListener(mOnLoginListener);

        editText_account.setText(profile.getAccount());
        editText_password.setText(profile.getPasswrod());
        editText_server.setText(profile.getServer());
        editText_username.setText(profile.getUsername());
        editText_photo.setText(profile.getPhotoURL());
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
            textViewLog.append("onLogining\n");
        }

        @Override
        public void onLoginSuccess(RegisterInfo info) {
            textViewLog.append("onLoginSuccess\n");
        }

        @Override
        public void onLoginFailure(RegisterInfo info) {
            textViewLog.append("onLoginFailure:"+info.errorMsg+"\n");
        }

        @Override
        public void onLogoutByServer(RegisterInfo info) {
            textViewLog.append("onLogoutByServer:"+info.errorMsg+"\n");
        }

        @Override
        public void onLogoutDone(RegisterInfo info) {

            textViewLog.append("onLogoutDone\n");
        }

        @Override
        public void onLogoutFailed(RegisterInfo info) {

            textViewLog.append("onLogoutFailed\n");
        }
    };

}