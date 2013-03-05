package android.netinf;

import android.content.Context;
import android.netinf.node.Node;
import android.netinf.node.api.ApiController;
import android.netinf.node.get.GetController;
import android.netinf.node.publish.PublishController;
import android.netinf.node.search.SearchController;
import android.netinf.node.services.bluetooth.BluetoothApi;
import android.netinf.node.services.bluetooth.BluetoothGet;
import android.netinf.node.services.bluetooth.BluetoothPublish;
import android.netinf.node.services.database.DatabaseService;
import android.netinf.node.services.http.HttpGetService;
import android.netinf.node.services.http.HttpPublishService;
import android.netinf.node.services.http.HttpSearchService;
import android.netinf.node.services.rest.RestApi;

public class NodeStarter implements Runnable {

    private Context mContext;

    public NodeStarter(Context context) {
        mContext = context;
    }

    @Override
    public void run() {

        // Setup Node
        Node node = Node.getInstance();

        // Set Controllers
        node.setApiController(new ApiController());
        node.setPublishController(new PublishController());
        node.setGetController(new GetController());
        node.setSearchController(new SearchController());

        // Create Api(s) and Service(s)
        // REST
        RestApi restApi = RestApi.getInstance();
        // Database
        DatabaseService db = new DatabaseService();
        // HTTP CL
        HttpPublishService httpPublish = new HttpPublishService();
        HttpGetService httpGet = new HttpGetService();
        HttpSearchService httpSearch = new HttpSearchService();
        // Bluetooth CL
        BluetoothApi bluetoothApi = new BluetoothApi(mContext);
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
        node.addPublishService(bluetoothApi, db);
        node.addGetService(bluetoothApi, db);
        node.addSearchService(bluetoothApi, db);

        // Debug
        node.addPublishService(null, db);
        node.addGetService(null, bluetoothGet);

        // Start Node
        node.start();

    }

}
