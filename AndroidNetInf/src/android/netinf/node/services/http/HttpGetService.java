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
import org.apache.commons.lang.RandomStringUtils;
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
import android.netinf.node.get.Get;
import android.netinf.node.get.GetService;
import android.util.Log;

public class HttpGetService implements GetService {

    public static final String TAG = "HttpGetService";

    public static final int TIMEOUT = 2000;

    @Override
    public Ndo get(Get get) {
        Log.v(TAG, "get()");

        // HTTP Client
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClient client = new DefaultHttpClient(params);

        // Repeat GET until ok result
        Ndo result = null;
        for (String peer : HttpCommon.PEERS) {
            try {
                HttpResponse response = client.execute(createGet(peer, get));
                result = parse(get.getNdo(), response);
//                if (result != null) {
//                    break;
//                }
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

        return result;
    }

    private HttpPost createGet(String peer, Get get) throws UnsupportedEncodingException {
        Log.v(TAG, "createGet()");

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

    private Ndo parse(Ndo ndo, HttpResponse response) throws NetInfException {
        Log.v(TAG, "parse()");

        int status = response.getStatusLine().getStatusCode();

        switch (status) {
            case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
                return parseLocatorResponse(ndo, response);
            case HttpStatus.SC_OK:
                return parseBinaryResponse(ndo, response);
            default:
                throw new NetInfException("Unhandled status code: " + status);
        }

    }

    private Ndo parseLocatorResponse(Ndo ndo, HttpResponse response) throws NetInfException {
        Log.v(TAG, "parseLocatorsResponse()");

        // Get JSON
        HttpEntity entity = HttpCommon.getEntity(response);
        InputStream content = HttpCommon.getContent(entity);
        String json = HttpCommon.getJson(content);
        JSONObject jo = HttpCommon.parseJson(json);

        // Result NDO
        Ndo result = new Ndo(ndo);

        // Locators (required, locator response without locators not useful)
        try {
            result.addLocators(getLocators(jo));
        } catch (JSONException e) {
            throw new NetInfException("Failed to parse locators", e);
        }

        // Metadata (optional)
        try {
            result.addMetadata(getMetadata(jo));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse metadata", e);
        }

        return result;
    }

    private Ndo parseBinaryResponse(Ndo ndo, HttpResponse response) throws NetInfException {
        Log.v(TAG, "parseBinaryResponse()");

        String contentType = HttpCommon.getContentType(response);
        if (contentType.startsWith("multipart/form-data")) {
            // Expected
            return parseMultipart(ndo, response);
        } else if (contentType.startsWith("application/octet-stream")) {
            // NiProxy
            return parseBinary(ndo, response);
        } else {
            throw new NetInfException("Unhandled content-type");
        }
    }

    private Ndo parseMultipart(Ndo ndo, HttpResponse response) throws NetInfException {
        Log.v(TAG, "parseMultipart()");

        // Result NDO
        Ndo result = new Ndo(ndo);

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
            binaryStream = new FileOutputStream(result.getOctets());

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

        // Update result NDO
        JSONObject jo = HttpCommon.parseJson(jsonStream.toString());
        try {
            result.addLocators(getLocators(jo));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse locators", e);
        }
        try {
            result.addMetadata(getMetadata(jo));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse metadata", e);
        }

        return result;

    }

    private Ndo parseBinary(Ndo ndo, HttpResponse response) throws NetInfException {
        Log.v(TAG, "parseBinary()");

        // Result NDO
        Ndo result = new Ndo(ndo);

        // Read
        InputStream in = null;
        OutputStream out = null;
        try {
            in = HttpCommon.getContent(HttpCommon.getEntity(response));
            out = new FileOutputStream(ndo.getOctets());
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (IOException e) {
            throw new NetInfException("Failed to parse application/octet-stream", e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }

        return result;

    }

    private Set<Locator> getLocators(JSONObject jo) throws JSONException {
        Log.v(TAG, "getLocators");
        Set<Locator> result = new LinkedHashSet<Locator>();
        JSONArray locs = jo.getJSONArray("loc");
        for (int i = 0; i < locs.length(); i++) {
            String loc = (String) locs.get(i);
            result.add(Locator.fromString(loc));
        }
        return result;
    }

    private Metadata getMetadata(JSONObject jo) throws JSONException {
        Log.v(TAG, "getMetadata");
        return new Metadata(jo.getJSONObject("metadata"));
    }

}
