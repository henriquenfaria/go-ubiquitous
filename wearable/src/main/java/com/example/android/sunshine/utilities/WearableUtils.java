package com.example.android.sunshine.utilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WearableUtils {
    // Data layer path
    public static final String WEATHER_WEARABLE_PATH = "/weather";

    // Intent Extras
    public static final String EXTRA_HIGH_TEMPERATURE = "extra_high_temperature";
    public static final String EXTRA_LOW_TEMPERATURE = "extra_low_temperature";

    // Intent Actions
    public static final String ACTION_WEATHER_UPDATED = " com.example.android.sunshine.ACTION_WEATHER_UPDATED";

    // Pref
    public static final String PREF_MAX_TEMP = "max_temp";
    public static final String PREF_MIN_TEMP = "min_temp";



    public static void saveWeatherData(Context context, long max, long min) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(PREF_MAX_TEMP, max);
        editor.putLong(PREF_MIN_TEMP, min);
        editor.apply();
    }

    public static long getMaxTempData(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long max = sp.getLong(PREF_MAX_TEMP, 0);
        return max;
    }

    public static long getMinTempData(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long min = sp.getLong(PREF_MIN_TEMP, 0);
        return min;
    }
}
