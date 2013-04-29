package android.netinf.node;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.netinf.common.ApiToServiceMap;
import android.netinf.node.api.Api;
import android.netinf.node.get.Get;
import android.netinf.node.get.GetController;
import android.netinf.node.get.GetResponse;
import android.netinf.node.get.GetService;
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishController;
import android.netinf.node.publish.PublishResponse;
import android.netinf.node.publish.PublishService;
import android.netinf.node.search.Search;
import android.netinf.node.search.SearchController;
import android.netinf.node.search.SearchResponse;
import android.netinf.node.search.SearchService;
import android.netinf.node.services.bluetooth.BluetoothApi;
import android.netinf.node.services.bluetooth.BluetoothGet;
import android.netinf.node.services.bluetooth.BluetoothPublish;
import android.netinf.node.services.bluetooth.BluetoothSearch;
import android.netinf.node.services.database.DatabaseService;
import android.netinf.node.services.http.HttpGetService;
import android.netinf.node.services.http.HttpPublishService;
import android.netinf.node.services.http.HttpSearchService;
import android.netinf.node.services.rest.RestApi;
import android.os.Environment;
import android.util.Log;


public class Node {

    public static final String TAG = Node.class.getSimpleName();

    /** Singleton Instance. */
    private static final Node INSTANCE = new Node();

    private Context mContext;

    private PublishController mPublishController;
    private GetController mGetController;
    private SearchController mSearchController;

    // TODO what is a good default choice?
//    private ExecutorService mRequestExecutor = Executors.newCachedThreadPool();
    private ExecutorService mRequestExecutor = Executors.newFixedThreadPool(5);

    private Node() {

    }

    public static void start(Context context) {

        // Setup Node
        Node node = INSTANCE;
        node.mContext = context;

        // Create Api(s) and Service(s)

        // REST
        RestApi restApi                     = RestApi.getInstance();

        // Database
        DatabaseService db                  = new DatabaseService(node.mContext);

        // HTTP CL
        HttpPublishService httpPublish      = new HttpPublishService();
        HttpGetService httpGet              = new HttpGetService();
        HttpSearchService httpSearch        = new HttpSearchService();

        // Bluetooth CL
        BluetoothApi bluetoothApi           = new BluetoothApi(node.mContext);
        BluetoothPublish bluetoothPublish   = new BluetoothPublish(bluetoothApi);
        BluetoothGet bluetoothGet           = new BluetoothGet(bluetoothApi);
        BluetoothSearch bluetoothSearch     = new BluetoothSearch(bluetoothApi);

        // Link Api(s) to PublishService(s)
        ApiToServiceMap<PublishService> publishMap = new ApiToServiceMap<PublishService>();
        publishMap.addLocalService(Api.JAVA, db);

        // Link Api(s) to GetService(s)
        ApiToServiceMap<GetService> getMap = new ApiToServiceMap<GetService>();
        getMap.addLocalService(Api.JAVA, db);
        getMap.addLocalService(bluetoothApi, db);
        getMap.addRemoteService(Api.JAVA, bluetoothGet);

        // Link Api(s) to SearchService(s)
        ApiToServiceMap<SearchService> searchMap = new ApiToServiceMap<SearchService>();
        searchMap.addLocalService(Api.JAVA, db);

        // Set Controllers
        node.setPublishController(new PublishController(publishMap));
        node.setGetController(new GetController(getMap));
        node.setSearchController(new SearchController(searchMap));

        // Start API(s)
        Set<Api> usedApis = new HashSet<Api>();
        usedApis.addAll(publishMap.getApis());
        usedApis.addAll(getMap.getApis());
        usedApis.addAll(searchMap.getApis());
        for (Api api : usedApis) {
            api.start();
        }

    }

    public static void clear() {

        // TODO proper implementation
//        boolean dbDeleted = INSTANCE.mContext.deleteDatabase(DatabaseService.DATABASE_NAME);
//        if (dbDeleted) {
//            Log.i(TAG, "Database deleted");
//        } else {
//            Log.w(TAG, "Failed to delete database");
//        }
        new DatabaseService(INSTANCE.mContext).clearDatabase();

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
        Log.i(TAG, "GET submitted for execution: " + get);

        // Wrap the Get in a Callable
        Callable<GetResponse> task = new Callable<GetResponse>() {
            @Override
            public GetResponse call() {
                return INSTANCE.mGetController.perform(get);
            }
        };

        // Submit the Callable to the Node's ExecutorService
        return INSTANCE.mRequestExecutor.submit(task);
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

    // Controllers

    private void setPublishController(PublishController publishController) {
        mPublishController = publishController;
    }

    private void setGetController(GetController getController) {
        mGetController = getController;
    }

    private void setSearchController(SearchController searchController) {
        mSearchController = searchController;
    }

}
