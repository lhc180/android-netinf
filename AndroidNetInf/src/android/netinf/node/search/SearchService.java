package android.netinf.node.search;

import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;


public interface SearchService {

    public SearchResponse perform(Search search);

}
