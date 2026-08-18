package com.dongxiao.practice.music;

public final class TargetNote {
    public static final int REGISTER_LOW = 0;
    public static final int REGISTER_MIDDLE = 1;
    public static final int REGISTER_HIGH = 2;

    public final String label;
    public final int scaleDegree;
    public final int register;
    public final int midi;
    public final double frequencyHz;

    public TargetNote(String label, int scaleDegree, int midi) {
        this(label, scaleDegree, midi, REGISTER_MIDDLE);
    }

    public TargetNote(String label, int scaleDegree, int midi, int register) {
        this.label = label;
        this.scaleDegree = scaleDegree;
        this.register = register;
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
