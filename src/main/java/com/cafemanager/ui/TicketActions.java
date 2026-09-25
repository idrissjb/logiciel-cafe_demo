package com.cafemanager.ui;

import com.cafemanager.model.Order;
import com.cafemanager.model.Payment;
import com.cafemanager.service.TicketService;
import com.cafemanager.util.AppPaths;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

/** Actions communes sur un ticket (imprimer, aperçu, PDF) utilisées par la caisse, l'historique et le paiement. */
public final class TicketActions {

    private static final TicketService TICKETS = new TicketService();

    private TicketActions() {
    }

    /** Imprime en arrière-plan et affiche le résultat dans un toast. */
    public static void print(Order order, Payment payment) {
        try {
            TICKETS.printAsync(order, payment).whenComplete((v, ex) -> Platform.runLater(() -> {
                if (ex == null) {
                    Toast.success("Ticket " + order.getTicketNumber() + " envoyé à l'imprimante");
                } else {
                    Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
                    Toast.error(cause.getMessage() == null ? "Échec de l'impression" : cause.getMessage());
                }
            }));
        } catch (RuntimeException e) {
            Toast.error(e.getMessage());
        }
    }

    /** Exporte le ticket en PDF (boîte de dialogue "Enregistrer sous"). */
    public static void savePdf(Order order, Payment payment) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le ticket en PDF");
        fc.setInitialDirectory(AppPaths.exports().toFile());
        fc.setInitialFileName("ticket-" + order.getTicketNumber() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        File f = fc.showSaveDialog(Modal.window());
        if (f == null) {
            return;
        }
        try {
            Path p = TICKETS.exportPdf(order, payment, f.toPath());
            Toast.success("PDF enregistré : " + p.getFileName());
        } catch (RuntimeException e) {
            Toast.error(e.getMessage());
        }
    }

    public static void preview(Order order, Payment payment) {
        TicketPreviewDialog.show(order, payment);
    }
}
