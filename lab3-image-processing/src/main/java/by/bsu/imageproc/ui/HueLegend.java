package by.bsu.imageproc.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class HueLegend extends Canvas {

    private static final int SIZE = 150;
    private static final int RADIUS = 48;

    public HueLegend() {
        super(SIZE, SIZE);
        GraphicsContext g = getGraphicsContext2D();
        double c = SIZE / 2.0;
        for (int deg = 0; deg < 360; deg++) {
            g.setStroke(Color.hsb(deg, 1, 1));
            g.setLineWidth(3);
            double a = Math.toRadians(deg);
            g.strokeLine(c + Math.cos(a) * 20, c + Math.sin(a) * 20, c + Math.cos(a) * RADIUS, c + Math.sin(a) * RADIUS);
        }
        g.setFill(Color.web("#1f2933"));
        g.setFont(Font.font(10));
        g.fillText("0° →", c + RADIUS + 2, c + 4);
        g.fillText("90° ↓", c - 14, c + RADIUS + 14);
        g.fillText("← 180°", 2, c + 4);
        g.fillText("270° ↑", c - 16, c - RADIUS - 6);
    }
}
