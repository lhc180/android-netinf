package android.netinf.node.logging;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.util.Log;

public class LogCatLogger implements LogService {

    public static final String TAG = LogCatLogger.class.getSimpleName();

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void log(LogEntry logEntry, Publish publish) {
        log(logEntry, (Object) publish);
    }

    @Override
    public void log(LogEntry logEntry, PublishResponse publishResponse) {
        log(logEntry, (Object) publishResponse);
    }

    @Override
    public void log(LogEntry logEntry, Get get) {
        log(logEntry, (Object) get);
    }

    @Override
    public void log(LogEntry logEntry, GetResponse getResponse) {
        log(logEntry, (Object) getResponse);
    }

    @Override
    public void log(LogEntry logEntry, Search search) {
        log(logEntry, (Object) search);
    }

    @Override
    public void log(LogEntry logEntry, SearchResponse searchResponse) {
        log(logEntry, (Object) searchResponse);
    }

    private void log(LogEntry logEntry, Object object) {
        Log.d(TAG, getDirection(logEntry) + " " + logEntry.getService() + " " + object.toString());
    }

    private String getDirection(LogEntry logEntry) {
        if (logEntry.isIncoming()) {
            return "INCOMING";
        } else {
            return "OUTGOING";
        }
    }

}
