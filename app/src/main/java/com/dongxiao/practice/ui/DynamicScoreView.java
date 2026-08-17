package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.dongxiao.practice.R;
import com.dongxiao.practice.song.PracticeSong;
import com.dongxiao.practice.song.SongNote;

public final class DynamicScoreView extends View {
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private PracticeSong song;
    private double beatPosition = 0.0;
    private int currentNoteIndex = -1;

    public DynamicScoreView(Context context) {
        super(context);
        init();
    }

    public DynamicScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setSong(PracticeSong song) {
        this.song = song;
        this.beatPosition = 0.0;
        this.currentNoteIndex = -1;
        invalidate();
    }

    public void setProgress(double beatPosition, int currentNoteIndex) {
        this.beatPosition = Math.max(0.0, beatPosition);
        this.currentNoteIndex = currentNoteIndex;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        drawPaperLines(canvas, width, height);
        if (song == null || song.notes.isEmpty()) {
            drawEmpty(canvas, width, height);
            return;
        }

        int columns = Math.max(4, Math.min(8, width / dp(54)));
        float gap = dp(6);
        float cellWidth = (width - gap * (columns + 1)) / columns;
        float cellHeight = dp(46);
        float top = dp(20);

        for (int i = 0; i < song.notes.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            float left = gap + col * (cellWidth + gap);
            float cellTop = top + row * (cellHeight + gap);
            drawNote(canvas, song.notes.get(i), i, left, cellTop, cellWidth, cellHeight);
        }
        drawProgress(canvas, width, height);
    }

    private void drawPaperLines(Canvas canvas, int width, int height) {
        linePaint.setColor(color(R.color.line));
        linePaint.setStrokeWidth(dp(1));
        for (int y = dp(28); y < height - dp(10); y += dp(34)) {
            canvas.drawLine(dp(10), y, width - dp(10), y, linePaint);
        }
    }

    private void drawNote(Canvas canvas, SongNote note, int index, float left, float top, float width, float height) {
        boolean current = index == currentNoteIndex;
        boolean passed = index < currentNoteIndex;
        rect.set(left, top, left + width, top + height);
        cellPaint.setStyle(Paint.Style.FILL);
        cellPaint.setColor(current ? color(R.color.cinnabar) : passed ? color(R.color.accent_soft) : color(R.color.paper));
        canvas.drawRoundRect(rect, dp(8), dp(8), cellPaint);
        cellPaint.setStyle(Paint.Style.STROKE);
        cellPaint.setStrokeWidth(current ? dp(2) : dp(1));
        cellPaint.setColor(current ? color(R.color.cinnabar_dark) : color(R.color.gold));
        canvas.drawRoundRect(rect, dp(8), dp(8), cellPaint);

        textPaint.setTextSize(dp(15));
        textPaint.setColor(current ? color(R.color.paper) : color(R.color.ink));
        canvas.drawText(note.label, left + width / 2.0f, top + dp(22), textPaint);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(current ? color(R.color.paper_deep) : color(R.color.muted));
        canvas.drawText(beatText(note.beats), left + width / 2.0f, top + dp(38), textPaint);
    }

    private void drawProgress(Canvas canvas, int width, int height) {
        double totalBeats = Math.max(1.0, song.totalBeats());
        float progress = (float) Math.min(1.0, beatPosition / totalBeats);
        float left = dp(12);
        float right = width - dp(12);
        float y = height - dp(12);
        linePaint.setStrokeWidth(dp(4));
        linePaint.setColor(color(R.color.line));
        canvas.drawLine(left, y, right, y, linePaint);
        linePaint.setColor(color(R.color.cinnabar));
        canvas.drawLine(left, y, left + (right - left) * progress, y, linePaint);
    }

    private void drawEmpty(Canvas canvas, int width, int height) {
        textPaint.setTextSize(dp(14));
        textPaint.setColor(color(R.color.muted));
        canvas.drawText("请选择曲目", width / 2.0f, height / 2.0f, textPaint);
    }

    private String beatText(double beats) {
        if (Math.abs(beats - Math.round(beats)) < 0.01) {
            return ((int) Math.round(beats)) + "拍";
        }
        return beats + "拍";
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
