package android.netinf.node.publish;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Publish {

    // Request
    private Api mSource;
    private String mId;
    private Ndo mNdo;
    private boolean mFullPut;
//    private Date mReceived;

    // Result
    private NetInfStatus mStatus;


    public Publish(Api source, String id, Ndo ndo) {
        if (ndo == null) {
            throw new NullPointerException("ndo must not be null");
        }
        mSource = source;
        mId = id;
        mNdo = ndo;
        mFullPut = false;
        mStatus = NetInfStatus.TIMEOUT;
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

    public void setFullPut(boolean isFullPut) {
        mFullPut = isFullPut;
    }

    public boolean isFullPut() {
        return mFullPut;
    }

    public void execute() {
        mStatus = Node.getInstance().publish(this);
    }

    public NetInfStatus getResult() {
        return mStatus;
    }

    @Override
    public String toString() {
        return mNdo.getUri();
    }

}
