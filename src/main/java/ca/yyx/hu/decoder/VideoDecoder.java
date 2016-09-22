package ca.yyx.hu.decoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class VideoDecoder {
    private MediaCodec mCodec;
    private MediaCodec.BufferInfo mCodecBufferInfo;
    private final static Object sLock = new Object();
    private ByteBuffer[] mInputBuffers;

    private final Context mContext;
    private int mHeight;
    private int mWidth;
    private SurfaceHolder mHolder;
    private byte[] mConfigSps;
    private byte[] mConfigPps;

    public static boolean isH246Video(byte[] ba) {
        return ba[0] == 0 && ba[1] == 0 && ba[2] == 0 && ba[3] == 1;
    }

    private static boolean isSps(byte[] ba)
    {
        return (ba[4] & 0x1f) == 7;
    }

    private static boolean isPps(byte[] ba)
    {
        return (ba[4] & 0x1f) == 8;
    }

    public VideoDecoder(Context context) {
        mContext = context;
    }

    public void decode(byte[] buffer, int size) {

        synchronized (sLock) {

            if (isSps(buffer))
            {
                int configSize = size - 4;
                mConfigSps = new byte[configSize];
                System.arraycopy(buffer, 4, mConfigSps, 0, configSize);
                Utils.logd("SPS: %d", mConfigSps.length);
            } else if (isPps(buffer)) {
                int configSize = size - 4;
                mConfigPps = new byte[configSize];
                System.arraycopy(buffer, 4, mConfigPps, 0, configSize);
                Utils.logd("PPS: %d", mConfigPps.length);
            }

            if (mCodec == null) {
                Utils.loge("Codec is not initialized");
                return;
            }

            ByteBuffer content = ByteBuffer.wrap(buffer);
            content.limit(size);
            content.position(0);

            while (content.hasRemaining()) {                                 // While there is remaining content...

                if (!codec_input_provide(content)) {                          // Process buffer; if no available buffers...
                    Utils.loge("Dropping content because there are no available buffers.");
                    return;
                }

                codec_output_consume();                                        // Send result to video codec
            }
        }
    }

    private void codec_init() {
        synchronized (sLock) {
            try {
                mCodec = MediaCodec.createDecoderByType("video/avc");       // Create video codec: ITU-T H.264 / ISO/IEC MPEG-4 Part 10, Advanced Video Coding (MPEG-4 AVC)
            } catch (Throwable t) {
                Utils.loge("Throwable creating video/avc decoder: " + t);
            }
            try {
                mCodecBufferInfo = new MediaCodec.BufferInfo();                         // Create Buffer Info
                MediaFormat format = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
                if (mConfigSps != null)
                {
                    int size = mConfigPps != null ? mConfigSps.length + mConfigPps.length : mConfigSps.length;
                    ByteBuffer csd = ByteBuffer.allocate(size).put(mConfigSps);
                    if (mConfigPps != null) {
                        csd.put(mConfigPps);
                    }
                    format.setByteBuffer("csd-0", csd);
                    Utils.logd("CSD-0: %d", csd.limit());
                }
                mCodec.configure(format, mHolder.getSurface(), null, 0);               // Configure codec for H.264 with given width and height, no crypto and no flag (ie decode)
                mCodec.start();                                             // Start codec
            } catch (Throwable t) {
                Utils.loge(t);
            }
        }
    }

    private void codec_stop() {
        synchronized (sLock) {
            if (mCodec != null) {
                mCodec.stop();
            }
            mCodec = null;
            mInputBuffers = null;
            mCodecBufferInfo = null;
        }
    }

    private boolean codec_input_provide(ByteBuffer content) {            // Called only by media_decode() with new NAL unit in Byte Buffer
        try {
            final int inputBufIndex = mCodec.dequeueInputBuffer(1000000);           // Get input buffer with 1 second timeout
            if (inputBufIndex < 0) {
                return false;                                                 // Done with "No buffer" error
            }
            if (mInputBuffers == null) {
                mInputBuffers = mCodec.getInputBuffers();                // Set mInputBuffers if needed
            }

            final ByteBuffer buffer = mInputBuffers[inputBufIndex];

            final int capacity = buffer.capacity();
            buffer.clear();
            if (content.remaining() <= capacity) {                           // If we can just put() the content...
                buffer.put(content);                                           // Put the content
            } else {                                                            // Else... (Should not happen ?)
                Utils.loge("content.hasRemaining (): " + content.hasRemaining() + "  capacity: " + capacity);

                int limit = content.limit();
                content.limit(content.position() + capacity);                 // Temporarily set constrained limit
                buffer.put(content);
                content.limit(limit);                                          // Restore original limit
            }
            buffer.flip();                                                   // Flip buffer for reading

            mCodec.queueInputBuffer(inputBufIndex, 0 /* offset */, buffer.limit(), 0, 0);
            return true;                                                    // Processed
        } catch (Throwable t) {
            Utils.loge(t);
        }
        return false;                                                     // Error: exception
    }

    private void codec_output_consume() {                                // Called only by media_decode() after codec_input_provide()
        int index;
        for (;;) {                                                          // Until no more buffers...
            index = mCodec.dequeueOutputBuffer (mCodecBufferInfo, 0);        // Dequeue an output buffer but do not wait
            if (index >= 0)
                mCodec.releaseOutputBuffer (index, true /*render*/);           // Return the buffer to the codec
            else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)         // See this 1st shortly after start. API >= 21: Ignore as getOutputBuffers() deprecated
                Utils.logd ("INFO_OUTPUT_BUFFERS_CHANGED");
            else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)          // See this 2nd shortly after start. Output format changed for subsequent data. See getOutputFormat()
                Utils.logd ("INFO_OUTPUT_FORMAT_CHANGED");
            else if (index == MediaCodec.INFO_TRY_AGAIN_LATER)
                break;
            else
                break;
        }
        if (index != MediaCodec.INFO_TRY_AGAIN_LATER)
            Utils.loge ("index: " + index);
    }

    public void onSurfaceHolderAvailable(SurfaceHolder holder, int width, int height) {
        mHolder = holder;
        mWidth = width;
        mHeight = (height > 1080) ? 1080 : height;
        codec_init();
    }

    public void stop() {
        codec_stop();
    }

    public int h264_after_get (byte [] ba, int idx) {
        idx += 4; // Pass 0, 0, 0, 1
        for (; idx < ba.length - 4; idx ++) {
            if (idx > 24)   // !!!! HACK !!!! else 0,0,0,1 indicates first size 21, instead of 25
                if (ba [idx] == 0 && ba [idx+1] == 0 && ba [idx+2] == 0 && ba [idx+3] == 1)
                    return (idx);
        }
        return (-1);
    }

}
