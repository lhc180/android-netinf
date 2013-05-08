package android.netinf.node.get;

import java.util.concurrent.TimeUnit;

import android.netinf.common.ApiToServiceMap;
import android.netinf.common.NetInfStatus;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.node.Node;
import android.util.Log;

public class GetController implements GetService {

    public static final int TIMEOUT = 5000;

    public static final String TAG = GetController.class.getSimpleName();

    private ApiToServiceMap<GetService> mServices;
    private RequestAggregator mRequestAggregator;

    public GetController(ApiToServiceMap<GetService> services) {
        mServices = services;
        mRequestAggregator = new RequestAggregator();
    }

    @Override
    public GetResponse perform(Get get) {

        // Reduce hop limit
        get = new Get.Builder(get).consumeHop().build();

        // Try to aggregate the Get
        boolean aggregated = mRequestAggregator.aggregate(get);

        if (aggregated) {

            Log.i(TAG, "Aggregated GET of " + get);
            GetResponse response = get.aggregate(TIMEOUT, TimeUnit.MILLISECONDS);
            mRequestAggregator.done(get);
            Log.i(TAG, "Aggregated GET of " + get + " done. STATUS " + response.getStatus());
            return response;

        } else {

            // Assume it will fail
            GetResponse response = new GetResponse(get, NetInfStatus.FAILED);

            // First check local services
            Log.i(TAG, "Local GET of " + get);
            for (GetService getService : mServices.getLocalServices(get.getSource())) {
                response = getService.perform(get);
                if (response.getStatus().isSuccess()) {
                    break;
                }
            }

            // Then check other services as needed
            if (response.getStatus().isError() && get.getHopLimit() > 0) {
                Log.i(TAG, "Remote GET of " + get);
                for (GetService getService : mServices.getRemoteServices(get.getSource())) {
                    response = getService.perform(get);
                    if (response.getStatus().isSuccess()) {
                        break;
                    }
                }
            }

            // Notify all waiting aggregated requests
            // synchronized so no new aggregated requests are added while responding
            mRequestAggregator.notify(get, response);

            // Publish the received data locally
            if (response.getStatus().isSuccess()) {
                Publish publish = new Publish.Builder(response.getNdo()).build();
                Node.submit(publish);
            }

            Log.i(TAG, "GET of " + get + " done. STATUS " + response.getStatus());
            return response;

        }

    }

}
