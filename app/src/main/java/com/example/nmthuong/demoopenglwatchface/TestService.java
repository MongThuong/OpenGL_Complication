package com.example.nmthuong.demoopenglwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.util.TimeZone;

/*
 * Created by 123725 on 3/19/18.
 */

public class TestService extends Gles2WatchFaceService {

    private static final String TAG = "TestService";


    /*Create List Compllication*/

    // TODO: Step 2, intro 1
    private static final int LEFT_COMPLICATION_ID = 0;
    private static final int RIGHT_COMPLICATION_ID = 1;
    private static final int BOTTOM_COMPLICATION_ID = 2;

    private Canvas mCanvas;
    private static final int[] COMPLICATION_IDS = {LEFT_COMPLICATION_ID,
            RIGHT_COMPLICATION_ID, BOTTOM_COMPLICATION_ID};

    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },

            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };


    // Used by {@link ComplicationConfigActivity} to retrieve id for complication locations and
    // to check if complication location is supported.
    // TODO: Step 3, expose complication information, part 1
    static int getComplicationId(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ComplicationConfigActivity} to retrieve all complication ids.
    // TODO: Step 3, expose complication information, part 2
    static int[] getComplicationIds()
    {
        return COMPLICATION_IDS;
    }

    // Used by {@link ComplicationConfigActivity} to retrieve complication types supported by
    // location.
    // TODO: Step 3, expose complication information, part 3
    static int[] getSupportedComplicationTypes(
            ComplicationConfigActivity.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case BOTTOM:
                return  COMPLICATION_SUPPORTED_TYPES[2];
            default:
                return new int[] {};
        }
    }

    /*----- Create Engine -----*/
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends Gles2WatchFaceService.Engine  {
        /**
         * Projection transformation matrix. Converts from 3D to 2D.
         */
        private final float[] mProjectionMatrix = new float[16];

        /**
         * View transformation matrices to use in interactive mode. Converts from world to camera-
         * relative coordinates. One matrix per camera position.
         */
        private final float[] mViewMatrices = new float[16];

        /**
         * The view transformation matrix to use in ambient mode
         */
        private final float[] mAmbientViewMatrix = new float[16];

        /**
         * Model transformation matrices. Converts from model-relative coordinates to world
         * coordinates. One matrix per degree of rotation.
         */
        private final float[][] mModelMatrices = new float[360][16];

        /**
         * Products of {@link #mViewMatrices} and {@link #mProjectionMatrix}. One matrix per camera
         * position.
         */
        private final float[] mVpMatrices = new float[16];

        /**
         * The product of {@link #mAmbientViewMatrix} and {@link #mProjectionMatrix}
         */
        private final float[] mAmbientVpMatrix = new float[16];

        /**
         * Product of {@link #mModelMatrices}, {@link #mViewMatrices}, and
         * {@link #mProjectionMatrix}.
         */
        private final float[] mMvpMatrix = new float[16];

        /**
         * Triangles for the 4 major ticks. These are grouped together to speed up rendering.
         */
        private Gles2ColoredTriangleList mMajorTickTriangles;

        /**
         * Triangles for the 8 minor ticks. These are grouped together to speed up rendering.
         */
        private Gles2ColoredTriangleList mMinorTickTriangles;

        /**
         * Triangle for the second hand.
         */
        private Gles2ColoredTriangleList mSecondHandTriangle;

        /**
         * Triangle for the minute hand.
         */
        private Gles2ColoredTriangleList mMinuteHandTriangle;

        /**
         * Triangle for the hour hand.
         */
        private Gles2ColoredTriangleList mHourHandTriangle;

        private Time mTime = new Time();

        /**
         * Whether we've registered {@link #mTimeZoneReceiver}.
         */
        private boolean mRegisteredTimeZoneReceiver;
        private Bitmap mBackgroundBitmap;

        private SurfaceHolder holder;
        private boolean hasActiveHolder = false;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(surfaceHolder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(TestService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());


//            mCanvas = surfaceHolder.getSurface().lockHardwareCanvas();
//            holder = surfaceHolder;

            // TODO: Step 2, intro 3
            initializeComplications();

        }

        /*-- Init Complications --*/

        // TODO: Step 2, initializeComplications()
        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            leftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            rightComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable bottomComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            bottomComplicationDrawable.setContext(getApplicationContext());


            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);

            setActiveComplications(COMPLICATION_IDS);
