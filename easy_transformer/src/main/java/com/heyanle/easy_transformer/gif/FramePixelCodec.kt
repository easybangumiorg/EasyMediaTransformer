package com.heyanle.easy_transformer.gif

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.view.Surface
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.transformer.Codec
import com.heyanle.easy_transformer.utils.EasyEGLSurface
import com.heyanle.easy_transformer.utils.EasyEGLSurfaceTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 将 Surface 画面编码为像素帧
 * Created by heyanlin on 2024/6/26.
 */
@UnstableApi
class FramePixelCodec(
    private val configFormat: Format
) : Codec, EasyEGLSurfaceTexture.TextureImageListener {


    companion object {
        const val INTERNAL_BUFFER_SIZE = 8196
        private const val TAG = "FramePixelCodec"
    }

    class Factory : Codec.EncoderFactory {
        override fun createForAudioEncoding(format: Format): Codec {
            return FramePixelCodec(format)
        }

        override fun createForVideoEncoding(format: Format): Codec {
            return FramePixelCodec(format)
        }
    }
    private val unused: ByteBuffer =
        ByteBuffer.allocate(INTERNAL_BUFFER_SIZE).order(ByteOrder.nativeOrder())

    private var inputStreamEnd = false
    private val bufferSize = configFormat.width * configFormat.height * 4
    private var presentationTimeUs = 0L
    private val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.LITTLE_ENDIAN)
    private val easyEGLSurface = EasyEGLSurface.newInstance(this)
    private val surfaceTexturePixelCopy =
        SurfaceTexturePixelCopy(easyEGLSurface).apply {
            width = configFormat.width
            height = configFormat.height
        }

    override fun getConfigurationFormat(): Format {
        return configFormat
    }

    override fun getName(): String {
        return TAG
    }

    override fun getInputSurface(): Surface {
        return easyEGLSurface
    }

    override fun maybeDequeueInputBuffer(inputBuffer: DecoderInputBuffer): Boolean {
        if (inputStreamEnd) {
            return false
        }
        inputBuffer.data = this.unused
        return true
    }

    override fun queueInputBuffer(inputBuffer: DecoderInputBuffer) {
        if (inputBuffer.isEndOfStream) {
            inputStreamEnd = true
        }
        inputBuffer.clear()
        inputBuffer.data = null
    }

    override fun signalEndOfInputStream() {
        inputStreamEnd = true
    }

    override fun getOutputFormat(): Format? {
        return configFormat
    }

    override fun getOutputBuffer(): ByteBuffer {
        return byteBuffer
    }

    override fun getOutputBufferInfo(): MediaCodec.BufferInfo {
        val copy = MediaCodec.BufferInfo()
        copy.set(
            0,
            bufferSize,
            presentationTimeUs,
            if (inputStreamEnd) MediaCodec.BUFFER_FLAG_END_OF_STREAM else MediaCodec.BUFFER_FLAG_KEY_FRAME
        )
        return copy
    }

    override fun releaseOutputBuffer(render: Boolean) { }

    override fun releaseOutputBuffer(renderPresentationTimeUs: Long) { }

    override fun isEnded(): Boolean {
        return inputStreamEnd
    }

    override fun release() {
        easyEGLSurface.release()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?): Boolean {
        surfaceTexture ?: return false
        surfaceTexture.updateTexImage()
        surfaceTexturePixelCopy.onFrameAvailable(byteBuffer)
        presentationTimeUs = surfaceTexture.timestamp
        return true
    }
}