package android.netinf.messages;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;

public class SearchResponse extends Response {

    public static class Builder {

        private String mId;
        private NetInfStatus mStatus = NetInfStatus.OK;
        private Set<Ndo> mResults = Collections.synchronizedSet(new HashSet<Ndo>());

        public Builder(String id) { mId = id; }
        public Builder(Search search) { mId = search.getId(); }

        public Builder addResult(Ndo result) { mResults.add(result); return this; }
        public Builder addResults(Set<Ndo> results) { mResults.addAll(results); return this; }

        public SearchResponse build() {
            return new SearchResponse(this);
        }

    }

    private final Set<Ndo> mResults;

    private SearchResponse(Builder builder) {
        mId = builder.mId;
        mStatus = builder.mStatus;
        mResults = Collections.unmodifiableSet(builder.mResults);
    }

    public Set<Ndo> getResults() {
        return mResults;
    }

    @Override
    public String toString() {
        return "{id=" + StringUtils.left(mId, 3) + "…, status=" + mStatus + ", results=" + mResults + "}";
    }

}
