package com.serenegiant.glutils;

import android.opengl.GLES20;

public class GLHelper {

    public static int initTex() {
        final int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        return tex[0];
    }

    public static void deleteTex(final int texId) {
        final int[] tex = { texId };
        GLES20.glDeleteTextures(1, tex, 0);
    }
}