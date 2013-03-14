package android.netinf.node.publish;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfUtils;
import android.netinf.common.Request;
import android.netinf.node.api.Api;

public class Publish extends Request {

    public static class Builder {

        private Api mSource;
        private String mId = NetInfUtils.newId();
        private int mHopLimit = 0;
        private Ndo mNdo;
        private boolean mFullPut = false;

        public Builder(Publish publish) {
            mSource = publish.getSource();
            mId = publish.getId();
            mHopLimit = publish.getHopLimit();
            mNdo = publish.getNdo();
            mFullPut = publish.isFullPut();
        }

        public Builder(Ndo ndo) {
            this(Api.JAVA, ndo);
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
        public Builder fullPut() { mFullPut = true; return this; }

        public Publish build() {
            return new Publish(this);
        }

    }

    private final Ndo mNdo;
    private final boolean mFullPut;

    private Publish(Builder builder) {
        super(builder.mSource, builder.mId, builder.mHopLimit);
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(mNdo.getUri());
        if (isFullPut()) {
            builder.append(" + ");
            builder.append(mNdo.getOctets().length());
            builder.append(" bytes");
        }
        return builder.toString();
    }

}
