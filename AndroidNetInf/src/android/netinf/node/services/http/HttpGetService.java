package android.netinf.node.services.http;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.apache.commons.io.IOUtils;
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

import android.netinf.common.Locator;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.node.Node;
import android.netinf.node.get.GetService;
import android.netinf.node.logging.LogEntry;
import android.util.Log;

public class HttpGetService implements GetService {

    public static final String TAG = HttpGetService.class.getSimpleName();

    public static final int TIMEOUT = 2000;

    @Override
    public GetResponse perform(Get get) {
        Log.i(TAG, "HTTP CL received GET: " + get);

        // HTTP Client
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClient client = new DefaultHttpClient(params);

        // Repeat GET until ok result
        for (String peer : HttpCommon.PEERS) {
            try {
                HttpResponse response = client.execute(createGet(peer, get));
                Node.log(LogEntry.newOutgoing("HTTP"), get);
                GetResponse getResponse = parse(get, response);
                Node.log(LogEntry.newIncoming("HTTP"), getResponse);
                if (getResponse.getStatus().isSuccess()) {
                    return getResponse;
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG, "GET to " + peer + " failed", e);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "GET to " + peer + " failed", e);
            } catch (IOException e) {
                Log.e(TAG, "GET to " + peer + " failed", e);
            } catch (NetInfException e) {
                Log.e(TAG, "GET to " + peer + " failed", e);
            }
        }

        return new GetResponse.Builder(get).failed().build();
    }

    private HttpPost createGet(String peer, Get get) throws UnsupportedEncodingException {

        HttpPost post = new HttpPost(peer + "/netinfproto/get");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");

        StringBuilder builder = new StringBuilder();
        builder.append("URI=");
        builder.append(get.getNdo().getCanonicalUri());
        builder.append("&msgid=");
        builder.append(RandomStringUtils.randomAlphanumeric(20));

        HttpEntity entity = new StringEntity(builder.toString(), "UTF-8");
        post.setEntity(entity);

        return post;

    }

    private GetResponse parse(Get get, HttpResponse response) throws NetInfException {

        int status = response.getStatusLine().getStatusCode();

        switch (status) {
            case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
                return parseLocatorResponse(get, response);
            case HttpStatus.SC_OK:
                return parseBinaryResponse(get, response);
            default:
                throw new NetInfException("Unhandled status code: " + status);
        }

    }

    private GetResponse parseLocatorResponse(Get get, HttpResponse response) throws NetInfException {

        // Get JSON
        HttpEntity entity = HttpCommon.getEntity(response);
        InputStream content = HttpCommon.getContent(entity);
        String json = HttpCommon.getJson(content);
        JSONObject jo = HttpCommon.parseJson(json);

        // Result NDO
        Ndo.Builder builder = new Ndo.Builder(get.getNdo());

        // Locators (required, locator response without locators not useful)
        try {
            builder.locators(getLocators(jo));
        } catch (JSONException e) {
            throw new NetInfException("Failed to parse locators", e);
        }

        // Metadata (optional)
        try {
            builder.metadata(getMetadata(jo));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse metadata", e);
        }

        return new GetResponse.Builder(get).ok(get).build();
    }

    private GetResponse parseBinaryResponse(Get get, HttpResponse response) throws NetInfException {

        String contentType = HttpCommon.getContentType(response);
        if (contentType.startsWith("multipart/form-data")) {
            // Expected
            return parseMultipart(get, response);
        } else if (contentType.startsWith("application/octet-stream")) {
            // NiProxy
            return parseBinary(get, response);
        } else {
            throw new NetInfException("Unhandled content-type");
        }
    }

    private GetResponse parseMultipart(Get get, HttpResponse response) throws NetInfException {

        // Multipart Boundary
        String contentType = HttpCommon.getContentType(response);
        byte[] boundary = contentType.substring(contentType.indexOf("boundary=") + 9).getBytes();

        // Multipart Content
        HttpEntity entity = HttpCommon.getEntity(response);
        InputStream content = HttpCommon.getContent(entity);

        // Multipart Stream
        @SuppressWarnings("deprecation")
        MultipartStream multipartStream = new MultipartStream(content, boundary);

        // Read
        OutputStream jsonStream = null;
        OutputStream binaryStream = null;

        try {

            jsonStream = new ByteArrayOutputStream();
            binaryStream = new FileOutputStream(get.getNdo().getOctets());

            // TODO Is the order of the fields always the same?
            multipartStream.readHeaders();
            multipartStream.readBodyData(jsonStream);
            multipartStream.readBoundary();
            multipartStream.readHeaders();
            multipartStream.readBodyData(binaryStream);

            binaryStream.flush();

            jsonStream.close();
            binaryStream.close();

        } catch (MalformedStreamException e) {
            throw new NetInfException("Malformed multipart/form-data", e);
        } catch (IOException e) {
            throw new NetInfException("Failed to parse multipart/form-data", e);
        } finally {
            IOUtils.closeQuietly(jsonStream);
            IOUtils.closeQuietly(binaryStream);
        }

        // Result NDO
        Ndo.Builder builder = new Ndo.Builder(get.getNdo());

        JSONObject jo = HttpCommon.parseJson(jsonStream.toString());
        try {
            builder.locators(getLocators(jo));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse locators", e);
        }
        try {
            builder.metadata(getMetadata(jo));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse metadata", e);
        }

        return new GetResponse.Builder(get).ok(get).build();

    }

    private GetResponse parseBinary(Get get, HttpResponse response) throws NetInfException {

        // Read
        InputStream in = null;
        OutputStream out = null;
        try {
            in = HttpCommon.getContent(HttpCommon.getEntity(response));
            out = get.getNdo().newCacheStream();
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (IOException e) {
            throw new NetInfException("Failed to parse application/octet-stream", e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }

        Ndo ndo = new Ndo.Builder(get.getNdo()).build();
        return new GetResponse.Builder(get).ok(get).build();

    }

    private Set<Locator> getLocators(JSONObject jo) throws JSONException {
        Set<Locator> result = new LinkedHashSet<Locator>();
        JSONArray locs = jo.getJSONArray("loc");
        for (int i = 0; i < locs.length(); i++) {
            String loc = (String) locs.get(i);
            result.add(Locator.fromString(loc));
        }
        return result;
    }

    private Metadata getMetadata(JSONObject jo) throws JSONException {
        return new Metadata(jo.getJSONObject("metadata"));
    }

}
