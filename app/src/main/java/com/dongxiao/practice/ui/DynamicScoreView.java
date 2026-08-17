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
    private static final int VISIBLE_ROWS = 3;
    private static final double EPSILON = 0.03;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        notePaint.setStyle(Paint.Style.FILL);
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

        drawPaper(canvas, width, height);
        if (song == null || song.notes.isEmpty()) {
            drawEmpty(canvas, width, height);
            return;
        }

        drawHeader(canvas, width);
        drawScoreRows(canvas, width, height);
        drawProgress(canvas, width, height);
    }

    private void drawPaper(Canvas canvas, int width, int height) {
        linePaint.setColor(color(R.color.line));
        linePaint.setStrokeWidth(dp(1));
        for (int y = dp(34); y < height - dp(10); y += dp(24)) {
            canvas.drawLine(dp(12), y, width - dp(12), y, linePaint);
        }
    }

    private void drawHeader(Canvas canvas, int width) {
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dp(11));
        textPaint.setColor(color(R.color.ink));
        String keyText = "1=" + keyName(song.keyLabel);
        String meterText = song.meterBeats + "/4";
        String tempoText = song.tempoBpm + " BPM";
        canvas.drawText(keyText + "   " + meterText + "   " + tempoText, width / 2.0f, dp(18), textPaint);

        textPaint.setTextSize(dp(9));
        textPaint.setColor(color(R.color.muted));
        canvas.drawText("动态跟随", width / 2.0f, dp(31), textPaint);
    }

    private void drawScoreRows(Canvas canvas, int width, int height) {
        float left = dp(12);
        float right = width - dp(12);
        float top = dp(40);
        float bottom = height - dp(24);
        float rowHeight = (bottom - top) / VISIBLE_ROWS;
        double beatsPerMeasure = Math.max(1, song.meterBeats);
        double beatsPerRow = beatsPerMeasure * visibleMeasuresPerRow(width);
        double visibleStart = visibleStartBeat(beatsPerMeasure, beatsPerRow * VISIBLE_ROWS);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            double rowStart = visibleStart + row * beatsPerRow;
            float rowTop = top + row * rowHeight;
            drawMeasureLines(canvas, rowStart, beatsPerRow, beatsPerMeasure, left, right, rowTop, rowHeight);
        }

        double noteStart = 0.0;
        for (int i = 0; i < song.notes.size(); i++) {
            SongNote note = song.notes.get(i);
            double noteEnd = noteStart + note.beats;
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                double rowStart = visibleStart + row * beatsPerRow;
                double rowEnd = rowStart + beatsPerRow;
                if (noteStart < rowEnd && noteEnd > rowStart) {
                    float rowTop = top + row * rowHeight;
                    drawNote(canvas, note, i, noteStart, noteEnd, rowStart, beatsPerRow, left, right, rowTop, rowHeight);
                }
            }
            noteStart = noteEnd;
        }
    }

    private void drawMeasureLines(
            Canvas canvas,
            double rowStart,
            double beatsPerRow,
            double beatsPerMeasure,
            float left,
            float right,
            float rowTop,
            float rowHeight
    ) {
        linePaint.setStrokeWidth(dp(1));
        linePaint.setColor(color(R.color.line));

        int firstMeasure = (int) Math.ceil(rowStart / beatsPerMeasure);
        int lastMeasure = (int) Math.floor((rowStart + beatsPerRow) / beatsPerMeasure);
        for (int measure = firstMeasure; measure <= lastMeasure; measure++) {
            double beat = measure * beatsPerMeasure;
            float x = beatToX(beat, rowStart, beatsPerRow, left, right);
            canvas.drawLine(x, rowTop + dp(4), x, rowTop + rowHeight - dp(4), linePaint);
        }
    }

    private void drawNote(
            Canvas canvas,
            SongNote note,
            int index,
            double noteStart,
            double noteEnd,
            double rowStart,
            double beatsPerRow,
            float left,
            float right,
            float rowTop,
            float rowHeight
    ) {
        float xStart = beatToX(Math.max(noteStart, rowStart), rowStart, beatsPerRow, left, right);
        float xEnd = beatToX(Math.min(noteEnd, rowStart + beatsPerRow), rowStart, beatsPerRow, left, right);
        float centerX = (xStart + xEnd) / 2.0f;
        float digitY = rowTop + rowHeight * 0.55f;
        boolean current = index == currentNoteIndex;
        boolean passed = currentNoteIndex >= 0 && index < currentNoteIndex;
        int noteColor = current ? color(R.color.cinnabar) : passed ? color(R.color.accent_dark) : color(R.color.ink);

        if (current) {
            rect.set(centerX - dp(13), digitY - dp(22), centerX + dp(13), digitY + dp(8));
            notePaint.setColor(color(R.color.accent_soft));
            canvas.drawOval(rect, notePaint);
            linePaint.setColor(color(R.color.cinnabar));
            linePaint.setStrokeWidth(dp(1.5f));
            canvas.drawLine(centerX, rowTop + dp(5), centerX, rowTop + rowHeight - dp(4), linePaint);
        }

        drawJianpuLabel(canvas, note, centerX, digitY, noteColor);
        drawOctaveDots(canvas, note, centerX, digitY, noteColor);
        drawDurationMarks(canvas, note, xStart, xEnd, centerX, digitY, noteColor);
    }

    private void drawJianpuLabel(Canvas canvas, SongNote note, float centerX, float digitY, int color) {
        String label = note.rest ? "0" : note.label;
        String accidental = "";
        String digit = label;
        if (label.length() > 1 && (label.startsWith("#") || label.startsWith("b"))) {
            accidental = label.substring(0, 1);
            digit = label.substring(1);
        }

        textPaint.setFakeBoldText(true);
        textPaint.setColor(note.rest ? color(R.color.muted) : color);
        if (!accidental.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setTextSize(dp(10));
            canvas.drawText(accidental, centerX - dp(4), digitY - dp(5), textPaint);
            centerX += dp(3);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(18));
        canvas.drawText(digit, centerX, digitY, textPaint);
    }

    private void drawOctaveDots(Canvas canvas, SongNote note, float centerX, float digitY, int color) {
        if (note.rest) {
            return;
        }
        int octaveOffset = octaveOffset(note);
        int dots = Math.min(3, Math.abs(octaveOffset));
        if (dots == 0) {
            return;
        }

        notePaint.setColor(color);
        for (int i = 0; i < dots; i++) {
            float y = octaveOffset > 0
                    ? digitY - dp(22 + i * 5)
                    : digitY + dp(7 + i * 5);
            canvas.drawCircle(centerX, y, dp(1.5f), notePaint);
        }
    }

    private void drawDurationMarks(
            Canvas canvas,
            SongNote note,
            float xStart,
            float xEnd,
            float centerX,
            float digitY,
            int color
    ) {
        linePaint.setColor(color);
        linePaint.setStrokeWidth(dp(1.5f));

        int underlineCount = underlineCount(note.beats);
        float underlineY = digitY + dp(octaveOffset(note) < 0 ? 18 : 12);
        for (int i = 0; i < underlineCount; i++) {
            canvas.drawLine(centerX - dp(8), underlineY + dp(i * 4), centerX + dp(8), underlineY + dp(i * 4), linePaint);
        }

        if (note.beats > 1.0 + EPSILON) {
            float dashStart = Math.max(centerX + dp(12), xStart + dp(14));
            float dashEnd = Math.max(dashStart + dp(8), xEnd - dp(4));
            canvas.drawLine(dashStart, digitY - dp(6), dashEnd, digitY - dp(6), linePaint);
        }

        if (isDottedDuration(note.beats)) {
            notePaint.setColor(color);
            canvas.drawCircle(centerX + dp(10), digitY - dp(5), dp(1.5f), notePaint);
        }
    }

    private void drawProgress(Canvas canvas, int width, int height) {
        double totalBeats = Math.max(1.0, song.totalBeats());
        float progress = (float) Math.min(1.0, beatPosition / totalBeats);
        float left = dp(14);
        float right = width - dp(14);
        float y = height - dp(10);
        linePaint.setStrokeWidth(dp(3));
        linePaint.setColor(color(R.color.line));
        canvas.drawLine(left, y, right, y, linePaint);
        linePaint.setColor(color(R.color.cinnabar));
        canvas.drawLine(left, y, left + (right - left) * progress, y, linePaint);
    }

    private void drawEmpty(Canvas canvas, int width, int height) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(dp(14));
        textPaint.setColor(color(R.color.muted));
        canvas.drawText("请选择曲目", width / 2.0f, height / 2.0f, textPaint);
    }

    private double visibleStartBeat(double beatsPerMeasure, double visibleBeats) {
        double totalBeats = song.totalBeats();
        double activeBeat = currentNoteIndex >= 0 ? beatPosition : 0.0;
        int currentMeasure = (int) Math.floor(activeBeat / beatsPerMeasure);
        double start = Math.max(0.0, (currentMeasure - 1) * beatsPerMeasure);
        if (start + visibleBeats > totalBeats) {
            start = Math.max(0.0, totalBeats - visibleBeats);
        }
        return start;
    }

    private int visibleMeasuresPerRow(int width) {
        if (width < dp(520)) {
            return 2;
        }
        return 3;
    }

    private float beatToX(double beat, double rowStart, double beatsPerRow, float left, float right) {
        double fraction = (beat - rowStart) / beatsPerRow;
        return (float) (left + Math.max(0.0, Math.min(1.0, fraction)) * (right - left));
    }

    private int octaveOffset(SongNote note) {
        if (song == null || note.rest) {
            return 0;
        }
        return Math.floorDiv(note.midi - song.rootMidi, 12);
    }

    private int underlineCount(double beats) {
        if (beats <= 0.125 + EPSILON) {
            return 3;
        }
        if (beats <= 0.25 + EPSILON) {
            return 2;
        }
        if (beats <= 0.5 + EPSILON) {
            return 1;
        }
        return 0;
    }

    private boolean isDottedDuration(double beats) {
        return near(beats, 0.375) || near(beats, 0.75) || near(beats, 1.5);
    }

    private boolean near(double value, double target) {
        return Math.abs(value - target) < EPSILON;
    }

    private String keyName(String keyLabel) {
        if (keyLabel == null || keyLabel.isEmpty()) {
            return "?";
        }
        return keyLabel.replace("调", "");
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
