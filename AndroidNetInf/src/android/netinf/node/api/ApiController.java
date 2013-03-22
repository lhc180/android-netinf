package android.netinf.node.api;

import java.util.LinkedHashSet;
import java.util.Set;

import android.util.Log;

public class ApiController {

    public static final String TAG = ApiController.class.getSimpleName();

    private Set<Api> mApis = new LinkedHashSet<Api>();

    public void addApi(Api api) {
        mApis.add(api);
    }

    public void start() {
        Log.v(TAG, "start()");
        for (Api api : mApis) {
            if (api != null) { // TODO Null API used to direct debug requests
                api.start();
            }
        }
    }

}