//           mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
//            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
        }

        /*-- Implementation onComplicationUpdate function --*/


        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);
            complicationDrawable.setBorderColorActive(Color.BLUE);

            Rect bounds = new Rect();
            if (complicationData.getType() == ComplicationData.TYPE_LONG_TEXT) {
            } else {
            }
            complicationDrawable.setBounds(bounds);

            invalidate();
        }


        /*-- GL Override functon --*/
        @Override
        public void onGlContextCreated() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated");
            }
            super.onGlContextCreated();

            // Draw Image on Background


            // Create program for drawing triangles.
            Gles2ColoredTriangleList.Program triangleProgram =
                    new Gles2ColoredTriangleList.Program();
            triangleProgram.use();

            // Create triangles for the ticks.
            mMajorTickTriangles = createMajorTicks(triangleProgram);
            mMinorTickTriangles = createMinorTicks(triangleProgram);

            // Create triangles for the hands.
            mSecondHandTriangle = createHand(
                    triangleProgram,
                    0.02f /* width */,
                    1.0f /* height */,
                    new float[]{
                            1.0f /* red */,
                            0.0f /* green */,
                            0.0f /* blue */,
                            1.0f /* alpha */
                    }
            );
            mMinuteHandTriangle = createHand(
                    triangleProgram,
                    0.06f /* width */,
                    0.8f /* height */,
                    new float[]{
                            0.7f /* red */,
                            0.7f /* green */,
                            0.7f /* blue */,
                            1.0f /* alpha */
                    }
            );
            mHourHandTriangle = createHand(
                    triangleProgram,
                    0.1f /* width */,
                    0.5f /* height */,
                    new float[]{
                            0.9f /* red */,
                            0.9f /* green */,
                            0.9f /* blue */,
                            1.0f /* alpha */
                    }
            );

            // Precompute the clock angles.
            for (int i = 0; i < mModelMatrices.length; ++i) {
                Matrix.setRotateM(mModelMatrices[i], 0, i, 0, 0, 1);
            }

            Matrix.setLookAtM(mViewMatrices, 0, 0, 0, -3,
                    0, 0, 0,
                    0, 1, 0);
            Matrix.setLookAtM(mAmbientViewMatrix, 0, 0, 0, -3,
                    0, 0, 0,
                    0, 1, 0);
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlSurfaceCreated: " + width + " x " + height);
            }
            super.onGlSurfaceCreated(width, height);

            // Update the projection matrix based on the new aspect ratio.
            final float aspectRatio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 2, 7);

            Matrix.multiplyMM(mVpMatrices, 0, mProjectionMatrix, 0, mViewMatrices, 0);
            Matrix.multiplyMM(mAmbientVpMatrix, 0, mProjectionMatrix, 0, mAmbientViewMatrix, 0);
            setComplicationLocation(width,height);
        }

        /*-- Set Complication location --*/

        private  void setComplicationLocation(int width, int height) {

            Log.d(TAG,"set complication location was called");
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect((midpointOfScreen - sizeOfComplication ),
                            (verticalOffset + sizeOfComplication),
                            (midpointOfScreen + horizontalOffset*2),
                            (verticalOffset + sizeOfComplication * 2));

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);
        }


        /**
         * Creates a triangle for a hand on the watch face.
         *
         * @param program program for drawing triangles
         * @param width   width of base of triangle
         * @param length  length of triangle
         * @param color   color in RGBA order, each in the range [0, 1]
         */
        private Gles2ColoredTriangleList createHand(Gles2ColoredTriangleList.Program program,
                                                    float width, float length, float[] color) {
            // Create the data for the VBO.
            float[] triangleCoords = new float[]{
                    // in counterclockwise order:
                    0, length, 0,   // top
                    -width / 2, 0, 0,   // bottom left
                    width / 2, 0, 0    // bottom right
            };
            return new Gles2ColoredTriangleList(program, triangleCoords, color);
        }

        /**
         * Creates a triangle list for the major ticks on the watch face.
         *
         * @param program program for drawing triangles
         */
        private Gles2ColoredTriangleList createMajorTicks(Gles2ColoredTriangleList.Program program) {
            // Create the data for the VBO.
            float[] trianglesCoords = new float[9 * 4];
            for (int i = 0; i < 4; i++) {
                float[] triangleCoords = getMajorTickTriangleCoords(i);
                System.arraycopy(triangleCoords, 0, trianglesCoords, i * 9, triangleCoords.length);
            }

            return new Gles2ColoredTriangleList(program, trianglesCoords,
                    new float[]{
                            1.0f /* red */,
                            1.0f /* green */,
                            1.0f /* blue */,
                            1.0f /* alpha */
                    }
            );
        }

        /**
         * Creates a triangle list for the minor ticks on the watch face.
         *
         * @param program program for drawing triangles
         */
        private Gles2ColoredTriangleList createMinorTicks(
                Gles2ColoredTriangleList.Program program) {
            // Create the data for the VBO.
            float[] trianglesCoords = new float[9 * (12 - 4)];
            int index = 0;
            for (int i = 0; i < 12; i++) {
                if (i % 3 == 0) {
                    // This is where a major tick goes, so skip it.
                    continue;
                }
                float[] triangleCoords = getMinorTickTriangleCoords(i);
                System.arraycopy(triangleCoords, 0, trianglesCoords, index, triangleCoords.length);
                index += 9;
            }

            return new Gles2ColoredTriangleList(program, trianglesCoords,
                    new float[]{
                            0.5f /* red */,
                            0.5f /* green */,
                            0.5f /* blue */,
                            1.0f /* alpha */
                    }
            );
        }

        private float[] getMajorTickTriangleCoords(int index) {
            return getTickTriangleCoords(0.03f, 0.09f, index * 360 / 4);
        }

        private float[] getMinorTickTriangleCoords(int index) {
            return getTickTriangleCoords(0.02f, 0.06f, index * 360 / 12);
        }

        private float[] getTickTriangleCoords(float width, float length, int angleDegrees) {
            float[] coords = new float[]{
                    0, 1, 0,   // top
                    width / 2, length + 1, 0,   // bottom left
                    -width / 2, length + 1, 0    // bottom right
            };

            rotateCoords(coords, angleDegrees);
            return coords;
        }

        /**
         * Destructively rotates the given coordinates in the XY plane about the origin by the given
         * angle.
         *
         * @param coords       flattened 3D coordinates
         * @param angleDegrees angle in degrees clockwise when viewed from negative infinity on the
         *                     Z axis
         */
        private void rotateCoords(float[] coords, int angleDegrees) {
            double angleRadians = Math.toRadians(angleDegrees);
            double cos = Math.cos(angleRadians);
            double sin = Math.sin(angleRadians);
            for (int i = 0; i < coords.length; i += 3) {
                float x = coords[i];
                float y = coords[i + 1];
                coords[i] = (float) (cos * x - sin * y);
                coords[i + 1] = (float) (sin * x + cos * y);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);

            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(inAmbientMode);
            }

            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we were detached.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();

                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TestService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TestService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }


        @Override
        public void onDraw() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }

            super.onDraw();

            final float[] vpMatrix;


            // Draw background color and select the appropriate view projection matrix. The
            // background should always be black in ambient mode. The view projection matrix used is
            // overhead in ambient. In interactive mode, it's tilted depending on the current time.
            if (isInAmbientMode()) {
                GLES20.glClearColor(0, 0, 0, 1);
                vpMatrix = mAmbientVpMatrix;
            } else {
                GLES20.glClearColor(0.0f, 0.2f, 0.2f, 1);
                vpMatrix = mVpMatrices;
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Compute angle indices for the three hands.
            mTime.setToNow();
            final int secIndex = mTime.second * 360 / 60;
            final int minIndex = mTime.minute * 360 / 60;
            final int hoursIndex = (mTime.hour % 12) * 360 / 12 + mTime.minute * 360 / 60 / 12;

            // Draw triangles from back to front. Don't draw the second hand in ambient mode.
            {
                // Combine the model matrix with the projection and camera view.
                Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[hoursIndex], 0);

                // Draw the triangle.
                mHourHandTriangle.draw(mMvpMatrix);
            }
            {
                // Combine the model matrix with the projection and camera view.
                Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[minIndex], 0);

                // Draw the triangle.
                mMinuteHandTriangle.draw(mMvpMatrix);
            }
            if (!isInAmbientMode()) {
                // Combine the model matrix with the projection and camera view.
                Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[secIndex], 0);

                // Draw the triangle.
                mSecondHandTriangle.draw(mMvpMatrix);
            }
            {
                // Draw the major and minor ticks.
                mMajorTickTriangles.draw(vpMatrix);
                mMinorTickTriangles.draw(vpMatrix);
            }
      //      drawComplications(mCanvas);
      //      holder.unlockCanvasAndPost(mCanvas);


            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        private void drawComplications(Canvas canvas) {
                long now = System.currentTimeMillis();
                int complicationId;
                ComplicationDrawable complicationDrawable;

                for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                    complicationId = COMPLICATION_IDS[i];
                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    complicationDrawable.draw(canvas, now);
                    Log.d(TAG, "draw complications was called");
                }
        }


    }
}
