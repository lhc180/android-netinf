package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothSocket;
import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfStatus;
import android.netinf.common.NetInfUtils;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.Node;
import android.util.Log;

public class BluetoothSocketHandler implements Runnable {

    public static final String TAG = BluetoothSocketHandler.class.getSimpleName();

    private BluetoothApi mApi;
    private BluetoothSocket mSocket;

    public BluetoothSocketHandler(BluetoothApi api, BluetoothSocket socket) {
        mApi = api;
        mSocket = socket;
    }

    @Override
    public void run() {

        DataInputStream in = null;
        DataOutputStream out = null;
        try {

            in = new DataInputStream(mSocket.getInputStream());
            out = new DataOutputStream(mSocket.getOutputStream());
            handleRequest(in, out);

        } catch (IOException e) {

            Log.e(TAG, "Failed to handle request", e);

        } finally {

            IOUtils.closeQuietly(mSocket);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);

        }

    }

    private void handleRequest(DataInputStream in, DataOutputStream out)
            throws IOException {
        Log.v(TAG, "handleRequest()");

        try {
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

            // TODO is this enough? is this sane? wait for socket closed instead?
            BluetoothCommon.readConfirmation(in);

        } catch (JSONException e) {
            throw new IOException("Failed to get type from JSON", e);
        }

    }

    private void handlePublish(DataInputStream in, DataOutputStream out, JSONObject publishJo)
            throws IOException {

        try {

            // Create Ndo from JSON
            Ndo ndo = NetInfUtils.toNdo(publishJo);
            Log.i(TAG, "Bluetooth API received PUBLISH: " + ndo.getUri());

            // Start creating Publish
            Publish.Builder publishBuilder = new Publish.Builder(mApi, ndo)
            .id(publishJo.getString("msgid"))
            .hoplimit(publishJo.getInt("hoplimit"));

            // Handle the fullput case
            if (publishJo.getBoolean("octets") == true) {
                byte[] octets = BluetoothCommon.readFile(in);
                ndo.cache(octets);
                publishBuilder.fullPut();
            }

            // Finish and execute the Publish
            // TODO add timeout
            Publish publish = publishBuilder.build();
            PublishResponse response = Node.submit(publish).get();

            // Convert PublishResponse into JSON and send it
            JSONObject responseJo = new JSONObject();
            responseJo.put("msgid", response.getRequest().getId());
            responseJo.put("status", response.getStatus().getCode());
            BluetoothCommon.write(responseJo, out);

        } catch (NetInfException e) {
            throw new IOException("Failed to handle PUBLISH", e);
        } catch (JSONException e) {
            throw new IOException("Failed to handle PUBLISH", e);
        } catch (InterruptedException e) {
            throw new IOException("Failed to handle PUBLISH", e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to handle PUBLISH", e);
        }

    }

    private void handleGet(DataInputStream in, DataOutputStream out, JSONObject jo)
            throws IOException {

        try {

            // Create Ndo from JSON
            Ndo ndo = NetInfUtils.toNdo(jo);
            Log.i(TAG, "Bluetooth API received GET: " + ndo.getUri());

            // Create and execute Get
            Get get = new Get.Builder(mApi, ndo)
            .id(jo.getString("msgid"))
            .hoplimit(jo.getInt("hoplimit"))
            .build();
            GetResponse response = Node.submit(get).get();

            // Convert GetResponse into JSON and send it
            JSONObject responseJo = new JSONObject();
            responseJo.put("msgid", response.getRequest().getId());

            if (response.getStatus().isError()) {
                // If Get failed
                responseJo.put("status", NetInfStatus.FAILED.getCode());
                BluetoothCommon.write(responseJo, out);
            } else {
                // If Get succeeded
                responseJo.put("status", NetInfStatus.OK.getCode());
                if (ndo.isCached()) {
                    // If cached, add octets
                    responseJo.put("octets", true);
                    BluetoothCommon.write(responseJo, out);
                    BluetoothCommon.write(ndo.getOctets(), out);
                } else {
                    // Otherwise, add locators
                    JSONArray locators = new JSONArray();
                    for (Locator locator : ndo.getLocators()) {
                        locators.put(locator.toString());
                    }
                    BluetoothCommon.write(responseJo, out);
                }
            }

        } catch (NetInfException e) {
            throw new IOException("Failed to handle GET", e);
        } catch (JSONException e) {
            throw new IOException("Failed to handle GET", e);
        } catch (InterruptedException e) {
            throw new IOException("Failed to handle GET", e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to handle GET", e);
        }

    }

    private void handleSearch(DataInputStream in, DataOutputStream out, JSONObject jo)
            throws IOException {

        try {

            // Start creating the Search
            Search.Builder searchBuilder = new Search.Builder(mApi)
            .id(jo.getString("msgid"))
            .hoplimit(jo.getInt("hoplimit"));

            // Add all tokens
            JSONArray tokens = jo.getJSONArray("tokens");
            for (int i = 0; i < tokens.length(); i++) {
                searchBuilder.token(tokens.getString(i));
            }

            // Finish the Search
            Search search = searchBuilder.build();
            Log.i(TAG, "Bluetooth API received SEARCH: " + search);

            // Execute the Search
            SearchResponse response = Node.submit(search).get();

            // Convert SearchResponse into JSON
            JSONObject responseJo = new JSONObject();
            responseJo.put("msgid", response.getRequest().getId());

            JSONArray results = new JSONArray();
            responseJo.put("results", results);

            if (response.getStatus().isError()) {

                // If Search failed
                responseJo.put("status", NetInfStatus.FAILED.getCode());
                BluetoothCommon.write(responseJo, out);

            } else {

                // If Search succeeded
                responseJo.put("status", NetInfStatus.OK.getCode());

                for (Ndo ndo : response.getResults()) {
                    JSONObject result = new JSONObject();
                    result.put("ni", ndo.getCanonicalUri());
                    result.put("metadata", ndo.getMetadata().toJson());
                }

                BluetoothCommon.write(responseJo, out);

            }

        } catch (JSONException e) {
            throw new IOException("Failed to handle SEARCH", e);
        } catch (InterruptedException e) {
            throw new IOException("Failed to handle SEARCH", e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to handle SEARCH", e);
        }

    }

}
