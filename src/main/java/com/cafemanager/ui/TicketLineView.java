package com.cafemanager.ui;

import com.cafemanager.model.OrderItem;
import com.cafemanager.util.Money;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Une ligne du ticket : nom, "qté × prix", total et boutons ➕ ➖ 🗑. */
class TicketLineView extends VBox {

    private final OrderItem item;
    private final Label name = new Label();
    private final Label total = new Label();
    private final Label detail = new Label();
    private final Label qty = new Label();

    TicketLineView(OrderItem item, Consumer<OrderItem> onSelect, Consumer<OrderItem> onMinus,
                   Consumer<OrderItem> onPlus, Consumer<OrderItem> onRemove) {
        this.item = item;
        getStyleClass().add("ticket-line");
        setSpacing(4);

        name.getStyleClass().add("line-name");
        name.setMaxWidth(Double.MAX_VALUE);
        name.setWrapText(true);
        total.getStyleClass().add("line-total");
        HBox top = new HBox(8, name, total);
        top.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(name, Priority.ALWAYS);

        detail.getStyleClass().add("line-detail");
        Button minus = qtyButton("−", "qty-btn");
        Button plus = qtyButton("+", "qty-btn");
        Button del = qtyButton("🗑", "qty-btn", "qty-del");
        qty.getStyleClass().add("line-qty");
        qty.setMinWidth(28);
        qty.setAlignment(Pos.CENTER);
        minus.setOnAction(e -> onMinus.accept(item));
        plus.setOnAction(e -> onPlus.accept(item));
        del.setOnAction(e -> onRemove.accept(item));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(6, detail, spacer, minus, qty, plus, del);
        bottom.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(top, bottom);
        setOnMousePressed(e -> onSelect.accept(item));
        refresh();
    }

    private static Button qtyButton(String text, String... styles) {
        Button b = Emoji.button(text);
        b.getStyleClass().addAll(styles);
        b.getStyleClass().add("pressable");
        b.setFocusTraversable(false);
        return b;
    }

    OrderItem item() {
        return item;
    }

    void refresh() {
        name.setText(item.getProductName());
        detail.setText(item.getQuantity() + " × " + Money.fmt(item.getUnitPriceCents()));
        qty.setText(String.valueOf(item.getQuantity()));
        total.setText(Money.fmt(item.lineTotal()));
    }

    void setSelected(boolean selected) {
        if (selected) {
            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
        } else {
            getStyleClass().remove("selected");
        }
    }

    /** Petit effet lorsque la quantité change. */
    void bump() {
        Anim.pop(total, 1.12);
        Anim.pop(qty, 1.25);
    }
}
