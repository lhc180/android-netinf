package android.netinf.streamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

/*
 * Useful links
 * http://stackoverflow.com/questions/14067339/create-video-from-screen-grabs-in-android
 * http://stackoverflow.com/questions/13458289/encoding-h-264-from-camera-with-android-mediacodec
 * http://stackoverflow.com/questions/7790566/how-to-get-raw-preview-data-from-camera-object-at-least-15-frames-per-second-in
 * http://stackoverflow.com/questions/13458289/encoding-h-264-from-camera-with-android-mediacodec
 * http://codesequoia.wordpress.com/2009/10/18/h-264-stream-structure/
 * http://vec.io/posts/android-hardware-decoding-with-mediacodec
 * http://stackoverflow.com/questions/14538871/the-buffer-size-for-callbackbuffer-of-android-camera-need-to-be-8-time-as-big-as
 * http://stackoverflow.com/questions/9618369/h-264-over-rtp-identify-sps-and-pps-frames
 * http://stackoverflow.com/questions/9618369/h-264-over-rtp-identify-sps-and-pps-frames
 * http://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-H.264-200305-S!!PDF-E&type=items
 */
public class Encoder implements Runnable {

    public static final String TAG = Encoder.class.getSimpleName();

    public static final File CHUNK_FOLDER = new File(Environment.getExternalStorageDirectory(), "Chunks");
    public static final int FILE_NAME_LENGTH = 5;
    public static final int BUFFER_NUM = 20;
    public static final int BUFFER_SIZE = 115200; // From mCodec's buffer size
    public static final int MIN_CHUNK_SIZE = 200 * 1024; // 200 kB

    private MediaCodec mCodec;
    //    private PipedInputStream mPipeIn;
    //    private PipedOutputStream mPipeOut;
    private Buffer mFrameFifo; //<byte[]>
    private boolean mStopped;

    private FileOutputStream mChunkOut;
    private int mChunkNumber = 0;
    private int mChunkSize = 0;

    public Encoder() {

        mFrameFifo = BufferUtils.blockingBuffer(new UnboundedFifoBuffer());

        //        try {
        //             Setup encoding queue
        //            mPipeIn = new PipedInputStream(1024*1024);
        //            mPipeOut = new PipedOutputStream(mPipeIn);
        //
        //             Setup output file
        //            File file = new File(Environment.getExternalStorageDirectory() + "/test.h264");
        //            FileUtils.deleteQuietly(file);
        //            mOutput = new BufferedOutputStream(FileUtils.openOutputStream(file));
        //
        //        } catch (IOException e) {
        //            Log.e(TAG, "Failed to create piped streams", e);
        //        }

        // Print Supported Formats
        //        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
        //            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
        //            Log.d(TAG, "Name: " + info.getName());
        //            for (String type : info.getSupportedTypes()) {
        //                MediaCodecInfo.CodecCapabilities c = info.getCapabilitiesForType(type);
        //                Log.d(TAG, "Supports Type: " + type);
        //                Log.d(TAG, "Color Formats: " + ArrayUtils.toString(c.colorFormats));
        ////                Log.d(TAG, "Profile Levels: " + c.profileLevels);
        //            }
        //        }

        //        mCodec = MediaCodec.createEncoderByType("video/avc");
        mCodec = MediaCodec.createByCodecName("OMX.TI.DUCATI1.VIDEO.H264E");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 320, 240);
        //      MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 640, 480);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        //        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        //        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        mCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodec.start();
    }

    // called from Camera.setPreviewCallbackWithBuffer(...) in other class
    @SuppressWarnings("unchecked")
    public void queueForEncoding(byte[] frame) {
        Log.v(TAG, "queueForEncoding()");
        // Fake 30 fps by encoding every frame twice
        mFrameFifo.add(frame);
        mFrameFifo.add(frame);
    }

    public void cancel() {
        mStopped = true;
    }

    public void close() throws IOException {
        mCodec.stop();
        mCodec.release();
        // TODO close the pipes
    }

    @Override
    public void run() {

        Log.v(TAG, "run()");
        Log.i(TAG, "Encoder running...");

        mStopped = false;
        sendEncoderInput();
        handleHeader();
        while (!mStopped) {
            sendEncoderInput();
            handleEncoderOutput();
        }

        Log.i(TAG, "Encoder stopped");

    }

