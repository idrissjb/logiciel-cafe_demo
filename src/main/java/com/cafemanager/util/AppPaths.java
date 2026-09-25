package com.cafemanager.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Emplacements des fichiers de l'application (base SQLite, images importées, exports PDF).
 * Par défaut : {@code ~/.cafemanager}. Peut être changé avec {@code -Dcafe.home=/mon/dossier}.
 */
public final class AppPaths {

    private static final Path HOME = Paths.get(
            System.getProperty("cafe.home", System.getProperty("user.home") + java.io.File.separator + ".cafemanager"));

    private AppPaths() {
    }

    public static Path home() {
        return ensure(HOME);
    }

    public static Path database() {
        return home().resolve("cafe.db");
    }

    public static Path images() {
        return ensure(home().resolve("images"));
    }

    public static Path exports() {
        return ensure(home().resolve("exports"));
    }

    private static Path ensure(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier " + dir, e);
        }
    }
}
