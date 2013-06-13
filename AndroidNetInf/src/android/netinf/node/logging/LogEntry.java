package android.netinf.node.logging;

public class LogEntry {

    private String mService;
    private boolean mIncoming;

    private LogEntry(String service, boolean incoming) {
        mService = service;
        mIncoming = incoming;
    }

    public static LogEntry newIncoming(String service) {
        return new LogEntry(service, true);
    }

    public static LogEntry newOutgoing(String service) {
        return new LogEntry(service, false);
    }

    public String getService() {
        return mService;
    }

    public boolean isIncoming() {
        return mIncoming;
    }

}
