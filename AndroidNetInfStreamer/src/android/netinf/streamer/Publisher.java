package android.netinf.streamer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.netinf.common.Locator;
import android.netinf.common.Ndo;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.node.Node;
import android.util.Log;

public class Publisher {

    public static final String TAG = Publisher.class.getSimpleName();

    private int mChunkNumber = 0;

    public void publish(File file) {

        try {

            // Publish the chunk
//            String hash = NetInfUtils.hash(file, "sha-256");
            Locator bluetooth = Locator.fromBluetooth();
            String algorithm = "chunk";
            String hash = "stream_name-" + mChunkNumber;
            Ndo ndo = new Ndo.Builder(algorithm, hash).addLocator(bluetooth).build();
            ndo.cache(file);
            Publish publish = new Publish.Builder(ndo).build();
            publishUntilSuccessful(publish);


            // Publish the index
            algorithm = "index";
            hash = "stream_name";
            ndo = new Ndo.Builder(algorithm, hash).addLocator(bluetooth).build();
            ndo.cache(Integer.toString(mChunkNumber), "utf-8");
            publish = new Publish.Builder(ndo).build();
            publishUntilSuccessful(publish);

            mChunkNumber++;

        } catch (IOException e) {
            Log.wtf(TAG, "Failed publishing chunk/index", e);
        }

    }

    private void publishUntilSuccessful(Publish publish) {

        Future<PublishResponse> future = Node.submit(publish);

        try {

            PublishResponse publishResponse = future.get();
            if (publishResponse.getStatus().isSuccess()) {
                return;
            }
            Log.e(TAG, "Failed to PUBLISH " + publish);

        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to PUBLISH " + publish);
        } catch (ExecutionException e) {
            Log.e(TAG, "Failed to PUBLISH " + publish);
        }

        publishUntilSuccessful(publish);

    }

}
