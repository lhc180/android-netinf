package android.netinf.node.services.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

// In case you don't want to send length prefixes
// it might have been nice to close the OutputStream after writing the message
// At the time of writing that doesn't seem to work
// If the OutputStream is closed, an IOException (already closed)
// is thrown when trying to read from the InputStream
// Since the OutputStream isn't closed the parser on the server side gets stuck
// Instead prefix messages with length

public class BluetoothCommon {

    public static final String TAG = BluetoothCommon.class.getSimpleName();

    public static final long TIMEOUT = 10000;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

//    private static ExecutorService mConnectExecutor = Executors.newCachedThreadPool();

    // Ugly hack to restart Bluetooth
    private static Boolean mRestartingBluetooth = false;
    public static void restartBluetooth(boolean wait) {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        // If status is not currently "restarting"
        // Set status "restarting" and toggle Bluetooth off/on
        synchronized (mRestartingBluetooth) {
            if (!mRestartingBluetooth) {
                Log.i(TAG, "Restarting Bluetooth...");
                mRestartingBluetooth = true;
                adapter.cancelDiscovery();
                adapter.disable();
                while (adapter.isEnabled()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.wtf(TAG, "Sleep interrupted", e);
                    }
                }
                adapter.enable();
            }
        }

        // If we don't want to wait until it is restarted, return
        if (!wait) {
            return;
        }

        // Wait until Bluetooth has finished restarting
        while (!adapter.isEnabled()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.wtf(TAG, "Sleep interrupted", e);
            }
        }

        // Set status to "not restarting"
        synchronized (mRestartingBluetooth) {
            if (mRestartingBluetooth) {
                mRestartingBluetooth = false;
                Log.i(TAG, "Bluetooth restarted");
            }
        }

        // Wait a little bit extra
        // Should not be necessary but unless you wait the errors are not always resolved
        // Could be that behind the scenes things take a bit longer
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.wtf(TAG, "Sleep interrupted", e);
        }

    }

//    // http://stackoverflow.com/questions/14834318/android-how-to-pair-bluetooth-devices-programmatically
//    private static void removeBond(BluetoothDevice device) {
//        try {
//            Log.d(TAG, "Start Un-Pairing...");
////            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
////            m.invoke(device, (Object[]) null);
//            Method m = device.getClass().getMethod("removeBond", new Class[] {});
//            m.invoke(device, new Object[] {});
//            Log.d(TAG, "Un-Pairing finished.");
//        } catch (Exception e) {
//            Log.wtf(TAG, e.getMessage());
//        }
//    }
//
//    //http://stackoverflow.com/questions/13767972/android-bluetooth-ibluetooth-createbond-not-found-in-4-2-1-but-works-in-earlie
//    public static boolean createBond(BluetoothDevice btDevice) {
//        try {
//            Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
//            Method createBondMethod = class1.getMethod("createBond");
//            Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
//            return returnValue.booleanValue();
//        } catch (Exception e) {
//            Log.wtf(TAG, e.getMessage());
//            return false;
//        }
//    }

    public static BluetoothSocket connect(BluetoothDevice device) throws IOException {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

//        Timer failedTimer = new Timer();
        BluetoothSocket socket = null;
        try {
            // Connect to remote device
            socket = device.createRfcommSocketToServiceRecord(BluetoothApi.NETINF_UUID);
//            final BluetoothSocket finalSocket = socket; // Inner class requires a final variable
            adapter.cancelDiscovery();
//            failedTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    Log.e(TAG, "Bluetooth connection timed out");
//                    IOUtils.closeQuietly(finalSocket);
////                    BluetoothUtils.restartBluetooth(false);
//                }
//            }, 5000);
            socket.connect();
//            failedTimer.cancel();
        } catch (IOException e) {
//            failedTimer.cancel();
            IOUtils.closeQuietly(socket); // Shouldn't be necessary, but who knows?
            // Sometimes Android 4.2.X fails and BluetoothSocket.connect() start to always throw
            // java.io.IOException: read failed, socket might closed, read ret: -1
            if (e.getMessage() != null && e.getMessage().equals("read failed, socket might closed, read ret: -1")) {
                Log.e(TAG, "Bluetooth bug encountered while connecting, restart required", e);
                restartBluetooth(false);
            }
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
            return socket;
        }

    }


