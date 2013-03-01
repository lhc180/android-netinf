package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfStatus;
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishService;
import android.util.Log;

public class BluetoothPublish implements PublishService {

    public static final String TAG = "BluetoothPublish";

    public static final int ATTEMPTS_PER_UUID = 2;

    private BluetoothApi mApi;

    public BluetoothPublish(BluetoothApi api) {
        mApi = api;
    }

    @Override
    public NetInfStatus publish(Publish publish) {
            Log.v(TAG, "publish()");

            NetInfStatus result = NetInfStatus.I_FAILED;

            // Create JSON representation of Pulbish
            JSONObject jo = null;
            try {
                jo = createPublishJson(publish);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to create JSON representation of Publish", e);
                return NetInfStatus.I_FAILED;
            }

            for (BluetoothDevice device : mApi.getBluetoothDevices()) {

                BluetoothSocket socket = null;
                DataInputStream bluetoothIn = null;
                DataOutputStream bluetoothOut = null;
                try {

                    // Connect
                    socket = BluetoothCommon.connect(device, mApi.getUuids(), ATTEMPTS_PER_UUID);
                    bluetoothIn = new DataInputStream(socket.getInputStream());
                    bluetoothOut = new DataOutputStream(socket.getOutputStream());

                    // Send
                    BluetoothCommon.write(jo, bluetoothOut);
                    if (publish.isFullPut()) {
                        BluetoothCommon.write(publish.getNdo().getOctets(), bluetoothOut);
                    }

                    // Receive
                    JSONObject response = BluetoothCommon.readJson(bluetoothIn);
                    if (response.getInt("status") == NetInfStatus.OK.getCode()) {
                        result = NetInfStatus.OK;
                        Log.i(TAG, "PUBLISH to " + device.getName() + " succeeded");
                    } else {
                        Log.e(TAG, "PUBLISH to " + device.getName() + " failed: " + response.getString("status"));
                    }

                } catch (IOException e) {
                    Log.e(TAG, "PUBLISH to " + device.getName() + " failed", e);
                } catch (JSONException e) {
                    Log.e(TAG, "PUBLISH to " + device.getName() + " failed", e);
                } finally {
                    IOUtils.closeQuietly(bluetoothIn);
                    IOUtils.closeQuietly(bluetoothOut);
                    IOUtils.closeQuietly(socket);
                }

            }
            return result;
    }

    private JSONObject createPublishJson(Publish publish) throws JSONException {
        Log.v(TAG, "createPublish");

        Ndo ndo = publish.getNdo();

        JSONObject jo = new JSONObject();

        JSONArray locators = new JSONArray();
        for (Locator locator : ndo.getLocators()) {
            locators.put(locator.toString());
        }

        JSONObject meta = new JSONObject();
        meta.put("meta", ndo.getMetadata().toJson());

        jo.put("type", "publish");
        jo.put("msgid", publish.getMessageId());
        jo.put("uri", ndo.getUri());
        jo.put("locators", locators);
        jo.put("ext", meta);

        if (publish.isFullPut()) {
            jo.put("fullput", true);
        }

        return jo;

    }
































//
//    private void close(BluetoothSocket socket) {
//        Log.v(TAG, "close()");
//        try {
//            socket.getInputStream().close();
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to close BluetoothSocket InputStream", e);
//        }
//        try {
//            socket.getOutputStream().close();
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to close BluetoothSocket OutputStream", e);
//        }
//        try {
//            socket.close();
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to close BluetoothSocket", e);
//        }
//    }

}
