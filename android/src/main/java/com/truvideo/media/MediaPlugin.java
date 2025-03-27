package com.truvideo.media;

import android.util.Log;

public class MediaPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
