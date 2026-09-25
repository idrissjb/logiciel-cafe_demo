package com.cafemanager.model;

/** Rôles utilisateur et droits associés. */
public enum Role {
    ADMINISTRATEUR("Administrateur"),
    CAISSIER("Caissier"),
    SERVEUR("Serveur");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Catalogue, catégories, utilisateurs, paramètres, dashboard : réservé à l'administrateur. */
    public boolean isAdmin() {
        return this == ADMINISTRATEUR;
    }

    public boolean canViewDashboard() {
        return this == ADMINISTRATEUR;
    }

    /** Historique / réimpression des tickets : administrateur et caissier. */
    public boolean canViewHistory() {
        return this != SERVEUR;
    }

    /** L'encaissement est réservé à l'admin et au caissier (sauf réglage "les serveurs peuvent encaisser"). */
    public boolean canPay(boolean serverCanPay) {
        return this != SERVEUR || serverCanPay;
    }

    @Override
    public String toString() {
        return label;
    }
}
