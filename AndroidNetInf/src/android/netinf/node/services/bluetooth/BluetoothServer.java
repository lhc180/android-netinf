package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfStatus;
import android.netinf.common.NetInfUtils;
import android.netinf.node.get.Get;
import android.netinf.node.publish.Publish;
import android.util.Log;


public class BluetoothServer implements Runnable {

    public static final String TAG = "BluetoothServer";

    private BluetoothApi mApi;
    private UUID mUuid;

    public BluetoothServer(BluetoothApi api, UUID uuid) {
        mApi = api;
        mUuid = uuid;
    }

    @Override
    public void run() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        while (true) {

            // Reset
            BluetoothSocket socket = null;
            DataInputStream in = null;
            DataOutputStream out = null;

            try {
                BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("android.netinf", mUuid);
                Log.i(TAG, adapter.getName() + " waiting for connections using UUID " + mUuid);
                socket = server.accept();
                Log.i(TAG, adapter.getName() + " accepted a connection using UUID " + mUuid);
                server.close();
                if (socket != null) {
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                    handleRequest(in, out);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to handle request", e);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to handle request", e);
            } catch (NetInfException e) {
                Log.e(TAG, "Failed to handle request", e);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(socket);
            }

        }

    }

    private void handleRequest(DataInputStream in, DataOutputStream out)
            throws IOException, JSONException, NetInfException {

        Log.v(TAG, "handleRequest()");

        JSONObject request = BluetoothCommon.readJson(in);
        Log.d(TAG, "request = " + request);
        if (request.getString("type").equals("publish")) {
            handlePublish(in, out, request);
        } else if (request.getString("type").equals("get")) {
            handleGet(in, out, request);
        } else {
            Log.wtf(TAG, "Bluetooth API received UNKNOWN: " + request.getString("type"));
        }

        // TODO WAIT UNTIL WE KNOW RESPONSE READ?!

    }

    private void handlePublish(DataInputStream in, DataOutputStream out, JSONObject jo)
            throws NetInfException, JSONException, IOException {

        Log.v(TAG, "handlePublish()");
        Ndo ndo = NetInfUtils.toNdo(jo);
        Log.i(TAG, "Bluetooth API received PUBLISH: " + ndo.getUri());

        // Create and execute publish
        Publish publish = new Publish(mApi, jo.getString("msgid"), ndo);
        if (jo.getBoolean("octets") == true) {
            byte[] octets = BluetoothCommon.readFile(in);
            ndo.setOctets(octets);
            publish.setFullPut(true);
        }
        publish.execute();

        // Create publish response
        JSONObject response = new JSONObject();
        response.put("msgid", publish.getId());
        response.put("status", publish.getResult().getCode());
        BluetoothCommon.write(response, out);

    }

    private void handleGet(DataInputStream in, DataOutputStream out, JSONObject jo)
            throws NetInfException, JSONException, IOException {

        Log.v(TAG, "handleGet()");
        Ndo ndo = NetInfUtils.toNdo(jo);
        Log.i(TAG, "Bluetooth API received GET: " + ndo.getUri());

        // Create and execute get
        Get get = new Get(mApi, jo.getString("msgid"), ndo);
        Ndo result = get.execute();

        // Create get response
        JSONObject response = new JSONObject();
        response.put("msgid", get.getId());

        if (result == null) {
            response.put("status", NetInfStatus.I_FAILED.getCode());
            BluetoothCommon.write(response, out);
            return;
        }

        response.put("status", NetInfStatus.OK.getCode());
        if (ndo.isCached()) {
            response.put("octets", true);
            BluetoothCommon.write(response, out);
            BluetoothCommon.write(ndo.getOctets(), out);
        } else {
            JSONArray locators = new JSONArray();
            for (Locator locator : ndo.getLocators()) {
                locators.put(locator.toString());
            }
            BluetoothCommon.write(response, out);
        }

    }

}
