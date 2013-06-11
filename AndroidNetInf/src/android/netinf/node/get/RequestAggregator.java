package android.netinf.node.get;

import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.messages.Get;

import com.google.common.collect.HashMultimap;

public class RequestAggregator {

    private HashMultimap<Ndo, Get> mPending = HashMultimap.create();

    /**
     * Aggregates a Get if possible.
     * @param get
     *     The Get to aggregate
     * @return
     *     true if the request was aggregated, otherwise false
     */
    public synchronized boolean aggregate(Get get) {

        boolean aggregated = mPending.containsKey(get.getNdo());
        mPending.put(get.getNdo(), get);
        return aggregated;

    }

    /**
     * Deaggregate all Gets waiting for the same Ndo.
     * @param get
     *     The Get that was finished
     * @return
     *     A Set of all Gets waiting for the same Ndo
     */
    public synchronized Set<Get> deaggregate(Get get) {

        return mPending.removeAll(get.getNdo());

    }

//    private Map<Ndo, Set<Get>> mPendingGets;
//
//    public RequestAggregator() {
//        mPendingGets = new HashMap<Ndo, Set<Get>>();
//    }
//
//    /**
//     * Try to aggregate a request.
//     * @param get
//     *     The Get request to aggregate
//     * @return
//     *     true if the request was aggregated, otherwise false
//     */
//    public synchronized boolean tryToAggregate(Get get) {
//        boolean aggregated = mPendingGets.containsKey(get.getNdo());
//        if (aggregated) {
//            // We should aggregate, add to set of aggregated requests
//            mPendingGets.get(get.getNdo()).add(get);
//        } else {
//            // We should not aggregate, create new empty set of aggregated requests
//            mPendingGets.put(get.getNdo(), new HashSet<Get>());
//        }
//        return aggregated;
//    }
//
//    /**
//     * Signal that a Get is no longer waiting for a response.
//     * @param get
//     *     The Get that is no longer waiting for a response
//     */
//    public synchronized void notWaitingAnymore(Get get) {
//        // This Get is for some reason (got its result, timed out, ...) not waiting for a response anymore
//        // This could happen after the "leader" called notify on an aggregation
//        // If so the Ndo key has been removed, i.e. do safety check
//        if (mPendingGets.containsKey(get.getNdo())) {
//            mPendingGets.get(get.getNdo()).remove(get);
//        }
//    }
//
//    /**
//     * Notify all aggregated Gets of the result.
//     * @param get
//     *     The completed Get
//     * @param response
//     *     The response to the completed Get to be used as result for the aggregated Gets
//     */
//    public synchronized void notifyAggregated(Get get, GetResponse response) {
//        // The "leader" of the aggregation got his result
//        // Notify others and remove the aggregation
//        for (Get aggregatedGet : mPendingGets.get(get.getNdo())) {
//            aggregatedGet.submitAggregatedResponse(response);
//        }
//        mPendingGets.remove(get.getNdo());
//    }



}
