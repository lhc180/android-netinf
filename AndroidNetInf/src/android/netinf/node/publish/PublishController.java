package android.netinf.node.publish;

import java.util.LinkedList;
import java.util.List;

import android.netinf.common.ApiToServiceMap;
import android.netinf.common.NetInfStatus;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.util.Log;

public class PublishController implements PublishService {

    public static final String TAG = PublishController.class.getSimpleName();

    private ApiToServiceMap<PublishService> mServices;

    public PublishController(ApiToServiceMap<PublishService> services) {
        mServices = services;
    }

    @Override
    public PublishResponse perform(Publish incomingPublish) {

        // Reduce hop limit
        Publish publish = new Publish.Builder(incomingPublish).consumeHop().build();

        List<PublishResponse> responses = new LinkedList<PublishResponse>();

        // Check local services
        for (PublishService publishService : mServices.getLocalServices(publish.getSource())) {
            responses.add(publishService.perform(publish));
        }

        // Check other services
        if (publish.getHopLimit() > 0) {
            for (PublishService publishService : mServices.getRemoteServices(publish.getSource())) {
                responses.add(publishService.perform(publish));
            }
        }

        // Decide aggregated status
        for (PublishResponse response : responses) {
            if (response.getStatus().isSuccess()) {
                Log.i(TAG, "PUBLISH of " + publish + " done. STATUS " + response.getStatus());
                return new PublishResponse(publish, NetInfStatus.OK);
            }
        }

        Log.i(TAG, "PUBLISH of " + publish + " failed. STATUS " + NetInfStatus.FAILED);
        return new PublishResponse(publish, NetInfStatus.FAILED);

    }

}
