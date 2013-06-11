package android.netinf.messages;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;

public class GetResponse extends Response {

    private final Ndo mNdo;

    public GetResponse(Get get, NetInfStatus status, Ndo ndo) {
        super(get, status);
        mNdo = ndo;
    }

    public GetResponse(Get get, NetInfStatus status) {
        this(get, status, null);
    }

    public GetResponse from(Get get) {
        if (getStatus().isSuccess()) {
            return new GetResponse(get, getStatus(), getNdo());
        } else {
            return new GetResponse(get, getStatus());
        }
    }

    @Override
    public Get getRequest() {
        return (Get) super.getRequest();
    }

    public Ndo getNdo() {
        if (mNdo == null) {
            throw new IllegalStateException("getNdo() called on the response of a failed GET request");
        }
        return mNdo;
    }

}
