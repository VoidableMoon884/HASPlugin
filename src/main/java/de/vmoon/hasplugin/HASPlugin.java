package de.vmoon.hasplugin;

import de.vmoon.hasplugin.commands.*;
import de.vmoon.hasplugin.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HASPlugin extends JavaPlugin {
    private static HASPlugin plugin;
    private ConfigManager configManager;
    private HasCommand hasCommandInstance;

    @Override
    public void onEnable() {
        // BStats config
        plugin = this;
        int pluginId = 24385;
        Metrics metrics = new Metrics(this, pluginId);

        configManager = new ConfigManager(this);
        LanguageManager languageManager = new LanguageManager(this);
        hasCommandInstance = new HasCommand(languageManager);


        Bukkit.getWorld("world").setPVP(false);

        //Commands
        getCommand("has").setExecutor(new HasCommand(languageManager));
        getCommand("pvp").setExecutor(new pvpCommand());
        getCommand("hashelp").setExecutor(new helpCommand());

        Bukkit.getConsoleSender().sendMessage("§6HASPlugin successfully loaded!");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§6HASPlugin successfully deactivated!");
        // Plugin shutdown logic
    }
    public static HASPlugin getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    public HasCommand getHasCommandInstance() {
        return hasCommandInstance;
    }

}