//    public void newChunk(byte[] data) {
//
//        try {
//            File chunk = new File(CHUNK_FOLDER, StringUtils.leftPad(Integer.toString(mChunkNumber), FILE_NAME_LENGTH, "0"));
//            FileUtils.deleteQuietly(chunk);
//            FileUtils.writeByteArrayToFile(chunk, data);
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to create chunk", e);
//        }
//
//    }

    private void writePartialChunk(int oneByte) throws IOException {
        mChunkOut.write(oneByte);
        mChunkSize++;
    }

    private void writePartialChunk(byte[] buffer) throws IOException {
        writePartialChunk(buffer, 0, buffer.length);
    }

    private void writePartialChunk(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        mChunkOut.write(buffer, byteOffset, byteCount);
        mChunkSize += byteCount;
    }

    private void nextChunk() {
        try {
            if (mChunkOut != null) {
                mChunkOut.flush();
                mChunkOut.close();
            }
            File chunk = new File(CHUNK_FOLDER, StringUtils.leftPad(Integer.toString(mChunkNumber), FILE_NAME_LENGTH, "0") + ".h264");
            Log.i(TAG, "New chunk: " + chunk.getAbsolutePath());
            FileUtils.deleteQuietly(chunk);
            mChunkOut = FileUtils.openOutputStream(chunk);
            mChunkSize = 0;
            mChunkNumber++;
        } catch (IOException e) {
            Log.e(TAG, "Problem while creating next chunk", e);
        }
    }

    private void handleHeader() {

        try {

            // Get output buffer from encoder, get encoded data and write it
            ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            // Dependant on how the video is encoded, we handle the two first encoded part differently
            // First we put the SPS/PPS in the first part into its chunk one
            // These will always be needed and it seems like only one pair is generated
            // There will be some padding 0x00 in the second part which should be put into the first chunk as well
            // Then put the rest of the second part, starting with 0x00000001 into chunk two.
            int buffersReceived = 0;
            byte[][] data = new byte[2][];

            // Get the two first parts
            do {
                int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    data[buffersReceived] = new byte[bufferInfo.size];
                    outputBuffer.get(data[buffersReceived]);
                    mCodec.releaseOutputBuffer(outputBufferIndex, false);
                    buffersReceived++;
                }
            } while (buffersReceived < 2);

            // We have the two first parts
            nextChunk();

            // Write the first part to chunk 1
            writePartialChunk(data[0]);
            int offset = 0;
            while (!(data[1][offset] == 0x00
                    && data[1][offset+1] == 0x00
                    && data[1][offset+2] == 0x00
                    && data[1][offset+3] == 0x01)) {
                writePartialChunk(data[1][offset]);
                offset++;
            }

            // Write the second part to chunk 2
            nextChunk();
            writePartialChunk(data[1], offset, data[1].length - offset);

        } catch (Throwable e) {
            Log.e(TAG, "Failed handling h264 header", e);
        }

    }

    private void handleEncoderOutput() {

        try {

            // Get output buffer from encoder, get encoded data and write it
            ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            // int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 1);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);

                // If min chunk size reached AND
                // frame is fractured, but a IDR-frame OR frame is IDR-frame
                // Then new chunk
                // 5 = idr, 7 = sps, 8 = sps, 28 = fragmented
                if(mChunkSize >= MIN_CHUNK_SIZE
                        && (((outData[4] & 0x1F) == 28 && (outData[5] & 0x1F) == 0x05) || ((outData[4] & 0x1F) == 0x05))) {
                    nextChunk();
                }
                Log.d(TAG, "Frame bytes: " + Arrays.toString(ArrayUtils.subarray(outData, 0, 10)) + "...");
                Log.d(TAG, "Frame type: " + (((outData[4] & 0x1F) == 28) ?  (outData[5] & 0x1F) : (outData[4] & 0x1F)));

                writePartialChunk(outData, 0, outData.length);

                Log.i("Encoder", outData.length + " bytes written");

                mCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);

            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed processing encoder output", e);
        }
    }

    private void sendEncoderInput() {
        // Get a frame to encode;
        byte[] frame = (byte[]) mFrameFifo.remove();

        // Get input buffer from encoder, insert the frame and hand it back
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(frame);
            mCodec.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
            Log.d(TAG, "Frame queue length: " + mFrameFifo.size());
        }
    }

}
