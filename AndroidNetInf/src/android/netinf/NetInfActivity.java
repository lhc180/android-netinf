package android.netinf;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.netinf.common.Locator;
import android.netinf.common.Metadata;
import android.netinf.common.Ndo;
import android.netinf.common.NetInfException;
import android.netinf.common.NetInfUtils;
import android.netinf.node.get.Get;
import android.netinf.node.get.GetResponse;
import android.netinf.node.publish.Publish;
import android.netinf.node.publish.PublishResponse;
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

    private Metadata getMeta1() {
        try {
        JSONObject meta = new JSONObject();
        meta.put("hello", "world");
        JSONObject deeper = new JSONObject();
        deeper.put("url", "example.com");
        meta.put("deeper", deeper);
        return new Metadata(meta);
        } catch (JSONException e) {
            throw new RuntimeException("wtf", e);
        }
    }

    private File getFile1() {
        return new File(Environment.getExternalStorageDirectory() + "/hello_world.jpg");
    }

    private Ndo getNdo1() {

        String algorithm = "sha-256";
        String hash = hash(getFile1());
        Locator locator = Locator.fromBluetooth("11:22:33:44");

        Ndo ndo = new Ndo.Builder(algorithm, hash).locator(locator).metadata(getMeta1()).build();
        try {
            ndo.cache(getFile1());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ndo;
    }

    public void debugPublish(View view) {
        Log.d(TAG, "debugPublish()");

        // Publish
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Publish publish = new Publish.Builder(null, NetInfUtils.newMessageId(), getNdo1()).build();
                Publish publish = new Publish.Builder(null, getNdo1()).fullPut().build();
                PublishResponse response = publish.call();
                Log.d(TAG, "PUBLISH resulted in status = " + response.getStatus());
            }
        }).start();

    }

    public void debugGet(View view) {
        Log.d(TAG, "debugGet()");

        // Get
        new Thread(new Runnable() {
            @Override
            public void run() {
                Get get = new Get.Builder(null, getNdo1()).build();
                GetResponse response = get.call();
                Log.d(TAG, "GET resulted in status = " + response.getStatus() + ", ndo = " + response.getNdo());
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
