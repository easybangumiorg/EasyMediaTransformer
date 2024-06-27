package com.heyanle.easy_transformer.gif

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.view.Surface
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.transformer.Codec
import com.alien.gpuimage.outputs.BitmapOut
import com.alien.gpuimage.outputs.PixelCopyOut
import com.alien.gpuimage.sources.OesTexturePipeline
import com.alien.gpuimage.sources.widget.GLOesTexture
import com.heyanle.easy_transformer.utils.EasyEGLSurface
import com.heyanle.easy_transformer.utils.EasyEGLSurfaceTexture
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * 将 Surface 画面编码为像素帧
 * Created by heyanlin on 2024/6/26.
 */
@UnstableApi
class FramePixelCodec(
    private val configFormat: Format
) : Codec, PixelCopyOut.Callback {


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

    private val reentrantReadWriteLock = ReentrantReadWriteLock()

    @Volatile
    private var lastBufferInfoTime = 0L
    private val bufferCache = ArrayDeque<Pair<Long, ByteBuffer>>()
    private val file = File("/storage/emulated/0/Android/data/com.heyanle.easybangumi4.debug/cache/Recorded/").apply {
        mkdirs()
    }
    private val bitmapOut = BitmapOut().apply {
        callback = object : BitmapOut.BitmapViewCallback {
            override fun onViewSwapToScreen(bitmap: android.graphics.Bitmap?, time: Long?) {
                lastBufferInfoTime = time ?: 0
                val b = bitmap?.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                Thread {
                    b?.let {
                        val file = File(file, "${time}.png")
                        it.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, file.outputStream())
                    }
                }.start()

            }
        }
    }

    private val pixelCopyOut = PixelCopyOut().apply {
        callback = this@FramePixelCodec
    }
    private val oesTexture = OesTexturePipeline().apply {
        setFormat(configFormat.width, configFormat.height, 0)
        //addTarget(pixelCopyOut)
        addTarget(bitmapOut)
    }


    override fun getConfigurationFormat(): Format {
        return configFormat
    }

    override fun getName(): String {
        return TAG
    }

    override fun getInputSurface(): Surface {
        return oesTexture.getSurface()
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

    override fun getOutputFormat(): Format {
        return configFormat
    }

    override fun getOutputBuffer(): ByteBuffer? {
        if (lastBufferInfoTime == 0L){
            return null
        }
        return unused
        reentrantReadWriteLock.write {
            if (bufferCache.isNotEmpty()){
                val buffer = bufferCache.removeFirst()
                lastBufferInfoTime = buffer.first
                return buffer.second
            }
            return null
        }
    }

    override fun getOutputBufferInfo(): MediaCodec.BufferInfo {
        val copy = MediaCodec.BufferInfo()
        copy.set(
            0,
            bufferSize,
            lastBufferInfoTime,
            if (inputStreamEnd) MediaCodec.BUFFER_FLAG_END_OF_STREAM else MediaCodec.BUFFER_FLAG_KEY_FRAME
        )
        return copy
    }

    override fun releaseOutputBuffer(render: Boolean) { }

    override fun releaseOutputBuffer(renderPresentationTimeUs: Long) {
        reentrantReadWriteLock.write {
            bufferCache.removeAll {
                it.first == renderPresentationTimeUs
            }
        }
    }

    override fun isEnded(): Boolean {
        return inputStreamEnd
    }

    override fun release() {
        oesTexture.release()
    }

    override fun onPixelFrameReady(byteBuffer: ByteBuffer, time: Long) {
        val buffer = ByteBuffer.allocate(byteBuffer.capacity()).order(byteBuffer.order())
        byteBuffer.position(0)
        buffer.put(byteBuffer)
        reentrantReadWriteLock.write {
            bufferCache.addLast(Pair(time, buffer))
        }
    }
}