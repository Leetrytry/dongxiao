package com.dongxiao.practice.song;

public final class ImageScoreMarker {
    public final int pageIndex;
    public final float left;
    public final float top;
    public final float width;
    public final float height;

    public ImageScoreMarker(int pageIndex, float left, float top, float width, float height) {
        this.pageIndex = pageIndex;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }
}
