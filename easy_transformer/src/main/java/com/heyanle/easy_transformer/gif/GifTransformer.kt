package com.heyanle.easy_transformer.gif

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Transformer

/**
 * Created by heyanlin on 2024/6/26.
 */
@UnstableApi
class GifTransformer {
    companion object {

        // 视频转 Gif
        // 1. 设置编码器为 FramePixelCodec
        // 2. 设置复用器为 GifMuxer
        fun build(transformer: Transformer): Transformer {
            return transformer
                .buildUpon()
                .setEncoderFactory(FramePixelCodec.Factory())
                .setMuxerFactory(
                    GifMuxer.Factory()
                )
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setRemoveAudio(true)
                .build()
        }
    }
}