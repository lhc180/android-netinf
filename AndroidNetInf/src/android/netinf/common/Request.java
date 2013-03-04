package android.netinf.common;

import java.util.concurrent.Callable;

import android.netinf.node.api.Api;

public abstract class Request<T> implements Callable<T> {

    private Api mSource;
    private String mId;

    protected Request(Api source, String id) {
        mSource = source;
        mId = id;
    }

    public Api getSource() {
        return mSource;
    }

    public String getId() {
        return mId;
    }

}
