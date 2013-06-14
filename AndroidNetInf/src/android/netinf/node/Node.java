package android.netinf.node;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.netinf.R;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.api.Api;
import android.netinf.node.get.GetController;
import android.netinf.node.get.GetService;
import android.netinf.node.logging.LogController;
import android.netinf.node.logging.LogEntry;
import android.netinf.node.logging.LogService;
import android.netinf.node.publish.PublishController;
import android.netinf.node.publish.PublishService;
import android.netinf.node.search.SearchController;
import android.netinf.node.search.SearchService;
import android.netinf.node.services.bluetooth.BluetoothApi;
import android.netinf.node.services.bluetooth.BluetoothGet;
import android.netinf.node.services.bluetooth.BluetoothPublish;
import android.netinf.node.services.bluetooth.BluetoothSearch;
import android.netinf.node.services.database.Database;
import android.netinf.node.services.http.HttpGetService;
import android.netinf.node.services.http.HttpPublishService;
import android.netinf.node.services.http.HttpSearchService;
import android.netinf.node.services.rest.RestApi;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;


public class Node {

    public static final String TAG = Node.class.getSimpleName();

    /** Singleton Instance. */
    private static final Node INSTANCE = new Node();

    private Context mContext;

    private PublishController mPublishController;
    private GetController mGetController;
    private SearchController mSearchController;
    private LogController mLogController;

    // TODO what is a good choice?
    private ExecutorService mRequestExecutor = Executors.newCachedThreadPool();
//    private ExecutorService mRequestExecutor = Executors.newFixedThreadPool(5);

    private Node() {

    }

    public static void start(Context context,
            List<LogService> logServices,
            SetMultimap<Api, PublishService> localPublishServices,
            SetMultimap<Api, PublishService> remotePublishServices,
            SetMultimap<Api, GetService> localGetServices,
            SetMultimap<Api, GetService> remoteGetServices,
            SetMultimap<Api, SearchService> localSearchServices,
            SetMultimap<Api, SearchService> remoteSearchServices) {

        // Load Settings
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        // Setup Node
        Node node = INSTANCE;
        node.mContext = context;
        node.mLogController = new LogController(logServices);
        node.mPublishController = new PublishController(localPublishServices, remotePublishServices);
        node.mGetController = new GetController(localGetServices, remoteGetServices);
        node.mSearchController = new SearchController(localSearchServices, remoteSearchServices);

        // Start Logging
        node.mLogController.start();

        // Start API(s) (and only start each once)
        Set<Api> usedApis = new HashSet<Api>();
        usedApis.addAll(localPublishServices.keySet());
        usedApis.addAll(remotePublishServices.keySet());
        usedApis.addAll(localGetServices.keySet());
        usedApis.addAll(remoteGetServices.keySet());
        usedApis.addAll(localSearchServices.keySet());
        usedApis.addAll(remoteSearchServices.keySet());
        for (Api api : usedApis) {
            api.start();
        }

    }


