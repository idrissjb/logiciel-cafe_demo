package com.cafemanager.service;

import com.cafemanager.dao.TicketDAO;
import com.cafemanager.model.Order;
import com.cafemanager.model.OrderItem;
import com.cafemanager.model.Payment;
import com.cafemanager.model.PaymentMethod;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.DateUtil;
import com.cafemanager.util.Money;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Construction du ticket (80 mm ou 58 mm), aperçu, impression et export PDF. */
public class TicketService {

    private final TicketDAO ticketDAO = new TicketDAO();
    private final PrintService printService = new PrintService();
    private final SettingsService settings = SettingsService.get();

    /** Nombre de caractères par ligne : 42 pour 80 mm, 32 pour 58 mm. */
    public int columns() {
        return settings.ticketWidthMm() == 58 ? 32 : 42;
    }

    /**
     * Met en forme le ticket.
     *
     * @param payment   paiement (null pour une addition non réglée)
     * @param duplicate true pour une réimpression (mention DUPLICATA)
     */
    public List<TicketLine> build(Order order, Payment payment, boolean duplicate) {
        int cols = columns();
        String cur = Money.currency();
        List<TicketLine> out = new ArrayList<>();

        // --- En-tête du café
        out.add(new TicketLine(center(settings.cafeName().toUpperCase(), (int) (cols / 1.4)), true, 1.4));
        if (!settings.address().isBlank()) {
            wrap(settings.address(), cols).forEach(l -> out.add(TicketLine.plain(center(l, cols))));
        }
        if (!settings.phone().isBlank()) {
            out.add(TicketLine.plain(center("Tél : " + settings.phone(), cols)));
        }
        if (duplicate) {
            out.add(TicketLine.bold(center("*** DUPLICATA ***", cols)));
        } else if (payment == null) {
            out.add(TicketLine.bold(center("*** ADDITION ***", cols)));
        }
        out.add(TicketLine.plain(rule(cols)));

        // --- Informations du ticket
        LocalDateTime when = payment != null && payment.getPaidAt() != null ? payment.getPaidAt()
                : (order.getPaidAt() != null ? order.getPaidAt() : LocalDateTime.now());
        out.add(TicketLine.bold("Ticket N° : " + order.getTicketNumber()));
        out.add(TicketLine.plain(lr("Date : " + DateUtil.date(when), "Heure : " + DateUtil.time(when), cols)));
        out.add(TicketLine.plain("Serveur : " + nz(order.getServerName())));
        if (order.getTableName() != null) {
            out.add(TicketLine.plain("Table : " + order.getTableName()));
        }
        out.add(TicketLine.plain("Caisse : " + settings.registerNumber()));
        out.add(TicketLine.plain(rule(cols)));

        // --- Lignes
        for (OrderItem it : order.getItems()) {
            wrap(it.getProductName(), cols).forEach(l -> out.add(TicketLine.plain(l)));
            out.add(TicketLine.plain(lr(it.getQuantity() + " x " + Money.plain(it.getUnitPriceCents()) + " " + cur,
                    Money.plain(it.lineTotal()) + " " + cur, cols)));
        }
        out.add(TicketLine.plain(rule(cols)));

        // --- Totaux
        if (order.discount() > 0) {
            out.add(TicketLine.plain(lr("Sous-total", Money.plain(order.subtotal()) + " " + cur, cols)));
            out.add(TicketLine.plain(lr("Remise", "-" + Money.plain(order.discount()) + " " + cur, cols)));
        }
        int bigCols = (int) (cols / 1.3);
        out.add(new TicketLine(lr("TOTAL", Money.plain(order.total()) + " " + cur, bigCols), true, 1.3));
        if (settings.vatRate() > 0) {
            String rate = (settings.vatRate() == Math.rint(settings.vatRate()))
                    ? String.valueOf((int) settings.vatRate()) : String.valueOf(settings.vatRate());
            out.add(TicketLine.plain(lr("dont TVA " + rate + "%",
                    Money.plain(Money.vatIncluded(order.total(), settings.vatRate())) + " " + cur, cols)));
        }

        // --- Paiement
        if (payment != null) {
            out.add(TicketLine.plain(rule(cols)));
            out.add(TicketLine.plain("Paiement : " + payment.getMethod().getLabel()));
            if (payment.getMethod() == PaymentMethod.CASH) {
                out.add(TicketLine.plain(lr("Reçu :", Money.plain(payment.getAmountReceivedCents()) + " " + cur, cols)));
                out.add(TicketLine.plain(lr("Monnaie :", Money.plain(payment.getChangeCents()) + " " + cur, cols)));
            }
        } else if (order.getPaymentMethod() != null) {
            out.add(TicketLine.plain(rule(cols)));
            out.add(TicketLine.plain("Paiement : " + order.getPaymentMethod().getLabel()));
        }

        // --- Pied de page
        out.add(TicketLine.plain(rule(cols)));
        if (!settings.footerMessage().isBlank()) {
            wrap(settings.footerMessage(), cols).forEach(l -> out.add(TicketLine.plain(center(l, cols))));
        }
        out.add(TicketLine.plain(center("À bientôt ☕", cols)));
        return out;
    }

    /** Ticket d'un ticket déjà encaissé : détecte automatiquement s'il s'agit d'un duplicata. */
    public List<TicketLine> buildForPaidOrder(Order order, Payment payment) {
        boolean duplicate = order.getId() != null && ticketDAO.printCount(order.getId()) > 0;
        return build(order, payment, duplicate);
    }

    /**
     * Imprime en arrière-plan sur l'imprimante configurée (ou par défaut).
     * Un ticket payé voit son compteur d'impressions incrémenté.
     */
    public CompletableFuture<Void> printAsync(Order order, Payment payment) {
        final boolean paid = payment != null && order.getId() != null;
        final List<TicketLine> lines = paid ? buildForPaidOrder(order, payment) : build(order, payment, false);
        final int width = settings.ticketWidthMm();
        final String printer = settings.printerName();
        return printService.printAsync(lines, width, columns(), printer).thenRun(() -> {
            if (paid) {
                ticketDAO.registerPrint(order.getId());
            }
        });
    }

    /** Enregistre le ticket au format PDF (largeur du papier thermique). */
    public Path exportPdf(Order order, Payment payment, Path target) {
        List<TicketLine> lines = payment != null ? buildForPaidOrder(order, payment) : build(order, null, false);
        try {
            return printService.exportPdf(lines, settings.ticketWidthMm(), columns(), target);
        } catch (java.io.IOException e) {
            throw new BusinessException("Export PDF impossible : " + e.getMessage());
        }
    }

    // ----------------------------------------------------------- mise en page

    private static String rule(int cols) {
        return "-".repeat(cols);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String center(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int left = (width - text.length()) / 2;
        return " ".repeat(left) + text;
    }

    /** Texte à gauche et à droite sur une même ligne de {@code width} caractères. */
    static String lr(String left, String right, int width) {
        int space = width - left.length() - right.length();
        if (space < 1) {
            int maxLeft = Math.max(1, width - right.length() - 1);
            left = left.substring(0, Math.min(left.length(), maxLeft));
            space = Math.max(1, width - left.length() - right.length());
        }
        return left + " ".repeat(space) + right;
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (word.length() > width) {
                if (cur.length() > 0) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (cur.length() + word.length() + (cur.length() > 0 ? 1 : 0) > width) {
                lines.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) {
                cur.append(' ');
            }
            cur.append(word);
        }
        if (cur.length() > 0 || lines.isEmpty()) {
            lines.add(cur.toString());
        }
        return lines;
    }
}
