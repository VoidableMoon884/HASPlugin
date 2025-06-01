package de.vmoon.hasplugin.manager;

import de.vmoon.hasplugin.HASPlugin;
import de.vmoon.hasplugin.commands.HasCommand;
import de.vmoon.hasplugin.commands.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class EndGameTimer {

    private final HASPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    // Referenz auf die HasCommand-Instanz, um den globalen Timer stoppen zu können
    private final HasCommand hasCommand;
    private BukkitTask timerTask;
    private int endGameTimeMinutes;

    /**
     * Konstruktor – übernimmt Plugin-Instanz, ConfigManager, LanguageManager und eine Referenz auf HasCommand.
     * So greifen wir zentral auf die Konfiguration, Sprachdatei und den globalen Timer zu.
     */
    public EndGameTimer(HASPlugin plugin, ConfigManager configManager, LanguageManager languageManager, HasCommand hasCommand) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageManager = languageManager;
        this.hasCommand = hasCommand;
        loadEndGameTime();
    }

    /**
     * Liest den Timerwert (in Minuten) aus der config.yml unter dem Schlüssel "endgameTime".
     * Wird kein Wert gefunden, wird standardmäßig 10 Minuten angenommen.
     */
    private void loadEndGameTime() {
        endGameTimeMinutes = configManager.getConfig().getInt("endgameTime", 10);
    }

    /**
     * Startet den EndGame-Timer:
     * - Die eingestellte Zeit (in Minuten) wird in Ticks umgerechnet.
     * - Nach Ablauf werden an alle Spieler via sendTitle die Nachricht angezeigt, dass
     *   die hiders gewonnen haben.
     * - Anschließend wird das Spiel beendet:
     *   o PvP wird deaktiviert,
     *   o Effekte werden von allen Spielern entfernt,
     *   o Der globale Timer (Hotbar-Timer) wird gestoppt (über hasCommand.stopGlobalTimer()),
     *   o Nach 5 Sekunden werden alle Spieler per TeleportManager an die gespeicherten Koordinaten teleportiert,
     *     Inventare geleert, Spielmodus auf ADVENTURE gesetzt und der Hotbar-Timer (Level) auf 0 gesetzt.
     */
    public void startTimer() {
        cancelTimer(); // Vorherigen Timer abbrechen, falls noch aktiv
        loadEndGameTime(); // Den aktuellen Wert neu einlesen

        int delayTicks = endGameTimeMinutes * 60 * 20; // Umrechnung: Minuten -> Ticks
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Zeige allen Spielern einen Title, dass die hiders gewonnen haben.
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(languageManager.getMessage("hiders_win"), "", 10, 70, 20);
                }

                // Deaktiviere PvP in der Welt "world", falls vorhanden.
                World world = Bukkit.getWorld("world");
                if (world != null) {
                    world.setPVP(false);
                }

                // Entferne alle relevanten Effekte von allen Spielern.
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.removePotionEffect(PotionEffectType.SLOW);
                    player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                    player.removePotionEffect(PotionEffectType.SATURATION);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                }

                // Stoppe den globalen Timer – damit der Hotbar-Timer (Spieler-Level etc.) gelöscht wird.
                hasCommand.stopGlobalTimer();

                // Nach 5 Sekunden Verzögerung: Teleportiere die Spieler, setze Spielmodus und leere Inventare.
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Teleportiere alle Spieler zu den in der config.yml gespeicherten Koordinaten.
                    TeleportManager teleportManager = new TeleportManager();
                    teleportManager.teleportAllPlayers();

                    // Setze Spielmodus, leere Inventare und lösche den Hotbar-Timer (setze Level auf 0).
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.setGameMode(GameMode.ADVENTURE);
                        player.getInventory().clear();
                        player.setLevel(0);
                    }
                }, 5 * 20L); // 5 Sekunden Verzögerung
            }
        }.runTaskLater(plugin, delayTicks);
    }

    /**
     * Bricht einen aktiven Timer ab.
     */
    public void cancelTimer() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
        }
    }

    /**
     * Gibt zurück, ob aktuell ein Timer läuft.
     */
    public boolean isRunning() {
        return timerTask != null && !timerTask.isCancelled();
    }
}