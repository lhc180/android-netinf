package android.netinf.node.get;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Get {

    // Request
    private Api mSource;
    private String mId;
    private Ndo mNdo;
//    private Date mReceived;

    // Result
//    private NetInfStatus mStatus;


    public Get(Api source, String id, Ndo ndo) {
        mSource = source;
        mId = id;
        mNdo = ndo;
    }

    public Api getSource() {
        return mSource;
    }

    public String getId() {
        return mId;
    }

    public Ndo getNdo() {
        return mNdo;
    }

    public Ndo execute() {
        return Node.getInstance().get(this);
    }

    @Override
    public String toString() {
        return mNdo.getUri();
    }

}
