package com.cafemanager.ui;

import com.cafemanager.util.PasswordStrength;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Indicateur de solidité : quatre segments colorés et un libellé (Faible / Moyen / Bon / Fort). */
public class PasswordStrengthMeter extends HBox {

    private static final int SEGMENTS = 4;

    private final Region[] segments = new Region[SEGMENTS];
    private final Label label = new Label();

    public PasswordStrengthMeter() {
        setSpacing(4);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("strength-meter");
        for (int i = 0; i < SEGMENTS; i++) {
            segments[i] = new Region();
            segments[i].getStyleClass().add("strength-seg");
            HBox.setHgrow(segments[i], Priority.ALWAYS);
            segments[i].setMaxWidth(Double.MAX_VALUE);
            getChildren().add(segments[i]);
        }
        label.getStyleClass().add("strength-label");
        label.setMinWidth(44);
        label.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(label);
        update("");
    }

    public void update(String password) {
        int score = PasswordStrength.score(password);
        for (int i = 0; i < SEGMENTS; i++) {
            segments[i].getStyleClass().remove("strength-seg-on");
            if (i < score) {
                segments[i].getStyleClass().add("strength-seg-on");
            }
        }
        getStyleClass().removeIf(c -> c.startsWith("strength-level-"));
        getStyleClass().add("strength-level-" + score);
        label.setText(PasswordStrength.label(score));
    }
}
