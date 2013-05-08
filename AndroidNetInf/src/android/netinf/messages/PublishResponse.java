package android.netinf.messages;

import android.netinf.common.NetInfStatus;

public class PublishResponse extends Response {

    public PublishResponse(Publish publish, NetInfStatus status) {
        super(publish, status);
    }

    @Override
    public Publish getRequest() {
        return (Publish) super.getRequest();
    }

}
