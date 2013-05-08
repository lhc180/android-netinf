package android.netinf.node.logging;

public class LogEntry {

    private String mService;
    private boolean mIncoming;

    public LogEntry(String service, boolean incoming) {
        mService = service;
        mIncoming = incoming;
    }

    public String getService() {
        return mService;
    }

    public boolean isIncoming() {
        return mIncoming;
    }

}
