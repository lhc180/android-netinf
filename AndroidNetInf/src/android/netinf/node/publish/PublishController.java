package android.netinf.node.publish;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.netinf.common.NetInfStatus;
import android.netinf.node.api.Api;
import android.util.Log;

public class PublishController implements PublishService {

    public static final String TAG = "PublishController";

    private Map<Api, Set<PublishService>> mPublishServices;
    private Map<Api, Set<PublishService>> mLocalPublishServices;

    public PublishController() {
        mPublishServices = new HashMap<Api, Set<PublishService>>();
        mLocalPublishServices = new HashMap<Api, Set<PublishService>>();
    }

    public void addPublishService(Api source, PublishService destination) {
        if (!mPublishServices.containsKey(source)) {
            mPublishServices.put(source, new LinkedHashSet<PublishService>());
        }
        mPublishServices.get(source).add(destination);
    }

    public void addLocalPublishService(Api source, PublishService destination) {
        if (!mLocalPublishServices.containsKey(source)) {
            mLocalPublishServices.put(source, new LinkedHashSet<PublishService>());
        }
        mLocalPublishServices.get(source).add(destination);
    }

    @Override
    public PublishResponse perform(Publish incomingPublish) {
        Log.v(TAG, "perform()");

        // Reduce hop limit
        Publish publish = new Publish.Builder(incomingPublish).consumeHop().build();

        List<PublishResponse> responses = new LinkedList<PublishResponse>();

        // Check local services
        Log.i(TAG, "Local PUBLISH of " + publish);
        for (PublishService publishService : mLocalPublishServices.get(publish.getSource())) {
            responses.add(publishService.perform(publish));
        }

        // Check other services
        if (publish.getHopLimit() > 0) {
            Log.i(TAG, "Remote PUBLISH of " + publish);
            for (PublishService publishService : mPublishServices.get(publish.getSource())) {
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
