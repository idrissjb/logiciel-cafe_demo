package com.cafemanager.ui;

import com.cafemanager.model.Order;
import com.cafemanager.model.Payment;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.service.PaymentService;
import com.cafemanager.service.SettingsService;
import com.cafemanager.service.TicketService;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Fenêtre de paiement : total, mode (espèces / carte / autre), montant reçu avec pavé numérique et calcul
 * automatique de la monnaie. Après validation : confirmation, impression automatique (selon les paramètres),
 * aperçu et retour à une nouvelle commande.
 */
public final class PaymentDialog {

    private final Order order;
    private final PaymentService payments = new PaymentService();
    private final BiConsumer<Order, Payment> onPaid;
    private final VBox card;
    private PaymentMethod method = PaymentMethod.CASH;

    private PaymentDialog(Order order, BiConsumer<Order, Payment> onPaid) {
        this.order = order;
        this.onPaid = onPaid;
        this.card = new VBox(20);
        card.getStyleClass().add("dialog-card");
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
    }

    /**
     * @param onPaid appelé dès que le paiement est enregistré (la caisse repart sur une nouvelle commande vide)
     */
    public static void show(Order order, BiConsumer<Order, Payment> onPaid) {
        PaymentDialog d = new PaymentDialog(order, onPaid);
        d.showForm();
        Modal.show(d.card);
    }

    // ================================================================ formulaire

