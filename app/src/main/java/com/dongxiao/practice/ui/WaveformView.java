package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public final class WaveformView extends View {
    private static final float WINDOW_SECONDS = 0.012f;
    private static final float TRACE_POINTS_PER_PIXEL = 0.55f;
    private static final float TRACE_VERTICAL_SCALE = 1.8f;
    private static final int TRACE_SMOOTHING_RADIUS = 2;
    private static final int CHART_SURFACE = Color.argb(218, 248, 243, 230);
    private static final int GUIDE = Color.rgb(214, 202, 184);
    private static final int GUIDE_SOFT = Color.rgb(226, 216, 199);
    private static final int MUTED = Color.rgb(111, 106, 97);
    private static final int ACCENT = Color.rgb(29, 122, 107);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path waveformPath = new Path();
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
        float plotTopPadding = 8.0f * density;
        float timeLabelSpace = 18.0f * density;
        float axisX = 30.0f * density;
        float left = axisX + 12.0f * density;
        float right = width - padding;
        float top = plotTopPadding;
        float bottom = height - timeLabelSpace;
        float midY = (top + bottom) / 2.0f;
        float halfHeight = Math.max(1.0f, (bottom - top) / 2.0f);
        float cornerRadius = 12.0f * density;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CHART_SURFACE);
        canvas.drawRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, paint);

        drawAmplitudeAxis(canvas, axisX, left, right, top, midY, bottom, density);
        drawTimeAxis(canvas, left, right, bottom, density);

        paint.setColor(GUIDE_SOFT);
        paint.setStrokeWidth(1.0f * density);
        canvas.drawLine(left, midY, right, midY, paint);

        if (sampleCount < 2 || timelineSamples.length == 0 || width <= left + padding) {
            paint.setColor(MUTED);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13.0f * density);
            canvas.drawText("等待音频波形", width / 2.0f, midY - 8.0f * density, paint);
            return;
        }

        float rmsHeight = Math.min(1.0f, calculateRms() * TRACE_VERTICAL_SCALE) * halfHeight;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(32, 29, 122, 107));
        canvas.drawRect(left, midY - rmsHeight, right, midY + rmsHeight, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        int points = Math.min(
                sampleCount,
                Math.max(2, (int) ((right - left) * TRACE_POINTS_PER_PIXEL))
        );
        waveformPath.reset();
        float previousX = left;
        float previousY = sampleToY(0, midY, halfHeight);
        waveformPath.moveTo(previousX, previousY);
        for (int point = 1; point < points; point++) {
            int sampleOffset = point * (sampleCount - 1) / (points - 1);
            float x = left + (right - left) * point / (points - 1.0f);
            float y = sampleToY(sampleOffset, midY, halfHeight);
            float midPointX = (previousX + x) / 2.0f;
            float midPointY = (previousY + y) / 2.0f;
            waveformPath.quadTo(previousX, previousY, midPointX, midPointY);
            previousX = x;
            previousY = y;
        }
        waveformPath.lineTo(previousX, previousY);

        paint.setStrokeWidth(4.0f * density);
        paint.setColor(Color.argb(48, 29, 122, 107));
        canvas.drawPath(waveformPath, paint);

        paint.setStrokeWidth(2.2f * density);
        paint.setColor(ACCENT);
        canvas.drawPath(waveformPath, paint);

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
        paint.setColor(GUIDE);
        canvas.drawLine(axisX, top, axisX, bottom, paint);
        canvas.drawLine(axisX - 3.0f * density, top, left, top, paint);
        canvas.drawLine(axisX - 3.0f * density, midY, right, midY, paint);
        canvas.drawLine(axisX - 3.0f * density, bottom, left, bottom, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(10.0f * density);
        paint.setColor(MUTED);
        float labelX = axisX - 5.0f * density;
        canvas.drawText("+1", labelX, top + 4.0f * density, paint);
        canvas.drawText("0", labelX, midY + 4.0f * density, paint);
        canvas.drawText("-1", labelX, bottom, paint);
    }

    private void drawTimeAxis(Canvas canvas, float left, float right, float bottom, float density) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.0f * density);
        paint.setColor(GUIDE);
        canvas.drawLine(left, bottom, right, bottom, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11.5f * density);
        paint.setColor(MUTED);
        float labelY = bottom + 14.0f * density;
        canvas.drawText(formatTimeLabel(WINDOW_SECONDS), left + 7.0f * density, labelY, paint);
        canvas.drawText(formatTimeLabel(WINDOW_SECONDS / 2.0f), (left + right) / 2.0f, labelY, paint);
        canvas.drawText("0ms", right - 7.0f * density, labelY, paint);
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

    private float sampleToY(int sampleOffset, float midY, float halfHeight) {
        return midY - clamp(smoothedSampleAtOffset(sampleOffset) * TRACE_VERTICAL_SCALE) * halfHeight;
    }

    private float smoothedSampleAtOffset(int offset) {
        float sum = 0.0f;
        int count = 0;
        for (int delta = -TRACE_SMOOTHING_RADIUS; delta <= TRACE_SMOOTHING_RADIUS; delta++) {
            int safeOffset = offset + delta;
            if (safeOffset >= 0 && safeOffset < sampleCount) {
                sum += sampleAtOffset(safeOffset);
                count++;
            }
        }
        return count == 0 ? 0.0f : sum / count;
    }

    private static float clamp(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    private static String formatTimeLabel(float seconds) {
        if (seconds < 1.0f) {
            return String.format("-%.0fms", seconds * 1000.0f);
        }
        return String.format("-%.0fs", seconds);
    }
}
