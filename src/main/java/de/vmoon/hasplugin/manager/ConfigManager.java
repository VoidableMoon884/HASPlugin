package de.vmoon.hasplugin.manager;

import de.vmoon.hasplugin.HASPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final HASPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(HASPlugin plugin) {
        this.plugin = plugin;
        createConfig();    // Erstellt die Datei, falls noch nicht existent
        loadConfig();      // Lädt die config.yml in den Speicher
    }

    // Stellt sicher, dass der Datafolder existiert und extrahiert config.yml, falls nötig
    private void createConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // Kopiert die config.yml aus dem Plugin-JAR in den Datafolder
            plugin.saveResource("config.yml", false);
        }
    }

    // Lädt die Konfiguration aus der Datei
    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // Gibt die geladene Konfiguration zurück
    public FileConfiguration getConfig() {
        return config;
    }

    // Speichert die momentan im Speicher befindliche Konfiguration in die Datei
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lädt die Konfiguration neu (zum Beispiel, wenn sie extern geändert wurde)
    public void reloadConfig() {
        loadConfig();
    }

    // Hilfsmethoden zum einfachen Auslesen von Werten
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    // Beispiel zum direkten Setzen eines Wertes und anschließenden Speichern
    public void set(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }
}