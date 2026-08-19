package com.dongxiao.practice.practice;

import com.dongxiao.practice.audio.PitchResult;
import com.dongxiao.practice.music.TargetNote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PracticeSessionScorer {
    private static final double HIT_TOLERANCE_CENTS = 25.0;

    private long firstFrameMs = -1L;
    private long lastFrameMs = -1L;
    private long lastVoicedMs = -1L;
    private double voicedMs = 0.0;
    private double sumAbsCents = 0.0;
    private double sumCents = 0.0;
    private double sumSquaresCents = 0.0;
    private int voicedFrames = 0;
    private int hitFrames = 0;
    private int onsetCount = 0;
    private int rapidMoveCount = 0;
    private long lastOnsetMs = -1L;
    private long lastRapidMoveMs = -1L;
    private double lastRms = 0.0;
    private Double lastCents = null;
    private double maxVibratoRateHz = 0.0;
    private double maxVibratoDepthCents = 0.0;
    private double maxVibratoRegularityPercent = 0.0;
    private double maxTonguingRateHz = 0.0;
    private double maxTonguingEvennessPercent = 0.0;
    private double maxAbsSlideCents = 0.0;
    private double maxSlideRangeCents = 0.0;
    private double maxSlideSmoothnessPercent = 0.0;
    private boolean slideLanded = false;
    private int ornamentCount = 0;
    private double maxOrnamentExcursionCents = 0.0;
    private double frameScoreSum = 0.0;
    private int frameScoreCount = 0;
    private int scaleCompletedNotes = 0;
    private int scaleTotalNotes = 0;
    private int scaleWrongAttempts = 0;
    private boolean scaleCompleted = false;
    private final Map<Integer, NoteAccumulator> noteAccumulators = new LinkedHashMap<>();

    public void reset() {
        firstFrameMs = -1L;
        lastFrameMs = -1L;
        lastVoicedMs = -1L;
        voicedMs = 0.0;
        sumAbsCents = 0.0;
        sumCents = 0.0;
        sumSquaresCents = 0.0;
        voicedFrames = 0;
        hitFrames = 0;
        onsetCount = 0;
        rapidMoveCount = 0;
        lastOnsetMs = -1L;
        lastRapidMoveMs = -1L;
        lastRms = 0.0;
        lastCents = null;
        maxVibratoRateHz = 0.0;
        maxVibratoDepthCents = 0.0;
        maxVibratoRegularityPercent = 0.0;
        maxTonguingRateHz = 0.0;
        maxTonguingEvennessPercent = 0.0;
        maxAbsSlideCents = 0.0;
        maxSlideRangeCents = 0.0;
        maxSlideSmoothnessPercent = 0.0;
        slideLanded = false;
        ornamentCount = 0;
        maxOrnamentExcursionCents = 0.0;
        frameScoreSum = 0.0;
        frameScoreCount = 0;
        scaleCompletedNotes = 0;
        scaleTotalNotes = 0;
        scaleWrongAttempts = 0;
        scaleCompleted = false;
        noteAccumulators.clear();
    }

    public void update(PitchResult pitch, TargetNote target, PracticeStats stats, long nowMs) {
        if (firstFrameMs < 0L) {
            firstFrameMs = nowMs;
        }
        lastFrameMs = nowMs;
        updateOnsetCounter(pitch.rms, nowMs);

        boolean hasPitch = pitch.voiced && target != null;
        if (!hasPitch) {
            lastRms = pitch.rms;
            return;
        }

        double cents = target.centsFrom(pitch.frequencyHz);
        double absCents = Math.abs(cents);
        long deltaMs = 0L;
        if (lastVoicedMs > 0L) {
            deltaMs = Math.max(0L, Math.min(250L, nowMs - lastVoicedMs));
            voicedMs += deltaMs;
        }
        lastVoicedMs = nowMs;
        voicedFrames++;
        sumAbsCents += absCents;
        sumCents += cents;
        sumSquaresCents += cents * cents;
        if (absCents <= HIT_TOLERANCE_CENTS) {
            hitFrames++;
        }
        double frameScore = frameScore(absCents, stats.stabilityCents);
        frameScoreSum += frameScore;
        frameScoreCount++;
        NoteAccumulator accumulator = noteAccumulators.get(target.midi);
        if (accumulator == null) {
            accumulator = new NoteAccumulator(target);
            noteAccumulators.put(target.midi, accumulator);
        }
        accumulator.add(cents, absCents, deltaMs, frameScore);
        updateRapidMoveCounter(cents, nowMs);
        onsetCount = Math.max(onsetCount, stats.onsetCount);
        maxVibratoRateHz = Math.max(maxVibratoRateHz, stats.vibratoRateHz);
        maxVibratoDepthCents = Math.max(maxVibratoDepthCents, stats.vibratoDepthCents);
        maxVibratoRegularityPercent = Math.max(maxVibratoRegularityPercent, stats.vibratoRegularityPercent);
        maxTonguingRateHz = Math.max(maxTonguingRateHz, stats.tonguingRateHz);
        maxTonguingEvennessPercent = Math.max(maxTonguingEvennessPercent, stats.tonguingEvennessPercent);
        maxAbsSlideCents = Math.max(maxAbsSlideCents, Math.abs(stats.slideDeltaCents));
        maxSlideRangeCents = Math.max(maxSlideRangeCents, stats.slideRangeCents);
        maxSlideSmoothnessPercent = Math.max(maxSlideSmoothnessPercent, stats.slideSmoothnessPercent);
        slideLanded = slideLanded || stats.slideLanded;
        ornamentCount = Math.max(ornamentCount, stats.ornamentCount);
        maxOrnamentExcursionCents = Math.max(maxOrnamentExcursionCents, stats.ornamentLastExcursionCents);
        lastRms = pitch.rms;
        lastCents = cents;
    }

    public void updateScaleProgress(ScalePracticeProgress progress) {
        if (progress == null) {
            return;
        }
        scaleCompletedNotes = Math.max(scaleCompletedNotes, progress.completedNotes);
        scaleTotalNotes = Math.max(scaleTotalNotes, progress.totalNotes);
        scaleWrongAttempts = Math.max(scaleWrongAttempts, progress.wrongAttempts);
        scaleCompleted = scaleCompleted || progress.completed;
    }

    public PracticeScore finish(PracticeMode mode) {
        if (voicedFrames < 3 || voicedMs < 700.0) {
            return new PracticeScore(
                    0,
                    voicedMs / 1000.0,
                    0.0,
                    Double.NaN,
                    0.0,
                    "有效声音太少，无法评分。",
                    "请至少稳定吹奏 2 秒以上再结束。"
            );
        }

        double meanAbsCents = sumAbsCents / voicedFrames;
        double meanCents = sumCents / voicedFrames;
        double stabilityCents = Math.sqrt(Math.max(0.0, sumSquaresCents / voicedFrames - meanCents * meanCents));
        double hitRate = hitFrames * 100.0 / voicedFrames;
        double durationSeconds = voicedMs / 1000.0;

        double pitchScore = scoreLowerIsBetter(meanAbsCents, 8.0, 55.0);
        double stabilityScore = PracticeStats.stabilityPercent(stabilityCents);
        double durationScore = Math.min(100.0, durationSeconds / targetDuration(mode) * 100.0);
        double hitScore = hitRate;
        double modeScore = modeScore(mode, durationSeconds);
        double frameAverageScore = frameScoreCount == 0 ? 0.0 : frameScoreSum / frameScoreCount;
        int total;
        if (mode == PracticeMode.LONG_TONE) {
            total = clampScore(frameAverageScore);
        } else if (mode == PracticeMode.SCALE) {
            total = clampScore(
                    pitchScore * 0.26
                            + hitScore * 0.22
                            + modeScore * 0.34
                            + stabilityScore * 0.12
                            + durationScore * 0.06
            );
        } else if (mode == PracticeMode.TONGUING) {
            total = clampScore(modeScore * 0.46 + hitScore * 0.28 + pitchScore * 0.16 + durationScore * 0.10);
        } else if (mode == PracticeMode.VIBRATO) {
            total = clampScore(modeScore * 0.58 + pitchScore * 0.18 + hitScore * 0.14 + durationScore * 0.10);
        } else if (mode == PracticeMode.SLIDE) {
            total = clampScore(modeScore * 0.54 + hitScore * 0.24 + pitchScore * 0.12 + durationScore * 0.10);
        } else if (mode == PracticeMode.ORNAMENT) {
            total = clampScore(modeScore * 0.52 + hitScore * 0.24 + pitchScore * 0.14 + durationScore * 0.10);
        } else {
            total = clampScore(
                    pitchScore * 0.34
                            + stabilityScore * 0.24
                            + hitScore * 0.18
                            + durationScore * 0.12
                            + modeScore * 0.12
            );
        }

        return new PracticeScore(
                total,
                durationSeconds,
                meanAbsCents,
                stabilityCents,
                hitRate,
                commentFor(total, meanAbsCents, stabilityCents),
                detailFor(mode, modeScore, frameAverageScore),
                mode == PracticeMode.LONG_TONE || mode == PracticeMode.SCALE ? buildNoteScores() : null
        );
    }

    private List<PracticeNoteScore> buildNoteScores() {
        List<PracticeNoteScore> scores = new ArrayList<>();
        for (NoteAccumulator accumulator : noteAccumulators.values()) {
            if (accumulator.voicedFrames == 0) {
                continue;
            }
            scores.add(accumulator.toScore());
        }
        return scores;
    }

    private void updateOnsetCounter(double rms, long nowMs) {
        boolean crossed = lastRms <= 0.025 && rms > 0.025;
        boolean pastRefractory = lastOnsetMs < 0L || nowMs - lastOnsetMs > 120L;
        if (crossed && pastRefractory) {
            onsetCount++;
            lastOnsetMs = nowMs;
        }
    }

    private void updateRapidMoveCounter(double cents, long nowMs) {
        if (lastCents == null) {
            return;
        }
        boolean movedFast = Math.abs(cents - lastCents) > 45.0;
        boolean pastRefractory = lastRapidMoveMs < 0L || nowMs - lastRapidMoveMs > 150L;
        if (movedFast && pastRefractory) {
            rapidMoveCount++;
            lastRapidMoveMs = nowMs;
        }
    }

    private double modeScore(PracticeMode mode, double durationSeconds) {
        switch (mode) {
            case LONG_TONE:
                return Math.min(100.0, durationSeconds / 8.0 * 100.0);
            case SCALE:
                if (scaleTotalNotes > 0) {
                    double completionScore = scaleCompletedNotes * 100.0 / scaleTotalNotes;
                    double wrongPenalty = Math.min(35.0, scaleWrongAttempts * 5.0);
                    return Math.max(0.0, completionScore - wrongPenalty);
                }
                return Math.min(100.0, noteAccumulators.size() / 8.0 * 100.0);
            case TONGUING:
                double onsetScore = Math.min(100.0, onsetCount / 10.0 * 100.0);
                double tonguingRateScore = scoreInRange(maxTonguingRateHz, 1.5, 4.5, 0.6, 7.0);
                return onsetScore * 0.45 + tonguingRateScore * 0.25 + maxTonguingEvennessPercent * 0.30;
            case VIBRATO:
                double rateScore = scoreInRange(maxVibratoRateHz, 4.0, 6.5, 2.0, 8.0);
                double depthScore = scoreInRange(maxVibratoDepthCents, 18.0, 65.0, 8.0, 100.0);
                return rateScore * 0.45 + depthScore * 0.30 + maxVibratoRegularityPercent * 0.25;
            case SLIDE:
                double slideRangeScore = scoreInRange(maxSlideRangeCents, 80.0, 350.0, 35.0, 700.0);
                double landingScore = slideLanded ? 100.0 : Math.min(100.0, hitFrames * 100.0 / Math.max(1, voicedFrames));
                return slideRangeScore * 0.45 + maxSlideSmoothnessPercent * 0.35 + landingScore * 0.20;
            case ORNAMENT:
                double countScore = Math.min(100.0, ornamentCount / 6.0 * 100.0);
                double excursionScore = scoreInRange(maxOrnamentExcursionCents, 55.0, 220.0, 35.0, 420.0);
                double returnScore = Math.min(100.0, hitFrames * 100.0 / Math.max(1, voicedFrames));
                return countScore * 0.50 + excursionScore * 0.25 + returnScore * 0.25;
            default:
                return 70.0;
        }
    }

    private String detailFor(PracticeMode mode, double modeScore, double frameAverageScore) {
        switch (mode) {
            case LONG_TONE:
                return String.format(Locale.CHINA, "长音专项：逐音平均 %.0f 分，持续度 %.0f 分。", frameAverageScore, modeScore);
            case SCALE:
                if (scaleTotalNotes > 0) {
                    return String.format(
                            Locale.CHINA,
                            "音阶专项：级进/三度/分解完成 %d/%d，误吹 %d 次，序列分 %.0f。%s",
                            scaleCompletedNotes,
                            scaleTotalNotes,
                            scaleWrongAttempts,
                            modeScore,
                            scaleCompleted ? "完整跑完一轮。" : "还没有完成整轮。"
                    );
                }
                return String.format(Locale.CHINA, "音阶专项：覆盖 %d 个音，序列分 %.0f。", noteAccumulators.size(), modeScore);
            case TONGUING:
                return String.format(
                        Locale.CHINA,
                        "吐音专项：8连吐起音 %d 次，速率 %.1f 次/秒，4+4均匀度 %.0f 分。",
                        onsetCount,
                        maxTonguingRateHz,
                        maxTonguingEvennessPercent
                );
            case VIBRATO:
                return String.format(
                        Locale.CHINA,
                        "气震专项：脉冲最高 %.1f Hz，深度 ±%.0f cent，规律性 %.0f 分。",
                        maxVibratoRateHz,
                        maxVibratoDepthCents,
                        maxVibratoRegularityPercent
                );
            case SLIDE:
                return String.format(
                        Locale.CHINA,
                        "滑音专项：起点到落点 %.0f cent，平滑度 %.0f 分，%s。",
                        Math.max(maxAbsSlideCents, maxSlideRangeCents),
                        maxSlideSmoothnessPercent,
                        slideLanded ? "落点已回到目标音" : "落点还需更稳"
                );
            case ORNAMENT:
                return String.format(
                        Locale.CHINA,
                        "装饰音专项：本-邻-本回落 %d 次，最大离音 %.0f cent，快速波动 %d 次。",
                        ornamentCount,
                        maxOrnamentExcursionCents,
                        rapidMoveCount
                );
            default:
                return "";
        }
    }

    private static double targetDuration(PracticeMode mode) {
        return mode == PracticeMode.LONG_TONE ? 8.0 : 6.0;
    }

    private static double scoreLowerIsBetter(double value, double excellent, double poor) {
        if (value <= excellent) {
            return 100.0;
        }
        if (value >= poor) {
            return 0.0;
        }
        return 100.0 * (poor - value) / (poor - excellent);
    }

    private static double frameScore(double absCents, double stabilityCents) {
        double pitchScore = scoreLowerIsBetter(absCents, 8.0, 55.0);
        double stabilityScore = PracticeStats.stabilityPercent(stabilityCents);
        double hitScore = absCents <= HIT_TOLERANCE_CENTS
                ? 100.0
                : scoreLowerIsBetter(absCents, HIT_TOLERANCE_CENTS, 70.0);
        if (Double.isNaN(stabilityScore)) {
            return (pitchScore * 0.72 + hitScore * 0.10) / 0.82;
        }
        return pitchScore * 0.72 + stabilityScore * 0.18 + hitScore * 0.10;
    }

    private static double scoreInRange(double value, double minGood, double maxGood, double minOk, double maxOk) {
        if (value >= minGood && value <= maxGood) {
            return 100.0;
        }
        if (value < minOk || value > maxOk) {
            return 0.0;
        }
        if (value < minGood) {
            return 100.0 * (value - minOk) / (minGood - minOk);
        }
        return 100.0 * (maxOk - value) / (maxOk - maxGood);
    }

    private static int clampScore(double value) {
        return (int) Math.max(0.0, Math.min(100.0, Math.round(value)));
    }

    private static String commentFor(int score, double meanAbsCents, double stabilityCents) {
        if (score >= 90) {
            return "表现很稳，音准和控制都比较扎实。";
        }
        if (score >= 75) {
            return "整体可用，继续压低偏差和波动。";
        }
        if (score >= 60) {
            return "已经有可识别的目标音，但稳定度还需要加强。";
        }
        if (meanAbsCents > 45.0) {
            return "主要问题是音准偏差较大，先慢吹找准目标音。";
        }
        if (stabilityCents > 40.0) {
            return "主要问题是音高波动较大，先减小气息和口风变化。";
        }
        return "有效练习质量偏低，建议延长吹奏时间并保持目标音。";
    }

    private static final class NoteAccumulator {
        private final int scaleDegree;
        private final int register;
        private final int midi;
        private double voicedMs = 0.0;
        private double sumCents = 0.0;
        private double sumAbsCents = 0.0;
        private double sumSquaresCents = 0.0;
        private double frameScoreSum = 0.0;
        private int voicedFrames = 0;
        private int hitFrames = 0;

        NoteAccumulator(TargetNote target) {
            this.scaleDegree = target.scaleDegree;
            this.register = target.register;
            this.midi = target.midi;
        }

        void add(double cents, double absCents, long deltaMs, double frameScore) {
            voicedMs += deltaMs;
            sumCents += cents;
            sumAbsCents += absCents;
            sumSquaresCents += cents * cents;
            frameScoreSum += frameScore;
            voicedFrames++;
            if (absCents <= HIT_TOLERANCE_CENTS) {
                hitFrames++;
            }
        }

        PracticeNoteScore toScore() {
            double meanCents = sumCents / voicedFrames;
            double meanAbsCents = sumAbsCents / voicedFrames;
            double stabilityCents = Math.sqrt(Math.max(
                    0.0,
                    sumSquaresCents / voicedFrames - meanCents * meanCents
            ));
            double hitRate = hitFrames * 100.0 / voicedFrames;
            double voicedSeconds = voicedMs / 1000.0;
            int score = clampScore(frameScoreSum / voicedFrames);
            return new PracticeNoteScore(
                    scaleDegree,
                    register,
                    midi,
                    score,
                    voicedSeconds,
                    meanCents,
                    meanAbsCents,
                    stabilityCents,
                    hitRate,
                    strengthsFor(score, voicedSeconds, meanAbsCents, stabilityCents, hitRate),
                    weaknessFor(voicedSeconds, meanCents, meanAbsCents, stabilityCents, hitRate),
                    suggestionFor(voicedSeconds, meanCents, meanAbsCents, stabilityCents, hitRate)
            );
        }
    }

    private static String strengthsFor(
            int score,
            double voicedSeconds,
            double meanAbsCents,
            double stabilityCents,
            double hitRate
    ) {
        if (score >= 90) {
            return "音准、稳定度和命中率都比较扎实。";
        }
        if (meanAbsCents <= 12.0 && hitRate >= 80.0) {
            return "音准接近目标，落在有效范围内的时间较多。";
        }
        if (PracticeStats.stabilityPercent(stabilityCents) >= 80.0) {
            return "气息控制较平稳，音高波动不大。";
        }
        if (voicedSeconds >= 2.0) {
            return "该音保持时间较完整，已经形成可分析的长音。";
        }
        return "已经能被系统稳定识别，可作为后续细练基础。";
    }

    private static String weaknessFor(
            double voicedSeconds,
            double meanCents,
            double meanAbsCents,
            double stabilityCents,
            double hitRate
    ) {
        if (voicedSeconds < 1.5) {
            return "该音有效练习时间偏短，评分代表性不足。";
        }
        if (meanAbsCents > 35.0) {
            return meanCents > 0.0
                    ? "整体偏高，目标音中心没有压稳。"
                    : "整体偏低，目标音中心没有托住。";
        }
        if (stabilityCents > 35.0) {
            return "音高上下摆动偏大，气息和口风变化较明显。";
        }
        if (hitRate < 60.0) {
            return "进入 ±25 cent 有效区间的时间偏少。";
        }
        if (Math.abs(meanCents) > 12.0) {
            return meanCents > 0.0 ? "平均音高略偏高。" : "平均音高略偏低。";
        }
        return "暂无明显短板，重点是继续拉长稳定时间。";
    }

    private static String suggestionFor(
            double voicedSeconds,
            double meanCents,
            double meanAbsCents,
            double stabilityCents,
            double hitRate
    ) {
        if (voicedSeconds < 1.5) {
            return "单独延长这个音，每次至少保持 2 秒以上再换音。";
        }
        if (meanAbsCents > 35.0) {
            return meanCents > 0.0
                    ? "放松口风并略收气息角度，先把音高向下校准。"
                    : "加强气息支撑并略收稳口风，先把音高向上校准。";
        }
        if (stabilityCents > 35.0) {
            return "用更均匀的气流吹直音，减少口风、下颌和手指的细小晃动。";
        }
        if (hitRate < 60.0) {
            return "先慢吹找准目标，进入 ±25 cent 后再延长时值。";
        }
        return "保持当前口风和气息状态，逐步把稳定时长拉到 6 到 8 秒。";
    }
}
