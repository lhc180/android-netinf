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

    public PublishController() {
        mPublishServices = new HashMap<Api, Set<PublishService>>();
    }

    public void registerPublishService(Api source, PublishService destination) {
        if (!mPublishServices.containsKey(source)) {
            mPublishServices.put(source, new LinkedHashSet<PublishService>());
        }
        mPublishServices.get(source).add(destination);
    }

    @Override
    public PublishResponse perform(Publish publish) {
        Log.v(TAG, "perform()");

        List<PublishResponse> responses = new LinkedList<PublishResponse>();

        for (PublishService publishService : mPublishServices.get(publish.getSource())) {
            responses.add(publishService.perform(publish));
        }

        for (PublishResponse response : responses) {
            if (response.getStatus().isSuccess()) {
                Log.i(TAG, "PUBLISH succeeded at least once");
                return new PublishResponse(publish, NetInfStatus.OK);
            }
        }

        Log.i(TAG, "PUBLISH failed to all");
        return new PublishResponse(publish, NetInfStatus.FAILED);

    }

}
