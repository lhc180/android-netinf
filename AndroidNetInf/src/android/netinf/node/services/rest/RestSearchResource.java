package android.netinf.node.services.rest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import android.netinf.common.Ndo;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.Node;
import android.util.Log;

public class RestSearchResource extends ServerResource {

    public static final String TAG = RestSearchResource.class.getSimpleName();

    public static final long TIMEOUT = 5000;

    @Get
    public Representation handleSearch() {

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
        Search search = new Search.Builder(RestApi.getInstance()).tokens(tokens).timeout(TIMEOUT).build();
        Log.i(TAG, "REST API received SEARCH: " + search);

        try {
            SearchResponse response = Node.submit(search).get();

            JSONObject json = new JSONObject();
            JSONArray results = new JSONArray();
            json.put("results", results);
            for (Ndo ndo : response.getResults()) {
                JSONObject result = new JSONObject();
                result.put("ni", ndo.getUri());
                result.put("meta", ndo.getMetadata().toJson());
                results.put(result);
            }
            setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(json.toString());

        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create search response JSON", e);
        } catch (InterruptedException e) {
            Log.wtf(TAG, "SEARCH failed", e);
        } catch (ExecutionException e) {
            Log.wtf(TAG, "SEARCH failed", e);
        }

        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;

    }

}