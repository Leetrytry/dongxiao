package com.dongxiao.practice.practice;

import com.dongxiao.practice.audio.PitchResult;
import com.dongxiao.practice.music.TargetNote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScalePracticeEngine {
    private static final int ASCENDING_NOTE_COUNT = 8;
    private static final double HIT_TOLERANCE_CENTS = 25.0;
    private static final double WRONG_NOTE_TOLERANCE_CENTS = 45.0;
    private static final long REQUIRED_HIT_MS = 360L;
    private static final long WRONG_NOTE_REFRACTORY_MS = 700L;

    private final List<TargetNote> sequence = new ArrayList<>();
    private int currentIndex = 0;
    private int completedNotes = 0;
    private int wrongAttempts = 0;
    private long hitStartMs = -1L;
    private long lastWrongMs = -1L;
    private long currentHitMs = 0L;
    private double lastCents = Double.NaN;
    private boolean completed = false;
    private boolean justAdvanced = false;
    private TargetNote lastWrongTarget = null;

    public void reset(List<TargetNote> targets, TargetNote startTarget) {
        sequence.clear();
        currentIndex = 0;
        completedNotes = 0;
        wrongAttempts = 0;
        hitStartMs = -1L;
        lastWrongMs = -1L;
        currentHitMs = 0L;
        lastCents = Double.NaN;
        completed = false;
        justAdvanced = false;
        lastWrongTarget = null;

        if (targets == null || targets.isEmpty()) {
            return;
        }
        int startIndex = indexOfTarget(targets, startTarget);
        int availableAscending = Math.min(ASCENDING_NOTE_COUNT, targets.size());
        if (startIndex + availableAscending > targets.size()) {
            startIndex = Math.max(0, targets.size() - availableAscending);
        }
        int endIndex = Math.min(targets.size() - 1, startIndex + availableAscending - 1);
        for (int i = startIndex; i <= endIndex; i++) {
            sequence.add(targets.get(i));
        }
        for (int i = endIndex - 1; i >= startIndex; i--) {
            sequence.add(targets.get(i));
        }
    }

    public boolean hasSequence() {
        return !sequence.isEmpty();
    }

    public TargetNote currentTarget() {
        if (sequence.isEmpty()) {
            return null;
        }
        return sequence.get(Math.min(currentIndex, sequence.size() - 1));
    }

    public ScalePracticeProgress snapshot() {
        return new ScalePracticeProgress(
                sequence,
                currentTarget(),
                nextTarget(),
                currentIndex,
                sequence.size(),
                completedNotes,
                wrongAttempts,
                completed,
                justAdvanced,
                lastCents,
                currentHitMs / 1000.0,
                lastWrongTarget
        );
    }

    public ScalePracticeProgress update(PitchResult pitch, long nowMs, List<TargetNote> candidates) {
        justAdvanced = false;
        if (sequence.isEmpty() || completed) {
            return snapshot();
        }

        TargetNote expected = currentTarget();
        if (pitch == null || !pitch.voiced || expected == null) {
            hitStartMs = -1L;
            currentHitMs = 0L;
            lastCents = Double.NaN;
            return snapshot();
        }

        double cents = expected.centsFrom(pitch.frequencyHz);
        lastCents = cents;
        double absCents = Math.abs(cents);
        if (absCents <= HIT_TOLERANCE_CENTS) {
            if (hitStartMs < 0L) {
                hitStartMs = nowMs;
            }
            currentHitMs = Math.max(0L, nowMs - hitStartMs);
            if (nowMs - hitStartMs >= REQUIRED_HIT_MS) {
                advance();
            }
        } else {
            hitStartMs = -1L;
            currentHitMs = 0L;
            updateWrongAttempt(pitch.frequencyHz, expected, candidates, nowMs);
        }
        return snapshot();
    }

    private void advance() {
        completedNotes = Math.min(sequence.size(), completedNotes + 1);
        hitStartMs = -1L;
        currentHitMs = 0L;
        lastWrongTarget = null;
        justAdvanced = true;
        if (currentIndex >= sequence.size() - 1) {
            completed = true;
            currentIndex = sequence.size() - 1;
        } else {
            currentIndex++;
        }
    }

    private void updateWrongAttempt(
            double frequencyHz,
            TargetNote expected,
            List<TargetNote> candidates,
            long nowMs
    ) {
        TargetNote nearest = closestTarget(frequencyHz, candidates == null ? Collections.emptyList() : candidates);
        if (nearest == null || nearest.midi == expected.midi) {
            return;
        }
        double nearestAbsCents = Math.abs(nearest.centsFrom(frequencyHz));
        if (nearestAbsCents <= WRONG_NOTE_TOLERANCE_CENTS
                && (lastWrongMs < 0L || nowMs - lastWrongMs >= WRONG_NOTE_REFRACTORY_MS)) {
            wrongAttempts++;
            lastWrongMs = nowMs;
            lastWrongTarget = nearest;
        }
    }

    private TargetNote nextTarget() {
        if (sequence.isEmpty() || currentIndex >= sequence.size() - 1) {
            return null;
        }
        return sequence.get(currentIndex + 1);
    }

    private static int indexOfTarget(List<TargetNote> targets, TargetNote target) {
        if (target != null) {
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i).midi == target.midi) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static TargetNote closestTarget(double frequencyHz, List<TargetNote> candidates) {
        TargetNote closest = null;
        double closestAbsCents = Double.MAX_VALUE;
        for (TargetNote candidate : candidates) {
            double absCents = Math.abs(candidate.centsFrom(frequencyHz));
            if (absCents < closestAbsCents) {
                closest = candidate;
                closestAbsCents = absCents;
            }
        }
        return closest;
    }
}
