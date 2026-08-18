package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public final class TunerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double cents = 0.0;
    private boolean hasPitch = false;
    private String targetLabel = "";
    private double stabilityPercent = Double.NaN;

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
        this.cents = cents;
        this.hasPitch = hasPitch;
        this.targetLabel = targetLabel == null ? "" : targetLabel;
        this.stabilityPercent = stabilityPercent;
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

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFFFFF"));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.parseColor("#202124"));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(16.0f * density);
        canvas.drawText(targetLabel, centerX, 24.0f * density, paint);
        drawStabilityText(canvas, centerX, density);

        paint.setColor(Color.parseColor("#E2DED6"));
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
            paint.setColor(tick == 0 ? Color.parseColor("#1D7A6B") : Color.parseColor("#9B958C"));
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
        int color = hasPitch ? needleColor(Math.abs(cents)) : Color.parseColor("#9B958C");

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(x, barTop + barHeight / 2.0f, 9.0f * density, paint);

        paint.setStrokeWidth(3.0f * density);
        canvas.drawLine(x, barTop - 34.0f * density, x, barTop + 6.0f * density, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCentsText(Canvas canvas, float centerX, float height, float density) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18.0f * density);
        paint.setColor(hasPitch ? needleColor(Math.abs(cents)) : Color.parseColor("#6F6A61"));

        String text;
        if (hasPitch) {
            String sign = cents > 0.0 ? "+" : "";
            text = String.format(Locale.CHINA, "%s%.0f cent", sign, cents);
        } else {
            text = "等待稳定音高";
        }
        canvas.drawText(text, centerX, height - 18.0f * density, paint);
    }

    private void drawStabilityText(Canvas canvas, float centerX, float density) {
        if (Double.isNaN(stabilityPercent)) {
            return;
        }
        double clamped = Math.max(0.0, Math.min(100.0, stabilityPercent));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(14.0f * density);
        paint.setColor(stabilityColor(clamped));
        canvas.drawText(String.format(Locale.CHINA, "稳定度 %.0f%%", clamped), centerX, 42.0f * density, paint);
    }

    private static float mapCentToX(float cent, float left, float right) {
        return left + (cent + 50.0f) / 100.0f * (right - left);
    }

    private static int needleColor(double absCents) {
        if (absCents <= 12.0) {
            return Color.parseColor("#1D7A6B");
        }
        if (absCents <= 25.0) {
            return Color.parseColor("#B45F06");
        }
        return Color.parseColor("#B3261E");
    }

    private static int stabilityColor(double percent) {
        if (percent >= 80.0) {
            return Color.parseColor("#1D7A6B");
        }
        if (percent >= 60.0) {
            return Color.parseColor("#B45F06");
        }
        return Color.parseColor("#B3261E");
    }
}
