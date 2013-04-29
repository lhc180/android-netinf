package android.netinf.node.services.bluetooth;

import java.util.concurrent.CountDownLatch;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

public class BluetoothRepair {

    public static final String TAG = BluetoothRepair.class.getSimpleName();

    private enum State {
        OK,
        FIX_IN_PROGRESS
    }

    private static State mState = State.OK;
    private static CountDownLatch mSignal;

    public static void needFix() {

        CountDownLatch signal;

        synchronized (mState) {
            if (mState == State.OK) {
                mState = State.FIX_IN_PROGRESS;
                mSignal = new CountDownLatch(1);
                signal = mSignal;
                startFix();
            } else {
                signal = mSignal;
            }
        }

        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.wtf(TAG, "Bluetooth fix interrupted", e);
        }


    }

    private static void startFix() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "(Debug) Fixing Bluetooth (Android 4.2.x Bug)...");
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                Log.d(TAG, "(Debug) Disabling Bluetooth...");
                adapter.cancelDiscovery();
                adapter.disable();
                while (adapter.isEnabled()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Log.wtf(TAG, "Sleep interrupted", e);
                    }
                }

                // Enable Bluetooth
                Log.d(TAG, "(Debug) Enabling Bluetooth...");
                adapter.enable();
                while (!adapter.isEnabled()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Log.wtf(TAG, "Sleep interrupted", e);
                    }
                }

                synchronized (mState) {
                    mState = State.OK;
                    mSignal.countDown();
                }
                Log.d(TAG, "(Debug) Bluetooth repair complete!");
            }
        }).start();

    }






    //  private enum State {
    //  OK,
    //  FIX_NEEDED,
    //  FIX_IN_PROGRESS
    //}
    //
    //private static final int NUM_SERVERS = 5;
    //
    //private static int mWaiting = 0;
    //private static State mState = State.OK;
    //private static CountDownLatch mSignal;
    //
    //public static void scheduleFix() {
    //
    //  synchronized (mState) {
    //      if (mState == State.OK) {
    //          Log.d(TAG, "(Debug) Bluetooth fix scheduled");
    //          mState = State.FIX_NEEDED;
    //          mWaiting = 0;
    //          mSignal = new CountDownLatch(1);
    //      }
    //  }
    //
    //}
    //
    //public static void awaitFix() {
    //
    //  CountDownLatch signal;
    //
    //  synchronized (mState) {
    //
    //      if (mState == State.OK) {
    //          return;
    //      }
    //
    //      mWaiting++;
    //      signal = mSignal;
    //
    //      Log.d(TAG, "(Debug) " + mWaiting + "/" + NUM_SERVERS + " servers waiting for Bluetooth fix");
    //
    //      if (mState == State.FIX_NEEDED && mWaiting == NUM_SERVERS) {
    //          startFix();
    //      }
    //
    //  }
    //
    //  try {
    //      signal.await();
    //  } catch (InterruptedException e) {
    //      Log.wtf(TAG, "Bluetooth fix interrupted", e);
    //  }
    //
    //}

    //
    //    /**
    //     * Asynchronous workaround for Bluetooth resource leakage introduced in Android 4.2.x.
    //     */
    //    private boolean mRepairing = false;
    //    public synchronized void repairBluetooth() {
    //
    //        if(mRepairing) {
    //            return;
    //        }
    //        mRepairing = true;
    //
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                Log.d(TAG, "(Debug) Repairing Bluetooth (Android 4.2.x resource leak)...");
    //
    //                // Kill current servers
    ////                Log.d(TAG, "(Debug) Shutting down Bluetooth servers...");
    ////                mServerExecutor.shutdownNow();
    //
    //                // Disable Bluetooth
    //                Log.d(TAG, "(Debug) Disabling Bluetooth...");
    //                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    //                adapter.cancelDiscovery();
    //                adapter.disable();
    //                while (adapter.isEnabled()) {
    //                    try {
    //                        Thread.sleep(50);
    //                    } catch (InterruptedException e) {
    //                        Log.wtf(TAG, "Sleep interrupted", e);
    //                    }
    //                }
    //
    //                // Enable Bluetooth
    //                Log.d(TAG, "(Debug) Enabling Bluetooth...");
    //                adapter.enable();
    //                while (!adapter.isEnabled()) {
    //                    try {
    //                        Thread.sleep(50);
    //                    } catch (InterruptedException e) {
    //                        Log.wtf(TAG, "Sleep interrupted", e);
    //                    }
    //                }
    //
    ////                Log.d(TAG, "(Debug) Restarting Bluetooth servers...");
    ////                mServerExecutor = Executors.newFixedThreadPool(BluetoothCommon.UUIDS.size());
    ////                for (UUID uuid : BluetoothCommon.UUIDS) {
    ////                    mServerExecutor.execute(new BluetoothServer(BluetoothApi.this, uuid));
    ////                }
    //
    //                mRepairing = false;
    //                Log.d(TAG, "(Debug) Bluetooth repair complete!");
    //            }
    //        }).start();
    //
    //    }

}
