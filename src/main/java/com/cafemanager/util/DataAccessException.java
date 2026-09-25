package com.cafemanager.util;

/** Erreur d'accès à la base de données (enveloppe les SQLException pour alléger les signatures). */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
