package android.netinf.messages;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.common.NetInfUtils;
import android.netinf.node.api.Api;
import android.util.Log;

public class Get extends Request {

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

        public Builder(Ndo ndo) {
            this(Api.JAVA, ndo);
        }

        public Builder id(String id) { mId = id; return this; }
        public Builder hoplimit(int hops) { mHopLimit = hops; return this; }
        public Builder consumeHop() { mHopLimit = Math.max(mHopLimit - 1, 0); return this; }

        public Get build() {
            return new Get(this);
        }

    }

    public static final String TAG = Get.class.getSimpleName();

    private final Ndo mNdo;
    private CountDownLatch mDone;
    private GetResponse mAggregatedResponse;

    private Get(Builder builder) {
        super(builder.mSource, builder.mId, builder.mHopLimit);
        mNdo = builder.mNdo;
        mDone = new CountDownLatch(1);
    }

    public Ndo getNdo() {
        return mNdo;
    }

    public GetResponse aggregate(long timeout, TimeUnit unit) {
        try {
            if (mDone.await(timeout, unit)) {
                return mAggregatedResponse;
            }
        } catch (InterruptedException e) {
            Log.wtf(TAG, "Aggregated GET interrupted", e);
        }
        return new GetResponse(this, NetInfStatus.FAILED);
    }

    public void submitAggregatedResponse(GetResponse otherGetResponse) {
        mAggregatedResponse = new GetResponse(this, otherGetResponse.getStatus(), otherGetResponse.getNdo());
        mDone.countDown();
    }

    @Override
    public String toString() {
        return mNdo.getUri();
    }

}
