package android.netinf;

import android.content.Context;
import android.netinf.node.Node;
import android.netinf.node.api.ApiController;
import android.netinf.node.get.GetController;
import android.netinf.node.publish.PublishController;
import android.netinf.node.search.SearchController;
import android.netinf.node.services.bluetooth.BluetoothApi;
import android.netinf.node.services.bluetooth.BluetoothPublish;
import android.netinf.node.services.database.DatabaseService;
import android.netinf.node.services.http.HttpPublishService;

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

        // Add DatabaseService
        DatabaseService db = new DatabaseService();
        node.addPublishService(db);
        node.addGetService(db);
        node.addSearchService(db);

        // Add RestApi
//        node.addApi(new RestApi());

        // Add HTTP CL
        node.addPublishService(new HttpPublishService());
//        node.addGetService(new HttpGetService());
//        node.addSearchService(new HttpSearchService());

        // Add Bluetooth CL
        BluetoothApi bluetoothApi = new BluetoothApi(mContext);
        node.addApi(bluetoothApi);
        node.addPublishService(new BluetoothPublish(bluetoothApi));

        // Start Node
        node.start();

    }

}
