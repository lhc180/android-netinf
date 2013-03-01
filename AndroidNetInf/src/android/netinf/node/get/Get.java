package android.netinf.node.get;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Get {

    // Request
    private Ndo mNdo;
    private Api mSource;
//    private Date mReceived;

    // Result
//    private NetInfStatus mStatus;


    public Get(Api source, Ndo ndo) {
        mSource = source;
        mNdo = ndo;
    }

    public Api getSource() {
        return mSource;
    }

    public Ndo getNdo() {
        return mNdo;
    }

    public Ndo execute() {
        return Node.getInstance().get(this);
    }


}
