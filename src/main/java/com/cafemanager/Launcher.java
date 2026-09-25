package com.cafemanager;

/**
 * Point d'entrée "classique" (sans héritage de Application) : indispensable pour lancer une application JavaFX
 * depuis un JAR ou certains IDE sans erreur "Les composants JavaFX runtime sont manquants".
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
