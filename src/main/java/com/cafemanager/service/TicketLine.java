package com.cafemanager.service;

/**
 * Une ligne de ticket déjà mise en forme (le texte est complété par des espaces pour l'alignement, en police à
 * chasse fixe). Le même contenu sert à l'aperçu, à l'imprimante thermique et à l'export PDF.
 *
 * @param text  texte de la ligne
 * @param bold  gras
 * @param scale facteur de taille (1.0 = normal, 1.4 = titre)
 */
public record TicketLine(String text, boolean bold, double scale) {

    public static TicketLine plain(String text) {
        return new TicketLine(text, false, 1.0);
    }

    public static TicketLine bold(String text) {
        return new TicketLine(text, true, 1.0);
    }

    public static TicketLine blank() {
        return new TicketLine("", false, 1.0);
    }
}
