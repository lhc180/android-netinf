package android.netinf.node.services.bluetooth;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothSocket;
import android.netinf.common.Locator;
import android.netinf.common.Metadata;
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

    private ExecutorService mMessageExecutor = Executors.newCachedThreadPool();
    private BluetoothSocketManager mManager;
    private BluetoothApi mApi;
    private BluetoothSocket mSocket;
    private Exception mException;

    public BluetoothSocketHandler(BluetoothSocketManager manager, BluetoothApi api, BluetoothSocket socket) {
        mManager = manager;
        mApi = api;
        mSocket = socket;
    }

    @Override
    public void run() {

        try {
            while (mException == null) {
                handleMessage();
            }
            Log.e(TAG, "Failed to handle request", mException);
        } catch (IOException e) {
            Log.e(TAG, "Failed to handle request", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to handle request", e);
        } catch (NetInfException e) {
            Log.e(TAG, "Failed to handle request", e);
        } finally {
            mManager.removeSocket(mSocket);
            IOUtils.closeQuietly(mSocket);
        }

    }

    public void setException(Exception exception) {
        mException = exception;
    }

    private void handleMessage() throws IOException, JSONException, NetInfException {

        // Read the JSON part of the next message
        JSONObject message = BluetoothCommon.readJson(mSocket);

        // Switch on message type
        String type = message.getString("type");
        if (type.equals("publish")) {
            handlePublish(message);
        } else if (type.equals("get")) {
            handleGet(message);
        } else if (type.equals("search")) {
            handleSearch(message);
        } else if (type.equals("publish-response")) {
            handlePublishResponse(message);
        } else if (type.equals("get-response")) {
            handleGetResponse(message);
        } else if (type.equals("search-response")) {
            handleSearchResponse(message);
        } else {
            Log.wtf(TAG, "Bluetooth API received UNKNOWN: " + type);
        }
    }


    private void handlePublish(JSONObject jo) throws IOException, NetInfException, JSONException {

        // Convert JSON to Publish

        // Create Ndo from JSON
        Ndo ndo = NetInfUtils.toNdo(jo);
        Log.i(TAG, "Bluetooth API received PUBLISH: " + ndo.getUri());

        // Start creating Publish
        Publish.Builder publishBuilder = new Publish.Builder(mApi, ndo)
        .id(jo.getString("msgid"))
        .hoplimit(jo.getInt("hoplimit"));

        // Handle the fullput case
        if (jo.getBoolean("octets") == true) {
            byte[] octets = BluetoothCommon.readFile(mSocket);
            ndo.cache(octets);
            publishBuilder.fullPut();
        }

        // Build the publish
        Publish publish = publishBuilder.build();

        // Submit the Publish for execution
        Future<PublishResponse> future = Node.submit(publish);

        // Write the result (asynchronously)
        writePublishResponse(future);

    }

    private void writePublishResponse(final Future<PublishResponse> future) {

        mMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Wait for PublishResponse with timeout
                    PublishResponse publishResponse = future.get(BluetoothCommon.getTimeout(), BluetoothCommon.TIMEOUT_UNIT);

                    // Convert PublishResponse into JSON and send it
                    JSONObject responseJo = new JSONObject();
                    responseJo.put("type", "publish-response");
                    responseJo.put("msgid", publishResponse.getId());
                    responseJo.put("status", publishResponse.getStatus().getCode());
                    BluetoothCommon.write(responseJo, mSocket);

                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to handle PUBLISH", e);
                } catch (ExecutionException e) {
                    Log.e(TAG, "Failed to handle PUBLISH", e);
                } catch (TimeoutException e) {
                    Log.e(TAG, "Failed to handle PUBLISH", e);
                } catch (IOException e) {
                    // Information sent over socket is incorrect
                    setException(e);
                } catch (JSONException e) {
                    // Information sent over socket is incorrect
                    setException(e);
                }
            }
        });
    }

    private void handleGet(JSONObject jo) throws IOException, NetInfException, JSONException {
        // Convert JSON to Get

        // Create Ndo from JSON
        Ndo ndo = NetInfUtils.toNdo(jo);

        // Create and execute Get
        Get get = new Get.Builder(mApi, ndo)
        .id(jo.getString("msgid"))
        .hoplimit(jo.getInt("hoplimit"))
        .build();

        Log.i(TAG, "Bluetooth API received GET: " + get);

        // Submit the Get for execution
        Future<GetResponse> future = Node.submit(get);

        // Write the result (asynchronously)
        writeGetResponse(get, future);

    }

    private void writeGetResponse(final Get get, final Future<GetResponse> future) {

        mMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Wait for GetResponse with timeout
                    GetResponse getResponse = future.get(BluetoothCommon.getTimeout(), BluetoothCommon.TIMEOUT_UNIT);

                    // Convert GetResponse into JSON and send it
                    JSONObject jo = new JSONObject();
                    jo.put("type", "get-response");
                    jo.put("msgid", getResponse.getId());

                    if (getResponse.getStatus().isError()) {
                        // If Get failed
                        jo.put("status", NetInfStatus.FAILED.getCode());
                        jo.put("uri", get.getNdo().getCanonicalUri());
                        BluetoothCommon.write(jo, mSocket);
                    } else {
                        // If Get succeeded
                        jo.put("status", NetInfStatus.OK.getCode());
                        jo.put("uri", getResponse.getNdo().getCanonicalUri());
                        if (get.getNdo().isCached()) {
                            // If cached, add octets
                            jo.put("octets", true);
                            BluetoothCommon.write(jo, get.getNdo().getOctets(), mSocket);
                        } else {
                            // Otherwise, add locators
                            JSONArray locators = new JSONArray();
                            for (Locator locator : get.getNdo().getLocators()) {
                                locators.put(locator.toString());
                            }
                            BluetoothCommon.write(jo, mSocket);
                        }
                    }

                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to handle GET", e);
                } catch (ExecutionException e) {
                    Log.e(TAG, "Failed to handle GET", e);
                } catch (TimeoutException e) {
                    Log.e(TAG, "Failed to handle GET", e);
                } catch (IOException e) {
                    // Information sent over socket is incorrect
                    setException(e);
                } catch (JSONException e) {
                    // Information sent over socket is incorrect
                    setException(e);
                }
            }
        });




    }

    private void writeSearchResponse(final Future<SearchResponse> future) {

        mMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Wait for SearchResponse with timeout
                    SearchResponse searchResponse = future.get(BluetoothCommon.getTimeout(), BluetoothCommon.TIMEOUT_UNIT);

                    // Convert SearchResponse into JSON
                    JSONObject responseJo = new JSONObject();
                    responseJo.put("type", "search-response");
                    responseJo.put("msgid", searchResponse.getId());

                    JSONArray results = new JSONArray();
                    responseJo.put("results", results);

                    if (searchResponse.getStatus().isError()) {
                        // If Search failed
                        responseJo.put("status", NetInfStatus.FAILED.getCode());
                        BluetoothCommon.write(responseJo, mSocket);
                    } else {
                        // If Search succeeded
                        responseJo.put("status", NetInfStatus.OK.getCode());
                        for (Ndo ndo : searchResponse.getResults()) {
                            JSONObject result = new JSONObject();
                            result.put("ni", ndo.getCanonicalUri());
                            result.put("metadata", ndo.getMetadata().toJson());
                        }
                    }

                    BluetoothCommon.write(responseJo, mSocket);

                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to handle GET", e);
                } catch (ExecutionException e) {
                    Log.e(TAG, "Failed to handle GET", e);
                } catch (TimeoutException e) {
                    Log.e(TAG, "Failed to handle GET", e);
                } catch (IOException e) {
                    // Information sent over socket is incorrect
                    setException(e);
                } catch (JSONException e) {
                    // Information sent over socket is incorrect
                    setException(e);
                }
            }
        });
    }

    private void handleSearch(JSONObject jo) throws IOException, JSONException {

        // Convert JSON to Search

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

        // Submit the Search for execution
        Future<SearchResponse> future = Node.submit(search);

        // Write the result (asynchronously)
        writeSearchResponse(future);

    }

    private void handlePublishResponse(JSONObject jo) throws IOException {

        try {

            String id = jo.getString("msgid");
            NetInfStatus status = NetInfStatus.valueOf(jo.getInt("status"));
            mManager.addResponse(new PublishResponse.Builder(id).status(status).build());

        } catch (JSONException e) {
            throw new IOException("Failed to handle PUBLISH-RESP", e);
        }

    }

    private void handleGetResponse(JSONObject jo) throws IOException {

        try {

            String id = jo.getString("msgid");
            NetInfStatus status = NetInfStatus.valueOf(jo.getInt("status"));
            String uri = jo.getString("uri");
            String algorithm = NetInfUtils.getAlgorithm(uri);
            String hash = NetInfUtils.getHash(uri);

            if (status.isError()) {
                mManager.addResponse(new GetResponse.Builder(id).failed().build());
            }

            Ndo.Builder builder = new Ndo.Builder(algorithm, hash);

            if (jo.has("locators")){
                JSONArray locators = jo.getJSONArray("locators");
                for (int i = 0; i < locators.length(); i++) {
                    builder.addLocator(Locator.fromString(locators.getString(i)));
                }
            }

            Ndo ndo = builder.build();

            if (jo.has("octets") && jo.getBoolean("octets")) {
                byte[] octets = BluetoothCommon.readFile(mSocket);
                ndo.cache(octets);
            }

            GetResponse getResponse = new GetResponse.Builder(id).ok(ndo).build();
            mManager.addResponse(getResponse);

        } catch (JSONException e) {
            throw new IOException("Failed to handle GET-RESP", e);
        } catch (NetInfException e) {
            throw new IOException("Failed to handle GET-RESP", e);
        }

    }

    private void handleSearchResponse(JSONObject jo) throws IOException {

        try {

            String id = jo.getString("msgid");
            NetInfStatus status = NetInfStatus.valueOf(jo.getInt("status"));

            if (status.isError()) {
                mManager.addResponse(new SearchResponse.Builder(id).build());
            }

            SearchResponse.Builder responseBuilder = new SearchResponse.Builder(id);

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

            mManager.addResponse(responseBuilder.build());

        } catch (JSONException e) {
            throw new IOException("Failed to handle SEARCH-RESP", e);
        }

    }









    private class MessageHandler implements Runnable {

        private final BluetoothSocketHandler mCaller;
        private final JSONObject mMessage;

        public MessageHandler(BluetoothSocketHandler caller, JSONObject message) {
            mCaller = caller;
            mMessage = message;
        }

        @Override
        public void run() {

        }



    }


}
