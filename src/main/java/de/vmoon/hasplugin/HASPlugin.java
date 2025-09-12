package de.vmoon.hasplugin;

import de.vmoon.hasplugin.commands.*;
import de.vmoon.hasplugin.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HASPlugin extends JavaPlugin {
    private static HASPlugin plugin;
    private StatisticsManager statisticsManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        saveDefaultConfig();
        int pluginId = 24385;
        Metrics metrics = new Metrics(this, pluginId);
        LanguageManager languageManager = new LanguageManager(this);
        statisticsManager = new StatisticsManager(this);
        Bukkit.getWorld("world").setPVP(false);
        Bukkit.getConsoleSender().sendMessage("§6HASPlugin erfolgreich geladen!");
        getCommand("has").setExecutor(new HasCommand(languageManager, statisticsManager));
        getCommand("pvp").setExecutor(new pvpCommand());
        getCommand("hashelp").setExecutor(new helpCommand());

    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§6HASPlugin erfolgreich deaktiviert!");
        // Plugin shutdown logic
    }
    public static HASPlugin getPlugin() {
        return plugin;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
}
