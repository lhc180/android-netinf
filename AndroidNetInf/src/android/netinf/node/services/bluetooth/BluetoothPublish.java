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
import android.netinf.node.publish.PublishResponse;
import android.netinf.node.publish.PublishService;
import android.util.Log;

public class BluetoothPublish implements PublishService {

    public static final String TAG = "BluetoothPublish";

    private BluetoothApi mApi;

    public BluetoothPublish(BluetoothApi api) {
        mApi = api;
    }

    @Override
    public PublishResponse perform(Publish publish) {
            Log.v(TAG, "publish()");
            Log.i(TAG, "Bluetooth CL received PUBLISH: " + publish);

            // Create JSON representation of Pulbish
            JSONObject jo = null;
            try {
                jo = createPublishJson(publish);
            } catch (JSONException e) {
                Log.wtf(TAG, "Failed to create JSON representation of Publish", e);
                return new PublishResponse(publish, NetInfStatus.FAILED);
            }

            // Publish to all relevant devices
            NetInfStatus status = NetInfStatus.FAILED;
            for (BluetoothDevice device : mApi.getBluetoothDevices()) {

                BluetoothSocket socket = null;
                DataInputStream in = null;
                DataOutputStream out = null;
                try {

                    // Connect
                    socket = BluetoothCommon.connect(device);
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());

                    // Send
                    BluetoothCommon.write(jo, out);
                    if (publish.isFullPut()) {
                        BluetoothCommon.write(publish.getNdo().getOctets(), out);
                    }

                    // Receive
                    JSONObject response = BluetoothCommon.readJson(in);
                    if (NetInfStatus.OK.equals(response.getInt("status"))) {
                        status = NetInfStatus.OK;
                        Log.i(TAG, "PUBLISH to " + device.getName() + " succeeded");
                    } else {
                        Log.e(TAG, "PUBLISH to " + device.getName() + " failed: " + response.getString("status"));
                    }

                } catch (IOException e) {
                    Log.e(TAG, "PUBLISH to " + device.getName() + " failed", e);
                } catch (JSONException e) {
                    Log.e(TAG, "PUBLISH to " + device.getName() + " failed", e);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                    IOUtils.closeQuietly(socket);
                }

            }
            return new PublishResponse(publish, status);
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
        jo.put("msgid", publish.getId());
        jo.put("hoplimit", publish.getHopLimit());
        jo.put("uri", ndo.getUri());
        jo.put("locators", locators);
        jo.put("ext", meta);

        if (publish.isFullPut()) {
            jo.put("octets", true);
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
