package android.netinf.node.search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.api.Api;


public class Search {

    private Api mSource;
    private Set<String> mTokens;
    private long mTimeout;
    private Set<Ndo> mResults;
    private CountDownLatch mPending;

    public Search(Api source, Set<String> tokens, long timeout) {
        mSource = source;
        mTokens = tokens;
        mTimeout = timeout;
        mResults = new HashSet<Ndo>();
        mPending = new CountDownLatch(0);
    }

    public Api getSource() {
        return mSource;
    }

    public Set<String> getTokens() {
        return mTokens;
    }

    public Set<Ndo> getResults() {
        return mResults;
    }

    public void setPending(int searches) {
        mPending = new CountDownLatch(searches);
    }

    public void await() {
        try {
            mPending.await(mTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
    }

    public synchronized void submitResults(Set<Ndo> results) {
        mResults.addAll(results);
        mPending.countDown();
    }

    public Set<Ndo> execute() {
        return Node.getInstance().search(this);
    }

}
