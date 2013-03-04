package android.netinf;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.netinf.common.Locator;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfUtils;
import android.netinf.node.get.Get;
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
            ndo.setOctets(getFile1());
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
                Ndo ndo = new Ndo(getNdo1());
                Publish publish = new Publish(null, RandomStringUtils.randomAlphanumeric(20), ndo);
                publish.setFullPut(true);
                publish.execute();
            }
        }).start();

    }

    public void debugGet(View view) {
        Log.d(TAG, "debugGet()");

        // Get
        new Thread(new Runnable() {
            @Override
            public void run() {
                Get get = new Get(null, NetInfUtils.newMessageId(), getNdo1());
                Ndo ndo = get.execute();
                Log.d(TAG, "result of the get: " + ndo);
            }
        }).start();



    }

    public void debugClearDatabase(View view) {
        Log.d(TAG, "debugClearDatabase()");

        DatabaseService db = new DatabaseService();
        db.clearDatabase();
    }

    public void debugStuff(View view) {
        Log.d(TAG, "debugStuff()");
        try {
            Log.d(TAG, NetInfUtils.getAlgorithm("ni://example.com/sha-256;hashityhash"));
        } catch (NetInfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
