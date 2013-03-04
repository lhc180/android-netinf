package android.netinf.node.search;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.node.api.Api;
import android.util.Log;

public class SearchController {

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

    public Set<Ndo> search(final Search search) {
        Log.v(TAG, "search()");

        search.setPending(mSearchServices.get(search.getSource()).size());

        for (final SearchService searchService : mSearchServices.get(search.getSource())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    searchService.search(search);
                }
            }).start();
        }

        search.await();

        Log.i(TAG, "SEARCH produced " + search.getResults().size() + " NDO(s)");

        return search.getResults();

    }

}
