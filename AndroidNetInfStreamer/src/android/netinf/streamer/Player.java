package android.netinf.streamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.netinf.common.Ndo;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.node.Node;
import android.os.Environment;
import android.util.Log;

public class Player implements Runnable {

    public static final String TAG = Player.class.getSimpleName();

    public static final int SLEEP = 500;
    public static final int TIMEOUT = 1000;
    public static final int ATTEMPTS = 5;
    public static final int BUFFER = 5;
    public static final File VIDEO_FILE = new File(Environment.getExternalStorageDirectory(), "video.h264");

    private Context mContext;
    private FileOutputStream mVideo;

    public Player(Context context) {
        mContext = context;
    }

    @Override
    public void run() {

        Log.i(TAG, "Player STARTED");

        try {

            // Setup output
            FileUtils.deleteQuietly(VIDEO_FILE);
            mVideo = FileUtils.openOutputStream(VIDEO_FILE);

            // Get current chunk
            int start = getCurrentChunk();
            int current = start;

            // Get chunks (chunk 0 is needed, contains header)
            getChunk(0);
            while(!Thread.currentThread().isInterrupted()) {
                if(current - start == BUFFER) {
                    play();
                }
                getChunk(current);
                current++;
            }

        } catch (IOException e)  {
            IOUtils.closeQuietly(mVideo);
        }

        Log.i(TAG, "Player STOPPED");

    }

    private int getCurrentChunk() throws IOException {

        Ndo ndo = new Ndo.Builder("index", "stream_name").build();
        Get get = new Get.Builder(ndo).build();

        GetResponse response = getUntilSuccess(get, ATTEMPTS);
        String content = FileUtils.readFileToString(response.getNdo().getOctets(), "utf-8");
        return Integer.parseInt(content.trim());

    }

    private void getChunk(int chunkNumber) throws IOException {

        Ndo ndo = new Ndo.Builder("chunk", "stream_name-" + chunkNumber).build();
        Get get = new Get.Builder(ndo).build();

        GetResponse response = getUntilSuccess(get, ATTEMPTS);
        Log.d(TAG, "Got another " + response.getNdo().getOctets().length() + " bytes of video");
        FileUtils.copyFile(response.getNdo().getOctets(), mVideo);
        mVideo.flush();
    }

    private GetResponse getUntilSuccess(Get get, int attempts) throws IOException {
        while (attempts > 0) {
            try {
//                GetResponse response = Node.submit(get).get(TIMEOUT, TimeUnit.MILLISECONDS);
                GetResponse response = Node.submit(get).get();
                if (response.getStatus().isSuccess()) {
                    return response;
                } else {
                    Log.w(TAG, "GET failed: " + response.getStatus());
                }
            } catch (Exception e) {
                Log.w(TAG, "GET failed", e);
            }
            try {
                Thread.currentThread().sleep(SLEEP);
            } catch (InterruptedException e) {
                Log.wtf(TAG, "Sleep interrupted", e);
            }
//            attempts--;
        }
        throw new IOException("GET failed: " + get);
    }

    private void play() {
        Log.d(TAG, "Playing " + VIDEO_FILE.getAbsolutePath() + "...");
        Intent vlc = new Intent();
        vlc.setAction(android.content.Intent.ACTION_VIEW);
        vlc.setDataAndType(Uri.fromFile(VIDEO_FILE), "video/*");
        mContext.startActivity(vlc);
    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }

//    File file = null;
//
//    FileOutputStream out = null;
//    try {
//        // Delete old temp file
//        file = new File(CHUNK_FOLDER, "joined.h264");
//        FileUtils.deleteQuietly(file);
//        // Create new temp file
//        out = FileUtils.openOutputStream(file);
//        FileUtils.copyFile(new File(CHUNK_FOLDER, "00000.h264"), out);
//        FileUtils.copyFile(new File(CHUNK_FOLDER, "00001.h264"), out);
//        out.flush();
//    } catch (Exception e) {
//        Log.e(TAG, "Failed to create joined h264 file", e);
//    } finally {
//        IOUtils.closeQuietly(out);
//    }
//
//
//    Log.d(TAG, "Playing " + file.getAbsolutePath() + ", " + file.length() + " bytes");
//    Intent vlc = new Intent();
//    vlc.setAction(android.content.Intent.ACTION_VIEW);
//    vlc.setDataAndType(Uri.fromFile(file), "video/*");
//    startActivity(vlc);

}
