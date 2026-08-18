package com.dongxiao.practice.practice;

public final class PracticeStats {
    private static final double STABILITY_EXCELLENT_CENTS = 8.0;
    private static final double STABILITY_POOR_CENTS = 50.0;

    public final boolean hasPitch;
    public final double cents;
    public final double stabilityCents;
    public final double heldSeconds;
    public final double vibratoRateHz;
    public final double vibratoDepthCents;
    public final int onsetCount;
    public final int rapidMoveCount;
    public final double slideDeltaCents;

    public PracticeStats(
            boolean hasPitch,
            double cents,
            double stabilityCents,
            double heldSeconds,
            double vibratoRateHz,
            double vibratoDepthCents,
            int onsetCount,
            int rapidMoveCount,
            double slideDeltaCents
    ) {
        this.hasPitch = hasPitch;
        this.cents = cents;
        this.stabilityCents = stabilityCents;
        this.heldSeconds = heldSeconds;
        this.vibratoRateHz = vibratoRateHz;
        this.vibratoDepthCents = vibratoDepthCents;
        this.onsetCount = onsetCount;
        this.rapidMoveCount = rapidMoveCount;
        this.slideDeltaCents = slideDeltaCents;
    }

    public static double stabilityPercent(double stabilityCents) {
        if (Double.isNaN(stabilityCents) || Double.isInfinite(stabilityCents)) {
            return 0.0;
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
}
