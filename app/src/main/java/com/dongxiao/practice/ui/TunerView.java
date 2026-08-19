package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.dongxiao.practice.music.TargetNote;

import java.util.Locale;

public final class TunerView extends View {
    private static final int CHART_SURFACE = Color.argb(218, 248, 243, 230);
    private static final int INK = Color.rgb(25, 38, 50);
    private static final int MUTED = Color.rgb(111, 106, 97);
    private static final int GUIDE = Color.rgb(213, 202, 184);
    private static final int CENTER_GUIDE = Color.rgb(29, 122, 107);
    private static final int ACCENT = Color.rgb(29, 122, 107);
    private static final int WARNING = Color.rgb(180, 95, 6);
    private static final int DANGER = Color.rgb(179, 38, 30);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double cents = 0.0;
    private boolean hasPitch = false;
    private String targetLabel = "";
    private int targetDegree = 0;
    private int targetRegister = TargetNote.REGISTER_MIDDLE;
    private double stabilityPercent = Double.NaN;
    private double heldSeconds = Double.NaN;

    public TunerView(Context context) {
        super(context);
    }

    public TunerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setReading(double cents, boolean hasPitch, String targetLabel) {
        setReading(cents, hasPitch, targetLabel, Double.NaN);
    }

    public void setReading(double cents, boolean hasPitch, String targetLabel, double stabilityPercent) {
        setReading(cents, hasPitch, targetLabel, stabilityPercent, Double.NaN);
    }

    public void setReading(
            double cents,
            boolean hasPitch,
            String targetLabel,
            double stabilityPercent,
            double heldSeconds
    ) {
        this.cents = cents;
        this.hasPitch = hasPitch;
        this.targetLabel = targetLabel == null ? "" : targetLabel;
        this.targetDegree = 0;
        this.targetRegister = TargetNote.REGISTER_MIDDLE;
        this.stabilityPercent = stabilityPercent;
        this.heldSeconds = heldSeconds;
        invalidate();
    }

    public void setReading(double cents, boolean hasPitch, TargetNote targetNote) {
        setReading(cents, hasPitch, targetNote, Double.NaN);
    }

    public void setReading(double cents, boolean hasPitch, TargetNote targetNote, double stabilityPercent) {
        setReading(cents, hasPitch, targetNote, stabilityPercent, Double.NaN);
    }

