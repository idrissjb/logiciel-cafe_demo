package com.cafemanager.ui;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.File;

/** Pastille ronde avec une icône (émoji) ou une image de produit. */
public class IconBadge extends StackPane {

    public IconBadge(String emoji, String imagePath, double size) {
        getStyleClass().add("icon-badge");
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);

        Image img = loadImage(imagePath, size);
        if (img != null) {
            ImageView view = new ImageView(img);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false);
            view.setClip(new Circle(size / 2, size / 2, size / 2));
            getChildren().add(view);
        } else {
            Label icon = new Label(emoji == null || emoji.isBlank() ? "☕" : emoji);
            icon.getStyleClass().add("emoji");
            icon.setStyle("-fx-font-size: " + Math.round(size * 0.46) + "px;");
            getChildren().add(icon);
        }
    }

    private static Image loadImage(String path, double size) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            File f = new File(path);
            if (!f.isFile()) {
                return null;
            }
            Image img = new Image(f.toURI().toString(), size * 2, size * 2, false, true, false);
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }
}
