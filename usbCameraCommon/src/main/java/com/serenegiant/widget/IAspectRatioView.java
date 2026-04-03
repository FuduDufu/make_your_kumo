package com.serenegiant.widget;

public interface IAspectRatioView {
    void setAspectRatio(double aspectRatio);
    void setAspectRatio(int width, int height);
    double getAspectRatio();
}