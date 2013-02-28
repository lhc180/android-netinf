package android.netinf.node.get;

import java.util.LinkedHashSet;
import java.util.Set;

import android.netinf.common.Ndo;
import android.util.Log;

public class GetController {

    public static final String TAG = "GetController";

    private Set<GetService> mGetServices = new LinkedHashSet<GetService>();

    public void addGetService(GetService getService) {
        mGetServices.add(getService);
    }

    public Ndo get(Ndo ndo) {
        Ndo result = null;
        for (GetService getService : mGetServices) {
            result = getService.get(ndo);
//            if (result != null) {
//                break;
//            }
        }
        if (result == null) {
            Log.i(TAG, "GET did not produce an NDO");
        } else {
            Log.i(TAG, "GET produced an NDO");
        }
        return result;
    }

}
