package android.netinf.node.publish;

import java.util.LinkedHashSet;
import java.util.Set;

import android.netinf.common.NetInfStatus;
import android.util.Log;

public class PublishController {

    public static final String TAG = "PublishController";

    Set<PublishService> mPublishServices = new LinkedHashSet<PublishService>();

    public void addPublishService(PublishService publishService) {
        mPublishServices.add(publishService);
    }

    public NetInfStatus publish(Publish publish) {
        NetInfStatus result = NetInfStatus.I_FAILED;
        for (PublishService publishService : mPublishServices) {
            // Local publishes only to local services. Non-local publishes to all.
            if (!publish.isLocal() || publishService.isLocal()) {
                NetInfStatus status = publishService.publish(publish);
                if (status.equals(NetInfStatus.OK)) {
                    result = NetInfStatus.OK;
                }
            }
        }
        if (result.equals(NetInfStatus.OK)) {
            Log.i(TAG, "PUBLISH succeeded at least once");
        } else {
            Log.i(TAG, "PUBLISH failed to all");
        }
        return result;
    }

}
