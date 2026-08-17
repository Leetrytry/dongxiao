package com.dongxiao.practice.song;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class JianpuTextSource {
    public final String title;
    public final String key;
    public final int tempoBpm;
    public final int meterBeats;
    public final List<String> assetPaths;

    public JianpuTextSource(String title, String key, int tempoBpm, int meterBeats, String... assetPaths) {
        this.title = title;
        this.key = key;
        this.tempoBpm = tempoBpm;
        this.meterBeats = meterBeats;
        this.assetPaths = Collections.unmodifiableList(Arrays.asList(assetPaths));
    }
}
