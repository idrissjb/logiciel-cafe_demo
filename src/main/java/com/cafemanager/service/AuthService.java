package com.cafemanager.service;

import com.cafemanager.dao.UserDAO;
import com.cafemanager.model.Role;
import com.cafemanager.model.User;
import com.cafemanager.util.BusinessException;
import com.cafemanager.util.FieldErrorsException;
import com.cafemanager.util.PasswordUtil;
import com.cafemanager.util.SessionManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Authentification et gestion de la session. */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException("Saisissez votre nom d'utilisateur et votre mot de passe.");
        }
        Optional<User> found = userDAO.findByUsername(username.trim());
        if (found.isEmpty() || !PasswordUtil.verify(password, found.get().getSalt(), found.get().getPasswordHash())) {
            throw new BusinessException("Nom d'utilisateur ou mot de passe incorrect.");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new BusinessException("Ce compte est désactivé. Contactez un administrateur.");
        }
        SessionManager.get().login(user);
        return user;
    }

    /** Longueur minimale d'un mot de passe choisi à l'inscription. */
    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,20}");

    /**
     * Vérifie tous les champs de l'inscription et renvoie les erreurs par champ
     * (clés : fullName, username, password, confirm, role). Vide = tout est valide.
     */
    public Map<String, String> validateRegistration(String fullName, String username, String password,
                                                    String confirm, Role role) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (fullName == null || fullName.trim().length() < 2) {
            errors.put("fullName", "Saisissez votre nom complet.");
        }

        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            errors.put("username", "Choisissez un nom d'utilisateur.");
        } else if (!USERNAME.matcher(name).matches()) {
            errors.put("username", "3 à 20 caractères : lettres, chiffres, . _ -");
        } else if (userDAO.findByUsername(name).isPresent()) {
            errors.put("username", "Ce nom d'utilisateur est déjà pris.");
        }

        if (password == null || password.isEmpty()) {
            errors.put("password", "Choisissez un mot de passe.");
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.put("password", MIN_PASSWORD_LENGTH + " caractères minimum.");
        } else if (confirm == null || confirm.isEmpty()) {
            errors.put("confirm", "Confirmez le mot de passe.");
        } else if (!password.equals(confirm)) {
            errors.put("confirm", "Les mots de passe diffèrent.");
        }

        if (role == null || role.isAdmin()) {
            errors.put("role", "Choisissez caissier ou serveur.");
        }
        return errors;
    }

    /**
     * Inscription depuis la page de connexion : crée un compte actif et ouvre la session.
     * Le rôle administrateur ne peut pas être choisi ici (il se donne depuis l'écran Utilisateurs).
     *
     * @throws FieldErrorsException si un ou plusieurs champs sont invalides
     */
    public User register(String fullName, String username, String password, String confirm, Role role) {
        Map<String, String> errors = validateRegistration(fullName, username, password, confirm, role);
        if (!errors.isEmpty()) {
            throw new FieldErrorsException(errors);
        }
        User u = new User();
        u.setFullName(fullName);
        u.setUsername(username);
        u.setRole(role);
        u.setActive(true);
        User created;
        try {
            created = new UserService().save(u, password);
        } catch (BusinessException e) {
            // Nom pris entre la vérification et l'insertion (deux postes en même temps)
            if (String.valueOf(e.getMessage()).contains("existe déjà")) {
                throw new FieldErrorsException(Map.of("username", "Ce nom d'utilisateur est déjà pris."));
            }
            throw e;
        }
        SessionManager.get().login(created);
        return created;
    }

    public void logout() {
        SessionManager.get().logout();
    }
}
