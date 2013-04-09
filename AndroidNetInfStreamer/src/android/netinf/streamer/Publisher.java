package android.netinf.streamer;

import java.io.File;
import java.io.IOException;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.publish.Publish;
import android.util.Log;

public class Publisher {

    public static final String TAG = Publisher.class.getSimpleName();

    private String mFirstHash;
    private int mDebugCounter = 0;

    public void publish(File file) {

        try {

            // Publish the chunk
//            String hash = NetInfUtils.hash(file, "sha-256");
            String hash = "chunk-" + mDebugCounter++;
            Ndo ndo = new Ndo.Builder("sha-256", hash).build();
            ndo.cache(file);
            Publish publish = new Publish.Builder(ndo).build();
            Node.submit(publish);

            // Publish the index, unless it is the first chunk
            if (mFirstHash == null) {

                mFirstHash = hash;

            } else {

                String indexHash = "test";
                Ndo indexNdo = new Ndo.Builder("video", indexHash).build();
                indexNdo.cache(mFirstHash + "\n" + hash, "utf-8");
                Publish indexPublish = new Publish.Builder(indexNdo).build();
                Node.submit(indexPublish);

            }

        } catch (IOException e) {
            Log.wtf(TAG, "Failed publishing chunk/index", e);
        }
//        catch (NoSuchAlgorithmException e) {
//            Log.wtf(TAG, "sha-256 not available", e);
//        }

    }



}
