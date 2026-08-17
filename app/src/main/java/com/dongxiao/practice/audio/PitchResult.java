package com.dongxiao.practice.audio;

public final class PitchResult {
    public final boolean voiced;
    public final double frequencyHz;
    public final double probability;
    public final double rms;

    private PitchResult(boolean voiced, double frequencyHz, double probability, double rms) {
        this.voiced = voiced;
        this.frequencyHz = frequencyHz;
        this.probability = probability;
        this.rms = rms;
    }

    public static PitchResult voiced(double frequencyHz, double probability, double rms) {
        return new PitchResult(true, frequencyHz, probability, rms);
    }

    public static PitchResult unvoiced(double rms) {
        return new PitchResult(false, 0.0, 0.0, rms);
    }
}
