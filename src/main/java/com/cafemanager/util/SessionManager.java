package com.cafemanager.util;

import com.cafemanager.model.Order;
import com.cafemanager.model.User;

/**
 * État de la session en cours : utilisateur connecté et commande en cours de saisie.
 * La commande en cours vit ici (et non dans le contrôleur) pour survivre aux changements d'écran.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;
    private Order draft;

    private SessionManager() {
    }

    public static SessionManager get() {
        return INSTANCE;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void login(User user) {
        this.currentUser = user;
        this.draft = null;
    }

    public void logout() {
        this.currentUser = null;
        this.draft = null;
    }

    public Order getDraft() {
        return draft;
    }

    public void setDraft(Order draft) {
        this.draft = draft;
    }
}
