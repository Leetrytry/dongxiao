package com.dongxiao.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.dongxiao.practice.music.TargetNote;
import com.dongxiao.practice.practice.PracticeMode;
import com.dongxiao.practice.practice.PracticeStats;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class PracticeVisualizerView extends View {
    private static final int PAPER = Color.argb(224, 248, 243, 230);
    private static final int INK = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(71, 85, 105);
    private static final int LINE = Color.rgb(235, 216, 185);
    private static final int ACCENT = Color.rgb(29, 122, 107);
    private static final int ACCENT_SOFT = Color.argb(42, 29, 122, 107);
    private static final int CINNABAR = Color.rgb(220, 38, 38);
    private static final int CINNABAR_SOFT = Color.argb(46, 220, 38, 38);
    private static final int WARNING = Color.rgb(180, 83, 9);
    private static final int DANGER = Color.rgb(179, 38, 30);
    private static final int EVENT_WINDOW_MS = 4200;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Deque<Long> onsetEvents = new ArrayDeque<>();
    private final Deque<Long> ornamentEvents = new ArrayDeque<>();

    private PracticeMode mode = PracticeMode.LONG_TONE;
    private TargetNote target;
    private PracticeStats stats;
    private double cents = 0.0;
    private boolean hasPitch = false;
    private int lastOnsetCount = 0;
    private int lastOrnamentCount = 0;

    public PracticeVisualizerView(Context context) {
        super(context);
        init();
    }

    public PracticeVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription("练习反馈等待拾音");
    }

    public void setReading(
            PracticeMode mode,
            double cents,
            boolean hasPitch,
            TargetNote target,
            PracticeStats stats
    ) {
        this.mode = mode == null ? PracticeMode.LONG_TONE : mode;
        this.cents = cents;
        this.hasPitch = hasPitch;
        this.target = target;
        this.stats = stats;
        long now = System.currentTimeMillis();
        updateEvents(now, stats);
        updateContentDescription();
        invalidate();
    }

    public void clear(PracticeMode mode, TargetNote target) {
        this.mode = mode == null ? PracticeMode.LONG_TONE : mode;
        this.target = target;
        this.stats = null;
        this.cents = 0.0;
        this.hasPitch = false;
        lastOnsetCount = 0;
        lastOrnamentCount = 0;
        onsetEvents.clear();
        ornamentEvents.clear();
        updateContentDescription();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        drawSurface(canvas, width, height, density);
        drawTargetHeader(canvas, width, density);
        float top = 38.0f * density;
        switch (mode) {
            case TONGUING:
                drawTonguing(canvas, width, height, top, density);
                break;
            case VIBRATO:
                drawVibrato(canvas, width, height, top, density);
                break;
            case SLIDE:
                drawSlide(canvas, width, height, top, density);
                break;
            case ORNAMENT:
                drawOrnament(canvas, width, height, top, density);
                break;
            case SCALE:
                drawScalePitch(canvas, width, height, top, density);
                break;
            case LONG_TONE:
            default:
                drawLongTone(canvas, width, height, top, density);
                break;
        }
    }

    private void drawSurface(Canvas canvas, float width, float height, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PAPER);
        canvas.drawRoundRect(new RectF(0, 0, width, height), 14.0f * density, 14.0f * density, paint);
    }

    private void drawTargetHeader(Canvas canvas, float width, float density) {
        paint.setTextSize(14.0f * density);
        if (mode == PracticeMode.LONG_TONE) {
            drawLongToneTopMetrics(canvas, width, density);
        } else {
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(hasPitch ? centsColor(Math.abs(cents)) : MUTED);
            String centsText = hasPitch ? formatCents(cents) : "等待拾音";
            canvas.drawText(centsText, width - 16.0f * density, 24.0f * density, paint);
        }

        if (target != null) {
            paint.setColor(INK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(15.0f * density);
            float centerX = width / 2.0f;
            float prefixWidth = paint.measureText("目标 ");
            float noteWidth = JianpuNoteSpan.measureWidth(paint, target.scaleDegree);
            float left = centerX - (prefixWidth + noteWidth) / 2.0f;
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("目标 ", left, 24.0f * density, paint);
            JianpuNoteSpan.drawCentered(
                    canvas,
                    paint,
                    target.scaleDegree,
                    target.register,
                    left + prefixWidth + noteWidth / 2.0f,
                    24.0f * density
            );
        }
    }

    private void drawLongTone(Canvas canvas, float width, float height, float top, float density) {
        float centerY = top + (height - top) * 0.48f;
        drawPitchRail(canvas, width, centerY, density, true);
    }

    private void drawScalePitch(Canvas canvas, float width, float height, float top, float density) {
        float centerY = top + (height - top) * 0.50f;
        drawPitchRail(canvas, width, centerY, density, true);
        drawCaption(canvas, "级进 / 三度 / 分解：当前音命中后推进", width, height, density);
    }

    private void drawTonguing(Canvas canvas, float width, float height, float top, float density) {
        float left = 18.0f * density;
        float right = width - left;
        float y = top + (height - top) * 0.48f;

        paint.setStrokeWidth(2.0f * density);
        paint.setColor(LINE);
        canvas.drawLine(left, y, right, y, paint);

        int slots = 8;
        long now = System.currentTimeMillis();
        trimEvents(onsetEvents, now);
        int active = onsetEvents.size();
        for (int i = 0; i < slots; i++) {
            float x = left + (right - left) * i / Math.max(1, slots - 1);
            boolean filled = i >= slots - active;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(filled ? ACCENT : Color.argb(58, 71, 85, 105));
            canvas.drawCircle(x, y, (filled ? 7.0f : 4.6f) * density, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * density);
        paint.setColor(Color.argb(120, 71, 85, 105));
        float splitX = left + (right - left) * 3.5f / Math.max(1, slots - 1);
        canvas.drawLine(splitX, y - 17.0f * density, splitX, y + 17.0f * density, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12.5f * density);
        paint.setColor(MUTED);
        canvas.drawText("8连吐 · 4+4均分", width / 2.0f, y - 28.0f * density, paint);
        paint.setTextSize(13.0f * density);
        paint.setColor(INK);
        String text = String.format(
                Locale.CHINA,
                "%.1f 次/秒 · 均匀 %s",
                stats == null ? 0.0 : stats.tonguingRateHz,
                formatPercent(stats == null ? 0.0 : stats.tonguingEvennessPercent)
        );
        canvas.drawText(text, width / 2.0f, height - 18.0f * density, paint);
    }

    private void drawVibrato(Canvas canvas, float width, float height, float top, float density) {
        float left = 18.0f * density;
        float right = width - left;
        float midY = top + (height - top) * 0.50f;
        float amplitude = Math.max(10.0f * density, Math.min(30.0f * density, (float) depth() / 2.0f * density));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ACCENT_SOFT);
        rect.set(left, midY - amplitude, right, midY + amplitude);
        canvas.drawRoundRect(rect, 12.0f * density, 12.0f * density, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * density);
        paint.setColor(LINE);
        canvas.drawLine(left, midY, right, midY, paint);
        drawVibratoGuideDots(canvas, left, right, midY, amplitude, density);

        paint.setStyle(Paint.Style.STROKE);
        path.reset();
        int points = 80;
        double cycles = Math.max(1.0, Math.min(8.0, rate() * 0.75));
        for (int i = 0; i < points; i++) {
            float x = left + (right - left) * i / (points - 1.0f);
            float y = midY - (float) Math.sin(i / (points - 1.0) * Math.PI * 2.0 * cycles) * amplitude;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        paint.setStrokeWidth(3.0f * density);
        paint.setColor(ACCENT);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        drawCaption(
                canvas,
                String.format(Locale.CHINA, "%.1f Hz · ±%.0f cent · 规律 %s", rate(), depth(), formatPercent(regularity())),
                width,
                height,
                density
        );
    }

    private void drawSlide(Canvas canvas, float width, float height, float top, float density) {
        float left = 24.0f * density;
        float right = width - left;
        float centerY = top + (height - top) * 0.50f;
        float currentX = mapCentsToX(cents, left, right);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ACCENT_SOFT);
        rect.set(width / 2.0f - 15.0f * density, centerY - 34.0f * density,
                width / 2.0f + 15.0f * density, centerY + 34.0f * density);
        canvas.drawRoundRect(rect, 9.0f * density, 9.0f * density, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f * density);
        paint.setColor(LINE);
        canvas.drawLine(left, centerY, right, centerY, paint);
        drawSlideAnchors(canvas, left, right, centerY, density);

        paint.setStyle(Paint.Style.STROKE);
        path.reset();
        float startX = stats == null || stats.slideDeltaCents >= 0.0 ? left : right;
        path.moveTo(startX, centerY + 28.0f * density);
        path.cubicTo(
                (startX + width / 2.0f) / 2.0f,
                centerY - 38.0f * density,
                (currentX + width / 2.0f) / 2.0f,
                centerY + 36.0f * density,
                currentX,
                centerY
        );
        paint.setStrokeWidth(3.2f * density);
        paint.setColor(hasPitch ? ACCENT : MUTED);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(hasPitch ? centsColor(Math.abs(cents)) : MUTED);
        canvas.drawCircle(currentX, centerY, 8.0f * density, paint);

        drawCaption(
                canvas,
                String.format(Locale.CHINA, "幅度 %.0f cent · 平滑 %s · %s",
                        stats == null ? 0.0 : stats.slideRangeCents,
                        formatPercent(stats == null ? 0.0 : stats.slideSmoothnessPercent),
                        stats != null && stats.slideLanded ? "落点到位" : "继续找落点"),
                width,
                height,
                density
        );
    }

    private void drawOrnament(Canvas canvas, float width, float height, float top, float density) {
        float centerX = width / 2.0f;
        float centerY = top + (height - top) * 0.50f;
        long now = System.currentTimeMillis();
        trimEvents(ornamentEvents, now);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f * density);
        paint.setColor(LINE);
        canvas.drawCircle(centerX, centerY, 34.0f * density, paint);
        paint.setColor(CINNABAR_SOFT);
        paint.setStrokeWidth(12.0f * density);
        rect.set(centerX - 44.0f * density, centerY - 44.0f * density,
                centerX + 44.0f * density, centerY + 44.0f * density);
        canvas.drawArc(rect, 205.0f, 130.0f, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ACCENT);
        canvas.drawCircle(centerX, centerY, 8.0f * density, paint);
        drawOrnamentLabels(canvas, centerX, centerY, density);
        paint.setColor(CINNABAR);
        int index = 0;
        for (Long ignored : ornamentEvents) {
            double angle = Math.toRadians(215.0 + index * 26.0);
            float x = centerX + (float) Math.cos(angle) * 44.0f * density;
            float y = centerY + (float) Math.sin(angle) * 44.0f * density;
            canvas.drawCircle(x, y, 4.2f * density, paint);
            index++;
        }

        drawCaption(
                canvas,
                String.format(Locale.CHINA, "回落 %d 次 · 离音 %.0f cent · %.0f ms",
                        stats == null ? 0 : stats.ornamentCount,
                        stats == null ? 0.0 : stats.ornamentLastExcursionCents,
                        stats == null ? 0.0 : stats.ornamentLastDurationMs),
                width,
                height,
                density
        );
    }

    private void drawVibratoGuideDots(
            Canvas canvas,
            float left,
            float right,
            float midY,
            float amplitude,
            float density
    ) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(130, 180, 83, 9));
        for (int i = 0; i < 4; i++) {
            float x = left + (right - left) * (i + 0.5f) / 4.0f;
            float y = midY - (i % 2 == 0 ? amplitude : -amplitude);
            canvas.drawCircle(x, y, 3.2f * density, paint);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11.5f * density);
        paint.setColor(MUTED);
        canvas.drawText("参考：每拍2-4个均匀脉冲", (left + right) / 2.0f, midY - amplitude - 12.0f * density, paint);
    }

    private void drawSlideAnchors(Canvas canvas, float left, float right, float centerY, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11.5f * density);
        paint.setColor(MUTED);
        float startX = stats == null || stats.slideDeltaCents >= 0.0 ? left : right;
        canvas.drawText("起", startX, centerY - 24.0f * density, paint);
        paint.setColor(ACCENT);
        canvas.drawText("落", (left + right) / 2.0f, centerY - 24.0f * density, paint);
    }

    private void drawOrnamentLabels(Canvas canvas, float centerX, float centerY, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11.5f * density);
        paint.setColor(MUTED);
        canvas.drawText("本", centerX, centerY - 48.0f * density, paint);
        canvas.drawText("邻", centerX - 48.0f * density, centerY + 5.0f * density, paint);
        canvas.drawText("本", centerX, centerY + 56.0f * density, paint);
    }

    private void drawPitchRail(Canvas canvas, float width, float centerY, float density, boolean showTicks) {
        float left = 22.0f * density;
        float right = width - left;
        float targetLeft = mapCentsToX(-25.0, left, right);
        float targetRight = mapCentsToX(25.0, left, right);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ACCENT_SOFT);
        rect.set(targetLeft, centerY - 20.0f * density, targetRight, centerY + 20.0f * density);
        canvas.drawRoundRect(rect, 10.0f * density, 10.0f * density, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f * density);
        paint.setColor(LINE);
        canvas.drawLine(left, centerY, right, centerY, paint);

        if (showTicks) {
            int[] ticks = {-50, -25, 0, 25, 50};
            for (int tick : ticks) {
                float x = mapCentsToX(tick, left, right);
                paint.setColor(tick == 0 ? ACCENT : MUTED);
                canvas.drawLine(x, centerY - 14.0f * density, x, centerY + 14.0f * density, paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(12.5f * density);
            paint.setFakeBoldText(false);
            int[] labeledTicks = {-50, 0, 50};
            for (int tick : labeledTicks) {
                float x = mapCentsToX(tick, left, right);
                paint.setColor(tick == 0 ? ACCENT : MUTED);
                canvas.drawText(String.valueOf(tick), x, centerY + 35.0f * density, paint);
            }
        }

        float x = hasPitch ? mapCentsToX(cents, left, right) : width / 2.0f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(hasPitch ? centsColor(Math.abs(cents)) : MUTED);
        canvas.drawCircle(x, centerY, 10.0f * density, paint);
        paint.setStrokeWidth(3.0f * density);
        canvas.drawLine(x, centerY - 32.0f * density, x, centerY + 8.0f * density, paint);
    }

    private void drawLongToneTopMetrics(Canvas canvas, float width, float density) {
        double held = stats == null ? 0.0 : stats.heldSeconds;
        double stability = stats == null
                ? 0.0
                : Math.max(0.0, Math.min(100.0, PracticeStats.stabilityPercent(stats.stabilityCents)));

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(14.0f * density);
        paint.setFakeBoldText(true);
        paint.setColor(ACCENT);
        canvas.drawText(String.format(Locale.CHINA, "命中 %.1fs", held), 16.0f * density, 24.0f * density, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(stability >= 80.0 ? ACCENT : stability >= 60.0 ? WARNING : DANGER);
        String stabilityText = stats == null || !stats.stabilityReady
                ? "稳定 --"
                : String.format(Locale.CHINA, "稳定 %.0f%%", stability);
        canvas.drawText(stabilityText, width - 16.0f * density, 24.0f * density, paint);
        paint.setFakeBoldText(false);
    }

    private void drawCaption(Canvas canvas, String text, float width, float height, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12.0f * density);
        paint.setColor(INK);
        canvas.drawText(text, width / 2.0f, height - 16.0f * density, paint);
    }

    private void updateEvents(long now, PracticeStats stats) {
        if (stats != null && stats.onsetCount > lastOnsetCount) {
            for (int i = lastOnsetCount; i < stats.onsetCount; i++) {
                onsetEvents.addLast(now);
            }
            lastOnsetCount = stats.onsetCount;
        }
        if (stats != null && stats.ornamentCount > lastOrnamentCount) {
            for (int i = lastOrnamentCount; i < stats.ornamentCount; i++) {
                ornamentEvents.addLast(now);
            }
            lastOrnamentCount = stats.ornamentCount;
        }
        trimEvents(onsetEvents, now);
        trimEvents(ornamentEvents, now);
    }

    private void updateContentDescription() {
        String modeLabel = mode == null ? "专项" : mode.label;
        String pitch = hasPitch ? formatCents(cents) : "未检测到音高";
        setContentDescription(modeLabel + "反馈，" + pitch);
    }

    private static void trimEvents(Deque<Long> events, long now) {
        while (!events.isEmpty() && now - events.peekFirst() > EVENT_WINDOW_MS) {
            events.removeFirst();
        }
        while (events.size() > 10) {
            events.removeFirst();
        }
    }

    private double rate() {
        return stats == null ? 0.0 : stats.vibratoRateHz;
    }

    private double depth() {
        return stats == null ? 0.0 : stats.vibratoDepthCents;
    }

    private double regularity() {
        return stats == null ? 0.0 : stats.vibratoRegularityPercent;
    }

    private static float mapCentsToX(double cents, float left, float right) {
        double clamped = Math.max(-70.0, Math.min(70.0, cents));
        return (float) (left + (clamped + 70.0) / 140.0 * (right - left));
    }

    private static int centsColor(double absCents) {
        if (absCents <= 18.0) {
            return ACCENT;
        }
        if (absCents <= 35.0) {
            return WARNING;
        }
        return DANGER;
    }

    private static String formatCents(double cents) {
        String sign = cents > 0.0 ? "+" : "";
        return String.format(Locale.CHINA, "%s%.0f cent", sign, cents);
    }

    private static String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent) || percent <= 0.0) {
            return "--";
        }
        return String.format(Locale.CHINA, "%.0f%%", Math.max(0.0, Math.min(100.0, percent)));
    }
}
