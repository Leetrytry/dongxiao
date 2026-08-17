package com.dongxiao.practice;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.dongxiao.practice.ui.TunerView;
import com.dongxiao.practice.ui.WaveformView;

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
    private LinearLayout homeContainer;
    private LinearLayout practiceContainer;
    private LinearLayout modeList;
    private LinearLayout scorePanel;
    private Spinner tuningSpinner;
    private Spinner fingeringSpinner;
    private Spinner targetSpinner;
    private CheckBox autoTargetCheck;
    private Button backButton;
    private Button startButton;
    private TunerView tunerView;
    private WaveformView waveformView;

    private final PracticeAnalyzer practiceAnalyzer = new PracticeAnalyzer();
    private final PracticeSessionScorer sessionScorer = new PracticeSessionScorer();
    private final List<TargetNote> targets = new ArrayList<>();
    private AudioAnalyzer audioAnalyzer;
    private PracticeMode currentPracticeMode;
    private boolean sessionHasFrames = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configureSystemBars();
        bindViews();
        setupSpinners();
        setupPracticeModes();
        setupStartButton();
        showHome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening();
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
        homeContainer = findViewById(R.id.homeContainer);
        practiceContainer = findViewById(R.id.practiceContainer);
        modeList = findViewById(R.id.modeList);
        scorePanel = findViewById(R.id.scorePanel);
        tuningSpinner = findViewById(R.id.tuningSpinner);
        fingeringSpinner = findViewById(R.id.fingeringSpinner);
        targetSpinner = findViewById(R.id.targetSpinner);
        autoTargetCheck = findViewById(R.id.autoTargetCheck);
        backButton = findViewById(R.id.backButton);
        startButton = findViewById(R.id.startButton);
        tunerView = findViewById(R.id.tunerView);
        waveformView = findViewById(R.id.waveformView);
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

        updateTargets();
    }

    private void setupPracticeModes() {
        modeList.removeAllViews();
        for (PracticeMode mode : PracticeMode.values()) {
            modeList.addView(createPracticeCard(mode));
        }
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

        TextView title = new TextView(this);
        title.setText(mode.label);
        title.setTextColor(getColorCompat(R.color.ink));
        title.setTextSize(18.0f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(
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

    private void setupStartButton() {
        startButton.setOnClickListener(view -> {
            if (audioAnalyzer != null && audioAnalyzer.isRunning()) {
                stopListening();
            } else {
                ensurePermissionAndStart();
            }
        });
    }

    private void showHome() {
        stopListening();
        currentPracticeMode = null;
        homeContainer.setVisibility(View.VISIBLE);
        practiceContainer.setVisibility(View.GONE);
    }

    private void enterPractice(PracticeMode mode) {
        currentPracticeMode = mode;
        homeContainer.setVisibility(View.GONE);
        practiceContainer.setVisibility(View.VISIBLE);
        practiceAnalyzer.reset();
        updateInstruction();
        updateTargets();
        pitchText.setText("未检测到稳定音高");
        detailText.setText("Hz、音名、cent 偏差和稳定度会显示在这里。");
        metricText.setText("练习指标等待开始。");
        scorePanel.setVisibility(View.GONE);
        scoreText.setText("");
        waveformView.clear();
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
        }
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
}
