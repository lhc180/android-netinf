package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfStatus;
import android.netinf.common.NetInfUtils;
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
        Log.i(TAG, "Bluetooth CL received SEARCH: " + search);

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

                // Receive
                return parseSearchResponse(search, in);

            } catch (IOException e) {
                Log.e(TAG, "SEARCH to " + device.getName() + " failed", e);
            } catch (JSONException e) {
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

    private SearchResponse parseSearchResponse(Search search, DataInputStream in) throws IOException, JSONException {

        JSONObject jo = BluetoothCommon.readJson(in);

        if (jo.getInt("status") != NetInfStatus.OK.getCode()) {
            return new SearchResponse.Builder(search).build();
        }

        SearchResponse.Builder responseBuilder = new SearchResponse.Builder(search);

        JSONArray results = jo.getJSONArray("results");
        for (int i = 0; i < results.length(); i++) {
            try {
                Ndo.Builder ndoBuilder = NetInfUtils.toNdoBuilder(results.getJSONObject(i).getString("ni"));
                ndoBuilder.metadata(new Metadata(results.getJSONObject(i).getString("metadata")));
                Ndo ndo = ndoBuilder.build();
                responseBuilder.addResult(ndo);
            } catch (JSONException e) {
                Log.w(TAG, "Skipped invalid search result", e);
            } catch (NetInfException e) {
                Log.w(TAG, "Skipped invalid search result", e);
            }
        }

        return responseBuilder.build();

    }

}
