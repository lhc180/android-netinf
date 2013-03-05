package android.netinf.node.get;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfUtils;
import android.netinf.common.Request;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Get extends Request<GetResponse> {

    public static class Builder {

        private Api mSource;
        private String mId = NetInfUtils.newId();
        private int mHopLimit = 2;
        private Ndo mNdo;

        public Builder(Get get) {
            mSource = get.getSource();
            mId = get.getId();
            mHopLimit = get.getHopLimit();
            mNdo = get.getNdo();
        }

        public Builder(Api api, Ndo ndo) {
            if (ndo == null) {
                throw new NullPointerException("ndo must not be null");
            }
            mSource = api;
            mNdo = ndo;
        }

        public Builder id(String id) { mId = id; return this; }
        public Builder hoplimit(int hops) { mHopLimit = hops; return this; }
        public Builder consumeHop() { mHopLimit = Math.max(mHopLimit - 1, 0); return this; }

        public Get build() {
            return new Get(this);
        }

    }

    private final Ndo mNdo;

    private Get(Builder builder) {
        super(builder.mSource, builder.mId, builder.mHopLimit);
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
