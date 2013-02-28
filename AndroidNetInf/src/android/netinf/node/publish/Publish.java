package android.netinf.node.publish;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.node.Node;

public class Publish {

    // Request
    private Ndo mNdo;
    private byte[] mOctets;
    private boolean mLocal;
//    private Date mReceived;

    // Result
//    private NetInfStatus mStatus;


    public Publish(Ndo ndo) {
        mNdo = ndo;
        mOctets = null;
        mLocal = false;
    }

    public Ndo getNdo() {
        return mNdo;
    }

    public void setOctets(File file) throws IOException {
        mOctets = FileUtils.readFileToByteArray(file);
    }

    public void setOctets(byte[] octets) {
        mOctets = octets;
    }

    public boolean isFullPut() {
        return mOctets != null;
    }

    public void setLocal(boolean local) {
        mLocal = local;
    }

    public boolean isLocal() {
        return mLocal;
    }

    public NetInfStatus execute() {
        return Node.getInstance().publish(this);
    }

}
