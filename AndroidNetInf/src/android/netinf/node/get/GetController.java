package android.netinf.node.get;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.node.Node;
import android.netinf.node.SettingsActivity;
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

        GetResponse getResponse = perform(get);

        // Publish
        publish(getResponse);

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
            Node.log(LogEntry.newOutgoing("UNKNOWN"), getResponse);
            future.set(aggregatedGetResponse);
        }

    }

    private GetResponse perform(Get get) {

        // Reduce hop limit (unless this was a local request)
        if (!get.isLocal()) {
            get = new Get.Builder(get).consumeHop().build();
        }

        // GetService(s)
        Collection<GetService> local = mLocalServices.get(get.getSource());
        Collection<GetService> remote = mRemoteServices.get(get.getSource());

        // Keep track of resolved locators
        Set<Locator> resolved = new HashSet<Locator>();
        Set<Locator> next = new HashSet<Locator>();

        // Assume Get will fail
        GetResponse getResponse = new GetResponse.Builder(get).failed().build();

        // Check all local services and collect locators
        for (GetService getService : local) {
            getResponse = getService.perform(get);
            if (getResponse.getStatus().isSuccess()) {
                if (getResponse.getNdo().isCached()) {
                    // We got the data, done!
                    return getResponse;
                } else {
                    // We might have gotten locators
                    next.addAll(getResponse.getNdo().getLocators());
                }
            }
        }
        // Remember to add possible initial locators
        next.addAll(get.getNdo().getLocators());

        // Check all remote services and resolve locators as necessary
        if (get.getHopLimit() > 0) {
            for (GetService getService : remote) {

                // If this was a local request, first try to resolve all locators
                if (get.isLocal()) {
                    getResponse = resolveLocators(get, resolved, next);
                    if (getResponse.getStatus().isSuccess() && getResponse.getNdo().isCached()) {
                        return getResponse;
                    }
                }

                // Then try the default routing of the service
                getResponse = getService.perform(get);

                if (getResponse.getStatus().isSuccess()) {
                    if (getResponse.getNdo().isCached()) {
                        // We got the data, done!
                        return getResponse;
                    } else if (get.isLocal()) {
                        // We might have gotten locators
                        next.addAll(getResponse.getNdo().getLocators());
                        getResponse = resolveLocators(get, resolved, next);
                        if (getResponse.getStatus().isSuccess() && getResponse.getNdo().isCached()) {
                            return getResponse;
                        }
                    }
                }
            }
        }

        return getResponse;

    }

    private GetResponse resolveLocators(Get get, Set<Locator> resolved, Set<Locator> next) {

        // Assume Get will fail
        GetResponse getResponse = new GetResponse.Builder(get).failed().build();

        // Don't resolve twice
        next.removeAll(resolved);

        while (!next.isEmpty()) {

            // Create Ndo with all known unresolved Locators
            Ndo nextNdo = new Ndo.Builder(get.getNdo()).setLocators(next).build();
            Get nextGet = new Get.Builder(get).ndo(nextNdo).build();

            // Remember resolved Locators and removed resolved locators
            resolved.addAll(next);
            next.clear();

            // Resolve the Locators
            for (GetService getService : mRemoteServices.get(get.getSource())) {
                getResponse = getService.resolveLocators(nextGet);
                if (getResponse.getStatus().isSuccess()) {
                    if (getResponse.getNdo().isCached()) {
                        // We got the data, done!
                        return getResponse;
                    } else {
                        // We might have gotten locators
                        next.addAll(getResponse.getNdo().getLocators());
                    }
                }
            }

        }

        return getResponse;

    }

    private void publish(GetResponse getResponse) {

        if (getResponse.getStatus().isError() || !SettingsActivity.getPreferenceAsBoolean("pref_key_publish_after_get")) {
            return;
        }

        Ndo ndo = getResponse.getNdo();
        if (SettingsActivity.getPreferenceAsBoolean("pref_key_include_bluetooth")) {
            ndo = new Ndo.Builder(ndo).addLocator(Locator.fromBluetooth()).build();
        }

        Publish.Builder publishBuilder = new Publish.Builder(ndo);
        if (SettingsActivity.getPreferenceAsBoolean("pref_key_include_octets")) {
            publishBuilder.fullPut();
        }
        Publish publish = publishBuilder.build();

        Node.submit(publish);

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
