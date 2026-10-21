package fr.clax.aurel.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class SettingsStore {
    private final Path path;
    public SettingsStore(Path path) { this.path = path.toAbsolutePath(); }

    public MenuSettings load() throws IOException {
        MenuSettings settings = new MenuSettings();
        if (!Files.exists(path)) return settings;
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            try { p.load(reader); }
            catch (IllegalArgumentException e) { throw new IOException("Configuration illisible : " + path, e); }
        }
        for (MenuSettings.Option key : MenuSettings.Option.values()) {
            String value = p.getProperty(key.name());
            if (value == null) continue;
            try { settings.set(key, Double.parseDouble(value)); }
            catch (NumberFormatException ignored) { /* Keep the default for this entry. */ }
        }
        for (MenuSettings.Flag key : MenuSettings.Flag.values()) {
            String value = p.getProperty(key.name());
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                settings.set(key, Boolean.parseBoolean(value));
            }
        }
        return settings;
    }

    public void save(MenuSettings settings) throws IOException {
        Properties p = new Properties();
        for (MenuSettings.Option key : MenuSettings.Option.values()) p.setProperty(key.name(), "" + settings.get(key));
        for (MenuSettings.Flag key : MenuSettings.Flag.values()) p.setProperty(key.name(), "" + settings.get(key));
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "aurel-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                p.store(writer, "Aurel - preferences visuelles locales");
            }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
