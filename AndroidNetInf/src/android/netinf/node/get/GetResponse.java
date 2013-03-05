package android.netinf.node.get;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.common.Response;

public class GetResponse extends Response {

    private final Ndo mNdo;

    public GetResponse(Get get, NetInfStatus status, Ndo ndo) {
        super(get.getId(), status);
        mNdo = ndo;
    }

    public GetResponse(Get get, NetInfStatus status) {
        this(get, status, null);
    }

    public Ndo getNdo() {
        return mNdo;
    }

}
