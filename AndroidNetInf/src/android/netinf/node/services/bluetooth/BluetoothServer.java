package android.netinf.node.services.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


public class BluetoothServer implements Runnable {

    public static final String TAG = BluetoothServer.class.getSimpleName();

    private BluetoothApi mApi;
    private UUID mUuid;

    public BluetoothServer(BluetoothApi api, UUID uuid) {
        mApi = api;
        mUuid = uuid;
    }

  @Override
  public void run() {

      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

      while (true) {
          try {
              BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("android.netinf", mUuid);
              while (true) {
                  BluetoothSocket socket = null;
                  try {

                      Log.i(TAG, adapter.getName() + " waiting for connections...");
                      socket = server.accept();
                      Log.i(TAG, adapter.getName() + " accepted a connection");
                      mApi.getManager().addSocket(socket);
                  } catch (IOException e) {
                      Log.e(TAG, "Failed to accept socket", e);
                  }
              }
          } catch (IOException e) {
              Log.e(TAG, "Failed create BluetoothServerSocket", e);
              if (e.getMessage() != null
                      && (e.getMessage().equals("Error: -1")
                              || e.getMessage().equals("read failed, socket might closed, read ret: -1")
                              || e.getMessage().equals("Software caused connection abort"))) {
                  // Sometimes BluetoothAdapter.listenUsingRfcommWithServiceRecord() acts up
                  // It starts to always throw exceptions until Bluetooth is restarted.
                  Log.e(TAG, "Bluetooth bug encountered while accepting, restart required", e);
                  BluetoothCommon.restartBluetooth(true);
              }
          }
      }
  }

//    @Override
//    public void run() {
//
//        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//
//        while (true) {
//            try {
//                BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("android.netinf", mUuid);
//                while (true) {
//                    BluetoothSocket socket = null;
//                    try {
//
//                        Log.i(TAG, adapter.getName() + " waiting for connections...");
//                        socket = server.accept();
//                        Log.i(TAG, adapter.getName() + " accepted a connection");
//                        mSocketExecutor.execute(new BluetoothSocketHandler(mApi, socket));
//                    } catch (IOException e) {
//                        Log.e(TAG, "Failed to accept socket", e);
//                    }
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Failed create BluetoothServerSocket", e);
//                if (e.getMessage() != null
//                        && (e.getMessage().equals("Error: -1")
//                                || e.getMessage().equals("read failed, socket might closed, read ret: -1")
//                                || e.getMessage().equals("Software caused connection abort"))) {
//                    // Sometimes BluetoothAdapter.listenUsingRfcommWithServiceRecord() acts up
//                    // It starts to always throw exceptions until Bluetooth is restarted.
//                    Log.e(TAG, "BluetoothServerSocket creation likely failed because of Android 4.2.X bug, requires Bluetooth restart)" + ": " + e.getMessage());
//                    BluetoothCommon.restartBluetooth(true);
//                }
//            }
//        }
//    }

}
