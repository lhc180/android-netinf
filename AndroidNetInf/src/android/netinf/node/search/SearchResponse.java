package android.netinf.node.search;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.common.Response;

public class SearchResponse extends Response {

    public static class Builder {

        private Search mSearch;
        private NetInfStatus mStatus = NetInfStatus.OK;
        private Set<Ndo> mResults = Collections.synchronizedSet(new HashSet<Ndo>());

        public Builder(Search search) {
            mSearch = search;
        }

        public Builder addResult(Ndo result) { mResults.add(result); return this; }
        public Builder addResults(Set<Ndo> results) { mResults.addAll(results); return this; }

        public SearchResponse build() {
            return new SearchResponse(this);
        }

    }

    private Set<Ndo> mResults;

    private SearchResponse(Builder builder) {
        super(builder.mSearch.getId(), builder.mStatus);
        mResults = builder.mResults;
    }

    public Set<Ndo> getResults() {
        return mResults;
    }

}
