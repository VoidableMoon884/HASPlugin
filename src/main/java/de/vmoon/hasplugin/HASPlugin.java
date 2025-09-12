package de.vmoon.hasplugin;

import de.vmoon.hasplugin.commands.*;
import de.vmoon.hasplugin.manager.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HASPlugin extends JavaPlugin implements Listener {

    private static HASPlugin plugin;
    private StatisticsManager statisticsManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        languageManager = new LanguageManager(this);
        statisticsManager = new StatisticsManager(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getWorld("world").setPVP(false);
        getLogger().info("HASPlugin erfolgreich geladen!");

        getCommand("has").setExecutor(new HasCommand(languageManager, statisticsManager));
        // ... andere Kommandos

    }

    @Override
    public void onDisable() {
        getLogger().info("HASPlugin erfolgreich deaktiviert!");
    }

    public static HASPlugin getPlugin() {
        return plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.isOp()) return;

        // Asynchron die Version prüfen
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://vmoon.de/plugin_versions?plugin=hasplugin");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(5000);
                    con.setReadTimeout(5000);
                    con.setRequestMethod("GET");

                    try (InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                        String latestVersion = json.get("version").getAsString();
                        String downloadLink = json.get("download_link").getAsString();

                        String currentVersion = getDescription().getVersion();

                        if (!currentVersion.equals(latestVersion)) {
                            String msg = languageManager.getMessage("update_available")
                                    .replace("%version%", latestVersion)
                                    .replace("%link%", downloadLink);
                            // Nachricht im Hauptthread senden
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendMessage(msg);
                                }
                            }.runTask(HASPlugin.getPlugin());
                        }
                    }
                    con.disconnect();
                } catch (Exception e) {
                    getLogger().warning("Fehler bei Update-Abfrage beim Join von " + player.getName() + ": " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(this);
    }
}
