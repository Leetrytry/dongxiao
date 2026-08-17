package com.dongxiao.practice.song;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.dongxiao.practice.music.MusicTheory;

public final class SongPlayer {
    public interface Listener {
        void onProgress(double beatPosition, int noteIndex);

        void onFinished();

        void onError(String message);
    }

    private static final int SAMPLE_RATE = 44100;
    private static final int CHUNK_FRAMES = 512;
    private static final double TWO_PI = Math.PI * 2.0;

    private final Listener listener;
    private volatile boolean running;
    private Thread workerThread;
    private AudioTrack audioTrack;

    public SongPlayer(Listener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void start(PracticeSong song) {
        stop();
        running = true;
        workerThread = new Thread(() -> playLoop(song), "dongxiao-song-player");
        workerThread.start();
    }

    public synchronized void stop() {
        running = false;
        AudioTrack track = audioTrack;
        if (track != null) {
            try {
                track.pause();
                track.flush();
            } catch (IllegalStateException ignored) {
                // Track may already be stopped by the platform.
            }
        }
        if (workerThread != null && workerThread != Thread.currentThread()) {
            try {
                workerThread.join(800);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        workerThread = null;
        releaseTrack();
    }

    private void playLoop(PracticeSong song) {
        int minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (minBuffer <= 0) {
            running = false;
            listener.onError("无法初始化伴奏播放器。");
            return;
        }

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                Math.max(minBuffer, CHUNK_FRAMES * 4),
                AudioTrack.MODE_STREAM
        );
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            running = false;
            releaseTrack();
            listener.onError("无法初始化伴奏播放器。");
            return;
        }

        double secondsPerBeat = 60.0 / song.tempoBpm;
        int totalFrames = (int) Math.ceil(song.totalBeats() * secondsPerBeat * SAMPLE_RATE);
        short[] buffer = new short[CHUNK_FRAMES];
        int frame = 0;
        int lastNoteIndex = -1;
        int progressFrameStep = SAMPLE_RATE / 20;

        try {
            audioTrack.play();
            while (running && frame < totalFrames) {
                int framesThisChunk = Math.min(CHUNK_FRAMES, totalFrames - frame);
                for (int i = 0; i < framesThisChunk; i++) {
                    buffer[i] = synthSample(song, secondsPerBeat, frame + i);
                }
                audioTrack.write(buffer, 0, framesThisChunk);

                double beat = frame / (SAMPLE_RATE * secondsPerBeat);
                int noteIndex = song.noteIndexAtBeat(beat);
                if (noteIndex != lastNoteIndex || frame % progressFrameStep < CHUNK_FRAMES) {
                    lastNoteIndex = noteIndex;
                    listener.onProgress(beat, noteIndex);
                }
                frame += framesThisChunk;
            }
            boolean completed = running && frame >= totalFrames;
            running = false;
            if (completed) {
                listener.onProgress(song.totalBeats(), Math.max(0, song.notes.size() - 1));
                listener.onFinished();
            }
        } catch (IllegalStateException error) {
            running = false;
            listener.onError("伴奏播放中断：" + error.getMessage());
        } finally {
            releaseTrack();
        }
    }

    private short synthSample(PracticeSong song, double secondsPerBeat, int frame) {
        double time = frame / (double) SAMPLE_RATE;
        double beat = time / secondsPerBeat;
        int noteIndex = song.noteIndexAtBeat(beat);
        SongNote note = song.notes.get(noteIndex);
        double noteStartBeat = song.beatStartForIndex(noteIndex);
        double localSeconds = Math.max(0.0, (beat - noteStartBeat) * secondsPerBeat);
        double noteSeconds = Math.max(0.08, note.beats * secondsPerBeat);

        double melody = note.rest ? 0.0 : Math.sin(TWO_PI * MusicTheory.frequencyForMidi(note.midi) * time)
                * pluckEnvelope(localSeconds, noteSeconds) * 0.16;
        double root = Math.sin(TWO_PI * MusicTheory.frequencyForMidi(song.rootMidi - 12) * time) * 0.08;
        double fifth = Math.sin(TWO_PI * MusicTheory.frequencyForMidi(song.rootMidi - 5) * time) * 0.05;
        double pulse = beatPulse(beat) * 0.05;
        double sample = softClip(melody + root + fifth + pulse);
        return (short) Math.round(sample * Short.MAX_VALUE);
    }

    private static double pluckEnvelope(double localSeconds, double noteSeconds) {
        double attack = Math.min(0.04, noteSeconds * 0.2);
        double release = Math.min(0.12, noteSeconds * 0.35);
        if (localSeconds < attack) {
            return localSeconds / attack;
        }
        if (localSeconds > noteSeconds - release) {
            return Math.max(0.0, (noteSeconds - localSeconds) / release);
        }
        return 0.75;
    }

    private static double beatPulse(double beat) {
        double phase = beat - Math.floor(beat);
        if (phase > 0.12) {
            return 0.0;
        }
        return Math.sin(Math.PI * phase / 0.12) * (1.0 - phase / 0.12);
    }

    private static double softClip(double sample) {
        return Math.max(-0.95, Math.min(0.95, sample));
    }

    private void releaseTrack() {
        AudioTrack track = audioTrack;
        audioTrack = null;
        if (track != null) {
            track.release();
        }
    }
}
