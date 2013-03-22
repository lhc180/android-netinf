package android.netinf.node.services.rest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.get.Get;
import android.netinf.node.get.GetResponse;
import android.util.Log;

public class RestGetResource extends ServerResource {

    public static final String TAG = RestGetResource.class.getSimpleName();

    @org.restlet.resource.Get
    public Representation handleGet() {
        Log.v(TAG, "handleGet()");

        // Extract
        Map<String, String> query = getQuery().getValuesMap();

        // Check for needed input
        if (!query.containsKey(RestCommon.ALGORITHM)) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Missing hash algorithm");
            return null; // TODO is this ok?
        }
        if (!query.containsKey(RestCommon.HASH)) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Missing hash");
            return null; // TODO is this ok?
        }

        // Create NDO
        String algorithm = query.get(RestCommon.ALGORITHM);
        String hash = query.get(RestCommon.HASH);
        Ndo ndo = new Ndo.Builder(algorithm, hash).build();
        Get get = new Get.Builder(RestApi.getInstance(), ndo).build();

        Log.i(TAG, "REST API received GET: " + get);

        // Get
        try {
            GetResponse response = Node.submit(get).get();

            // Get was performed but did not result in octets
            if (response.getStatus().isError() || !response.getNdo().isCached()) {
                setStatus(Status.SUCCESS_NO_CONTENT);
                return null;
            }

            // Get was performed and resulted in octets
            JSONObject json = new JSONObject();
            json.put(RestCommon.PATH, response.getNdo().getOctets().getCanonicalPath());
            json.put(RestCommon.META, response.getNdo().getMetadata().toString());
            setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(json.toString());

        } catch (InterruptedException e) {
            Log.e(TAG, "GET failed", e);
        } catch (ExecutionException e) {
            Log.e(TAG, "GET failed", e);
        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create JSON", e);
        } catch (IOException e) {
            Log.wtf(TAG, "Failed to get file path", e);
        }
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;

    }

}
