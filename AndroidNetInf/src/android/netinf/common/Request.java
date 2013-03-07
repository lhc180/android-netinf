package android.netinf.common;

import android.netinf.node.api.Api;

public abstract class Request {

    private Api mSource;
    private String mId;
    private int mHopLimit;

    protected Request(Api source, String id, int hopLimit) {
        mSource = source;
        mId = id;
        mHopLimit = hopLimit;
    }

    public Api getSource() {
        return mSource;
    }

    public String getId() {
        return mId;
    }

    public int getHopLimit() {
        return mHopLimit;
    }

}
