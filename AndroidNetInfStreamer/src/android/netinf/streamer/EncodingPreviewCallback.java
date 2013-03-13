package android.netinf.streamer;

import android.hardware.Camera;
import android.util.Log;

public class EncodingPreviewCallback implements Camera.PreviewCallback {

    public static final String TAG = EncodingPreviewCallback.class.getSimpleName();

    public static final int BUFFER_NUM = 20;
    public static final int BUFFER_SIZE = 115200; // From encoders buffer size, depends on the video res 320x240 = 115200

    /** Encoder to use for each frame received in onPreviewFrame(). */
    private Encoder mEncoder;

    public EncodingPreviewCallback(Camera camera, Encoder encoder){
        // Add some reusable buffers to use for the preview frames
        for (int i = 0; i < BUFFER_NUM; i++) {
            camera.addCallbackBuffer(new byte[BUFFER_SIZE]);
        }
        mEncoder = encoder;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.v(TAG, "onPreviewFrame()");
        mEncoder.queueForEncoding(data);
        // Return the buffer, otherwise it will be unavailable for reuse
        camera.addCallbackBuffer(data);
    }

}
