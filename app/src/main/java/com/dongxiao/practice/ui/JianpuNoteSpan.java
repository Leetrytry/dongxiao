package com.dongxiao.practice.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

import com.dongxiao.practice.music.TargetNote;

public final class JianpuNoteSpan extends ReplacementSpan {
    private final int degree;
    private final int register;

    public JianpuNoteSpan(int degree, int register) {
        this.degree = degree;
        this.register = register;
    }

    public static SpannableString textFor(TargetNote note) {
        SpannableString text = new SpannableString(String.valueOf(note.scaleDegree));
        text.setSpan(
                new JianpuNoteSpan(note.scaleDegree, note.register),
                0,
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return text;
    }

    public static void appendTo(SpannableStringBuilder builder, TargetNote note) {
        int start = builder.length();
        builder.append(String.valueOf(note.scaleDegree));
        builder.setSpan(
                new JianpuNoteSpan(note.scaleDegree, note.register),
                start,
                builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    @Override
    public int getSize(
            Paint paint,
            CharSequence text,
            int start,
            int end,
            Paint.FontMetricsInt fm
    ) {
        if (fm != null) {
            paint.getFontMetricsInt(fm);
            int extra = (int) Math.ceil(dotExtra(paint));
            if (register == TargetNote.REGISTER_LOW) {
                fm.descent += extra;
                fm.bottom = Math.max(fm.bottom, fm.descent);
            } else if (register == TargetNote.REGISTER_HIGH) {
                fm.ascent -= extra;
                fm.top = Math.min(fm.top, fm.ascent);
            }
        }
        return Math.round(measureWidth(paint, degree));
    }

    @Override
    public void draw(
            Canvas canvas,
            CharSequence text,
            int start,
            int end,
            float x,
            int top,
            int y,
            int bottom,
            Paint paint
    ) {
        drawCentered(canvas, paint, degree, register, x + measureWidth(paint, degree) / 2.0f, y);
    }

    public static float measureWidth(Paint paint, int degree) {
        return paint.measureText(String.valueOf(degree)) + paint.getTextSize() * 0.22f;
    }

    public static void drawCentered(
            Canvas canvas,
            Paint paint,
            int degree,
            int register,
            float centerX,
            float baseline
    ) {
        String digit = String.valueOf(degree);
        Paint.Align oldAlign = paint.getTextAlign();
        Paint.Style oldStyle = paint.getStyle();

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(digit, centerX, baseline, paint);

        if (register == TargetNote.REGISTER_LOW || register == TargetNote.REGISTER_HIGH) {
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float radius = dotRadius(paint);
            float gap = dotGap(paint);
            float dotY = register == TargetNote.REGISTER_LOW
                    ? baseline + metrics.descent + gap + radius
                    : baseline + metrics.ascent - gap - radius;
            canvas.drawCircle(centerX, dotY, radius, paint);
        }

        paint.setTextAlign(oldAlign);
        paint.setStyle(oldStyle);
    }

    private static float dotRadius(Paint paint) {
        return Math.max(1.0f, paint.getTextSize() * 0.065f);
    }

    private static float dotGap(Paint paint) {
        return Math.max(1.0f, paint.getTextSize() * 0.08f);
    }

    private static float dotExtra(Paint paint) {
        return dotGap(paint) + dotRadius(paint) * 2.0f;
    }
}
