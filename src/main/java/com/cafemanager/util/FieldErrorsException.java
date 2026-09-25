package com.cafemanager.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Erreurs de saisie rattachées chacune à un champ de formulaire (clé du champ → message). */
public class FieldErrorsException extends BusinessException {

    private final Map<String, String> errors;

    public FieldErrorsException(Map<String, String> errors) {
        super(errors.values().iterator().next());
        this.errors = new LinkedHashMap<>(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
