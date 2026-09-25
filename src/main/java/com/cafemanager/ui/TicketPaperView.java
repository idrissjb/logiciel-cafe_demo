package com.cafemanager.ui;

import com.cafemanager.service.TicketLine;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/** Aperçu à l'écran du ticket thermique (police à chasse fixe, fond papier). */
public class TicketPaperView extends VBox {

    public TicketPaperView(List<TicketLine> lines, int cols) {
        getStyleClass().add("thermal-paper");
        setPadding(new Insets(22, 20, 26, 20));
        setSpacing(0);
        setMaxWidth(USE_PREF_SIZE);

        double base = cols <= 32 ? 13.5 : 12.5;
        Font probe = Font.font(monoFamily(), base);
        for (TicketLine l : lines) {
            Label label = new Label(l.text().isEmpty() ? " " : l.text());
            label.setFont(Font.font(monoFamily(), l.bold() ? FontWeight.BOLD : FontWeight.NORMAL, base * l.scale()));
            label.getStyleClass().add("thermal-line");
            label.setWrapText(false);
            label.setMinWidth(USE_PREF_SIZE);
            getChildren().add(label);
        }
        // largeur = cols caractères
        double charW = new javafx.scene.text.Text("0") {{ setFont(probe); }}.getLayoutBounds().getWidth();
        setPrefWidth(charW * cols + 40);
        setMinWidth(charW * cols + 40);
    }

    private static String monoFamily() {
        for (String f : new String[]{"Consolas", "Courier New", "DejaVu Sans Mono", "Menlo", "Liberation Mono"}) {
            if (Font.getFamilies().contains(f)) {
                return f;
            }
        }
        return "Monospaced";
    }
}
