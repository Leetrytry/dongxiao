package com.dongxiao.practice.song;

public final class SongNote {
    public final String label;
    public final int midi;
    public final double beats;

    public SongNote(String label, int midi, double beats) {
        this.label = label;
        this.midi = midi;
        this.beats = beats;
    }
}
