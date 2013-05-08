package android.netinf.node.logging;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;

public interface LogService {

    // TODO this is not good design, refactor
    public void log(LogEntry logEntry, Publish publish);
    public void log(LogEntry logEntry, PublishResponse publishResponse);
    public void log(LogEntry logEntry, Get get);
    public void log(LogEntry logEntry, GetResponse getResponse);
    public void log(LogEntry logEntry, Search search);
    public void log(LogEntry logEntry, SearchResponse searchResponse);

}
