package android.netinf.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class NetInfUtils {

    public static final String TAG = "NetInfUtils";

    public static String getAuthority(String uri) throws NetInfException {
        Log.v(TAG, "getAuthority()");
        Pattern pattern = Pattern.compile("://(.*?)/");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new NetInfException("Failed to parse authority from URI");
        }
    }

    public static String getAlgorithm(String uri) throws NetInfException {
        Log.v(TAG, "getAlgorithm()");
        Pattern pattern = Pattern.compile("/(.*?);");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new NetInfException("Failed to parse hash algorithm from URI");
        }
    }

    public static String getHash(String uri) throws NetInfException {
        Log.v(TAG, "getHash()");
        Pattern pattern = Pattern.compile(";(.*?)$");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new NetInfException("Failed to parse hash from URI");
        }
    }

    public static String newMessageId() {
        // TODO good enough?
        return RandomStringUtils.randomAlphanumeric(20);
    }

    public static Ndo toNdo(JSONObject jo) throws NetInfException {
        Log.v(TAG, "toNdo()");

        // Required
        // Algorithm and hash
        String uri = null;
        try {
            uri = jo.getString("uri");
        } catch (JSONException e) {
            throw new NetInfException("Failed to create NDO from JSON, URI not found", e);
        }

        String algorithm = getAlgorithm(uri);
        String hash = getHash(uri);
        Ndo ndo = new Ndo(algorithm, hash);

        // Optional
        // Authority
        try {
            ndo.setAuthority(getAuthority(uri));
        } catch (NetInfException e) {
            Log.w(TAG, "Failed to parse authority, defaulting to none", e);
        }
        // Locators
        try {
            JSONArray locators = jo.getJSONArray("locators");
            for (int i = 0; i < locators.length(); i++) {
                try {
                    ndo.addLocator(Locator.fromString(locators.getString(i)));
                } catch (JSONException e) {
                    Log.w(TAG, "Invalid locator, skipped", e);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse locators, defaulting to none", e);
        }
        // Metadata
        try {
            ndo.addMetadata(new Metadata(jo.getJSONObject("ext").getJSONObject("meta")));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse metadata, defaulting to empty");
        }

        return ndo;

    }

    //    public static JSONObject toJson(Ndo ndo) throws NetInfException {
    //        try {
    //            JSONObject jo = new JSONObject();
    //            jo.put("uri", ndo.getUri());
    //            jo.put("locators", new JSONArray(ndo.getLocators()));
    //            jo.put("metadata", ndo.getMetadata().toJson());
    //            if (ndo.getFullPut()) {
    //                byte[] bytes = FileUtils.readFileToByteArray(ndo.getCache());
    //                jo.put("base64", Base64.encodeToString(bytes, Base64.DEFAULT));
    //            }
    //            return jo;
    //        } catch (JSONException e) {
    //            throw new NetInfException("Failed to create JSONObject from Ndo", e);
    //        } catch (IOException e) {
    //            throw new NetInfException("Failed to create JSONObject from Ndo", e);
    //        }
    //    }
    //
    //    public static Ndo fromJson(String json) throws NetInfException {
    //
    //        // TODO these stacked try-catch are horrible
    //
    //        // Outer try, required
    //        try {
    //
    //            JSONObject jo = new JSONObject(json);
    //            String uri = jo.getString("uri");
    //            String algorithm = getAlgorithm(uri);
    //            String hash = getAlgorithm(uri);
    //
    //            Ndo ndo = new Ndo(algorithm, hash);
    //
    //            // Authority
    //            try {
    //                ndo.setAuthority(getAuthority(uri));
    //            } catch (NetInfException e) {
    //                Log.w(TAG, "URI did not contain a valid authority: " + uri);
    //            }
    //
    //            // Locators
    //            try {
    //                JSONArray ja = jo.getJSONArray("locators");
    //                for (int i = 0; i < ja.length(); i++) {
    //                    try {
    //                        ndo.addLocator(Locator.fromString(ja.getString(i)));
    //                    } catch (JSONException e) {
    //                        Log.w(TAG, "JSON contained an invalid locator", e);
    //                    }
    //                }
    //            } catch (JSONException e) {
    //                Log.w(TAG, "JSON did not contain valid locators");
    //            }
    //
    //            // Metadata
    //            try {
    //                ndo.addMetadata(new Metadata(jo.getJSONObject("metadata")));
    //            } catch (JSONException e) {
    //                Log.w(TAG, "JSON did not contain valid metadata");
    //            }
    //
    //            // Data
    //
    //
    //        } catch (JSONException e) {
    //            throw new NetInfException("Failed to create Ndo, URI did not contain algorithm and hash", e);
    //        }
    //
    //    }

}
