package android.netinf.node.logging;

import java.util.List;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;

public class LogController implements LogService {

    public static final String TAG = LogController.class.getSimpleName();

    private List<LogService> mLogServices;

    public LogController(List<LogService> logServices) {
        mLogServices = logServices;
    }

    @Override
    public void start() {
        for (LogService logService : mLogServices) {
            logService.start();
        }
    }

    @Override
    public void log(LogEntry logEntry, Publish publish) {
        for (LogService logService : mLogServices) {
            logService.log(logEntry, publish);
        }
    }

    @Override
    public void log(LogEntry logEntry, PublishResponse publishResponse) {
        for (LogService logService : mLogServices) {
            logService.log(logEntry, publishResponse);
        }
    }

    @Override
    public void log(LogEntry logEntry, Get get) {
        for (LogService logService : mLogServices) {
            logService.log(logEntry, get);
        }
    }

    @Override
    public void log(LogEntry logEntry, GetResponse getResponse) {
        for (LogService logService : mLogServices) {
            logService.log(logEntry, getResponse);
        }
    }

    @Override
    public void log(LogEntry logEntry, Search search) {
        for (LogService logService : mLogServices) {
            logService.log(logEntry, search);
        }
    }

    @Override
    public void log(LogEntry logEntry, SearchResponse searchResponse) {
        for (LogService logService : mLogServices) {
            logService.log(logEntry, searchResponse);
        }
    }

}
