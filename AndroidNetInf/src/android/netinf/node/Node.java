package android.netinf.node;

import java.util.Set;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.node.api.Api;
import android.netinf.node.api.ApiController;
import android.netinf.node.get.GetController;
import android.netinf.node.get.GetService;
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishController;
import android.netinf.node.publish.PublishService;
import android.netinf.node.search.SearchController;
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

    private Node() {

    }

    public static Node getInstance() {
        return INSTANCE;
    }

    public void start() {
        Log.v(TAG, "start()");
        mApiController.start();
    }

    public NetInfStatus publish(Publish publish) {
        return mPublishController.publish(publish);
    }

    public Ndo get(Ndo ndo) {
        return mGetController.get(ndo);
    }

    public Set<Ndo> search(Set<String> tokens, long timeout) {
        return mSearchController.search(tokens, timeout);
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

    public void addPublishService(PublishService publishService) {
        mPublishController.addPublishService(publishService);
    }

    public void addGetService(GetService getService) {
        mGetController.addGetService(getService);
    }

    public void addSearchService(SearchService searchService) {
        mSearchController.addSearchService(searchService);
    }

    public void addApi(Api api) {
        mApiController.addApi(api);
    }

}
