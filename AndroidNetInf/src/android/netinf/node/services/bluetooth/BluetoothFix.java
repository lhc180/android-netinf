package android.netinf.node.services.bluetooth;

import java.util.concurrent.CountDownLatch;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

public class BluetoothFix {

    public static final String TAG = BluetoothFix.class.getSimpleName();

    private enum State {
        OK,
        FIX_IN_PROGRESS
    }

    private static Object mLock = new Object();
    private static State mState = State.OK;
    private static CountDownLatch mSignal;

    /**
     * Android 4.2.X introduced a bug where Bluetooth resources are not released properly.
     * This seems to result in BluetoothAdapter.listenUsingRfcommWithServiceRecord(...)
     * and BluetoothSocket.connect() throwing IOExceptions with error code -1.
     * Restarting Bluetooth seems to be the only current work around.
     * http://code.google.com/p/android/issues/detail?id=41110
     */
    public static void needFix(boolean waitUntilFixed) {

        CountDownLatch signal;

        synchronized (mLock) {
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
            if (waitUntilFixed) {
                signal.await();
            }
        } catch (InterruptedException e) {
            Log.wtf(TAG, "Bluetooth fix interrupted", e);
        }


    }

    private static void startFix() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "(Debug) Fixing Bluetooth (Android 4.2.X Bug)...");
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

                synchronized (mLock) {
                    mState = State.OK;
                    mSignal.countDown();
                }
                Log.d(TAG, "(Debug) Bluetooth fix complete!");
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
