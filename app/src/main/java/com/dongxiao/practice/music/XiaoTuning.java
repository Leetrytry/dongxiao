package com.dongxiao.practice.music;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class XiaoTuning {
    private static final int[] DEGREE_ORDER = {1, 2, 3, 4, 5, 6, 7};
    private static final int[] DEGREE_TO_MAJOR_SEMITONE = {
            0, 0, 2, 4, 5, 7, 9, 11
    };

    public final String label;
    public final int tonicMidi;
    public final int tubeMidi;

    public XiaoTuning(String label, int tonicMidi) {
        this.label = label;
        this.tonicMidi = tonicMidi;
        this.tubeMidi = tonicMidi + semitoneOfDegree(5) - 12;
    }

    public static List<XiaoTuning> defaults() {
        return Arrays.asList(
                new XiaoTuning("G调", MusicTheory.midiForNote("G", 4)),
                new XiaoTuning("F调", MusicTheory.midiForNote("F", 4)),
                new XiaoTuning("E调", MusicTheory.midiForNote("E", 4))
        );
    }

    public List<TargetNote> createTargets(FingeringMode fingeringMode) {
        List<TargetNote> targets = new ArrayList<>();
        int startIndex = degreeIndex(fingeringMode.tubeDegree);
        int tubeSemitone = semitoneOfDegree(fingeringMode.tubeDegree);

        for (int step = 0; step < 15; step++) {
            int degreeIndex = startIndex + step;
            int degree = DEGREE_ORDER[degreeIndex % DEGREE_ORDER.length];
            int octaveAdd = degreeIndex / DEGREE_ORDER.length;
            int semitoneOffset = semitoneOfDegree(degree) + octaveAdd * 12 - tubeSemitone;
            int midi = tubeMidi + semitoneOffset;
            targets.add(new TargetNote(degreeLabel(degreeIndex, degree), degree, midi));
        }
        return targets;
    }

    public TargetNote closestTarget(double frequencyHz, List<TargetNote> candidates) {
        TargetNote closest = candidates.get(0);
        double closestAbsCents = Double.MAX_VALUE;
        for (TargetNote candidate : candidates) {
            double cents = Math.abs(candidate.centsFrom(frequencyHz));
            if (cents < closestAbsCents) {
                closest = candidate;
                closestAbsCents = cents;
            }
        }
        return closest;
    }

    public String referenceText(FingeringMode fingeringMode) {
        int tubeDegree = fingeringMode == null ? 5 : fingeringMode.tubeDegree;
        String tubeDegreeLabel = degreeLabel(degreeIndex(tubeDegree), tubeDegree);
        return label + "基准：" + tubeDegreeLabel + " = "
                + MusicTheory.noteName(tubeMidi) + " / "
                + MusicTheory.formatHz(MusicTheory.frequencyForMidi(tubeMidi));
    }

    @Override
    public String toString() {
        return label;
    }

    private static int semitoneOfDegree(int degree) {
        if (degree < 1 || degree > 7) {
            throw new IllegalArgumentException("Scale degree must be 1..7: " + degree);
        }
        return DEGREE_TO_MAJOR_SEMITONE[degree];
    }

    private static int degreeIndex(int degree) {
        for (int i = 0; i < DEGREE_ORDER.length; i++) {
            if (DEGREE_ORDER[i] == degree) {
                return i;
            }
        }
        throw new IllegalArgumentException("Scale degree must be 1..7: " + degree);
    }

    private static String degreeLabel(int degreeIndex, int degree) {
        int register = degreeIndex / DEGREE_ORDER.length;
        if (register <= 0) {
            return degree + "\u0323";
        }
        if (register == 1) {
            return String.valueOf(degree);
        }
        return degree + "\u0307";
    }

}
