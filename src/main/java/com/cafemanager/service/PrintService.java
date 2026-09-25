package com.cafemanager.service;

import com.cafemanager.util.BusinessException;

import javax.print.PrintServiceLookup;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Impression des tickets via Java Print Service (java.awt.print) et export PDF.
 * <p>
 * Le ticket est dessiné en police à chasse fixe sur une page dont la largeur est celle du papier thermique
 * (80 mm ou 58 mm) : n'importe quelle imprimante thermique installée avec son pilote Windows/macOS/Linux convient.
 */
public class PrintService {

    private static final double MM_TO_PT = 72.0 / 25.4;
    private static final double MARGIN_PT = 6;
    private static final Charset CP1252 = Charset.forName("windows-1252");

    /** Un seul thread d'impression : les tickets sortent dans l'ordre et l'interface ne se fige jamais. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ticket-printer");
        t.setDaemon(true);
        return t;
    });

    // ---------------------------------------------------------- imprimantes

    /** Noms des imprimantes installées sur la machine. */
    public static List<String> availablePrinters() {
        List<String> names = new ArrayList<>();
        try {
            for (javax.print.PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
                names.add(ps.getName());
            }
        } catch (Throwable t) {
            // environnement sans système d'impression : liste vide
        }
        return names;
    }

    private static javax.print.PrintService findPrinter(String name) {
        if (name != null && !name.isBlank()) {
            for (javax.print.PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (ps.getName().equalsIgnoreCase(name.trim())) {
                    return ps;
                }
            }
            throw new BusinessException("Imprimante introuvable : « " + name + " ». Vérifiez les paramètres.");
        }
        javax.print.PrintService def = PrintServiceLookup.lookupDefaultPrintService();
        if (def == null) {
            throw new BusinessException("Aucune imprimante installée. Utilisez l'aperçu / l'export PDF.");
        }
        return def;
    }

    // ------------------------------------------------------------ impression

    public CompletableFuture<Void> printAsync(List<TicketLine> lines, int widthMm, int cols, String printerName) {
        return CompletableFuture.runAsync(() -> print(lines, widthMm, cols, printerName), EXECUTOR);
    }

    public void print(List<TicketLine> lines, int widthMm, int cols, String printerName) {
        try {
            javax.print.PrintService target = findPrinter(printerName);
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(target);
            job.setJobName("Ticket");

            double width = widthMm * MM_TO_PT;
            double height = estimateHeight(lines, width, cols);
            Paper paper = new Paper();
            paper.setSize(width, height);
            paper.setImageableArea(MARGIN_PT, MARGIN_PT, width - 2 * MARGIN_PT, height - 2 * MARGIN_PT);
            PageFormat pf = job.defaultPage();
            pf.setPaper(paper);
            pf.setOrientation(PageFormat.PORTRAIT);

            job.setPrintable(new TicketPrintable(lines, cols), pf);
            job.print();
        } catch (PrinterException e) {
            throw new BusinessException("Échec de l'impression : " + e.getMessage());
        } catch (java.awt.HeadlessException e) {
            throw new BusinessException("Impression indisponible dans cet environnement.");
        }
    }

    private static double lineHeight(double fontSize) {
        return fontSize * 1.22;
    }

    private static double fitFontSize(double printableWidth, int cols) {
        // Police Monospaced : largeur d'un caractère = 0,6 x la taille
        double size = printableWidth / (cols * 0.6);
        return Math.max(6, Math.min(size, 12));
    }

    private static double estimateHeight(List<TicketLine> lines, double pageWidth, int cols) {
        double fs = fitFontSize(pageWidth - 2 * MARGIN_PT, cols);
        double h = 2 * MARGIN_PT + 30;                        // marge de coupe
        for (TicketLine l : lines) {
            h += lineHeight(fs * l.scale());
        }
        return Math.max(h, 100);
    }

    /** Dessine le ticket ligne par ligne. */
    private static final class TicketPrintable implements Printable {
        private final List<TicketLine> lines;
        private final int cols;

        TicketPrintable(List<TicketLine> lines, int cols) {
            this.lines = lines;
            this.cols = cols;
        }

        @Override
        public int print(Graphics graphics, PageFormat pf, int pageIndex) {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g = (Graphics2D) graphics;
            g.translate(pf.getImageableX(), pf.getImageableY());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // On mesure la vraie largeur d'un caractère pour que `cols` caractères tiennent exactement.
            Font probe = new Font(Font.MONOSPACED, Font.PLAIN, 100);
            FontMetrics fm = g.getFontMetrics(probe);
            double charW100 = fm.charWidth('0');
            double size = pf.getImageableWidth() / (cols * (charW100 / 100.0));
            size = Math.max(6, Math.min(size, 12));

            double y = 0;
            for (TicketLine l : lines) {
                Font font = new Font(Font.MONOSPACED, l.bold() ? Font.BOLD : Font.PLAIN, 10)
                        .deriveFont((float) (size * l.scale()));
                g.setFont(font);
                y += lineHeight(size * l.scale());
                g.drawString(displayable(l.text(), font), 0f, (float) y);
            }
            return PAGE_EXISTS;
        }

        /** Retire les caractères que la police ne sait pas dessiner (ex. l'émoji ☕ sur une imprimante thermique). */
        private static String displayable(String text, Font font) {
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (font.canDisplay(ch)) {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------ PDF

    /**
     * Écrit un PDF d'une seule page à la largeur du papier thermique (police Courier standard, aucune dépendance).
     */
    public Path exportPdf(List<TicketLine> lines, int widthMm, int cols, Path target) throws IOException {
        double pageW = widthMm * MM_TO_PT;
        double margin = 8;
        double fs = (pageW - 2 * margin) / (cols * 0.6);      // Courier : 0,6 em par caractère
        double pageH = 2 * margin + 20;
        for (TicketLine l : lines) {
            pageH += lineHeight(fs * l.scale());
        }

        StringBuilder content = new StringBuilder();
        double y = pageH - margin;
        for (TicketLine l : lines) {
            double size = fs * l.scale();
            y -= lineHeight(size);
            content.append("BT /").append(l.bold() ? "F2" : "F1").append(' ').append(fmt(size)).append(" Tf ")
                    .append(fmt(margin)).append(' ').append(fmt(y)).append(" Td (")
                    .append(escape(encodable(l.text()))).append(") Tj ET\n");
        }
        byte[] stream = content.toString().getBytes(CP1252);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");

        obj(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offsets, 2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        obj(out, offsets, 3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + fmt(pageW) + " " + fmt(pageH) + "] "
                + "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>");

        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
        out.write(stream);
        write(out, "\nendstream\nendobj\n");

        obj(out, offsets, 5, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>");
        obj(out, offsets, 6, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>");

        int xref = out.size();
        write(out, "xref\n0 7\n0000000000 65535 f \n");
        for (int off : offsets) {
            write(out, String.format("%010d 00000 n \n", off));
        }
        write(out, "trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target, out.toByteArray());
        return target;
    }

    private static void obj(ByteArrayOutputStream out, List<Integer> offsets, int n, String body) {
        offsets.add(out.size());
        write(out, n + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(CP1252);
        out.write(b, 0, b.length);
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    /** Garde uniquement les caractères représentables en Windows-1252 (accents français inclus). */
    private static String encodable(String s) {
        CharsetEncoder enc = CP1252.newEncoder();
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (enc.canEncode(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
