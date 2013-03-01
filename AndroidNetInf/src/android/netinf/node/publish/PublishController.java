package android.netinf.node.publish;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import android.netinf.common.NetInfStatus;
import android.netinf.node.api.Api;
import android.util.Log;

public class PublishController {

    public static final String TAG = "PublishController";

    Map<Api, Set<PublishService>> mPublishServices;

    public PublishController() {
        mPublishServices = new HashMap<Api, Set<PublishService>>();
    }

    public void registerPublishService(Api source, PublishService destination) {
        if (!mPublishServices.containsKey(source)) {
            mPublishServices.put(source, new LinkedHashSet<PublishService>());
        }
        mPublishServices.get(source).add(destination);
    }

    public NetInfStatus publish(Publish publish) {
        Log.v(TAG, "publish()");

        NetInfStatus finalStatus = NetInfStatus.I_FAILED;

        for (PublishService publishService : mPublishServices.get(publish.getSource())) {
            NetInfStatus partialStatus = publishService.publish(publish);
            if (partialStatus.equals(NetInfStatus.OK)) {
                finalStatus = NetInfStatus.OK;
            }
        }

        if (finalStatus.equals(NetInfStatus.OK)) {
            Log.i(TAG, "PUBLISH succeeded at least once");
        } else {
            Log.i(TAG, "PUBLISH failed to all");
        }
        return finalStatus;

    }

}
