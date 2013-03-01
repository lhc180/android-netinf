package android.netinf.node.services.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
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
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishService;
import android.util.Log;

public class HttpPublishService implements PublishService {

    public static final String TAG = "HttpPublishService";

    public static final int TIMEOUT = 2000;

    @Override
    public NetInfStatus publish(Publish publish) {
        Log.v(TAG, "publish()");

        // HTTP Client
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClient client = new DefaultHttpClient(params);

        // Publish to all peers
        NetInfStatus result = NetInfStatus.I_FAILED;
        for (String peer : HttpCommon.PEERS) {
            try {
                HttpResponse response = client.execute(createPublish(peer, publish));
                Log.d(TAG, IOUtils.toString(response.getEntity().getContent()));
                int status = response.getStatusLine().getStatusCode();
                // NiProxy returns 200, Erlang returns 201
                if (status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK) {
                    Log.i(TAG, "PUBLISH to " + peer + " succeeded");
                    result = NetInfStatus.OK;
                } else {
                    Log.e(TAG, "PUBLISH to " + peer + " failed: " + status);
                }

            } catch (ClientProtocolException e) {
                Log.e(TAG, "PUBLISH to " + peer + " failed", e);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "PUBLISH to " + peer + " failed", e);
            } catch (IOException e) {
                Log.e(TAG, "PUBLISH to " + peer + " failed", e);
            }
        }

        return result;
    }

    private HttpPost createPublish(String peer, Publish publish) throws UnsupportedEncodingException {
        Log.v(TAG, "createPublish()");

        Ndo ndo = publish.getNdo();

        HttpPost post = new HttpPost(peer + "/netinfproto/publish");

        MultipartEntity multipart = new MultipartEntity();

        StringBody uri = new StringBody(ndo.getCanonicalUri());
        multipart.addPart("URI", uri);

        StringBody msgid = new StringBody(publish.getMessageId());
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
