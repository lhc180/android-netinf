package android.netinf.node.services.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.node.publish.PublishService;
import android.util.Log;

public class HttpPublishService implements PublishService {

    public static final String TAG = HttpPublishService.class.getSimpleName();

    @Override
    public PublishResponse perform(Publish publish) {
        Log.i(TAG, "HTTP PUBLISH " + publish);

        // HTTP Client
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, HttpCommon.getTimeout());
        HttpConnectionParams.setSoTimeout(params, HttpCommon.getTimeout());
        HttpClient client = new DefaultHttpClient(params);

        // Publish to all peers
        NetInfStatus status = NetInfStatus.FAILED;
        for (String peer : HttpCommon.getPeers()) {
            try {
                HttpResponse response = client.execute(createPublish(peer, publish));
                // Log.d(TAG, IOUtils.toString(response.getEntity().getContent()));
                int code = response.getStatusLine().getStatusCode();
                // NiProxy returns 200, Erlang returns 201
                if (code == HttpStatus.SC_CREATED || code == HttpStatus.SC_OK) {
                    Log.i(TAG, "PUBLISH to " + peer + " succeeded");
                    status = NetInfStatus.OK;
                } else {
                    Log.e(TAG, "PUBLISH to " + peer + " failed: " + code);
                }

            } catch (ClientProtocolException e) {
                Log.e(TAG, "PUBLISH to " + peer + " failed", e);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "PUBLISH to " + peer + " failed", e);
            } catch (IOException e) {
                Log.e(TAG, "PUBLISH to " + peer + " failed", e);
            }
        }

        return new PublishResponse.Builder(publish).status(status).build();

    }

    private HttpPost createPublish(String peer, Publish publish) throws UnsupportedEncodingException {

        Ndo ndo = publish.getNdo();

        HttpPost post = new HttpPost(peer + "/netinfproto/publish");

        MultipartEntity multipart = new MultipartEntity();

        StringBody uri = new StringBody(ndo.getCanonicalUri());
        multipart.addPart("URI", uri);

        StringBody msgid = new StringBody(publish.getId());
        multipart.addPart("msgid", msgid);

        int i = 1;
        for (Locator locator : ndo.getLocators()) {
            StringBody loc = new StringBody(locator.toString());
            multipart.addPart("loc" + i, loc);
            i++;
        }

        StringBody meta = new StringBody(ndo.getMetadata().toMetaString());
        multipart.addPart("ext", meta);

        if (publish.isFullPut()) {
            StringBody fullput = new StringBody("true");
            multipart.addPart("fullPut", fullput);
            FileBody octets = new FileBody(ndo.getOctets());
            multipart.addPart("octets", octets);
        }

        StringBody rform = new StringBody("json");
        multipart.addPart("rform", rform);

        post.setEntity(multipart);

        return post;
    }

}
