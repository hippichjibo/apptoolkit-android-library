package com.jibo.apptoolkit.android.example.ui.fragment.mjpeg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * Created by alexz on 11/28/17.
 */
class MjpegGLSurfaceView(context: Context) : GLSurfaceView(context) {

    @Volatile
    private var isAspectRatioSet: Boolean? = false

    protected var ratio = 1f
    @Volatile
    private var updateNeeded: Boolean? = true
    private var bmp: Bitmap? = null

    private var mRenderer: GLRenderer? = null

    private var surfaceEventsListener: MjpegVideoFragment.SurfaceEventsListener? = null

    fun setSurfaceEventsListener(surfaceEventsListener: MjpegVideoFragment.SurfaceEventsListener) {
        this.surfaceEventsListener = surfaceEventsListener
    }

    init {

        init(context)
    }

    private fun init(context: Context) {
        setEGLContextClientVersion(2)
        mRenderer = GLRenderer(context)
        setRenderer(mRenderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun onResume() {
        super.onResume()
        isAspectRatioSet = false
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        if (surfaceEventsListener != null) surfaceEventsListener?.surfaceDestroyed()
    }

    fun setBmpRecalculate(bitmap: Bitmap) {
        //we need to update vertices to proper propotions of video frame
        synchronized(this) {
            if (!isAspectRatioSet!!) {
                var newWidthRatio = bitmap.width.toFloat() / bitmap.height
                var newHeightRatio = 1f
                if (newWidthRatio > ratio) {
                    newHeightRatio = ratio / newWidthRatio
                    newWidthRatio = ratio
                }
                mRenderer?.updateVertices(newWidthRatio, newHeightRatio)
                mRenderer?.setupSurfaceCoords()
                isAspectRatioSet = true
            }
        }

        this.bmp = bitmap
        updateNeeded = true
    }

    internal inner class GLRenderer(var mContext: Context) : GLSurfaceView.Renderer {

        // Our matrices
        private val mModelMatrix = FloatArray(16)
        private val mViewMatrix = FloatArray(16)
        private val mProjectionMatrix = FloatArray(16)
        private val mMVPMatrix = FloatArray(16)

        private var muMVPMatrixHandle: Int = 0
        private var mTextureUniformHandle: Int = 0

        @Volatile
        var vertices = floatArrayOf(1f, 1f, 0.0f, 1f, -1f, 0.0f, -1f, -1f, 0.0f, -1f, 1f, 0.0f)

        var indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        var uvs = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f)
        private val textureIds = IntArray(1)
        @Volatile
        var vertexBuffer: FloatBuffer? = null
        var drawListBuffer: ShortBuffer? = null
        @Volatile
        var uvBuffer: FloatBuffer? = null
        var mProgram: Int = 0

        fun updateVertices(wRatio: Float, hRatio: Float) {
            synchronized(vertices) {
                vertices = floatArrayOf(wRatio, hRatio, 0.0f, wRatio, -hRatio, 0.0f, -wRatio, -hRatio, 0.0f, -wRatio, hRatio, 0.0f)
            }
        }

        init {

            updateVertices(1f, 1f)
            setupSurfaceCoords()
        }

        fun setupSurfaceCoords() {

            // The vertex buffer.
            vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            vertexBuffer?.put(vertices)
            vertexBuffer?.position(0)

            // initialize byte buffer for the draw list
            drawListBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            drawListBuffer?.put(indices)
            drawListBuffer?.position(0)

            // The texture buffer
            uvBuffer = ByteBuffer.allocateDirect(uvs.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            uvBuffer?.put(uvs)
            uvBuffer?.position(0)
        }

        override fun onDrawFrame(unused: GL10) {

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

            GLES20.glUseProgram(mProgram)

            synchronized(this) {
                if (updateNeeded!! && bmp != null) {
                    loadGLTextureFromBitmap(bmp ?: Bitmap.createBitmap(bmp))
                    bmp?.recycle()
                    bmp = null
                    updateNeeded = false
                }
            }

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

            // get handle to vertex shader's vPosition member
            val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            // Enable generic vertex attribute array
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

            // Get handle to texture coordinates location
            val mTexCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord")
            // Enable generic vertex attribute array
            GLES20.glEnableVertexAttribArray(mTexCoordLoc)
            // Prepare the texturecoordinates
            GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)

            // Get handle to shape's transformation matrix
            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

            Matrix.setIdentityM(mModelMatrix, 0)
            Matrix.scaleM(mModelMatrix, 0, 1f, 1f, 0f)
            Matrix.translateM(mModelMatrix, 0, 0f, 0f, 0f)
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)

            // Get handle to textures locations
            val mSamplerLoc = GLES20.glGetUniformLocation(mProgram, "s_texture")

            // Set the sampler texture unit to 0, where we have saved the texture.
            GLES20.glUniform1i(mSamplerLoc, 0)

            // Draw the triangle
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle)
            GLES20.glDisableVertexAttribArray(mTexCoordLoc)

            GLES20.glFinish()
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            ratio = width.toFloat() / height.toFloat()

            // Redo the Viewport, making it fullscreen.
            GLES20.glViewport(0, 0, width, height)

            updateMatrices(width.toFloat() / height)

            //we need to recalculate all the stuff
            isAspectRatioSet = false

            if (surfaceEventsListener != null) surfaceEventsListener?.surfaceChanged(width, height)
        }

