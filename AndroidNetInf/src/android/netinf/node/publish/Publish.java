package android.netinf.node.publish;

import android.netinf.common.Ndo;
import android.netinf.common.Request;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Publish extends Request<PublishResponse> {

    public static class Builder {

        private Api mSource;
        private String mId;
        private Ndo mNdo;
        private boolean mFullPut = false;

        public Builder(Api api, String id, Ndo ndo) {
            if (ndo == null) {
                throw new NullPointerException("ndo must not be null");
            }
            mSource = api;
            mId = id;
            mNdo = ndo;
        }

        public Builder fullPut() { mFullPut = true; return this; }

        public Publish build() {
            return new Publish(this);
        }

    }

    private Ndo mNdo;
    private boolean mFullPut;

    private Publish(Builder builder) {
        super(builder.mSource, builder.mId);
        mNdo = builder.mNdo;
        mFullPut = builder.mFullPut;
    }

    public Ndo getNdo() {
        return mNdo;
    }

    public boolean isFullPut() {
        return mFullPut;
    }

    @Override
    public PublishResponse call() {
        return Node.getInstance().perform(this);
    }

    @Override
    public String toString() {
        return mNdo.getUri();
    }

}
