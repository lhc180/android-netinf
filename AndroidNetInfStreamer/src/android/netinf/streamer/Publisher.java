package android.netinf.streamer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import android.netinf.common.Ndo;
import android.netinf.node.Node;
import android.netinf.node.publish.Publish;
import android.util.Log;

public class Publisher {

    public static final String TAG = Publisher.class.getSimpleName();

    private int mChunkNumber = 0;

    public void publish(File file) {
        Log.v(TAG, "publish()");

        try {

            Ndo ndo = new Ndo.Builder("video_chunk", "hash" + StringUtils.leftPad(Integer.toString(mChunkNumber), 5, "0")).build();
            ndo.cache(file); // Would like to attach octets instead?

            Publish publish = new Publish.Builder(ndo).fullPut().build();

            Node.submit(publish);

            mChunkNumber++;

        } catch (IOException e) {
            Log.e(TAG, "Failed to publish " + file.getAbsolutePath());
        }

    }

}
