package android.netinf.node.services.bluetooth;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.netinf.node.api.Api;

public class BluetoothApi implements Api {

    public static final String TAG = BluetoothApi.class.getSimpleName();

    private ExecutorService mServerExecutor;
    private ScheduledExecutorService mDiscoveryExecutor;
    private BluetoothDiscovery mBluetoothDiscovery;

    public BluetoothApi(Context context) {

        mServerExecutor = Executors.newFixedThreadPool(BluetoothCommon.UUIDS.size());
        mDiscoveryExecutor = Executors.newSingleThreadScheduledExecutor();
        mBluetoothDiscovery = new BluetoothDiscovery(context);

    }

    public Set<BluetoothDevice> getBluetoothDevices() {
        return mBluetoothDiscovery.getBluetoothDevices();
    }

    @Override
    public void start() {
        // TODO enable bluetooth discovery when relevant
        mDiscoveryExecutor.scheduleWithFixedDelay(mBluetoothDiscovery, 0, BluetoothDiscovery.DELAY, TimeUnit.MILLISECONDS);
        for (UUID uuid : BluetoothCommon.UUIDS) {
            // TODO make certain UUIDs are restarted if thread crashes?
            mServerExecutor.execute(new BluetoothServer(this, uuid));
        }
    }

    @Override
    public void stop() {
        // TODO clean up stuff properly
        mDiscoveryExecutor.shutdown();
        mServerExecutor.shutdown();
    }



}
