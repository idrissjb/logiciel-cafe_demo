package com.cafemanager.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.util.Duration;

/** Interrupteur (on/off) moderne, animé. */
public class SwitchControl extends StackPane {

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Circle thumb = new Circle(10);

    public SwitchControl() {
        Region track = new Region();
        track.getStyleClass().add("switch-track");
        track.setMinSize(48, 26);
        track.setPrefSize(48, 26);
        track.setMaxSize(48, 26);
        thumb.getStyleClass().add("switch-thumb");
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(track, thumb);
        setMaxSize(48, 26);
        setMinSize(48, 26);
        thumb.setTranslateX(3);
        getStyleClass().add("switch");
        setOnMouseClicked((MouseEvent e) -> setSelected(!isSelected()));
        selected.addListener((o, was, is) -> {
            new Timeline(new KeyFrame(Duration.millis(130),
                    new KeyValue(thumb.translateXProperty(), is ? 25 : 3, Interpolator.EASE_BOTH))).play();
            updateStyle(is);
        });
        setCursor(javafx.scene.Cursor.HAND);
    }

    public SwitchControl(boolean initial) {
        this();
        setSelected(initial);
        thumb.setTranslateX(initial ? 25 : 3);
        updateStyle(initial);
    }

    private void updateStyle(boolean on) {
        if (on) {
            if (!getStyleClass().contains("on")) {
                getStyleClass().add("on");
            }
        } else {
            getStyleClass().remove("on");
        }
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }
}
