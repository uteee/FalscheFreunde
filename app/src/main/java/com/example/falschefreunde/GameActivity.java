package com.example.falschefreunde;
/*
 * IMPORTANT!
 * For this file the given example
 * https://github.com/googlearchive/android-AccelerometerPlay
 * was used and modified
 */

    /*
     * Copyright (C) 2010 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

import android.annotation.TargetApi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

/**
 * Class for the whole game, its view and functions
 */
public class GameActivity extends AppCompatActivity {
    /**
     * Create an instance of the SensorManager
     */
    private SensorManager sensorManager;
    /**
     * Create an instance of the PowerManager
     */
    private PowerManager powerManager;
    /**
     * Create an instance of the WindowManager
     */
    private WindowManager windowManager;
    /**
     * Create an instance of Display
     */
    private Display display;
    /**
     * Create an instance of DisplayMetrics
     */
    private DisplayMetrics displaymetrics;
    /**
     * Create an instance of PawerManager.WakeLock
     */
    private PowerManager.WakeLock wakeLock;
    /**
     * Create an instance of GameView
     */
    private GameView gameView;

    /**
     * Create an instance of ActionBar
     */
    public ActionBar actionbar;

    /**
     * Called when GameActivity is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();

        displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        actionbar = getSupportActionBar();

        actionbar.setDisplayHomeAsUpEnabled(true);

        gameView = new GameView(this);

        gameView.setBackgroundResource(R.drawable.wood);

        setContentView(gameView);
    }

    /**
     * Return to Menu, when back button in actionbar gets clicked
     * Made this with help of:
     * https://www.geeksforgeeks.org/how-to-add-and-customize-back-button-of-action-bar-in-android/
     * https://stackoverflow.com/questions/6121797/android-how-to-change-layout-on-button-click
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), MenuActivity.class);
        startActivity(myIntent);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // when the activity is resumed, we acquire a wake-lock so that the screen stays on, since the user will likely not be fiddling with the screen or buttons.
        wakeLock.acquire();

        // Start the simulation
        gameView.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When the activity is paused, we make sure to stop the simulation, release our sensor resources and wake locks
        // Stop the simulation
        gameView.stopSimulation();

        // and release our wake-lock
        wakeLock.release();
    }

    /**
     * class GameView
     */
    class GameView extends FrameLayout implements SensorEventListener {
        /**
         * diameter of the balls in meters
         */
        private static final float sBallDiameter = 0.004f;
        private static final float sBallDiameter2 = sBallDiameter * sBallDiameter;

        /**
         * width and height for the ball
         */
        private final int ballWidth;
        private final int ballHeight;

        /**
         * diameter of the true and false friends in meters
         */
        private static final float friendDiameter = 0.008f;

        /**
         * width and height for the true and false friends
         */
        private final int friendWidth;
        private final int friendHeight;

        /**
         * Create an instance of Sensor
         */
        private Sensor accelerometer;

        /**
         * Last time the ballposition got updated
         */
        private long mLastT;

        /**
         * Dots per inch in x and y-direction of the display
         */
        private float dpiX;
        private float dpiY;

        /**
         * Values to to calculate meters to pixels
         */
        private float metersToPixelsX;
        private float metersToPixelsY;

        /**
         *
         */
        private float mXOrigin;
        private float mYOrigin;

        /**
         * Variables to store the values from the accelerometer Sensor
         */
        private float sensorX;
        private float sensorY;

        /**
         *x and y-values of the boarder of the screen
         */
        private float horizontalBound;
        private float verticalBound;

        /**
         * Create an instance of GameSystem
         */
        private final GameSystem gameSystem;

        /**
         * Create an instance of Random
         */
        Random random = new Random();

        /**
         * Width of the display in pixel
         */
        int width = displaymetrics.widthPixels;

        /**
         * Height of the display in pixel
         */
        int height = displaymetrics.heightPixels;

        /**
         * Class Friend
         */
        class Friend extends View {
            /**
             * width and height of the display divided with friendWidth to have a value to to use
             * for random, so the friends get values in steps, so they doesn't overlap with each other
             */
            int randomx = width/friendWidth;
            int randomy = height/friendWidth;

            /**
             * Randomized x and y-position for the friends
             * Note: Width and Height are bigger than the actual display so sometimes the friends
             * get drawn outside of the screen. Tried to use the bound values that are used to
             * check is the ball hits the boarder of the screen, but it didn't work
             */
            final private float posX = (( - width + 100)  / 2.0f + (float) random.nextInt(randomx-1) * width  / randomx) / metersToPixelsX;
            final private float posY = (( - height + 300) / 2.0f + (float) random.nextInt(randomy-1) * height / randomy) / metersToPixelsY;

