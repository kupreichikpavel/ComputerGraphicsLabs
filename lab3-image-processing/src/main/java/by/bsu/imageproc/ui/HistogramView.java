package by.bsu.imageproc.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class HistogramView extends Region {

    private static final double PAD_LEFT = 8;
    private static final double PAD_BOTTOM = 18;
    private static final double PAD_TOP = 34;
    private static final double CLIP_FACTOR = 3;

    private final Canvas canvas = new Canvas();
    private final String title;
    private long[] histogram;
    private Integer threshold;

    public HistogramView(String title) {
        this.title = title;
        getChildren().add(canvas);
        getStyleClass().add("histogram");
        setMinHeight(140);
        setPrefHeight(160);
    }

    public void show(long[] values, Integer thresholdValue) {
        histogram = values;
        threshold = thresholdValue;
        draw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (histogram == null || w < 40) {
            g.setFill(Color.web("#1f2933"));
            g.setFont(Font.font(12));
            g.fillText(title, PAD_LEFT, 15);
            return;
        }
        long total = 0;
        long max = 1;
        long innerMax = 1;
        int nonZero = 0;
        for (int i = 0; i < histogram.length; i++) {
            total += histogram[i];
            max = Math.max(max, histogram[i]);
            if (i > 0 && i < histogram.length - 1) {
                innerMax = Math.max(innerMax, histogram[i]);
            }
            if (histogram[i] > 0) {
                nonZero++;
            }
        }
        boolean log = nonZero <= 2;
        boolean clip = !log && max > CLIP_FACTOR * innerMax;
        long scale = clip ? innerMax : max;
        String subtitle = log ? " (логарифмическая шкала)" : "";

        g.setFill(Color.web("#1f2933"));
        g.setFont(Font.font(12));
        g.fillText(title + subtitle, PAD_LEFT, 15);

        double plotW = w - 2 * PAD_LEFT;
        double plotH = h - PAD_TOP - PAD_BOTTOM;
        double bar = plotW / histogram.length;
        for (int i = 0; i < histogram.length; i++) {
            double barH;
            boolean clipped = false;
            if (log) {
                barH = plotH * Math.log1p(histogram[i]) / Math.log1p(max);
            } else {
                barH = plotH * histogram[i] / scale;
                if (barH > plotH) {
                    barH = plotH;
                    clipped = true;
                }
            }
            g.setFill(clipped ? Color.web("#e07b20") : Color.web("#2357c6"));
            g.fillRect(PAD_LEFT + i * bar, PAD_TOP + plotH - barH, Math.max(1, bar), barH);
        }
        g.setStroke(Color.web("#c9d1db"));
        g.strokeLine(PAD_LEFT, PAD_TOP + plotH, PAD_LEFT + plotW, PAD_TOP + plotH);
        g.setFill(Color.web("#66717f"));
        g.setFont(Font.font(10));
        g.fillText("0", PAD_LEFT, h - 4);
        g.fillText("128", PAD_LEFT + plotW / 2 - 8, h - 4);
        g.fillText("255", PAD_LEFT + plotW - 18, h - 4);
        g.fillText("пикселей: " + total, w - 120, 15);
        if (clip) {
            g.setFill(Color.web("#e07b20"));
            String text = String.format("пики обрезаны: 0 — %.1f%%, 255 — %.1f%% пикселей", 100.0 * histogram[0] / total,
                    100.0 * histogram[histogram.length - 1] / total);
            g.fillText(text, PAD_LEFT, 29);
        }
        if (threshold != null) {
            double x = PAD_LEFT + (threshold + 0.5) * bar;
            g.setStroke(Color.web("#d64545"));
            g.setLineWidth(2);
            g.strokeLine(x, PAD_TOP, x, PAD_TOP + plotH);
            g.setLineWidth(1);
            g.setFill(Color.web("#d64545"));
            g.setFont(Font.font(12));
            g.fillText("T = " + threshold, Math.min(x + 4, w - 50), PAD_TOP + 12);
        }
    }
}
