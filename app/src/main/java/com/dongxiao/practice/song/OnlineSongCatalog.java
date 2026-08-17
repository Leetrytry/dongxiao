package com.dongxiao.practice.song;

import java.util.Arrays;
import java.util.List;

public final class OnlineSongCatalog {
    private OnlineSongCatalog() {
    }

    public static List<OnlineSongResource> defaults() {
        return Arrays.asList(
                abc(
                        "The Banshee",
                        "Tunebook ABC",
                        "CC BY-SA",
                        "https://raw.githubusercontent.com/jw35/tunebook-abc/master/abc2/0401_the-banshee.abc"
                ),
                abc(
                        "The Merry Blacksmith",
                        "Tunebook ABC",
                        "CC BY-SA",
                        "https://raw.githubusercontent.com/jw35/tunebook-abc/master/abc2/0402_the-merry-blacksmith.abc"
                ),
                abc(
                        "Morrison's",
                        "Tunebook ABC",
                        "CC BY-SA",
                        "https://raw.githubusercontent.com/jw35/tunebook-abc/master/abc2/0802_morrisons.abc"
                ),
                abc(
                        "Father O'Flynn",
                        "Tunebook ABC",
                        "CC BY-SA",
                        "https://raw.githubusercontent.com/jw35/tunebook-abc/master/abc2/0801_father-oflynn.abc"
                ),
                abc(
                        "My Darling Asleep",
                        "Tunebook ABC",
                        "CC BY-SA",
                        "https://raw.githubusercontent.com/jw35/tunebook-abc/master/abc2/0901_my-darling-asleep.abc"
                ),
                abc(
                        "Rattling Bog",
                        "Tunebook ABC",
                        "CC BY-SA",
                        "https://raw.githubusercontent.com/jw35/tunebook-abc/master/abc2/0201_rattling-bog.abc"
                ),
                new OnlineSongResource(
                        "abcnotation.com 曲库",
                        "abcnotation.com",
                        "ABC 索引",
                        "逐曲确认",
                        "https://abcnotation.com/search",
                        false
                ),
                new OnlineSongResource(
                        "OpenScore",
                        "OpenScore",
                        "MusicXML / MuseScore",
                        "CC0-1.0",
                        "https://github.com/OpenScore",
                        false
                ),
                new OnlineSongResource(
                        "中国民歌集成数据集",
                        "Anthology of Chinese Folk Songs",
                        "MusicXML / MIDI",
                        "需逐项核验",
                        "https://github.com/m-july/Anthology-of-Chinese-Folk-Songs",
                        false
                )
        );
    }

    private static OnlineSongResource abc(String title, String source, String license, String url) {
        return new OnlineSongResource(title, source, "ABC", license, url, true);
    }
}
