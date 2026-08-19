package com.dongxiao.practice.practice;

import java.util.Locale;

public final class PracticeStats {
    private static final double STABILITY_EXCELLENT_CENTS = 8.0;
    private static final double STABILITY_POOR_CENTS = 50.0;

    public final boolean hasPitch;
    public final double cents;
    public final double stabilityCents;
    public final boolean stabilityReady;
    public final double heldSeconds;
    public final double vibratoRateHz;
    public final double vibratoDepthCents;
    public final double vibratoRegularityPercent;
    public final int onsetCount;
    public final double tonguingRateHz;
    public final double tonguingEvennessPercent;
    public final int rapidMoveCount;
    public final double slideDeltaCents;
    public final double slideRangeCents;
    public final double slideSmoothnessPercent;
    public final boolean slideLanded;
    public final int ornamentCount;
    public final double ornamentLastExcursionCents;
    public final double ornamentLastDurationMs;

    public PracticeStats(
            boolean hasPitch,
            double cents,
            double stabilityCents,
            boolean stabilityReady,
            double heldSeconds,
            double vibratoRateHz,
            double vibratoDepthCents,
            double vibratoRegularityPercent,
            int onsetCount,
            double tonguingRateHz,
            double tonguingEvennessPercent,
            int rapidMoveCount,
            double slideDeltaCents,
            double slideRangeCents,
            double slideSmoothnessPercent,
            boolean slideLanded,
            int ornamentCount,
            double ornamentLastExcursionCents,
            double ornamentLastDurationMs
    ) {
        this.hasPitch = hasPitch;
        this.cents = cents;
        this.stabilityCents = stabilityCents;
        this.stabilityReady = stabilityReady;
        this.heldSeconds = heldSeconds;
        this.vibratoRateHz = vibratoRateHz;
        this.vibratoDepthCents = vibratoDepthCents;
        this.vibratoRegularityPercent = vibratoRegularityPercent;
        this.onsetCount = onsetCount;
        this.tonguingRateHz = tonguingRateHz;
        this.tonguingEvennessPercent = tonguingEvennessPercent;
        this.rapidMoveCount = rapidMoveCount;
        this.slideDeltaCents = slideDeltaCents;
        this.slideRangeCents = slideRangeCents;
        this.slideSmoothnessPercent = slideSmoothnessPercent;
        this.slideLanded = slideLanded;
        this.ornamentCount = ornamentCount;
        this.ornamentLastExcursionCents = ornamentLastExcursionCents;
        this.ornamentLastDurationMs = ornamentLastDurationMs;
    }

    public static double stabilityPercent(double stabilityCents) {
        if (Double.isNaN(stabilityCents) || Double.isInfinite(stabilityCents)) {
            return Double.NaN;
        }
        if (stabilityCents <= STABILITY_EXCELLENT_CENTS) {
            return 100.0;
        }
        if (stabilityCents >= STABILITY_POOR_CENTS) {
            return 0.0;
        }
        return 100.0 * (STABILITY_POOR_CENTS - stabilityCents)
                / (STABILITY_POOR_CENTS - STABILITY_EXCELLENT_CENTS);
    }

    public static String formatStabilityPercent(double stabilityCents) {
        double percent = stabilityPercent(stabilityCents);
        if (Double.isNaN(percent)) {
            return "--";
        }
        return String.format(Locale.CHINA, "%.0f%%", percent);
    }
}
