package de.vmoon.hasplugin.commands;

import de.vmoon.hasplugin.HASPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class HasCommand implements CommandExecutor, TabCompleter, Listener {
    private int defaultTime = 90;
    private int time = defaultTime;
    private BukkitRunnable runnable;
    private boolean timerRunning = false;
    private Player selectedPlayer = null;
    private TeleportManager teleportManager;
    private Team noNameTagTeam;

    public HasCommand() {
        this.teleportManager = new TeleportManager();
        Bukkit.getPluginManager().registerEvents(this, HASPlugin.getPlugin());
        setupNoNameTagTeam();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("has")) {
            if (!sender.hasPermission("has.run")) {
                sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                return true;
            }
            if (moreThanOnePlayerOnline()) {
                if (args.length == 0) {
                    time = defaultTime;
                    if (selectedPlayer == null || !selectedPlayer.isOnline()) {
                        selectedPlayer = selectRandomPlayer();
                    }
                    startgame();


                    return true;
                }
                else {
                    if (args[0].equalsIgnoreCase("stop")) {
                        if (!sender.hasPermission("has.stop")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        if (timerRunning) {
                            stopTimer();
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.getInventory().clear();
                            }
                            Bukkit.broadcastMessage("Der Timer wurde gestoppt.");
                        }
                        else {
                            sender.sendMessage("Es läuft kein Timer.");
                        }
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("select")) {
                        if (!sender.hasPermission("has.select")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        if (args.length == 2) {
                            if (!sender.hasPermission("has.select.random")) {
                                sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                                return true;
                            }
                            if (args[1].equalsIgnoreCase("random")) {
                                selectedPlayer = selectRandomPlayer();
                                sender.sendMessage("Ein neuer zufälliger Spieler wurde ausgewählt!");
                            } else {
                                Player newSelectedPlayer = Bukkit.getPlayer(args[1]);
                                if (newSelectedPlayer != null && newSelectedPlayer.isOnline()) {
                                    selectedPlayer = newSelectedPlayer;
                                    sender.sendMessage(selectedPlayer.getName() + " wurde als der gesuchte Spieler ausgewählt!");
                                } else {
                                    sender.sendMessage("Der angegebene Spieler ist nicht online.");
                                }
                            }
                        }
                        else {
                            sender.sendMessage("Verwendung: /has select <Spieler | random>");
                        }
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("teleportall")) {
                        if (!sender.hasPermission("has.teleportall")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        teleportAllPlayers();
                        sender.sendMessage("Alle Spieler wurden zu den gespeicherten Koordinaten teleportiert.");
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("cancel")) {
                        if (!sender.hasPermission("has.cancel")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        cancelgame();
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("reload")) {
                        if (!sender.hasPermission("has.reload")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        sender.sendMessage("§6Config erfolgreich neu geladen!");
                        reload();
                    }
                    else if (args[0].equalsIgnoreCase("help")) {
                        if (!sender.hasPermission("has.help")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        sender.sendMessage("Bitte benutze /hashelp!");
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("skip")) {
                        if (!sender.hasPermission("has.skip")) {
                            sender.sendMessage("§cDu hast keine Berechtigung um diesen Befehl auszuführen!");
                            return true;
                        }
                        time = 5;
                        sender.sendMessage("§a§lDer Timer wurde auf 5 Sekunden gesetzt!");
                        return true;
                    }
                    else {
                        try {
                            time = Integer.parseInt(args[0]);
                            Bukkit.broadcastMessage("Die Zeit wurde auf " + time + " Sekunden gesetzt.");
                            if (selectedPlayer == null || !selectedPlayer.isOnline()) {
                                selectedPlayer = selectRandomPlayer();
                            }
                            startgame();
                            enablepvp();
                        } catch (NumberFormatException e) {
                            sender.sendMessage("Bitte gib eine gültige Zahl ein oder verwende 'stop' zum Stoppen des Timers.");
                        }
                    }
                }
            }
            else {
                Bukkit.broadcastMessage("§cEs ist nur ein Spieler online. Es müssen mindestens 2 Spieler auf dem Server sein!");
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("stop");
            completions.add("select");
            completions.add("cancel");
            completions.add("reload");
            completions.add("teleportall");
            completions.add("help");
            completions.add("skip");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            List<String> completions = new ArrayList<>();
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            completions.add("random");
            return completions.stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.setGameMode(GameMode.SPECTATOR);
        checkIfSelectedPlayerKilledEveryone();
        if (timerRunning)
            if (allPlayersDead()) {
                gameEnd();
                stopTimer();
            }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (selectedPlayer != null && event.getPlayer().equals(selectedPlayer)) {
            if (timerRunning) {
                // Wenn der Timer läuft, blockiere die Bewegung des Spielers
                event.setCancelled(true);
            }
            else {
                event.setCancelled(false);
            }
        }
    }

    private void startTimer() {
        if (timerRunning) {
            return;
        }

        if (selectedPlayer != null) {
            selectedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, time * 20, 1));
        }

        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                switch (time) {
                    case 90:
                    case 80:
                    case 70:
                    case 60:
                    case 50:
                    case 40:
                    case 30:
                    case 20:
                    case 10:
                    case 5:
                    case 4:
                    case 3:
                    case 2:
                    case 1:
                        Bukkit.broadcastMessage("§aNoch " + time + (time == 1 ? " Sekunde" : " Sekunden") + " übrig!");
                        break;
                    case 0:
                        break;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setLevel(time);
                }
                if (time == 0) {
                    Bukkit.broadcastMessage("§aDie Zeit ist abgelaufen. Der Sucher sucht jetzt");
                    time = defaultTime;
                    giveDiamondSword(selectedPlayer);
                    removeBlindnessEffect();
                    stopTimer();
                    return;
                }
                time--;
                checkIfSelectedPlayerKilledEveryone();
                if (allPlayersDead()) {
                    gameEnd();
                    stopTimer();
                }
            }
        };
        runnable.runTaskTimer(HASPlugin.getPlugin(), 0, 20);
        timerRunning = true;
    }

    private boolean allPlayersDead() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead()) {
                return false;
            }
        }
        return true;
    }

    private void gameEnd() {
        Bukkit.broadcastMessage("§cAlle Spieler sind tot! Das Spiel endet.");
    }

    private void stopTimer() {
        if (runnable != null) {
            runnable.cancel();
        }
        timerRunning = false;
        time = defaultTime;
        removeBlindnessEffect();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setLevel(0);
        }
    }

    private Player selectRandomPlayer() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (!onlinePlayers.isEmpty()) {
            Random random = new Random();
            return onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        }
        return null;
    }

    private void giveDiamondSword(Player player) {
        if (player != null) {
            ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = diamondSword.getItemMeta();
            meta.setDisplayName("§bDiamantschwert");
            diamondSword.setItemMeta(meta);
            player.getInventory().addItem(diamondSword);
        }
    }

    private void removeBlindnessEffect() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private void teleportAllPlayers() {
        teleportManager.teleportAllPlayers();
    }

    private void startgame() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            teleportManager.teleportAllPlayers();
            player.getInventory().clear();
            enablepvp();
            noNameTagTeam.addEntry(player.getName());
        }
        startTimer();
    }
    private void cancelgame() {
        for (Player player: Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            disablepvp();
        }
        Bukkit.broadcastMessage("§cDas Spiel wurde abgebrochen!");
    }
    private void checkIfSelectedPlayerKilledEveryone() {
        if (selectedPlayer == null || !selectedPlayer.isOnline()) {
            // Der ausgewählte Spieler ist nicht gesetzt oder nicht online
            return;
        }

        long countAlivePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(selectedPlayer) && !player.isDead())
                .count();

        if (countAlivePlayers == 0) {
            Bukkit.broadcastMessage("§cDer Sucher §f(" + selectedPlayer.getName() + ")§c hat alle Spieler getötet!");
            disablepvp();
            Bukkit.getScheduler().runTaskLater(HASPlugin.getPlugin(), () -> {
                teleportAllPlayers();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setGameMode(GameMode.ADVENTURE);
                    player.getInventory().clear();
                }
            }, 20L * 5);
        }
    }
    private void setupNoNameTagTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            noNameTagTeam = board.getTeam("noNameTag");
            if (noNameTagTeam == null) {
                noNameTagTeam = board.registerNewTeam("noNameTag");
            }
            noNameTagTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            for (Player player : Bukkit.getOnlinePlayers()) {
                noNameTagTeam.addEntry(player.getName());
            }
        }
    }

    private void reload() {
        teleportManager.reloadConfig();
    }
    private boolean moreThanOnePlayerOnline() {
        return Bukkit.getOnlinePlayers().size() > 1;
    }
    private void disablepvp() {
        Bukkit.getWorld("world").setPVP(false);
        Bukkit.getConsoleSender().sendMessage("§6[DEBUG] PVP wurde deaktiviert!");
    }
    private void enablepvp() {
        Bukkit.getWorld("world").setPVP(true);
        Bukkit.getConsoleSender().sendMessage("§6[DEBUG] PVP wurde aktiviert!");
    }

}
