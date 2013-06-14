package android.netinf.node.get;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.node.Node;
import android.netinf.node.api.Api;
import android.netinf.node.logging.LogEntry;
import android.util.Log;

import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.SettableFuture;

public class GetController {

    public static final String TAG = GetController.class.getSimpleName();

    public static final int TIMEOUT = 5000;

    private SetMultimap<Api, GetService> mLocalServices;
    private SetMultimap<Api, GetService> mRemoteServices;

    private ExecutorService mGetExecutor = Executors.newCachedThreadPool();
    private InProgressTracker<Get, GetResponse> mInProgressTracker = new InProgressTracker<Get, GetResponse>();
    private RequestAggregator mRequestAggregator = new RequestAggregator();


    public GetController(SetMultimap<Api, GetService> local, SetMultimap<Api, GetService> remote) {
        mLocalServices = local;
        mRemoteServices = remote;
    }

    public Future<GetResponse> submit(final Get get) {

        Log.i(TAG, "NEW GET " + get);
        Node.log(LogEntry.newIncoming("UNKNOWN"), get);

        SettableFuture<GetResponse> future = mInProgressTracker.newFutureOrNull(get);

        if (future == null) {

            // Request is in progress
            Log.d(TAG, "GET " + get + " is already in progress");
            future = SettableFuture.create();
            future.set(new GetResponse.Builder(get).failed().build());

        } else if (!mRequestAggregator.aggregate(get)) {

            // Request was not aggregates
            Log.d(TAG, "GET " + get + " was NOT aggregated");
            mGetExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    handle(get);
                }
            });

        } else {
            Log.d(TAG, "GET " + get + " was aggregated");
        }

        return future;

    }

    private void handle(Get get) {

        // Reduce hop limit (unless this was a local request)
        if (get.getSource() != Api.JAVA) {
            get = new Get.Builder(get).consumeHop().build();
        }

        // Assume it will fail
        GetResponse getResponse = new GetResponse.Builder(get).failed().build();

        // First check local services
        for (GetService getService : mLocalServices.get(get.getSource())) {
            getResponse = getService.perform(get);
            if (getResponse.getStatus().isSuccess()) {
                break;
            }
        }

        // Then check other services as needed
        if (getResponse.getStatus().isError() && get.getHopLimit() > 0) {
            for (GetService getService : mRemoteServices.get(get.getSource())) {
                getResponse = getService.perform(get);
                if (getResponse.getStatus().isSuccess()) {
                    break;
                }
            }
        }

        Node.log(LogEntry.newOutgoing("UNKNOWN"), getResponse);

        // Get aggregated requests
        Set<Get> gets = mRequestAggregator.deaggregate(get);

        // Add non-aggregated request
        gets.add(get);

        // Stop all the Gets and set their futures
        Map<Get, SettableFuture<GetResponse>> futures = mInProgressTracker.stopFutures(gets);
        for (Get aggregated : futures.keySet()) {
            SettableFuture<GetResponse> future = futures.get(aggregated);
            GetResponse aggregatedGetResponse = new GetResponse.Builder(getResponse).id(aggregated.getId()).build();
            Log.i(TAG, "GET " + get + "\n-> " + aggregatedGetResponse);
            future.set(aggregatedGetResponse);
        }

    }

//        // Check if the Get is already in progress to avoid network loops
//        boolean started = mInProgressTracker.tryToStart(get);
//        if (!started) {
//            return new GetResponse(get, NetInfStatus.FAILED);
//        }
//
//        // Try to aggregate the Get
//        boolean aggregated = mRequestAggregator.tryToAggregate(get);
//        if (aggregated) {
//
//            Log.i(TAG, "Aggregated GET of " + get);
//            GetResponse response = get.aggregate(TIMEOUT, TimeUnit.MILLISECONDS);
//            mInProgressTracker.stop(get);
//            mRequestAggregator.notWaitingAnymore(get);
//            Log.i(TAG, "Aggregated GET of " + get + " done. STATUS " + response.getStatus());
//            Node.log(LogEntry.newOutgoing("UNKNOWN"), response);
//            return response;
//
//        } else {
//
//            // Assume it will fail
//            GetResponse response = new GetResponse(get, NetInfStatus.FAILED);
//
//            // First check local services
//            Log.i(TAG, "Local GET of " + get);
//            for (GetService getService : mServices.getLocalServices(get.getSource())) {
//                response = getService.perform(get);
//                if (response.getStatus().isSuccess()) {
//                    break;
//                }
//            }
//
//            // Then check other services as needed
//            if (response.getStatus().isError() && get.getHopLimit() > 0) {
//                Log.i(TAG, "Remote GET of " + get);
//                for (GetService getService : mServices.getRemoteServices(get.getSource())) {
//                    response = getService.perform(get);
//                    if (response.getStatus().isSuccess()) {
//                        break;
//                    }
//                }
//            }
//
//            // The Get is no longer in progress
//            mInProgressTracker.stop(get);
//
//            // Notify all waiting aggregated requests
//            // synchronized so no new aggregated requests are added while responding
//            mRequestAggregator.notifyAggregated(get, response);
//
//            // Publish the received data locally
//            if (response.getStatus().isSuccess()) {
//                Publish publish = new Publish.Builder(response.getNdo()).build();
//                Node.submit(publish);
//            }
//
//            Log.i(TAG, "GET of " + get + " done. STATUS " + response.getStatus());
//            Node.log(LogEntry.newOutgoing("UNKNOWN"), response);
//            return response;
//
//        }
//
//    }

}
