package com.jfloydconsult.mataturider.utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SurveyLab {

    private static SurveyLab sSurveyLab;
    private static Context mContext;


    public SurveyLab(Context context) {
        mContext = context;
    }

    public static SurveyLab get(Context context) {
        if (sSurveyLab == null) {
            sSurveyLab = new SurveyLab(context);
        }
        return sSurveyLab;
    }

    public boolean checkConnectivity() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                //we are connected to a network
                connected = true;
            } else {
                Toast.makeText(mContext, "Check your Internet Connection...", Toast.LENGTH_SHORT).show();
                connected = false;
            }
        }
        return connected;
    }
}
