package de.vmoon.hasplugin.manager;

import de.vmoon.hasplugin.HASPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LanguageManager {
    private final HASPlugin plugin;
    private File languageFile;
    private FileConfiguration languageConfig;
    private Set<String> availableLanguages = new HashSet<>(); // Speichert die verfügbaren Sprachen

    public LanguageManager(HASPlugin plugin) {
        this.plugin = plugin;
        saveDefaultLanguageFile();
        reloadLanguage();
    }

    private void saveDefaultLanguageFile() {
        languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false);
        }
    }

    public void reloadLanguage() {
        if (languageFile == null) {
            languageFile = new File(plugin.getDataFolder(), "language.yml");
        }
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // VERFÜGBARE SPRACHEN AKTUALISIEREN
        availableLanguages.clear();
        if (languageConfig.isConfigurationSection("languages")) {
            availableLanguages.addAll(languageConfig.getConfigurationSection("languages").getKeys(false));
        }
    }

    public String getMessage(String key) {
        String language = plugin.getConfig().getString("language", "de");
        return languageConfig.getString("languages." + language + "." + key, "§c[Error] message not found.");
    }

    public void setLanguage(String language) {
        FileConfiguration config = plugin.getConfig();
        config.set("language", language);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        reloadLanguage();
    }

    // TAB-COMPLETE Funktionen:
    public Set<String> getAvailableLanguagesForTabComplete() {
        return Collections.unmodifiableSet(availableLanguages);
    }
}
