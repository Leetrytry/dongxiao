package com.dongxiao.practice.song;

import com.dongxiao.practice.music.MusicTheory;

import java.util.Arrays;
import java.util.List;

public final class SongRepository {
    private SongRepository() {
    }

    public static List<PracticeSong> defaults() {
        return Arrays.asList(
                new PracticeSong(
                        "小星星片段",
                        "F调",
                        90,
                        4,
                        MusicTheory.midiForNote("F", 4),
                        Arrays.asList(
                                n("1", "F", 4, 1.0),
                                n("1", "F", 4, 1.0),
                                n("5", "C", 5, 1.0),
                                n("5", "C", 5, 1.0),
                                n("6", "D", 5, 1.0),
                                n("6", "D", 5, 1.0),
                                n("5", "C", 5, 2.0),
                                n("4", "Bb", 4, 1.0),
                                n("4", "Bb", 4, 1.0),
                                n("3", "A", 4, 1.0),
                                n("3", "A", 4, 1.0),
                                n("2", "G", 4, 1.0),
                                n("2", "G", 4, 1.0),
                                n("1", "F", 4, 2.0)
                        )
                ),
                new PracticeSong(
                        "五声音阶行进",
                        "G调",
                        76,
                        4,
                        MusicTheory.midiForNote("G", 4),
                        Arrays.asList(
                                n("5", "D", 4, 1.0),
                                n("6", "E", 4, 1.0),
                                n("1", "G", 4, 1.0),
                                n("2", "A", 4, 1.0),
                                n("3", "B", 4, 1.0),
                                n("5", "D", 5, 1.0),
                                n("3", "B", 4, 1.0),
                                n("2", "A", 4, 1.0),
                                n("1", "G", 4, 2.0),
                                n("6", "E", 4, 1.0),
                                n("5", "D", 4, 1.0),
                                n("1", "G", 4, 2.0)
                        )
                ),
                new PracticeSong(
                        "慢速换气练习",
                        "E调",
                        60,
                        4,
                        MusicTheory.midiForNote("E", 4),
                        Arrays.asList(
                                n("1", "E", 4, 2.0),
                                n("2", "F#", 4, 2.0),
                                n("3", "G#", 4, 2.0),
                                n("5", "B", 4, 2.0),
                                n("6", "C#", 5, 2.0),
                                n("5", "B", 4, 2.0),
                                n("3", "G#", 4, 2.0),
                                n("1", "E", 4, 2.0)
                        )
                )
        );
    }

    private static SongNote n(String label, String noteName, int octave, double beats) {
        return new SongNote(label, MusicTheory.midiForNote(noteName, octave), beats);
    }
}
