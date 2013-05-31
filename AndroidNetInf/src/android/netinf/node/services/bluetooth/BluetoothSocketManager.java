package android.netinf.node.services.bluetooth;

import java.util.Map;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BluetoothSocketManager {

    // Bluetooth MAC Address -> BluetoothSocket
    private static Map<String, BluetoothSocket> mSockets;

    private static BluetoothSocket getSocket(BluetoothDevice device) {

        if (mSockets.containsKey(device.getAddress()) {

        }

    }

}
