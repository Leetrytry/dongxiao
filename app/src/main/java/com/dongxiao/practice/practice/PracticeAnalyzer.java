package com.dongxiao.practice.practice;

import com.dongxiao.practice.audio.PitchResult;
import com.dongxiao.practice.music.TargetNote;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PracticeAnalyzer {
    private static final long HISTORY_MS = 3000L;
    private static final long STABILITY_WINDOW_MS = 2000L;
    private static final long SLIDE_WINDOW_MS = 1500L;
    private static final double ONSET_RMS = 0.025;
    private static final double LONG_TONE_TOLERANCE_CENTS = 25.0;

    private final Deque<Sample> history = new ArrayDeque<>();
    private long stableSinceMs = -1L;
    private long lastOnsetMs = -1L;
    private long lastRapidMoveMs = -1L;
    private double lastRms = 0.0;
    private Double lastCents = null;
    private int onsetCount = 0;
    private int rapidMoveCount = 0;

    public void reset() {
        history.clear();
        stableSinceMs = -1L;
        lastOnsetMs = -1L;
        lastRapidMoveMs = -1L;
        lastRms = 0.0;
        lastCents = null;
        onsetCount = 0;
        rapidMoveCount = 0;
    }

    public PracticeStats update(PitchResult pitch, TargetNote target, long nowMs) {
        boolean hasPitch = pitch.voiced && target != null;
        double cents = hasPitch ? target.centsFrom(pitch.frequencyHz) : 0.0;

        history.addLast(new Sample(nowMs, hasPitch, cents, pitch.rms));
        trimHistory(nowMs);

        updateLongToneHold(hasPitch, cents, nowMs);
        updateOnsetCounter(pitch.rms, nowMs);
        updateRapidMoveCounter(hasPitch, cents, nowMs);

        lastRms = pitch.rms;
        if (hasPitch) {
            lastCents = cents;
        }

        return new PracticeStats(
                hasPitch,
                cents,
                stability(nowMs),
                heldSeconds(nowMs),
                vibratoRateHz(nowMs),
                vibratoDepthCents(nowMs),
                onsetCount,
                rapidMoveCount,
                slideDeltaCents(nowMs)
        );
    }

    private void updateLongToneHold(boolean hasPitch, double cents, long nowMs) {
        if (hasPitch && Math.abs(cents) <= LONG_TONE_TOLERANCE_CENTS) {
            if (stableSinceMs < 0L) {
                stableSinceMs = nowMs;
            }
        } else {
            stableSinceMs = -1L;
        }
    }

    private void updateOnsetCounter(double rms, long nowMs) {
        boolean crossed = lastRms <= ONSET_RMS && rms > ONSET_RMS;
        boolean pastRefractory = lastOnsetMs < 0L || nowMs - lastOnsetMs > 120L;
        if (crossed && pastRefractory) {
            onsetCount++;
            lastOnsetMs = nowMs;
        }
    }

    private void updateRapidMoveCounter(boolean hasPitch, double cents, long nowMs) {
        if (!hasPitch || lastCents == null) {
            return;
        }
        boolean movedFast = Math.abs(cents - lastCents) > 45.0;
        boolean pastRefractory = lastRapidMoveMs < 0L || nowMs - lastRapidMoveMs > 150L;
        if (movedFast && pastRefractory) {
            rapidMoveCount++;
            lastRapidMoveMs = nowMs;
        }
    }

    private double heldSeconds(long nowMs) {
        if (stableSinceMs < 0L) {
            return 0.0;
        }
        return (nowMs - stableSinceMs) / 1000.0;
    }

    private double stability(long nowMs) {
        double sum = 0.0;
        double sumSquares = 0.0;
        int count = 0;
        for (Sample sample : history) {
            if (sample.hasPitch && nowMs - sample.timestampMs <= STABILITY_WINDOW_MS) {
                sum += sample.cents;
                sumSquares += sample.cents * sample.cents;
                count++;
            }
        }
        if (count < 2) {
            return 0.0;
        }
        double mean = sum / count;
        double variance = Math.max(0.0, sumSquares / count - mean * mean);
        return Math.sqrt(variance);
    }

    private double vibratoRateHz(long nowMs) {
        WindowStats stats = windowStats(nowMs, 2200L);
        if (stats.count < 6 || stats.durationSeconds <= 0.5) {
            return 0.0;
        }

        int crossings = 0;
        int previousSign = 0;
        for (Sample sample : history) {
            if (!sample.hasPitch || nowMs - sample.timestampMs > 2200L) {
                continue;
            }
            double centered = sample.cents - stats.mean;
            int sign = Math.abs(centered) < 5.0 ? 0 : (centered > 0.0 ? 1 : -1);
            if (sign != 0 && previousSign != 0 && sign != previousSign) {
                crossings++;
            }
            if (sign != 0) {
                previousSign = sign;
            }
        }
        return crossings / 2.0 / stats.durationSeconds;
    }

    private double vibratoDepthCents(long nowMs) {
        WindowStats stats = windowStats(nowMs, 2200L);
        if (stats.count < 6) {
            return 0.0;
        }
        return Math.max(0.0, (stats.max - stats.min) / 2.0);
    }

    private double slideDeltaCents(long nowMs) {
        Sample first = null;
        Sample last = null;
        for (Sample sample : history) {
            if (sample.hasPitch && nowMs - sample.timestampMs <= SLIDE_WINDOW_MS) {
                if (first == null) {
                    first = sample;
                }
                last = sample;
            }
        }
        if (first == null || last == null || first == last) {
            return 0.0;
        }
        return last.cents - first.cents;
    }

    private WindowStats windowStats(long nowMs, long windowMs) {
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int count = 0;
        long firstMs = -1L;
        long lastMs = -1L;
        for (Sample sample : history) {
            if (sample.hasPitch && nowMs - sample.timestampMs <= windowMs) {
                if (firstMs < 0L) {
                    firstMs = sample.timestampMs;
                }
                lastMs = sample.timestampMs;
                sum += sample.cents;
                min = Math.min(min, sample.cents);
                max = Math.max(max, sample.cents);
                count++;
            }
        }
        double mean = count == 0 ? 0.0 : sum / count;
        double durationSeconds = firstMs < 0L || lastMs <= firstMs ? 0.0 : (lastMs - firstMs) / 1000.0;
        return new WindowStats(count, mean, min, max, durationSeconds);
    }

    private void trimHistory(long nowMs) {
        while (!history.isEmpty() && nowMs - history.peekFirst().timestampMs > HISTORY_MS) {
            history.removeFirst();
        }
    }

    private static final class Sample {
        final long timestampMs;
        final boolean hasPitch;
        final double cents;
        final double rms;

        Sample(long timestampMs, boolean hasPitch, double cents, double rms) {
            this.timestampMs = timestampMs;
            this.hasPitch = hasPitch;
            this.cents = cents;
            this.rms = rms;
        }
    }

    private static final class WindowStats {
        final int count;
        final double mean;
        final double min;
        final double max;
        final double durationSeconds;

        WindowStats(int count, double mean, double min, double max, double durationSeconds) {
            this.count = count;
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.durationSeconds = durationSeconds;
        }
    }
}
