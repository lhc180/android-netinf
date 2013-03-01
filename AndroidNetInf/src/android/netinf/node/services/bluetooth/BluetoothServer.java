package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfUtils;
import android.netinf.node.publish.Publish;
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

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        UUID uuid = mApi.getUuid();

        try {
            BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("android.netinf", uuid);
            Log.i(TAG, adapter.getName() + " waiting for connections using UUID " + uuid);
            mSocket = server.accept();
            Log.i(TAG, adapter.getName() + " accepted a connection using UUID " + uuid);
            server.close();
            if (mSocket != null) {
                mIn = new DataInputStream(mSocket.getInputStream());
                mOut = new DataOutputStream(mSocket.getOutputStream());
                handleRequest();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to handle request", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to handle request", e);
        } catch (NetInfException e) {
            Log.e(TAG, "Failed to handle request", e);
        } finally {
            mApi.returnUuid(uuid);
            IOUtils.closeQuietly(mIn);
            IOUtils.closeQuietly(mOut);
            IOUtils.closeQuietly(mSocket);
        }

    }

    private void handleRequest() throws IOException, JSONException, NetInfException {
        Log.v(TAG, "handleRequest()");

        JSONObject request = BluetoothCommon.readJson(mIn);
        Log.d(TAG, "request = " + request);
        if (request.getString("type").equals("publish")) {
            Log.i(TAG, "Bluetooth API received PUBLISH");
            handlePublish(request);
        } else {
            Log.wtf(TAG, "Bluetooth API received UNKNOWN: " + request.getString("type"));
        }

        // TODO WAIT UNTIL WE KNOW RESPONSE READ?!

    }

    private void handlePublish(JSONObject jo) throws NetInfException, JSONException, IOException {
        Log.v(TAG, "handlePublish()");

        Publish publish = createPublish(jo);
        publish.execute();
        JSONObject response = createPublishResponse(publish);
        BluetoothCommon.write(response, mOut);

    }

    private Publish createPublish(JSONObject jo) throws NetInfException, JSONException, IOException {
        Log.v(TAG, "createPublish()");

        Ndo ndo = NetInfUtils.toNdo(jo);
        Publish publish = new Publish(mApi, jo.getString("msgid"), ndo);
        if (jo.getBoolean("fullput") == true) {
            byte[] octets = BluetoothCommon.readFile(mIn);
            ndo.setOctets(octets);
            publish.setFullPut(true);
        }
        return publish;
    }

    private JSONObject createPublishResponse(Publish publish) throws JSONException {
        Log.v(TAG, "createPublishResponse()");

        JSONObject response = new JSONObject();
        response.put("msgid", publish.getMessageId());
        response.put("status", publish.getResult().getCode());
        return response;
    }

}
