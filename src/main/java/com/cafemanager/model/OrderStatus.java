package com.cafemanager.model;

public enum OrderStatus {
    /** Commande en cours de saisie / de service. */
    OPEN,
    /** Addition demandée : en attente de paiement. */
    PENDING_PAYMENT,
    PAID,
    CANCELLED
}
