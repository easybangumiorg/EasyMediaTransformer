package com.heyanle.easy_transformer.gif

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.heyanle.easy_transformer.utils.EasyEGLSurfaceTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by heyanlin on 2024/6/26.
 */
@UnstableApi
class SurfaceTexturePixelCopy(
    private val textureId: Int,
) {

    companion object {
        private const val TAG = "SurfaceTexturePixelCopy"
    }

    var width: Int = 0
    var height: Int = 0

    private var fboId: Int = 0

    fun onFrameAvailable(byteBuffer: ByteBuffer) {
        if (fboId == 0){
            val int = IntArray(1)
            GLES20.glGenFramebuffers(1, int, 0)
            fboId = int[0]
            Log.i(TAG, "create fbo $fboId")
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            textureId,
            0
        )
        val fboStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (fboStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "initFBO failed, status: $fboStatus")
        }

        byteBuffer.rewind()
        byteBuffer.position(0)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }
}