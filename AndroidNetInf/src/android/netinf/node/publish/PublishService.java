package android.netinf.node.publish;

import android.netinf.common.NetInfService;
import android.netinf.common.NetInfStatus;

public interface PublishService extends NetInfService {

    public NetInfStatus publish(Publish publish);

}
