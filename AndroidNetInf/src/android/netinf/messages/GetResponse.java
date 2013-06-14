package android.netinf.messages;

import org.apache.commons.lang3.StringUtils;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;

public class GetResponse extends Response {

    public static class Builder {

        private String mId;
        private NetInfStatus mStatus = NetInfStatus.FAILED;
        private Ndo mNdo;

        public Builder(String id) { mId = id; }
        public Builder(Get get) { mId = get.getId(); }
        public Builder(GetResponse getResponse) { mId = getResponse.mId; mStatus = getResponse.mStatus; mNdo = getResponse.mNdo; }

        public Builder ok(Ndo ndo) { mStatus = NetInfStatus.OK; mNdo = ndo; return this; }
        public Builder ok(Get get) { mId = get.getId(); mStatus = NetInfStatus.OK; mNdo = get.getNdo(); return this; }
        public Builder failed() { mStatus = NetInfStatus.FAILED; return this; }
        public Builder id(String id)  { mId = id; return this; }

        public GetResponse build() {
            return new GetResponse(this);
        }

    }

    private final Ndo mNdo;

    private GetResponse(Builder builder) {
        mId = builder.mId;
        mStatus = builder.mStatus;
        mNdo = builder.mNdo;
    }

    public Ndo getNdo() {
        if (mNdo == null) {
            throw new IllegalStateException("NDO is null, getNdo() called on a failed GET request?");
        }
        return mNdo;
    }

    @Override
    public String toString() {
        return "{id=" + StringUtils.left(mId, 3) + "…, status=" + mStatus + ", ndo=" + mNdo + "}";
    }

}
