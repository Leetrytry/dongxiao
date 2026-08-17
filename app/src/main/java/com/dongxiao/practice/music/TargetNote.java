package com.dongxiao.practice.music;

public final class TargetNote {
    public final String label;
    public final int scaleDegree;
    public final int midi;
    public final double frequencyHz;

    public TargetNote(String label, int scaleDegree, int midi) {
        this.label = label;
        this.scaleDegree = scaleDegree;
        this.midi = midi;
        this.frequencyHz = MusicTheory.frequencyForMidi(midi);
    }

    public double centsFrom(double detectedFrequencyHz) {
        return MusicTheory.centsBetween(detectedFrequencyHz, frequencyHz);
    }

    @Override
    public String toString() {
        return label;
    }
}
