package android.netinf.node.publish;

import android.netinf.common.NetInfStatus;
import android.netinf.common.Response;

public class PublishResponse extends Response {

    public PublishResponse(Publish publish, NetInfStatus status) {
        super(publish.getId(), status);
    }

}
