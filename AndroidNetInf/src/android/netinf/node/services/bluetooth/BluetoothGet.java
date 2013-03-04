package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfStatus;
import android.netinf.node.get.Get;
import android.netinf.node.get.GetService;
import android.util.Log;

public class BluetoothGet implements GetService {

    public static final String TAG = BluetoothGet.class.getSimpleName();

    private BluetoothApi mApi;

    public BluetoothGet(BluetoothApi api) {
        mApi = api;
    }

    @Override
    public Ndo get(Get get) {
        Log.v(TAG, "get()");
        Log.i(TAG, "Bluetooth CL received GET: " + get);

        // Create JSON representation of the Get
        JSONObject jo = null;
        try {
            jo = createGetJson(get);
        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create JSON representation of Get", e);
            return null;
        }

        // Publish to all relevant devices
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

                // Read
                return handleGetResponse(get, in);

            } catch (IOException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            } catch (JSONException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            } catch (NetInfException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            }

        }

        return null;

    }

    private JSONObject createGetJson(Get get) throws JSONException {

        JSONObject jo = new JSONObject();

        jo.put("type", "get");
        jo.put("msgid", get.getId());
        jo.put("uri", get.getNdo().getUri());

        return jo;

    }

    private Ndo handleGetResponse(Get get, DataInputStream in) throws IOException, JSONException, NetInfException {

        JSONObject jo = BluetoothCommon.readJson(in);

        if (jo.getInt("status") != NetInfStatus.OK.getCode()) {
            return null;
        }

        Ndo ndo = new Ndo(get.getNdo());

        if (jo.has("octets") && jo.getBoolean("octets")) {
            byte[] octets = BluetoothCommon.readFile(in);
            ndo.setOctets(octets);
        }

        if (jo.has("locators")){
            JSONArray locators = jo.getJSONArray("locators");
            for (int i = 0; i < locators.length(); i++) {
                ndo.addLocator(Locator.fromString(locators.getString(i)));
            }
        }

        return ndo;

    }

}
