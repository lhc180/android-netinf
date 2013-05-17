package android.netinf.streamer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.netinf.common.Ndo;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.node.Node;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;

/*
 * Useful links
 * http://developer.android.com/guide/topics/media/camera.html#capture-video
 */
public class StreamerActivity extends Activity {

    public static final String TAG = StreamerActivity.class.getSimpleName();

    public static final File CHUNK_FOLDER = new File(Environment.getExternalStorageDirectory(), "Chunks");

    /** Camera used to record the stream, not null implies recording. */
    private Camera mCamera;
    /** Surface used to show the preview of the stream. */
    private SurfaceView mSurface;
    /** Executor to run the Encoder. */
    private ExecutorService mEncoderExecutor = Executors.newSingleThreadExecutor();
    /** Encoder used to encode the video. */
    private Encoder mEncoder;
    /** A callback handed to the Camera that receives each frame and encodes it using the Encoder. */
    private EncodingPreviewCallback mEncodingCallback;

    /** Executor to run the Player. */
    private ExecutorService mPlayerExecutor = Executors.newSingleThreadExecutor();
    /** Player that downloads, merges and plays chunks, */
    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        // Start the NetInf library
        Node.start(getApplicationContext());

        setContentView(R.layout.activity_main);

    }

    public void debug() {
        while (true) {

            Ndo ndo = new Ndo.Builder("index", "stream_name").build();
            Get get = new Get.Builder(ndo).build();
            Future<GetResponse> f1 = Node.submit(get);
            Future<GetResponse> f2 = Node.submit(get);
            Future<GetResponse> f3 = Node.submit(get);
            Future<GetResponse> f4 = Node.submit(get);
            try {
                Log.d(TAG, "---------" + System.currentTimeMillis());
                GetResponse resp1 = f1.get();
                GetResponse resp2 = f2.get();
                GetResponse resp3 = f3.get();
                GetResponse resp4 = f4.get();
                Log.d(TAG, "---------" + System.currentTimeMillis());
                Log.d(TAG, "------------" + resp1.getStatus() + " " + (resp1.getStatus().isSuccess() ? resp1.getNdo() : resp1.getRequest().getNdo()));
                Log.d(TAG, "------------" + resp2.getStatus() + " " + (resp2.getStatus().isSuccess() ? resp2.getNdo() : resp2.getRequest().getNdo()));
                Log.d(TAG, "------------" + resp3.getStatus() + " " + (resp3.getStatus().isSuccess() ? resp3.getNdo() : resp3.getRequest().getNdo()));
                Log.d(TAG, "------------" + resp4.getStatus() + " " + (resp4.getStatus().isSuccess() ? resp4.getNdo() : resp4.getRequest().getNdo()));
            } catch (ExecutionException e) {
                Log.wtf(TAG, "--------get failed", e);
            } catch (InterruptedException e) {
                Log.wtf(TAG, "--------get failed", e);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu()");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected()");
        // Switch on menu item pressed.
        switch (item.getItemId()) {
            case R.id.menu_record:
                delayedToggleRecord();
                return true;
            case R.id.menu_play:
                togglePlay();
                return true;
            case R.id.menu_clear:
                clear();
                return true;
            case R.id.menu_preference:
                Node.showPreferences(this);
                return true;
            case R.id.menu_debug:
                // TODO remove debug
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        debug();
                    }
                }).start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void togglePlay() {
        Log.v(TAG, "togglePlay()");

        if (mPlayer == null) {
            mPlayer = new Player(this);
            mPlayerExecutor.execute(mPlayer);
        } else {
            mPlayer.cancel();
            mPlayer = null;
        }

    }

    public void delayedToggleRecord() {
        Log.v(TAG, "delayedToggleRecord()");
        setContentView(R.layout.activity_main);
        // Ugly fix: a short delay before toggling the camera, otherwise it might not be ready
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toggleRecord();
            }
        }, 1000);
    }

    public void toggleRecord() {
        Log.v(TAG, "toggleRecord()");
        if (mCamera == null) {
            try {
                startRecording();
            } catch (IOException e) {
                Log.e(TAG, "startRecording() failed", e);
                stopRecording();
            }
        } else {
            stopRecording();
        }

    }

    //
    public void startRecording() throws IOException {
        Log.v(TAG, "startRecording()");

        // Get surface and camera
        mSurface = (SurfaceView) findViewById(R.id.surfaceView1);
        mCamera = Camera.open();                                                                // 1

        // Log supported parameters
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Log.d(TAG, "Supported preview sizes:");
        for (Camera.Size size : sizes) {
            Log.d(TAG, size.width + "x" + size.height);
        }
        List<int[]> ranges = mCamera.getParameters().getSupportedPreviewFpsRange();
        Log.d(TAG, "Supported preview FPS ranges:");
        for (int[] range : ranges) {
            Log.d(TAG, ArrayUtils.toString(range));
        }

        // Set preview parameters
        mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFormat(ImageFormat.YV12);
        //        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPreviewSize(320, 240);
        parameters.setPreviewFpsRange(15000, 15000);
        mCamera.setParameters(parameters);

        Log.d(TAG, "Supported preview formats: " + ArrayUtils.toString(mCamera.getParameters().getSupportedPreviewFormats().toArray()));
        Log.d(TAG, "Preview format is: " + mCamera.getParameters().getPreviewFormat());

        // Setup Encoder and the per frame callback
        mEncoder = new Encoder();
        mEncoder.setChunkPublisher(new Publisher());
        mEncodingCallback = new EncodingPreviewCallback(mCamera, mEncoder);
        mEncoderExecutor.execute(mEncoder);
        mCamera.setPreviewCallbackWithBuffer(mEncodingCallback);
        mCamera.setPreviewDisplay(mSurface.getHolder());                                        // 2
        mCamera.startPreview();                                                                 // 3
        // Unlocking the camera causes onPreviewFrame() to stop getting called

    }

    public void stopRecording() {
        Log.v(TAG, "stopRecording()");

        mCamera.setPreviewCallbackWithBuffer(null);

        if (mCamera != null) {
            Log.i(TAG, "Stopping Camera");
            mCamera.lock();                                                                         // 5d
            mCamera.stopPreview();                                                                  // 6
            mCamera.release();                                                                      // 7
        }

        if (mEncoder != null) {
            Log.i(TAG, "Stopping Encoder");
            mEncoder.cancel();
        }

        mCamera = null;
        mEncoder = null;

    }

    public void clear() {

        Log.i(TAG, "Deleting previously recorded chunks");

        Node.clear();
        FileUtils.deleteQuietly(CHUNK_FOLDER);

    }

}
