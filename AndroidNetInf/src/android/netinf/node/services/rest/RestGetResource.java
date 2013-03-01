package android.netinf.node.services.rest;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

import android.netinf.common.Ndo;
import android.netinf.node.get.Get;
import android.util.Log;

public class RestGetResource extends ServerResource {

    public static final String TAG = "RestGetResource";

    @org.restlet.resource.Get
    public Representation handleGet() {
        Log.i(TAG, "REST API received GET");

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
        Ndo ndo = new Ndo(algorithm, hash);
        Get get = new Get(RestApi.getInstance(), ndo);

        // Get
        Ndo result = get.execute();
        if (result == null || !result.isCached()) {
            setStatus(Status.SUCCESS_NO_CONTENT);
            return null;
        }

        try {

            JSONObject json = new JSONObject();
            json.put(RestCommon.PATH, result.getOctets().getCanonicalPath());
            json.put(RestCommon.META, result.getMetadata().toString());

            setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(json.toString());

        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create JSON", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        } catch (IOException e) {
            Log.wtf(TAG, "Failed to get file path", e);
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }

    }

}
