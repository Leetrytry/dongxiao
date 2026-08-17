package com.dongxiao.practice.song;

import com.dongxiao.practice.music.MusicTheory;

import java.util.ArrayList;
import java.util.List;

public final class JianpuTextSongImporter {
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    private static final int MAX_NOTES = 420;

    private JianpuTextSongImporter() {
    }

    public static PracticeSong parse(JianpuTextSource source, String text) {
        int rootMidi = MusicTheory.midiForNote(source.key, 4);
        List<SongNote> notes = new ArrayList<>();
        String[] lines = text.replace("\r", "").split("\n");
        for (String rawLine : lines) {
            if (notes.size() >= MAX_NOTES) {
                break;
            }
            String line = rawLine.trim();
            if (!isLikelyScoreLine(line)) {
                continue;
            }
            parseLine(line, rootMidi, notes);
        }
        if (notes.isEmpty()) {
            notes.add(new SongNote("0", rootMidi, 4.0, true));
        }
        return new PracticeSong(
                "图片转谱 · " + source.title,
                source.key + "调",
                clamp(source.tempoBpm, 40, 180),
                clamp(source.meterBeats, 2, 9),
                rootMidi,
                notes
        );
    }

    private static void parseLine(String line, int rootMidi, List<SongNote> notes) {
        int accidental = 0;
        boolean hasAccidental = false;
        int index = 0;
        while (index < line.length() && notes.size() < MAX_NOTES) {
            char c = line.charAt(index);
            if (isSharp(c)) {
                accidental = 1;
                hasAccidental = true;
                index++;
                continue;
            }
            if (isFlat(c)) {
                accidental = -1;
                hasAccidental = true;
                index++;
                continue;
            }

            NoteToken token = noteToken(c, line, index);
            if (token == null) {
                index++;
                continue;
            }

            Duration duration = durationAfter(line, index + 1);
            String label = token.degree == 0
                    ? "0"
                    : (hasAccidental ? accidentalLabel(accidental) : "") + token.degree;
            int midi = token.degree == 0
                    ? rootMidi
                    : rootMidi + MAJOR_SCALE[token.degree - 1] + token.octaveOffset * 12 + accidental;
            notes.add(new SongNote(label, midi, duration.beats, token.degree == 0));

            accidental = 0;
            hasAccidental = false;
            index = duration.nextIndex;
        }
    }

    private static Duration durationAfter(String line, int start) {
        int index = start;
        int extensions = 0;
        int underlines = 0;
        boolean dotted = false;
        while (index < line.length()) {
            char c = line.charAt(index);
            if (isExtension(c)) {
                extensions++;
                index++;
            } else if (c == '/' || c == '_' || c == '=') {
                underlines++;
                index++;
            } else if (c == '.' || c == '·' || c == '•') {
                dotted = true;
                index++;
            } else if (Character.isWhitespace(c)) {
                index++;
            } else {
                break;
            }
        }
        double beats = 1.0 + extensions;
        if (underlines > 0) {
            beats = beats / Math.pow(2.0, Math.min(3, underlines));
        }
        if (dotted && beats <= 2.0) {
            beats *= 1.5;
        }
        return new Duration(Math.max(0.125, Math.min(8.0, beats)), index);
    }

    private static boolean isLikelyScoreLine(String line) {
        if (line.isEmpty()) {
            return false;
        }
        String lower = line.toLowerCase();
        String[] skipWords = {
                "作曲", "作词", "演唱", "歌手", "记谱", "制谱", "编辑", "上传", "欢迎",
                "http", "www", "qq", "小红书", "公众号", "appstore", "注：", "建议",
                "曲谱", "简谱", "专辑", "电视剧", "电影", "演奏：", "源自"
        };
        for (String skipWord : skipWords) {
            if (lower.contains(skipWord.toLowerCase())) {
                return false;
            }
        }
        if ((line.contains("1=") || lower.contains("j=") || lower.contains("q="))
                && countNoteChars(line) <= 4) {
            return false;
        }
        int notes = countNoteChars(line);
        if (notes < 3) {
            return countCjkChars(line) == 0 && notes > 0;
        }
        int cjk = countCjkChars(line);
        boolean hasMusicSeparator = line.indexOf('|') >= 0
                || line.indexOf('-') >= 0
                || line.indexOf('—') >= 0
                || line.indexOf(':') >= 0
                || line.indexOf('：') >= 0
                || line.indexOf('(') >= 0
                || line.indexOf('（') >= 0;
        if (!hasMusicSeparator && notes < 8) {
            return false;
        }
        return cjk <= notes || notes >= 8;
    }

    private static int countNoteChars(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (noteToken(line.charAt(i), line, i) != null) {
                count++;
            }
        }
        return count;
    }

    private static int countCjkChars(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(line.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                count++;
            }
        }
        return count;
    }

    private static NoteToken noteToken(char c, String line, int index) {
        switch (c) {
            case '0':
            case 'O':
            case 'o':
                return new NoteToken(0, 0);
            case '1':
            case '①':
            case '❶':
                return new NoteToken(1, 0);
            case '2':
            case '②':
            case '❷':
                return new NoteToken(2, 0);
            case '3':
            case '③':
            case '❸':
            case '£':
                return new NoteToken(3, 0);
            case '4':
            case '④':
            case '❹':
                return new NoteToken(4, 0);
            case '5':
            case '⑤':
            case '❺':
                return new NoteToken(5, 0);
            case '6':
            case '⑥':
            case '❻':
            case '€':
                return new NoteToken(6, 0);
            case '7':
            case '⑦':
            case '❼':
                return new NoteToken(7, 0);
            case '8':
            case '⑧':
                return new NoteToken(1, 1);
            case '9':
            case '⑨':
                return new NoteToken(2, 1);
            case 'i':
            case 'I':
            case 'l':
                return isNoteLikeLetter(line, index) ? new NoteToken(1, 1) : null;
            case 'z':
            case 'Z':
                return isNoteLikeLetter(line, index) ? new NoteToken(2, 1) : null;
            default:
                return null;
        }
    }

    private static boolean isNoteLikeLetter(String line, int index) {
        char before = index > 0 ? line.charAt(index - 1) : ' ';
        char after = index + 1 < line.length() ? line.charAt(index + 1) : ' ';
        return isNumericNoteNeighbor(before) || isNumericNoteNeighbor(after);
    }

    private static boolean isNumericNoteNeighbor(char c) {
        return (c >= '0' && c <= '9')
                || c == '|' || c == '-' || c == '—' || c == ':' || c == '：'
                || c == '(' || c == ')' || c == '（' || c == '）'
                || Character.isWhitespace(c);
    }

    private static boolean isSharp(char c) {
        return c == '#' || c == '♯';
    }

    private static boolean isFlat(char c) {
        return c == 'b' || c == '♭';
    }

    private static boolean isExtension(char c) {
        return c == '-' || c == '—' || c == '一' || c == '–';
    }

    private static String accidentalLabel(int accidental) {
        if (accidental > 0) {
            return "#";
        }
        if (accidental < 0) {
            return "b";
        }
        return "";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class NoteToken {
        final int degree;
        final int octaveOffset;

        NoteToken(int degree, int octaveOffset) {
            this.degree = degree;
            this.octaveOffset = octaveOffset;
        }
    }

    private static final class Duration {
        final double beats;
        final int nextIndex;

        Duration(double beats, int nextIndex) {
            this.beats = beats;
            this.nextIndex = nextIndex;
        }
    }
}