//    // TODO figure out why this DOES recover after Bluetooth failure due to Android bug
//    public static BluetoothSocket connect(BluetoothDevice device) throws IOException {
//
//        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//        String local = adapter.getName();
//        String remote = device.getName();
//        BluetoothSocket socket = null;
//        // Try one UUID at a time, a few times each until one connects
//        for (UUID uuid : UUIDS) {
//            for (int attempt = 1; attempt <= ATTEMPTS_PER_UUID; attempt++) {
//                try {
//                    Log.i(TAG, local + " trying to connect to " + remote
//                            + " (UUID " + uuid + ", try " + attempt + "/" + ATTEMPTS_PER_UUID + ")");
//                    socket = device.createRfcommSocketToServiceRecord(uuid);
//                    // IOException "Service discovery failed"?
//                    // Did all Api(s) get added to the ApiController and started?
//                    adapter.cancelDiscovery();
//                    Log.d(TAG, "I WILL SEE THIS");
//
//                    socket.connect();
//                    Log.d(TAG, "I WANT TO SEE THIS");
//                } catch (IOException e) {
//                    // if (e.getMessage() != null && e.getMessage().contains("read failed, socket might closed, read ret: -1")) {
//                    // Workaround for Android 4.2.X Bluetooth Bug
//                    // BluetoothFix.needFix(false);
//                    // throw new IOException(BluetoothAdapter.getDefaultAdapter().getName() + " failed to connect to " + device.getName() + " because of Android 4.2.X bug", e);
//                    // } else {
//                    Log.w(TAG, local + " failed to connect to " + remote
//                            + " (UUID " + uuid + ", try " + attempt + "/" + ATTEMPTS_PER_UUID + ")"
//                            + ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
//                    // continue;
//                    // }
//                }
//                if (socket.isConnected()) {
//                    Log.i(TAG, local + " connected to " + remote);
//                    return socket;
//                }
//            }
//        }
//        throw new IOException(local + " failed to connect to " + remote);
//    }
//
//    // TODO figure out why connect2 does not recover after Bluetooth failure due to Android bug
//    public static BluetoothSocket connect2(BluetoothDevice device) throws IOException {
//
//        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//        String local = adapter.getName();
//        String remote = device.getName();
//        BluetoothSocket socket = null;
//        // Try one UUID at a time, a few times each until one connects
//        for (UUID uuid : UUIDS) {
//            for (int attempt = 1; attempt <= ATTEMPTS_PER_UUID; attempt++) {
//                try {
//                    Log.i(TAG, local + " trying to connect to " + remote
//                            + " (UUID " + uuid + ", try " + attempt + "/" + ATTEMPTS_PER_UUID + ")");
//                    socket = device.createRfcommSocketToServiceRecord(uuid);
//                    adapter.cancelDiscovery();
//                    connect(socket);
//                    Log.i(TAG, local + " connected to " + remote);
//                    return socket;
//                } catch (IOException e) {
//                        Log.w(TAG, local + " failed to connect to " + remote
//                                + " (UUID " + uuid + ", try " + attempt + "/" + ATTEMPTS_PER_UUID + ")"
//                                + ((e.getMessage() != null) ? ": " + e.getMessage() : ""));
//                }
//            }
//        }
//        throw new IOException(local + " failed to connect to " + remote);
//    }
//
//    private static void connect(final BluetoothSocket socket) throws IOException {
//
//        final CountDownLatch done = new CountDownLatch(1);
//
//        mConnectExecutor.submit(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    socket.connect();
//                } catch (IOException e) {
//                    Log.w(TAG, "BluetoothSocket.connect() failed" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
//                }
//                done.countDown();
//            };
//
//        });
//
//        try {
//            if (!done.await(1000, TimeUnit.MILLISECONDS)) {
//                IOUtils.closeQuietly(socket);
//                throw new IOException("Connection attempt timed out");
//            }
//        } catch (InterruptedException e) {
//            IOUtils.closeQuietly(socket);
//            throw new IOException("Connection attempt interrupted", e);
//        }
//
//        if (!socket.isConnected()) {
//            IOUtils.closeQuietly(socket);
//            throw new IOException("Socket not connected");
//        }
//
//    }

//    public static String messageToString(Message message) {
//
//        String result = "<Failed to convert message to string>";
//        try {
//            File file = new File(Environment.getExternalStorageDirectory() + "/message.txt");
//            FileOutputStream fis = new FileOutputStream(file);
//            MessageWriter writer = new DefaultMessageWriter();
//            writer.writeMessage(message, fis);
//            return FileUtils.readFileToString(file);
//        } catch (IOException e) {
//            Log.wtf(TAG, "Failed to convert message to string", e);
//        }
//        return result;
//
//    }

