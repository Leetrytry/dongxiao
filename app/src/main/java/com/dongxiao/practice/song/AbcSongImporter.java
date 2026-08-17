package com.dongxiao.practice.song;

import com.dongxiao.practice.music.MusicTheory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AbcSongImporter {
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] JIANPU_BY_SEMITONE = {
            "1", "#1", "2", "b3", "3", "4", "#4", "5", "b6", "6", "b7", "7"
    };

    private AbcSongImporter() {
    }

    public static PracticeSong parse(String abcText) {
        if (abcText == null || abcText.trim().isEmpty()) {
            throw new IllegalArgumentException("曲谱内容为空。");
        }

        String title = "导入曲目";
        String key = "C";
        int tempoBpm = 90;
        int meterBeats = 4;
        double defaultBeatLength = 0.5;
        boolean inBody = false;
        StringBuilder bodyBuilder = new StringBuilder();

        String[] lines = abcText.replace("\r", "").split("\n");
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.length() > 2 && line.charAt(1) == ':') {
                char header = line.charAt(0);
                String value = line.substring(2).trim();
                switch (header) {
                    case 'T':
                        if (!value.isEmpty()) {
                            title = value;
                        }
                        break;
                    case 'M':
                        meterBeats = parseMeterBeats(value, meterBeats);
                        break;
                    case 'L':
                        defaultBeatLength = parseLengthAsBeats(value, defaultBeatLength);
                        break;
                    case 'Q':
                        tempoBpm = parseTempo(value, tempoBpm);
                        break;
                    case 'K':
                        key = parseKey(value);
                        inBody = true;
                        break;
                    default:
                        break;
                }
                continue;
            }

            if (inBody) {
                bodyBuilder.append(line).append(' ');
            }
        }

        if (bodyBuilder.length() == 0) {
            for (String rawLine : lines) {
                String line = stripComment(rawLine).trim();
                if (!line.isEmpty() && !(line.length() > 2 && line.charAt(1) == ':')) {
                    bodyBuilder.append(line).append(' ');
                }
            }
        }

        int rootMidi = rootMidiForKey(key);
        List<SongNote> notes = parseNotes(bodyBuilder.toString(), key, rootMidi, defaultBeatLength);
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("没有解析到可播放音符。请使用标准 ABC 文本链接。");
        }

        return new PracticeSong(
                title,
                key + "调",
                clamp(tempoBpm, 40, 180),
                clamp(meterBeats, 2, 9),
                rootMidi,
                notes
        );
    }

    private static List<SongNote> parseNotes(String body, String key, int rootMidi, double defaultBeatLength) {
        List<SongNote> notes = new ArrayList<>();
        int signature = keySignature(key);
        int index = 0;
        while (index < body.length()) {
            char c = body.charAt(index);
            if (Character.isWhitespace(c) || c == '|' || c == ':' || c == '-' || c == '!' || c == '"') {
                index++;
                continue;
            }
            if (c == '{') {
                index = skipUntil(body, index + 1, '}');
                continue;
            }
            if (c == '(') {
                index = skipTuplet(body, index + 1);
                continue;
            }
            if (c == '[') {
                ParsedNote chord = parseChord(body, index, key, rootMidi, signature, defaultBeatLength);
                if (chord != null) {
                    notes.add(chord.note);
                    index = chord.nextIndex;
                    continue;
                }
                index++;
                continue;
            }

            ParsedNote parsed = parseSingleNote(body, index, key, rootMidi, signature, defaultBeatLength);
            if (parsed != null) {
                notes.add(parsed.note);
                index = parsed.nextIndex;
            } else {
                index++;
            }
        }
        return notes;
    }

    private static ParsedNote parseChord(
            String body,
            int start,
            String key,
            int rootMidi,
            int signature,
            double defaultBeatLength
    ) {
        int close = body.indexOf(']', start + 1);
        if (close <= start) {
            return null;
        }
        String chordBody = body.substring(start + 1, close);
        ParsedNote first = parseSingleNote(chordBody, 0, key, rootMidi, signature, defaultBeatLength);
        Duration duration = parseDuration(body, close + 1, defaultBeatLength);
        if (first == null) {
            return null;
        }
        SongNote original = first.note;
        return new ParsedNote(
                new SongNote(original.label, original.midi, duration.beats, original.rest),
                duration.nextIndex
        );
    }

    private static ParsedNote parseSingleNote(
            String text,
            int start,
            String key,
            int rootMidi,
            int signature,
            double defaultBeatLength
    ) {
        int index = start;
        int accidental = 0;
        boolean explicitAccidental = false;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == '^') {
                accidental++;
                explicitAccidental = true;
            } else if (c == '_') {
                accidental--;
                explicitAccidental = true;
            } else if (c == '=') {
                accidental = 0;
                explicitAccidental = true;
            } else {
                break;
            }
            index++;
        }
        if (index >= text.length()) {
            return null;
        }

        char noteChar = text.charAt(index);
        boolean rest = noteChar == 'z' || noteChar == 'x';
        if (!rest && !isNoteLetter(noteChar)) {
            return null;
        }
        index++;

        int octave = Character.isLowerCase(noteChar) ? 5 : 4;
        if (!rest) {
            if (!explicitAccidental) {
                accidental = signatureAccidental(Character.toUpperCase(noteChar), signature);
            }
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c == '\'') {
                    octave++;
                } else if (c == ',') {
                    octave--;
                } else {
                    break;
                }
                index++;
            }
        }

        Duration duration = parseDuration(text, index, defaultBeatLength);
        if (rest) {
            return new ParsedNote(new SongNote("0", rootMidi, duration.beats, true), duration.nextIndex);
        }
        int midi = midiForAbcNote(Character.toUpperCase(noteChar), octave, accidental);
        return new ParsedNote(new SongNote(jianpuLabel(midi, rootMidi), midi, duration.beats), duration.nextIndex);
    }

    private static Duration parseDuration(String text, int start, double defaultBeatLength) {
        int index = start;
        StringBuilder numeratorBuilder = new StringBuilder();
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            numeratorBuilder.append(text.charAt(index));
            index++;
        }

        double multiplier = numeratorBuilder.length() == 0 ? 1.0 : parsePositiveDouble(numeratorBuilder.toString(), 1.0);
        if (index < text.length() && text.charAt(index) == '/') {
            index++;
            int slashCount = 1;
            while (index < text.length() && text.charAt(index) == '/') {
                slashCount++;
                index++;
            }
            StringBuilder denominatorBuilder = new StringBuilder();
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                denominatorBuilder.append(text.charAt(index));
                index++;
            }
            if (denominatorBuilder.length() > 0) {
                multiplier /= parsePositiveDouble(denominatorBuilder.toString(), 2.0);
            } else {
                multiplier /= Math.pow(2.0, slashCount);
            }
        }
        return new Duration(Math.max(0.125, defaultBeatLength * multiplier), index);
    }

    private static String jianpuLabel(int midi, int rootMidi) {
        int diff = Math.floorMod(midi - rootMidi, 12);
        if (diff >= 0 && diff < JIANPU_BY_SEMITONE.length) {
            return JIANPU_BY_SEMITONE[diff];
        }
        for (int i = 0; i < MAJOR_SCALE.length; i++) {
            if (diff == MAJOR_SCALE[i]) {
                return String.valueOf(i + 1);
            }
        }
        return "?";
    }

    private static int midiForAbcNote(char noteLetter, int octave, int accidental) {
        int pitchClass = naturalPitchClass(noteLetter) + accidental;
        int octaveAdjust = Math.floorDiv(pitchClass, 12);
        int normalizedPitchClass = Math.floorMod(pitchClass, 12);
        return (octave + octaveAdjust + 1) * 12 + normalizedPitchClass;
    }

    private static int rootMidiForKey(String key) {
        String root = normalizeKeyRoot(key);
        return MusicTheory.midiForNote(root, 4);
    }

    private static int keySignature(String key) {
        String normalized = key.trim().replace(" ", "");
        boolean minor = normalized.toLowerCase(Locale.US).endsWith("m")
                || normalized.toLowerCase(Locale.US).contains("min");
        String root = normalizeKeyRoot(normalized);
        if (minor) {
            switch (root) {
                case "A":
                    return 0;
                case "E":
                    return 1;
                case "B":
                    return 2;
                case "F#":
                    return 3;
                case "C#":
                    return 4;
                case "G#":
                    return 5;
                case "D#":
                    return 6;
                case "A#":
                    return 7;
                case "D":
                    return -1;
                case "G":
                    return -2;
                case "C":
                    return -3;
                case "F":
                    return -4;
                case "Bb":
                    return -5;
                case "Eb":
                    return -6;
                case "Ab":
                    return -7;
                default:
                    return 0;
            }
        }
        switch (root) {
            case "C":
                return 0;
            case "G":
                return 1;
            case "D":
                return 2;
            case "A":
                return 3;
            case "E":
                return 4;
            case "B":
                return 5;
            case "F#":
                return 6;
            case "C#":
                return 7;
            case "F":
                return -1;
            case "Bb":
                return -2;
            case "Eb":
                return -3;
            case "Ab":
                return -4;
            case "Db":
                return -5;
            case "Gb":
                return -6;
            case "Cb":
                return -7;
            default:
                return 0;
        }
    }

    private static int signatureAccidental(char noteLetter, int signature) {
        char[] sharps = {'F', 'C', 'G', 'D', 'A', 'E', 'B'};
        char[] flats = {'B', 'E', 'A', 'D', 'G', 'C', 'F'};
        if (signature > 0) {
            for (int i = 0; i < signature && i < sharps.length; i++) {
                if (noteLetter == sharps[i]) {
                    return 1;
                }
            }
        } else if (signature < 0) {
            for (int i = 0; i < -signature && i < flats.length; i++) {
                if (noteLetter == flats[i]) {
                    return -1;
                }
            }
        }
        return 0;
    }

    private static int naturalPitchClass(char noteLetter) {
        switch (noteLetter) {
            case 'C':
                return 0;
            case 'D':
                return 2;
            case 'E':
                return 4;
            case 'F':
                return 5;
            case 'G':
                return 7;
            case 'A':
                return 9;
            case 'B':
                return 11;
            default:
                return 0;
        }
    }

    private static String parseKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "C";
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        boolean minor = lower.endsWith("m") || lower.contains("min") || lower.contains("minor");
        return normalizeKeyRoot(trimmed) + (minor ? "m" : "");
    }

    private static String normalizeKeyRoot(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "C";
        }
        char root = Character.toUpperCase(trimmed.charAt(0));
        String suffix = "";
        if (trimmed.length() > 1) {
            char accidental = trimmed.charAt(1);
            if (accidental == '#') {
                suffix = "#";
            } else if (accidental == 'b') {
                suffix = "b";
            }
        }
        String note = root + suffix;
        try {
            MusicTheory.midiForNote(note, 4);
            return note;
        } catch (IllegalArgumentException ignored) {
            return "C";
        }
    }

    private static int parseMeterBeats(String value, int fallback) {
        if (value == null || value.trim().isEmpty() || "C".equals(value.trim())) {
            return fallback;
        }
        String[] parts = value.trim().split("/");
        if (parts.length == 2) {
            try {
                return Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double parseLengthAsBeats(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        String[] parts = value.trim().split("/");
        if (parts.length != 2) {
            return fallback;
        }
        try {
            double numerator = Double.parseDouble(parts[0].trim());
            double denominator = Double.parseDouble(parts[1].trim());
            if (numerator <= 0.0 || denominator <= 0.0) {
                return fallback;
            }
            return 4.0 * numerator / denominator;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseTempo(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = value.trim();
        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex >= 0 && equalsIndex < trimmed.length() - 1) {
            trimmed = trimmed.substring(equalsIndex + 1).trim();
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String stripComment(String line) {
        int index = line.indexOf('%');
        return index >= 0 ? line.substring(0, index) : line;
    }

    private static int skipUntil(String text, int index, char endChar) {
        while (index < text.length() && text.charAt(index) != endChar) {
            index++;
        }
        return Math.min(text.length(), index + 1);
    }

    private static int skipTuplet(String text, int index) {
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isNoteLetter(char c) {
        char upper = Character.toUpperCase(c);
        return upper >= 'A' && upper <= 'G';
    }

    private static double parsePositiveDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value);
            return parsed > 0.0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ParsedNote {
        final SongNote note;
        final int nextIndex;

        ParsedNote(SongNote note, int nextIndex) {
            this.note = note;
            this.nextIndex = nextIndex;
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
