package com.serenegiant.utils;

import android.os.Handler;
import android.os.HandlerThread;

public class HandlerThreadHandler extends Handler {

    public static HandlerThreadHandler createHandler(final String name) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();
        return new HandlerThreadHandler(thread);
    }

    private HandlerThreadHandler(final HandlerThread thread) {
        super(thread.getLooper());
    }
}