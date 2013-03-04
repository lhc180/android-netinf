package android.netinf.node.services.rest;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import android.netinf.common.Locator;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfStatus;
import android.netinf.common.NetInfUtils;
import android.netinf.node.publish.Publish;
import android.util.Log;

public class RestPublishResource extends ServerResource {

    public static final String TAG = "RestPublishResource";

    @Post
    @Put
    public Representation handlePublish() {
        Log.v(TAG, "handlePublish()");

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

        // Add Bluetooth
        if (query.containsKey(RestCommon.BLUETOOTH)) {
            ndo.addLocator(Locator.fromBluetooth(query.get(RestCommon.BLUETOOTH)));
        }

        // Add Metadata
        if (query.containsKey(RestCommon.META)) {
            try {
                ndo.addMetadata(new Metadata(query.get(RestCommon.META)));
            } catch (NetInfException e) {
                Log.w(TAG, "Tried to add invalid JSON as metadata", e);
            }
        }

        // Create Publish
        Publish publish = new Publish(RestApi.getInstance(), NetInfUtils.newMessageId(), ndo);

        // Full Put
        if (query.containsKey(RestCommon.PATH)) {
            try {
                ndo.setOctets(new File(query.get(RestCommon.PATH)));
                publish.setFullPut(true);
            } catch (IOException e) {
                Log.e(TAG, "Failed to set NDO octets", e);
            }
        }

        Log.i(TAG, "REST API received PUBLISH: " + publish);

        // Publish
        publish.execute();
        NetInfStatus status = publish.getResult();
        if (status != NetInfStatus.OK) {
            setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }

        setStatus(Status.SUCCESS_CREATED);
        return null;

    }

}
