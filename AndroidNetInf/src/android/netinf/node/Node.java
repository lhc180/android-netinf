package android.netinf.node;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.netinf.node.api.Api;
import android.netinf.node.api.ApiController;
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
import android.netinf.node.services.database.DatabaseService;
import android.netinf.node.services.http.HttpGetService;
import android.netinf.node.services.http.HttpPublishService;
import android.netinf.node.services.http.HttpSearchService;
import android.netinf.node.services.rest.RestApi;
import android.util.Log;


public class Node {

    public static final String TAG = "Node";

    /** Singleton Instance. */
    private static final Node INSTANCE = new Node();

    private Context mContext;

    private PublishController mPublishController;
    private GetController mGetController;
    private SearchController mSearchController;
    private ApiController mApiController;

    private ExecutorService mRequestExecutor = Executors.newCachedThreadPool();

    private Node() {

    }

    public static void start(Context context) {
        Log.v(TAG, "start()");

        // Setup Node
        Node node = INSTANCE;
        node.mContext = context;

        // Set Controllers
        node.setApiController(new ApiController());
        node.setPublishController(new PublishController());
        node.setGetController(new GetController());
        node.setSearchController(new SearchController());

        // Create Api(s) and Service(s)
        // REST
        RestApi restApi = RestApi.getInstance();
        // Database
        DatabaseService db = new DatabaseService(node.mContext);
        // HTTP CL
        HttpPublishService httpPublish = new HttpPublishService();
        HttpGetService httpGet = new HttpGetService();
        HttpSearchService httpSearch = new HttpSearchService();
        // Bluetooth CL
        BluetoothApi bluetoothApi = new BluetoothApi(node.mContext);
        BluetoothPublish bluetoothPublish =  new BluetoothPublish(bluetoothApi);
        BluetoothGet bluetoothGet =  new BluetoothGet(bluetoothApi);

        // Link source Api(s) and destination Service(s)
        // Requests received on the REST Api should use...
//        node.registerPublishService(restApi, db);
//        node.registerPublishService(restApi, httpPublish);
//        node.registerPublishService(restApi, bluetoothPublish);
//        node.registerGetService(restApi, db);
//        node.registerGetService(restApi, httpGet);
//        node.registerSearchService(restApi, db);
//        node.registerSearchService(restApi, httpSearch);
        // Requests received on the Bluetooth Api should use...
        node.addLocalPublishService(bluetoothApi, db);
        node.addLocalGetService(bluetoothApi, db);
        node.addLocalSearchService(bluetoothApi, db);

        // Debug
        node.addLocalPublishService(Api.JAVA, db);
        node.addGetService(Api.JAVA, bluetoothGet);

        // Start API(s)
        node.mApiController.start();
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

//    @Override
//    public PublishResponse perform(Publish publish) {
//        return mPublishController.perform(publish);
//    }
//
//    @Override
//    public GetResponse perform(Get get) {
//        return mGetController.perform(get);
//    }
//
//    @Override
//    public SearchResponse perform(Search search) {
//        return mSearchController.perform(search);
//    }

    private void setApiController(ApiController apiController) {
        mApiController = apiController;
    }

    private void setPublishController(PublishController publishController) {
        mPublishController = publishController;
    }

    private void setGetController(GetController getController) {
        mGetController = getController;
    }

    private void setSearchController(SearchController searchController) {
        mSearchController = searchController;
    }

    private void addPublishService(Api source, PublishService destination) {
        mApiController.addApi(source);
        mPublishController.addPublishService(source, destination);
    }

    private void addGetService(Api source, GetService destination) {
        mApiController.addApi(source);
        mGetController.addGetService(source, destination);
    }

    private void addSearchService(Api source, SearchService destination) {
        mApiController.addApi(source);
        mSearchController.addSearchService(source, destination);
    }

    private void addLocalPublishService(Api source, PublishService destination) {
        mApiController.addApi(source);
        mPublishController.addLocalPublishService(source, destination);
    }

    private void addLocalGetService(Api source, GetService destination) {
        mApiController.addApi(source);
        mGetController.addLocalGetService(source, destination);
    }

    private void addLocalSearchService(Api source, SearchService destination) {
        mApiController.addApi(source);
        mSearchController.addLocalSearchService(source, destination);
    }

}
