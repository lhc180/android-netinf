package android.netinf.node.search;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.netinf.node.api.Api;
import android.util.Log;

public class SearchController implements SearchService {

    public static final String TAG = "SearchController";

    private Map<Api, Set<SearchService>> mSearchServices;

    public SearchController() {
        mSearchServices = new HashMap<Api, Set<SearchService>>();
    }

    public void registerSearchService(Api source, SearchService destination) {
        if (!mSearchServices.containsKey(source)) {
            mSearchServices.put(source, new LinkedHashSet<SearchService>());
        }
        mSearchServices.get(source).add(destination);
    }

    @Override
    public SearchResponse perform(final Search search) {
        Log.v(TAG, "search()");

        Set<SearchService> searchServices = mSearchServices.get(search.getSource());
        final CountDownLatch pendingSearches = new CountDownLatch(searchServices.size());
        final SearchResponse.Builder builder = new SearchResponse.Builder(search);

        for (final SearchService searchService : searchServices) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SearchResponse response = searchService.perform(search);
                    builder.addResults(response.getResults());
                    pendingSearches.countDown();
                }
            }).start();
        }

        try {
            pendingSearches.await(search.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.i(TAG, "SEARCH interrupted");
        }

        Log.i(TAG, "SEARCH produced " + search.getResults().size() + " NDO(s)");

        return builder.build();

    }

}
