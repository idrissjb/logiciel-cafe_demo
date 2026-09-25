package com.cafemanager.ui;

import com.cafemanager.model.DiscountMode;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.Money;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panneau "ticket" toujours visible à droite de la caisse : un papier de caisse (lignes modifiables, sous-total,
 * remise, total) et les gros boutons d'action. Il se met à jour en temps réel sans jamais être reconstruit
 * entièrement (les lignes apparaissent / disparaissent avec une animation).
 */
public class TicketPanel extends VBox {

    // --- callbacks branchés par POSController
    public Consumer<OrderItem> onPlus = i -> { };
    public Consumer<OrderItem> onMinus = i -> { };
    public Consumer<OrderItem> onRemove = i -> { };
    public Runnable onPay = () -> { };
    public Runnable onQuickPay = () -> { };
    public Runnable onPrint = () -> { };
    public Runnable onDiscount = () -> { };
    public Runnable onCancel = () -> { };
    public Runnable onPickTable = () -> { };
    public Runnable onPreview = () -> { };

    private Order order;
    private final Map<OrderItem, TicketLineView> views = new HashMap<>();
    private OrderItem selected;

    private final Label title = new Label();
    private final Label meta = new Label();
    private final Button tableButton = new Button();
    private final VBox lines = new VBox(2);
    private final ScrollPane scroll = new ScrollPane(lines);
    private final Label emptyHint = new Label("Touchez un produit\npour l'ajouter au ticket");
    private final Label subtotal = new Label();
    private final Label discountLabel = new Label("Remise");
    private final Label discount = new Label();
    private final Label total = new Label();
    private final Label countLabel = new Label();

    private final Button payBtn = action("💵  PAIEMENT", "btn-primary", "btn-xl");
    private final Button quickBtn = action("✅  ENCAISSER", "btn-success", "btn-xl");
    private final Button printBtn = action("🖨  IMPRIMER", "btn-secondary", "btn-lg");
    private final Button discountBtn = action("💸  REMISE", "btn-secondary", "btn-lg");
    private final Button cancelBtn = action("🗑  ANNULER", "btn-danger-soft", "btn-lg");

    public TicketPanel() {
        getStyleClass().add("ticket-panel");
        setSpacing(14);
        setMinWidth(400);
        setPrefWidth(430);
        setMaxWidth(460);

        getChildren().addAll(buildPaper(), buildActions());
        VBox.setVgrow(getChildren().get(0), Priority.ALWAYS);

        payBtn.setOnAction(e -> onPay.run());
        quickBtn.setOnAction(e -> onQuickPay.run());
        printBtn.setOnAction(e -> onPrint.run());
        discountBtn.setOnAction(e -> onDiscount.run());
        cancelBtn.setOnAction(e -> onCancel.run());
        tableButton.setOnAction(e -> onPickTable.run());
        quickBtn.setTooltip(new Tooltip("Encaissement rapide : espèces, montant exact"));
    }

    // ------------------------------------------------------------ construction

