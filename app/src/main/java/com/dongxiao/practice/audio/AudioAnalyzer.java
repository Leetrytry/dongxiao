package com.dongxiao.practice.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public final class AudioAnalyzer {
    public interface Listener {
        void onAudioFrame(PitchResult result, int sampleRate, long timestampMs);

        void onAudioError(String message);
    }

    private static final int[] SAMPLE_RATE_CANDIDATES = {44100, 48000, 22050};
    private static final int FRAME_SIZE = 4096;

    private final Listener listener;
    private volatile boolean running;
    private Thread workerThread;
    private AudioRecord audioRecord;

    public AudioAnalyzer(Listener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    @SuppressLint("MissingPermission")
    public synchronized void start() {
        if (running) {
            return;
        }

        int sampleRate = chooseSampleRate();
        if (sampleRate <= 0) {
            listener.onAudioError("无法初始化麦克风：没有可用采样率。");
            return;
        }

        int minBuffer = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufferSize = Math.max(minBuffer, FRAME_SIZE * 4);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            releaseRecord();
            listener.onAudioError("无法初始化麦克风，请检查录音权限或设备状态。");
            return;
        }

        running = true;
        workerThread = new Thread(() -> captureLoop(sampleRate), "dongxiao-audio-analyzer");
        workerThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException ignored) {
                // Recorder may already be stopped by the platform.
            }
        }

        if (workerThread != null) {
            try {
                workerThread.join(800);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }
        releaseRecord();
    }

    private void captureLoop(int sampleRate) {
        short[] shortBuffer = new short[FRAME_SIZE];
        try {
            audioRecord.startRecording();
            while (running) {
                int read = audioRecord.read(shortBuffer, 0, shortBuffer.length);
                if (read <= 0) {
                    continue;
                }

                float[] samples = new float[read];
                for (int i = 0; i < read; i++) {
                    samples[i] = shortBuffer[i] / 32768.0f;
                }

                PitchResult result = PitchDetector.detect(samples, sampleRate);
                listener.onAudioFrame(result, sampleRate, System.currentTimeMillis());
            }
        } catch (IllegalStateException error) {
            listener.onAudioError("录音过程中断：" + error.getMessage());
        } finally {
            running = false;
        }
    }

    private static int chooseSampleRate() {
        for (int sampleRate : SAMPLE_RATE_CANDIDATES) {
            int minBuffer = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            if (minBuffer > 0) {
                return sampleRate;
            }
        }
        return -1;
    }

    private void releaseRecord() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }
}
