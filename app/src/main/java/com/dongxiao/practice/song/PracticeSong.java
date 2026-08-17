package com.dongxiao.practice.song;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PracticeSong {
    public final String title;
    public final String keyLabel;
    public final int tempoBpm;
    public final int meterBeats;
    public final int rootMidi;
    public final List<SongNote> notes;

    public PracticeSong(
            String title,
            String keyLabel,
            int tempoBpm,
            int meterBeats,
            int rootMidi,
            List<SongNote> notes
    ) {
        this.title = title;
        this.keyLabel = keyLabel;
        this.tempoBpm = tempoBpm;
        this.meterBeats = meterBeats;
        this.rootMidi = rootMidi;
        this.notes = Collections.unmodifiableList(notes);
    }

    public double totalBeats() {
        double total = 0.0;
        for (SongNote note : notes) {
            total += note.beats;
        }
        return total;
    }

    public int noteIndexAtBeat(double beatPosition) {
        double cursor = 0.0;
        for (int i = 0; i < notes.size(); i++) {
            cursor += notes.get(i).beats;
            if (beatPosition < cursor) {
                return i;
            }
        }
        return Math.max(0, notes.size() - 1);
    }

    public double beatStartForIndex(int noteIndex) {
        double cursor = 0.0;
        for (int i = 0; i < noteIndex && i < notes.size(); i++) {
            cursor += notes.get(i).beats;
        }
        return cursor;
    }

    public String metaText() {
        return String.format(
                Locale.CHINA,
                "%s · %d/%d · %d BPM · %.0f 拍",
                keyLabel,
                meterBeats,
                4,
                tempoBpm,
                totalBeats()
        );
    }

    @Override
    public String toString() {
        return title;
    }
}
