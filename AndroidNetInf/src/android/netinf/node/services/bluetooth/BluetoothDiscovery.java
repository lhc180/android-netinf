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
import android.netinf.node.SettingsActivity;
import android.util.Log;

public class BluetoothDiscovery implements Runnable {

    public static final String TAG = BluetoothDiscovery.class.getSimpleName();

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
                Log.i(TAG, "Bluetooth device found: " + device.getName() + ", " + device.getAddress());
                mNewDevices.put(device.getAddress(), new SeenDevice(device, new Date()));

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                // Discover done, change new and old lists
                updateDevices();
                Log.i(TAG, "Bluetooth discovery done: " + mSeenDevices.values());

            }

        }
    };

    public Set<BluetoothDevice> getBluetoothDevices() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>();// = adapter.getBondedDevices();

        StringBuilder builder = new StringBuilder();

        // Filter depending of settings
        if (SettingsActivity.getPreference("pref_key_bluetooth_routing").equalsIgnoreCase("Static")) {
            builder.append("Routing Bluetooth to static devices: [");
            // Peers are stored as simple names separated by space
            String[] peers = SettingsActivity.getPreference("pref_key_bluetooth_static_devices").split(" ");
            for (String peer : peers) {
                // Look for peers in seen devices
                for (SeenDevice seenDevice : mSeenDevices.values()) {
                    if (seenDevice.mDevice.getName().equalsIgnoreCase(peer.trim())) {
                        devices.add(seenDevice.mDevice);
                    }
                }
                // Look for peers in bonded devices
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    if (device.getName().equalsIgnoreCase(peer.trim())) {
                        devices.add(device);
                    }
                }
            }
        } else if (SettingsActivity.getPreference("pref_key_bluetooth_routing").equalsIgnoreCase("Bonded")) {
            builder.append("Routing Bluetooth to bonded devices: [");
            devices.addAll(adapter.getBondedDevices());
        } else {
            builder.append("Routing Bluetooth to all discovered devices: [");
            for (SeenDevice seenDevice : mSeenDevices.values()) {
                devices.add(seenDevice.mDevice);
            }
        }

        for (BluetoothDevice device : devices) {
            builder.append(device.getName());
            builder.append(", ");
        }
        if (devices.size() > 0) {
            builder.setLength(builder.length() - 2);
        }
        builder.append("]");
        Log.d(TAG, builder.toString());
        return devices;

    }

}
