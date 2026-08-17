package com.dongxiao.practice.music;

import java.util.Locale;

public final class MusicTheory {
    private static final String[] NOTE_NAMES = {
            "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
    };

    private MusicTheory() {
    }

    public static double frequencyForMidi(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    public static int nearestMidi(double frequencyHz) {
        return (int) Math.round(69.0 + 12.0 * log2(frequencyHz / 440.0));
    }

    public static double centsBetween(double frequencyHz, double targetFrequencyHz) {
        if (frequencyHz <= 0.0 || targetFrequencyHz <= 0.0) {
            return 0.0;
        }
        return 1200.0 * log2(frequencyHz / targetFrequencyHz);
    }

    public static String noteName(int midi) {
        int pitchClass = Math.floorMod(midi, 12);
        int octave = midi / 12 - 1;
        return NOTE_NAMES[pitchClass] + octave;
    }

    public static int midiForNote(String noteName, int octave) {
        return (octave + 1) * 12 + pitchClass(noteName);
    }

    public static String formatHz(double frequencyHz) {
        return String.format(Locale.CHINA, "%.1f Hz", frequencyHz);
    }

    public static String formatCents(double cents) {
        String sign = cents > 0.0 ? "+" : "";
        return String.format(Locale.CHINA, "%s%.0f cent", sign, cents);
    }

    public static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }

    private static int pitchClass(String noteName) {
        switch (noteName) {
            case "C":
                return 0;
            case "C#":
            case "Db":
                return 1;
            case "D":
                return 2;
            case "D#":
            case "Eb":
                return 3;
            case "E":
                return 4;
            case "F":
                return 5;
            case "F#":
            case "Gb":
                return 6;
            case "G":
                return 7;
            case "G#":
            case "Ab":
                return 8;
            case "A":
                return 9;
            case "A#":
            case "Bb":
                return 10;
            case "B":
                return 11;
            default:
                throw new IllegalArgumentException("Unknown note: " + noteName);
        }
    }
}