    private void showForm() {
        long total = order.total();

        Label title = new Label("Paiement");
        title.getStyleClass().add("dialog-title");
        Label totalCaption = new Label("TOTAL À PAYER");
        totalCaption.getStyleClass().add("pay-caption");
        Label totalValue = new Label(Money.fmt(total));
        totalValue.getStyleClass().add("pay-total");
        VBox totalBox = new VBox(2, totalCaption, totalValue);
        totalBox.getStyleClass().add("pay-total-box");
        totalBox.setAlignment(Pos.CENTER);

        // ---- modes de paiement
        ToggleGroup group = new ToggleGroup();
        HBox methods = new HBox(12);
        for (PaymentMethod m : PaymentMethod.values()) {
            ToggleButton b = new ToggleButton();
            Emoji.above(b, m.getIcon(), m.getLabel());
            b.getStyleClass().addAll("method-card", "pressable");
            b.setToggleGroup(group);
            b.setUserData(m);
            b.setFocusTraversable(false);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            if (m == method) {
                b.setSelected(true);
            }
            methods.getChildren().add(b);
        }

        // ---- espèces
        TextField received = new TextField();
        received.getStyleClass().add("big-input");
        received.setPromptText("0.00");
        Keypad.restrictToAmount(received, true);
        Label unit = new Label(Money.currency());
        unit.getStyleClass().add("input-unit");
        unit.setMinWidth(Region.USE_PREF_SIZE);
        Label receivedCaption = new Label("Montant reçu");
        receivedCaption.getStyleClass().add("pay-caption");
        HBox receivedRow = new HBox(10, received, unit);
        receivedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(received, Priority.ALWAYS);

        HBox quick = new HBox(8);
        List<Long> suggestions = suggestions(total);
        for (int i = 0; i < suggestions.size(); i++) {
            long v = suggestions.get(i);
            Button chip = Dialogs.button(i == 0 ? "Exact" : Money.compact(v), "chip");
            chip.setFocusTraversable(false);
            chip.setMinWidth(Region.USE_PREF_SIZE);
            chip.setOnAction(e -> received.setText(Money.plain(v).replace('.', ',')));
            quick.getChildren().add(chip);
        }

        Label changeCaption = new Label("Monnaie à rendre");
        changeCaption.getStyleClass().add("pay-caption");
        Label change = new Label("—");
        change.getStyleClass().add("pay-change");
        VBox changeBox = new VBox(2, changeCaption, change);
        changeBox.getStyleClass().add("pay-change-box");

        Keypad pad = new Keypad(received);
        pad.setPrefSize(290, 250);

        VBox cashLeft = new VBox(12, receivedCaption, receivedRow, quick, changeBox);
        HBox.setHgrow(cashLeft, Priority.ALWAYS);
        cashLeft.setPrefWidth(370);
        cashLeft.setMinWidth(Region.USE_PREF_SIZE);
        HBox cashPane = new HBox(22, cashLeft, pad);

        Label otherInfo = new Label();
        otherInfo.getStyleClass().add("dialog-message");
        otherInfo.setWrapText(true);
        StackPane otherPane = new StackPane(otherInfo);
        otherPane.setAlignment(Pos.CENTER_LEFT);
        otherPane.setMinHeight(120);

        StackPane modePane = new StackPane(cashPane, otherPane);

        Button cancel = Dialogs.button("ANNULER", "btn-ghost", "btn-lg");
        Button validate = Dialogs.button("✔  VALIDER LE PAIEMENT", "btn-success", "btn-xl");
        validate.setDefaultButton(true);
        cancel.setOnAction(e -> Modal.close());
        HBox actions = new HBox(12, Dialogs.spacer(), cancel, validate);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // ---- logique d'affichage
        Runnable update = () -> {
            boolean cash = method == PaymentMethod.CASH;
            cashPane.setVisible(cash);
            cashPane.setManaged(cash);
            otherPane.setVisible(!cash);
            otherPane.setManaged(!cash);
            if (cash) {
                long rec = parse(received.getText());
                if (received.getText().isBlank()) {
                    change.setText("—");
                    change.getStyleClass().removeAll("ok", "bad");
                    validate.setDisable(true);
                } else if (rec >= total) {
                    change.setText(Money.fmt(rec - total));
                    change.getStyleClass().remove("bad");
                    if (!change.getStyleClass().contains("ok")) {
                        change.getStyleClass().add("ok");
                    }
                    validate.setDisable(false);
                } else {
                    change.setText("Il manque " + Money.fmt(total - rec));
                    change.getStyleClass().remove("ok");
                    if (!change.getStyleClass().contains("bad")) {
                        change.getStyleClass().add("bad");
                    }
                    validate.setDisable(true);
                }
            } else {
                otherInfo.setText(method == PaymentMethod.CARD
                        ? "Encaissez " + Money.fmt(total) + " sur le terminal de paiement, puis validez."
                        : "Encaissement de " + Money.fmt(total) + " par un autre moyen (mobile, chèque, bon...). Validez une fois reçu.");
                validate.setDisable(false);
            }
        };
        group.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            method = (PaymentMethod) b.getUserData();
            update.run();
            if (method == PaymentMethod.CASH) {
                received.requestFocus();
            }
        });
        received.textProperty().addListener((o, a, b) -> update.run());
        update.run();

        validate.setOnAction(e -> {
            try {
                long rec = method == PaymentMethod.CASH ? parse(received.getText()) : total;
                Payment p = payments.pay(order, method, rec);
                onPaid.accept(order, p);
                showSuccess(p);
            } catch (BusinessException ex) {
                Toast.warning(ex.getMessage());
                Anim.shake(validate);
            } catch (RuntimeException ex) {
                Toast.error("Erreur d'enregistrement : " + ex.getMessage());
            }
        });

        card.getChildren().setAll(title, totalBox, methods, modePane, actions);
        card.setPrefWidth(700);
        javafx.application.Platform.runLater(received::requestFocus);
    }

    // ================================================================ confirmation

    private void showSuccess(Payment p) {
        Label check = new Label("✔");
        check.getStyleClass().add("success-check");
        Label title = new Label("Paiement enregistré");
        title.getStyleClass().add("dialog-title");
        Label sub = new Label("Ticket N° " + order.getTicketNumber() + "  ·  " + Money.fmt(p.getAmountDueCents())
                + "  ·  " + p.getMethod().getLabel());
        sub.getStyleClass().add("dialog-detail");
        VBox head = new VBox(6, check, title, sub);
        head.setAlignment(Pos.CENTER);

        List<Node> parts = new ArrayList<>();
        parts.add(head);
        if (p.getMethod() == PaymentMethod.CASH) {
            Label cap = new Label("MONNAIE À RENDRE");
            cap.getStyleClass().add("pay-caption");
            Label val = new Label(Money.fmt(p.getChangeCents()));
            val.getStyleClass().addAll("pay-total", "ok");
            VBox changeBox = new VBox(2, cap, val);
            changeBox.setAlignment(Pos.CENTER);
            changeBox.getStyleClass().add("pay-total-box");
            parts.add(changeBox);
        }

        Label printStatus = new Label();
        printStatus.getStyleClass().add("dialog-detail");
        printStatus.setAlignment(Pos.CENTER);
        printStatus.setMaxWidth(Double.MAX_VALUE);
        parts.add(printStatus);

        Button preview = Dialogs.button("👁  APERÇU", "btn-secondary", "btn-lg");
        Button print = Dialogs.button("🖨  IMPRIMER LE TICKET", "btn-primary", "btn-lg");
        Button next = Dialogs.button("➕  NOUVELLE COMMANDE", "btn-success", "btn-lg");
        next.setDefaultButton(true);
        preview.setOnAction(e -> TicketActions.preview(order, p));
        print.setOnAction(e -> TicketActions.print(order, p));
        next.setOnAction(e -> Modal.close());
        HBox actions = new HBox(12, preview, print, next);
        actions.setAlignment(Pos.CENTER);
        parts.add(actions);

        card.getChildren().setAll(parts);
        Anim.fadeIn(card, 160);

        // Impression automatique si activée dans les paramètres
        if (SettingsService.get().autoPrint()) {
            printStatus.setText("Impression du ticket en cours…");
            new TicketService().printAsync(order, p).whenComplete((v, ex) -> javafx.application.Platform.runLater(() -> {
                if (ex == null) {
                    printStatus.setText("✔ Ticket imprimé");
                } else {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    printStatus.setText("⚠ " + c.getMessage() + " — utilisez l'aperçu / PDF.");
                }
            }));
        } else {
            printStatus.setText("Impression automatique désactivée (Paramètres).");
        }
    }

    // ================================================================ utilitaires

    private static long parse(String text) {
        try {
            return text == null || text.isBlank() ? 0 : Money.parse(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Montants proposés : exact, puis arrondis aux billets usuels (10, 50, 100, 200 DH). */
    private static List<Long> suggestions(long total) {
        List<Long> list = new ArrayList<>();
        list.add(total);
        for (long step : new long[]{1000, 5000, 10000, 20000}) {
            long v = ((total + step - 1) / step) * step;
            if (!list.contains(v)) {
                list.add(v);
            }
        }
        for (long extra : new long[]{10000, 20000}) {
            if (extra > total && !list.contains(extra) && list.size() < 5) {
                list.add(extra);
            }
        }
        return list.size() > 5 ? list.subList(0, 5) : list;
    }
}
