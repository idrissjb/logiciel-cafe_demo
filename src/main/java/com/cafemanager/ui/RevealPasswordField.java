package com.cafemanager.ui;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Champ mot de passe avec bouton « œil » (afficher / masquer) et alerte Verr. Maj.
 * Utilisable directement dans un FXML ({@code promptText} est pris en charge).
 */
public class RevealPasswordField extends VBox {

    // Icônes Material Design (Apache 2.0)
    private static final String EYE = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5"
            + "c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8"
            + "c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_OFF = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89"
            + " 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28"
            + " 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21"
            + " 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08"
            + "l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16"
            + "c0-1.66-1.34-3-3-3l-.17.01z";

    private final PasswordField hidden = new PasswordField();
    private final TextField shown = new TextField();
    private final SVGPath icon = new SVGPath();
    private final Tooltip tip = new Tooltip("Afficher le mot de passe");
    private final Label capsLabel = new Label("⚠  Verr. Maj activée");
    private boolean revealed;

    public RevealPasswordField() {
        setSpacing(4);

        hidden.getStyleClass().addAll("login-input", "reveal-input");
        shown.getStyleClass().addAll("login-input", "reveal-input");
        shown.textProperty().bindBidirectional(hidden.textProperty());
        shown.promptTextProperty().bind(hidden.promptTextProperty());
        shown.setManaged(false);
        shown.setVisible(false);

        icon.setContent(EYE);
        icon.getStyleClass().add("eye-icon");
        Button eye = new Button();
        eye.setGraphic(icon);
        eye.getStyleClass().add("eye-button");
        eye.setFocusTraversable(false);
        eye.setTooltip(tip);
        eye.setOnAction(e -> toggle());
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        StackPane.setMargin(eye, new Insets(0, 8, 0, 0));

        capsLabel.getStyleClass().add("caps-warning");
        capsLabel.setManaged(false);
        capsLabel.setVisible(false);

        getChildren().addAll(new StackPane(hidden, shown, eye), capsLabel);

        for (TextField f : new TextField[]{hidden, shown}) {
            f.addEventHandler(KeyEvent.KEY_PRESSED, e -> refreshCaps());
            f.addEventHandler(KeyEvent.KEY_RELEASED, e -> refreshCaps());
            f.focusedProperty().addListener((o, was, now) -> refreshCaps());
        }
    }

    private void toggle() {
        revealed = !revealed;
        shown.setVisible(revealed);
        shown.setManaged(revealed);
        hidden.setVisible(!revealed);
        hidden.setManaged(!revealed);
        icon.setContent(revealed ? EYE_OFF : EYE);
        tip.setText(revealed ? "Masquer le mot de passe" : "Afficher le mot de passe");
        focusField();
        TextField active = revealed ? shown : hidden;
        active.positionCaret(active.getText().length());
    }

    /** Alerte visible seulement quand le champ a le focus et que Verr. Maj est activée. */
    private void refreshCaps() {
        Platform.runLater(() -> {
            boolean focused = hidden.isFocused() || shown.isFocused();
            boolean on = focused && Platform.isKeyLocked(KeyCode.CAPS).orElse(false);
            capsLabel.setVisible(on);
            capsLabel.setManaged(on);
        });
    }

    public void focusField() {
        (revealed ? shown : hidden).requestFocus();
    }

    /** Bordure rouge quand la saisie est invalide. */
    public void setInvalid(boolean invalid) {
        for (TextField f : new TextField[]{hidden, shown}) {
            f.getStyleClass().remove("login-input-error");
            if (invalid) {
                f.getStyleClass().add("login-input-error");
            }
        }
    }

    public String getText() {
        return hidden.getText();
    }

    public void setText(String text) {
        hidden.setText(text);
    }

    public void clear() {
        hidden.clear();
    }

    public StringProperty textProperty() {
        return hidden.textProperty();
    }

    public String getPromptText() {
        return hidden.getPromptText();
    }

    public void setPromptText(String prompt) {
        hidden.setPromptText(prompt);
    }
}
