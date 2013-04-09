package android.netinf.node.services.rest;


import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.restlet.Component;
import org.restlet.data.Protocol;

import android.netinf.node.api.Api;
import android.util.Log;

public class RestApi implements Api {

    public static final String TAG = RestApi.class.getSimpleName();

    /** Singleton Instance. */
    private static final RestApi INSTANCE = new RestApi();

    private Component mComponent = new Component();

    private RestApi() {
        // TODO get settings in a nice way
        int port = 8080;
        mComponent.getServers().add(Protocol.HTTP, port);
        mComponent.getDefaultHost().attach("/publish", RestPublishResource.class);
        mComponent.getDefaultHost().attach("/get", RestGetResource.class);
        mComponent.getDefaultHost().attach("/search", RestSearchResource.class);
        disableLogging();
    }

    public static RestApi getInstance() {
        return INSTANCE;
    }

    @Override
    public void start() {
        try {
            mComponent.start();
        } catch (Exception e) {
            Log.e(TAG, "RestApi failed to start", e);
        }
    }

    @Override
    public void stop() {
        try {
            mComponent.stop();
        } catch (Exception e) {
            Log.e(TAG, "RestApi failed to stop", e);
        }
    }

    private void disableLogging() {
        // Disable Restlet Logging
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.removeHandler(rootLogger.getHandlers()[0]);
    }

}
