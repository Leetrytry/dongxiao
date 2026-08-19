package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.dongxiao.practice.R;
import com.dongxiao.practice.music.TargetNote;
import com.dongxiao.practice.practice.ScalePracticeProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ScaleScoreView extends View {
    private static final int FALLBACK_ROW_NOTES = 8;
    private static final double REQUIRED_HIT_SECONDS = 0.36;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<TargetNote> sequence = new ArrayList<>();
    private final List<Integer> sectionStarts = new ArrayList<>();
    private final List<String> sectionLabels = new ArrayList<>();

    private int currentIndex = 0;
    private int completedNotes = 0;
    private int wrongAttempts = 0;
    private double currentHitSeconds = 0.0;
    private boolean completed = false;

    public ScaleScoreView(Context context) {
        super(context);
        init();
    }

    public ScaleScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        notePaint.setStyle(Paint.Style.FILL);
        setContentDescription("音阶乐谱准备中");
    }

    public void setProgress(ScalePracticeProgress progress) {
        sequence.clear();
        sectionStarts.clear();
        sectionLabels.clear();
        if (progress != null && progress.sequence != null) {
            sequence.addAll(progress.sequence);
            if (progress.sectionStarts != null && !progress.sectionStarts.isEmpty()) {
                sectionStarts.addAll(progress.sectionStarts);
            }
            if (progress.sectionLabels != null && !progress.sectionLabels.isEmpty()) {
                sectionLabels.addAll(progress.sectionLabels);
            }
            currentIndex = progress.currentIndex;
            completedNotes = progress.completedNotes;
            wrongAttempts = progress.wrongAttempts;
            currentHitSeconds = progress.currentHitSeconds;
            completed = progress.completed;
            updateContentDescription(progress);
        } else {
            currentIndex = 0;
            completedNotes = 0;
            wrongAttempts = 0;
            currentHitSeconds = 0.0;
            completed = false;
            setContentDescription("音阶乐谱准备中");
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || sequence.isEmpty()) {
            return;
        }

        drawPaperLines(canvas, width, height);
        List<Integer> starts = rowStarts();
        int rowCount = starts.size();
        for (int row = 0; row < rowCount; row++) {
            int start = starts.get(row);
            int end = row == rowCount - 1 ? sequence.size() : starts.get(row + 1);
            drawRow(canvas, start, end, row, rowCount, width, height);
        }
        drawOverallProgress(canvas, width, height);
    }

    private void drawPaperLines(Canvas canvas, int width, int height) {
        linePaint.setStrokeWidth(dp(1));
        linePaint.setColor(color(R.color.line));
        float left = dp(48);
        float right = width - dp(14);
        float top = dp(10);
        float bottom = height - dp(14);
        int rowCount = rowStarts().size();
        float rowHeight = (bottom - top) / Math.max(1, rowCount);
        for (int row = 0; row < rowCount; row++) {
            float y = top + row * rowHeight + rowHeight * 0.56f;
            canvas.drawLine(left, y, right, y, linePaint);
        }
    }

    private void drawRow(Canvas canvas, int start, int end, int row, int rowCount, int width, int height) {
        float left = dp(52);
        float right = width - dp(16);
        float top = dp(8);
        float bottom = height - dp(16);
        float rowHeight = (bottom - top) / Math.max(1, rowCount);
        float rowTop = top + row * rowHeight;
        int count = Math.max(1, end - start);
        float cellWidth = (right - left) / count;

        drawSectionLabel(canvas, row, rowTop, rowHeight);
        drawMeasureBars(canvas, left, rowTop, rowHeight, cellWidth, count);
        for (int i = start; i < end; i++) {
            int indexInRow = i - start;
            float centerX = left + (indexInRow + 0.5f) * cellWidth;
            float baseline = rowTop + rowHeight * 0.68f;
            drawNote(canvas, sequence.get(i), i, centerX, baseline, cellWidth);
        }
    }

    private void drawSectionLabel(Canvas canvas, int row, float rowTop, float rowHeight) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dp(10.5f));
        textPaint.setColor(color(R.color.muted));
        String label = row < sectionLabels.size() ? sectionLabels.get(row) : "练习";
        canvas.drawText(label, dp(12), rowTop + rowHeight * 0.60f, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawMeasureBars(
            Canvas canvas,
            float left,
            float rowTop,
            float rowHeight,
            float cellWidth,
            int count
    ) {
        linePaint.setStrokeWidth(dp(1));
        linePaint.setColor(color(R.color.line));
        float barTop = rowTop + dp(7);
        float barBottom = rowTop + rowHeight - dp(5);
        for (int i = 0; i <= count; i++) {
            if (i != 0 && i != count && i % 4 != 0) {
                continue;
            }
            float x = left + i * cellWidth;
            canvas.drawLine(x, barTop, x, barBottom, linePaint);
        }
    }

    private void drawNote(
            Canvas canvas,
            TargetNote note,
            int index,
            float centerX,
            float baseline,
            float cellWidth
    ) {
        boolean isCurrent = !completed && index == currentIndex;
        boolean isCompleted = completed || index < completedNotes;
        int noteColor = isCurrent
                ? color(R.color.cinnabar)
                : isCompleted ? color(R.color.accent_dark) : color(R.color.ink);

        if (isCurrent) {
            rect.set(
                    centerX - Math.min(dp(16), cellWidth * 0.38f),
                    baseline - dp(24),
                    centerX + Math.min(dp(16), cellWidth * 0.38f),
                    baseline + dp(8)
            );
            notePaint.setColor(color(R.color.cinnabar_soft));
            canvas.drawOval(rect, notePaint);
            linePaint.setColor(color(R.color.cinnabar));
            linePaint.setStrokeWidth(dp(1.5f));
            canvas.drawLine(centerX, baseline - dp(27), centerX, baseline + dp(11), linePaint);
        }

        textPaint.setTextSize(dp(19.5f));
        textPaint.setColor(noteColor);
        textPaint.setFakeBoldText(true);
        JianpuNoteSpan.drawCentered(canvas, textPaint, note.scaleDegree, note.register, centerX, baseline);

        if (isCompleted) {
            notePaint.setColor(color(R.color.accent));
            canvas.drawCircle(centerX, baseline + dp(15), dp(2.0f), notePaint);
        } else if (isCurrent) {
            drawHitProgress(canvas, centerX, baseline, cellWidth);
        }
    }

    private void drawHitProgress(Canvas canvas, float centerX, float baseline, float cellWidth) {
        float progress = (float) Math.max(0.0, Math.min(1.0, currentHitSeconds / REQUIRED_HIT_SECONDS));
        float lineWidth = Math.min(dp(28), cellWidth * 0.52f);
        float left = centerX - lineWidth / 2.0f;
        float right = centerX + lineWidth / 2.0f;
        float y = baseline + dp(14);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setColor(color(R.color.line));
        canvas.drawLine(left, y, right, y, linePaint);
        linePaint.setColor(color(R.color.cinnabar));
        canvas.drawLine(left, y, left + (right - left) * progress, y, linePaint);
    }

    private void drawOverallProgress(Canvas canvas, int width, int height) {
        float left = dp(18);
        float right = width - dp(18);
        float y = height - dp(7);
        float progress = sequence.isEmpty() ? 0.0f : Math.min(1.0f, completedNotes / (float) sequence.size());
        linePaint.setStrokeWidth(dp(3));
        linePaint.setColor(color(R.color.line));
        canvas.drawLine(left, y, right, y, linePaint);
        linePaint.setColor(color(R.color.accent));
        canvas.drawLine(left, y, left + (right - left) * progress, y, linePaint);
    }

    private List<Integer> rowStarts() {
        if (!sectionStarts.isEmpty()) {
            return sectionStarts;
        }
        List<Integer> starts = new ArrayList<>();
        for (int start = 0; start < sequence.size(); start += FALLBACK_ROW_NOTES) {
            starts.add(start);
        }
        if (starts.isEmpty()) {
            starts.add(0);
        }
        return starts;
    }

    private void updateContentDescription(ScalePracticeProgress progress) {
        if (progress.totalNotes <= 0 || progress.currentTarget == null) {
            setContentDescription("音阶乐谱准备中");
            return;
        }
        String text = progress.completed
                ? String.format(
                Locale.CHINA,
                "音阶乐谱已完成，共%d个音，误吹%d次",
                progress.totalNotes,
                progress.wrongAttempts
        )
                : String.format(
                Locale.CHINA,
                "音阶乐谱，第%d个，共%d个，当前音%d，误吹%d次",
                progress.currentIndex + 1,
                progress.totalNotes,
                progress.currentTarget.scaleDegree,
                progress.wrongAttempts
        );
        setContentDescription(text);
    }

    private int color(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(colorResId, null);
        }
        return getResources().getColor(colorResId);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
