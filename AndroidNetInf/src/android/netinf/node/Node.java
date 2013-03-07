package android.netinf.node;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import android.util.Log;


public class Node {

    public static final String TAG = "Node";

    /** Singleton Instance. */
    private static final Node INSTANCE = new Node();

    private PublishController mPublishController;
    private GetController mGetController;
    private SearchController mSearchController;
    private ApiController mApiController;

    private ExecutorService mRequestExecutor = Executors.newCachedThreadPool();

    private Node() {

    }

    public static Node getInstance() {
        return INSTANCE;
    }

    public void start() {
        Log.v(TAG, "start()");
        mApiController.start();
    }

    public Future<PublishResponse> submit(final Publish publish) {
        Log.i(TAG, "PUBLISH submitted for execution: " + publish);

        // Wrap the Publish in a Callable
        Callable<PublishResponse> task = new Callable<PublishResponse>() {
            @Override
            public PublishResponse call() {
                return mPublishController.perform(publish);
            }
        };

        // Submit the Callable to the Node's ExecutorService
        return mRequestExecutor.submit(task);
    }

    public Future<GetResponse> submit(final Get get) {
        Log.i(TAG, "GET submitted for execution: " + get);

        // Wrap the Get in a Callable
        Callable<GetResponse> task = new Callable<GetResponse>() {
            @Override
            public GetResponse call() {
                return mGetController.perform(get);
            }
        };

        // Submit the Callable to the Node's ExecutorService
        return mRequestExecutor.submit(task);
    }

    public Future<SearchResponse> submit(final Search search) {
        Log.i(TAG, "GET submitted for execution: " + search);

        // Wrap the Search in a Callable
        Callable<SearchResponse> task = new Callable<SearchResponse>() {
            @Override
            public SearchResponse call() {
                return mSearchController.perform(search);
            }
        };

        // Submit the Callable to the Node's ExecutorService
        return mRequestExecutor.submit(task);
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

    public void setApiController(ApiController apiController) {
        mApiController = apiController;
    }

    public void setPublishController(PublishController publishController) {
        mPublishController = publishController;
    }

    public void setGetController(GetController getController) {
        mGetController = getController;
    }

    public void setSearchController(SearchController searchController) {
        mSearchController = searchController;
    }

    public void addPublishService(Api source, PublishService destination) {
        mApiController.addApi(source);
        mPublishController.addPublishService(source, destination);
    }

    public void addGetService(Api source, GetService destination) {
        mApiController.addApi(source);
        mGetController.addGetService(source, destination);
    }

    public void addSearchService(Api source, SearchService destination) {
        mApiController.addApi(source);
        mSearchController.addSearchService(source, destination);
    }

    public void addLocalPublishService(Api source, PublishService destination) {
        mApiController.addApi(source);
        mPublishController.addLocalPublishService(source, destination);
    }

    public void addLocalGetService(Api source, GetService destination) {
        mApiController.addApi(source);
        mGetController.addLocalGetService(source, destination);
    }

    public void addLocalSearchService(Api source, SearchService destination) {
        mApiController.addApi(source);
        mSearchController.addLocalSearchService(source, destination);
    }

}
