package android.netinf.node.services.bluetooth;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothSocketManager {

    public static final String TAG = BluetoothSocketManager.class.getSimpleName();

    // Bluetooth MAC Address -> BluetoothSocket
    private static Map<String, BluetoothSocket> mSockets;

    private static BluetoothSocket getSocket(BluetoothDevice device) throws IOException {

        if (mSockets.containsKey(device.getAddress())) {

        }
        return mSockets.get(device.getAddress());

    }

    private static void reconnect(BluetoothDevice device) throws IOException {

        if (mSockets.containsKey(device.getAddress())) {
            IOUtils.closeQuietly(mSockets.get(device.getAddress()));
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothSocket socket = null;
        try {
            // Connect to remote device
            Log.d(TAG, "createRfcommSocketToServiceRecord");
            socket = device.createRfcommSocketToServiceRecord(BluetoothApi.NETINF_UUID);
            Log.d(TAG, "after createRfcommSocketToServiceRecord");
            adapter.cancelDiscovery();
            Log.d(TAG, "connect");
            socket.connect();
            Log.d(TAG, "after connect");
        } catch (IOException e) {
            IOUtils.closeQuietly(socket); // Shouldn't be necessary, but who knows?
            throw new IOException(adapter.getName() + " failed to connect to " + device.getName(), e);
        }

        // According to the documentation of BluetoothSocket.connect()
        // "If this method returns without an exception then this socket is now connected."
        // Ended up here with unconnected sockets a bit to often
        // Of course the socket could have died unexpectedly
        if (!socket.isConnected()) {
            IOUtils.closeQuietly(socket);
            throw new IOException(adapter.getName() + " failed to connect to " + device.getName() + ": socket is not connected");
        } else {
            mSockets.put(device.getName(), socket);
        }

    }

    private static send

}
