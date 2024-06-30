package com.heyanle.easy_transformer.gif

import android.graphics.Rect
import android.graphics.RectF
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.MediaSource

/**
 * Created by heyanle on 2024/6/28.
 * https://github.com/heyanLE
 */
class GifTransformer(
    private val videoRectF: RectF,
    private val cropRectF: RectF,

    private val speed: Float,
    private val fps: Float,
    private val startPosition: Long,
    private val endPosition: Long,

    private val mediaItem: MediaItem,
    private val mediaSourceFactory: MediaSource.Factory
) {



}