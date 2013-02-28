package android.netinf.node.search;

import java.util.LinkedHashSet;
import java.util.Set;

import android.netinf.common.Ndo;
import android.util.Log;

public class SearchController {

    public static final String TAG = "SearchController";

    private Set<SearchService> mSearchServices = new LinkedHashSet<SearchService>();

    public void addSearchService(SearchService searchService) {
        mSearchServices.add(searchService);
    }

    public int searchServiceCount() {
        return mSearchServices.size();
    }

    public Set<Ndo> search(final Set<String> tokens, long timeout) {

        final Search search = new Search(this);

        for (final SearchService searchService : mSearchServices) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    searchService.search(search, tokens);
                }
            }).start();
        }

        search.await(timeout);

        Log.i(TAG, "SEARCH produced " + search.getResults().size() + " NDO(s)");

        return search.getResults();

    }

}
