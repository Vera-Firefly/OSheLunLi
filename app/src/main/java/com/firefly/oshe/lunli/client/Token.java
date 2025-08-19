package com.firefly.oshe.lunli.client;

public class Token {

    public static String DefaultAPI() {
        String api = String.format(
                "https://%s",
                getApi("AA")
                );
    	return api;
    }

    public static String supabaseAPI() {
        String api = String.format(
                "https://%s",
                getApi("AA")
                );
        return api;
    }

    public static String supabaseToken() {
        String token = null;
        token = String.format("%s.%s.%s",
                getApi("AA"),
                getApi("AA"),
                getApi("AA")
        );
        return token;
    }

    public static String[] API() {
        // TODO: 
    	return null;
    }

    public static String TOKEN() {
        String token = null;
        String keys[] = getToken("AB");
        String api = getApi("AA");
        token = api + keys[0] + getApi("90") + keys[1];
    	return token;
    }

    static {
        System.loadLibrary("firefly");
    }

    public static native String[] getToken(String input);
    public static native String getApi(String input);
}
