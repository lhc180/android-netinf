package android.netinf.messages;

import android.netinf.common.NetInfStatus;


public abstract class Response extends Message {

    protected NetInfStatus mStatus;

    public NetInfStatus getStatus() {
        return mStatus;
    }

}
