package android.netinf.node.search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.netinf.common.Ndo;


public class Search {

    private Set<Ndo> mResults;
    private CountDownLatch mRemaining;

    public Search(SearchController searchController) {
        mResults = new HashSet<Ndo>();
        mRemaining = new CountDownLatch(searchController.searchServiceCount());
    }

    public void await(long timeout) {
        try {
            mRemaining.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
    }

    public synchronized void submitResults(Set<Ndo> results) {
        mResults.addAll(results);
        mRemaining.countDown();
    }

    public Set<Ndo> getResults() {
        return mResults;
    }

}
