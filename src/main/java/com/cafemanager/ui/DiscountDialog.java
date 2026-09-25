package com.cafemanager.ui;

import com.cafemanager.model.DiscountMode;
import com.cafemanager.model.Order;
import com.cafemanager.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Remise en pourcentage ou en montant fixe sur le ticket en cours. */
public final class DiscountDialog {

    private DiscountDialog() {
    }

    public static void show(Order order, Runnable onApplied) {
        ToggleGroup mode = new ToggleGroup();
        ToggleButton percent = segment("Pourcentage  %", mode);
        ToggleButton amount = segment("Montant  " + Money.currency(), mode);
        if (order.getDiscountMode() == DiscountMode.AMOUNT) {
            amount.setSelected(true);
        } else {
            percent.setSelected(true);
        }
        HBox segments = new HBox(0, percent, amount);
        segments.getStyleClass().add("segmented");

        TextField input = new TextField();
        input.getStyleClass().add("big-input");
        input.setPromptText("0");
        if (order.getDiscountMode() == DiscountMode.PERCENT) {
            input.setText(String.valueOf(order.getDiscountValue()));
        } else if (order.getDiscountMode() == DiscountMode.AMOUNT) {
            input.setText(Money.plain(order.getDiscountValue()).replace('.', ','));
        }
        Label unit = new Label();
        unit.getStyleClass().add("input-unit");
        HBox inputRow = new HBox(10, input, unit);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, javafx.scene.layout.Priority.ALWAYS);

        HBox chips = new HBox(8);
        for (int p : new int[]{5, 10, 15, 20, 50}) {
            Button chip = Dialogs.button(p + " %", "chip");
            chip.setOnAction(e -> {
                percent.setSelected(true);
                input.setText(String.valueOf(p));
            });
            chips.getChildren().add(chip);
        }

        Label preview = new Label();
        preview.getStyleClass().add("dialog-detail");

        Keypad pad = new Keypad(input);
        pad.setPrefWidth(300);
        pad.setPrefHeight(230);

        Runnable refresh = () -> {
            boolean pct = percent.isSelected();
            long value = parse(input.getText(), pct);
            long disc = pct ? Math.min(order.subtotal(), Money.percentOf(order.subtotal(), Math.min(value, 100)))
                    : Math.min(order.subtotal(), value);
            preview.setText("Remise : -" + Money.fmt(disc) + "     Nouveau total : " + Money.fmt(order.subtotal() - disc));
        };
        mode.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            input.clear();
            unit.setText(percent.isSelected() ? "%" : Money.currency());
            Keypad.restrictToAmount(input, !percent.isSelected());
            refresh.run();
        });
        input.textProperty().addListener((o, a, b) -> refresh.run());
        unit.setText(percent.isSelected() ? "%" : Money.currency());
        Keypad.restrictToAmount(input, !percent.isSelected());
        refresh.run();

        VBox left = new VBox(14, segments, inputRow, chips, preview);
        left.setPrefWidth(360);
        HBox body = new HBox(24, left, pad);
        body.setPadding(new Insets(4, 0, 0, 0));

        Button cancel = Dialogs.button("ANNULER", "btn-ghost", "btn-lg");
        Button remove = Dialogs.button("RETIRER LA REMISE", "btn-secondary", "btn-lg");
        Button apply = Dialogs.button("APPLIQUER", "btn-primary", "btn-lg");
        apply.setDefaultButton(true);
        cancel.setOnAction(e -> Modal.close());
        remove.setOnAction(e -> {
            order.setDiscount(DiscountMode.NONE, 0);
            Modal.close();
            onApplied.run();
        });
        apply.setOnAction(e -> {
            boolean pct = percent.isSelected();
            long value = parse(input.getText(), pct);
            if (value <= 0) {
                Anim.shake(input);
                return;
            }
            if (pct && value > 100) {
                Toast.warning("Le pourcentage ne peut pas dépasser 100 %");
                Anim.shake(input);
                return;
            }
            if (!pct && value > order.subtotal()) {
                Toast.warning("La remise dépasse le sous-total");
                Anim.shake(input);
                return;
            }
            order.setDiscount(pct ? DiscountMode.PERCENT : DiscountMode.AMOUNT, value);
            Modal.close();
            onApplied.run();
        });

        VBox card = order.getDiscountMode() != DiscountMode.NONE
                ? Dialogs.card("Remise", body, cancel, remove, apply)
                : Dialogs.card("Remise", body, cancel, apply);
        Modal.show(card);
        input.requestFocus();
    }

    private static long parse(String text, boolean percent) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return percent ? Long.parseLong(text.trim()) : Money.parse(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static ToggleButton segment(String text, ToggleGroup g) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g);
        b.getStyleClass().addAll("segment", "pressable");
        b.setFocusTraversable(false);
        return b;
    }
}
