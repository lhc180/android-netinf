package android.netinf.node.services.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.netinf.common.NetInfException;
import android.netinf.node.SettingsActivity;
import android.os.Environment;
import android.util.Log;

public class HttpCommon {

    public static final String TAG = HttpCommon.class.getSimpleName();

    // public static final String[] PEERS = {"http://213.159.185.124:8080", "http://213.159.185.166:8082"};
//    public static final String[] PEERS = {"http://213.159.185.166:8082"};
//    public static final String[] PEERS = {"http://213.159.185.124:8080"};

    public static int getTimeout() {
        return SettingsActivity.getPreferenceAsInt("pref_key_http_timeout");
    }

    public static String[] getPeers() {
        if (SettingsActivity.getPreference("pref_key_http_routing").equalsIgnoreCase("Static")) {
            // Static routing
            String[] peers = SettingsActivity.getPreference("pref_key_http_static_peers").split(" ");
            for (int i = 0; i < peers.length; i++) {
                peers[i] = "http://" + peers[i].trim();
            }
            Log.d(TAG, "Routing HTTP to static peers: " + Arrays.toString(peers));
            return peers;
        } else {
            // No routing
            Log.d(TAG, "HTTP routing disabled");
            return new String[0];
        }
    }

    public static String getContentType(HttpResponse response) throws NetInfException {
        Header header = getEntity(response).getContentType();
        if (header == null) {
            throw new NetInfException("HTTP response content-type is null");
        }
        return header.getValue();
    }

    public static HttpEntity getEntity(HttpResponse response) throws NetInfException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new NetInfException("HTTP response entity is null");
        }
        return entity;
    }

    public static InputStream getContent(HttpEntity entity) throws NetInfException {
        try {
            return entity.getContent();
        } catch (IllegalStateException e) {
            throw new NetInfException("HTTP response content can not be reused", e);
        } catch (IOException e) {
            throw new NetInfException("Failed to create content stream", e);
        }
    }

    public static String getJson(InputStream content) throws NetInfException {
        try {
            return IOUtils.toString(content);
        } catch (IOException e) {
            throw new NetInfException("Failed to convert stream to string", e);
        }
    }

    public static JSONObject parseJson(String json) throws NetInfException {
        Log.d(TAG, "json = " + json);
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new NetInfException("Failed to parse JSON in HTTP response", e);
        }
    }

    public static String postToString(MultipartEntity multipart) {

        String result = "<Failed to convert post to string>";
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/post.txt");
            FileOutputStream fis = new FileOutputStream(file);
            multipart.writeTo(fis);
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            Log.wtf(TAG, "Failed to convert post to string", e);
        }
        return result;

    }

}
