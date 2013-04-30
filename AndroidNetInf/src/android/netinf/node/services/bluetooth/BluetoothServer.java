package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
import android.netinf.node.Node;
import android.netinf.node.get.Get;
import android.netinf.node.get.GetResponse;
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishResponse;
import android.netinf.node.search.Search;
import android.netinf.node.search.SearchResponse;
import android.util.Log;


public class BluetoothServer implements Runnable {

    public static final String TAG = BluetoothServer.class.getSimpleName();

    private BluetoothApi mApi;
    private UUID mUuid;

    public BluetoothServer(BluetoothApi api, UUID uuid) {
        mApi = api;
        mUuid = uuid;
    }

    @Override
    public void run() {

        try {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        while (true) {

            // Reset
            BluetoothSocket socket = null;
            DataInputStream in = null;
            DataOutputStream out = null;

            try {
                Log.d(TAG, "listen");
                BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("android.netinf", mUuid);
                Log.i(TAG, adapter.getName() + " waiting for connections using UUID " + mUuid);
                Log.d(TAG, "accept");
                socket = server.accept();
                Log.i(TAG, adapter.getName() + " accepted a connection using UUID " + mUuid);
                Log.d(TAG, "close");
                server.close();
                server = null; // Trying to fix the Bluetooth resource leakage
                if (socket != null) {
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                    handleRequest(in, out);
                }
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("-1")) {
                    // Workaround for Android 4.2.X Bluetooth Bug
                    Log.e(TAG, "(Debug) Failed to restart server because of Android 4.2.X bug");
                    BluetoothFix.needFix();
                } else {
                    Log.e(TAG, "Failed to handle request", e);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to handle request", e);
            } catch (NetInfException e) {
                Log.e(TAG, "Failed to handle request", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to handle request", e);
            } catch (ExecutionException e) {
                Log.e(TAG, "Failed to handle request", e);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(socket);
            }

        }

        //Log.i(TAG, adapter.getName() + " no longer waiting for connections using UUID " + mUuid);

        } catch (Throwable e) {
            Log.wtf(TAG, "THIS SHOULD NOT HAPPEN D:", e);
        }

    }

    private void handleRequest(DataInputStream in, DataOutputStream out)
            throws IOException, JSONException, NetInfException, InterruptedException, ExecutionException {
        Log.v(TAG, "handleRequest()");

        JSONObject request = BluetoothCommon.readJson(in);
        if (request.getString("type").equals("publish")) {
            handlePublish(in, out, request);
        } else if (request.getString("type").equals("get")) {
            handleGet(in, out, request);
        } else if (request.getString("type").equals("search")) {
            handleSearch(in, out, request);
        } else {
            Log.wtf(TAG, "Bluetooth API received UNKNOWN: " + request.getString("type"));
        }

        // TODO WAIT UNTIL WE KNOW RESPONSE READ?!
        BluetoothCommon.readEos(in);
    }

    private void handlePublish(DataInputStream in, DataOutputStream out, JSONObject publishJo)
            throws NetInfException, JSONException, IOException, InterruptedException, ExecutionException {

        Ndo ndo = NetInfUtils.toNdo(publishJo);
        Log.i(TAG, "Bluetooth API received PUBLISH: " + ndo.getUri());

        // Create and execute publish
        Publish.Builder publishBuilder = new Publish.Builder(mApi, ndo)
                .id(publishJo.getString("msgid"))
                .hoplimit(publishJo.getInt("hoplimit"));

        if (publishJo.getBoolean("octets") == true) {
            byte[] octets = BluetoothCommon.readFile(in);
            ndo.cache(octets);
            publishBuilder.fullPut();
        }

        Publish publish = publishBuilder.build();
        PublishResponse response = Node.submit(publish).get();

        // Create publish response
        JSONObject responseJo = new JSONObject();
        responseJo.put("msgid", response.getId());
        responseJo.put("status", response.getStatus().getCode());
        BluetoothCommon.write(responseJo, out);

    }

    private void handleGet(DataInputStream in, DataOutputStream out, JSONObject jo)
            throws NetInfException, JSONException, IOException, InterruptedException, ExecutionException {
        Log.v(TAG, "handleGet()");

        Ndo ndo = NetInfUtils.toNdo(jo);
        Log.i(TAG, "Bluetooth API received GET: " + ndo.getUri());

        // Create and execute get
        Get get = new Get.Builder(mApi, ndo)
                .id(jo.getString("msgid"))
                .hoplimit(jo.getInt("hoplimit"))
                .build();
        GetResponse response = Node.submit(get).get();

        // Create get response
        JSONObject responseJo = new JSONObject();
        responseJo.put("msgid", response.getId());

        if (response.getStatus().isError()) {
            responseJo.put("status", NetInfStatus.FAILED.getCode());
            BluetoothCommon.write(responseJo, out);
            return;
        }

        responseJo.put("status", NetInfStatus.OK.getCode());
        if (ndo.isCached()) {
            responseJo.put("octets", true);
            BluetoothCommon.write(responseJo, out);
            BluetoothCommon.write(ndo.getOctets(), out);
        } else {
            JSONArray locators = new JSONArray();
            for (Locator locator : ndo.getLocators()) {
                locators.put(locator.toString());
            }
            BluetoothCommon.write(responseJo, out);
        }

    }

    private void handleSearch(DataInputStream in, DataOutputStream out, JSONObject jo)
            throws JSONException, InterruptedException, ExecutionException, IOException {

        // Create and execute search
        Search.Builder searchBuilder = new Search.Builder(mApi)
                .id(jo.getString("msgid"))
                .hoplimit(jo.getInt("hoplimit"));

        JSONArray tokens = jo.getJSONArray("tokens");
        for (int i = 0; i < tokens.length(); i++) {
            searchBuilder.token(tokens.getString(i));
        }

        Search search = searchBuilder.build();

        Log.i(TAG, "Bluetooth API received SEARCH: " + search);

        SearchResponse response = Node.submit(search).get();

        // Create search response
        JSONObject responseJo = new JSONObject();
        responseJo.put("msgid", response.getId());

        JSONArray results = new JSONArray();
        responseJo.put("results", results);

        if (response.getStatus().isError()) {
            responseJo.put("status", NetInfStatus.FAILED.getCode());
            BluetoothCommon.write(responseJo, out);
            return;
        }

        responseJo.put("status", NetInfStatus.OK.getCode());

        for (Ndo ndo : response.getResults()) {
            JSONObject result = new JSONObject();
            result.put("ni", ndo.getCanonicalUri());
            result.put("metadata", ndo.getMetadata().toJson());
        }

        BluetoothCommon.write(responseJo, out);

    }

}
