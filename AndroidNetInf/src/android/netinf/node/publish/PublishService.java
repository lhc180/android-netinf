package android.netinf.node.publish;

import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;

public interface PublishService {

    public PublishResponse perform(Publish publish);

}