            /**
             * Constructor for Friend
             *
             * @param context: the context
             */
            public Friend(Context context) {super(context);}

            /**
             * Constructor for Friend with attributes
             *
             * @param context: the context
             * @param attrs:   the attrs
             */
            public Friend(Context context, AttributeSet attrs) {super(context, attrs);}

        }


        /**
         * Class Ball
         */
        class Ball extends View {
            /**
             * Randomized x and y-position for the Ball
             */
            private float posX = (float) Math.random();
            private float posY = (float) Math.random();

            /**
             * Variables for the velocity of the ball
             */
            private float velX;
            private float velY;

            /**
             * Variables for the x and y-position of a false friend the ball hit, so it
             * sticks to is until it gets released
             */
            private float stickX;
            private float stickY;

            /**
             * Boolean, that gets set, when the ball collides with one of the false friends
             * default is false
             */
            private Boolean collision = false;

            /**
             * Constructor for Ball
             *
             * @param context: the context
             */
            public Ball(Context context) {
                super(context);
            }

            /**
             * Constructor for Ball with attributes
             *
             * @param context: the context
             * @param attrs:   the attrs
             */
            public Ball(Context context, AttributeSet attrs) {
                super(context, attrs);
            }

            /**
             * Constructor for Ball
             *
             * @param context:      the context
             * @param attrs:        the attrs
             * @param defStyleAttr: the def style attr
             */
            public Ball(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }

            /**
             * Constructor for Ball
             *
             * @param context:      the context
             * @param attrs:        the attrs
             * @param defStyleAttr: the def style attr
             * @param defStyleRes:  the def style res
             */
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public Ball(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
                super(context, attrs, defStyleAttr, defStyleRes);
            }

            /**
             * computePhysics calculates the new x and y-position of the ball to move it and the
             * new velocity of it
             *
             * @param sx: data from the accelerometer in x-direction
             * @param sy: data from the accelerometer in y-direction
             * @param dT: time
             */
            public void computePhysics(float sx, float sy, float dT) {

                final float ax = -sx/5;
                final float ay = -sy/5;

                posX += velX * dT + ax * dT * dT / 2;
                posY += velY * dT + ay * dT * dT / 2;

                velX += ax * dT;
                velY += ay * dT;
            }

            /**
             * resolveCollisionWithBounds checks is the ball hits the boarder of the screen.
             * If it does so, the value of the x and/or y-position gets set to the value of the bound,
             * so it doesn't roll out of the screen.
             * The velocity is set to 0 for the direction, where the ball hits the screen boarder.
             */
            public void resolveCollisionWithBounds() {
                final float xmax = horizontalBound;
                final float ymax = verticalBound;
                final float x = posX;
                final float y = posY;
                if (x > xmax) {
                    posX = xmax;
                    velX = 0;
                } else if (x < -xmax) {
                    posX = -xmax;
                    velX = 0;
                }
                if (y > ymax) {
                    posY = ymax;
                    velY = 0;
                } else if (y < -ymax) {
                    posY = -ymax;
                    velY = 0;
                }

            }

