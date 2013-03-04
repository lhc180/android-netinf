package android.netinf.node.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.common.Request;
import android.netinf.node.Node;
import android.netinf.node.api.Api;


public class Search extends Request<SearchResponse> {

    public static class Builder {

        private Api mSource;
        private String mId;
        private long mTimeout = 1000;
        private Set<String> mTokens = new HashSet<String>();
        private Set<Ndo> mResults = new HashSet<Ndo>();

        public Builder(Api api, String id) {
            mSource = api;
            mId = id;
        }

        public Builder timeout(long timeout) { mTimeout = timeout; return this; }
        public Builder token(String token) { mTokens.add(token); return this; }
        public Builder tokens(Set<String> tokens) { mTokens.addAll(tokens); return this; }

        public Search build() {
            return new Search(this);
        }

    }

    private Set<String> mTokens;
    private long mTimeout;
    private Set<Ndo> mResults;

    private Search(Builder builder) {
        super(builder.mSource, builder.mId);
        mTokens = Collections.unmodifiableSet(builder.mTokens);
        mTimeout = builder.mTimeout;
        mResults = builder.mResults;
    }

    public long getTimeout() {
        return mTimeout;
    }

    public Set<String> getTokens() {
        return mTokens;
    }

    public Set<Ndo> getResults() {
        return mResults;
    }

    @Override
    public SearchResponse call() throws Exception {
        return Node.getInstance().perform(this);
    }

    @Override
    public String toString() {
        return Arrays.deepToString(mTokens.toArray());
    }

}
