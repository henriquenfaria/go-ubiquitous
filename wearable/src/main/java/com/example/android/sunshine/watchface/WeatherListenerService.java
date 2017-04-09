package com.example.android.sunshine.watchface;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.android.sunshine.utilities.WearableUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;


// Service to receive weather data from Sunshine app
public class WeatherListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static long TIMEOUT_SECONDS = 30;
    private final static String TAG = WeatherListenerService.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Created");

        if (null == mGoogleApiClient) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            Log.v(TAG, "GoogleApiClient created");
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            Log.v(TAG, "Connecting to GoogleApiClient..");
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroyed");

        if (null != mGoogleApiClient) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
                Log.v(TAG, "GoogleApiClient disconnected");
            }
        }

        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                if (WearableUtils.PATH_WEATHER_DATA.equalsIgnoreCase(dataEvent.getDataItem()
                        .getUri().getPath())) {

                    Asset iconAsset = dataMap.getAsset(WearableUtils.DATA_WEATHER_ICON);
                    Bitmap bitmap = loadBitmapFromAsset(iconAsset);
                    long high = dataMap.getLong(WearableUtils.DATA_WEATHER_HIGH_TEMPERATURE);
                    long low = dataMap.getLong(WearableUtils.DATA_WEATHER_LOW_TEMPERATURE);

                    WearableUtils.saveWeatherData(WeatherListenerService.this, bitmap, high, low);

                    sendWeatherUpdateBroadcast();
                }
            }
        }
        dataEvents.release();
    }

    private void sendWeatherUpdateBroadcast() {
        Intent intent = new Intent();
        intent.setAction(WearableUtils.ACTION_WEATHER_UPDATED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // https://developer.android.com/training/wearables/data-layer/assets.html#ReceiveAsset
    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!result.isSuccess()) {
            return null;
        }

        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
