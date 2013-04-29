package android.netinf.node.get;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.netinf.common.Ndo;

public class RequestAggregator {

    private Map<Ndo, Set<Get>> mPendingGets;

    public RequestAggregator() {
        mPendingGets = new HashMap<Ndo, Set<Get>>();
    }

    /**
     * Aggregate a requests if possible.
     * @param get
     *     The Get request to check if it can be aggregated
     * @return
     *     true if the request was aggregated, otherwise false
     */
    public synchronized boolean aggregate(Get get) {
        boolean aggregated = mPendingGets.containsKey(get.getNdo());
        if (aggregated) {
            // We should aggregate, add to set of aggregated requests
            mPendingGets.get(get.getNdo()).add(get);
        } else {
            // We should not aggregate, create new empty set of aggregated requests
            mPendingGets.put(get.getNdo(), new HashSet<Get>());
        }
        return aggregated;
    }

    /**
     * Signal that a Get is no longer waiting for response.
     * @param get
     *     The Get no longer waiting for response
     */
    public synchronized void done(Get get) {
        // This could happen after the "leader" called notify on an aggregation
        // If so the Ndo key has been removed, i.e. do safety check
        if (mPendingGets.containsKey(get.getNdo())) {
            mPendingGets.get(get.getNdo()).remove(get);
        }
    }

    /**
     * Notify all Gets for the same Ndo of the response.
     * @param get
     *     The completed Get
     * @param response
     *     The response to the completed Get
     */
    public synchronized void notify(Get get, GetResponse response) {
        // The "leader" of the aggregation got his result
        // Notify others an remove the aggregation
        for (Get aggregatedGet : mPendingGets.get(get.getNdo())) {
            aggregatedGet.submitAggregatedResponse(response);
        }
        mPendingGets.remove(get.getNdo());
    }



}
