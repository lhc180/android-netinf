package android.netinf.messages;

import org.apache.commons.lang3.StringUtils;

import android.netinf.common.NetInfStatus;

public class PublishResponse extends Response {

    public static class Builder {

        private String mId;
        private NetInfStatus mStatus = NetInfStatus.FAILED;

        public Builder(String id) { mId = id; }
        public Builder(Publish publish) { mId = publish.getId(); }

        public Builder ok() { mStatus = NetInfStatus.OK; return this; }
        public Builder failed() { mStatus = NetInfStatus.FAILED; return this; }
        public Builder status(NetInfStatus status) { mStatus = status; return this; }
        public Builder id(String id)  { mId = id; return this; }

        public PublishResponse build() {
            return new PublishResponse(this);
        }

    }

    private PublishResponse(Builder builder) {
        mId = builder.mId;
        mStatus = builder.mStatus;
    }

    @Override
    public String toString() {
        return "{id=" + StringUtils.left(mId, 3) + "…, status=" + mStatus + "}";
    }

}
