/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import com.example.android.sunshine.R;
import com.example.android.sunshine.utilities.WearableUtils;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;



/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

    private static final String TAG = SunshineWatchFace.class.getSimpleName();

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;


    private static final String DATE_FORMAT = "%02d.%02d.%d";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mRegisteredWeatherUpdateReceiver = false;
        Paint mBackgroundPaint;
        Paint mTimePaint;
        Paint mDatePaint;
        Paint mWeatherPaint;
        boolean mAmbient;
        Calendar mCalendar;
        long mHigh;
        long mLow;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        final BroadcastReceiver mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    mHigh = intent.getLongExtra(WearableUtils.EXTRA_HIGH_TEMPERATURE, 0);
                    mLow = intent.getLongExtra(WearableUtils.EXTRA_LOW_TEMPERATURE, 0);
                    invalidate();
                }
            }
        };


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            //TODO: We probably need to check for the initial and updated weather data

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTimePaint = new Paint();
            mTimePaint = createPaint(resources.getColor(R.color.digital_text));
            mTimePaint.setTextSize(resources.getDimension(R.dimen.time_text_size));

            mDatePaint = new Paint();
            mDatePaint = createPaint(resources.getColor(R.color.digital_text));
            mDatePaint.setTextSize(resources.getDimension(R.dimen.date_text_size));

            mWeatherPaint = new Paint();
            mWeatherPaint = createPaint(resources.getColor(R.color.digital_text));
            mWeatherPaint.setTextSize(resources.getDimension(R.dimen.weather_text_size));

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceivers();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceivers();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private float getXOffset(String text, Paint paint, Rect watchBounds) {
            float centerX = watchBounds.exactCenterX();
            float timeLength = paint.measureText(text);
            return centerX - (timeLength / 2.0f);
        }

        private float getTimeYOffset(String timeText, Paint timePaint, Rect watchBounds) {
            float centerY = watchBounds.exactCenterY();
            Rect textBounds = new Rect();
            timePaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
            int textHeight = textBounds.height();
            return centerY/2 + (textHeight / 2.0f);
        }

        private float getDateYOffset(String dateText, Paint datePaint) {
            Rect textBounds = new Rect();
            datePaint.getTextBounds(dateText, 0, dateText.length(), textBounds);
            return textBounds.height() + 15.0f;
        }

        private float getWeatherYOffset(String weatherText, Paint weatherPaint) {
            Rect textBounds = new Rect();
            weatherPaint.getTextBounds(weatherText, 0, weatherText.length(), textBounds);
            return textBounds.height() + 45.0f;
        }

        private void registerReceivers() {
            if (!mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
            }

            if (!mRegisteredWeatherUpdateReceiver) {
                mRegisteredWeatherUpdateReceiver = true;
                IntentFilter filter = new IntentFilter(WearableUtils.ACTION_WEATHER_UPDATED);
                LocalBroadcastManager.getInstance(SunshineWatchFace.this).registerReceiver(mWeatherUpdateReceiver, filter);
            }
        }

        private void unregisterReceivers() {
            if (mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = false;
                SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            }

            if (mRegisteredWeatherUpdateReceiver) {
                mRegisteredWeatherUpdateReceiver = false;
                LocalBroadcastManager.getInstance(SunshineWatchFace.this).unregisterReceiver(mWeatherUpdateReceiver);
            }
        }

       /* @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
           // mXOffset = resources.getDimension(isRound  ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTimePaint.setTextSize(textSize);
        }*/

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                Resources resources = SunshineWatchFace.this.getResources();

                if (mAmbient) {
                    mTimePaint.setColor(resources.getColor(R.color.digital_text_ambient));
                    mDatePaint.setColor(resources.getColor(R.color.digital_text_ambient));
                    mWeatherPaint.setColor(resources.getColor(R.color.digital_text_ambient));
                } else {
                    mTimePaint.setColor(resources.getColor(R.color.digital_text));
                    mDatePaint.setColor(resources.getColor(R.color.digital_text_ambient));
                    mWeatherPaint.setColor(resources.getColor(R.color.digital_text_ambient));

                }

                if (mLowBitAmbient) {
                    mTimePaint.setAntiAlias(!inAmbientMode);
                    mDatePaint.setAntiAlias(!inAmbientMode);
                    mWeatherPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            Resources resources = SunshineWatchFace.this.getResources();

            if (isInAmbientMode()) {
                canvas.drawColor(resources.getColor(R.color.background_ambient));
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String text = mAmbient
                    ? String.format(Locale.getDefault(), "%02d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE))
                    : String.format(Locale.getDefault(), "%02d:%02d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE), mCalendar.get(Calendar.SECOND));
            float timeXOffset = getXOffset(text, mTimePaint, bounds);
            float timeYOffset = getTimeYOffset(text, mTimePaint, bounds);
            canvas.drawText(text, timeXOffset, timeYOffset, mTimePaint);

            String dateText = String.format(DATE_FORMAT, mCalendar.get(Calendar.DAY_OF_MONTH), (mCalendar.get(Calendar.MONTH) + 1), mCalendar.get(Calendar.YEAR));
            float dateXOffset = getXOffset(dateText, mDatePaint, bounds);
            float dateYOffset = getDateYOffset(dateText, mDatePaint);
            canvas.drawText(dateText, dateXOffset, timeYOffset + dateYOffset, mDatePaint);

            // TODO: set real weather value
            String highLow = resources.getString(R.string.temperature_text, mHigh, mLow);
            float weatherXOffset = getXOffset(highLow, mWeatherPaint, bounds);
            float weatherYDateOffset = getWeatherYOffset(highLow, mWeatherPaint);
            canvas.drawText(highLow, weatherXOffset, timeYOffset + dateYOffset + weatherYDateOffset, mWeatherPaint);

        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

    }

}
