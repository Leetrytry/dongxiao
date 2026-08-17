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
    public final int tubeMidi;

    public XiaoTuning(String label, int tubeMidi) {
        this.label = label;
        this.tubeMidi = tubeMidi;
    }

    public static List<XiaoTuning> defaults() {
        return Arrays.asList(
                new XiaoTuning("C调洞箫（筒音 C3）", MusicTheory.midiForNote("C", 3)),
                new XiaoTuning("D调洞箫（筒音 D3）", MusicTheory.midiForNote("D", 3)),
                new XiaoTuning("Eb调洞箫（筒音 Eb3）", MusicTheory.midiForNote("Eb", 3)),
                new XiaoTuning("E调洞箫（筒音 E3）", MusicTheory.midiForNote("E", 3)),
                new XiaoTuning("F调洞箫（筒音 F3）", MusicTheory.midiForNote("F", 3)),
                new XiaoTuning("F#调洞箫（筒音 F#3）", MusicTheory.midiForNote("F#", 3)),
                new XiaoTuning("G调洞箫（筒音 G3）", MusicTheory.midiForNote("G", 3)),
                new XiaoTuning("A调洞箫（筒音 A3）", MusicTheory.midiForNote("A", 3)),
                new XiaoTuning("Bb调洞箫（筒音 Bb3）", MusicTheory.midiForNote("Bb", 3)),
                new XiaoTuning("B调洞箫（筒音 B3）", MusicTheory.midiForNote("B", 3))
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
            targets.add(new TargetNote(registerLabel(step) + degree, degree, midi));
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

    private static String registerLabel(int step) {
        if (step < 3) {
            return "低音";
        }
        if (step < 10) {
            return "中音";
        }
        return "高音";
    }
}
