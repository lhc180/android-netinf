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

    public void log(LogEntry logEntry, Object object) {
        Log.d(TAG, getDirection(logEntry) + " " + logEntry.getService() + " " + object.toString());
    }

    @Override
    public void log(LogEntry logEntry, Publish publish) {
        log(logEntry, publish);
    }

    @Override
    public void log(LogEntry logEntry, PublishResponse publishResponse) {
        log(logEntry, publishResponse);
    }

    @Override
    public void log(LogEntry logEntry, Get get) {
        log(logEntry, get);
    }

    @Override
    public void log(LogEntry logEntry, GetResponse getResponse) {
        log(logEntry, getResponse);
    }

    @Override
    public void log(LogEntry logEntry, Search search) {
        log(logEntry, search);
    }

    @Override
    public void log(LogEntry logEntry, SearchResponse searchResponse) {
        log(logEntry, searchResponse);
    }

    public String getDirection(LogEntry logEntry) {
        if (logEntry.isIncoming()) {
            return "INCOMING";
        } else {
            return "OUTGOING";
        }
    }

}
