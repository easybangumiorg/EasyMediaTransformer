package com.heyanle.easy_transformer.gif

import android.graphics.Bitmap
import android.media.MediaCodec
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.Muxer
import androidx.media3.transformer.InAppMuxer
import com.google.common.collect.ImmutableList
import java.io.BufferedOutputStream
import java.io.File
import java.nio.ByteBuffer

/**
 * Created by heyanlin on 2024/6/26.
 */
@UnstableApi
class GifMuxer(
    path: String,
) : Muxer {

    class Factory : Muxer.Factory {

        override fun create(path: String): Muxer {
            return GifMuxer(path)
        }

        override fun getSupportedSampleMimeTypes(trackType: Int): ImmutableList<String> {
            return if (trackType == C.TRACK_TYPE_VIDEO) {
                ImmutableList.of(MimeTypes.VIDEO_H264)
            } else {
                ImmutableList.of()
            }
        }
    }

    private val file = File(path)
    private val outputStream = BufferedOutputStream(file.outputStream())
    private val animatedGifEncoder = AnimatedGifEncoder().apply {
        start(outputStream)
    }
    private var presentationTimeUs = -1L
    private var lastGifAppendTimeUs = -1L
    private var bmp: Bitmap? = null
    private var format: Format? = null

    override fun addTrack(format: Format): Muxer.TrackToken {
        this.format = format
        return object : Muxer.TrackToken { }
    }

    override fun addMetadataEntry(metadataEntry: Metadata.Entry) { }

    override fun writeSampleData(
        trackToken: Muxer.TrackToken,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        if (presentationTimeUs == -1L) {
            presentationTimeUs = bufferInfo.presentationTimeUs
        } else {
            val diff = bufferInfo.presentationTimeUs - presentationTimeUs
            animatedGifEncoder.setDelay((diff / 1000).toInt())
            animatedGifEncoder.addFrame(bmp)
            lastGifAppendTimeUs = bufferInfo.presentationTimeUs
        }

        var b = bmp
        val f = format
        if (b == null && f != null){
            b = Bitmap.createBitmap(f.width, f.height, Bitmap.Config.ARGB_8888)
        }
        b?.copyPixelsFromBuffer(byteBuffer)
        bmp = b

    }



    override fun close() {
        val b = bmp
        if (lastGifAppendTimeUs != presentationTimeUs && b != null){
            animatedGifEncoder.setDelay(((presentationTimeUs - lastGifAppendTimeUs)/100).toInt())
            animatedGifEncoder.addFrame(bmp)
        }
        animatedGifEncoder.finish()
        outputStream.close()
        bmp?.recycle()

    }
}