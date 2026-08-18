package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public final class WaveformView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] samples = new float[0];

    public WaveformView(Context context) {
        super(context);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSamples(float[] samples) {
        this.samples = samples == null ? new float[0] : samples;
        invalidate();
    }

    public void clear() {
        samples = new float[0];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float padding = 14.0f * density;
        float axisX = 30.0f * density;
        float left = axisX + 12.0f * density;
        float right = width - padding;
        float top = padding;
        float bottom = height - padding;
        float midY = (top + bottom) / 2.0f;
        float halfHeight = Math.max(1.0f, (bottom - top) / 2.0f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFFFFF"));
        canvas.drawRect(0, 0, width, height, paint);

        drawAmplitudeAxis(canvas, axisX, left, right, top, midY, bottom, density);

        paint.setColor(Color.parseColor("#E8E1D6"));
        paint.setStrokeWidth(1.0f * density);
        canvas.drawLine(left, midY, right, midY, paint);

        if (samples.length < 2 || width <= left + padding) {
            paint.setColor(Color.parseColor("#9B958C"));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13.0f * density);
            canvas.drawText("等待音频波形", width / 2.0f, midY - 8.0f * density, paint);
            return;
        }

        float rmsHeight = Math.min(1.0f, calculateRms(samples)) * halfHeight;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(38, 29, 122, 107));
        canvas.drawRect(left, midY - rmsHeight, right, midY + rmsHeight, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.parseColor("#1D7A6B"));

        int points = Math.max(2, (int) (right - left));
        for (int point = 0; point < points; point++) {
            int start = point * samples.length / points;
            int end = Math.max(start + 1, (point + 1) * samples.length / points);
            float min = 1.0f;
            float max = -1.0f;
            for (int i = start; i < end && i < samples.length; i++) {
                float sample = clamp(samples[i]);
                min = Math.min(min, sample);
                max = Math.max(max, sample);
            }
            float x = left + (right - left) * point / (points - 1.0f);
            canvas.drawLine(x, midY - max * halfHeight, x, midY - min * halfHeight, paint);
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawAmplitudeAxis(
            Canvas canvas,
            float axisX,
            float left,
            float right,
            float top,
            float midY,
            float bottom,
            float density
    ) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f * density);
        paint.setColor(Color.parseColor("#D8D0C4"));
        canvas.drawLine(axisX, top, axisX, bottom, paint);
        canvas.drawLine(axisX - 3.0f * density, top, left, top, paint);
        canvas.drawLine(axisX - 3.0f * density, midY, right, midY, paint);
        canvas.drawLine(axisX - 3.0f * density, bottom, left, bottom, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(10.0f * density);
        paint.setColor(Color.parseColor("#7A7167"));
        float labelX = axisX - 5.0f * density;
        canvas.drawText("+1", labelX, top + 4.0f * density, paint);
        canvas.drawText("0", labelX, midY + 4.0f * density, paint);
        canvas.drawText("-1", labelX, bottom, paint);
    }

    private static float calculateRms(float[] samples) {
        double sum = 0.0;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    private static float clamp(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }
}
