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
    public SearchResponse perform(Search search) {

        // Reduce hop limit (unless this was a local request)
        if (!search.isLocal()) {
            search = new Search.Builder(search).consumeHop().build();
        }
        final Search finalSearch = search;

        // Get search services to be used
        Log.i(TAG, "SEARCH " + finalSearch);
        Set<SearchService> searchServices = new LinkedHashSet<SearchService>(mLocalServices.get(finalSearch.getSource()));
        if (finalSearch.getHopLimit() > 0) {
            searchServices.addAll(mRemoteServices.get(finalSearch.getSource()));
        }

        final CountDownLatch pendingSearches = new CountDownLatch(searchServices.size());
        final SearchResponse.Builder searchResponseBuilder = new SearchResponse.Builder(finalSearch);

        for (final SearchService searchService : searchServices) {
            // TODO use an Executor instead
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SearchResponse response = searchService.perform(finalSearch);
                    searchResponseBuilder.addResults(response.getResults());
                    pendingSearches.countDown();
                }
            }).start();
        }

        try {
            pendingSearches.await(finalSearch.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.i(TAG, "SEARCH interrupted");
        }

        SearchResponse searchResponse = searchResponseBuilder.build();
        Log.i(TAG, "SEARCH " + finalSearch + "\n-> " + searchResponse);
        return searchResponse;

    }

}
