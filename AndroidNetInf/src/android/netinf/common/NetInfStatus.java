package android.netinf.common;

public class NetInfStatus {

    public static final NetInfStatus OK = new NetInfStatus(200);
    public static final NetInfStatus I_FAILED = new NetInfStatus(500);
    public static final NetInfStatus YOU_FAILED = new NetInfStatus(400);
    public static final NetInfStatus TIMEOUT = new NetInfStatus(504);
    public static final NetInfStatus DUPLICATE_REQUEST = new NetInfStatus(508);

    private int mCode;

    private NetInfStatus(int code) {
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }

    public boolean equals(NetInfStatus status) {
        return mCode == status.mCode;
    }

}
