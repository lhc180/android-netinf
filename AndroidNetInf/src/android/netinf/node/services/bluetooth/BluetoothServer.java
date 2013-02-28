package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfStatus;
import android.util.Log;


public class BluetoothServer implements Runnable {

    public static final String TAG = "BluetoothServer";

    private BluetoothApi mApi;
    private BluetoothSocket mSocket;
    private DataOutputStream mOut;
    private DataInputStream mIn;

    public BluetoothServer(BluetoothApi api) {
        mApi = api;
    }

    @Override
    public void run() {

        try {

            Log.v(TAG, "run()");

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            UUID uuid = mApi.getUuid();

            try {
                BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("android.netinf", uuid);
                Log.i(TAG, adapter.getName() + " waiting for connections using UUID " + uuid);
                mSocket = server.accept();
                Log.i(TAG, adapter.getName() + " accepted a connection using UUID " + uuid);
                server.close();
                if (mSocket != null) {
                    setupStreams();
                    handleRequest();
                    //                    socket.close();
                    //                    handleClose(socket);
                }
            } catch (IOException e) {
                Log.wtf(TAG, "Failed waiting for, handling, or returning response", e);
            } finally {
                mApi.returnUuid(uuid);
            }

        } catch (Throwable e) {
            Log.wtf(TAG, "GOTTA CATCH 'EM ALL", e);
        }

    }

    private void setupStreams() throws IOException {
        Log.v(TAG, "setupStreams()");

        mIn = new DataInputStream(mSocket.getInputStream());
        mOut = new DataOutputStream(mSocket.getOutputStream());
    }

    private void handleRequest() throws IOException, JSONException, NetInfException {
        Log.v(TAG, "handleRequest()");

        JSONObject request = BluetoothCommon.readJson(mIn);
        Log.d(TAG, "request = " + request);
        if (request.getString("type").equals("publish")) {
            handlePublish(request);
        } else {
            Log.wtf(TAG, "Unhandled request type: " + request.getString("type"));
        }
    }

    private void handlePublish(JSONObject jo) throws NetInfException, JSONException, IOException {


        // TODO WORK HERE!!!!!!!!!!!!!!!!

//        Ndo ndo = NetInfUtils.fromJson(publish);
//
//        NetInfStatus status = Node.getInstance().publish(ndo);
//        JSONObject response = createPublishResponse(publish, status);
//        BluetoothCommon.write(response, mOut);
    }

    private JSONObject createPublishResponse(JSONObject publish, NetInfStatus status) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("msgid", publish.getString("msgid"));
        response.put("status", status.getCode());
        return response;
    }

}
