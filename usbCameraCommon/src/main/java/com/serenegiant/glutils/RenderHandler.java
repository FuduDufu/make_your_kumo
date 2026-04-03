package com.serenegiant.glutils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

public class RenderHandler extends Handler {
    private static final String TAG = "RenderHandler";

    private static final int MSG_SET_EGL_CONTEXT = 1;
    private static final int MSG_DRAW = 2;
    private static final int MSG_RELEASE = 3;

    private EGLBase mEgl;
    private EGLBase.IEglSurface mEglSurface;
    private GLDrawer2D mDrawer;
    private int mTexId;
    private final float[] mTexMatrix = new float[16];
    private boolean mIsActive = false;

    public static RenderHandler createHandler(final String name) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();
        return new RenderHandler(thread);
    }

    private RenderHandler(final HandlerThread thread) {
        super(thread.getLooper());
    }

    public void setEglContext(final EGLBase.IContext sharedContext,
                              final int texId, final Surface surface, final boolean isRecordable) {
        final Object[] params = new Object[]{ sharedContext, surface };
        sendMessage(obtainMessage(MSG_SET_EGL_CONTEXT, texId, isRecordable ? 1 : 0, params));
    }

    public void draw(final float[] texMatrix) {
        if (mIsActive) {
            sendMessage(obtainMessage(MSG_DRAW, texMatrix));
        }
    }

    public void release() {
        sendMessage(obtainMessage(MSG_RELEASE));
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_SET_EGL_CONTEXT: {
                final Object[] params = (Object[]) msg.obj;
                final EGLBase.IContext sharedContext = (EGLBase.IContext) params[0];
                final Surface surface = (Surface) params[1];
                final int texId = msg.arg1;
                final boolean isRecordable = msg.arg2 != 0;
                try {
                    mEgl = new EGLBase(sharedContext, false, isRecordable);
                    mEglSurface = mEgl.createFromSurface(surface);
                    mEglSurface.makeCurrent();
                    mDrawer = new GLDrawer2D();
                    mTexId = texId;
                    mIsActive = true;
                } catch (final Exception e) {
                    Log.e(TAG, "setEglContext:", e);
                }
                break;
            }
            case MSG_DRAW: {
                if (mIsActive && mEglSurface != null) {
                    final float[] texMatrix = (float[]) msg.obj;
                    if (texMatrix != null) {
                        System.arraycopy(texMatrix, 0, mTexMatrix, 0, texMatrix.length);
                    }
                    mEglSurface.makeCurrent();
                    mDrawer.draw(mTexId, mTexMatrix);
                    mEglSurface.swap();
                }
                break;
            }
            case MSG_RELEASE: {
                mIsActive = false;
                if (mDrawer != null) {
                    mDrawer.release();
                    mDrawer = null;
                }
                if (mEglSurface != null) {
                    mEglSurface.release();
                    mEglSurface = null;
                }
                if (mEgl != null) {
                    mEgl.release();
                    mEgl = null;
                }
                getLooper().quit();
                break;
            }
        }
    }
}