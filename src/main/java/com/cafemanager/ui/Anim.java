package com.cafemanager.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Animations discrètes et rapides (100 à 200 ms) : survol "3D", pression, apparition, disparition.
 * Une seule animation "de transformation" tourne à la fois sur un nœud (la précédente est arrêtée).
 */
public final class Anim {

    private static final String KEY = "cafe.anim";

    private Anim() {
    }

    /** Joue {@code a} en arrêtant l'animation précédente du même nœud. */
    private static void play(Node n, Animation a) {
        Object prev = n.getProperties().get(KEY);
        if (prev instanceof Animation) {
            ((Animation) prev).stop();
        }
        n.getProperties().put(KEY, a);
        a.play();
    }

    private static Timeline transform(Node n, double scale, double translateY, double ms, Interpolator ip) {
        return new Timeline(new KeyFrame(Duration.millis(ms),
                new KeyValue(n.scaleXProperty(), scale, ip),
                new KeyValue(n.scaleYProperty(), scale, ip),
                new KeyValue(n.translateYProperty(), translateY, ip)));
    }

    // ---------------------------------------------------------------- apparition

    public static void fadeIn(Node n, double ms) {
        n.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(ms), n);
        ft.setToValue(1);
        ft.play();
    }

    /** Apparition douce : fondu + glissement vertical. */
    public static void slideIn(Node n, double fromY, double ms) {
        n.setOpacity(0);
        n.setTranslateY(fromY);
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(ms),
                new KeyValue(n.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(n.translateYProperty(), 0, Interpolator.EASE_OUT)));
        tl.play();
    }

    /** Fondu de sortie puis action (typiquement : retirer le nœud). */
    public static void fadeOut(Node n, double ms, Runnable done) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), n);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (done != null) {
                done.run();
            }
        });
        ft.play();
    }

    // ------------------------------------------------------------------ feedback

    /** Petit "pop" (grossit puis revient) : utilisé quand une quantité ou un total change. */
    public static void pop(Node n, double scale) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(n.scaleXProperty(), 1), new KeyValue(n.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(90), new KeyValue(n.scaleXProperty(), scale, Interpolator.EASE_OUT),
                        new KeyValue(n.scaleYProperty(), scale, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(200), new KeyValue(n.scaleXProperty(), 1, Interpolator.EASE_IN),
                        new KeyValue(n.scaleYProperty(), 1, Interpolator.EASE_IN)));
        play(n, tl);
    }

    /** Secousse horizontale (erreur de connexion, saisie invalide). */
    public static void shake(Node n) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), n);
        tt.setFromX(0);
        tt.setByX(9);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> n.setTranslateX(0));
        tt.play();
    }

    // -------------------------------------------------------------------- 3D

    /**
     * Effet carte "3D" : au survol la carte monte, grossit légèrement et son ombre s'agrandit ;
     * à la pression elle s'enfonce (0.96) puis revient.
     *
     * @param shadow ombre de la carte (déjà appliquée avec {@code node.setEffect})
     */
    public static void installCardEffects(Region node, DropShadow shadow, double lift, double hoverScale) {
        final double r0 = shadow.getRadius();
        final double y0 = shadow.getOffsetY();
        final double rHover = r0 + 16;
        final double yHover = y0 + 8;

        node.hoverProperty().addListener((obs, was, hovered) -> {
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(150),
                    new KeyValue(node.scaleXProperty(), hovered ? hoverScale : 1, Interpolator.EASE_OUT),
                    new KeyValue(node.scaleYProperty(), hovered ? hoverScale : 1, Interpolator.EASE_OUT),
                    new KeyValue(node.translateYProperty(), hovered ? -lift : 0, Interpolator.EASE_OUT),
                    new KeyValue(shadow.radiusProperty(), hovered ? rHover : r0, Interpolator.EASE_OUT),
                    new KeyValue(shadow.offsetYProperty(), hovered ? yHover : y0, Interpolator.EASE_OUT)));
            play(node, tl);
        });

        node.addEventFilter(MouseEvent.MOUSE_PRESSED, e ->
                play(node, transform(node, 0.96, node.getTranslateY(), 90, Interpolator.EASE_OUT)));
        node.addEventFilter(MouseEvent.MOUSE_RELEASED, e ->
                play(node, transform(node, node.isHover() ? hoverScale : 1, node.isHover() ? -lift : 0, 130,
                        Interpolator.EASE_OUT)));
    }

    /**
     * Retour visuel de pression pour tous les nœuds portant la classe CSS "pressable" (boutons tactiles) :
     * réduction à 0.97 pendant l'appui.
     */
    public static void installGlobalPressFeedback(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Node n = pressable(e.getTarget());
            if (n != null && !n.isDisabled()) {
                play(n, transform(n, 0.97, n.getTranslateY(), 80, Interpolator.EASE_OUT));
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            Node n = pressable(e.getTarget());
            if (n != null) {
                play(n, transform(n, 1, n.getTranslateY(), 120, Interpolator.EASE_OUT));
            }
        });
    }

    private static Node pressable(Object target) {
        Node n = target instanceof Node ? (Node) target : null;
        while (n != null) {
            if (n.getStyleClass().contains("pressable")) {
                return n;
            }
            n = n.getParent();
        }
        return null;
    }
}
