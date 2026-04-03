package com.serenegiant.utils;

public class FpsCounter {
    private static final float DEFAULT_SMOOTHING = 0.005f;

    private long mStartTime;
    private int mFrameCount;
    private float mFps;
    private float mTotalFps;
    private float mSmoothing;

    public FpsCounter() {
        this(DEFAULT_SMOOTHING);
    }

    public FpsCounter(final float smoothing) {
        mSmoothing = smoothing;
        reset();
    }

    public void reset() {
        mStartTime = System.nanoTime();
        mFrameCount = 0;
        mFps = 0;
        mTotalFps = 0;
    }

    public synchronized boolean update() {
        mFrameCount++;
        final long now = System.nanoTime();
        final long elapsed = now - mStartTime;
        if (elapsed >= 1000000000L) {
            final float fps = mFrameCount * 1000000000.0f / elapsed;
            mFps = mFps <= 0 ? fps : mFps * (1.0f - mSmoothing) + fps * mSmoothing;
            mTotalFps = fps;
            mFrameCount = 0;
            mStartTime = now;
            return true;
        }
        return false;
    }

    public float getFps() {
        return mFps;
    }

    public float getTotalFps() {
        return mTotalFps;
    }
}