package com.dongxiao.practice.practice;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PracticeScore {
    public final int score;
    public final double voicedSeconds;
    public final double meanAbsCents;
    public final double stabilityCents;
    public final double hitRate;
    public final String comment;
    public final String modeDetail;
    public final List<PracticeNoteScore> noteScores;

    public PracticeScore(
            int score,
            double voicedSeconds,
            double meanAbsCents,
            double stabilityCents,
            double hitRate,
            String comment,
            String modeDetail
    ) {
        this(score, voicedSeconds, meanAbsCents, stabilityCents, hitRate, comment, modeDetail, Collections.emptyList());
    }

    public PracticeScore(
            int score,
            double voicedSeconds,
            double meanAbsCents,
            double stabilityCents,
            double hitRate,
            String comment,
            String modeDetail,
            List<PracticeNoteScore> noteScores
    ) {
        this.score = score;
        this.voicedSeconds = voicedSeconds;
        this.meanAbsCents = meanAbsCents;
        this.stabilityCents = stabilityCents;
        this.hitRate = hitRate;
        this.comment = comment;
        this.modeDetail = modeDetail;
        this.noteScores = noteScores == null ? Collections.emptyList() : Collections.unmodifiableList(noteScores);
    }

    public String format() {
        return String.format(
                Locale.CHINA,
                "本次评分：%d 分\n%s\n有效发声：%.1f 秒 · 平均偏差：%.0f cent · 稳定度：%.0f%% · 命中率：%.0f%%\n%s",
                score,
                comment,
                voicedSeconds,
                meanAbsCents,
                PracticeStats.stabilityPercent(stabilityCents),
                hitRate,
                modeDetail
        );
    }
}
