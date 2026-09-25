package com.cafemanager.ui;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Pavé numérique tactile qui écrit dans un champ texte (montant reçu, remise...). */
public class Keypad extends GridPane {

    public Keypad(TextField target) {
        setHgap(10);
        setVgap(10);
        String[] keys = {"7", "8", "9", "4", "5", "6", "1", "2", "3", ",", "0", "⌫"};
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            Button b = new Button(k);
            b.getStyleClass().addAll("btn", "keypad-btn", "pressable");
            b.setFocusTraversable(false);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMaxHeight(Double.MAX_VALUE);
            GridPane.setHgrow(b, Priority.ALWAYS);
            GridPane.setVgrow(b, Priority.ALWAYS);
            b.setOnAction(e -> press(target, k));
            add(b, i % 3, i / 3);
        }
    }

    private static void press(TextField target, String key) {
        String t = target.getText() == null ? "" : target.getText();
        switch (key) {
            case "⌫":
                if (!t.isEmpty()) {
                    target.setText(t.substring(0, t.length() - 1));
                }
                break;
            case ",":
                if (!t.contains(",") && !t.contains(".")) {
                    target.setText((t.isEmpty() ? "0" : t) + ",");
                }
                break;
            default:
                target.setText(t + key);
        }
        target.positionCaret(target.getText().length());
    }

    /** Filtre de saisie : chiffres et un séparateur décimal (2 décimales max). */
    public static void restrictToAmount(TextField field, boolean decimals) {
        String regex = decimals ? "\\d{0,7}([.,]\\d{0,2})?" : "\\d{0,3}";
        field.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change ->
                change.getControlNewText().matches(regex) ? change : null));
    }
}
