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
    private Map<Api, Set<SearchService>> mLocalSearchServices;

    public SearchController() {
        mSearchServices = new HashMap<Api, Set<SearchService>>();
        mLocalSearchServices = new HashMap<Api, Set<SearchService>>();
    }

    public void addSearchService(Api source, SearchService destination) {
        if (!mSearchServices.containsKey(source)) {
            mSearchServices.put(source, new LinkedHashSet<SearchService>());
        }
        mSearchServices.get(source).add(destination);
    }

    public void addLocalSearchService(Api source, SearchService destination) {
        if (!mLocalSearchServices.containsKey(source)) {
            mLocalSearchServices.put(source, new LinkedHashSet<SearchService>());
        }
        mLocalSearchServices.get(source).add(destination);
    }

    @Override
    public SearchResponse perform(Search incomingSearch) {
        Log.v(TAG, "search()");

        // Reduce hop limit
        final Search search = new Search.Builder(incomingSearch).consumeHop().build();

        // Get search services to be used
        Set<SearchService> searchServices = new LinkedHashSet<SearchService>(mLocalSearchServices.get(search.getSource()));
        if (search.getHopLimit() > 0) {
            searchServices.addAll(mSearchServices.get(search.getSource()));
        }

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
