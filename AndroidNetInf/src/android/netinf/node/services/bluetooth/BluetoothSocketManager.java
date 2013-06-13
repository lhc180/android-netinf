package android.netinf.node.services.bluetooth;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.get.InProgressTracker;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.SettableFuture;

public class BluetoothSocketManager {

    public static final String TAG = BluetoothSocketManager.class.getSimpleName();

    private BluetoothApi mApi;

    private ExecutorService mExecutor = Executors.newCachedThreadPool();
    private BiMap<BluetoothDevice, BluetoothSocket> mSockets = HashBiMap.create();

    private InProgressTracker<Publish, PublishResponse> mPublishes = new InProgressTracker<Publish, PublishResponse>();
    private InProgressTracker<Get, GetResponse> mGets = new InProgressTracker<Get, GetResponse>();
    private InProgressTracker<Search, SearchResponse> mSearches = new InProgressTracker<Search, SearchResponse>();

    public BluetoothSocketManager(BluetoothApi api) {
        mApi = api;
    }

    public synchronized void addSocket(BluetoothSocket socket) {
        // Store socket
        mSockets.put(socket.getRemoteDevice(), socket);
        // Start reading socket
        mExecutor.execute(new BluetoothSocketHandler(this, mApi, socket));
    }

    public synchronized void removeSocket(BluetoothSocket socket) {
        mSockets.inverse().remove(socket);
    }

    public synchronized BluetoothSocket getSocket(BluetoothDevice device) throws IOException {
        if (mSockets.containsKey(device)) {
            return mSockets.get(device);
        } else {
            BluetoothSocket socket = BluetoothCommon.connect(device);
            addSocket(socket);
            return socket;
        }
    }

    // Assumption: Requests are always received before Responses.
    public Future<PublishResponse> getResponse(Publish publish) {
        return mPublishes.newFutureOrInProgress(publish);
    }

    public Future<GetResponse> getResponse(Get get) {
        return mGets.newFutureOrInProgress(get);
    }

    public Future<SearchResponse> getResponse(Search search) {
        return mSearches.newFutureOrInProgress(search);
    }

    public void addResponse(PublishResponse publishResponse) {
        SettableFuture<PublishResponse> future = mPublishes.stopFuture(publishResponse);
        if (future != null) {
            future.set(publishResponse);
        }
    }

    public void addResponse(GetResponse getResponse) {
        SettableFuture<GetResponse> future = mGets.stopFuture(getResponse);
        if (future != null) {
            future.set(getResponse);
        }
    }

    public void addResponse(SearchResponse searchResponse) {
        SettableFuture<SearchResponse> future = mSearches.stopFuture(searchResponse);
        if (future != null) {
            future.set(searchResponse);
        }
    }


//    // Bluetooth MAC Address -> BluetoothSocket
//    private static Map<String, BluetoothSocket> mSockets;
//
//    private static BluetoothSocket getSocket(BluetoothDevice device) throws IOException {
//
//        if (mSockets.containsKey(device.getAddress())) {
//
//        }
//        return mSockets.get(device.getAddress());
//
//    }
//
//    private static void reconnect(BluetoothDevice device) throws IOException {
//
//        if (mSockets.containsKey(device.getAddress())) {
//            IOUtils.closeQuietly(mSockets.get(device.getAddress()));
//        }
//
//        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//
//        BluetoothSocket socket = null;
//        try {
//            // Connect to remote device
//            Log.d(TAG, "createRfcommSocketToServiceRecord");
//            socket = device.createRfcommSocketToServiceRecord(BluetoothApi.NETINF_UUID);
//            Log.d(TAG, "after createRfcommSocketToServiceRecord");
//            adapter.cancelDiscovery();
//            Log.d(TAG, "connect");
//            socket.connect();
//            Log.d(TAG, "after connect");
//        } catch (IOException e) {
//            IOUtils.closeQuietly(socket); // Shouldn't be necessary, but who knows?
//            throw new IOException(adapter.getName() + " failed to connect to " + device.getName(), e);
//        }
//
//        // According to the documentation of BluetoothSocket.connect()
//        // "If this method returns without an exception then this socket is now connected."
//        // Ended up here with unconnected sockets a bit to often
//        // Of course the socket could have died unexpectedly
//        if (!socket.isConnected()) {
//            IOUtils.closeQuietly(socket);
//            throw new IOException(adapter.getName() + " failed to connect to " + device.getName() + ": socket is not connected");
//        } else {
//            mSockets.put(device.getName(), socket);
//        }
//
//    }
//
//    private static send

}
