package com.example.phonegapcordovacamera;

import org.apache.cordova.DroidGap;

import android.os.Bundle;

public class MainActivity extends DroidGap
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.setIntegerProperty("splashscreen", R.drawable.ic_launcher);
        super.loadUrl("file:///android_asset/www/index.html");
    }
}