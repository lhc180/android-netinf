package android.netinf.node;

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

    private Node() {

    }

    public static Node getInstance() {
        return INSTANCE;
    }

    public void start() {
        Log.v(TAG, "start()");
        mApiController.start();
    }

    @Override
    public PublishResponse perform(Publish publish) {
        return mPublishController.perform(publish);
    }

    @Override
    public GetResponse perform(Get get) {
        return mGetController.perform(get);
    }

    public SearchResponse search(Search search) {
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

    public void registerPublishService(Api source, PublishService destination) {
        mApiController.addApi(source);
        mPublishController.registerPublishService(source, destination);
    }

    public void registerGetService(Api source, GetService destination) {
        mApiController.addApi(source);
        mGetController.registerGetService(source, destination);
    }

    public void registerSearchService(Api source, SearchService destination) {
        mApiController.addApi(source);
        mSearchController.registerSearchService(source, destination);
    }

    public void addApi(Api api) {
        mApiController.addApi(api);
    }

}
