package android.netinf.node.services.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.search.SearchService;
import android.util.Log;

public class HttpSearchService implements SearchService {

    public static final String TAG = HttpSearchService.class.getSimpleName();

    public static final int TIMEOUT = 1000;

    @Override
    public SearchResponse perform(Search search) {
        Log.i(TAG, "HTTP CL received SEARCH: " + search);

        // HTTP Client
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClient client = new DefaultHttpClient(params);

        Set<Ndo> results = new LinkedHashSet<Ndo>();
        for (String peer : HttpCommon.getPeers()) {
            try {
                HttpResponse response = client.execute(createSearch(peer, search));
                int status = response.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    results.addAll(parse(response));
                } else {
                    Log.e(TAG, "SEARCH to " + peer + " failed: " + status);
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG, "SEARCH to " + peer + " failed", e);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "SEARCH to " + peer + " failed", e);
            } catch (IOException e) {
                Log.e(TAG, "SEARCH to " + peer + " failed", e);
            } catch (NetInfException e) {
                Log.e(TAG, "SEARCH to " + peer + " failed", e);
            }
        }

        return new SearchResponse.Builder(search).addResults(results).build();

    }

    private HttpPost createSearch(String peer, Search search) throws UnsupportedEncodingException {

        HttpPost post = new HttpPost(peer + "/netinfproto/search");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");

        StringBuilder builder = new StringBuilder();
        builder.append("?msgid=");
        builder.append(RandomStringUtils.randomAlphanumeric(20));
        builder.append("&tokens=");
        builder.append(createTokens(search.getTokens()));

        HttpEntity entity = new StringEntity(builder.toString(), "UTF-8");
        post.setEntity(entity);

        return post;

    }

    private String createTokens(Set<String> tokens) {
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            builder.append(token);
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    private Set<Ndo> parse(HttpResponse response) throws NetInfException {

        Set<Ndo> ndos = new LinkedHashSet<Ndo>();

        // Get Results
        HttpEntity entity = HttpCommon.getEntity(response);
        InputStream content = HttpCommon.getContent(entity);
        String json = HttpCommon.getJson(content);
        JSONObject jo = HttpCommon.parseJson(json);

        JSONArray results = null;
        try {
            results = jo.getJSONArray("results");
            Log.d(TAG, results.toString(4));
        } catch (JSONException e) {
            throw new NetInfException("Failed to parse results", e);
        }

        // Results to NDO(s)
        for (int i = 0; i < results.length(); i++) {
            try {
                JSONObject result = results.getJSONObject(i);

                String algorithm = getAlgorithm(result);
                String hash = getHash(result);
                String metadata = getMetadata(result);

                Ndo ndo = new Ndo.Builder(algorithm, hash).metadata(new Metadata(metadata)).build();
                ndos.add(ndo);

            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse result", e);
            }
        }

        return ndos;
    }

    private String getAlgorithm(JSONObject jo) throws JSONException {
        String ni = jo.getString("ni");
        Pattern pattern = Pattern.compile("/(.*?);");
        Matcher matcher = pattern.matcher(ni);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new JSONException("Failed to parse hash algorithm");
        }
    }

    private String getHash(JSONObject jo) throws JSONException {
        String ni = jo.getString("ni");
        Pattern pattern = Pattern.compile(";(.*?)$");
        Matcher matcher = pattern.matcher(ni);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new JSONException("Failed to parse hash");
        }
    }

    private String getMetadata(JSONObject jo) throws JSONException {
        String metadata = jo.getString("metadata");
        return metadata.toString();
    }

}
