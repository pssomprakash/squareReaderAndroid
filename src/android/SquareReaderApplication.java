package com.om.squareReaderAndroid;

import android.util.Log;
import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.squareup.sdk.reader.ReaderSdk;

public class SquareReaderApplication extends Application {
  private static final String LOG_TAG = "square-reader-plugin-android";

  @Override public void onCreate() {
    super.onCreate();
    ReaderSdk.initialize(this);
  }

  @Override protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
  }

  private static void log(String message) {
    Log.d(LOG_TAG, message);
  }
}