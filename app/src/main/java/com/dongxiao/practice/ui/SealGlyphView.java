package com.dongxiao.practice.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

public final class SealGlyphView extends View {
    private final Paint washPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String glyph = "簫";
    private int tintColor = Color.rgb(245, 211, 144);

    public SealGlyphView(Context context) {
        super(context);
        init(null);
    }

    public SealGlyphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SealGlyphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public void setGlyph(String glyph) {
        if (!TextUtils.isEmpty(glyph)) {
            this.glyph = glyph;
            invalidate();
        }
    }

    public void setTintColor(int tintColor) {
        this.tintColor = tintColor;
        invalidate();
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            int[] styleable = new int[]{android.R.attr.text, android.R.attr.textColor};
            TypedArray array = getContext().obtainStyledAttributes(attrs, styleable);
            CharSequence text = array.getText(0);
            if (!TextUtils.isEmpty(text)) {
                glyph = text.toString();
            }
            tintColor = array.getColor(1, tintColor);
            array.recycle();
        }
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) {
            return;
        }

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float centerX = left + width / 2.0f;
        float centerY = top + height / 2.0f;
        float size = Math.min(width, height);

        washPaint.setStyle(Paint.Style.FILL);
        washPaint.setColor(withAlpha(tintColor, 34));
        canvas.drawOval(
                centerX - size * 0.42f,
                centerY - size * 0.44f,
                centerX + size * 0.42f,
                centerY + size * 0.44f,
                washPaint
        );

        strokePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setTextSize(size * 0.82f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(Math.max(1.0f, size * 0.035f));
        strokePaint.setColor(withAlpha(tintColor, 74));

        Paint.FontMetrics metrics = strokePaint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2.0f;
        canvas.drawText(glyph, centerX, baseline, strokePaint);

        glyphPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        glyphPaint.setTextAlign(Paint.Align.CENTER);
        glyphPaint.setTextSize(size * 0.82f);
        glyphPaint.setStyle(Paint.Style.FILL);
        glyphPaint.setFakeBoldText(true);
        glyphPaint.setColor(withAlpha(tintColor, 224));
        canvas.drawText(glyph, centerX, baseline, glyphPaint);

        glyphPaint.setFakeBoldText(false);
        glyphPaint.setColor(withAlpha(Color.WHITE, 56));
        canvas.drawText(glyph, centerX - size * 0.018f, baseline - size * 0.018f, glyphPaint);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(
                clamp(alpha),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
