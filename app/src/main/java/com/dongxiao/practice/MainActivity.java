package com.dongxiao.practice;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import com.dongxiao.practice.practice.PracticeNoteScore;
import com.dongxiao.practice.practice.PracticeScore;
import com.dongxiao.practice.practice.PracticeSessionScorer;
import com.dongxiao.practice.practice.PracticeStats;
import com.dongxiao.practice.practice.ScalePracticeEngine;
import com.dongxiao.practice.practice.ScalePracticeProgress;
import com.dongxiao.practice.song.ImageScore;
import com.dongxiao.practice.song.ImageScoreMarker;
import com.dongxiao.practice.song.ImageScoreRepository;
import com.dongxiao.practice.song.JianpuTextCatalog;
import com.dongxiao.practice.song.JianpuTextSongImporter;
import com.dongxiao.practice.song.JianpuTextSource;
import com.dongxiao.practice.song.PracticeSong;
import com.dongxiao.practice.song.SongPlayer;
import com.dongxiao.practice.ui.DynamicScoreView;
import com.dongxiao.practice.ui.JianpuNoteSpan;
import com.dongxiao.practice.ui.PracticeVisualizerView;
import com.dongxiao.practice.ui.ScaleScoreView;
import com.dongxiao.practice.ui.SealGlyphView;
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
    private static final float PRACTICE_HOME_CARD_HEIGHT_RATIO = 0.72f;
    private static final double HOLD_PRAISE_SECONDS = 10.0;

    private TextView statusText;
    private TextView instructionText;
    private TextView pitchText;
    private TextView detailText;
    private TextView metricText;
    private TextView techniqueMetricText;
    private TextView scoreText;
    private TextView modeTitleText;
    private TextView practiceSummaryText;
    private TextView songTitleText;
    private TextView songMetaText;
    private TextView songStatusText;
    private TextView imageScorePageText;
    private ScaleScoreView scaleScoreView;
    private LinearLayout homeContainer;
    private FrameLayout practiceContainer;
    private FrameLayout songContainer;
    private LinearLayout modeList;
    private LinearLayout scorePanel;
    private Spinner tuningSpinner;
    private Spinner fingeringSpinner;
    private Spinner targetSpinner;
    private Spinner songSpinner;
    private Spinner imageScoreSpinner;
    private CheckBox autoTargetCheck;
    private ImageButton backButton;
    private Button startButton;
    private ImageButton songBackButton;
    private Button songPlayButton;
    private Button imageScorePrevButton;
    private Button imageScoreNextButton;
    private Button scoreDetailButton;
    private ImageView imageScoreView;
    private ImageView practiceHeaderImage;
    private ImageView holdPraiseThumb;
    private PracticeVisualizerView practiceVisualizerView;
    private WaveformView waveformView;
    private DynamicScoreView dynamicScoreView;

    private final PracticeAnalyzer practiceAnalyzer = new PracticeAnalyzer();
    private final PracticeSessionScorer sessionScorer = new PracticeSessionScorer();
    private final ScalePracticeEngine scalePracticeEngine = new ScalePracticeEngine();
    private final List<TargetNote> targets = new ArrayList<>();
    private final List<PracticeSong> songs = new ArrayList<>();
    private final List<ImageScore> imageScores = ImageScoreRepository.defaults();
    private AudioAnalyzer audioAnalyzer;
    private SongPlayer songPlayer;
    private ArrayAdapter<PracticeSong> songAdapter;
    private ArrayAdapter<ImageScore> imageScoreAdapter;
    private PracticeMode currentPracticeMode;
    private PracticeScore latestPracticeScore;
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
        setupScoreReport();
        setupSongPractice();
        showHome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening(false);
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
        techniqueMetricText = findViewById(R.id.techniqueMetricText);
        scoreText = findViewById(R.id.scoreText);
        modeTitleText = findViewById(R.id.modeTitleText);
        practiceSummaryText = findViewById(R.id.practiceSummaryText);
        songTitleText = findViewById(R.id.songTitleText);
        songMetaText = findViewById(R.id.songMetaText);
        songStatusText = findViewById(R.id.songStatusText);
        imageScorePageText = findViewById(R.id.imageScorePageText);
        scaleScoreView = findViewById(R.id.scaleScoreView);
        practiceHeaderImage = findViewById(R.id.practiceHeaderImage);
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
        scoreDetailButton = findViewById(R.id.scoreDetailButton);
        imageScoreView = findViewById(R.id.imageScoreView);
        holdPraiseThumb = findViewById(R.id.holdPraiseThumb);
        practiceVisualizerView = findViewById(R.id.practiceVisualizerView);
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
                resetScalePractice();
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
                resetScalePractice();
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
        PracticeMode[] modes = PracticeMode.values();
        for (int index = 0; index < modes.length; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dp(12);

            for (int column = 0; column < 2 && index + column < modes.length; column++) {
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                if (column == 0) {
                    cardParams.rightMargin = dp(6);
                } else {
                    cardParams.leftMargin = dp(6);
                }
                row.addView(createPracticeCard(modes[index + column]), cardParams);
            }
            modeList.addView(row, rowParams);
        }
        modeList.addView(createSongPracticeCard());
        backButton.setOnClickListener(view -> showHome());
    }

    private View createPracticeCard(PracticeMode mode) {
        return createHomeActionCard(
                mode.label,
                mode.instruction,
                "练",
                getPracticeDecorationRes(mode),
                view -> enterPractice(mode),
                false
        );
    }

    private View createSongPracticeCard() {
        return createHomeActionCard(
                "曲目练习",
                "选择本地图片谱，播放智能伴奏，并在原图上跟随高亮练习。",
                "曲",
                R.drawable.deco_home_card_song,
                view -> enterSongPractice(),
                true
        );
    }

    private View createHomeActionCard(
            String titleText,
            String descriptionText,
            String sealText,
            int decorationResId,
            View.OnClickListener listener,
            boolean isSongCard
    ) {
        FrameLayout card = isSongCard ? new FrameLayout(this) : new RatioHomeCard(this);
        card.setMinimumHeight(dp(isSongCard ? 192 : 108));
        card.setBackgroundResource(R.drawable.bg_practice_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(titleText + "。" + descriptionText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(3));
            card.setClipToOutline(true);
        }

        ImageView cardDecoration = createHomeCardDecoration(decorationResId, isSongCard);
        FrameLayout.LayoutParams decorationParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        card.addView(cardDecoration, decorationParams);

        View scrim = new View(this);
        scrim.setBackgroundColor(Color.argb(isSongCard ? 70 : 58, 255, 253, 246));
        card.addView(scrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        if (isSongCard) {
            addSongHomeCardContent(card, titleText, descriptionText, sealText);
        } else {
            addPracticeHomeCardContent(card, titleText, sealText);
        }

        if (isSongCard) {
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(192)
            );
            cardParams.topMargin = dp(2);
            card.setLayoutParams(cardParams);
        }
        card.setOnClickListener(listener);
        return card;
    }

    private void addPracticeHomeCardContent(
            FrameLayout card,
            String titleText,
            String sealText
    ) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.BOTTOM);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        View seal = createHomeBadge(sealText);
        LinearLayout.LayoutParams sealParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        sealParams.rightMargin = dp(8);
        topRow.addView(seal, sealParams);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(getColorCompat(R.color.ink));
        title.setTextSize(16.5f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        topRow.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        content.addView(topRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        card.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private void addSongHomeCardContent(
            FrameLayout card,
            String titleText,
            String descriptionText,
            String sealText
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));

        View seal = createHomeBadge(sealText);
        LinearLayout.LayoutParams sealParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        sealParams.rightMargin = dp(16);
        row.addView(seal, sealParams);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(getColorCompat(R.color.ink));
        title.setTextSize(19.0f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView description = new TextView(this);
        description.setText(descriptionText);
        description.setTextColor(getColorCompat(R.color.muted));
        description.setTextSize(13.0f);
        description.setLineSpacing(dp(2), 1.0f);
        description.setMaxLines(1);
        description.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(4);
        textColumn.addView(description, descriptionParams);

        row.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        card.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
        ));
    }

    private int getPracticeDecorationRes(PracticeMode mode) {
        switch (mode) {
            case LONG_TONE:
                return R.drawable.deco_card_long_tone;
            case SCALE:
                return R.drawable.deco_card_scale;
            case TONGUING:
                return R.drawable.deco_card_tonguing;
            case VIBRATO:
                return R.drawable.deco_card_vibrato;
            case SLIDE:
                return R.drawable.deco_card_slide;
            case ORNAMENT:
                return R.drawable.deco_card_ornament;
            default:
                return R.drawable.deco_card_scale;
        }
    }

    private ImageView createHomeCardDecoration(int decorationResId, boolean isSongCard) {
        ImageView decoration = new FocusCropImageView(this);
        decoration.setImageResource(decorationResId);
        decoration.setAlpha(isSongCard ? 0.46f : 0.52f);
        decoration.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ((FocusCropImageView) decoration).setCropFocus(0.5f, getHomeCardCropFocusY(decorationResId));
        return decoration;
    }

    private float getHomeCardCropFocusY(int decorationResId) {
        if (decorationResId == R.drawable.deco_home_card_song) {
            return 0.43f;
        }
        if (decorationResId == R.drawable.deco_card_long_tone) {
            return 0.34f;
        }
        if (decorationResId == R.drawable.deco_card_tonguing) {
            return 0.28f;
        }
        if (decorationResId == R.drawable.deco_card_vibrato) {
            return 0.58f;
        }
        if (decorationResId == R.drawable.deco_card_slide) {
            return 0.72f;
        }
        if (decorationResId == R.drawable.deco_card_ornament) {
            return 0.31f;
        }
        return 0.5f;
    }

    private View createHomeBadge(String sealText) {
        SealGlyphView glyphView = new SealGlyphView(this);
        glyphView.setGlyph("练".equals(sealText) ? "練" : sealText);
        glyphView.setTintColor("曲".equals(sealText)
                ? getColorCompat(R.color.gold_dark)
                : getColorCompat(R.color.cinnabar_dark));
        return glyphView;
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

    private void setupScoreReport() {
        scorePanel.setClickable(true);
        scorePanel.setFocusable(true);
        scorePanel.setOnClickListener(view -> {
            if (latestPracticeScore != null) {
                showPracticeScoreDialog(latestPracticeScore);
            }
        });
        scoreDetailButton.setOnClickListener(view -> {
            if (latestPracticeScore != null) {
                showPracticeScoreDialog(latestPracticeScore);
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
        setHoldPraiseVisible(false);
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
        setHoldPraiseVisible(false);
        updateInstruction();
        updateTargets();
        resetScalePractice();
        pitchText.setText("未检测到稳定音高");
        detailText.setText("Hz、音名和 cent 偏差会显示在这里。");
        metricText.setText("练习指标等待开始。");
        updateTechniqueMetricText("练习指标等待开始。");
        scorePanel.setVisibility(View.GONE);
        scoreText.setText("");
        latestPracticeScore = null;
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
        ArrayAdapter<T> adapter = new ArrayAdapter<T>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                bindSpinnerText(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                bindSpinnerText(view);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private <T> ArrayAdapter<T> createAdapter(List<T> items) {
        ArrayAdapter<T> adapter = new ArrayAdapter<T>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                bindSpinnerText(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                bindSpinnerText(view);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void bindSpinnerText(TextView view) {
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextColor(getColorCompat(R.color.ink));
        view.setTextSize(14.0f);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(dp(8), 0, dp(22), 0);
        view.setMinHeight(dp(44));
    }

    private ArrayAdapter<TargetNote> createTargetAdapter(List<TargetNote> items) {
        ArrayAdapter<TargetNote> adapter = new ArrayAdapter<TargetNote>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                bindTargetNoteText(view, getItem(position));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                bindTargetNoteText(view, getItem(position));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void bindTargetNoteText(TextView view, TargetNote target) {
        if (target == null) {
            view.setText("");
            return;
        }
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextColor(getColorCompat(R.color.ink));
        view.setTextSize(14.0f);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setMinHeight(dp(48));
        view.setText(JianpuNoteSpan.textFor(target));
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

        ArrayAdapter<TargetNote> targetAdapter = createTargetAdapter(new ArrayList<>(targets));
        targetSpinner.setAdapter(targetAdapter);
        if (!targets.isEmpty()) {
            targetSpinner.setSelection(0);
            TargetNote target = targets.get(targetSpinner.getSelectedItemPosition());
            practiceVisualizerView.clear(selectedPracticeMode(), target);
            if (statusText != null) {
                statusText.setText(formatReferenceText(tuning, fingeringMode));
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
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(tuningLabel).append(" · ").append(fingeringLabel).append(" · ");
        PracticeMode mode = selectedPracticeMode();
        if (mode == PracticeMode.SCALE && target != null) {
            builder.append("音阶从 ");
            JianpuNoteSpan.appendTo(builder, target);
            builder.append(" 起，级进/三度/分解");
        } else if (isFixedTargetTechnique(mode) && target != null) {
            builder.append("固定目标 ");
            JianpuNoteSpan.appendTo(builder, target);
        } else if (autoTargetCheck != null && autoTargetCheck.isChecked()) {
            builder.append("自动匹配目标音");
        } else if (target == null) {
            builder.append("未选目标音");
        } else {
            JianpuNoteSpan.appendTo(builder, target);
        }
        practiceSummaryText.setText(builder);
    }

    private void resetScalePractice() {
        scalePracticeEngine.reset(targets, selectedTarget());
        if (selectedPracticeMode() == PracticeMode.SCALE) {
            updateScaleScoreView(scalePracticeEngine.snapshot());
        }
    }

    private void ensureScalePracticeReady() {
        if (!scalePracticeEngine.hasSequence()) {
            resetScalePractice();
        }
    }

    private void updateScaleScoreView(ScalePracticeProgress progress) {
        if (scaleScoreView == null) {
            return;
        }
        if (selectedPracticeMode() != PracticeMode.SCALE) {
            scaleScoreView.setVisibility(View.GONE);
            scaleScoreView.setProgress(null);
            return;
        }
        scaleScoreView.setVisibility(View.VISIBLE);
        scaleScoreView.setProgress(progress);
    }

    private void updateInstruction() {
        PracticeMode mode = selectedPracticeMode();
        if (mode != null && modeTitleText != null) {
            modeTitleText.setText(mode.label);
        }
        if (mode != null && practiceHeaderImage != null) {
            practiceHeaderImage.setImageResource(getPracticeDecorationRes(mode));
        }
        if (mode != null && instructionText != null) {
            instructionText.setText(mode.instruction);
        }
        updateAutoTargetControlForMode(mode);
        if (mode == PracticeMode.SCALE) {
            resetScalePractice();
            updateScaleScoreView(scalePracticeEngine.snapshot());
        } else if (scaleScoreView != null) {
            scaleScoreView.setVisibility(View.GONE);
            scaleScoreView.setProgress(null);
        }
    }

    private void updateAutoTargetControlForMode(PracticeMode mode) {
        if (autoTargetCheck == null) {
            return;
        }
        if (mode == PracticeMode.SCALE) {
            autoTargetCheck.setEnabled(false);
            autoTargetCheck.setAlpha(0.62f);
            autoTargetCheck.setText("音阶模式按序列推进");
        } else if (isFixedTargetTechnique(mode)) {
            autoTargetCheck.setEnabled(false);
            autoTargetCheck.setAlpha(0.62f);
            autoTargetCheck.setText("专项模式固定目标音");
        } else {
            autoTargetCheck.setEnabled(true);
            autoTargetCheck.setAlpha(1.0f);
            autoTargetCheck.setText("自动匹配最近目标音");
        }
    }

    private static boolean shouldAutoMatchTarget(PracticeMode mode) {
        return mode == PracticeMode.LONG_TONE;
    }

    private static boolean isFixedTargetTechnique(PracticeMode mode) {
        return mode == PracticeMode.TONGUING
                || mode == PracticeMode.VIBRATO
                || mode == PracticeMode.SLIDE
                || mode == PracticeMode.ORNAMENT;
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
        resetScalePractice();
        sessionHasFrames = false;
        latestPracticeScore = null;
        scorePanel.setVisibility(View.GONE);
        scoreText.setText("");
        waveformView.clear();
        resetScrollToTop();
        audioAnalyzer = new AudioAnalyzer(new AudioAnalyzer.Listener() {
            @Override
            public void onAudioFrame(PitchResult result, float[] samples, int sampleRate, long timestampMs) {
                runOnUiThread(() -> {
                    waveformView.appendSamples(samples, sampleRate);
                    handleAudioFrame(result, timestampMs);
                });
            }

            @Override
            public void onAudioError(String message) {
                runOnUiThread(() -> {
                    statusText.setText(message);
                    startButton.setText("开始拾音");
                    practiceVisualizerView.clear(selectedPracticeMode(), selectedTarget());
                    setHoldPraiseVisible(false);
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
        stopListening(true);
    }

    private void stopListening(boolean showReportDialog) {
        boolean shouldScore = audioAnalyzer != null && currentPracticeMode != null && sessionHasFrames;
        if (audioAnalyzer != null) {
            audioAnalyzer.stop();
            audioAnalyzer = null;
        }
        startButton.setText("开始拾音");
        if (shouldScore) {
            PracticeScore score = sessionScorer.finish(currentPracticeMode);
            latestPracticeScore = score;
            scoreText.setText(formatScorePanelSummary(score));
            scorePanel.setVisibility(View.VISIBLE);
            statusText.setText("本次练习已结束，评分报告已生成。");
            if (showReportDialog) {
                showPracticeScoreDialog(score);
            }
        } else {
            statusText.setText("拾音已停止。");
        }
        if (waveformView != null) {
            waveformView.clear();
        }
        setHoldPraiseVisible(false);
    }

    private void handleAudioFrame(PitchResult result, long timestampMs) {
        if (targets.isEmpty()) {
            return;
        }

        XiaoTuning tuning = selectedTuning();
        PracticeMode mode = selectedPracticeMode();
        TargetNote target = selectedTarget();
        ScalePracticeProgress scaleProgress = null;
        if (mode == PracticeMode.SCALE) {
            ensureScalePracticeReady();
            target = scalePracticeEngine.currentTarget();
        } else if (result.voiced
                && shouldAutoMatchTarget(mode)
                && autoTargetCheck.isChecked()
                && tuning != null) {
            target = tuning.closestTarget(result.frequencyHz, targets);
        }
        if (target == null || mode == null) {
            return;
        }

        PracticeStats stats = practiceAnalyzer.update(result, target, timestampMs);
        sessionScorer.update(result, target, stats, timestampMs);
        if (mode == PracticeMode.SCALE) {
            scaleProgress = scalePracticeEngine.update(result, timestampMs, targets);
            sessionScorer.updateScaleProgress(scaleProgress);
            updateScaleScoreView(scaleProgress);
            if (scaleProgress.justAdvanced) {
                practiceAnalyzer.reset();
            }
        }
        updateHoldPraise(mode, stats);
        sessionHasFrames = true;
        TargetNote displayTarget = mode == PracticeMode.SCALE
                ? scalePracticeEngine.currentTarget()
                : target;
        if (displayTarget == null) {
            displayTarget = target;
        }
        if (result.voiced) {
            updateVoicedUi(result, displayTarget, mode, stats, scaleProgress);
        } else {
            updateUnvoicedUi(displayTarget, mode, stats, scaleProgress);
        }
    }

    private void updateVoicedUi(
            PitchResult result,
            TargetNote target,
            PracticeMode mode,
            PracticeStats stats,
            ScalePracticeProgress scaleProgress
    ) {
        int detectedMidi = MusicTheory.nearestMidi(result.frequencyHz);
        double detectedMidiFrequency = MusicTheory.frequencyForMidi(detectedMidi);
        double centsToNearest = MusicTheory.centsBetween(result.frequencyHz, detectedMidiFrequency);
        double centsToTarget = target.centsFrom(result.frequencyHz);

        practiceVisualizerView.setReading(mode, centsToTarget, true, target, stats);
        pitchText.setText(String.format(
                Locale.CHINA,
                "检测 %s · %s",
                MusicTheory.noteName(detectedMidi),
                MusicTheory.formatHz(result.frequencyHz)
        ));
        detailText.setText(formatVoicedDetail(target, centsToTarget, centsToNearest, result.rms));
        String metrics = mode == PracticeMode.SCALE
                ? formatScaleMetrics(scaleProgress, stats)
                : formatMetrics(mode, stats);
        metricText.setText(metrics);
        updateTechniqueMetricText(metrics);
    }

    private void updateUnvoicedUi(
            TargetNote target,
            PracticeMode mode,
            PracticeStats stats,
            ScalePracticeProgress scaleProgress
    ) {
        practiceVisualizerView.setReading(mode, 0.0, false, target, stats);
        pitchText.setText("未检测到稳定音高");
        detailText.setText("请稳定吹出一个清晰长音，避免麦克风贴得太近或环境噪声过大。");
        String metrics = mode == PracticeMode.SCALE
                ? formatScaleMetrics(scaleProgress, stats)
                : formatMetrics(mode, stats);
        metricText.setText(metrics);
        updateTechniqueMetricText(metrics);
    }

    private void updateTechniqueMetricText(String metrics) {
        if (techniqueMetricText != null) {
            techniqueMetricText.setText(metrics);
        }
    }

    private static double stabilityPercentForMode(PracticeMode mode, PracticeStats stats) {
        if (mode != PracticeMode.LONG_TONE || stats == null || !stats.hasPitch || !stats.stabilityReady) {
            return Double.NaN;
        }
        return PracticeStats.stabilityPercent(stats.stabilityCents);
    }

    private static double heldSecondsForMode(PracticeMode mode, PracticeStats stats) {
        if (mode != PracticeMode.LONG_TONE || stats == null) {
            return Double.NaN;
        }
        return stats.heldSeconds;
    }

    private void updateHoldPraise(PracticeMode mode, PracticeStats stats) {
        boolean visible = mode == PracticeMode.LONG_TONE
                && stats != null
                && stats.heldSeconds >= HOLD_PRAISE_SECONDS;
        setHoldPraiseVisible(visible);
    }

    private void setHoldPraiseVisible(boolean visible) {
        if (holdPraiseThumb == null) {
            return;
        }
        holdPraiseThumb.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private static String safeStabilityCents(PracticeStats stats) {
        if (stats == null || !stats.stabilityReady || Double.isNaN(stats.stabilityCents)) {
            return "--";
        }
        return String.format(Locale.CHINA, "%.1f cent", stats.stabilityCents);
    }

    private static double amplitudePercent(double rms) {
        if (Double.isNaN(rms) || Double.isInfinite(rms)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, rms * 100.0));
    }

    private String formatScorePanelSummary(PracticeScore score) {
        String reportType = score.noteScores.isEmpty()
                ? "专项报告"
                : String.format(Locale.CHINA, "逐音分析 %d 项", score.noteScores.size());
        return String.format(
                Locale.CHINA,
                "综合评分 %d 分 · 有效 %.1f 秒\n%s，点击查看完整报告。",
                score.score,
                score.voicedSeconds,
                reportType
        );
    }

    private void showPracticeScoreDialog(PracticeScore score) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int dialogHeight = Math.max(dp(300), Math.min(screenHeight - dp(64), dp(720)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_score_dialog);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dialogHeight
        ));

        root.addView(createScoreDialogHeader(score));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setClipToPadding(false);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(16), 0, dp(8));
        addScoreMetricGrid(body, score);
        addScoreCommentCard(body, score);
        addNoteScoreCards(body, score);
        scrollView.addView(body, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));

        Button closeButton = new Button(this);
        closeButton.setText("关闭报告");
        closeButton.setAllCaps(false);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(15.0f);
        closeButton.setBackgroundResource(R.drawable.bg_button_primary);
        closeButton.setOnClickListener(view -> dialog.dismiss());
        root.addView(closeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        dialog.setContentView(root);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    Math.min(screenWidth - dp(24), dp(620)),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private View createScoreDialogHeader(PracticeScore score) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);

        TextView title = createText("练习评分报告", 20.0f, R.color.ink, true);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        textColumn.addView(title);

        TextView subtitle = createText(
                score.noteScores.isEmpty() ? "综合表现" : "综合表现 · 逐音建议",
                13.0f,
                R.color.muted,
                false
        );
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(4);
        textColumn.addView(subtitle, subtitleParams);

        header.addView(textColumn, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        LinearLayout scoreBox = new LinearLayout(this);
        scoreBox.setOrientation(LinearLayout.VERTICAL);
        scoreBox.setGravity(Gravity.CENTER);
        scoreBox.setBackgroundResource(R.drawable.bg_score_metric);
        scoreBox.setMinimumWidth(dp(88));

        TextView scoreValue = createText(String.valueOf(score.score), 32.0f, R.color.cinnabar, true);
        scoreValue.setGravity(Gravity.CENTER);
        TextView scoreLabel = createText("综合分", 12.0f, R.color.muted, false);
        scoreLabel.setGravity(Gravity.CENTER);
        scoreBox.addView(scoreValue);
        scoreBox.addView(scoreLabel);

        LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(dp(92), dp(76));
        scoreParams.leftMargin = dp(12);
        header.addView(scoreBox, scoreParams);
        return header;
    }

    private void addScoreMetricGrid(LinearLayout body, PracticeScore score) {
        LinearLayout firstRow = createHorizontalRow();
        addMetricTile(firstRow, "有效发声", String.format(Locale.CHINA, "%.1f 秒", score.voicedSeconds));
        addMetricTile(firstRow, "平均偏差", MusicTheory.formatCents(score.meanAbsCents));
        body.addView(firstRow);

        LinearLayout secondRow = createHorizontalRow();
        LinearLayout.LayoutParams secondRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        secondRowParams.topMargin = dp(8);
        addMetricTile(secondRow, "稳定度", PracticeStats.formatStabilityPercent(score.stabilityCents));
        addMetricTile(secondRow, "命中率", String.format(Locale.CHINA, "%.0f%%", score.hitRate));
        body.addView(secondRow, secondRowParams);
    }

    private void addScoreCommentCard(LinearLayout body, PracticeScore score) {
        LinearLayout card = createDialogCard(R.drawable.bg_score_metric);
        TextView title = createText("总评", 15.0f, R.color.ink, true);
        TextView comment = createText(score.comment, 14.0f, R.color.ink, false);
        comment.setLineSpacing(dp(2), 1.0f);
        TextView detail = createText(score.modeDetail, 13.0f, R.color.muted, false);
        detail.setLineSpacing(dp(2), 1.0f);

        card.addView(title);
        card.addView(comment, topMarginParams(6));
        if (!TextUtils.isEmpty(score.modeDetail)) {
            card.addView(detail, topMarginParams(6));
        }
        body.addView(card, topMarginParams(12));
    }

    private void addNoteScoreCards(LinearLayout body, PracticeScore score) {
        TextView sectionTitle = createText(
                score.noteScores.isEmpty() ? "逐音分析" : "逐音分析（" + score.noteScores.size() + "项）",
                16.0f,
                R.color.ink,
                true
        );
        body.addView(sectionTitle, topMarginParams(16));

        if (score.noteScores.isEmpty()) {
            TextView emptyText = createText("本次有效声音不足，暂无逐音建议。", 13.0f, R.color.muted, false);
            body.addView(emptyText, topMarginParams(8));
            return;
        }

        for (PracticeNoteScore noteScore : score.noteScores) {
            body.addView(createNoteScoreCard(noteScore), topMarginParams(10));
        }
    }

    private View createNoteScoreCard(PracticeNoteScore noteScore) {
        LinearLayout card = createDialogCard(R.drawable.bg_note_score_card);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        SpannableStringBuilder noteLabel = new SpannableStringBuilder();
        JianpuNoteSpan.appendTo(noteLabel, new TargetNote(
                String.valueOf(noteScore.scaleDegree),
                noteScore.scaleDegree,
                noteScore.midi,
                noteScore.register
        ));
        noteLabel.append("  ").append(String.valueOf(noteScore.score)).append(" 分");

        TextView noteTitle = createText("", 17.0f, R.color.ink, true);
        noteTitle.setText(noteLabel);
        titleRow.addView(noteTitle, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        TextView noteMeta = createText(
                String.format(
                        Locale.CHINA,
                        "%.1f秒 · %s · 稳定%s · 命中%.0f%%",
                        noteScore.voicedSeconds,
                        MusicTheory.formatCents(noteScore.meanCents),
                        PracticeStats.formatStabilityPercent(noteScore.stabilityCents),
                        noteScore.hitRate
                ),
                12.0f,
                R.color.muted,
                false
        );
        noteMeta.setGravity(Gravity.RIGHT);
        titleRow.addView(noteMeta, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.25f
        ));
        card.addView(titleRow);

        addAssessmentLine(card, "优点", noteScore.strengths, R.color.accent);
        addAssessmentLine(card, "不足", noteScore.weaknesses, R.color.warning);
        addAssessmentLine(card, "建议", noteScore.suggestions, R.color.cinnabar);
        return card;
    }

    private void addAssessmentLine(LinearLayout card, String label, String text, int labelColor) {
        TextView line = createText(label + "：" + text, 13.0f, R.color.ink, false);
        line.setLineSpacing(dp(2), 1.0f);
        line.setTextColor(getColorCompat(labelColor));
        card.addView(line, topMarginParams(8));
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setBaselineAligned(false);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void addMetricTile(LinearLayout row, String label, String value) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackgroundResource(R.drawable.bg_score_metric);
        tile.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView valueView = createText(value, 17.0f, R.color.ink, true);
        TextView labelView = createText(label, 12.0f, R.color.muted, false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(2);
        tile.addView(valueView);
        tile.addView(labelView, labelParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        if (row.getChildCount() > 0) {
            params.leftMargin = dp(8);
        }
        row.addView(tile, params);
    }

    private LinearLayout createDialogCard(int backgroundResId) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(backgroundResId);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        return card;
    }

    private TextView createText(String text, float textSize, int colorResId, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(getColorCompat(colorResId));
        textView.setIncludeFontPadding(true);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private LinearLayout.LayoutParams topMarginParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private CharSequence formatReferenceText(XiaoTuning tuning, FingeringMode fingeringMode) {
        int tubeDegree = fingeringMode == null ? 5 : fingeringMode.tubeDegree;
        TargetNote tubeNote = new TargetNote(
                String.valueOf(tubeDegree),
                tubeDegree,
                tuning.tubeMidi,
                TargetNote.REGISTER_LOW
        );
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(tuning.label).append("基准：");
        JianpuNoteSpan.appendTo(builder, tubeNote);
        builder.append(" = ")
                .append(MusicTheory.noteName(tuning.tubeMidi))
                .append(" / ")
                .append(MusicTheory.formatHz(MusicTheory.frequencyForMidi(tuning.tubeMidi)));
        return builder;
    }

    private CharSequence formatVoicedDetail(
            TargetNote target,
            double centsToTarget,
            double centsToNearest,
            double rms
    ) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("目标：");
        JianpuNoteSpan.appendTo(builder, target);
        builder.append(String.format(
                Locale.CHINA,
                "（%s / %s）\n目标偏差：%s · 最近音偏差：%s\n幅度：%.0f%%",
                MusicTheory.noteName(target.midi),
                MusicTheory.formatHz(target.frequencyHz),
                MusicTheory.formatCents(centsToTarget),
                MusicTheory.formatCents(centsToNearest),
                amplitudePercent(rms)
        ));
        return builder;
    }

    private String formatMetrics(PracticeMode mode, PracticeStats stats) {
        switch (mode) {
            case LONG_TONE:
                return "目标：偏差小于 25 cent 后持续计时。";
            case SCALE:
                return String.format(
                        Locale.CHINA,
                        "当前偏差：%s\n稳定度：%s\n建议：慢速逐音换指，先稳住再进入下一个音。",
                        MusicTheory.formatCents(stats.cents),
                        safeStabilityCents(stats)
                );
            case TONGUING:
                return String.format(
                        Locale.CHINA,
                        "吐音：%d 次 · %.1f 次/秒\n均匀度：%s · 回本音：%s\n目标：音头短、间隔匀、落点准。",
                        stats.onsetCount,
                        stats.tonguingRateHz,
                        formatPercent(stats.tonguingEvennessPercent),
                        MusicTheory.formatCents(stats.cents)
                );
            case VIBRATO:
                return String.format(
                        Locale.CHINA,
                        "气震：%.1f Hz · 深度 ±%.0f cent\n规律性：%s · 中心：%s\n目标：频率匀，音高中心不漂。",
                        stats.vibratoRateHz,
                        stats.vibratoDepthCents,
                        formatPercent(stats.vibratoRegularityPercent),
                        MusicTheory.formatCents(stats.cents)
                );
            case SLIDE:
                return String.format(
                        Locale.CHINA,
                        "滑音：%s · 幅度 %.0f cent\n平滑度：%s · 落点：%s\n目标：滑入后停在目标音。",
                        slideDirection(stats.slideDeltaCents),
                        stats.slideRangeCents,
                        formatPercent(stats.slideSmoothnessPercent),
                        stats.slideLanded ? "已到位" : MusicTheory.formatCents(stats.cents)
                );
            case ORNAMENT:
                return String.format(
                        Locale.CHINA,
                        "打/叠：回落 %d 次 · 离音 %.0f cent\n最近用时：%.0f ms · 本音：%s\n目标：离得短，回得准。",
                        stats.ornamentCount,
                        stats.ornamentLastExcursionCents,
                        stats.ornamentLastDurationMs,
                        MusicTheory.formatCents(stats.cents)
                );
            default:
                return "";
        }
    }

    private String formatScaleMetrics(ScalePracticeProgress progress, PracticeStats stats) {
        if (progress == null || progress.totalNotes <= 0) {
            return "音阶序列准备中。";
        }
        if (progress.completed) {
            return String.format(
                    Locale.CHINA,
                    "音阶完成：%d/%d\n误吹次数：%d\n建议：查看三段乐谱中哪一段最容易错。",
                    progress.completedNotes,
                    progress.totalNotes,
                    progress.wrongAttempts
            );
        }
        String centsText = stats == null || !stats.hasPitch ? "--" : MusicTheory.formatCents(stats.cents);
        return String.format(
                Locale.CHINA,
                "音阶：%s · %d/%d\n当前偏差：%s\n误吹次数：%d",
                scaleSectionLabel(progress),
                progress.currentIndex + 1,
                progress.totalNotes,
                centsText,
                progress.wrongAttempts
        );
    }

    private static String scaleSectionLabel(ScalePracticeProgress progress) {
        if (progress == null || progress.sectionStarts == null || progress.sectionStarts.isEmpty()) {
            return "序列";
        }
        int section = 0;
        for (int i = 0; i < progress.sectionStarts.size(); i++) {
            if (progress.currentIndex >= progress.sectionStarts.get(i)) {
                section = i;
            }
        }
        if (progress.sectionLabels != null && section < progress.sectionLabels.size()) {
            return progress.sectionLabels.get(section);
        }
        return "序列";
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

    private static String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent) || percent <= 0.0) {
            return "--";
        }
        return String.format(Locale.CHINA, "%.0f%%", percent);
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

    private static final class RatioHomeCard extends FrameLayout {
        RatioHomeCard(Activity context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            if (width <= 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            int height = Math.max(getSuggestedMinimumHeight(), Math.round(width * PRACTICE_HOME_CARD_HEIGHT_RATIO));
            int compactHeightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, compactHeightSpec);
            setMeasuredDimension(width, height);
        }
    }

    private static final class FocusCropImageView extends ImageView {
        private final Matrix cropMatrix = new Matrix();
        private float cropFocusX = 0.5f;
        private float cropFocusY = 0.5f;

        FocusCropImageView(Activity context) {
            super(context);
            super.setScaleType(ScaleType.MATRIX);
        }

        void setCropFocus(float focusX, float focusY) {
            cropFocusX = clampFocus(focusX);
            cropFocusY = clampFocus(focusY);
            updateCropMatrix();
        }

        @Override
        public void setImageDrawable(Drawable drawable) {
            super.setImageDrawable(drawable);
            updateCropMatrix();
        }

        @Override
        public void setScaleType(ScaleType scaleType) {
            super.setScaleType(ScaleType.MATRIX);
            updateCropMatrix();
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            updateCropMatrix();
        }

        private void updateCropMatrix() {
            Drawable drawable = getDrawable();
            int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
            if (drawable == null || viewWidth <= 0 || viewHeight <= 0) {
                return;
            }

            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            if (drawableWidth <= 0 || drawableHeight <= 0) {
                return;
            }

            float scale = Math.max(
                    (float) viewWidth / (float) drawableWidth,
                    (float) viewHeight / (float) drawableHeight
            );
            float scaledWidth = drawableWidth * scale;
            float scaledHeight = drawableHeight * scale;
            float dx = getPaddingLeft() - Math.max(0.0f, scaledWidth - viewWidth) * cropFocusX;
            float dy = getPaddingTop() - Math.max(0.0f, scaledHeight - viewHeight) * cropFocusY;

            cropMatrix.reset();
            cropMatrix.setScale(scale, scale);
            cropMatrix.postTranslate(Math.round(dx), Math.round(dy));
            super.setImageMatrix(cropMatrix);
        }

        private static float clampFocus(float value) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }
    }
}
