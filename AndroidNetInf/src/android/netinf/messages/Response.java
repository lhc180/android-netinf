package android.netinf.messages;

import android.netinf.common.NetInfStatus;


public abstract class Response {

    private Request mRequest;
    private NetInfStatus mStatus;

    protected Response(Request request, NetInfStatus status) {
        mRequest = request;
        mStatus = status;
    }

    public Request getRequest() {
        return mRequest;
    }

    public NetInfStatus getStatus() {
        return mStatus;
    }

}