            /**
             * resolveCollisionWithTrueFriend checks if the ball hits the true friend.
             * If it does, the game ends and an new screen with random placed friends gets created
             *
             * @param tposX: x-position of the true friend
             * @param tposY: y-position of the true friend
             */
            public void resolveCollisionWithTrueFriend(float tposX, float tposY){
                final float x = posX;
                final float y = posY;
                if ( x * metersToPixelsX >= tposX * metersToPixelsX - friendWidth &&
                     x * metersToPixelsX <= tposX * metersToPixelsX + friendWidth &&
                     y * metersToPixelsY >= tposY * metersToPixelsX - friendWidth &&
                     y * metersToPixelsY <= tposY * metersToPixelsX + friendWidth ) {
                    velX = 0;
                    velY = 0;
                    posX = tposX;
                    posY = tposY;
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }

            /**
             * resolveCollisionWithFalseFriend checks if the ball hit one of the false friends
             * If it does, the x and y-position of the ball are set to the position of the false friend
             * it collided with.
             * In stickX and stickY the position of this false friend gets saved.
             * Collision is set to true.
             *
             * If collision is true, the ball can be released, when the absolute values of the
             * accelerometer are bigger than two.
             * Collision is set to false again.
             *
             *
             * @param fposX:    x-positions of all falsefriends
             * @param fposY:    y-positions of all falsefriends
             * @param sx:       value of the accelerometer in x-direction
             * @param sy:       value of the accelerometer in y-direction
             */
            public void resolveCollisionWithFalseFriend(float[] fposX, float[] fposY, float sx, float sy) {
                final float x = posX;
                final float y = posY;
                if ( !collision ) {
                    for (int i = 0; i < fposX.length; i++) {
                        if (x * metersToPixelsX >= fposX[i] * metersToPixelsX - friendWidth &&
                            x * metersToPixelsX <= fposX[i] * metersToPixelsX + friendWidth &&
                            y * metersToPixelsY >= fposY[i] * metersToPixelsX - friendWidth &&
                            y * metersToPixelsY <= fposY[i] * metersToPixelsX + friendWidth  ) {
                            velX = 0;
                            velY = 0;
                            posX = fposX[i];
                            posY = fposY[i];
                            stickX = fposX[i];
                            stickY = fposY[i];
                            collision = true;
                            break;
                        }
                    }
                }
                else if ( collision ) {
                    if( Math.abs(sensorX) > 2.0f && Math.abs(sensorY) > 2.0f)
                    {
                        Log.d("sensorx", Float.toString(sensorX));
                        Log.d("sensory", Float.toString(sensorY));
                        velX = 0.000002f;
                        velY = 0.000002f;

                        collision = false;

                    }
                    else {
                        posX = stickX;
                        posY = stickY;
                    }

                }
            }
        }

        /**
         * Class GameSystem
         */
        class GameSystem {
            /**
             * Number, how many false friends should be there
             */
            int FalseFriendsNumber = 8;
            /**
             * Create one object of the Ball class
             */
            private final GameView.Ball ball = new Ball(getContext());
            /**
             * Create an array of Friends as false friends
             */
            private final Friend[] falsefriends = new Friend[FalseFriendsNumber];
            /**
             * Create one object of the Friend class as true friend
             */
            private final Friend truefriend = new Friend(getContext());

            /**
             * Two Arrays to store the X and Y-position of the false friends, that get initialized
             * later in this class
             */
            private float[] falsePosX = new float[FalseFriendsNumber];
            private float[] falsePosY = new float[FalseFriendsNumber];

            /**
             * Constructor for GameSystem
             */
            GameSystem() {

                /**
                 * Add false friends to view                 *
                 */
                for (int i = 0; i < falsefriends.length; i++) {
                    falsefriends[i] = new Friend(getContext());
                    falsefriends[i].setBackgroundResource(R.drawable.falsefriend);
                    falsefriends[i].setLayerType(LAYER_TYPE_HARDWARE, null);
                    falsePosX[i] = falsefriends[i].posX;
                    falsePosY[i] = falsefriends[i].posY;
                    addView(falsefriends[i], new ViewGroup.LayoutParams(friendWidth, friendHeight));
                }

                /**
                 * Add true friend to view
                 */
                truefriend.setBackgroundResource(R.drawable.truefriend);
                truefriend.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(truefriend, new ViewGroup.LayoutParams(friendWidth, friendHeight));

                /**
                 * Add ball to the view
                 */
                ball.setBackgroundResource(R.drawable.ball);
                ball.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(ball, new ViewGroup.LayoutParams(ballWidth, ballHeight));

            }

            /**
             *
             * @param sx:           data from the accelerometer in x-direction
             * @param sy:           data from the accelerometer in y-direction
             * @param timestamp:    time from now
             */
            private void updatePosition(float sx, float sy, long timestamp) {
                final long t = timestamp;
                if (mLastT != 0) {
                    final float dT = (float) (t - mLastT) / 1000.f /** (1.0f / 1000000000.0f)*/;

                    GameView.Ball ball = this.ball;
                    ball.computePhysics(sx, sy, dT);

                }
                mLastT = t;
            }

            /**
             * Update updates the position of the ball and calls the other functions to check if
             * the ball collided with something
             *
             * @param sx:   data from the accelerometer in x-directoin
             * @param sy:   data from the accelerometer in y-direction
             * @param now:  timestamp form now
             */
             public void update(float sx, float sy, long now) {
                updatePosition(sx, sy, now);

                // We do no more than a limited number of iterations
                final int NUM_MAX_ITERATIONS = 8;

                for (int k = 0; k < NUM_MAX_ITERATIONS; k++) {

                        this.ball.resolveCollisionWithBounds();
                        this.ball.resolveCollisionWithTrueFriend(truefriend.posX, truefriend.posY);
                        this.ball.resolveCollisionWithFalseFriend(falsePosX, falsePosY, sx, sy);
                }
            }

