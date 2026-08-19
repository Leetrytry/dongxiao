package com.dongxiao.practice.practice;

import com.dongxiao.practice.music.TargetNote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScalePracticeProgress {
    public final List<TargetNote> sequence;
    public final List<Integer> sectionStarts;
    public final List<String> sectionLabels;
    public final TargetNote currentTarget;
    public final TargetNote nextTarget;
    public final int currentIndex;
    public final int totalNotes;
    public final int completedNotes;
    public final int wrongAttempts;
    public final boolean completed;
    public final boolean justAdvanced;
    public final double lastCents;
    public final double currentHitSeconds;
    public final TargetNote lastWrongTarget;

    ScalePracticeProgress(
            List<TargetNote> sequence,
            List<Integer> sectionStarts,
            List<String> sectionLabels,
            TargetNote currentTarget,
            TargetNote nextTarget,
            int currentIndex,
            int totalNotes,
            int completedNotes,
            int wrongAttempts,
            boolean completed,
            boolean justAdvanced,
            double lastCents,
            double currentHitSeconds,
            TargetNote lastWrongTarget
    ) {
        this.sequence = sequence == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sequence));
        this.sectionStarts = sectionStarts == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sectionStarts));
        this.sectionLabels = sectionLabels == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sectionLabels));
        this.currentTarget = currentTarget;
        this.nextTarget = nextTarget;
        this.currentIndex = currentIndex;
        this.totalNotes = totalNotes;
        this.completedNotes = completedNotes;
        this.wrongAttempts = wrongAttempts;
        this.completed = completed;
        this.justAdvanced = justAdvanced;
        this.lastCents = lastCents;
        this.currentHitSeconds = currentHitSeconds;
        this.lastWrongTarget = lastWrongTarget;
    }
}
