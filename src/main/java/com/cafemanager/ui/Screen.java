package com.cafemanager.ui;

/** Cycle de vie d'un écran de l'application (affiché / masqué). */
public interface Screen {

    /** Appelé chaque fois que l'écran est affiché : rechargez ici les données. */
    default void onShow() {
    }

    default void onHide() {
    }
}
