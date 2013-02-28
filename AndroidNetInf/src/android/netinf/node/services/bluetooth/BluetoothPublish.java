package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.lang.RandomStringUtils;
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
    public boolean isLocal() {
        return false;
    }

    @Override
    public NetInfStatus publish(Publish publish) {
        try {
            Log.v(TAG, "publish()");

            NetInfStatus result = NetInfStatus.I_FAILED;
            JSONObject jo = createPublishJson(publish);

            for (BluetoothDevice device : mApi.getBluetoothDevices()) {
                try {
                    BluetoothSocket socket = BluetoothCommon.connect(device, mApi.getUuids(), ATTEMPTS_PER_UUID);
                    DataInputStream bluetoothIn = new DataInputStream(socket.getInputStream());
                    DataOutputStream bluetoothOut = new DataOutputStream(socket.getOutputStream());
                    BluetoothCommon.write(jo, bluetoothOut);
                    if (publish.isFullPut()) {
                        BluetoothCommon.write(publish.getNdo().getCache(), bluetoothOut);
                    }

                    JSONObject response = BluetoothCommon.readJson(bluetoothIn);
                    if (response.getString("status").equals("ok")) {
                        result = NetInfStatus.OK;
                        Log.i(TAG, "PUBLISH to " + device.getName() + " succeeded");
                    } else {
                        Log.e(TAG, "PUBLISH to " + device.getName() + " failed: " + response.getString("status"));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "PUBLISH to " + device.getName() + " failed", e);
                }
            }
            return result;

        } catch (Throwable e) {
            Log.wtf(TAG, "GOTTA CATCH 'EM ALL", e);
        }
        return NetInfStatus.I_FAILED;
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
        jo.put("msgid", RandomStringUtils.randomAlphanumeric(20));
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
