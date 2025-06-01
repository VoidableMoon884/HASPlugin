package de.vmoon.hasplugin.manager;

import de.vmoon.hasplugin.HASPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LanguageManager {
    private final HASPlugin plugin;
    private final ConfigManager configManager;
    private File languageFile;
    private FileConfiguration languageConfig;

    public LanguageManager(HASPlugin plugin) {
        this.plugin = plugin;
        // Hole den zentralen ConfigManager aus der Hauptklasse
        this.configManager = plugin.getConfigManager();
        saveDefaultLanguageFile();
        reloadLanguage();
    }

    private void saveDefaultLanguageFile() {
        languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            // Kopiert language.yml aus dem Plugin-JAR, falls sie nicht vorhanden ist
            plugin.saveResource("language.yml", false);
        }
    }

    /**
     * Diese Methode lädt zuerst die aktuelle config.yml neu ein,
     * damit auch der „language“-Wert (und alle anderen) aktualisiert werden,
     * und lädt anschließend die language.yml.
     */
    public void reloadLanguage() {
        // Wichtig: Zuerst die Hauptkonfiguration neu laden, um externe Änderungen zu erfassen!
        configManager.reloadConfig();

        if (languageFile == null) {
            languageFile = new File(plugin.getDataFolder(), "language.yml");
        }
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    /**
     * Liest den aktuellen Sprachwert aus der (neu geladenen) config.yml über den ConfigManager ein
     * und liefert den entsprechenden Nachrichtentext aus der language.yml.
     */
    public String getMessage(String key) {
        // Nutzt den reingeladenen Wert aus der config.yml
        String language = configManager.getConfig().getString("language", "de");
        return languageConfig.getString("languages." + language + "." + key, "§c[Error] message not found.");
    }

    /**
     * Ändert die Sprache in der config.yml und speichert diese Änderung.
     * Im Anschluss wird die Sprachkonfiguration neu geladen.
     */
    public void setLanguage(String language) {
        FileConfiguration config = configManager.getConfig();
        config.set("language", language);
        configManager.saveConfig();
        reloadLanguage();
    }
}