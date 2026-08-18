package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public final class WaveformView extends View {
    private static final float WINDOW_SECONDS = 4.0f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] timelineSamples = new float[0];
    private int timelineSampleRate = 0;
    private int writeIndex = 0;
    private int sampleCount = 0;

    public WaveformView(Context context) {
        super(context);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSamples(float[] samples) {
        appendSamples(samples, timelineSampleRate > 0 ? timelineSampleRate : 44100);
    }

    public void appendSamples(float[] samples, int sampleRate) {
        if (samples == null || samples.length == 0 || sampleRate <= 0) {
            return;
        }
        ensureCapacity(sampleRate);
        for (float sample : samples) {
            timelineSamples[writeIndex] = clamp(sample);
            writeIndex = (writeIndex + 1) % timelineSamples.length;
            if (sampleCount < timelineSamples.length) {
                sampleCount++;
            }
        }
        invalidate();
    }

    public void clear() {
        writeIndex = 0;
        sampleCount = 0;
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
        float bottom = height - padding - 16.0f * density;
        float midY = (top + bottom) / 2.0f;
        float halfHeight = Math.max(1.0f, (bottom - top) / 2.0f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFFFFF"));
        canvas.drawRect(0, 0, width, height, paint);

        drawAmplitudeAxis(canvas, axisX, left, right, top, midY, bottom, density);
        drawTimeAxis(canvas, left, right, bottom, density);

        paint.setColor(Color.parseColor("#E8E1D6"));
        paint.setStrokeWidth(1.0f * density);
        canvas.drawLine(left, midY, right, midY, paint);

        if (sampleCount < 2 || timelineSamples.length == 0 || width <= left + padding) {
            paint.setColor(Color.parseColor("#9B958C"));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13.0f * density);
            canvas.drawText("等待音频波形", width / 2.0f, midY - 8.0f * density, paint);
            return;
        }

        float rmsHeight = Math.min(1.0f, calculateRms()) * halfHeight;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(38, 29, 122, 107));
        canvas.drawRect(left, midY - rmsHeight, right, midY + rmsHeight, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.parseColor("#1D7A6B"));

        int points = Math.max(2, (int) (right - left));
        int maxSamples = timelineSamples.length;
        int emptySamples = maxSamples - sampleCount;
        for (int point = 0; point < points; point++) {
            int start = point * maxSamples / points - emptySamples;
            int end = Math.max(start + 1, (point + 1) * maxSamples / points - emptySamples);
            if (end <= 0 || start >= sampleCount) {
                continue;
            }
            start = Math.max(0, start);
            end = Math.min(sampleCount, end);
            float min = 1.0f;
            float max = -1.0f;
            for (int i = start; i < end; i++) {
                float sample = sampleAtOffset(i);
                min = Math.min(min, sample);
                max = Math.max(max, sample);
            }
            float x = left + (right - left) * point / (points - 1.0f);
            canvas.drawLine(x, midY - max * halfHeight, x, midY - min * halfHeight, paint);
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void ensureCapacity(int sampleRate) {
        int capacity = Math.max(1, Math.round(sampleRate * WINDOW_SECONDS));
        if (timelineSamples.length == capacity && timelineSampleRate == sampleRate) {
            return;
        }
        timelineSamples = new float[capacity];
        timelineSampleRate = sampleRate;
        writeIndex = 0;
        sampleCount = 0;
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

    private void drawTimeAxis(Canvas canvas, float left, float right, float bottom, float density) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f * density);
        paint.setColor(Color.parseColor("#D8D0C4"));
        canvas.drawLine(left, bottom, right, bottom, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(10.0f * density);
        paint.setColor(Color.parseColor("#7A7167"));
        float labelY = bottom + 13.0f * density;
        canvas.drawText(String.format("-%.0fs", WINDOW_SECONDS), left, labelY, paint);
        canvas.drawText(String.format("-%.0fs", WINDOW_SECONDS / 2.0f), (left + right) / 2.0f, labelY, paint);
        canvas.drawText("0s", right, labelY, paint);
    }

    private float calculateRms() {
        double sum = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            float sample = sampleAtOffset(i);
            sum += sample * sample;
        }
        return sampleCount == 0 ? 0.0f : (float) Math.sqrt(sum / sampleCount);
    }

    private float sampleAtOffset(int offset) {
        int oldestIndex = writeIndex - sampleCount;
        if (oldestIndex < 0) {
            oldestIndex += timelineSamples.length;
        }
        return timelineSamples[(oldestIndex + offset) % timelineSamples.length];
    }

    private static float clamp(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }
}
