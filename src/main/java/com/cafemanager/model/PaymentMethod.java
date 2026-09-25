package com.cafemanager.model;

public enum PaymentMethod {
    CASH("Espèces", "💵"),
    CARD("Carte bancaire", "💳"),
    OTHER("Autre", "📱");

    private final String label;
    private final String icon;

    PaymentMethod(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return label;
    }
}
