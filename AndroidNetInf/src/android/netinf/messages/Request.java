package android.netinf.messages;

import android.netinf.node.api.Api;

public abstract class Request extends Message {

    protected Api mSource;
    protected int mHopLimit;

    public Api getSource() {
        return mSource;
    }

    public int getHopLimit() {
        return mHopLimit;
    }

    public boolean isLocal() {
        return mSource.equals(Api.JAVA);
    }

}
