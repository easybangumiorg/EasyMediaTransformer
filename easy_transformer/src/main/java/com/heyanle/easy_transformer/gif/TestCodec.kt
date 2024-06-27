package com.heyanle.easy_transformer.gif

import android.content.Context
import android.media.MediaCodec
import android.view.Surface
import android.view.SurfaceView
import androidx.media3.common.Format
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.video.PlaceholderSurface
import androidx.media3.transformer.Codec
import androidx.media3.transformer.Codec.EncoderFactory
import com.alien.gpuimage.outputs.BitmapOut
import com.alien.gpuimage.outputs.PixelCopyOut
import com.alien.gpuimage.sources.OesTexturePipeline
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by heyanle on 2024/6/27.
 * https://github.com/heyanLE
 */
@UnstableApi
class TestEncoder(context: Context, format: Format, private val surfaceView: SurfaceView) :
    Codec {
    class Factory(private val context: Context, private val  surfaceView: SurfaceView) : EncoderFactory {
        override fun createForAudioEncoding(format: Format): Codec {
            return TestEncoder(context, format, surfaceView = surfaceView)
        }

        override fun createForVideoEncoding(format: Format): Codec {
            return TestEncoder(context, format, surfaceView)
        }
    }

    private val context: Context
    private val configurationFormat: Format
    private val buffer: ByteBuffer
    private var inputStreamEnded = false

    init {
        this.context = context
        configurationFormat = format
        buffer = ByteBuffer.allocateDirect(INTERNAL_BUFFER_SIZE).order(ByteOrder.nativeOrder())
    }

    private val file = File("/storage/emulated/0/Android/data/com.heyanle.easybangumi4.debug/cache/Recorded/").apply {
        mkdirs()
    }
    private val bitmapOut = BitmapOut().apply {
        callback = object : BitmapOut.BitmapViewCallback {
            override fun onViewSwapToScreen(bitmap: android.graphics.Bitmap?, time: Long?) {
                val b = bitmap?.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                Thread {
                    b?.let {
                        val file = File(file, "${time}.png")
                        it.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, file.outputStream())
                    }
                }.start()

            }
        }
    }
    private val oesTexture = OesTexturePipeline().apply {
        setFormat(configurationFormat.width, configurationFormat.height, 0)
        //addTarget(pixelCopyOut)
        addTarget(bitmapOut)
    }



    override fun getName(): String {
        return TAG
    }

    override fun getConfigurationFormat(): Format {
        return configurationFormat
    }

    override fun getInputSurface(): Surface {
        return surfaceView.holder.surface
    }

    override fun maybeDequeueInputBuffer(inputBuffer: DecoderInputBuffer): Boolean {
        if (inputStreamEnded) {
            return false
        }
        inputBuffer.data = buffer
        return true
    }

    override fun queueInputBuffer(inputBuffer: DecoderInputBuffer) {
        Assertions.checkState(
            !inputStreamEnded, "Input buffer can not be queued after the input stream has ended."
        )
        if (inputBuffer.isEndOfStream) {
            inputStreamEnded = true
        }
        inputBuffer.clear()
        inputBuffer.data = null
    }

    override fun signalEndOfInputStream() {
        inputStreamEnded = true
    }

    override fun getOutputFormat(): Format {
        return configurationFormat
    }

    override fun getOutputBuffer(): ByteBuffer? {
        return null
    }

    override fun getOutputBufferInfo(): MediaCodec.BufferInfo? {
        return null
    }

    override fun isEnded(): Boolean {
        return inputStreamEnded
    }

    override fun releaseOutputBuffer(render: Boolean) {}
    override fun releaseOutputBuffer(renderPresentationTimeUs: Long) {}
    override fun release() {}

    companion object {
        private const val TAG = "DroppingEncoder"
        private const val INTERNAL_BUFFER_SIZE = 8196
    }
}
