package android.netinf.node.search;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.api.Api;
import android.util.Log;

import com.google.common.collect.SetMultimap;

public class SearchController implements SearchService {

    public static final String TAG = SearchController.class.getSimpleName();

    private SetMultimap<Api, SearchService> mLocalServices;
    private SetMultimap<Api, SearchService> mRemoteServices;

    public SearchController(SetMultimap<Api, SearchService> local, SetMultimap<Api, SearchService> remote) {
        mLocalServices = local;
        mRemoteServices = remote;
    }

    @Override
    public SearchResponse perform(Search incomingSearch) {

        // Reduce hop limit
        final Search search = new Search.Builder(incomingSearch).consumeHop().build();

        // Get search services to be used
        Log.i(TAG, "Local SEARCH of " + search);
        Set<SearchService> searchServices = new LinkedHashSet<SearchService>(mLocalServices.get(search.getSource()));
        if (search.getHopLimit() > 0) {
            Log.i(TAG, "Remote SEARCH of " + search);
            searchServices.addAll(mRemoteServices.get(search.getSource()));
        }

        final CountDownLatch pendingSearches = new CountDownLatch(searchServices.size());
        final SearchResponse.Builder searchResponseBuilder = new SearchResponse.Builder(search);

        for (final SearchService searchService : searchServices) {
            // TODO use an Executor instead
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SearchResponse response = searchService.perform(search);
                    searchResponseBuilder.addResults(response.getResults());
                    pendingSearches.countDown();
                }
            }).start();
        }

        try {
            pendingSearches.await(search.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.i(TAG, "SEARCH interrupted");
        }

        SearchResponse response = searchResponseBuilder.build();
        Log.i(TAG, "SEARCH produced " + response.getResults().size() + " NDO(s)");
        return response;

    }

}
