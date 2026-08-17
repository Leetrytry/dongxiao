package com.dongxiao.practice.song;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class LocalSongStore {
    private static final String PREFS_NAME = "dongxiao_local_songs";
    private static final String KEY_SONGS = "songs";

    private LocalSongStore() {
    }

    public static List<PracticeSong> load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = preferences.getString(KEY_SONGS, "[]");
        List<PracticeSong> songs = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                songs.add(fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return songs;
    }

    public static void save(Context context, List<PracticeSong> songs) {
        JSONArray array = new JSONArray();
        for (PracticeSong song : songs) {
            array.put(toJson(song));
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SONGS, array.toString())
                .apply();
    }

    private static JSONObject toJson(PracticeSong song) {
        JSONObject object = new JSONObject();
        JSONArray notes = new JSONArray();
        try {
            object.put("title", song.title);
            object.put("keyLabel", song.keyLabel);
            object.put("tempoBpm", song.tempoBpm);
            object.put("meterBeats", song.meterBeats);
            object.put("rootMidi", song.rootMidi);
            for (SongNote note : song.notes) {
                JSONObject noteObject = new JSONObject();
                noteObject.put("label", note.label);
                noteObject.put("midi", note.midi);
                noteObject.put("beats", note.beats);
                noteObject.put("rest", note.rest);
                notes.put(noteObject);
            }
            object.put("notes", notes);
        } catch (JSONException ignored) {
            // JSONObject only rejects unsupported values, which we do not use here.
        }
        return object;
    }

    private static PracticeSong fromJson(JSONObject object) throws JSONException {
        JSONArray noteArray = object.getJSONArray("notes");
        List<SongNote> notes = new ArrayList<>();
        for (int i = 0; i < noteArray.length(); i++) {
            JSONObject noteObject = noteArray.getJSONObject(i);
            notes.add(new SongNote(
                    noteObject.getString("label"),
                    noteObject.getInt("midi"),
                    noteObject.getDouble("beats"),
                    noteObject.optBoolean("rest", false)
            ));
        }
        return new PracticeSong(
                object.getString("title"),
                object.getString("keyLabel"),
                object.getInt("tempoBpm"),
                object.getInt("meterBeats"),
                object.getInt("rootMidi"),
                notes
        );
    }
}