    private VBox buildPaper() {
        title.getStyleClass().add("ticket-title");
        meta.getStyleClass().add("ticket-meta");
        tableButton.getStyleClass().addAll("ticket-table-btn", "pressable");
        tableButton.setFocusTraversable(false);

        Button previewBtn = Emoji.button("👁");
        previewBtn.getStyleClass().addAll("ticket-mini-btn", "pressable");
        previewBtn.setFocusTraversable(false);
        previewBtn.setTooltip(new Tooltip("Aperçu du ticket"));
        previewBtn.setOnAction(e -> onPreview.run());
        HBox head = new HBox(8, title, Dialogs.spacer(), previewBtn);
        head.setAlignment(Pos.CENTER_LEFT);
        tableButton.setMaxWidth(Double.MAX_VALUE);
        tableButton.setAlignment(Pos.CENTER_LEFT);

        Label colProduct = new Label("PRODUIT");
        Label colPrice = new Label("PRIX");
        colProduct.getStyleClass().add("ticket-col");
        colPrice.getStyleClass().add("ticket-col");
        HBox cols = new HBox(colProduct, Dialogs.spacer(), colPrice);
        cols.setPadding(new Insets(2, 4, 0, 4));

        lines.getStyleClass().add("ticket-lines");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().addAll("flat-scroll", "ticket-scroll");
        emptyHint.getStyleClass().add("ticket-empty");
        emptyHint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        StackPane listHolder = new StackPane(scroll, emptyHint);
        VBox.setVgrow(listHolder, Priority.ALWAYS);
        emptyHint.setMouseTransparent(true);

        countLabel.getStyleClass().add("ticket-meta");

        GridPane totals = new GridPane();
        totals.setVgap(4);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHalignment(javafx.geometry.HPos.RIGHT);
        totals.getColumnConstraints().addAll(c1, c2);
        Label subLbl = new Label("Sous-total");
        subLbl.getStyleClass().add("total-small");
        subtotal.getStyleClass().add("total-small");
        discountLabel.getStyleClass().add("total-small");
        discount.getStyleClass().add("total-small");
        totals.add(subLbl, 0, 0);
        totals.add(subtotal, 1, 0);
        totals.add(discountLabel, 0, 1);
        totals.add(discount, 1, 1);

        Label totalLbl = new Label("TOTAL");
        totalLbl.getStyleClass().add("total-big-label");
        total.getStyleClass().add("total-big");
        HBox totalRow = new HBox(totalLbl, Dialogs.spacer(), total);
        totalRow.setAlignment(Pos.BASELINE_LEFT);

        VBox paper = new VBox(8, head, meta, tableButton, dashed(), cols, listHolder, dashed(), countLabel, totals, totalRow);
        paper.getStyleClass().add("ticket-paper");
        return paper;
    }

    private static Region dashed() {
        Region r = new Region();
        r.getStyleClass().add("ticket-dash");
        r.setMinHeight(1);
        r.setPrefHeight(1);
        return r;
    }

