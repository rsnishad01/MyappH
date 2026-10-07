package com.example.util.filter

import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.example.ui.filters.BeautySettings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * High-performance GPU-accelerated OpenGL ES 2.0 Renderer for CameraX Beauty Filters & LUTs
 * Renders live video at 30+ FPS with Bilateral Blur, Skin Whitening, Lip Tint, Eye Bright, and 8 LUT Preset Filters.
 */
class BeautyFilterGLRenderer : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    var onSurfaceTextureAvailable: ((SurfaceTexture, Int) -> Unit)? = null

    // Real-time filter parameters
    var beautySettings = BeautySettings()
    var activeFilterMode: Int = 0 // 0 = Beauty, 1..8 = LUT Filters
    var currentFaceLandmarks = FaceMeshHelper.FaceLandmarkData(hasFace = false)

    private var surfaceTexture: SurfaceTexture? = null
    private var cameraTextureId: Int = 0

    private var programId: Int = 0
    private var aPositionHandle: Int = 0
    private var aTextureCoordHandle: Int = 0
    private var uTextureHandle: Int = 0
    private var uSTMatrixHandle: Int = 0

    private var uSkinSmoothnessHandle: Int = 0
    private var uSkinWhiteningHandle: Int = 0
    private var uLipTintHandle: Int = 0
    private var uEyeBrightHandle: Int = 0
    private var uFilterModeHandle: Int = 0
    private var uTexelSizeHandle: Int = 0

    private var uLipBoundsHandle: Int = 0
    private var uLeftEyeBoundsHandle: Int = 0
    private var uRightEyeBoundsHandle: Int = 0

    private var surfaceWidth = 1080
    private var surfaceHeight = 1920

    private val stMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private var updateSurface = false

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer

    // Full screen quad vertices
    private val cubeCoordinates = floatArrayOf(
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f
    )

    // Texture UV coordinates (flipped for Android Camera)
    private val textureCoordinates = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    )

    init {
        vertexBuffer = ByteBuffer.allocateDirect(cubeCoordinates.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(cubeCoordinates)
        vertexBuffer.position(0)

        textureBuffer = ByteBuffer.allocateDirect(textureCoordinates.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureCoordinates)
        textureBuffer.position(0)

        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (programId == 0) {
            Log.e("BeautyGLRenderer", "Failed to create OpenGL ES shader program")
            return
        }

        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTextureCoord")
        uTextureHandle = GLES20.glGetUniformLocation(programId, "uTexture")
        uSTMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix")

        uSkinSmoothnessHandle = GLES20.glGetUniformLocation(programId, "uSkinSmoothness")
        uSkinWhiteningHandle = GLES20.glGetUniformLocation(programId, "uSkinWhitening")
        uLipTintHandle = GLES20.glGetUniformLocation(programId, "uLipTint")
        uEyeBrightHandle = GLES20.glGetUniformLocation(programId, "uEyeBright")
        uFilterModeHandle = GLES20.glGetUniformLocation(programId, "uFilterMode")
        uTexelSizeHandle = GLES20.glGetUniformLocation(programId, "uTexelSize")

        uLipBoundsHandle = GLES20.glGetUniformLocation(programId, "uLipBounds")
        uLeftEyeBoundsHandle = GLES20.glGetUniformLocation(programId, "uLeftEyeBounds")
        uRightEyeBoundsHandle = GLES20.glGetUniformLocation(programId, "uRightEyeBounds")

        // Create OES Camera Texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        cameraTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(cameraTextureId)
        surfaceTexture?.setOnFrameAvailableListener(this)

        onSurfaceTextureAvailable?.invoke(surfaceTexture!!, cameraTextureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(this) {
            if (updateSurface) {
                surfaceTexture?.updateTexImage()
                surfaceTexture?.getTransformMatrix(stMatrix)
                updateSurface = false
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (programId == 0) return

        GLES20.glUseProgram(programId)

        // Set Vertex & Texture coordinates
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle)

        // Bind OES Camera Texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glUniform1i(uTextureHandle, 0)

        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)

        // Pass Beauty Filter Sliders (0 to 100 normalized to 0.0 - 1.0)
        val smoothnessNorm = (beautySettings.skinSmoothness / 100f).coerceIn(0f, 1f)
        val whiteningNorm = (beautySettings.skinWhitening / 100f).coerceIn(0f, 1f)
        val lipTintNorm = (beautySettings.lipTint / 100f).coerceIn(0f, 1f)
        val eyeBrightNorm = (beautySettings.eyeBright / 100f).coerceIn(0f, 1f)

        GLES20.glUniform1f(uSkinSmoothnessHandle, smoothnessNorm)
        GLES20.glUniform1f(uSkinWhiteningHandle, whiteningNorm)
        GLES20.glUniform1f(uLipTintHandle, lipTintNorm)
        GLES20.glUniform1f(uEyeBrightHandle, eyeBrightNorm)
        GLES20.glUniform1i(uFilterModeHandle, activeFilterMode)

        GLES20.glUniform2f(uTexelSizeHandle, 1.0f / surfaceWidth.toFloat(), 1.0f / surfaceHeight.toFloat())

        // Pass Landmark bounding boxes for selective Lip Tint & Eye Brightening
        if (currentFaceLandmarks.hasFace) {
            passRectUniform(uLipBoundsHandle, currentFaceLandmarks.lipBounds)
            passRectUniform(uLeftEyeBoundsHandle, currentFaceLandmarks.leftEyeBounds)
            passRectUniform(uRightEyeBoundsHandle, currentFaceLandmarks.rightEyeBounds)
        } else {
            passRectUniform(uLipBoundsHandle, RectF(0f, 0f, 0f, 0f))
            passRectUniform(uLeftEyeBoundsHandle, RectF(0f, 0f, 0f, 0f))
            passRectUniform(uRightEyeBoundsHandle, RectF(0f, 0f, 0f, 0f))
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle)
    }

    private fun passRectUniform(handle: Int, rect: RectF) {
        if (handle >= 0) {
            GLES20.glUniform4f(handle, rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    override fun onFrameAvailable(st: SurfaceTexture?) {
        synchronized(this) {
            updateSurface = true
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0

        val program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("BeautyGLRenderer", "Could not link program: ${GLES20.glGetProgramInfoLog(program)}")
                GLES20.glDeleteProgram(program)
                return 0
            }
        }
        return program
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e("BeautyGLRenderer", "Could not compile shader $shaderType: ${GLES20.glGetShaderInfoLog(shader)}")
                GLES20.glDeleteShader(shader)
                return 0
            }
        }
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            uniform mat4 uSTMatrix;
            varying vec2 vTextureCoord;
            
            void main() {
                gl_Position = aPosition;
                vTextureCoord = (uSTMatrix * vec4(aTextureCoord, 0.0, 1.0)).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            
            varying vec2 vTextureCoord;
            uniform samplerExternalOES uTexture;
            
            uniform float uSkinSmoothness; // 0.0 to 1.0
            uniform float uSkinWhitening;  // 0.0 to 1.0
            uniform float uLipTint;        // 0.0 to 1.0
            uniform float uEyeBright;      // 0.0 to 1.0
            uniform int uFilterMode;       // 0 = Beauty, 1..8 = LUT Filters
            uniform vec2 uTexelSize;
            
            uniform vec4 uLipBounds;
            uniform vec4 uLeftEyeBounds;
            uniform vec4 uRightEyeBounds;
            
            // Bilateral Blur algorithm for smooth skin texture
            vec3 applyBilateralBlur(samplerExternalOES tex, vec2 uv, vec2 stepSize, float smoothness) {
                vec3 centerColor = texture2D(tex, uv).rgb;
                if (smoothness <= 0.01) return centerColor;
                
                vec3 sum = vec3(0.0);
                float totalWeight = 0.0;
                float sigmaSpatial = 2.0;
                float sigmaRange = 0.18;
                
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        vec2 offset = vec2(float(x), float(y)) * stepSize * 1.5;
                        vec3 sampleColor = texture2D(tex, uv + offset).rgb;
                        
                        float spatialDist = float(x * x + y * y);
                        float spatialWeight = exp(-spatialDist / (2.0 * sigmaSpatial * sigmaSpatial));
                        
                        vec3 diff = centerColor - sampleColor;
                        float rangeDist = dot(diff, diff);
                        float rangeWeight = exp(-rangeDist / (2.0 * sigmaRange * sigmaRange));
                        
                        float weight = spatialWeight * rangeWeight;
                        sum += sampleColor * weight;
                        totalWeight += weight;
                    }
                }
                
                vec3 blurred = sum / max(totalWeight, 0.001);
                return mix(centerColor, blurred, smoothness);
            }
            
            // Color Grading for 8 Extra LUT Filters
            vec3 applyLutFilter(vec3 color, int mode) {
                if (mode == 1) { // 1. Natural Glam
                    color = vec3(
                        pow(color.r, 0.90) * 1.08 + 0.03,
                        pow(color.g, 0.92) * 1.04 + 0.02,
                        pow(color.b, 0.95) * 1.02 + 0.01
                    );
                } else if (mode == 2) { // 2. Vintage Film
                    color = vec3(
                        color.r * 0.90 + color.g * 0.15 + 0.08,
                        color.g * 0.85 + color.b * 0.10 + 0.05,
                        color.b * 0.70 + color.r * 0.15 + 0.12
                    );
                } else if (mode == 3) { // 3. Cyberpunk Neon
                    color = vec3(
                        color.r * 1.35 + 0.10,
                        color.g * 0.90,
                        color.b * 1.45 + 0.15
                    );
                } else if (mode == 4) { // 4. Sunset Glow
                    color = vec3(
                        color.r * 1.25 + 0.12,
                        color.g * 1.05 + 0.05,
                        color.b * 0.75 - 0.02
                    );
                } else if (mode == 5) { // 5. Tokyo Chic
                    color = vec3(
                        color.r * 0.95 + 0.02,
                        color.g * 1.05 + 0.04,
                        color.b * 1.20 + 0.08
                    );
                } else if (mode == 6) { // 6. Cinema Dark
                    color = vec3(
                        pow(color.r, 1.20) * 1.10,
                        pow(color.g, 1.25) * 1.05,
                        pow(color.b, 1.30) * 0.95
                    );
                } else if (mode == 7) { // 7. Monochrome Soft
                    float gray = dot(color, vec3(0.299, 0.587, 0.114));
                    color = vec3(pow(gray, 0.95) + 0.04);
                } else if (mode == 8) { // 8. Rosy Romance
                    color = vec3(
                        color.r * 1.20 + 0.08,
                        color.g * 0.98 + 0.04,
                        color.b * 1.05 + 0.06
                    );
                }
                return clamp(color, 0.0, 1.0);
            }
            
            void main() {
                // 1. Bilateral Blur Skin Smoothing
                vec3 color = applyBilateralBlur(uTexture, vTextureCoord, uTexelSize, uSkinSmoothness);
                
                // 2. Skin Whitening & Luminous Brightness
                if (uSkinWhitening > 0.01) {
                    float brightness = uSkinWhitening * 0.28;
                    color = clamp(color + vec3(brightness), 0.0, 1.0);
                    color = pow(color, vec3(1.0 - uSkinWhitening * 0.18));
                }
                
                // 3. Lip Tint (Pink/Rose overlay on lip landmarks)
                if (uLipTint > 0.01 && uLipBounds.z > uLipBounds.x && uLipBounds.w > uLipBounds.y) {
                    vec2 uv = vTextureCoord;
                    if (uv.x >= uLipBounds.x && uv.x <= uLipBounds.z && uv.y >= uLipBounds.y && uv.y <= uLipBounds.w) {
                        vec2 center = vec2((uLipBounds.x + uLipBounds.z) * 0.5, (uLipBounds.y + uLipBounds.w) * 0.5);
                        vec2 radius = vec2((uLipBounds.z - uLipBounds.x) * 0.5, (uLipBounds.w - uLipBounds.y) * 0.5);
                        vec2 dist = (uv - center) / max(radius, vec2(0.001));
                        float alpha = 1.0 - smoothstep(0.3, 1.0, length(dist));
                        if (alpha > 0.0) {
                            vec3 pinkTint = vec3(0.95, 0.28, 0.52);
                            color = mix(color, mix(color, pinkTint, 0.55), alpha * uLipTint);
                        }
                    }
                }
                
                // 4. Eye Brightening
                if (uEyeBright > 0.01) {
                    vec2 uv = vTextureCoord;
                    bool inLeft = (uv.x >= uLeftEyeBounds.x && uv.x <= uLeftEyeBounds.z && uv.y >= uLeftEyeBounds.y && uv.y <= uLeftEyeBounds.w);
                    bool inRight = (uv.x >= uRightEyeBounds.x && uv.x <= uRightEyeBounds.z && uv.y >= uRightEyeBounds.y && uv.y <= uRightEyeBounds.w);
                    if (inLeft || inRight) {
                        color = clamp(color * (1.0 + uEyeBright * 0.35) + vec3(uEyeBright * 0.08), 0.0, 1.0);
                    }
                }
                
                // 5. Apply LUT Filter if active
                if (uFilterMode > 0) {
                    color = applyLutFilter(color, uFilterMode);
                }
                
                gl_FragColor = vec4(color, 1.0);
            }
        """
    }
}
