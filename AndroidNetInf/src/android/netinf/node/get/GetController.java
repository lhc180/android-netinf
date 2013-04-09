package android.netinf.node.get;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.node.api.Api;
import android.util.Log;

public class GetController implements GetService {

    public static final int TIMEOUT = 5000;

    public static final String TAG = GetController.class.getSimpleName();

    private Map<Api, Set<GetService>> mGetServices;
    private Map<Api, Set<GetService>> mLocalGetServices;
    private Map<Ndo, Set<Get>> mPendingGets;

    public GetController() {
        mGetServices = new HashMap<Api, Set<GetService>>();
        mLocalGetServices = new HashMap<Api, Set<GetService>>();
        mPendingGets = new HashMap<Ndo, Set<Get>>();
    }

    public void addGetService(Api source, GetService destination) {
        if (!mGetServices.containsKey(source)) {
            mGetServices.put(source, new LinkedHashSet<GetService>());
        }
        mGetServices.get(source).add(destination);
    }

    public void addLocalGetService(Api source, GetService destination) {
        if (!mLocalGetServices.containsKey(source)) {
            mLocalGetServices.put(source, new LinkedHashSet<GetService>());
        }
        mLocalGetServices.get(source).add(destination);
    }

    @Override
    public GetResponse perform(Get get) {

        // Reduce hop limit
        get = new Get.Builder(get).consumeHop().build();


        // Determine if this get should be aggregated, if not setup for coming aggregation
        // Synchronized because otherwise two threads might perform the GET
        boolean aggregate = false;
        synchronized (mPendingGets) {
            aggregate = mPendingGets.containsKey(get.getNdo());
            if (aggregate) {
                // We should aggregate, add to set of aggregated requests
                mPendingGets.get(get.getNdo()).add(get);
            } else {
                // We should not aggregate, create new empty set of aggregated requests
                mPendingGets.put(get.getNdo(), new HashSet<Get>());
            }
        }

        if (aggregate) {

            Log.i(TAG, "Aggregated GET of " + get);

            GetResponse response = get.aggregate(TIMEOUT, TimeUnit.MILLISECONDS);
            // The response to the aggregated get could be submitted here but in that case we just ignore it
            synchronized (mPendingGets) {
                mPendingGets.get(get.getNdo()).remove(get);
            }

            Log.i(TAG, "Aggregated GET of " + get + " done. STATUS " + response.getStatus());
            return response;

        } else {

            // Assume it will fail
            GetResponse response = new GetResponse(get, NetInfStatus.FAILED);

            // First check local services
            Log.i(TAG, "Local GET of " + get);
            for (GetService getService : mLocalGetServices.get(get.getSource())) {
                response = getService.perform(get);
                if (response.getStatus().isSuccess()) {
                    break;
                }
            }

            // Then check other services as needed
            if (response.getStatus().isError() && get.getHopLimit() > 0) {
                Log.i(TAG, "Remote GET of " + get);
                for (GetService getService : mGetServices.get(get.getSource())) {
                    response = getService.perform(get);
                    if (response.getStatus().isSuccess()) {
                        break;
                    }
                }
            }

            // Notify all waiting aggregated requests
            // synchronized so no new aggregated requests are added while responding
            synchronized (mPendingGets) {
                for (Get aggregatedGet : mPendingGets.get(get.getNdo())) {
                    aggregatedGet.submitAggregatedResponse(response);
                }
                mPendingGets.remove(get.getNdo());
            }

            //
            Log.i(TAG, "GET of " + get + " done. STATUS " + response.getStatus());
            return response;

        }

    }

}
