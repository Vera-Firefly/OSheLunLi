package com.firefly.oshe.lunli.client;

import com.firefly.oshe.lunli.info.InfoDistributor;

public class Token {

    public static String DefaultAPI() {
        String api = String.format(
                "https://%s",
                getApi(InfoDistributor.DEFAULT_API)
                );
    	return api;
    }

    public static String supabaseAPI() {
        String api = String.format(
                "https://%s",
                getApi(InfoDistributor.SUPABASE_API)
                );
        return api;
    }

    public static String supabaseToken() {
        String token = null;
        token = String.format("%s.%s.%s",
                getApi(InfoDistributor.SUPABASE_TOKEN_1),
                getApi(InfoDistributor.SUPABASE_TOKEN_2),
                getApi(InfoDistributor.SUPABASE_TOKEN_3)
        );
        return token;
    }

    public static String[] API() {
        // TODO: 
    	return null;
    }

    public static String TOKEN() {
        String token = null;
        String api = getApi(InfoDistributor.TOKEN_1);
        String api1 = getApi(InfoDistributor.TOKEN_2);
        String api2 = getApi(InfoDistributor.TOKEN_3);
        token = String.format("%s%s%s", api, api1, api2);
    	return token;
    }

    static {
        System.loadLibrary("firefly");
    }

    public static native String[] getToken(String input);
    public static native String getApi(String input);
}
