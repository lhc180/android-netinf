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
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;

public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private File mChunkFolder = new File(Environment.getExternalStorageDirectory(), "Chunks");

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private SurfaceView mSurface;

    private ExecutorService mExecutor = Executors.newFixedThreadPool(2);
//    private ChunkGenerator mChunkGenerator;
    private Encoder mEncoder;
    private EncodingPreviewCallback mEncodingCallback;
    private LocalServerSocket mServer;
    private LocalSocket mSender;
    private LocalSocket mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
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
        switch (item.getItemId()) {
            case R.id.menu_record:
                delayedToggleRecord();
                return true;
            case R.id.menu_play:
                togglePlay();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void togglePlay() {

        File joined = null;
        FileOutputStream out = null;
        try {

            joined = new File(mChunkFolder, "joined.h264");
            FileUtils.deleteQuietly(joined);

            out = FileUtils.openOutputStream(joined);
            FileUtils.copyFile(new File(mChunkFolder, "00000.h264"), out);
            FileUtils.copyFile(new File(mChunkFolder, "00003.h264"), out);
            FileUtils.copyFile(new File(mChunkFolder, "00001.h264"), out);
            FileUtils.copyFile(new File(mChunkFolder, "00002.h264"), out);

            out.flush();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create joined h264 file", e);
        } finally {
            IOUtils.closeQuietly(out);
        }

//        File file = new File(mChunkFolder, "all.h264");
//        Log.d(TAG, "Playing " + file.getAbsolutePath() + ", " + file.getTotalSpace() + " bytes");
        Log.d(TAG, "Playing " + joined.getAbsolutePath() + ", " + joined.getTotalSpace() + " bytes");
        Intent vlc = new Intent();
        vlc.setAction(android.content.Intent.ACTION_VIEW);
        vlc.setDataAndType(Uri.fromFile(joined), "video/*");
        startActivity(vlc);

//        Log.v(TAG, "togglePlay()");
//        setContentView(R.layout.play);
////        try {
//            VideoView videoView = (VideoView) findViewById(R.id.videoView1);
//            videoView.setVideoPath(mChunkFolder.getAbsolutePath() + "/00002");
//            videoView.setMediaController(new MediaController(this));
//            videoView.requestFocus();
//            videoView.start();
////        } catch (IOException e) {
////            Log.e(TAG, "togglePlay() failed", e);
////        }
    }

    public void delayedToggleRecord() {
        Log.v(TAG, "delayedToggleRecord()");
        setContentView(R.layout.activity_main);
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

    // http://developer.android.com/guide/topics/media/camera.html#capture-video
    public void startRecording() throws IOException {
        Log.v(TAG, "startRecording()");

        // Setup local sockets
        mServer = new LocalServerSocket("com.example.recorder");
        mSender = new LocalSocket();
        mSender.connect(new LocalSocketAddress("com.example.recorder"));
        mReceiver = mServer.accept();
        closeQuietly(mServer);

        // Setup chunk generator
//        mChunkGenerator = new ChunkGenerator(mReceiver);
//        mVideoExtractor = new VideoExtractor(mReceiver.getFileDescriptor());
//        mExecutor.execute(mChunkGenerator);
//      mExecutor.execute(mVideoExtractor);

        mSurface = (SurfaceView) findViewById(R.id.surfaceView1);

        Log.d(TAG, "mSurface = " + mSurface);
        Log.d(TAG, "getHolder() = " + mSurface.getHolder());
        Log.d(TAG, "getSurface() = " + mSurface.getHolder().getSurface());

        mCamera = Camera.open();                                                                // 1

        // Extra settings
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Log.d(TAG, "Supported preview sizes:");
        for (Camera.Size size : sizes) {
            Log.d(TAG, size.width + "x" + size.height);
        }
        List<int[]> ranges = mCamera.getParameters().getSupportedPreviewFpsRange();
        Log.d(TAG, "Supported FPS ranges:");
        for (int[] range : ranges) {
            Log.d(TAG, ArrayUtils.toString(range));
        }

        mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
//        parameters.setPreviewFormat(ImageFormat.YV12);
//        parameters.setPictureSize(640, 480);              // Doesn't work?
        parameters.setPreviewSize(320, 240);
        parameters.setPreviewFpsRange(15000, 15000);
//        parameters.setPreviewFpsRange(30000, 30000);
//        parameters.
        mCamera.setParameters(parameters);

        mEncoder = new Encoder();
        mEncodingCallback = new EncodingPreviewCallback(mCamera, mEncoder);
//        mChunkGenerator = new ChunkGenerator(mEncoder.getInputStream());
        mExecutor.execute(mEncoder);
//        mExecutor.execute(mChunkGenerator);
        mCamera.setPreviewCallbackWithBuffer(mEncodingCallback);
        mCamera.setPreviewDisplay(mSurface.getHolder());                                        // 2
        mCamera.startPreview();                                                                 // 3



        // Unlocking the camera causes onPreviewFrame() to stop getting called
//        mCamera.unlock();                                                                       // 4a

//        mMediaRecorder = new MediaRecorder();
//        mMediaRecorder.setCamera(mCamera);                                                      // 4b1
////        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);                     // 4b2
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);                        // 4b3
////        mMediaRecorder.setOutputFormat(8);                                                      // 4b4 i
////        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);                                                      // 4b4 i
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);                                                      // 4b4 i
////        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);                         // 4b4 ii
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);                        // 4b4 iii
////        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);                        // 4b4 iii
////        mMediaRecorder.setOutputFile(mFile.getCanonicalPath());                                 // 4b5
////        mMediaRecorder.setOutputFile(mSender.getFileDescriptor());                              // 4b5
//        mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/chunk");                              // 4b5
//        mMediaRecorder.setPreviewDisplay(mSurface.getHolder().getSurface());                    // 4b6
////        mMediaRecorder.setVideoSize(480, 360);                                                  // Extra
//        mMediaRecorder.setVideoEncodingBitRate(500000);
////        mMediaRecorder.setAudioEncodingBitRate(128000);
//        mMediaRecorder.setVideoFrameRate(30);
//        mMediaRecorder.setMaxDuration(0);
//        mMediaRecorder.prepare();                                                               // 4c
//        mMediaRecorder.start();                                                                 // 4d

    }

    public void stopRecording() {
        Log.v(TAG, "stopRecording()");

        mCamera.setPreviewCallbackWithBuffer(null);

        if (mMediaRecorder != null) {
            Log.i(TAG, "Stopping MediaRecorder");
            mMediaRecorder.stop();                                                                  // 5a
            mMediaRecorder.reset();                                                                 // 5b
            mMediaRecorder.release();                                                               // 5c
        }

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

//        if (mChunkGenerator != null) {
//            Log.i(TAG, "Stopping ChunkGenerator");
//            mChunkGenerator.cancel();
//        }

        mEncoder = null;
//        mChunkGenerator = null;
        mMediaRecorder = null;
        mCamera = null;

        closeQuietly(mSender);
        closeQuietly(mReceiver);

    }

    private void closeQuietly(LocalServerSocket socket) {
        if (socket == null) { return; }
        try { socket.close(); } catch (IOException e) { }
    }

    private void closeQuietly(LocalSocket socket) {
        if (socket == null) { return; }
        try { socket.close(); } catch (IOException e) { }
    }

}
