package de.vmoon.hasplugin;

import de.vmoon.hasplugin.commands.HasCommand;
import de.vmoon.hasplugin.commands.helpCommand;
import de.vmoon.hasplugin.commands.pvpCommand;
import de.vmoon.hasplugin.manager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HASPlugin extends JavaPlugin {
    private static HASPlugin plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        int pluginId = 24385;
        Metrics metrics = new Metrics(this, pluginId);
        LanguageManager languageManager = new LanguageManager(this);
        Bukkit.getWorld("world").setPVP(false);
        Bukkit.getConsoleSender().sendMessage("§6HASPlugin erfolgreich geladen!");
        getCommand("has").setExecutor(new HasCommand(languageManager));
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
}
