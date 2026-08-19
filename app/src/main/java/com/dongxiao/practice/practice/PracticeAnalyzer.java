package com.dongxiao.practice.practice;

import com.dongxiao.practice.audio.PitchResult;
import com.dongxiao.practice.music.TargetNote;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PracticeAnalyzer {
    private static final long HISTORY_MS = 3000L;
    private static final long STABILITY_WINDOW_MS = 2000L;
    private static final long MIN_STABILITY_SPAN_MS = 700L;
    private static final int MIN_STABILITY_SAMPLES = 6;
    private static final long SLIDE_WINDOW_MS = 1500L;
    private static final double ONSET_RMS = 0.025;
    private static final double ONSET_RISE_DELTA = 0.012;
    private static final long ONSET_REFRACTORY_MS = 150L;
    private static final double LONG_TONE_TOLERANCE_CENTS = 25.0;
    private static final double ORNAMENT_CENTER_CENTS = 25.0;
    private static final double ORNAMENT_AWAY_CENTS = 55.0;
    private static final long ORNAMENT_MIN_MS = 60L;
    private static final long ORNAMENT_MAX_MS = 850L;

    private final Deque<Sample> history = new ArrayDeque<>();
    private final Deque<Long> onsetTimes = new ArrayDeque<>();
    private long stableSinceMs = -1L;
    private long lastOnsetMs = -1L;
    private long lastRapidMoveMs = -1L;
    private long ornamentAwaySinceMs = -1L;
    private double lastRms = 0.0;
    private Double lastCents = null;
    private double ornamentMaxExcursionCents = 0.0;
    private double ornamentLastExcursionCents = 0.0;
    private double ornamentLastDurationMs = 0.0;
    private int onsetCount = 0;
    private int rapidMoveCount = 0;
    private int ornamentCount = 0;

    public void reset() {
        history.clear();
        onsetTimes.clear();
        stableSinceMs = -1L;
        lastOnsetMs = -1L;
        lastRapidMoveMs = -1L;
        ornamentAwaySinceMs = -1L;
        lastRms = 0.0;
        lastCents = null;
        ornamentMaxExcursionCents = 0.0;
        ornamentLastExcursionCents = 0.0;
        ornamentLastDurationMs = 0.0;
        onsetCount = 0;
        rapidMoveCount = 0;
        ornamentCount = 0;
    }

    public PracticeStats update(PitchResult pitch, TargetNote target, long nowMs) {
        boolean hasPitch = pitch.voiced && target != null;
        double cents = hasPitch ? target.centsFrom(pitch.frequencyHz) : 0.0;

        history.addLast(new Sample(nowMs, hasPitch, cents, pitch.rms));
        trimHistory(nowMs);

        updateLongToneHold(hasPitch, cents, nowMs);
        updateOnsetCounter(pitch.rms, nowMs);
        updateRapidMoveCounter(hasPitch, cents, nowMs);
        updateOrnamentCounter(hasPitch, cents, nowMs);

        lastRms = pitch.rms;
        if (hasPitch) {
            lastCents = cents;
        }

        StabilityStats stabilityStats = stability(nowMs);
        SlideStats slideStats = slideStats(nowMs);
        return new PracticeStats(
                hasPitch,
                cents,
                stabilityStats.cents,
                stabilityStats.ready,
                heldSeconds(nowMs),
                vibratoRateHz(nowMs),
                vibratoDepthCents(nowMs),
                vibratoRegularityPercent(nowMs),
                onsetCount,
                tonguingRateHz(nowMs),
                tonguingEvennessPercent(nowMs),
                rapidMoveCount,
                slideStats.deltaCents,
                slideStats.rangeCents,
                slideStats.smoothnessPercent,
                slideStats.landed,
                ornamentCount,
                ornamentLastExcursionCents,
                ornamentLastDurationMs
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
        boolean impulse = rms > ONSET_RMS && rms - lastRms >= ONSET_RISE_DELTA;
        boolean pastRefractory = lastOnsetMs < 0L || nowMs - lastOnsetMs > ONSET_REFRACTORY_MS;
        if ((crossed || impulse) && pastRefractory) {
            onsetCount++;
            lastOnsetMs = nowMs;
            onsetTimes.addLast(nowMs);
        }
        trimOnsetTimes(nowMs);
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

    private void updateOrnamentCounter(boolean hasPitch, double cents, long nowMs) {
        if (!hasPitch) {
            ornamentAwaySinceMs = -1L;
            ornamentMaxExcursionCents = 0.0;
            return;
        }

        double absCents = Math.abs(cents);
        if (ornamentAwaySinceMs < 0L) {
            if (absCents >= ORNAMENT_AWAY_CENTS) {
                ornamentAwaySinceMs = nowMs;
                ornamentMaxExcursionCents = absCents;
            }
            return;
        }

        ornamentMaxExcursionCents = Math.max(ornamentMaxExcursionCents, absCents);
        long durationMs = nowMs - ornamentAwaySinceMs;
        if (absCents <= ORNAMENT_CENTER_CENTS) {
            if (durationMs >= ORNAMENT_MIN_MS
                    && durationMs <= ORNAMENT_MAX_MS
                    && ornamentMaxExcursionCents >= ORNAMENT_AWAY_CENTS) {
                ornamentCount++;
                ornamentLastExcursionCents = ornamentMaxExcursionCents;
                ornamentLastDurationMs = durationMs;
            }
            ornamentAwaySinceMs = -1L;
            ornamentMaxExcursionCents = 0.0;
        } else if (durationMs > ORNAMENT_MAX_MS) {
            ornamentAwaySinceMs = -1L;
            ornamentMaxExcursionCents = 0.0;
        }
    }

    private double heldSeconds(long nowMs) {
        if (stableSinceMs < 0L) {
            return 0.0;
        }
        return (nowMs - stableSinceMs) / 1000.0;
    }

    private StabilityStats stability(long nowMs) {
        double sum = 0.0;
        double sumSquares = 0.0;
        int count = 0;
        long firstMs = -1L;
        long lastMs = -1L;
        for (Sample sample : history) {
            if (sample.hasPitch && nowMs - sample.timestampMs <= STABILITY_WINDOW_MS) {
                if (firstMs < 0L) {
                    firstMs = sample.timestampMs;
                }
                lastMs = sample.timestampMs;
                sum += sample.cents;
                sumSquares += sample.cents * sample.cents;
                count++;
            }
        }
        boolean ready = count >= MIN_STABILITY_SAMPLES
                && firstMs >= 0L
                && lastMs - firstMs >= MIN_STABILITY_SPAN_MS;
        if (!ready) {
            return new StabilityStats(Double.NaN, false);
        }
        double mean = sum / count;
        double variance = Math.max(0.0, sumSquares / count - mean * mean);
        return new StabilityStats(Math.sqrt(variance), true);
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

    private double vibratoRegularityPercent(long nowMs) {
        CrossingStats crossings = crossingStats(nowMs, 2200L);
        if (crossings.count < 4 || crossings.meanIntervalMs <= 0.0) {
            return 0.0;
        }
        double coefficientOfVariation = crossings.stddevIntervalMs / crossings.meanIntervalMs;
        return percentLowerIsBetter(coefficientOfVariation, 0.12, 0.55);
    }

    private double tonguingRateHz(long nowMs) {
        trimOnsetTimes(nowMs);
        if (onsetTimes.size() < 2) {
            return 0.0;
        }
        long first = onsetTimes.peekFirst();
        long last = onsetTimes.peekLast();
        if (last <= first) {
            return 0.0;
        }
        return (onsetTimes.size() - 1) * 1000.0 / (last - first);
    }

    private double tonguingEvennessPercent(long nowMs) {
        trimOnsetTimes(nowMs);
        if (onsetTimes.size() < 3) {
            return 0.0;
        }
        long previous = -1L;
        double sum = 0.0;
        double sumSquares = 0.0;
        int count = 0;
        for (Long time : onsetTimes) {
            if (previous >= 0L) {
                double interval = time - previous;
                sum += interval;
                sumSquares += interval * interval;
                count++;
            }
            previous = time;
        }
        if (count == 0) {
            return 0.0;
        }
        double mean = sum / count;
        double variance = Math.max(0.0, sumSquares / count - mean * mean);
        double coefficientOfVariation = Math.sqrt(variance) / Math.max(1.0, mean);
        return percentLowerIsBetter(coefficientOfVariation, 0.10, 0.55);
    }

    private SlideStats slideStats(long nowMs) {
        Sample first = null;
        Sample last = null;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double totalMove = 0.0;
        Double previousCents = null;
        for (Sample sample : history) {
            if (sample.hasPitch && nowMs - sample.timestampMs <= SLIDE_WINDOW_MS) {
                if (first == null) {
                    first = sample;
                }
                last = sample;
                min = Math.min(min, sample.cents);
                max = Math.max(max, sample.cents);
                if (previousCents != null) {
                    totalMove += Math.abs(sample.cents - previousCents);
                }
                previousCents = sample.cents;
            }
        }
        if (first == null || last == null || first == last) {
            return new SlideStats(0.0, 0.0, 0.0, false);
        }
        double delta = last.cents - first.cents;
        double range = Math.max(0.0, max - min);
        double netMove = Math.abs(delta);
        double smoothness = totalMove <= 1.0 ? 0.0 : Math.min(100.0, netMove / totalMove * 100.0);
        boolean landed = Math.abs(last.cents) <= LONG_TONE_TOLERANCE_CENTS
                && Math.abs(first.cents) >= 55.0
                && range >= 70.0;
        return new SlideStats(delta, range, smoothness, landed);
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

    private void trimOnsetTimes(long nowMs) {
        while (!onsetTimes.isEmpty() && nowMs - onsetTimes.peekFirst() > 4000L) {
            onsetTimes.removeFirst();
        }
    }

    private CrossingStats crossingStats(long nowMs, long windowMs) {
        WindowStats stats = windowStats(nowMs, windowMs);
        if (stats.count < 6) {
            return new CrossingStats(0, 0.0, 0.0);
        }

        long previousCrossingMs = -1L;
        int previousSign = 0;
        double sum = 0.0;
        double sumSquares = 0.0;
        int intervalCount = 0;
        int crossingCount = 0;
        for (Sample sample : history) {
            if (!sample.hasPitch || nowMs - sample.timestampMs > windowMs) {
                continue;
            }
            double centered = sample.cents - stats.mean;
            int sign = Math.abs(centered) < 5.0 ? 0 : (centered > 0.0 ? 1 : -1);
            if (sign != 0 && previousSign != 0 && sign != previousSign) {
                crossingCount++;
                if (previousCrossingMs >= 0L) {
                    double interval = sample.timestampMs - previousCrossingMs;
                    sum += interval;
                    sumSquares += interval * interval;
                    intervalCount++;
                }
                previousCrossingMs = sample.timestampMs;
            }
            if (sign != 0) {
                previousSign = sign;
            }
        }
        if (intervalCount == 0) {
            return new CrossingStats(crossingCount, 0.0, 0.0);
        }
        double mean = sum / intervalCount;
        double variance = Math.max(0.0, sumSquares / intervalCount - mean * mean);
        return new CrossingStats(crossingCount, mean, Math.sqrt(variance));
    }

    private static double percentLowerIsBetter(double value, double excellent, double poor) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        if (value <= excellent) {
            return 100.0;
        }
        if (value >= poor) {
            return 0.0;
        }
        return 100.0 * (poor - value) / (poor - excellent);
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

    private static final class SlideStats {
        final double deltaCents;
        final double rangeCents;
        final double smoothnessPercent;
        final boolean landed;

        SlideStats(double deltaCents, double rangeCents, double smoothnessPercent, boolean landed) {
            this.deltaCents = deltaCents;
            this.rangeCents = rangeCents;
            this.smoothnessPercent = smoothnessPercent;
            this.landed = landed;
        }
    }

    private static final class CrossingStats {
        final int count;
        final double meanIntervalMs;
        final double stddevIntervalMs;

        CrossingStats(int count, double meanIntervalMs, double stddevIntervalMs) {
            this.count = count;
            this.meanIntervalMs = meanIntervalMs;
            this.stddevIntervalMs = stddevIntervalMs;
        }
    }

    private static final class StabilityStats {
        final double cents;
        final boolean ready;

        StabilityStats(double cents, boolean ready) {
            this.cents = cents;
            this.ready = ready;
        }
    }
}
