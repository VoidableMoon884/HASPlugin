package de.vmoon.hasplugin.manager;

import de.vmoon.hasplugin.HASPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LanguageManager {
    private final HASPlugin plugin;
    private File languageFile;
    private FileConfiguration languageConfig;

    public LanguageManager(HASPlugin plugin) {
        this.plugin = plugin;
        saveDefaultLanguageFile();
        reloadLanguage();
    }

    private void saveDefaultLanguageFile() {
        languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false);  // <-- Jetzt funktioniert `saveResource`
        }
    }

    public void reloadLanguage() {
        if (languageFile == null) {
            languageFile = new File(plugin.getDataFolder(), "language.yml");
        }
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    public String getMessage(String key) {
        String language = plugin.getConfig().getString("language", "de"); // Standard: Deutsch
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
}
