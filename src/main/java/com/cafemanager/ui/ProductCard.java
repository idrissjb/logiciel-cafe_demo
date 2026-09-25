package com.cafemanager.ui;

import com.cafemanager.model.Product;
import com.cafemanager.util.Money;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Grande carte produit tactile, légèrement 3D : ombre douce, montée au survol, enfoncement au clic.
 * Un clic ajoute directement le produit au ticket (aucun bouton "ajouter").
 */
public class ProductCard extends StackPane {

    private final Label plusOne = new Label("+1");

    public ProductCard(Product p, Consumer<Product> onAdd) {
        getStyleClass().add("product-card");
        setFocusTraversable(false);
        setMinSize(120, 120);

        IconBadge badge = new IconBadge(p.getIcon(), p.getImagePath(), 64);

        Label name = new Label(p.getName().toUpperCase(Locale.FRENCH));
        name.getStyleClass().add("product-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        name.setMinHeight(38);

        Label price = new Label(Money.compact(p.getPriceCents()));
        price.getStyleClass().add("product-price");

        VBox box = new VBox(8, badge, name, price);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);
        box.setPadding(new javafx.geometry.Insets(12, 10, 12, 10));

        plusOne.getStyleClass().add("product-plus");
        plusOne.setOpacity(0);
        plusOne.setMouseTransparent(true);
        StackPane.setAlignment(plusOne, Pos.TOP_RIGHT);
        StackPane.setMargin(plusOne, new javafx.geometry.Insets(10, 12, 0, 0));

        getChildren().addAll(box, plusOne);

        DropShadow shadow = new DropShadow(14, 0, 6, Color.rgb(0, 0, 0, 0.42));
        setEffect(shadow);
        Anim.installCardEffects(this, shadow, 6, 1.03);

        // Ajout au relâchement du bouton, sauf si le doigt/la souris a glissé (défilement tactile)
        final double[] press = new double[2];
        setOnMousePressed(e -> {
            press[0] = e.getSceneX();
            press[1] = e.getSceneY();
        });
        setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            double moved = Math.hypot(e.getSceneX() - press[0], e.getSceneY() - press[1]);
            if (moved < 14 && getBoundsInLocal().contains(e.getX(), e.getY())) {
                onAdd.accept(p);
                flashAdded();
            }
        });
    }

    /** Petit "+1" qui s'envole depuis la carte. */
    private void flashAdded() {
        plusOne.setTranslateY(6);
        plusOne.setOpacity(1);
        new Timeline(new KeyFrame(Duration.millis(450),
                new KeyValue(plusOne.translateYProperty(), -22, Interpolator.EASE_OUT),
                new KeyValue(plusOne.opacityProperty(), 0, Interpolator.EASE_IN))).play();
    }
}
