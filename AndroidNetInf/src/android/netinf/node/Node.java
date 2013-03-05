package android.netinf.node;

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


public class Node implements PublishService, GetService, SearchService {

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

    // TODO use these 3
    public Future<PublishResponse> submit(Publish publish) {
        Log.i(TAG, "PUBLISH submitted to node for execution: " + publish);
        return Node.getInstance().mRequestExecutor.submit(publish);
    }

    public Future<GetResponse> submit(Get get) {
        Log.i(TAG, "GET submitted to node for execution: " + get);
        return Node.getInstance().mRequestExecutor.submit(get);
    }

    public Future<SearchResponse> submit(Search search) {
        Log.i(TAG, "SEARCH submitted to node for execution: " + search);
        return Node.getInstance().mRequestExecutor.submit(search);
    }

    @Override
    public PublishResponse perform(Publish publish) {
        return mPublishController.perform(publish);
    }

    @Override
    public GetResponse perform(Get get) {
        return mGetController.perform(get);
    }

    @Override
    public SearchResponse perform(Search search) {
        return mSearchController.perform(search);
    }

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
