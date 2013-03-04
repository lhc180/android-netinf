package android.netinf.node.get;

import android.netinf.common.Ndo;
import android.netinf.common.Request;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Get extends Request<GetResponse> {

    public static class Builder {

        private Api mSource;
        private String mId;
        private Ndo mNdo;

        public Builder(Api api, String id, Ndo ndo) {
            if (ndo == null) {
                throw new NullPointerException("ndo must not be null");
            }
            mSource = api;
            mId = id;
            mNdo = ndo;
        }

        public Get build() {
            return new Get(this);
        }

    }

    private Ndo mNdo;

    private Get(Builder builder) {
        super(builder.mSource, builder.mId);
        mNdo = builder.mNdo;
    }

    public Ndo getNdo() {
        return mNdo;
    }

    @Override
    public GetResponse call() {
        return Node.getInstance().perform(this);
    }

    @Override
    public String toString() {
        return mNdo.getUri();
    }

}
