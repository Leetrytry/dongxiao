package com.dongxiao.practice.practice;

import java.util.Locale;

public final class PracticeScore {
    public final int score;
    public final double voicedSeconds;
    public final double meanAbsCents;
    public final double stabilityCents;
    public final double hitRate;
    public final String comment;
    public final String modeDetail;

    public PracticeScore(
            int score,
            double voicedSeconds,
            double meanAbsCents,
            double stabilityCents,
            double hitRate,
            String comment,
            String modeDetail
    ) {
        this.score = score;
        this.voicedSeconds = voicedSeconds;
        this.meanAbsCents = meanAbsCents;
        this.stabilityCents = stabilityCents;
        this.hitRate = hitRate;
        this.comment = comment;
        this.modeDetail = modeDetail;
    }

    public String format() {
        return String.format(
                Locale.CHINA,
                "本次评分：%d 分\n%s\n有效发声：%.1f 秒 · 平均偏差：%.0f cent · 稳定度：%.0f cent · 命中率：%.0f%%\n%s",
                score,
                comment,
                voicedSeconds,
                meanAbsCents,
                stabilityCents,
                hitRate,
                modeDetail
        );
    }
}
