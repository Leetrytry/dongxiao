package com.dongxiao.practice.practice;

public final class PracticeNoteScore {
    public final int scaleDegree;
    public final int register;
    public final int midi;
    public final int score;
    public final double voicedSeconds;
    public final double meanCents;
    public final double meanAbsCents;
    public final double stabilityCents;
    public final double hitRate;
    public final String strengths;
    public final String weaknesses;
    public final String suggestions;

    public PracticeNoteScore(
            int scaleDegree,
            int register,
            int midi,
            int score,
            double voicedSeconds,
            double meanCents,
            double meanAbsCents,
            double stabilityCents,
            double hitRate,
            String strengths,
            String weaknesses,
            String suggestions
    ) {
        this.scaleDegree = scaleDegree;
        this.register = register;
        this.midi = midi;
        this.score = score;
        this.voicedSeconds = voicedSeconds;
        this.meanCents = meanCents;
        this.meanAbsCents = meanAbsCents;
        this.stabilityCents = stabilityCents;
        this.hitRate = hitRate;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.suggestions = suggestions;
    }
}
