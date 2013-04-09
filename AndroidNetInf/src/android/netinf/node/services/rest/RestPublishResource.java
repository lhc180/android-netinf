package android.netinf.node.services.rest;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import android.netinf.common.Locator;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.node.Node;
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishResponse;
import android.util.Log;

public class RestPublishResource extends ServerResource {

    public static final String TAG = RestPublishResource.class.getSimpleName();

    @Post
    @Put
    public Representation handlePublish() {

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
        Ndo.Builder ndoBuilder = new Ndo.Builder(algorithm, hash);

        // Add Bluetooth
        if (query.containsKey(RestCommon.BLUETOOTH)) {
            ndoBuilder.locator(Locator.fromBluetooth(query.get(RestCommon.BLUETOOTH)));
        }

        // Add Metadata
        if (query.containsKey(RestCommon.META)) {
            try {
                ndoBuilder.metadata(new Metadata(query.get(RestCommon.META)));
            } catch (NetInfException e) {
                Log.w(TAG, "Tried to add invalid JSON as metadata", e);
            }
        }
        Ndo ndo = ndoBuilder.build();


        // Create Publish
        Publish.Builder publishBuilder = new Publish.Builder(RestApi.getInstance(), ndo);

        // Full Put
        if (query.containsKey(RestCommon.PATH)) {
            try {
                ndo.cache(new File(query.get(RestCommon.PATH)));
                publishBuilder.fullPut();
            } catch (IOException e) {
                Log.e(TAG, "Failed to set NDO octets", e);
            }
        }

        Log.i(TAG, "REST API received PUBLISH: " + publishBuilder);

        // Publish
        try {
            Publish publish = publishBuilder.build();
            PublishResponse response = Node.submit(publish).get();

            if (response.getStatus().isSuccess()) {
                setStatus(Status.SUCCESS_CREATED);
                return null;
            }

        } catch (InterruptedException e) {
            Log.e(TAG, "PUBLISH failed", e);
        } catch (ExecutionException e) {
            Log.e(TAG, "PUBLISH failed", e);
        }

        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;

    }

}
