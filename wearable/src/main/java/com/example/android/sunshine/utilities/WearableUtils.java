package com.example.android.sunshine.utilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.sunshine.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WearableUtils {

    private static final String TAG = WearableUtils.class.getSimpleName();

    // Data layer path
    public static final String PATH_WEATHER_DATA = "/weather";

    // Data layer items keys
    public static final String DATA_WEATHER_ICON = "weather_icon";
    public static final String DATA_WEATHER_HIGH_TEMPERATURE = "high_temperature";
    public static final String DATA_WEATHER_LOW_TEMPERATURE = "low_temperature";

    // Intent Actions
    public static final String ACTION_WEATHER_UPDATED = " com.example.android.sunshine.ACTION_WEATHER_UPDATED";

    // Pref
    public static final String PREF_HIGH_TEMPERATURE = "pref_high_temp";
    public static final String PREF_LOW_TEMPERATURE = "pref_low_temp";

    // Weather icon file paths
    public static final String WEATHER_ICON_DIRECTORY = "weatherIcon";
    public static final String WEATHER_ICON_NAME = "icon.png";

    // Store weather data (high, low and icon) locally to improve performance and battery usage
    public static void saveWeatherData(Context context, Bitmap icon, long max, long min) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(PREF_HIGH_TEMPERATURE, max);
        editor.putLong(PREF_LOW_TEMPERATURE, min);
        editor.apply();

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(createFile(context));
            icon.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (Exception e) {
            Log.d(TAG, "Exception while saving weather icon to internal storage");
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "IOException while saving weather icon to internal storage");
                e.printStackTrace();
            }
        }
    }

    // Get high temperature value from shared preferences
    public static long getHighTemperatureData(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long high = sp.getLong(PREF_HIGH_TEMPERATURE, 0);
        return high;
    }

    // Get low temperature value from shared preferences
    public static long getLowTemperatureData(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long low = sp.getLong(PREF_LOW_TEMPERATURE, 0);
        return low;
    }

    // Get weather icon from internal storage
    public static Bitmap getWeatherIconData(Context context) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(createFile(context));
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            return getResizedBitmap(original,
                    context.getResources().getDimension(R.dimen.weather_icon_size),
                    context.getResources().getDimension(R.dimen.weather_icon_size));
        } catch (Exception e) {
            Log.d(TAG, "Exception while reading weather icon from internal storage");
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "IOException while reading weather icon from internal storage");
                e.printStackTrace();
            }
        }
        return null;
    }

    // Resize bitmap size to reduce storage usage
    public static Bitmap getResizedBitmap(Bitmap bm, float newHeight, float newWidth) {
        if (bm == null) {
            return null;
        }

        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = newWidth / width;
        float scaleHeight = newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }

    private static File createFile(Context context) {
        File directory = context.getDir(WEATHER_ICON_DIRECTORY, Context.MODE_PRIVATE);
        File file =  new File(directory, WEATHER_ICON_NAME);
        return file;
    }
}
