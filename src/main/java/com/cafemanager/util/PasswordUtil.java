package com.cafemanager.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hachage des mots de passe (PBKDF2-HMAC-SHA256 + sel aléatoire). Aucun mot de passe n'est stocké en clair. */
public final class PasswordUtil {

    private static final int ITERATIONS = 40_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String saltBase64) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltBase64),
                    ITERATIONS, KEY_BITS);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Hachage impossible", e);
        }
    }

    public static boolean verify(String password, String saltBase64, String expectedHash) {
        byte[] a = hash(password, saltBase64).getBytes();
        byte[] b = expectedHash.getBytes();
        return MessageDigest.isEqual(a, b);
    }
}
