package com.serenegiant.glutils;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import java.util.concurrent.CountDownLatch;

public class RendererHolder {
    private static final String TAG = "RendererHolder";

    private static final int MSG_ADD_SURFACE = 1;
    private static final int MSG_REMOVE_SURFACE = 2;
    private static final int MSG_DRAW = 3;
    private static final int MSG_RELEASE = 4;
    private static final int MSG_RESIZE = 5;
    private static final int MSG_CAPTURE = 6;

    public interface RenderHolderCallback {
        void onCreate(Surface surface);
        void onFrameAvailable();
        void onDestroy();
    }

    private final SparseArray<EGLBase.IEglSurface> mTargetSurfaces = new SparseArray<>();
    private SurfaceTexture mMasterTexture;
    private Surface mMasterSurface;
    private EGLBase mEgl;
    private GLDrawer2D mDrawer;
    private int mTexId;
    private final Handler mHandler;

    public RendererHolder(final int width, final int height, final RenderHolderCallback callback) {
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                    case MSG_ADD_SURFACE:
                        handleAddSurface(msg.arg1, (Surface) msg.obj, msg.arg2 != 0);
                        break;
                    case MSG_REMOVE_SURFACE:
                        handleRemoveSurface(msg.arg1);
                        break;
                    case MSG_DRAW:
                        handleDraw();
                        break;
                    case MSG_RELEASE:
                        handleRelease();
                        break;
                    case MSG_RESIZE:
                        handleResize(msg.arg1, msg.arg2);
                        break;
                    case MSG_CAPTURE:
                        handleCapture((String) msg.obj);
                        break;
                }
            }
        };

        // Initialize on GL thread and wait
        final CountDownLatch latch = new CountDownLatch(1);
        mHandler.post(() -> {
            mEgl = new EGLBase(null, false, false);
            mTexId = GLDrawer2D.initTex();
            mMasterTexture = new SurfaceTexture(mTexId);
            mMasterTexture.setDefaultBufferSize(width, height);
            mMasterSurface = new Surface(mMasterTexture);
            mMasterTexture.setOnFrameAvailableListener(
                    texture -> mHandler.sendEmptyMessage(MSG_DRAW));
            mDrawer = new GLDrawer2D();
            latch.countDown();
        });

        try {
            latch.await();
        } catch (final InterruptedException e) {
            Log.e(TAG, "init interrupted:", e);
        }
    }

    public Surface getSurface() {
        return mMasterSurface;
    }

    public void addSurface(final int surfaceId, final Surface surface, final boolean isRecordable) {
        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_ADD_SURFACE, surfaceId, isRecordable ? 1 : 0, surface));
    }

    public void removeSurface(final int surfaceId) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_REMOVE_SURFACE, surfaceId, 0));
    }

    public void resize(final int width, final int height) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RESIZE, width, height));
    }

    public void captureStill(final String path) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_CAPTURE, path));
    }

    public void release() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE));
    }

    private void handleAddSurface(final int surfaceId, final Surface surface,
                                  final boolean isRecordable) {
        try {
            final EGLBase.IEglSurface eglSurface = mEgl.createFromSurface(surface);
            mTargetSurfaces.put(surfaceId, eglSurface);
        } catch (final Exception e) {
            Log.e(TAG, "handleAddSurface:", e);
        }
    }

    private void handleRemoveSurface(final int surfaceId) {
        final EGLBase.IEglSurface surface = mTargetSurfaces.get(surfaceId);
        if (surface != null) {
            surface.release();
            mTargetSurfaces.remove(surfaceId);
        }
    }

    private void handleDraw() {
        if (mMasterTexture == null || mDrawer == null) return;
        mMasterTexture.updateTexImage();
        final float[] matrix = new float[16];
        mMasterTexture.getTransformMatrix(matrix);
        final int n = mTargetSurfaces.size();
        for (int i = 0; i < n; i++) {
            final EGLBase.IEglSurface surface = mTargetSurfaces.valueAt(i);
            surface.makeCurrent();
            mDrawer.draw(mTexId, matrix);
            surface.swap();
        }
    }

    private void handleResize(final int width, final int height) {
        if (mMasterTexture != null) {
            mMasterTexture.setDefaultBufferSize(width, height);
        }
    }

    private void handleCapture(final String path) {
        Log.i(TAG, "captureStill: " + path);
    }

    private void handleRelease() {
        final int n = mTargetSurfaces.size();
        for (int i = 0; i < n; i++) {
            mTargetSurfaces.valueAt(i).release();
        }
        mTargetSurfaces.clear();
        if (mDrawer != null) {
            mDrawer.release();
            mDrawer = null;
        }
        if (mTexId > 0) {
            GLDrawer2D.deleteTex(mTexId);
        }
        if (mEgl != null) {
            mEgl.release();
            mEgl = null;
        }
        if (mMasterSurface != null) {
            mMasterSurface.release();
            mMasterSurface = null;
        }
        if (mMasterTexture != null) {
            mMasterTexture.release();
            mMasterTexture = null;
        }
        mHandler.getLooper().quit();
    }
}