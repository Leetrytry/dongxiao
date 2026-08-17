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
        float midY = height / 2.0f;
        float padding = 14.0f * density;
        float left = padding;
        float right = width - padding;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FFFFFF"));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.parseColor("#E8E1D6"));
        paint.setStrokeWidth(1.0f * density);
        canvas.drawLine(left, midY, right, midY, paint);

        if (samples.length < 2 || width <= padding * 2.0f) {
            paint.setColor(Color.parseColor("#9B958C"));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13.0f * density);
            canvas.drawText("等待音频波形", width / 2.0f, midY - 8.0f * density, paint);
            return;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.parseColor("#1D7A6B"));

        int points = Math.max(2, (int) (right - left));
        int step = Math.max(1, samples.length / points);
        float lastX = left;
        float lastY = midY - clamp(samples[0]) * (height * 0.38f);
        for (int i = step; i < samples.length; i += step) {
            float x = left + (right - left) * i / (samples.length - 1.0f);
            float y = midY - clamp(samples[i]) * (height * 0.38f);
            canvas.drawLine(lastX, lastY, x, y, paint);
            lastX = x;
            lastY = y;
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private static float clamp(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }
}
