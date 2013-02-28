package android.netinf.node.services.bluetooth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.netinf.node.api.Api;

public class BluetoothApi implements Api {

    Stack<UUID> mAvailableUuids;
    Set<UUID> mAllUuids;
    ExecutorService mServerExecutor;
    ScheduledExecutorService mDiscoveryExecutor;
    BluetoothDiscovery mBluetoothDiscovery;

    public BluetoothApi(Context context) {

        mAvailableUuids = new Stack<UUID>();
        mAvailableUuids.push(UUID.fromString("111a8500-6ae2-11e2-bcfd-0800200c9a66"));
        mAvailableUuids.push(UUID.fromString("111a8501-6ae2-11e2-bcfd-0800200c9a66"));
        mAvailableUuids.push(UUID.fromString("111a8502-6ae2-11e2-bcfd-0800200c9a66"));
        mAvailableUuids.push(UUID.fromString("111a8503-6ae2-11e2-bcfd-0800200c9a66"));
        mAvailableUuids.push(UUID.fromString("111a8504-6ae2-11e2-bcfd-0800200c9a66"));

        mAllUuids = Collections.unmodifiableSet(new HashSet<UUID>(mAvailableUuids));

        mServerExecutor = Executors.newFixedThreadPool(mAvailableUuids.size());

        mDiscoveryExecutor = Executors.newSingleThreadScheduledExecutor();

        mBluetoothDiscovery = new BluetoothDiscovery(context);

    }

    public Set<BluetoothDevice> getBluetoothDevices() {
        return mBluetoothDiscovery.getBluetoothDevices();
    }

    public Set<UUID> getUuids() {
        return mAllUuids;
    }

    public UUID getUuid() {
        return mAvailableUuids.pop();
    }

    public void returnUuid(UUID uuid) {
        mAvailableUuids.push(uuid);
        mServerExecutor.execute(new BluetoothServer(this));
    }

    @Override
    public void start() {
//        mDiscoveryExecutor.scheduleWithFixedDelay(mBluetoothDiscovery, 0, BluetoothDiscovery.DELAY, TimeUnit.MILLISECONDS);
        for (int i = 0; i < mAllUuids.size(); i++) {
            // TODO make certain UUIDs are returned if thread crashes
            mServerExecutor.execute(new BluetoothServer(this));
        }
    }

    @Override
    public void stop() {
        // TODO clean up stuff properly
        mDiscoveryExecutor.shutdown();
        mServerExecutor.shutdown();
    }



}
