package android.netinf.node.services.rest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.util.Log;

public class RestSearchResource extends ServerResource {

    public static final String TAG = "RestSearchResource";

    public static final long TIMEOUT = 5000;

    @Get
    public Representation handleSearch() {
        Log.i(TAG, "REST API received SEARCH");

        // Extract
        Map<String, String> query = getQuery().getValuesMap();

        // Check for needed input
        if (!query.containsKey(RestCommon.TOKENS)) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Missing tokens");
            return null; // TODO is this ok?
        }

        Set<String> tokens = new HashSet<String>();
        for (String token : query.get(RestCommon.TOKENS).split(" ")) {
            tokens.add(token);
        }
        Set<Ndo> ndos = Node.getInstance().search(tokens, TIMEOUT);

        try {
            JSONObject json = new JSONObject();
            JSONArray results = new JSONArray();
            json.put("results", results);
            for (Ndo ndo : ndos) {
                JSONObject result = new JSONObject();
                result.put("ni", ndo.getUri());
                result.put("meta", ndo.getMetadata().toJson());
                results.put(result);
            }
            setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(json.toString());
        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create search response JSON");
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }

    }

}