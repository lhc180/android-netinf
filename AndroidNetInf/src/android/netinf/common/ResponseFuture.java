package android.netinf.common;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

public class ResponseFuture<V> implements Future<V> {

    public static final String TAG = ResponseFuture.class.getSimpleName();

    private SettableFuture<V> mFuture = SettableFuture.create();
    private boolean mMoreTime = true;

    private ResponseFuture() { }

    public static <V> ResponseFuture<V> create() {
        return new ResponseFuture<V>();
    }

    public void resetTimeout() {
        mMoreTime = true;
    }

    public boolean set(V value) {
        return mFuture.set(value);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {

        while (mMoreTime) {
            mMoreTime = false;
            try {
                return mFuture.get(timeout, unit);
            } catch (TimeoutException e) {
                Log.w(TAG, "Timeout extended");
            }
        }

        throw new TimeoutException("Timeout waiting for task.");

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    @Override
    public boolean isCancelled() {
        return mFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mFuture.isDone();
    }

}
