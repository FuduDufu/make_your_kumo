package com.serenegiant.glutils;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

public class EGLBase {
    private static final String TAG = "EGLBase";
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    public interface IContext {
        EGLContext getEGLContext();
    }

    public interface IEglSurface {
        void makeCurrent();
        void swap();
        void release();
    }

    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEglConfig = null;

    public EGLBase(final IContext sharedContext, final boolean withDepthBuffer, final boolean isRecordable) {
        init(sharedContext, withDepthBuffer, isRecordable);
    }

    private void init(final IContext sharedContext, final boolean withDepthBuffer, final boolean isRecordable) {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }
        final int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }
        final int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, isRecordable ? 1 : 0,
                EGL14.EGL_NONE
        };
        final EGLConfig[] configs = new EGLConfig[1];
        final int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);
        mEglConfig = configs[0];
        final EGLContext context = sharedContext != null ? sharedContext.getEGLContext() : EGL14.EGL_NO_CONTEXT;
        final int[] attrib_list = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE };
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, context, attrib_list, 0);
    }

    public IContext getContext() {
        return () -> mEglContext;
    }

    public IEglSurface createFromSurface(final Surface surface) {
        final int[] surfaceAttribs = { EGL14.EGL_NONE };
        final EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, surfaceAttribs, 0);
        return createIEglSurface(eglSurface);
    }

    public IEglSurface createFromSurface(final SurfaceTexture surfaceTexture) {
        final int[] surfaceAttribs = { EGL14.EGL_NONE };
        final EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surfaceTexture, surfaceAttribs, 0);
        return createIEglSurface(eglSurface);
    }

    public IEglSurface createOffscreen(final int width, final int height) {
        final int[] surfaceAttribs = { EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE };
        final EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttribs, 0);
        return createIEglSurface(eglSurface);
    }

    private IEglSurface createIEglSurface(final EGLSurface eglSurface) {
        return new IEglSurface() {
            @Override
            public void makeCurrent() {
                if (!EGL14.eglMakeCurrent(mEglDisplay, eglSurface, eglSurface, mEglContext)) {
                    throw new RuntimeException("eglMakeCurrent failed");
                }
            }
            @Override
            public void swap() {
                EGL14.eglSwapBuffers(mEglDisplay, eglSurface);
            }
            @Override
            public void release() {
                EGL14.eglDestroySurface(mEglDisplay, eglSurface);
            }
        };
    }

    public void makeCurrent(final IEglSurface surface) {
        surface.makeCurrent();
    }

    public void release() {
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            EGL14.eglTerminate(mEglDisplay);
        }
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
    }
}