    public static void start(Context context) {

        // Load Settings
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        // Database
        Database db                         = new Database(context);

        // Create Api(s) and Service(s)

        // REST
        RestApi restApi                     = RestApi.getInstance();

        // HTTP CL
        HttpPublishService httpPublish      = new HttpPublishService();
        HttpGetService httpGet              = new HttpGetService();
        HttpSearchService httpSearch        = new HttpSearchService();

        // Bluetooth CL
        BluetoothApi bluetoothApi           = new BluetoothApi(context);
        BluetoothPublish bluetoothPublish   = new BluetoothPublish(bluetoothApi);
        BluetoothGet bluetoothGet           = new BluetoothGet(bluetoothApi);
        BluetoothSearch bluetoothSearch     = new BluetoothSearch(bluetoothApi);

        // Link Api(s) to PublishService(s)
        SetMultimap<Api, PublishService> localPublishServices = HashMultimap.create();
        localPublishServices.put(Api.JAVA, db);
        localPublishServices.put(bluetoothApi, db);
        localPublishServices.put(Api.JAVA, httpPublish);
        SetMultimap<Api, PublishService> remotePublishServices = HashMultimap.create();

        // Link Api(s) to GetService(s)
        SetMultimap<Api, GetService> localGetServices = HashMultimap.create();
        localGetServices.put(Api.JAVA, db);
        localGetServices.put(bluetoothApi, db);
        SetMultimap<Api, GetService> remoteGetServices = HashMultimap.create();
        remoteGetServices.put(Api.JAVA, bluetoothGet);
        remoteGetServices.put(bluetoothApi, bluetoothGet);

        // Link Api(s) to SearchService(s)
        SetMultimap<Api, SearchService> localSearchServices = HashMultimap.create();
        localSearchServices.put(Api.JAVA, db);
        localSearchServices.put(bluetoothApi, db);
        SetMultimap<Api, SearchService> remoteSearchServices = HashMultimap.create();

        // LogService(s)
        List<LogService> logServices = new LinkedList<LogService>();
        // logServices.add(new LogCatLogger());
        // logServices.add(new VisualizationService());

        // Start Node
        start(context, logServices,
                localPublishServices, remotePublishServices,
                localGetServices, remoteGetServices,
                localSearchServices, remoteSearchServices);

    }

    public static Context getContext() {
        return INSTANCE.mContext;
    }

    public static void showPreferences(Activity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    public static void clear() {

        // TODO proper implementation
        // Context.deleteDatabase() does not seem to remove the database until the app is restarted
        // http://code.google.com/p/android/issues/detail?id=13727
        new Database(INSTANCE.mContext).clearDatabase();

        File cache = new File(Environment.getExternalStorageDirectory(), "shared");
        boolean cacheDeleted = FileUtils.deleteQuietly(cache);
        if (cacheDeleted) {
            Log.i(TAG, "Cache deleted");
        } else {
            Log.w(TAG, "Failed to delete cache");
        }

    }

    public static Future<PublishResponse> submit(final Publish publish) {
        Log.i(TAG, "PUBLISH submitted for execution: " + publish);

        // Wrap the Publish in a Callable
        Callable<PublishResponse> task = new Callable<PublishResponse>() {
            @Override
            public PublishResponse call() {
                return INSTANCE.mPublishController.perform(publish);
            }
        };

        // Submit the Callable to the Node's ExecutorService
        return INSTANCE.mRequestExecutor.submit(task);
    }

    public static Future<GetResponse> submit(final Get get) {
        return INSTANCE.mGetController.submit(get);
    }

    public static Future<SearchResponse> submit(final Search search) {
        Log.i(TAG, "GET submitted for execution: " + search);

        // Wrap the Search in a Callable
        Callable<SearchResponse> task = new Callable<SearchResponse>() {
            @Override
            public SearchResponse call() {
                return INSTANCE.mSearchController.perform(search);
            }
        };

        // Submit the Callable to the Node's ExecutorService
        return INSTANCE.mRequestExecutor.submit(task);
    }

    public static void log(LogEntry logEntry, Publish publish) {
        INSTANCE.mLogController.log(logEntry, publish);
    }

    public static void log(LogEntry logEntry, PublishResponse publishResponse) {
        INSTANCE.mLogController.log(logEntry, publishResponse);
    }

    public static void log(LogEntry logEntry, Get get) {
        INSTANCE.mLogController.log(logEntry, get);
    }

    public static void log(LogEntry logEntry, GetResponse getResponse) {
        INSTANCE.mLogController.log(logEntry, getResponse);
    }

    public static void log(LogEntry logEntry, Search search) {
        INSTANCE.mLogController.log(logEntry, search);
    }

    public static void log(LogEntry logEntry, SearchResponse searchResponse) {
        INSTANCE.mLogController.log(logEntry, searchResponse);
    }

}