        private fun updateMatrices(ratio: Float) {
            val left = -ratio
            val bottom = -1.0f
            val top = 1.0f
            val near = 1.0f
            val far = 50.0f
            Matrix.orthoM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far)

            // Position the eye behind the origin.
            val eyeX = 0.0f
            val eyeY = 0.0f
            val eyeZ = 1f

            // We are looking toward the distance
            val lookX = 0.0f
            val lookY = 0.0f
            val lookZ = 0f

            // Set our up vector. This is where our head would be pointing were we holding the camera.
            val upX = 0.0f
            val upY = 1f
            val upZ = 0.0f

            Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)
        }

        override fun onSurfaceCreated(gl: GL10, config: javax.microedition.khronos.egl.EGLConfig) {

            // Set the clear color to black
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1f)

            // Create the shaders, images
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs_Image)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs_Image)

            mProgram = GLES20.glCreateProgram()             // create empty OpenGL ES Program
            GLES20.glAttachShader(mProgram, vertexShader)   // add the vertex shader to program
            GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
            GLES20.glLinkProgram(mProgram)                  // creates OpenGL ES program executables

            // Set our shader programm
            GLES20.glUseProgram(mProgram)

            if (surfaceEventsListener != null) surfaceEventsListener?.surfaceCreated()
        }

        private fun loadShader(type: Int, shaderCode: String): Int {

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            val shader = GLES20.glCreateShader(type)

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            // return the shader
            return shader
        }

        private fun loadGLTextureFromBitmap(bitmap: Bitmap) {

            GLES20.glDeleteTextures(1, textureIds, 0)
            GLES20.glGenTextures(1, textureIds, 0)

            mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")

            // Bind texture to texturename
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glUniform1i(mTextureUniformHandle, 0)

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            // Set wrapping mode
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            // Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }

//        companion object {
//            val vs_Image = "uniform mat4 uMVPMatrix;" +
//                           "attribute vec4 vPosition;" +
//                           "attribute vec2 a_texCoord;" +
//                           "varying vec2 v_texCoord;" +
//                           "void main() {" +
//                           "  gl_Position = uMVPMatrix * vPosition;" +
//                           "  v_texCoord = a_texCoord;" +
//                           "}"
//
//            val fs_Image = "precision mediump float;" +
//                           "varying vec2 v_texCoord;" +
//                           "uniform sampler2D s_texture;" +
//                           "void main() {" +
//                           "  gl_FragColor = texture2D( s_texture, v_texCoord );" +
//                           "}"
//        }

    }

    companion object {
        private val TAG = MjpegGLSurfaceView::class.java.name

        val vs_Image = "uniform mat4 uMVPMatrix;" +
                       "attribute vec4 vPosition;" +
                       "attribute vec2 a_texCoord;" +
                       "varying vec2 v_texCoord;" +
                       "void main() {" +
                       "  gl_Position = uMVPMatrix * vPosition;" +
                       "  v_texCoord = a_texCoord;" +
                       "}"

        val fs_Image = "precision mediump float;" +
                       "varying vec2 v_texCoord;" +
                       "uniform sampler2D s_texture;" +
                       "void main() {" +
                       "  gl_FragColor = texture2D( s_texture, v_texCoord );" +
                       "}"
    }

}
