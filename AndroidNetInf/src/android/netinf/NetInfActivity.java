package android.netinf;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.netinf.common.Locator;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.node.publish.Publish;
import android.netinf.node.services.database.DatabaseService;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;

public class NetInfActivity extends Activity {

    public static final String TAG = "NetInfActivity";

    private Executor mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExecutor.execute(new NodeStarter(this));

        setContentView(R.layout.activity_main);

    }

    private File getFile1() {
        return new File(Environment.getExternalStorageDirectory() + "/hello_world.jpg");
    }

    private Ndo getNdo1() {
        Ndo ndo = null;
        try {

            ndo = new Ndo("sha-256", hash(getFile1()));
            ndo.addLocator(Locator.fromBluetooth("11:22:33:44"));
            JSONObject meta = new JSONObject();
            meta.put("hello", "world");
            JSONObject deeper = new JSONObject();
            deeper.put("url", "example.com");
            meta.put("deeper", deeper);
            ndo.addMetadata(new Metadata(meta));

        } catch (Throwable e) {
            Log.wtf(TAG, "debugPublish() failed", e);
        }
        return ndo;
    }

    public void debugPublish(View view) {
        Log.d(TAG, "debugPublish()");

        // Publish
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Publish publish = new Publish(getNdo1());
                    publish.setOctets(getFile1());
                    publish.execute();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void debugClearDatabase(View view) {
        Log.d(TAG, "debugPublish()");

        DatabaseService db = new DatabaseService();
        db.clearDatabase();
    }

    private static String hash(File file) {
        // Hash a file
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] binaryHash = digest.digest(bytes);
            String hash = Base64.encodeToString(binaryHash, Base64.NO_PADDING | Base64.NO_WRAP);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