//    public static void readConfirmation(DataInputStream bluetoothIn) throws IOException {
//        Log.v(TAG, "readEos()");
////        try {
////            Log.d(TAG, "Reading until 'EOS'...");
////            int b = 0;
////            do {
////                b = bluetoothIn.readInt();
////                // Log.d(TAG, "(Debug) b = " + b);
////            } while (b != -1);
////            Log.d(TAG, "Read 'EOS'!");
////        } catch (Throwable e) {
////            Log.e(TAG, "Failed to read 'EOS'", e);
////        }
//        bluetoothIn.readInt();
//    }

//    public static void writeConfirmation(DataOutputStream bluetoothOut) throws IOException {
//        Log.v(TAG, "writeEos()");
////        Log.d(TAG, "Wrote 'EOS'");
////        bluetoothOut.writeInt(-1);
//        bluetoothOut.writeInt(0);
//    }



//    public static void write(JSONObject jo, DataOutputStream bluetoothOut) throws IOException {
//        Log.v(TAG, "write(JSONObject)");
//        Log.d(TAG, "Wrote: " + jo.toString());
//        byte[] buffer = jo.toString().getBytes("UTF-8");
//        bluetoothOut.writeInt(buffer.length);
//        bluetoothOut.write(buffer);
//    }
//
//    public static void write(File file, DataOutputStream bluetoothOut) throws IOException {
//        Log.v(TAG, "write(File)");
//        long length = file.length();
//        if (length > Integer.MAX_VALUE) {
//            throw new IOException("Failed to write file. File too long.");
//        }
//        bluetoothOut.writeInt((int) length);
//        IOUtils.copy(new FileInputStream(file), bluetoothOut);
//    }

    public static void write(JSONObject jo, BluetoothSocket socket) throws IOException {
        synchronized (socket) {
//            Log.v(TAG, "write(JSON)");
            byte[] buffer = jo.toString().getBytes("UTF-8");
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(buffer.length);
            out.write(buffer);
            Log.d(TAG, "Wrote JSON " + buffer.length + " bytes: " + jo.toString());
        }
    }

    public static void write(JSONObject jo, File file, BluetoothSocket socket) throws IOException {
        synchronized (socket) {
//            Log.v(TAG, "write(JSON+File)");
            write(jo, socket);
//            Log.v(TAG, "write(File)");
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("Failed to write file. File too long.");
            }
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt((int) length);
            IOUtils.copy(new FileInputStream(file), out);
            Log.d(TAG, "Wrote file " + length + " bytes");
        }
    }

    private static byte[] read(BluetoothSocket socket) throws IOException {
//        Log.v(TAG, "read()");
        // Read appropriate part from the Bluetooth stream
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int length = in.readInt();
        byte[] buffer = new byte[length];

        int offset = 0;
        while (offset < length) {
            offset += in.read(buffer, offset, length - offset);
            Log.d(TAG, "Read " + offset + "/" + length + " bytes");
        }

        return buffer;
    }

    public static JSONObject readJson(BluetoothSocket socket) throws IOException {
//        Log.v(TAG, "readJson()");
        try {
            byte[] buffer = read(socket);
            String json = new String(buffer, "UTF-8");
            Log.d(TAG, "Read: " + new JSONObject(json).toString());
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new IOException("InputStream did not contain valid JSON", e);
        }
    }

    public static byte[] readFile(BluetoothSocket socket) throws IOException {
//        Log.v(TAG, "readFile()");
        // TODO Don't read entire file into memory? Or maybe for cut through forwarding
        return read(socket);
    }

    private static String bluetoothStateToString(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTED: return "STATE_CONNECTED";
            case BluetoothAdapter.STATE_CONNECTING: return "STATE_CONNECTING";
            case BluetoothAdapter.STATE_DISCONNECTED: return "STATE_DISCONNECTED";
            case BluetoothAdapter.STATE_DISCONNECTING: return "STATE_DISCONNECTING";
            case BluetoothAdapter.STATE_OFF: return "STATE_OFF";
            case BluetoothAdapter.STATE_ON: return "STATE_ON";
            case BluetoothAdapter.STATE_TURNING_OFF: return "STATE_TURNING_OFF";
            case BluetoothAdapter.STATE_TURNING_ON: return "STATE_TURNING_ON";
            default: return "UNKNOWN";
        }
    }

    public static boolean isBluetoothAvailable() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.e(TAG, "Bluetooth NOT supported");
            return false;
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.e(TAG, "Bluetooth NOT enabled: " + bluetoothStateToString(BluetoothAdapter.getDefaultAdapter().getState()));
            return false;
        }

        return true;

    }

}
