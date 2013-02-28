package android.netinf.node.services.bluetooth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothDiscovery implements Runnable {

    public static final String TAG = "BluetoothDiscovery";

    /** How often to run the Bluetooth discovery. */
    public static final int DELAY = 600000;
    /** If you haven't seen a device for this long, remove it. */
    public static final int TOO_OLD = 6000000;

    private Context mContext;
    private Map<String, SeenDevice> mSeenDevices;
    private Map<String, SeenDevice> mNewDevices;

    public BluetoothDiscovery(Context context) {

        mContext = context;
        mSeenDevices = new HashMap<String, SeenDevice>();
        mNewDevices = new HashMap<String, SeenDevice>();
        registerBroadcastReceiver();

    }

    private static class SeenDevice {

        private static DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
        private BluetoothDevice mDevice;
        private Date mSeen;

        public SeenDevice(BluetoothDevice device, Date seen) {
            mDevice = device;
            mSeen = seen;
        }

        @Override
        public String toString() {
            return "{" + mDevice.getName() + ", " + mDevice.getAddress() + ", " + DATE_FORMAT.format(mSeen) + "}";
        }

    }

    @Override
    public void run() {

        try {
            doBluetoothDiscovery();
        } catch (Throwable e) {
            Log.wtf(TAG, "errorz", e);
        }

    }

    private void doBluetoothDiscovery() {

        Log.i(TAG, "Bluetooth discovery starting...");
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        updateDevices();
        BluetoothAdapter.getDefaultAdapter().startDiscovery();

    }

    private void updateDevices() {

        // Create new map, change reference when done
        Map<String, SeenDevice> updatedSeen = new HashMap<String, SeenDevice>(mSeenDevices);

        // Remove old devices
        Date now = new Date();
        Set<String> toRemove = new HashSet<String>();
        for (String address : mSeenDevices.keySet()) {
            SeenDevice seenDevice = mSeenDevices.get(address);
            if (now.getTime() - seenDevice.mSeen.getTime() > TOO_OLD) {
                toRemove.add(address);
            }
        }
        for (String address : toRemove) {
            updatedSeen.remove(address);
        }

        // Add new devices
        updatedSeen.putAll(mNewDevices);

        mNewDevices = new HashMap<String, SeenDevice>();
        mSeenDevices = updatedSeen;

    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Found BluetoothDevice, add it to the new list
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.v(TAG, "Found Bluetooth device: " + device.getName() + ", " + device.getAddress());
                mNewDevices.put(device.getAddress(), new SeenDevice(device, new Date()));

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                // Discover done, change new and old lists
                updateDevices();
                Log.i(TAG, "Bluetooth discovery done: " + mSeenDevices.values());

            }

        }
    };

    public Set<BluetoothDevice> getBluetoothDevices() {
        // TODO DEBUG, return bonded devices
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            Log.d(TAG, "bonded: " + device.getName());
        }
        return BluetoothAdapter.getDefaultAdapter().getBondedDevices();

//        Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>();
//        for (SeenDevice seenDevice : mSeenDevices.values()) {
//            // TODO DEBUG, only return TPA-* devices
//            if (seenDevice.mDevice.getName().startsWith("TPA-")) {
//                devices.add(seenDevice.mDevice);
//            }
//        }
//        return devices;
    }

}