    private GridPane buildActions() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        for (int i = 0; i < 6; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 6);
            grid.getColumnConstraints().add(cc);
        }
        grid.add(payBtn, 0, 0, 3, 1);
        grid.add(quickBtn, 3, 0, 3, 1);
        grid.add(printBtn, 0, 1, 2, 1);
        grid.add(discountBtn, 2, 1, 2, 1);
        grid.add(cancelBtn, 4, 1, 2, 1);
        for (Button b : new Button[]{payBtn, quickBtn, printBtn, discountBtn, cancelBtn}) {
            b.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(b, Priority.ALWAYS);
        }
        return grid;
    }

    private static Button action(String text, String... classes) {
        Button b = Emoji.button(text);
        b.getStyleClass().addAll("btn", "pressable");
        b.getStyleClass().addAll(classes);
        b.setFocusTraversable(false);
        return b;
    }

    // ------------------------------------------------------------- mise à jour

    /** Affiche une commande (reconstruction complète, sans animation). */
    public void setOrder(Order o) {
        this.order = o;
        lines.getChildren().clear();
        views.clear();
        selected = null;
        for (OrderItem it : o.getItems()) {
            addView(it, false);
        }
        if (!o.getItems().isEmpty()) {
            select(o.getItems().get(o.getItems().size() - 1));
        }
        refreshTotals(false);
    }

    /** Un produit vient d'être cliqué : nouvelle ligne (apparition douce) ou quantité mise à jour. */
    public void itemAdded(OrderItem item) {
        TicketLineView v = views.get(item);
        if (v == null) {
            addView(item, true);
            Platform.runLater(() -> scroll.setVvalue(1.0));
        } else {
            v.refresh();
            v.bump();
            ensureVisible(v);
        }
        select(item);
        refreshTotals(true);
    }

    public void itemChanged(OrderItem item) {
        TicketLineView v = views.get(item);
        if (v != null) {
            v.refresh();
            v.bump();
        }
        select(item);
        refreshTotals(true);
    }

    public void itemRemoved(OrderItem item) {
        TicketLineView v = views.remove(item);
        if (v != null) {
            Anim.fadeOut(v, 140, () -> lines.getChildren().remove(v));
        }
        if (selected == item) {
            selected = null;
            if (!order.getItems().isEmpty()) {
                select(order.getItems().get(order.getItems().size() - 1));
            }
        }
        refreshTotals(true);
    }

    public OrderItem selectedItem() {
        return selected != null && order != null && order.getItems().contains(selected) ? selected : null;
    }

    private void addView(OrderItem it, boolean animate) {
        TicketLineView v = new TicketLineView(it, this::select, i -> onMinus.accept(i), i -> onPlus.accept(i),
                i -> onRemove.accept(i));
        views.put(it, v);
        lines.getChildren().add(v);
        if (animate) {
            Anim.slideIn(v, 14, 170);
        }
    }

    private void select(OrderItem it) {
        if (selected != null && views.containsKey(selected)) {
            views.get(selected).setSelected(false);
        }
        selected = it;
        TicketLineView v = views.get(it);
        if (v != null) {
            v.setSelected(true);
        }
    }

    private void ensureVisible(TicketLineView v) {
        Platform.runLater(() -> {
            double contentH = lines.getBoundsInLocal().getHeight();
            double viewH = scroll.getViewportBounds().getHeight();
            if (contentH > viewH) {
                double y = v.getBoundsInParent().getMinY();
                scroll.setVvalue(Math.min(1, Math.max(0, y / (contentH - viewH))));
            }
        });
    }

    /** Met à jour en-tête, totaux et boutons. */
    public void refreshTotals(boolean animate) {
        if (order == null) {
            return;
        }
        title.setText("TICKET #" + order.getTicketNumber());
        meta.setText(DateUtil.date(order.getCreatedAt()) + "  ·  " + DateUtil.time(order.getCreatedAt())
                + (order.getServerName() != null ? "  ·  " + order.getServerName() : ""));
        Emoji.text(tableButton, order.getTableName() == null ? "🍽  Sans table  ▾" : "🍽  " + order.getTableName() + "  ▾");

        boolean empty = order.isEmpty();
        emptyHint.setVisible(empty);
        scroll.setVisible(!empty);
        int n = order.itemCount();
        countLabel.setText(empty ? "" : n + (n > 1 ? " articles" : " article"));

        subtotal.setText(Money.fmt(order.subtotal()));
        if (order.getDiscountMode() == DiscountMode.PERCENT && order.discount() > 0) {
            discountLabel.setText("Remise (" + order.getDiscountValue() + " %)");
        } else {
            discountLabel.setText("Remise");
        }
        discount.setText(order.discount() > 0 ? "-" + Money.fmt(order.discount()) : Money.fmt(0));

        String newTotal = Money.fmt(order.total());
        boolean changed = !newTotal.equals(total.getText());
        total.setText(newTotal);
        if (animate && changed) {
            Anim.pop(total, 1.07);
        }

        payBtn.setDisable(empty);
        quickBtn.setDisable(empty);
        printBtn.setDisable(empty);
        discountBtn.setDisable(empty);
        cancelBtn.setDisable(empty && !order.isPersisted());
    }

    /** Active / désactive les boutons d'encaissement (rôle sans droit de paiement). */
    public void setPaymentAllowed(boolean allowed) {
        payBtn.setVisible(allowed);
        payBtn.setManaged(allowed);
        quickBtn.setVisible(allowed);
        quickBtn.setManaged(allowed);
        GridPane grid = (GridPane) getChildren().get(1);
        if (!allowed) {
            GridPane.setRowIndex(payBtn, 0);
        }
        Emoji.text(printBtn, allowed ? "🖨  IMPRIMER" : "🖨  ADDITION");
        grid.requestLayout();
    }
}
