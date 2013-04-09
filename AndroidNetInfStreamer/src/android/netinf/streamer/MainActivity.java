package android.netinf.streamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
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
public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();

    /** Camera used to record the stream, not null implies recording. */
    private Camera mCamera;
    /** Surface used to show the preview of the stream. */
    private SurfaceView mSurface;
    /** Executor to run the Encoder. */
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    /** Encoder used to encode the video. */
    private Encoder mEncoder;
    /** A callback handed to the Camera that receives each frame and encodes it using the Encoder. */
    private EncodingPreviewCallback mEncodingCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        // Start the NetInf library
        Node.start(getApplicationContext());

        setContentView(R.layout.activity_main);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void togglePlay() {
        Log.v(TAG, "togglePlay()");

        File chunkFolder = new File(Environment.getExternalStorageDirectory(), "Chunks");
        File file = null;

        FileOutputStream out = null;
        try {
            // Delete old temp file
            file = new File(chunkFolder, "joined.h264");
            FileUtils.deleteQuietly(file);
            // Create new temp file
            out = FileUtils.openOutputStream(file);
            FileUtils.copyFile(new File(chunkFolder, "00000.h264"), out);
            FileUtils.copyFile(new File(chunkFolder, "00001.h264"), out);
            out.flush();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create joined h264 file", e);
        } finally {
            IOUtils.closeQuietly(out);
        }


        Log.d(TAG, "Playing " + file.getAbsolutePath() + ", " + file.length() + " bytes");
        Intent vlc = new Intent();
        vlc.setAction(android.content.Intent.ACTION_VIEW);
        vlc.setDataAndType(Uri.fromFile(file), "video/*");
        startActivity(vlc);
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
        mExecutor.execute(mEncoder);
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

        Node.clear();

    }

}
