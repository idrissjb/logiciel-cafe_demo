package com.cafemanager.ui;

import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.model.Payment;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Détails d'un ticket : informations, lignes, remise, paiement. */
public final class OrderDetailsDialog {

    private OrderDetailsDialog() {
    }

    public static void show(Order o, Payment p) {
        GridPane info = new GridPane();
        info.setHgap(24);
        info.setVgap(10);
        int r = 0;
        r = row(info, r, "Ticket", "N° " + o.getTicketNumber());
        r = row(info, r, "Date", DateUtil.date(o.getPaidAt()) + " à " + DateUtil.time(o.getPaidAt()));
        r = row(info, r, "Serveur", o.getServerName() == null ? "—" : o.getServerName());
        r = row(info, r, "Table", o.getTableName() == null ? "Comptoir / sans table" : o.getTableName());
        r = row(info, r, "Paiement", o.getPaymentMethod() == null ? "—" : o.getPaymentMethod().getLabel());
        if (p != null && p.getMethod() == PaymentMethod.CASH) {
            r = row(info, r, "Reçu / Monnaie", Money.fmt(p.getAmountReceivedCents()) + "  /  " + Money.fmt(p.getChangeCents()));
        }

        VBox lines = new VBox(0);
        lines.getStyleClass().add("details-lines");
        for (OrderItem it : o.getItems()) {
            Label name = new Label(it.getProductName());
            name.getStyleClass().add("dialog-message");
            Label qty = new Label(it.getQuantity() + " × " + Money.fmt(it.getUnitPriceCents()));
            qty.getStyleClass().add("dialog-detail");
            Label total = new Label(Money.fmt(it.lineTotal()));
            total.getStyleClass().add("dialog-message");
            VBox left = new VBox(2, name, qty);
            HBox line = new HBox(left, Dialogs.spacer(), total);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setPadding(new Insets(10, 0, 10, 0));
            line.setStyle("-fx-border-color: transparent transparent -c-line transparent; -fx-border-width: 0 0 1 0;");
            lines.getChildren().add(line);
        }

        GridPane totals = new GridPane();
        totals.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        totals.getColumnConstraints().addAll(c1, new ColumnConstraints());
        int t = 0;
        if (o.discount() > 0) {
            t = totalRow(totals, t, "Sous-total", Money.fmt(o.subtotal()), false);
            t = totalRow(totals, t, "Remise", "-" + Money.fmt(o.discount()), false);
        }
        totalRow(totals, t, "TOTAL", Money.fmt(o.total()), true);

        VBox body = new VBox(16, info, lines, totals);
        body.setPrefWidth(480);
        Button close = Dialogs.button("FERMER", "btn-ghost", "btn-lg");
        Button view = Dialogs.button("👁  VOIR LE TICKET", "btn-secondary", "btn-lg");
        Button print = Dialogs.button("🖨  RÉIMPRIMER", "btn-primary", "btn-lg");
        close.setOnAction(e -> Modal.close());
        view.setOnAction(e -> TicketActions.preview(o, p));
        print.setOnAction(e -> TicketActions.print(o, p));
        Modal.show(Dialogs.card("Détails du ticket", body, close, view, print));
    }

    private static int row(GridPane g, int r, String k, String v) {
        Label key = new Label(k);
        key.getStyleClass().add("dialog-detail");
        Label val = new Label(v);
        val.getStyleClass().add("dialog-message");
        g.add(key, 0, r);
        g.add(val, 1, r);
        return r + 1;
    }

    private static int totalRow(GridPane g, int r, String k, String v, boolean big) {
        Label key = new Label(k);
        Label val = new Label(v);
        key.getStyleClass().add(big ? "dialog-title" : "dialog-detail");
        val.getStyleClass().add(big ? "dialog-title" : "dialog-detail");
        if (big) {
            val.setStyle("-fx-text-fill: -c-accent-hi;");
        }
        g.add(key, 0, r);
        g.add(val, 1, r);
        return r + 1;
    }
}
