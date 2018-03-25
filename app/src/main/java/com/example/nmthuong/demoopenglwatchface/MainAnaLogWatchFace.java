package com.example.nmthuong.demoopenglwatchface;

import android.content.Context;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MainAnaLogWatchFace extends Gles2WatchFaceService {

    /* Expected frame rate in interactive mode */
    private static final long FPS = 60;
    private float[][] mModelMatrix = new float[360][16];
    private float[] mViewMatrix = new float[16];
    private float[] mAmbientViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mVPMatrix = new float[16];
    private float[] mAmbiantVPMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private int mPerVertexProgramHandle;
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;

    /**
     * How many bytes per float.
     */
    private final int mBytesPerFloat = 4;

    /**
     * How many elements per vertex.
     */
    private final int mStrideBytes = 7 * mBytesPerFloat;

    /**
     * Offset of the position data.
     */
    private final int mPositionOffset = 0;

    /**
     * Size of the position data in elements.
     */
    private final int mPositionDataSize = 3;

    /**
     * Offset of the color data.
     */
    private final int mColorOffset = 3;

    /**
     * Size of the color data in elements.
     */
    private final int mColorDataSize = 4;

    private FloatBuffer mTriangle1Vertices;
    private FloatBuffer mTriangle2Vertices;
    private FloatBuffer mTriangle3Vertices;

    @Override
    public MyEngine onCreateEngine() {
        return new MyEngine(getApplicationContext());
    }

    protected class MyEngine extends Gles2WatchFaceService.Engine {

        private Context mActivityContext;

        public MyEngine(Context myContext) {
            mActivityContext = myContext;
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MainAnaLogWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onGlContextCreated() {
            super.onGlContextCreated();


            final float[] triangle1VerticesData = {
                    // X, Y, Z,
                    // R, G, B, A
                    -0.5f, -0.25f, 0.0f,
                    1.0f, 0.0f, 0.0f, 1.0f,
                    0.5f, -0.25f, 0.0f,
                    0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.559016994f, 0.0f,
                    0.0f, 1.0f, 0.0f, 1.0f};
            // This triangle is yellow, cyan, and magenta.
            final float[] triangle2VerticesData = {
                    // X, Y, Z,
                    // R, G, B, A
                    -0.5f, -0.25f, 0.0f,
                    1.0f, 1.0f, 0.0f, 1.0f,
                    0.5f, -0.25f, 0.0f,
                    0.0f, 1.0f, 1.0f, 1.0f,
                    0.0f, 0.559016994f, 0.0f,
                    1.0f, 0.0f, 1.0f, 1.0f};
            // This triangle is white, gray, and black.
            final float[] triangle3VerticesData = {
                    // X, Y, Z,
                    // R, G, B, A
                    -0.5f, -0.25f, 0.0f,
                    1.0f, 1.0f, 1.0f, 1.0f,
                    0.5f, -0.25f, 0.0f,
                    0.5f, 0.5f, 0.5f, 1.0f,
                    0.0f, 0.559016994f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f};
            // Initialize the buffers.
            mTriangle1Vertices = ByteBuffer.allocateDirect(triangle1VerticesData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangle2Vertices = ByteBuffer.allocateDirect(triangle2VerticesData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangle3Vertices = ByteBuffer.allocateDirect(triangle3VerticesData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangle1Vertices.put(triangle1VerticesData).position(0);
            mTriangle2Vertices.put(triangle2VerticesData).position(0);
            mTriangle3Vertices.put(triangle3VerticesData).position(0);

            Matrix.setLookAtM(mViewMatrix, 0, 0,
                    0, -3, 0,
                    0, 0, 0,
                    1, 0);
            Matrix.setLookAtM(mAmbientViewMatrix, 0, 0, 0,
                    -3, 0, 0,
                    0, 0, 1, 0);

            final String vertexShader =
                    "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.
                            + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                            + "attribute vec4 a_Color;        \n"        // Per-vertex color information we will pass in.
                            + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.
                            + "void main()                    \n"        // The entry point for our vertex shader.
                            + "{                              \n"
                            + "   v_Color = a_Color;          \n"        // Pass the color through to the fragment shader.
                            // It will be interpolated across the triangle.
                            + "   gl_Position = u_MVPMatrix   \n"    // gl_Position is a special variable used to store the final position.
                            + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                            + "}                              \n";    // normalized screen coordinates.
            final String fragmentShader =
                    "precision mediump float;       \n"        // Set the default precision to medium. We don't need as high of a
                            // precision in the fragment shader.
                            + "varying vec4 v_Color;          \n"        // This is the color from the vertex shader interpolated across the
                            // triangle per fragment.
                            + "void main()                    \n"        // The entry point for our fragment shader.
                            + "{                              \n"
                            + "   gl_FragColor = v_Color;     \n"        // Pass the color directly through the pipeline.
                            + "}                              \n";
            // Load in the vertex shader.
            int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            if (vertexShaderHandle != 0) {
                // Pass in the shader source.
                GLES20.glShaderSource(vertexShaderHandle, vertexShader);
                // Compile the shader.
                GLES20.glCompileShader(vertexShaderHandle);
                // Get the compilation status.
                final int[] compileStatus = new int[1];
                GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
                // If the compilation failed, delete the shader.
                if (compileStatus[0] == 0) {
                    GLES20.glDeleteShader(vertexShaderHandle);
                    vertexShaderHandle = 0;
                }
            }
            if (vertexShaderHandle == 0) {
                throw new RuntimeException("Error creating vertex shader.");
            }
            // Load in the fragment shader shader.
            int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            if (fragmentShaderHandle != 0) {
                // Pass in the shader source.
                GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);
                // Compile the shader.
                GLES20.glCompileShader(fragmentShaderHandle);
                // Get the compilation status.
                final int[] compileStatus = new int[1];
                GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
                // If the compilation failed, delete the shader.
                if (compileStatus[0] == 0) {
                    GLES20.glDeleteShader(fragmentShaderHandle);
                    fragmentShaderHandle = 0;
                }
            }
            if (fragmentShaderHandle == 0) {
                throw new RuntimeException("Error creating fragment shader.");
            }
            Log.e("Q", "hehe");
            // Create a program object and store the handle to it.
            int programHandle = GLES20.glCreateProgram();
            if (programHandle != 0) {
                // Bind the vertex shader to the program.
                GLES20.glAttachShader(programHandle, vertexShaderHandle);
                // Bind the fragment shader to the program.
                GLES20.glAttachShader(programHandle, fragmentShaderHandle);
                // Bind attributes
                GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
                GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
                // Link the two shaders together into a program.
                GLES20.glLinkProgram(programHandle);
                // Get the link status.
                final int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
                // If the link failed, delete the program.
                if (linkStatus[0] == 0) {
                    GLES20.glDeleteProgram(programHandle);
                    programHandle = 0;
                }
            }
            if (programHandle == 0) {
                throw new RuntimeException("Error creating program.");
            }
            // Set program handles. These will later be used to pass in values to the program.
            mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
            mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
            mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
            // Tell OpenGL to use this program when rendering.
            GLES20.glUseProgram(programHandle);

        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            super.onGlSurfaceCreated(width, height);

            //TODO:  Not clear about aspectRatio
            final float aspectRatio = (float) width / height; // 1 Not clear
            Matrix.frustumM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 2, 7);

            Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            Matrix.multiplyMM(mAmbiantVPMatrix, 0, mProjectionMatrix, 0, mAmbientViewMatrix, 0);
        }

        @Override
        public void onDraw() {
            super.onDraw();
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            // Do a complete rotation every 10 seconds.
            long time = SystemClock.uptimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            // Draw the triangle facing straight on.
            Matrix.setIdentityM(mModelMatrix[10], 0);
            Matrix.rotateM(mModelMatrix[10], 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
            Matrix.rotateM(mModelMatrix[10], 0, 90.0f, 0.0f, 0.0f, 1.0f);
            Matrix.translateM(mModelMatrix[10],0,0,-1,0f);
            drawTriangle(mTriangle1Vertices);


            if (isVisible() && !isInAmbientMode()) {
             //   invalidate();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
        //    invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

        }

        private int compileShader(final int shaderType, final String shaderSource) {
            int shaderHandle = GLES20.glCreateShader(shaderType);

            if (shaderHandle != 0) {
                // Pass in the shader source.
                GLES20.glShaderSource(shaderHandle, shaderSource);

                // Compile the shader.
                GLES20.glCompileShader(shaderHandle);

                // Get the compilation status.
                final int[] compileStatus = new int[1];
                GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

                // If the compilation failed, delete the shader.
                if (compileStatus[0] == 0) {
                    Log.e("a", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                    GLES20.glDeleteShader(shaderHandle);
                    shaderHandle = 0;
                }
            }

            if (shaderHandle == 0) {
                throw new RuntimeException("Error creating shader.");
            }

            return shaderHandle;
        }

        private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
            int programHandle = GLES20.glCreateProgram();

            if (programHandle != 0) {
                // Bind the vertex shader to the program.
                GLES20.glAttachShader(programHandle, vertexShaderHandle);

                // Bind the fragment shader to the program.
                GLES20.glAttachShader(programHandle, fragmentShaderHandle);

                // Bind attributes
                if (attributes != null) {
                    final int size = attributes.length;
                    for (int i = 0; i < size; i++) {
                        GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                    }
                }

                // Link the two shaders together into a program.
                GLES20.glLinkProgram(programHandle);

                // Get the link status.
                final int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

                // If the link failed, delete the program.
                if (linkStatus[0] == 0) {
                    Log.e("a", "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                    GLES20.glDeleteProgram(programHandle);
                    programHandle = 0;
                }
            }

            if (programHandle == 0) {
                throw new RuntimeException("Error creating program.");
            }

            return programHandle;
        }

        protected String getVertexShader() {
            return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
        }

        protected String getFragmentShader() {
            return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
        }

        private void drawTriangle(final FloatBuffer aTriangleBuffer) {
            // Pass in the position information
            aTriangleBuffer.position(mPositionOffset);
            GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                    mStrideBytes, aTriangleBuffer);

            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Pass in the color information
            aTriangleBuffer.position(mColorOffset);
            GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                    mStrideBytes, aTriangleBuffer);

            GLES20.glEnableVertexAttribArray(mColorHandle);

            // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
            // (which currently contains model * view).
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix[10], 0);

            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

        }


    }


}


