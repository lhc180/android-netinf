package android.netinf.node.get;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.netinf.messages.Request;
import android.netinf.messages.Response;

import com.google.common.util.concurrent.SettableFuture;

public class InProgressTracker<K extends Request, V extends Response> {

    Map<String, SettableFuture<V>> mInProgress = new HashMap<String, SettableFuture<V>>();

    /**
     * Tries to start a request.
     * @param get
     *     The Get to start
     * @return
     *     A Future representing the result of the request if the request has a new id,
     *     otherwise null
     */
    public synchronized SettableFuture<V> newFutureOrNull(K request) {

        if (mInProgress.containsKey(request.getId())) {
            return null;
        } else {
            SettableFuture<V> future = SettableFuture.create();
            mInProgress.put(request.getId(), future);
            return future;
        }

    }

    public synchronized SettableFuture<V> newFutureOrInProgress(K request) {

        if (mInProgress.containsKey(request.getId())) {
            return mInProgress.get(request.getId());
        } else {
            SettableFuture<V> future = SettableFuture.create();
            mInProgress.put(request.getId(), future);
            return future;
        }

    }

    /**
     * Tries to stop a number of Gets and return their Futures.
     * @param gets
     *     The Gets to stop
     * @return
     *     A map with the subset of Gets that were actually stopped as keys
     *     and their corresponding Futures as values. Could be empty.
     */
    public synchronized Map<K, SettableFuture<V>> stopFutures(Set<K> requests) {

        Map<K, SettableFuture<V>> futures = new HashMap<K, SettableFuture<V>>();
        for (K request : requests) {
            SettableFuture<V> future = mInProgress.remove(request.getId());
            if (future != null) {
                futures.put(request, future);
            }
        }

        return futures;
    }

    /**
     * Tries to stop a Get waiting for a given Response and return its Future.
     * @param response
     *     The Response of the waiting Get
     * @return
     *     The Future of the waiting Get if it exists, otherwise null
     */
    public synchronized SettableFuture<V> stopFuture(V response) {
        return mInProgress.remove(response.getId());
    }

//    public synchronized ResponseFuture<V> getFutureOrNull(V response) {
//        return mInProgress.get(response.getId());
//    }













//    package android.netinf.node.get;
//
//    import java.util.HashMap;
//    import java.util.Map;
//    import java.util.Set;
//    import java.util.concurrent.Future;
//
//    import android.netinf.common.NetInfStatus;
//    import android.netinf.messages.Get;
//    import android.netinf.messages.GetResponse;
//
//    import com.google.common.util.concurrent.SettableFuture;
//
//    public class InProgressTracker {
//
//        Map<String, SettableFuture<GetResponse>> mInProgress = new HashMap<String, SettableFuture<GetResponse>>();
//
//        public synchronized Future<GetResponse> start(Get get) {
//
//            SettableFuture<GetResponse> future = SettableFuture.create();
//
//            if (mInProgress.containsKey(get.getId())) {
//                future.set(new GetResponse(get, NetInfStatus.FAILED));
//            } else {
//                mInProgress.put(get.getId(), future);
//            }
//
//            return future;
//
//        }
//
//        /**
//         * Tries to stop a number of Gets and return their Futures.
//         * @param gets
//         *     The Gets to stop
//         * @return
//         *     A map with the subset of Gets that were actually stopped as keys
//         *     and their corresponding Futures as values. Could be empty.
//         */
//        public synchronized Map<Get, SettableFuture<GetResponse>> stop(Set<Get> gets) {
//
//            Map<Get, SettableFuture<GetResponse>> futures = new HashMap<Get, SettableFuture<GetResponse>>();
//            for (Get get : gets) {
//                SettableFuture<GetResponse> future = mInProgress.remove(get.getId());
//                if (future != null) {
//                    futures.put(get, future);
//                }
//            }
//
//            return futures;
//        }















//  /**
//  * Stops a Get and returns the corresponding Future corresponding to its id.
//  * @param get
//  *     The Get to stop
//  * @return
//  *     The Future corresponding to the Get if it is in progress, otherwise null
//  */
// public synchronized SettableFuture<GetResponse> stop(Get get) {
//     return mInProgress.remove(get.getId());
// }

//    Set<String> mInProgress = new HashSet<String>();
//
//    public synchronized boolean tryToStart(Get get) {
//        if (mInProgress.contains(get.getId())) {
//            return false;
//        } else {
//            mInProgress.add(get.getId());
//            return true;
//        }
//    }
//
//    public synchronized void stop(Get get) {
//        mInProgress.remove(get.getId());
//    }

}
