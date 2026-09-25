package com.cafemanager.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * Illustration : tasse de café moderne dessinée avec des formes JavaFX (aucune image nécessaire),
 * avec volutes de vapeur animées et légère lévitation.
 */
public class CoffeeCup extends Group {

    private final Timeline float_;
    private final Timeline steamLoop;

    public CoffeeCup() {
        // Soucoupe
        Ellipse saucerShadow = new Ellipse(0, 108, 150, 20);
        saucerShadow.setFill(Color.rgb(0, 0, 0, 0.45));
        saucerShadow.setEffect(new GaussianBlur(18));

        Ellipse saucer = new Ellipse(0, 92, 138, 26);
        saucer.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#F4E4CC")), new Stop(1, Color.web("#B98A5E"))));
        Ellipse saucerInner = new Ellipse(0, 88, 88, 14);
        saucerInner.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#C9976A")), new Stop(1, Color.web("#E7CDA8"))));

        // Anse
        Arc handle = new Arc(96, 6, 40, 44, -80, 170);
        handle.setType(ArcType.OPEN);
        handle.setFill(null);
        handle.setStroke(Color.web("#E9D2B0"));
        handle.setStrokeWidth(15);
        handle.setStrokeLineCap(StrokeLineCap.ROUND);

        // Corps de la tasse
        Path body = new Path(
                new MoveTo(-92, -58),
                new LineTo(92, -58),
                new CubicCurveTo(92, 34, 62, 92, 0, 92),
                new CubicCurveTo(-62, 92, -92, 34, -92, -58));
        body.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#C99B6D")), new Stop(0.35, Color.web("#F6E7CF")),
                new Stop(0.7, Color.web("#EBD3AE")), new Stop(1, Color.web("#B98757"))));
        body.setStroke(null);
        body.setEffect(new DropShadow(22, 0, 10, Color.rgb(0, 0, 0, 0.35)));

        // Reflet
        Path shine = new Path(
                new MoveTo(-70, -40),
                new QuadCurveTo(-74, 20, -44, 58),
                new QuadCurveTo(-58, 10, -56, -40),
                new LineTo(-70, -40));
        shine.setFill(Color.rgb(255, 255, 255, 0.35));
        shine.setStroke(null);

        // Bord + café
        Ellipse rim = new Ellipse(0, -58, 92, 20);
        rim.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF3DE")), new Stop(1, Color.web("#D9B98D"))));
        Ellipse coffee = new Ellipse(0, -56, 80, 15);
        coffee.setFill(new RadialGradient(0, 0, 0, -0.2, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7A4A2A")), new Stop(1, Color.web("#2C170B"))));
        Ellipse crema = new Ellipse(-14, -60, 34, 5);
        crema.setFill(Color.rgb(240, 200, 150, 0.28));

        // Vapeur : 3 volutes
        Group steam = new Group();
        steamLoop = new Timeline();
        double[] xs = {-34, 0, 34};
        for (int i = 0; i < xs.length; i++) {
            Path wisp = new Path(
                    new MoveTo(xs[i], -80),
                    new CubicCurveTo(xs[i] - 22, -110, xs[i] + 22, -132, xs[i], -164),
                    new CubicCurveTo(xs[i] - 14, -186, xs[i] + 12, -196, xs[i], -214));
            wisp.setFill(null);
            wisp.setStroke(Color.rgb(255, 245, 230, 0.55));
            wisp.setStrokeWidth(7);
            wisp.setStrokeLineCap(StrokeLineCap.ROUND);
            wisp.setEffect(new GaussianBlur(4));
            wisp.setOpacity(0);
            steam.getChildren().add(wisp);
            Duration offset = Duration.millis(i * 650);
            steamLoop.getKeyFrames().addAll(
                    new KeyFrame(offset, new KeyValue(wisp.opacityProperty(), 0), new KeyValue(wisp.translateYProperty(), 14),
                            new KeyValue(wisp.scaleYProperty(), 0.8)),
                    new KeyFrame(offset.add(Duration.millis(900)), new KeyValue(wisp.opacityProperty(), 0.85, Interpolator.EASE_OUT)),
                    new KeyFrame(offset.add(Duration.millis(2600)), new KeyValue(wisp.opacityProperty(), 0, Interpolator.EASE_IN),
                            new KeyValue(wisp.translateYProperty(), -20, Interpolator.EASE_OUT),
                            new KeyValue(wisp.scaleYProperty(), 1.1)));
        }
        steamLoop.setCycleCount(Animation.INDEFINITE);
        steamLoop.getKeyFrames().add(new KeyFrame(Duration.millis(3900)));

        getChildren().addAll(saucerShadow, saucer, saucerInner, handle, body, shine, rim, coffee, crema, steam);

        // Lévitation douce
        float_ = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(translateYProperty(), 0)),
                new KeyFrame(Duration.millis(2400), new KeyValue(translateYProperty(), -8, Interpolator.EASE_BOTH)));
        float_.setAutoReverse(true);
        float_.setCycleCount(Animation.INDEFINITE);
    }

    public void start() {
        steamLoop.play();
        float_.play();
    }

    public void stop() {
        steamLoop.stop();
        float_.stop();
    }
}
