package by.bsu.imageproc.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class HistogramView extends Region {

    private static final double PAD_LEFT = 8;
    private static final double PAD_BOTTOM = 18;
    private static final double PAD_TOP = 22;

    private final Canvas canvas = new Canvas();
    private final String title;
    private final boolean logScale;
    private long[] histogram;
    private Integer threshold;

    public HistogramView(String title, boolean logScale) {
        this.title = title;
        this.logScale = logScale;
        getChildren().add(canvas);
        getStyleClass().add("histogram");
        setMinHeight(130);
        setPrefHeight(150);
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
        g.setFill(Color.web("#1f2933"));
        g.setFont(Font.font(12));
        g.fillText(logScale ? title + " (логарифмическая шкала)" : title, PAD_LEFT, 15);
        if (histogram == null || w < 40) {
            return;
        }
        long total = 0;
        long max = 1;
        for (long v : histogram) {
            total += v;
            max = Math.max(max, v);
        }
        double plotW = w - 2 * PAD_LEFT;
        double plotH = h - PAD_TOP - PAD_BOTTOM;
        double bar = plotW / histogram.length;
        g.setFill(Color.web("#2357c6"));
        for (int i = 0; i < histogram.length; i++) {
            double barH = logScale
                    ? plotH * Math.log1p(histogram[i]) / Math.log1p(max)
                    : plotH * histogram[i] / max;
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
