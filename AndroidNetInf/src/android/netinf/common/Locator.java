package android.netinf.common;

import java.io.Serializable;

import android.bluetooth.BluetoothAdapter;

/**
 * Represents a locator to an {@link Ndo}.
 * @author Linus Sunde
 *
 */
public class Locator implements Serializable {

    /** HTTP Scheme. */
    public static final String HTTP = "http://";
    /** Bluetooth Scheme. */
    public static final String BLUETOOTH = "nimacbt://";

    /** Locator. */
    private String mLocator;

    private Locator() {

    }

    public static Locator fromString(String string) {
        Locator locator = new Locator();
        locator.mLocator = string;
        return locator;
    }

    /**
     * Creates a new Locator given a Bluetooth MAC address.
     * @param mac
     *     The Bluetooth MAC address
     * @return
     *     A new Locator to the given Bluetooth MAC address
     */
    public static Locator fromBluetooth(String mac) {
        Locator locator = new Locator();
        locator.mLocator = BLUETOOTH + mac;
        return locator;
    }

    public static Locator fromBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        String mac = adapter.getAddress();
        return fromBluetooth(mac);
    }

    @Override
    public String toString() {
        return mLocator;
    }

}
