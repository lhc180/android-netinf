package android.netinf.node.api;

import java.util.LinkedHashSet;
import java.util.Set;

import android.util.Log;

public class ApiController {

    public static final String TAG = "ApiController";

    Set<Api> mApis = new LinkedHashSet<Api>();

    public void addApi(Api api) {
        mApis.add(api);
    }

    public void start() {
        Log.v(TAG, "start()");
        for (Api api : mApis) {
            api.start();
        }
    }

}
