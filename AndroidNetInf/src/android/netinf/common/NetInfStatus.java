package android.netinf.common;

public class NetInfStatus {

    public static final NetInfStatus OK = new NetInfStatus(200);
    public static final NetInfStatus FAILED = new NetInfStatus(404);

    private int mCode;

    private NetInfStatus(int code) {
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }

    public boolean equals(int code) {
        return code == mCode;
    }

    public boolean equals(NetInfStatus status) {
        return mCode == status.mCode;
    }

    public boolean isSuccess() {
        return equals(OK);
    }

    public boolean isError() {
        return equals(FAILED);
    }

    public static NetInfStatus valueOf(int code) {
        return new NetInfStatus(code);
    }

}