    public void setReading(
            double cents,
            boolean hasPitch,
            TargetNote targetNote,
            double stabilityPercent,
            double heldSeconds
    ) {
        this.cents = cents;
        this.hasPitch = hasPitch;
        this.targetLabel = targetNote == null ? "" : "目标 ";
        this.targetDegree = targetNote == null ? 0 : targetNote.scaleDegree;
        this.targetRegister = targetNote == null ? TargetNote.REGISTER_MIDDLE : targetNote.register;
        this.stabilityPercent = stabilityPercent;
        this.heldSeconds = heldSeconds;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float padding = 18.0f * density;
        float centerX = width / 2.0f;
        float barTop = height * 0.48f;
        float barHeight = 10.0f * density;
        float barLeft = padding;
        float barRight = width - padding;
        float cornerRadius = 12.0f * density;
        float topInfoBaseline = 22.0f * density;
        float topInfoTextSize = 14.0f * density;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CHART_SURFACE);
        canvas.drawRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, paint);

        paint.setColor(INK);
        paint.setTextSize(topInfoTextSize);
        drawTopMetrics(canvas, width, padding, topInfoBaseline, topInfoTextSize);
        paint.setColor(INK);
        paint.setTextSize(topInfoTextSize);
        drawTargetLabel(canvas, centerX, topInfoBaseline);

        paint.setColor(GUIDE);
        RectF bar = new RectF(barLeft, barTop, barRight, barTop + barHeight);
        canvas.drawRoundRect(bar, barHeight / 2.0f, barHeight / 2.0f, paint);

        drawTicks(canvas, barLeft, barRight, barTop, density);
        drawNeedle(canvas, barLeft, barRight, barTop, barHeight, density);
        drawCentsText(canvas, centerX, height, density);
    }

    private void drawTicks(Canvas canvas, float barLeft, float barRight, float barTop, float density) {
        paint.setStrokeWidth(1.0f * density);
        paint.setTextSize(11.0f * density);
        paint.setTextAlign(Paint.Align.CENTER);

        int[] tickValues = {-50, -25, 0, 25, 50};
        for (int tick : tickValues) {
            float x = mapCentToX(tick, barLeft, barRight);
            paint.setColor(tick == 0 ? CENTER_GUIDE : MUTED);
            float top = tick == 0 ? barTop - 18.0f * density : barTop - 10.0f * density;
            float bottom = barTop + 20.0f * density;
            canvas.drawLine(x, top, x, bottom, paint);
            canvas.drawText(String.valueOf(tick), x, barTop + 38.0f * density, paint);
        }
    }

    private void drawNeedle(
            Canvas canvas,
            float barLeft,
            float barRight,
            float barTop,
            float barHeight,
            float density
    ) {
        float clampedCents = (float) Math.max(-50.0, Math.min(50.0, cents));
        float x = hasPitch ? mapCentToX(clampedCents, barLeft, barRight) : (barLeft + barRight) / 2.0f;
        int color = hasPitch ? needleColor(Math.abs(cents)) : MUTED;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(x, barTop + barHeight / 2.0f, 9.0f * density, paint);

        paint.setStrokeWidth(3.0f * density);
        canvas.drawLine(x, barTop - 24.0f * density, x, barTop + 6.0f * density, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCentsText(Canvas canvas, float centerX, float height, float density) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18.0f * density);
        paint.setColor(hasPitch ? needleColor(Math.abs(cents)) : MUTED);

        String text;
        if (hasPitch) {
            String sign = cents > 0.0 ? "+" : "";
            text = String.format(Locale.CHINA, "%s%.0f cent", sign, cents);
        } else {
            text = "等待稳定音高";
        }
        canvas.drawText(text, centerX, height - 18.0f * density, paint);
    }

    private void drawTargetLabel(Canvas canvas, float centerX, float baseline) {
        paint.setTextAlign(Paint.Align.CENTER);
        if (targetDegree <= 0) {
            canvas.drawText(targetLabel, centerX, baseline, paint);
            return;
        }

        float prefixWidth = paint.measureText(targetLabel);
        float noteWidth = JianpuNoteSpan.measureWidth(paint, targetDegree);
        float left = centerX - (prefixWidth + noteWidth) / 2.0f;
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(targetLabel, left, baseline, paint);
        JianpuNoteSpan.drawCentered(
                canvas,
                paint,
                targetDegree,
                targetRegister,
                left + prefixWidth + noteWidth / 2.0f,
                baseline
        );
    }

    private void drawTopMetrics(
            Canvas canvas,
            float width,
            float padding,
            float baseline,
            float textSize
    ) {
        if (Double.isNaN(stabilityPercent) && Double.isNaN(heldSeconds)) {
            return;
        }
        paint.setTextSize(textSize);
        if (!Double.isNaN(heldSeconds)) {
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(ACCENT);
            canvas.drawText(
                    String.format(Locale.CHINA, "命中 %.1fs", Math.max(0.0, heldSeconds)),
                    padding,
                    baseline,
                    paint
            );
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        String text = "稳定 --";
        int color = MUTED;
        if (!Double.isNaN(stabilityPercent)) {
            double clamped = Math.max(0.0, Math.min(100.0, stabilityPercent));
            text = String.format(Locale.CHINA, "稳定 %.0f%%", clamped);
            color = stabilityColor(clamped);
        }
        paint.setColor(color);
        canvas.drawText(text, width - padding, baseline, paint);
    }

    private static float mapCentToX(float cent, float left, float right) {
        return left + (cent + 50.0f) / 100.0f * (right - left);
    }

    private static int needleColor(double absCents) {
        if (absCents <= 12.0) {
            return ACCENT;
        }
        if (absCents <= 25.0) {
            return WARNING;
        }
        return DANGER;
    }

    private static int stabilityColor(double percent) {
        if (percent >= 80.0) {
            return ACCENT;
        }
        if (percent >= 60.0) {
            return WARNING;
        }
        return DANGER;
    }
}
