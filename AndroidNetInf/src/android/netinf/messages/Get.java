package android.netinf.messages;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfUtils;
import android.netinf.node.Node;
import android.netinf.node.api.Api;

public class Get extends Request {

    public static class Builder {

        private String mId = NetInfUtils.newId();
        private Api mSource = Api.JAVA;
        private int mHopLimit = 2;
        private Ndo mNdo;
//        private SettableFuture<GetResponse> mFutureResponse = SettableFuture.create();

        public Builder(Get get) {
            mId = get.mId;
            mSource = get.mSource;
            mHopLimit = get.mHopLimit;
            mNdo = get.mNdo;
//            mFutureResponse = get.mFutureResponse;
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
        public Builder source(Api source) { mSource = source; return this; }
        public Builder hoplimit(int hops) { mHopLimit = hops; return this; }
        public Builder consumeHop() { mHopLimit = Math.max(mHopLimit - 1, 0); return this; }

        public Get build() {
            return new Get(this);
        }

    }

    public static final String TAG = Get.class.getSimpleName();

    private final Ndo mNdo;
//    private final SettableFuture<GetResponse> mFutureResponse;;

    private Get(Builder builder) {
        mId = builder.mId;
        mSource = builder.mSource;
        mHopLimit = builder.mHopLimit;
        mNdo = builder.mNdo;
//        mFutureResponse = builder.mFutureResponse;
    }

    public Ndo getNdo() {
        return mNdo;
    }

    public GetResponse getResponse(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return Node.submit(this).get(timeout, unit);
    }

//
//    public Future<GetResponse> getFutureResponse() {
//        return mFutureResponse;
//    }

    @Override
    public String toString() {
        return "{id = " + mId + ", ndo = " + mNdo.getUri() + "}";
    }

}
