package com.dongxiao.practice.practice;

public final class PracticeStats {
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
}
