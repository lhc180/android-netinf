package android.netinf.streamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
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
 * http://stackoverflow.com/questions/13703596/mediacodec-and-camera-colorspaces-dont-match
 * http://wiki.videolan.org/YUV#Semi-planar
 */
public class Encoder implements Runnable {

    public static final String TAG = Encoder.class.getSimpleName();

    public static final File CHUNK_FOLDER = new File(Environment.getExternalStorageDirectory(), "Chunks");
    public static final int FILE_NAME_LENGTH = 5;
    public static final int BUFFER_NUM = 20;
    public static final int BUFFER_SIZE = 115200; // From mCodec's buffer size
    public static final int MIN_CHUNK_SIZE = 1;//50 * 1024; // Min bytes per chunk, can only split on IDR-frames

    /** MediaCodec that encodes the frames into a h264 byte stream. */
    private MediaCodec mCodec;
    /** Buffer keeping track of uncoded frames, it should not grow if things are fast enough. */
    private Buffer mFrameFifo; //<byte[]>
    private boolean mStopped;

    /** Current chunk file. */
    private File mChunk;
    /** OutputStream to the current chunk. */
    private FileOutputStream mChunkOut;
    /** Current chunk number. */
    private int mChunkNumber = 0;
    /** Byte written to current chunk so far. */
    private int mChunkSize = 0;
    /** Publisher to call when chunk done. */
    private Publisher mPublisher;

    public Encoder() {

        // Print Supported Formats
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            for (String type : info.getSupportedTypes()) {
                if (type.equals("video/avc")) {
                    MediaCodecInfo.CodecCapabilities c = info.getCapabilitiesForType(type);
                    Log.d(TAG, "Name: " + info.getName());
                    Log.d(TAG, "Supports Type: " + type);
                    Log.d(TAG, "Color Formats: " + ArrayUtils.toString(c.colorFormats));
                }
            }
        }

        mFrameFifo = BufferUtils.blockingBuffer(new UnboundedFifoBuffer());

        mCodec = MediaCodec.createByCodecName("OMX.TI.DUCATI1.VIDEO.H264E");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 320, 240);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
        // FRAME_RATE is in reality 30, 15 tricks the encoder to insert more IDR-frames, for smaller min chunks
        // I_FRAME_INTERVAL is 1/s but since FRAME_RATE is wrong we get double the amount, 2/s
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        // mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        // mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar);
        // mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 2130708361);
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodec.start();
    }

    public byte[] YV12toYUV420PackedSemiPlanar(final byte[] input, final int width, final int height) {
        /*
         * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12
         * We convert by putting the corresponding U and V bytes together (interleaved).
         */
        byte[] output = new byte[input.length];

        final int frameSize = width * height;
        final int qFrameSize = frameSize/4;

        System.arraycopy(input, 0, output, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            output[frameSize + i*2] = input[frameSize + i + qFrameSize]; // Cb (U)
            output[frameSize + i*2 + 1] = input[frameSize + i]; // Cr (V)
        }
        return output;
    }

    // called from Camera.setPreviewCallbackWithBuffer(...) in other class
    @SuppressWarnings("unchecked")
    public void queueForEncoding(byte[] frame) {
        // Log.v(TAG, "queueForEncoding()");
        // Fake 30 fps by encoding every frame twice
        frame = YV12toYUV420PackedSemiPlanar(frame, 320, 240);
        mFrameFifo.add(frame);
        mFrameFifo.add(frame);
        if (mFrameFifo.size() > 30) {
            Log.w(TAG, "More than 30 uncoded frames in queue, encoder is lagging behind!");
        }
    }

    public void cancel() {
        mStopped = true;
    }

    public void close() {
        mCodec.stop();
        mCodec.release();
        IOUtils.closeQuietly(mChunkOut);
    }

    @Override
    public void run() {

        Log.v(TAG, "run()");
        Log.i(TAG, "Encoder running...");

        mStopped = false;
        sendEncoderInput();
        handleHeader();
        while (!mStopped) {
            if (!mFrameFifo.isEmpty()) {
                sendEncoderInput();
            }
            handleEncoderOutput();
        }
        close();

        Log.i(TAG, "Encoder stopped");

    }

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

    public void setChunkPublisher(Publisher publisher) {
        mPublisher = publisher;
    }

    private void nextChunk() {
        try {
            if (mChunkOut != null) {
                mChunkOut.flush();
                mChunkOut.close();
            }
            if (mChunk != null && mPublisher != null) {
                mPublisher.publish(mChunk);
            }
            mChunk = new File(CHUNK_FOLDER, StringUtils.leftPad(Integer.toString(mChunkNumber), FILE_NAME_LENGTH, "0") + ".h264");
            Log.i(TAG, "New chunk: " + mChunk.getAbsolutePath());
            FileUtils.deleteQuietly(mChunk);
            mChunkOut = FileUtils.openOutputStream(mChunk);
            mChunkSize = 0;
            mChunkNumber++;
        } catch (IOException e) {
            Log.e(TAG, "Problem while creating next chunk", e);
        }
    }

    private void handleHeader() {
        // Log.v(TAG, "handleHeader");

        try {

            // Get output buffer from encoder, get encoded data and write it
            ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            // Dependent on how the video is encoded, we handle the two first encoded part differently
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
        // Log.v(TAG, "handleEncoderOutput");

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
                // Log.d(TAG, "Frame bytes: " + Arrays.toString(ArrayUtils.subarray(outData, 0, 10)) + "...");
                // Log.d(TAG, "Frame type: " + (((outData[4] & 0x1F) == 28) ?  (outData[5] & 0x1F) : (outData[4] & 0x1F)));

                writePartialChunk(outData, 0, outData.length);

                // Log.i("Encoder", outData.length + " bytes written");

                mCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);

            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed processing encoder output", e);
        }
    }

    private void sendEncoderInput() {
        // Log.v(TAG, "sendEncoderInput");

        // Get input buffer from encoder, if we got one insert a frame and hand it back
        int inputBufferIndex = mCodec.dequeueInputBuffer(1);
        if (inputBufferIndex >= 0) {
            // Get a frame to encode and put it in the buffer;
            byte[] frame = (byte[]) mFrameFifo.remove();

            // Get buffers
            ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(frame);
            mCodec.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
        }
    }

}
