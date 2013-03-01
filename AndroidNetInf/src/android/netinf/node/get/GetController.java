package android.netinf.node.get;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.node.api.Api;
import android.util.Log;

public class GetController {

    public static final String TAG = "GetController";

    Map<Api, Set<GetService>> mGetServices;

    public GetController() {
        mGetServices = new HashMap<Api, Set<GetService>>();
    }

    public void registerGetService(Api source, GetService destination) {
        if (!mGetServices.containsKey(source)) {
            mGetServices.put(source, new LinkedHashSet<GetService>());
        }
        mGetServices.get(source).add(destination);
    }

    public Ndo get(Get get) {
        Log.v(TAG, "get()");

        for (GetService getService : mGetServices.get(get.getSource())) {
            Ndo ndo = getService.get(get);
            if (ndo != null) {
                Log.i(TAG, "GET produced an NDO");
                return ndo;
            }
        }

        Log.i(TAG, "GET did not produce an NDO");
        return null;
    }

}
