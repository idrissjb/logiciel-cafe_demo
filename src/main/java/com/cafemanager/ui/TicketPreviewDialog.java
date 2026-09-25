package com.cafemanager.ui;

import com.cafemanager.model.Order;
import com.cafemanager.model.Payment;
import com.cafemanager.service.TicketLine;
import com.cafemanager.service.TicketService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Aperçu du ticket thermique avec impression et export PDF. */
public final class TicketPreviewDialog {

    private TicketPreviewDialog() {
    }

    public static void show(Order order, Payment payment) {
        TicketService ts = new TicketService();
        List<TicketLine> lines = payment != null ? ts.buildForPaidOrder(order, payment) : ts.build(order, null, false);
        TicketPaperView paper = new TicketPaperView(lines, ts.columns());

        StackPane holder = new StackPane(paper);
        holder.setAlignment(Pos.TOP_CENTER);
        holder.getStyleClass().add("preview-holder");
        ScrollPane sp = new ScrollPane(holder);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().addAll("flat-scroll", "preview-scroll");
        sp.setPrefViewportHeight(480);
        sp.setPrefWidth(paper.getPrefWidth() + 60);

        Button print = Dialogs.button("🖨  IMPRIMER", "btn-primary", "btn-lg");
        Button pdf = Dialogs.button("📄  ENREGISTRER EN PDF", "btn-secondary", "btn-lg");
        Button close = Dialogs.button("FERMER", "btn-ghost", "btn-lg");
        print.setOnAction(e -> TicketActions.print(order, payment));
        pdf.setOnAction(e -> TicketActions.savePdf(order, payment));
        close.setOnAction(e -> Modal.close());
        VBox card = Dialogs.card("Ticket " + order.getTicketNumber(), sp, close, pdf, print);
        Modal.show(card);
    }
}
