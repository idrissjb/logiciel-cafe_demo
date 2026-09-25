package com.cafemanager.util;

/** Erreur "métier" dont le message est destiné à être affiché tel quel à l'utilisateur. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
