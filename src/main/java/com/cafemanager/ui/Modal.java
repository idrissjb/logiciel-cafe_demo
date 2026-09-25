package com.cafemanager.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fenêtres modales "in-scene" : un voile sombre sur toute l'application avec une carte au centre.
 * Plus moderne et plus fluide qu'une fenêtre système, et compatible écran tactile.
 * ESC ferme la fenêtre du dessus.
 */
public final class Modal {

    private static final class Layer {
        final StackPane backdrop;
        final Node content;
        final Runnable onClose;

        Layer(StackPane backdrop, Node content, Runnable onClose) {
            this.backdrop = backdrop;
            this.content = content;
            this.onClose = onClose;
        }
    }

    private static StackPane host;
    private static final Deque<Layer> LAYERS = new ArrayDeque<>();

    private Modal() {
    }

    /** À appeler une fois : {@code root} est le StackPane racine de la scène. */
    public static void install(StackPane root, Scene scene) {
        host = root;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && !LAYERS.isEmpty()) {
                close();
                e.consume();
            }
        });
    }

    public static boolean isOpen() {
        return !LAYERS.isEmpty();
    }

    public static void show(Node content) {
        show(content, true, null);
    }

    public static void show(Node content, boolean dismissOnBackdrop, Runnable onClose) {
        StackPane backdrop = new StackPane(content);
        backdrop.getStyleClass().add("modal-backdrop");
        StackPane.setMargin(content, new javafx.geometry.Insets(24));
        if (dismissOnBackdrop) {
            backdrop.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
                if (e.getTarget() == backdrop) {
                    close();
                }
            });
        }
        Layer layer = new Layer(backdrop, content, onClose);
        LAYERS.push(layer);
        host.getChildren().add(backdrop);

        backdrop.setOpacity(0);
        content.setScaleX(0.94);
        content.setScaleY(0.94);
        FadeTransition fade = new FadeTransition(Duration.millis(140), backdrop);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(170), content);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
        content.requestFocus();
    }

    /** Ferme la fenêtre du dessus. */
    public static void close() {
        Layer layer = LAYERS.peek();
        if (layer == null) {
            return;
        }
        LAYERS.pop();
        FadeTransition fade = new FadeTransition(Duration.millis(110), layer.backdrop);
        fade.setToValue(0);
        fade.setOnFinished(e -> host.getChildren().remove(layer.backdrop));
        fade.play();
        layer.backdrop.setMouseTransparent(true);
        if (layer.onClose != null) {
            layer.onClose.run();
        }
    }

    public static void closeAll() {
        while (!LAYERS.isEmpty()) {
            close();
        }
    }

    /** Fenêtre principale (propriétaire des sélecteurs de fichiers). */
    public static javafx.stage.Window window() {
        return host == null || host.getScene() == null ? null : host.getScene().getWindow();
    }

    /** Conteneur racine (utilisé par les toasts). */
    static StackPane host() {
        return host;
    }
}
