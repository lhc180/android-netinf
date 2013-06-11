package android.netinf.messages;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.netinf.common.NetInfUtils;
import android.netinf.node.api.Api;


public class Search extends Request {

    public static class Builder {

        private String mId = NetInfUtils.newId();
        private Api mSource;
        private int mHopLimit = 2;
        private long mTimeout = 1000;
        private Set<String> mTokens = new HashSet<String>();

        public Builder(Search search) {
            mSource = search.getSource();
            mId = search.getId();
            mHopLimit = search.getHopLimit();
            mTimeout = search.getTimeout();
            mTokens = search.getTokens();
        }

        public Builder(Api api) {
            mSource = api;
        }

        public Builder id(String id) { mId = id; return this; }
        public Builder hoplimit(int hops) { mHopLimit = hops; return this; }
        public Builder consumeHop() { mHopLimit = Math.max(mHopLimit - 1, 0); return this; }
        public Builder timeout(long timeout) { mTimeout = timeout; return this; }
        public Builder token(String token) { mTokens.add(token); return this; }
        public Builder tokens(Set<String> tokens) { mTokens.addAll(tokens); return this; }

        public Search build() {
            return new Search(this);
        }

    }

    private final Set<String> mTokens;
    private final long mTimeout;

    private Search(Builder builder) {
        mId = builder.mId;
        mSource = builder.mSource;
        mHopLimit = builder.mHopLimit;
        mTokens = Collections.unmodifiableSet(builder.mTokens);
        mTimeout = builder.mTimeout;
    }

    public long getTimeout() {
        return mTimeout;
    }

    public Set<String> getTokens() {
        return mTokens;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(mTokens.toArray());
    }

}
