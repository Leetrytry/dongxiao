package com.dongxiao.practice.audio;

public final class PitchDetector {
    private static final double DEFAULT_THRESHOLD = 0.15;
    private static final double MIN_RMS = 0.010;

    private PitchDetector() {
    }

    public static PitchResult detect(float[] samples, int sampleRate) {
        return detect(samples, sampleRate, 80.0, 1400.0, DEFAULT_THRESHOLD);
    }

    public static PitchResult detect(
            float[] samples,
            int sampleRate,
            double minFrequency,
            double maxFrequency,
            double threshold
    ) {
        if (samples == null || samples.length < 128 || sampleRate <= 0) {
            return PitchResult.unvoiced(0.0);
        }

        double rms = calculateRms(samples);
        if (rms < MIN_RMS) {
            return PitchResult.unvoiced(rms);
        }

        int minTau = Math.max(2, (int) Math.floor(sampleRate / maxFrequency));
        int maxTau = Math.min(samples.length / 2, (int) Math.ceil(sampleRate / minFrequency));
        if (maxTau <= minTau + 2) {
            return PitchResult.unvoiced(rms);
        }

        int window = samples.length - maxTau;
        if (window <= 0) {
            return PitchResult.unvoiced(rms);
        }

        double[] yin = new double[maxTau + 1];
        for (int tau = 1; tau <= maxTau; tau++) {
            double sum = 0.0;
            for (int i = 0; i < window; i++) {
                double delta = samples[i] - samples[i + tau];
                sum += delta * delta;
            }
            yin[tau] = sum;
        }

        yin[0] = 1.0;
        double runningSum = 0.0;
        for (int tau = 1; tau <= maxTau; tau++) {
            runningSum += yin[tau];
            if (runningSum == 0.0) {
                yin[tau] = 1.0;
            } else {
                yin[tau] = yin[tau] * tau / runningSum;
            }
        }

        int tauEstimate = -1;
        for (int tau = minTau; tau <= maxTau; tau++) {
            if (yin[tau] < threshold) {
                while (tau + 1 <= maxTau && yin[tau + 1] < yin[tau]) {
                    tau++;
                }
                tauEstimate = tau;
                break;
            }
        }

        if (tauEstimate == -1) {
            tauEstimate = minIndex(yin, minTau, maxTau);
            if (tauEstimate == -1 || yin[tauEstimate] > 0.45) {
                return PitchResult.unvoiced(rms);
            }
        }

        double betterTau = parabolicInterpolation(yin, tauEstimate, maxTau);
        if (betterTau <= 0.0) {
            return PitchResult.unvoiced(rms);
        }

        double frequency = sampleRate / betterTau;
        if (frequency < minFrequency || frequency > maxFrequency) {
            return PitchResult.unvoiced(rms);
        }

        double probability = Math.max(0.0, Math.min(1.0, 1.0 - yin[tauEstimate]));
        if (probability < 0.55) {
            return PitchResult.unvoiced(rms);
        }

        return PitchResult.voiced(frequency, probability, rms);
    }

    private static double calculateRms(float[] samples) {
        double sum = 0.0;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / samples.length);
    }

    private static int minIndex(double[] values, int startInclusive, int endInclusive) {
        int bestIndex = -1;
        double bestValue = Double.MAX_VALUE;
        for (int i = startInclusive; i <= endInclusive && i < values.length; i++) {
            if (values[i] < bestValue) {
                bestValue = values[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static double parabolicInterpolation(double[] values, int tau, int maxTau) {
        if (tau <= 1 || tau >= maxTau) {
            return tau;
        }

        double left = values[tau - 1];
        double center = values[tau];
        double right = values[tau + 1];
        double divisor = 2.0 * (2.0 * center - right - left);
        if (Math.abs(divisor) < 1e-9) {
            return tau;
        }
        return tau + (right - left) / divisor;
    }
}
