package android.netinf.common;

public abstract class Response {

    private String mId;
    private NetInfStatus mStatus;

    protected Response(String id, NetInfStatus status) {
        mId = id;
        mStatus = status;
    }

    public NetInfStatus getStatus() {
        return mStatus;
    }

    public String getId() {
        return mId;
    }

}
