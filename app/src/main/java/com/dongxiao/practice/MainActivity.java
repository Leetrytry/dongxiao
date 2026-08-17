package com.dongxiao.practice;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.dongxiao.practice.audio.AudioAnalyzer;
import com.dongxiao.practice.audio.PitchResult;
import com.dongxiao.practice.music.FingeringMode;
import com.dongxiao.practice.music.MusicTheory;
import com.dongxiao.practice.music.TargetNote;
import com.dongxiao.practice.music.XiaoTuning;
import com.dongxiao.practice.practice.PracticeAnalyzer;
import com.dongxiao.practice.practice.PracticeMode;
import com.dongxiao.practice.practice.PracticeScore;
import com.dongxiao.practice.practice.PracticeSessionScorer;
import com.dongxiao.practice.practice.PracticeStats;
import com.dongxiao.practice.song.ImageScore;
import com.dongxiao.practice.song.ImageScoreMarker;
import com.dongxiao.practice.song.ImageScoreRepository;
import com.dongxiao.practice.song.JianpuTextCatalog;
import com.dongxiao.practice.song.JianpuTextSongImporter;
import com.dongxiao.practice.song.JianpuTextSource;
import com.dongxiao.practice.song.PracticeSong;
import com.dongxiao.practice.song.SongPlayer;
import com.dongxiao.practice.ui.DynamicScoreView;
import com.dongxiao.practice.ui.TunerView;
import com.dongxiao.practice.ui.WaveformView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_RECORD_AUDIO = 1001;

    private TextView statusText;
    private TextView instructionText;
    private TextView pitchText;
    private TextView detailText;
    private TextView metricText;
    private TextView scoreText;
    private TextView modeTitleText;
    private TextView practiceSummaryText;
    private TextView songTitleText;
    private TextView songMetaText;
    private TextView songStatusText;
    private TextView imageScorePageText;
    private LinearLayout homeContainer;
    private LinearLayout practiceContainer;
    private LinearLayout songContainer;
    private LinearLayout modeList;
    private LinearLayout scorePanel;
    private Spinner tuningSpinner;
    private Spinner fingeringSpinner;
    private Spinner targetSpinner;
    private Spinner songSpinner;
    private Spinner imageScoreSpinner;
    private CheckBox autoTargetCheck;
    private Button backButton;
    private Button startButton;
    private Button songBackButton;
    private Button songPlayButton;
    private Button imageScorePrevButton;
    private Button imageScoreNextButton;
    private ImageView imageScoreView;
    private TunerView tunerView;
    private WaveformView waveformView;
    private DynamicScoreView dynamicScoreView;

    private final PracticeAnalyzer practiceAnalyzer = new PracticeAnalyzer();
    private final PracticeSessionScorer sessionScorer = new PracticeSessionScorer();
    private final List<TargetNote> targets = new ArrayList<>();
    private final List<PracticeSong> songs = new ArrayList<>();
    private final List<ImageScore> imageScores = ImageScoreRepository.defaults();
    private AudioAnalyzer audioAnalyzer;
    private SongPlayer songPlayer;
    private ArrayAdapter<PracticeSong> songAdapter;
    private ArrayAdapter<ImageScore> imageScoreAdapter;
    private PracticeMode currentPracticeMode;
    private boolean sessionHasFrames = false;
    private int imageScorePageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configureSystemBars();
        bindViews();
        setupSpinners();
        setupPracticeModes();
        setupStartButton();
        setupSongPractice();
        showHome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening();
        stopSongPlayback(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                startListening();
            } else {
                statusText.setText("没有录音权限，无法进行音准监测。");
            }
        }
    }

    private void bindViews() {
        statusText = findViewById(R.id.statusText);
        instructionText = findViewById(R.id.instructionText);
        pitchText = findViewById(R.id.pitchText);
        detailText = findViewById(R.id.detailText);
        metricText = findViewById(R.id.metricText);
        scoreText = findViewById(R.id.scoreText);
        modeTitleText = findViewById(R.id.modeTitleText);
        practiceSummaryText = findViewById(R.id.practiceSummaryText);
        songTitleText = findViewById(R.id.songTitleText);
        songMetaText = findViewById(R.id.songMetaText);
        songStatusText = findViewById(R.id.songStatusText);
        imageScorePageText = findViewById(R.id.imageScorePageText);
        homeContainer = findViewById(R.id.homeContainer);
        practiceContainer = findViewById(R.id.practiceContainer);
        songContainer = findViewById(R.id.songContainer);
        modeList = findViewById(R.id.modeList);
        scorePanel = findViewById(R.id.scorePanel);
        tuningSpinner = findViewById(R.id.tuningSpinner);
        fingeringSpinner = findViewById(R.id.fingeringSpinner);
        targetSpinner = findViewById(R.id.targetSpinner);
        songSpinner = findViewById(R.id.songSpinner);
        imageScoreSpinner = findViewById(R.id.imageScoreSpinner);
        autoTargetCheck = findViewById(R.id.autoTargetCheck);
        backButton = findViewById(R.id.backButton);
        startButton = findViewById(R.id.startButton);
        songBackButton = findViewById(R.id.songBackButton);
        songPlayButton = findViewById(R.id.songPlayButton);
        imageScorePrevButton = findViewById(R.id.imageScorePrevButton);
        imageScoreNextButton = findViewById(R.id.imageScoreNextButton);
        imageScoreView = findViewById(R.id.imageScoreView);
        tunerView = findViewById(R.id.tunerView);
        waveformView = findViewById(R.id.waveformView);
        dynamicScoreView = findViewById(R.id.dynamicScoreView);
    }

    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getColorCompat(R.color.background));
            getWindow().setNavigationBarColor(getColorCompat(R.color.background));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        View rootView = findViewById(R.id.rootScrollView);
        int originalLeft = rootView.getPaddingLeft();
        int originalTop = rootView.getPaddingTop();
        int originalRight = rootView.getPaddingRight();
        int originalBottom = rootView.getPaddingBottom();
        rootView.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    originalLeft + insets.getSystemWindowInsetLeft(),
                    originalTop + insets.getSystemWindowInsetTop(),
                    originalRight + insets.getSystemWindowInsetRight(),
                    originalBottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        rootView.requestApplyInsets();
    }

    private void setupSpinners() {
        List<XiaoTuning> tunings = XiaoTuning.defaults();
        ArrayAdapter<XiaoTuning> tuningAdapter = createAdapter(tunings);
        tuningSpinner.setAdapter(tuningAdapter);
        tuningSpinner.setSelection(1);

        ArrayAdapter<FingeringMode> fingeringAdapter = createAdapter(FingeringMode.values());
        fingeringSpinner.setAdapter(fingeringAdapter);

        AdapterView.OnItemSelectedListener targetRefreshingListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTargets();
                practiceAnalyzer.reset();
                sessionScorer.reset();
                sessionHasFrames = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        tuningSpinner.setOnItemSelectedListener(targetRefreshingListener);
        fingeringSpinner.setOnItemSelectedListener(targetRefreshingListener);
        targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                practiceAnalyzer.reset();
                sessionScorer.reset();
                sessionHasFrames = false;
                updatePracticeSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        autoTargetCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updatePracticeSummary());

        updateTargets();
    }

    private void setupPracticeModes() {
        modeList.removeAllViews();
        for (PracticeMode mode : PracticeMode.values()) {
            modeList.addView(createPracticeCard(mode));
        }
        modeList.addView(createSongPracticeCard());
        backButton.setOnClickListener(view -> showHome());
    }

    private View createPracticeCard(PracticeMode mode) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_practice_card);
        card.setClickable(true);
        card.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(mode.label);
        title.setTextColor(getColorCompat(R.color.ink));
        title.setTextSize(18.0f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView seal = new TextView(this);
        seal.setText("练");
        seal.setGravity(Gravity.CENTER);
        seal.setTextColor(getColorCompat(R.color.paper));
        seal.setTextSize(13.0f);
        seal.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        seal.setBackgroundResource(R.drawable.bg_seal);

        titleRow.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        titleRow.addView(seal, new LinearLayout.LayoutParams(
                dp(28),
                dp(28)
        ));
        card.addView(titleRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView description = new TextView(this);
        description.setText(mode.instruction);
        description.setTextColor(getColorCompat(R.color.muted));
        description.setTextSize(13.0f);
        description.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(6);
        card.addView(description, descriptionParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setOnClickListener(view -> enterPractice(mode));
        return card;
    }

    private View createSongPracticeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_practice_card);
        card.setClickable(true);
        card.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("曲目练习");
        title.setTextColor(getColorCompat(R.color.ink));
        title.setTextSize(18.0f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView seal = new TextView(this);
        seal.setText("曲");
        seal.setGravity(Gravity.CENTER);
        seal.setTextColor(getColorCompat(R.color.paper));
        seal.setTextSize(13.0f);
        seal.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        seal.setBackgroundResource(R.drawable.bg_seal);

        titleRow.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));
        titleRow.addView(seal, new LinearLayout.LayoutParams(
                dp(28),
                dp(28)
        ));
        card.addView(titleRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView description = new TextView(this);
        description.setText("使用默认图片谱曲库，播放智能伴奏并在原图上高亮当前音符。");
        description.setTextColor(getColorCompat(R.color.muted));
        description.setTextSize(13.0f);
        description.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(6);
        card.addView(description, descriptionParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setOnClickListener(view -> enterSongPractice());
        return card;
    }

    private void setupStartButton() {
        startButton.setOnClickListener(view -> {
            if (audioAnalyzer != null && audioAnalyzer.isRunning()) {
                stopListening();
            } else {
                ensurePermissionAndStart();
            }
        });
    }

    private void setupSongPractice() {
        songPlayer = new SongPlayer(new SongPlayer.Listener() {
            @Override
            public void onProgress(double beatPosition, int noteIndex) {
                runOnUiThread(() -> dynamicScoreView.setProgress(beatPosition, noteIndex));
            }

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    songPlayButton.setText("播放伴奏");
                    songStatusText.setText("伴奏播放完成。可以重新播放或选择其他曲目。");
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    songPlayButton.setText("播放伴奏");
                    songStatusText.setText(message);
                });
            }
        });

        songs.addAll(loadBundledJianpuSongs());

        songAdapter = createAdapter(songs);
        songSpinner.setAdapter(songAdapter);
        songSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stopSongPlayback(true);
                updateSelectedSong();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        imageScoreAdapter = createAdapter(imageScores);
        imageScoreSpinner.setAdapter(imageScoreAdapter);
        imageScoreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                imageScorePageIndex = 0;
                updateSelectedImageScore(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        songBackButton.setOnClickListener(view -> showHome());
        songPlayButton.setOnClickListener(view -> toggleSongPlayback());
        imageScorePrevButton.setOnClickListener(view -> moveImageScorePage(-1));
        imageScoreNextButton.setOnClickListener(view -> moveImageScorePage(1));
        updateSelectedImageScore(true);
        updateSelectedSong();
    }

    private void resetScrollToTop() {
        View rootView = findViewById(R.id.rootScrollView);
        rootView.post(() -> rootView.scrollTo(0, 0));
    }

    private void showHome() {
        stopListening();
        stopSongPlayback(true);
        currentPracticeMode = null;
        homeContainer.setVisibility(View.VISIBLE);
        practiceContainer.setVisibility(View.GONE);
        songContainer.setVisibility(View.GONE);
        resetScrollToTop();
    }

    private void enterPractice(PracticeMode mode) {
        stopSongPlayback(true);
        currentPracticeMode = mode;
        homeContainer.setVisibility(View.GONE);
        practiceContainer.setVisibility(View.VISIBLE);
        songContainer.setVisibility(View.GONE);
        practiceAnalyzer.reset();
        updateInstruction();
        updateTargets();
        pitchText.setText("未检测到稳定音高");
        detailText.setText("Hz、音名、cent 偏差和稳定度会显示在这里。");
        metricText.setText("练习指标等待开始。");
        scorePanel.setVisibility(View.GONE);
        scoreText.setText("");
        updatePracticeSummary();
        resetScrollToTop();
        waveformView.clear();
    }

    private void enterSongPractice() {
        stopListening();
        currentPracticeMode = null;
        homeContainer.setVisibility(View.GONE);
        practiceContainer.setVisibility(View.GONE);
        songContainer.setVisibility(View.VISIBLE);
        updateSelectedSong();
        resetScrollToTop();
    }

    private void updateSelectedSong() {
        PracticeSong song = selectedSong();
        if (song == null) {
            return;
        }
        songTitleText.setText(song.title);
        songMetaText.setText(song.metaText());
        if (song.title.startsWith("图片转谱 · ")) {
            songStatusText.setText("该曲使用谱面驱动的智能背景伴奏，播放时会在原图片谱上动态高亮；建议对照原图校正细节。");
        } else {
            songStatusText.setText("智能伴奏会按谱面节奏播放，当前音符会同步高亮。");
        }
        dynamicScoreView.setSong(song);
        configureImageDynamicScore(song);
        songPlayButton.setText("播放伴奏");
    }

    private void configureImageDynamicScore(PracticeSong song) {
        ImageScore imageScore = imageScoreForConvertedSong(song);
        if (imageScore == null) {
            return;
        }
        try {
            dynamicScoreView.setImageScore(loadImageScoreBitmaps(imageScore), loadImageScoreMarkers(imageScore));
        } catch (IOException | IllegalArgumentException error) {
            songStatusText.setText("原图动态高亮加载失败，已回退为简谱显示。");
        }
    }

    private ImageScore imageScoreForConvertedSong(PracticeSong song) {
        String prefix = "图片转谱 · ";
        if (song == null || !song.title.startsWith(prefix)) {
            return null;
        }
        String imageScoreTitle = song.title.substring(prefix.length());
        for (ImageScore imageScore : imageScores) {
            if (imageScore.title.equals(imageScoreTitle)) {
                return imageScore;
            }
        }
        return null;
    }

    private List<Bitmap> loadImageScoreBitmaps(ImageScore imageScore) throws IOException {
        List<Bitmap> bitmaps = new ArrayList<>();
        for (String assetPath : imageScore.assetPaths) {
            try (InputStream inputStream = getAssets().open(assetPath)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                }
            }
        }
        if (bitmaps.isEmpty()) {
            throw new IOException("No image pages");
        }
        return bitmaps;
    }

    private List<ImageScoreMarker> loadImageScoreMarkers(ImageScore imageScore) throws IOException {
        List<ImageScoreMarker> markers = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < imageScore.assetPaths.size(); pageIndex++) {
            String layoutText = readAssetText(layoutAssetPath(imageScore.assetPaths.get(pageIndex)));
            String[] lines = layoutText.split("\\n");
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length < 4) {
                    continue;
                }
                markers.add(new ImageScoreMarker(
                        pageIndex,
                        Float.parseFloat(parts[0]),
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])
                ));
            }
        }
        if (markers.isEmpty()) {
            throw new IOException("No image score markers");
        }
        return markers;
    }

    private String layoutAssetPath(String imageAssetPath) {
        int slash = imageAssetPath.lastIndexOf('/');
        int dot = imageAssetPath.lastIndexOf('.');
        String fileName = imageAssetPath.substring(slash + 1, dot > slash ? dot : imageAssetPath.length());
        return "score_layout/" + fileName + ".txt";
    }

    private List<PracticeSong> loadBundledJianpuSongs() {
        List<PracticeSong> convertedSongs = new ArrayList<>();
        for (JianpuTextSource source : JianpuTextCatalog.defaults()) {
            try {
                StringBuilder builder = new StringBuilder();
                for (String assetPath : source.assetPaths) {
                    builder.append(readAssetText(assetPath)).append('\n');
                }
                convertedSongs.add(JianpuTextSongImporter.parse(source, builder.toString()));
            } catch (IOException | IllegalArgumentException ignored) {
                // A single OCR source should not prevent the rest of the bundled score library from loading.
            }
        }
        return convertedSongs;
    }

    private void toggleSongPlayback() {
        if (songPlayer != null && songPlayer.isRunning()) {
            stopSongPlayback(false);
            songStatusText.setText("伴奏已停止。");
            return;
        }
        PracticeSong song = selectedSong();
        if (song == null) {
            songStatusText.setText("请先选择曲目。");
            return;
        }
        dynamicScoreView.setProgress(0.0, -1);
        songPlayButton.setText("停止伴奏");
        songStatusText.setText("智能伴奏播放中，请跟随高亮音符练习。");
        songPlayer.start(song);
    }

    private void updateSelectedImageScore(boolean syncSongSelection) {
        ImageScore imageScore = selectedImageScore();
        if (imageScore == null) {
            imageScoreView.setImageDrawable(null);
            imageScorePageText.setText("暂无图片谱");
            imageScorePrevButton.setEnabled(false);
            imageScoreNextButton.setEnabled(false);
            return;
        }
        imageScorePageIndex = Math.max(0, Math.min(imageScorePageIndex, imageScore.pageCount() - 1));
        imageScorePageText.setText(String.format(
                Locale.CHINA,
                "第 %d / %d 页",
                imageScorePageIndex + 1,
                imageScore.pageCount()
        ));
        imageScorePrevButton.setEnabled(imageScorePageIndex > 0);
        imageScoreNextButton.setEnabled(imageScorePageIndex < imageScore.pageCount() - 1);
        try (InputStream inputStream = getAssets().open(imageScore.assetPath(imageScorePageIndex))) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageScoreView.setImageBitmap(bitmap);
        } catch (IOException error) {
            imageScoreView.setImageDrawable(null);
            imageScorePageText.setText("图片谱加载失败");
        }
        if (syncSongSelection) {
            selectConvertedSongForImageScore(imageScore);
        }
    }

    private void selectConvertedSongForImageScore(ImageScore imageScore) {
        String convertedTitle = "图片转谱 · " + imageScore.title;
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).title.equals(convertedTitle)) {
                if (songSpinner.getSelectedItemPosition() != i) {
                    songSpinner.setSelection(i);
                }
                return;
            }
        }
    }

    private void moveImageScorePage(int delta) {
        ImageScore imageScore = selectedImageScore();
        if (imageScore == null) {
            return;
        }
        imageScorePageIndex = Math.max(0, Math.min(imageScorePageIndex + delta, imageScore.pageCount() - 1));
        updateSelectedImageScore(false);
    }

    private String readAssetText(String assetPath) throws IOException {
        try (InputStream inputStream = getAssets().open(assetPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
    }

    private void stopSongPlayback(boolean resetProgress) {
        if (songPlayer != null && songPlayer.isRunning()) {
            songPlayer.stop();
        }
        if (songPlayButton != null) {
            songPlayButton.setText("播放伴奏");
        }
        if (resetProgress && dynamicScoreView != null) {
            PracticeSong song = selectedSong();
            if (song != null) {
                dynamicScoreView.setSong(song);
            }
        }
    }

    private <T> ArrayAdapter<T> createAdapter(T[] items) {
        ArrayAdapter<T> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private <T> ArrayAdapter<T> createAdapter(List<T> items) {
        ArrayAdapter<T> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int getColorCompat(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorResId);
        }
        return getResources().getColor(colorResId);
    }

    private void updateTargets() {
        XiaoTuning tuning = selectedTuning();
        FingeringMode fingeringMode = selectedFingeringMode();
        if (tuning == null || fingeringMode == null) {
            return;
        }

        targets.clear();
        targets.addAll(tuning.createTargets(fingeringMode));

        ArrayAdapter<TargetNote> targetAdapter = createAdapter(new ArrayList<>(targets));
        targetSpinner.setAdapter(targetAdapter);
        if (!targets.isEmpty()) {
            targetSpinner.setSelection(0);
            TargetNote target = targets.get(targetSpinner.getSelectedItemPosition());
            tunerView.setReading(0.0, false, "目标 " + target.label);
            if (statusText != null) {
                statusText.setText(tuning.referenceText(fingeringMode));
            }
            updatePracticeSummary();
        }
    }

    private void updatePracticeSummary() {
        if (practiceSummaryText == null) {
            return;
        }
        XiaoTuning tuning = selectedTuning();
        FingeringMode fingeringMode = selectedFingeringMode();
        TargetNote target = selectedTarget();
        String tuningLabel = tuning == null ? "未选调门" : tuning.label;
        String fingeringLabel = fingeringMode == null ? "未选筒音" : fingeringMode.label;
        String targetLabel;
        if (autoTargetCheck != null && autoTargetCheck.isChecked()) {
            targetLabel = "自动匹配目标音";
        } else {
            targetLabel = target == null ? "未选目标音" : target.label;
        }
        practiceSummaryText.setText(tuningLabel + " · " + fingeringLabel + " · " + targetLabel);
    }

    private void updateInstruction() {
        PracticeMode mode = selectedPracticeMode();
        if (mode != null && modeTitleText != null) {
            modeTitleText.setText(mode.label);
        }
        if (mode != null && instructionText != null) {
            instructionText.setText(mode.instruction);
        }
    }

    private void ensurePermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        startListening();
    }

    private void startListening() {
        practiceAnalyzer.reset();
        sessionScorer.reset();
        sessionHasFrames = false;
        scorePanel.setVisibility(View.GONE);
        scoreText.setText("");
        resetScrollToTop();
        audioAnalyzer = new AudioAnalyzer(new AudioAnalyzer.Listener() {
            @Override
            public void onAudioFrame(PitchResult result, float[] samples, int sampleRate, long timestampMs) {
                runOnUiThread(() -> {
                    waveformView.setSamples(samples);
                    handleAudioFrame(result, sampleRate, timestampMs);
                });
            }

            @Override
            public void onAudioError(String message) {
                runOnUiThread(() -> {
                    statusText.setText(message);
                    startButton.setText("开始拾音");
                    tunerView.setReading(0.0, false, "麦克风不可用");
                    waveformView.clear();
                });
            }
        });
        audioAnalyzer.start();
        if (audioAnalyzer.isRunning()) {
            startButton.setText("停止拾音");
            statusText.setText("拾音中。请让手机麦克风距离洞箫 20 到 50 厘米。");
        }
    }

    private void stopListening() {
        boolean shouldScore = audioAnalyzer != null && currentPracticeMode != null && sessionHasFrames;
        if (audioAnalyzer != null) {
            audioAnalyzer.stop();
            audioAnalyzer = null;
        }
        startButton.setText("开始拾音");
        if (shouldScore) {
            PracticeScore score = sessionScorer.finish(currentPracticeMode);
            scoreText.setText(score.format());
            scorePanel.setVisibility(View.VISIBLE);
            statusText.setText("本次练习已结束，评分已生成。");
        } else {
            statusText.setText("拾音已停止。");
        }
        if (waveformView != null) {
            waveformView.clear();
        }
    }

    private void handleAudioFrame(PitchResult result, int sampleRate, long timestampMs) {
        if (targets.isEmpty()) {
            return;
        }

        XiaoTuning tuning = selectedTuning();
        PracticeMode mode = selectedPracticeMode();
        TargetNote target = selectedTarget();
        if (result.voiced && autoTargetCheck.isChecked() && tuning != null) {
            target = tuning.closestTarget(result.frequencyHz, targets);
        }
        if (target == null || mode == null) {
            return;
        }

        PracticeStats stats = practiceAnalyzer.update(result, target, timestampMs);
        sessionScorer.update(result, target, stats, timestampMs);
        sessionHasFrames = true;
        if (result.voiced) {
            updateVoicedUi(result, sampleRate, target, mode, stats);
        } else {
            updateUnvoicedUi(target, mode, stats);
        }
    }

    private void updateVoicedUi(
            PitchResult result,
            int sampleRate,
            TargetNote target,
            PracticeMode mode,
            PracticeStats stats
    ) {
        int detectedMidi = MusicTheory.nearestMidi(result.frequencyHz);
        double detectedMidiFrequency = MusicTheory.frequencyForMidi(detectedMidi);
        double centsToNearest = MusicTheory.centsBetween(result.frequencyHz, detectedMidiFrequency);
        double centsToTarget = target.centsFrom(result.frequencyHz);

        tunerView.setReading(centsToTarget, true, "目标 " + target.label);
        pitchText.setText(String.format(
                Locale.CHINA,
                "检测 %s · %s",
                MusicTheory.noteName(detectedMidi),
                MusicTheory.formatHz(result.frequencyHz)
        ));
        detailText.setText(String.format(
                Locale.CHINA,
                "目标：%s（%s / %s）\n目标偏差：%s · 最近音偏差：%s\n置信度：%.0f%% · RMS：%.3f · 采样率：%d",
                target.label,
                MusicTheory.noteName(target.midi),
                MusicTheory.formatHz(target.frequencyHz),
                MusicTheory.formatCents(centsToTarget),
                MusicTheory.formatCents(centsToNearest),
                result.probability * 100.0,
                result.rms,
                sampleRate
        ));
        metricText.setText(formatMetrics(mode, stats));
    }

    private void updateUnvoicedUi(TargetNote target, PracticeMode mode, PracticeStats stats) {
        tunerView.setReading(0.0, false, "目标 " + target.label);
        pitchText.setText("未检测到稳定音高");
        detailText.setText("请稳定吹出一个清晰长音，避免麦克风贴得太近或环境噪声过大。");
        metricText.setText(formatMetrics(mode, stats));
    }

    private String formatMetrics(PracticeMode mode, PracticeStats stats) {
        switch (mode) {
            case LONG_TONE:
                return String.format(
                        Locale.CHINA,
                        "连续命中：%.1f 秒\n稳定度：%.1f cent\n目标：偏差小于 25 cent 后持续计时。",
                        stats.heldSeconds,
                        stats.stabilityCents
                );
            case SCALE:
                return String.format(
                        Locale.CHINA,
                        "当前偏差：%s\n稳定度：%.1f cent\n建议：慢速逐音换指，先稳住再进入下一个音。",
                        MusicTheory.formatCents(stats.cents),
                        stats.stabilityCents
                );
            case TONGUING:
                return String.format(
                        Locale.CHINA,
                        "起音次数：%d\n当前偏差：%s\n建议：吐音后快速回到本音，不要让音头明显偏高。",
                        stats.onsetCount,
                        MusicTheory.formatCents(stats.cents)
                );
            case VIBRATO:
                return String.format(
                        Locale.CHINA,
                        "气震频率：%.1f Hz\n气震深度：±%.0f cent\n建议：先控制在 4 到 6 Hz，幅度保持均匀。",
                        stats.vibratoRateHz,
                        stats.vibratoDepthCents
                );
            case SLIDE:
                return String.format(
                        Locale.CHINA,
                        "最近滑动：%s（%.0f cent）\n当前偏差：%s\n建议：滑到目标音后停稳，不要越过太多。",
                        slideDirection(stats.slideDeltaCents),
                        stats.slideDeltaCents,
                        MusicTheory.formatCents(stats.cents)
                );
            case ORNAMENT:
                return String.format(
                        Locale.CHINA,
                        "快速音高波动：%d 次\n当前偏差：%s\n建议：装饰动作要短，本音落点仍靠近 0 cent。",
                        stats.rapidMoveCount,
                        MusicTheory.formatCents(stats.cents)
                );
            default:
                return "";
        }
    }

    private String slideDirection(double slideDeltaCents) {
        if (slideDeltaCents > 80.0) {
            return "向上";
        }
        if (slideDeltaCents < -80.0) {
            return "向下";
        }
        return "接近平稳";
    }

    private XiaoTuning selectedTuning() {
        Object item = tuningSpinner.getSelectedItem();
        return item instanceof XiaoTuning ? (XiaoTuning) item : null;
    }

    private FingeringMode selectedFingeringMode() {
        Object item = fingeringSpinner.getSelectedItem();
        return item instanceof FingeringMode ? (FingeringMode) item : null;
    }

    private PracticeMode selectedPracticeMode() {
        return currentPracticeMode;
    }

    private TargetNote selectedTarget() {
        Object item = targetSpinner.getSelectedItem();
        return item instanceof TargetNote ? (TargetNote) item : null;
    }

    private PracticeSong selectedSong() {
        Object item = songSpinner.getSelectedItem();
        return item instanceof PracticeSong ? (PracticeSong) item : null;
    }

    private ImageScore selectedImageScore() {
        Object item = imageScoreSpinner.getSelectedItem();
        return item instanceof ImageScore ? (ImageScore) item : null;
    }
}
