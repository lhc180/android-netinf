package android.netinf.node.services.bluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Locator;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.node.Node;
import android.netinf.node.get.GetService;
import android.netinf.node.logging.LogEntry;
import android.util.Log;

public class BluetoothGet implements GetService {

    public static final String TAG = BluetoothGet.class.getSimpleName();

    private BluetoothApi mApi;

    public BluetoothGet(BluetoothApi api) {
        mApi = api;
    }

    @Override
    public GetResponse perform(Get get) {
        return perform(get, mApi.getBluetoothDevices());
    }

    @Override
    public GetResponse resolveLocators(Get get) {

        Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>();
        for (Locator locator : get.getNdo().getLocators()) {
            for (BluetoothDevice device : mApi.getAllBluetoothDevices()) {
                if (locator.getUri().contains(device.getAddress())) {
                    devices.add(device);
                    break;
                }
            }
        }

        Log.d(TAG, "Bluetooth locators resulted in: " + devices);

        return perform(get, devices);

    }

    private GetResponse perform(Get get, Set<BluetoothDevice> devices) {
        Log.i(TAG, "Bluetooth GET " + get);

        // Check if Bluetooth is available, could be restarting
        if (!BluetoothCommon.isBluetoothAvailable()) {
            return new GetResponse.Builder(get).failed().build();
        }

        // Create JSON representation of the Get
        JSONObject jo = null;
        try {
            jo = createGetJson(get);
        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create JSON representation of Get", e);
            return new GetResponse.Builder(get).failed().build();
        }

        // Get from all relevant devices
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        List<BluetoothDevice> random = new ArrayList<BluetoothDevice>(devices);
        Collections.shuffle(random);
        for (BluetoothDevice device : random) {

            try {

                Log.d(TAG, adapter.getName() + " getting socket to " + device.getName());

                // Get BluetoothSocket
                BluetoothSocket socket = mApi.getManager().getSocket(device);
                Log.i(TAG, adapter.getName() + " got socket to " + device.getName() + ", sending GET " + get);

                // Send Get
                BluetoothCommon.write(jo, socket);
                Node.log(LogEntry.newOutgoing("Bluetooth"), get);

                // Wait for Response
                GetResponse response = mApi.getManager().getResponse(get).get(BluetoothCommon.getTimeout(), BluetoothCommon.TIMEOUT_UNIT);
                Node.log(LogEntry.newIncoming("Bluetooth"), response);
                if (response.getStatus().isSuccess()) {
                    return response;
                }

            } catch (IOException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            } catch (ExecutionException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            } catch (TimeoutException e) {
                Log.e(TAG, "GET to " + device.getName() + " failed", e);
            }

        }

        Log.d(TAG, "after");

        return new GetResponse.Builder(get).failed().build();

    }

    private JSONObject createGetJson(Get get) throws JSONException {

        JSONObject jo = new JSONObject();

        jo.put("type", "get");
        jo.put("msgid", get.getId());
        jo.put("hoplimit", get.getHopLimit());
        jo.put("uri", get.getNdo().getUri());

        return jo;

    }



}
