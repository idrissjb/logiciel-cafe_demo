package com.cafemanager.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Fabrique de cartes de dialogue au style de l'application + dialogues standard (confirmation, info, erreur). */
public final class Dialogs {

    public static final String DEFAULT_CONFIRM_TEXT = "Voulez-vous vraiment effectuer cette action ?";

    private Dialogs() {
    }

    /** Carte de dialogue : titre, corps, barre d'actions à droite. */
    public static VBox card(String title, Node body, Node... actions) {
        Label t = new Label(title);
        t.getStyleClass().add("dialog-title");
        VBox card = new VBox(18);
        card.getStyleClass().add("dialog-card");
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        card.getChildren().add(t);
        if (body != null) {
            card.getChildren().add(body);
        }
        if (actions != null && actions.length > 0) {
            HBox bar = new HBox(12);
            bar.setAlignment(Pos.CENTER_RIGHT);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            bar.getChildren().add(spacer);
            bar.getChildren().addAll(actions);
            card.getChildren().add(bar);
        }
        return card;
    }

    public static Button button(String text, String... styleClasses) {
        Button b = Emoji.button(text);
        b.getStyleClass().addAll("btn", "pressable");
        b.getStyleClass().addAll(styleClasses);
        b.setFocusTraversable(true);
        return b;
    }

    /** Fenêtre de confirmation (annuler / confirmer) pour les actions importantes. */
    public static void confirm(String title, String detail, String confirmLabel, boolean danger, Runnable onConfirm) {
        Label msg = new Label(DEFAULT_CONFIRM_TEXT);
        msg.getStyleClass().add("dialog-message");
        msg.setWrapText(true);
        VBox body = new VBox(8, msg);
        if (detail != null && !detail.isBlank()) {
            Label d = new Label(detail);
            d.getStyleClass().add("dialog-detail");
            d.setWrapText(true);
            body.getChildren().add(d);
        }
        body.setPrefWidth(420);

        Button cancel = button("ANNULER", "btn-ghost", "btn-lg");
        Button ok = button(confirmLabel == null ? "CONFIRMER" : confirmLabel, danger ? "btn-danger" : "btn-primary", "btn-lg");
        cancel.setOnAction(e -> Modal.close());
        ok.setOnAction(e -> {
            Modal.close();
            onConfirm.run();
        });
        if (!danger) {
            ok.setDefaultButton(true);
        }
        Modal.show(card(title, body, cancel, ok));
    }

    /** Petite fenêtre de saisie d'un texte (renommer, ajouter...). */
    public static void prompt(String title, String label, String initial, String okLabel,
                              java.util.function.Consumer<String> onOk) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        javafx.scene.control.TextField field = new javafx.scene.control.TextField(initial == null ? "" : initial);
        field.setPrefWidth(380);
        VBox body = new VBox(8, l, field);
        Button cancel = button("ANNULER", "btn-ghost", "btn-lg");
        Button ok = button(okLabel == null ? "ENREGISTRER" : okLabel, "btn-primary", "btn-lg");
        ok.setDefaultButton(true);
        cancel.setOnAction(e -> Modal.close());
        ok.setOnAction(e -> {
            String v = field.getText() == null ? "" : field.getText().trim();
            if (v.isEmpty()) {
                Anim.shake(field);
                return;
            }
            Modal.close();
            onOk.accept(v);
        });
        Modal.show(card(title, body, cancel, ok));
        field.requestFocus();
        field.selectAll();
    }

    public static void info(String title, String message) {
        Label msg = new Label(message);
        msg.getStyleClass().add("dialog-message");
        msg.setWrapText(true);
        msg.setPrefWidth(420);
        Button ok = button("OK", "btn-primary", "btn-lg");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> Modal.close());
        Modal.show(card(title, msg, ok));
    }

    public static void error(String message) {
        info("Une erreur est survenue", message);
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Insets pad(double all) {
        return new Insets(all);
    }
}
