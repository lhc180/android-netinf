package android.netinf.node.get;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import android.netinf.common.NetInfStatus;
import android.netinf.node.api.Api;
import android.util.Log;

public class GetController implements GetService {

    public static final String TAG = "GetController";

    private Map<Api, Set<GetService>> mGetServices;
    private Map<Api, Set<GetService>> mLocalGetServices;

    public GetController() {
        mGetServices = new HashMap<Api, Set<GetService>>();
        mLocalGetServices = new HashMap<Api, Set<GetService>>();
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
    public GetResponse perform(Get incomingGet) {
        Log.v(TAG, "get()");

        // Reduce hop limit
        Get get = new Get.Builder(incomingGet).consumeHop().build();

        // Check local services
        for (GetService getService : mLocalGetServices.get(get.getSource())) {
            GetResponse response = getService.perform(get);
            if (response.getStatus().isSuccess()) {
                Log.i(TAG, "GET produced an NDO");
                return new GetResponse(get, NetInfStatus.OK, response.getNdo());
            }
        }

        // Check other services
        if (get.getHopLimit() > 0) {
            for (GetService getService : mGetServices.get(get.getSource())) {
                GetResponse response = getService.perform(get);
                if (response.getStatus().isSuccess()) {
                    Log.i(TAG, "GET produced an NDO");
                    return new GetResponse(get, NetInfStatus.OK, response.getNdo());
                }
            }
        }

        Log.i(TAG, "GET did not produce an NDO");
        return new GetResponse(get, NetInfStatus.FAILED);
    }

}
