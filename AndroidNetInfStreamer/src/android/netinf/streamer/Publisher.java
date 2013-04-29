package android.netinf.streamer;

import java.io.File;
import java.io.IOException;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.publish.Publish;
import android.util.Log;

public class Publisher {

    public static final String TAG = Publisher.class.getSimpleName();

    private int mChunkNumber = 0;

    public void publish(File file) {

        try {

            // Publish the chunk
//            String hash = NetInfUtils.hash(file, "sha-256");
            String algorithm = "chunk";
            String hash = "stream_name-" + mChunkNumber;
            Ndo ndo = new Ndo.Builder(algorithm, hash).build();
            ndo.cache(file);
            Publish publish = new Publish.Builder(ndo).build();
            Node.submit(publish);


            // Publish the index
            algorithm = "index";
            hash = "stream_name";
            ndo = new Ndo.Builder(algorithm, hash).build();
            ndo.cache(Integer.toString(mChunkNumber), "utf-8");
            publish = new Publish.Builder(ndo).build();
            Node.submit(publish);

            mChunkNumber++;

        } catch (IOException e) {
            Log.wtf(TAG, "Failed publishing chunk/index", e);
        }

    }



}
