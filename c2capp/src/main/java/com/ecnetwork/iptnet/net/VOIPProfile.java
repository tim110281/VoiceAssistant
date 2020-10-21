package com.ecnetwork.iptnet.net;

import android.content.Context;
import android.content.SharedPreferences;

public class VOIPProfile {

    private final  String kUserFile = "userfile";
    private final  String kAccountKey = "voipaccount";
    private final  String kPasswordKey = "voippassword";
    private final  String kServerKey = "voipserver";
    private final  String kNameKey = "voipname";
    private final  String kPhotoKey = "voipphoto";
    private final  String kCalleeId = "voipcalleeid";

    private String mAccount;
    private String mPassword;
    private String mServer;
    private String mUsername;
    private String mPhotoURL;
    private String mCalleeId;

    Context mContext = null;
    public  VOIPProfile(Context context)
    {
        mContext = context;
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(kUserFile, Context.MODE_PRIVATE);
        mAccount = sharedPreferences.getString(kAccountKey,"");
        mPassword = sharedPreferences.getString(kPasswordKey,"");
        mServer = sharedPreferences.getString(kServerKey,"");
        mUsername = sharedPreferences.getString(kNameKey,"");
        mPhotoURL = sharedPreferences.getString(kPhotoKey,"");
        mCalleeId = sharedPreferences.getString(kCalleeId,"");

    }
    public void save(String account, String password, String Server, String username, String photoURL)
    {

        mAccount = account;
        mPassword = password;
        mServer = Server;
        mUsername = username;
        mPhotoURL = photoURL;


        SharedPreferences sharedPreferences = mContext.getSharedPreferences(kUserFile, Context.MODE_PRIVATE);
        final boolean commit = sharedPreferences.edit()
                .putString(kAccountKey, mAccount)
                .putString(kPasswordKey, mPassword)
                .putString(kServerKey, mServer)
                .putString(kNameKey, mUsername)
                .putString(kPhotoKey, mPhotoURL)
                .putString(kCalleeId, mCalleeId)
                .commit();

    }

    public void save(String calleeId)
    {
        mCalleeId = calleeId;
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(kUserFile, Context.MODE_PRIVATE);
        final boolean commit = sharedPreferences.edit()
                .putString(kAccountKey, mAccount)
                .putString(kPasswordKey, mPassword)
                .putString(kServerKey, mServer)
                .putString(kNameKey, mUsername)
                .putString(kPhotoKey, mPhotoURL)
                .putString(kCalleeId, mCalleeId)
                .commit();
    }

    public String getAccount()
    {
        return mAccount;
    }

    public String getPasswrod()
    {
        return mPassword;
    }

    public String getServer()
    {
        return mServer;
    }

    public String getUsername()
    {
        return mUsername;
    }

    public String getPhotoURL()
    {
        return mPhotoURL;
    }

    public String getCalleeId()
    {
        return mCalleeId;
    }
}
