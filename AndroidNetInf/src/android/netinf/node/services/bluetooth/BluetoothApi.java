package android.netinf.node.services.bluetooth;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.netinf.node.api.Api;

public class BluetoothApi implements Api {

    public static final String TAG = BluetoothApi.class.getSimpleName();

    public static final UUID NETINF_UUID = UUID.fromString("111a8500-6ae2-11e2-bcfd-0800200c9a66");

    private ExecutorService mServerExecutor = Executors.newSingleThreadExecutor();
    private BluetoothSocketManager mManager = new BluetoothSocketManager(this);

    private ScheduledExecutorService mDiscoveryExecutor = Executors.newSingleThreadScheduledExecutor();
    private BluetoothDiscovery mBluetoothDiscovery;

    public BluetoothApi(Context context) {
        mBluetoothDiscovery = new BluetoothDiscovery(context);
    }

    public Set<BluetoothDevice> getBluetoothDevices() {
        return mBluetoothDiscovery.getBluetoothDevices();
    }

    public Set<BluetoothDevice> getAllBluetoothDevices() {
        return mBluetoothDiscovery.getAllBluetoothDevices();
    }

    public BluetoothSocketManager getManager() {
        return mManager;
    }

    @Override
    public void start() {
        // TODO enable bluetooth discovery when relevant
        // mDiscoveryExecutor.scheduleWithFixedDelay(mBluetoothDiscovery, 0, BluetoothDiscovery.DELAY, TimeUnit.MILLISECONDS);
        mServerExecutor.execute(new BluetoothServer(this, NETINF_UUID));
    }

    @Override
    public void stop() {
        // TODO clean up stuff properly
        mDiscoveryExecutor.shutdown();
        mServerExecutor.shutdown();
    }

}
