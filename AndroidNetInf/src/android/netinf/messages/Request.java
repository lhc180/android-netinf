package android.netinf.messages;

import android.netinf.node.api.Api;

public abstract class Request extends Message {

    private Api mSource;
    private int mHopLimit;

    protected Request(Api source, String id, int hopLimit) {
        super(id);
        mSource = source;
        mHopLimit = hopLimit;
    }

    public Api getSource() {
        return mSource;
    }

    public int getHopLimit() {
        return mHopLimit;
    }

}
