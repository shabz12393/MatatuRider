package com.jfloydconsult.mataturider.common;

import com.jfloydconsult.mataturider.retrofit.IGoogleAPI;
import com.jfloydconsult.mataturider.retrofit.RetrofitClient;

public class Common {
    public static final String baseURL = "https://maps.googleapis.com";
    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
