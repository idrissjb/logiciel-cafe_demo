package com.cafemanager.model;

/** État d'une table du café. */
public enum TableStatus {
    LIBRE("Libre", "free"),
    OCCUPEE("Occupée", "busy"),
    EN_ATTENTE("Commande en attente", "waiting"),
    PAYEE("Payée", "paid");

    private final String label;
    private final String cssClass;

    TableStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    /** Suffixe de classe CSS : table-free, table-busy, ... */
    public String getCssClass() {
        return cssClass;
    }
}