            /**
             * get X-position of false friend
             *
             * @param i: position from the array
             * @return x-position of false friend
             */
            public float getFalseFriendPosX(int i){
                return falsefriends[i].posX;
            }

            /**
             * Get y-position of false friend
             *
             * @param i: position from the array
             * @return y-position of false friend
             */
            public float getFalseFriendPosY(int i){
                return falsefriends[i].posY;
            }


        }

        /**
         * Start simulation
         */
        public void startSimulation() {
            /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
             */

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        /**
         * Stop simulation
         */
        public void stopSimulation() {
            sensorManager.unregisterListener(this);
        }

        /**
         * Constructor for GameView
         * It calculates some vales depending on the screensize.
         * The Ball and the Friends get the width an height.
         * A new GameSystem object gets created.
         *
         * @param context: the context
         */
        public GameView(Context context) {
            super(context);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            dpiX = metrics.xdpi;
            dpiY = metrics.ydpi;
            metersToPixelsX = dpiX / 0.0254f;
            metersToPixelsY = dpiY / 0.0254f;

            // rescale the ball so it's about 0.5 cm on screen
            ballWidth = (int) (sBallDiameter * metersToPixelsX + 0.5f);
            ballHeight = (int) (sBallDiameter * metersToPixelsY + 0.5f);

            friendWidth = (int) (friendDiameter * metersToPixelsX + 0.5f);
            friendHeight = (int) (friendDiameter * metersToPixelsY + 0.5f);

            gameSystem = new GameSystem();

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            // compute the origin of the screen relative to the origin of
            // the bitmap
            mXOrigin = (w - ballWidth) * 0.5f;
            mYOrigin = (h - ballHeight) * 0.5f;
            horizontalBound = ((w / metersToPixelsX - sBallDiameter) * 0.5f);
            verticalBound = ((h / metersToPixelsY - sBallDiameter) * 0.5f);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            /*
             * record the accelerometer data, the event's timestamp as well as
             * the current time. The latter is needed so we can calculate the
             * "present" time during rendering. In this application, we need to
             * take into account how the screen is rotated with respect to the
             * sensors (which always return data in a coordinate space aligned
             * to with the screen in its native orientation).
             */

            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    sensorX = event.values[0];
                    sensorY = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    sensorX = -event.values[1];
                    sensorY = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    sensorX = -event.values[0];
                    sensorY = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    sensorX = event.values[1];
                    sensorY = -event.values[0];
                    break;
            }
        }

        /**
         * Draws Ball and Friends on the canvas of the view.
         * Invalidate makes the Ball with its continuously new positions seem to roll.
         * @param canvas
         */
        @Override
        protected void onDraw(Canvas canvas) {
            /*
             * Compute the new position of our object, based on accelerometer
             * data and present time.
             */
            final GameSystem gameSystem = this.gameSystem;
            final long now = System.currentTimeMillis();
            final float sx = sensorX;
            final float sy = sensorY;

            gameSystem.update(sx, sy, now);

            final float xc = mXOrigin;
            final float yc = mYOrigin;
            final float xs = metersToPixelsX;
            final float ys = metersToPixelsY;

            for (int i = 0; i < gameSystem.falsefriends.length; i++) {

                float x = mXOrigin + gameSystem.getFalseFriendPosX(i) * metersToPixelsX;
                float y = mYOrigin - gameSystem.getFalseFriendPosY(i) * metersToPixelsY;

                gameSystem.falsefriends[i].setTranslationX(x);
                gameSystem.falsefriends[i].setTranslationY(y);
            }

            final float tx = xc + gameSystem.truefriend.posX * xs;
            final float ty = yc - gameSystem.truefriend.posY * ys;
            gameSystem.truefriend.setTranslationX(tx);
            gameSystem.truefriend.setTranslationY(ty);

            final float bx = xc + gameSystem.ball.posX * xs;
            final float by = yc - gameSystem.ball.posY * ys;
            gameSystem.ball.setTranslationX(bx);
            gameSystem.ball.setTranslationY(by);

            invalidate();

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}