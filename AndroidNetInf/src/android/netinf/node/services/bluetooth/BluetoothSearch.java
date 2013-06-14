package android.netinf.node.services.bluetooth;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.search.SearchService;
import android.util.Log;

public class BluetoothSearch implements SearchService {

    public static final String TAG = BluetoothSearch.class.getSimpleName();

    private BluetoothApi mApi;

    public BluetoothSearch(BluetoothApi api) {
        mApi = api;
    }

    @Override
    public SearchResponse perform(Search search) {
        Log.i(TAG, "Bluetooth SEARCH " + search);

        // Create JSON representation of Search
        JSONObject jo = null;
        try {
            jo = createSearchJson(search);
        } catch (JSONException e) {
            Log.wtf(TAG, "Failed to create JSON representation of Search", e);
            return new SearchResponse.Builder(search).build();
        }

        // Search to all relevant devices
        for (BluetoothDevice device : mApi.getBluetoothDevices()) {

            try {

                // Connect
                BluetoothSocket socket = mApi.getManager().getSocket(device);

                // Send
                BluetoothCommon.write(jo, socket);

                // Receive
                return mApi.getManager().getResponse(search).get(BluetoothCommon.getTimeout(), BluetoothCommon.TIMEOUT_UNIT);

            } catch (IOException e) {
                Log.e(TAG, "SEARCH to " + device.getName() + " failed", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "SEARCH to " + device.getName() + " failed", e);
            } catch (ExecutionException e) {
                Log.e(TAG, "SEARCH to " + device.getName() + " failed", e);
            } catch (TimeoutException e) {
                Log.e(TAG, "SEARCH to " + device.getName() + " failed", e);
            }
        }

        return new SearchResponse.Builder(search).build();

    }

    private JSONObject createSearchJson(Search search) throws JSONException {

            JSONObject jo = new JSONObject();

            jo.put("type", "search");
            jo.put("msgid", search.getId());
            jo.put("hoplimit", search.getHopLimit());

            JSONArray tokens = new JSONArray();
            for (String token : search.getTokens()) {
                tokens.put(token);
            }
            jo.put("tokens", tokens);

            return jo;
    }

}
