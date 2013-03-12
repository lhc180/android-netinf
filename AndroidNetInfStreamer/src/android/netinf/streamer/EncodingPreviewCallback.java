package android.netinf.streamer;

import android.hardware.Camera;
import android.util.Log;

public class EncodingPreviewCallback implements Camera.PreviewCallback {

    public static final String TAG = EncodingPreviewCallback.class.getSimpleName();

    public static final int BUFFER_NUM = 20;
    public static final int BUFFER_SIZE = 115200; // From encoders buffer size

    private Encoder mEncoder;

    public EncodingPreviewCallback(Camera camera, Encoder encoder){
        for (int i = 0; i < BUFFER_NUM; i++) {
            camera.addCallbackBuffer(new byte[BUFFER_SIZE]);
        }
        mEncoder = encoder;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.v(TAG, "onPreviewFrame()");

        mEncoder.queueForEncoding(data);
        camera.addCallbackBuffer(data);

    }

}
