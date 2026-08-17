package com.dongxiao.practice.song;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ImageScore {
    public final String title;
    public final List<String> assetPaths;

    public ImageScore(String title, String... assetPaths) {
        this.title = title;
        this.assetPaths = Collections.unmodifiableList(Arrays.asList(assetPaths));
    }

    public int pageCount() {
        return assetPaths.size();
    }

    public String assetPath(int pageIndex) {
        if (assetPaths.isEmpty()) {
            return "";
        }
        int safeIndex = Math.max(0, Math.min(pageIndex, assetPaths.size() - 1));
        return assetPaths.get(safeIndex);
    }

    @Override
    public String toString() {
        return title;
    }
}
