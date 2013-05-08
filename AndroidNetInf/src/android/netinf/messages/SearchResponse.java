package android.netinf.messages;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;

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

    private final Set<Ndo> mResults;

    private SearchResponse(Builder builder) {
        super(builder.mSearch, builder.mStatus);
        mResults = Collections.unmodifiableSet(builder.mResults);
    }

    @Override
    public Search getRequest() {
        return (Search) super.getRequest();
    }

    public Set<Ndo> getResults() {
        return mResults;
    }

}
