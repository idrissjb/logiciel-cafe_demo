package com.cafemanager.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Petites notifications qui apparaissent en bas de l'écran puis disparaissent seules. */
public final class Toast {

    public enum Kind {
        INFO("toast-info", "ℹ"), SUCCESS("toast-success", "✔"), WARNING("toast-warning", "⚠"), ERROR("toast-error", "✖");

        final String css;
        final String icon;

        Kind(String css, String icon) {
            this.css = css;
            this.icon = icon;
        }
    }

    private Toast() {
    }

    public static void info(String message) {
        show(message, Kind.INFO);
    }

    public static void success(String message) {
        show(message, Kind.SUCCESS);
    }

    public static void warning(String message) {
        show(message, Kind.WARNING);
    }

    public static void error(String message) {
        show(message, Kind.ERROR);
    }

    public static void show(String message, Kind kind) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(message, kind));
            return;
        }
        StackPane host = Modal.host();
        if (host == null) {
            return;
        }
        Label icon = new Label(kind.icon);
        icon.getStyleClass().add("toast-icon");
        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);
        text.setMaxWidth(560);
        HBox box = new HBox(12, icon, text);
        box.getStyleClass().addAll("toast", kind.css);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);
        box.setMouseTransparent(true);
        StackPane.setAlignment(box, Pos.BOTTOM_CENTER);
        StackPane.setMargin(box, new Insets(0, 0, 34, 0));
        host.getChildren().add(box);

        box.setOpacity(0);
        box.setTranslateY(16);
        FadeTransition in = new FadeTransition(Duration.millis(160), box);
        in.setToValue(1);
        TranslateTransition up = new TranslateTransition(Duration.millis(160), box);
        up.setToY(0);
        PauseTransition stay = new PauseTransition(Duration.millis(kind == Kind.ERROR ? 3800 : 2400));
        FadeTransition out = new FadeTransition(Duration.millis(280), box);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(new javafx.animation.ParallelTransition(in, up), stay, out);
        seq.setOnFinished(e -> host.getChildren().remove(box));
        seq.play();
    }
}
