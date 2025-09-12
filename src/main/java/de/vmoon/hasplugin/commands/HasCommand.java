package de.vmoon.hasplugin.commands;

import de.vmoon.hasplugin.HASPlugin;
import de.vmoon.hasplugin.manager.LanguageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class HasCommand implements CommandExecutor, TabCompleter, Listener {
    private int defaultTime = 90;
    private boolean gamerunning;
    private int time = defaultTime;
    private BukkitRunnable runnable;
    private boolean timerRunning = false;
    private Player selectedPlayer = null;
    private TeleportManager teleportManager;
    private Team noNameTagTeam;
    private Map<Player, Integer> timers = new HashMap<>();
    private int globalTimer = 0;
    private BukkitTask globalTask = null;
    private BukkitTask countdownTask = null;
    private long countAlivePlayers = 0;
    // Globale Map, um die Spieler zu tracken, die gevotet haben
    private final Set<Player> playersVoted = new HashSet<>();
    private LanguageManager languageManager;

    public HasCommand(LanguageManager languageManager) {
        this.teleportManager = new TeleportManager();
        this.languageManager = languageManager;
        Bukkit.getPluginManager().registerEvents(this, HASPlugin.getPlugin());
        setupNoNameTagTeam();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        selectRandomPlayer();
        if (cmd.getName().equalsIgnoreCase("has")) {
            if (!sender.hasPermission("has.run")) {
                sender.sendMessage(languageManager.getMessage("no_permission"));
                return true;
            }
            else if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("has.reload")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    sender.sendMessage(languageManager.getMessage("config_reload"));
                    reload();
                }
                else if (args[0].equalsIgnoreCase("help")) {
                    if (!sender.hasPermission("has.help")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    sender.sendMessage("Bitte benutze /hashelp!");
                    return true;
                }
                else if (args[0].equalsIgnoreCase("beep")) {
                    if (!sender.hasPermission("has.beep")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    Player player = (Player) sender;
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        // Spieler ist im Spectator-Modus, nichts machen
                        sender.sendMessage(languageManager.getMessage("spectator_error"));
                        return true; // Beendet den Befehl
                    }
                    sender.sendMessage(languageManager.getMessage("sound_played"));
                    playbeep((Player) sender);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("teleportall")) {
                    if (!sender.hasPermission("has.teleportall")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    teleportAllPlayers();
                    sender.sendMessage(languageManager.getMessage("all_teleport"));
                    return true;
                }
                else if (args[0].equalsIgnoreCase("endgame")) {
                    if (!sender.hasPermission("has.endgame")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    if (!moreThanOnePlayerOnline()) {
                        sender.sendMessage(languageManager.getMessage("not_enough_players"));
                        return true;
                    }
                    endgame();
                    sender.sendMessage(languageManager.getMessage("game_end"));
                    return true;
                }
                else if (args[0].equalsIgnoreCase("version")) {
                    if (!sender.hasPermission("has.version")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    sender.sendMessage("§c[HASPlugin] §rHASPlugin Version 2.8.8");
                    return true;
                }
                else if (args[0].equalsIgnoreCase("language")) {
                    if (!sender.hasPermission("has.language")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(languageManager.getMessage("select_language"));
                        return true;
                    }

                    String newLanguage = args[1].toLowerCase();
                    languageManager.setLanguage(newLanguage);
                    sender.sendMessage(languageManager.getMessage("language_set").replace("%language%", newLanguage));
                    return true;
                }

                else if (args[0].equalsIgnoreCase("vote")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(languageManager.getMessage("only_players"));
                        return true;
                    }

                    Player player = (Player) sender;

                    if (!player.hasPermission("has.vote")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }

                    if (!moreThanOnePlayerOnline()) {
                        sender.sendMessage(languageManager.getMessage("not_enought_players"));
                        return true;
                    }

                    // Überprüfe, ob der ausgewählte Spieler existiert
                    if (selectedPlayer == null || !Bukkit.getOnlinePlayers().contains(selectedPlayer)) {
                        sender.sendMessage(languageManager.getMessage("selected_player_not_online"));
                        return true;
                    }

                    // Spieler hinzufügen, wenn sie gevotet haben
                    if (playersVoted.contains(player)) {
                        sender.sendMessage(languageManager.getMessage("already_hidden"));
                        return true;
                    }

                    playersVoted.add(player);
                    sender.sendMessage(languageManager.getMessage("hide"));

                    // Überprüfen, ob alle Spieler (außer selectedPlayer) gevotet haben
                    Set<Player> requiredPlayers = Bukkit.getOnlinePlayers().stream()
                            .filter(p -> !p.equals(selectedPlayer))
                            .collect(Collectors.toSet());

                    if (playersVoted.containsAll(requiredPlayers)) {
                        // Alle Spieler haben gevotet, führe die Logik aus
                        if (timerRunning) {
                            if (time > 5) {
                                time = 5;
                                Bukkit.broadcastMessage(languageManager.getMessage("timer_5"));
                            } else if (time < 5) {
                                player.sendMessage(languageManager.getMessage("timer_under_5"));
                            }
                        } else {
                            player.sendMessage(languageManager.getMessage("no_timer"));
                        }

                        // Liste zurücksetzen, da die Aktion abgeschlossen ist
                        playersVoted.clear();
                    } else {
                        // Noch nicht alle haben gevotet
                        Bukkit.broadcastMessage(languageManager.getMessage("not_all_hide"));
                    }

                    return true;
                }
                else if (args[0].equalsIgnoreCase("add")) {
                    if (!sender.hasPermission("has.addtime")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }

                    // Überprüfen, ob eine Zeit angegeben wurde
                    if (args.length < 2) {
                        sender.sendMessage(languageManager.getMessage("wrong_time"));
                        return true;
                    }

                    try {
                        // Zeit in Sekunden aus den Argumenten parsen
                        int secondsToAdd = Integer.parseInt(args[1]);

                        // Negative Zeit verhindern
                        if (secondsToAdd <= 0) {
                            sender.sendMessage(languageManager.getMessage("right_number"));
                            return true;
                        }

                        // Prüfen, ob der Timer läuft
                        if (!timerRunning) {
                            sender.sendMessage(languageManager.getMessage("no_game_running"));
                            return true;
                        }

                        // Variable 'time' aktualisieren
                        time += secondsToAdd;
                        Bukkit.broadcastMessage(languageManager.getMessage("time_added_1") + secondsToAdd + languageManager.getMessage("time_added_2") + time + languageManager.getMessage("time_added_3"));

                        // Effekte für den Spieler verlängern
                        if (selectedPlayer != null) {
                            // Dauer der Effekte berechnen
                            int newDuration = (time) * 20; // Minecraft erwartet die Dauer in Ticks (20 Ticks = 1 Sekunde)

                            // Effekte neu anwenden oder verlängern
                            selectedPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
                            selectedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, newDuration, 0, true, false, true));
                        } else {
                            sender.sendMessage(languageManager.getMessage("no_player_effect"));
                        }

                    } catch (NumberFormatException e) {
                        sender.sendMessage(languageManager.getMessage("right_number"));
                    }

                    return true;
                }

                else if (args[0].equalsIgnoreCase("stop")) {
                    if (!sender.hasPermission("has.stop")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    cancelgame();
                    if (timerRunning) {
                        stopTimer();
                    }
                    else {
                        sender.sendMessage(languageManager.getMessage("no_timer"));
                    }
                    return true;
                }
                else if (args[0].equalsIgnoreCase("select")) {
                    if (!sender.hasPermission("has.select")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    if (!moreThanOnePlayerOnline()) {
                        sender.sendMessage(languageManager.getMessage("not_enough_players"));
                        return true;
                    }
                    if (args.length == 2) {
                        if (!sender.hasPermission("has.select.random")) {
                            sender.sendMessage(languageManager.getMessage("no_permission"));
                            return true;
                        }
                        if (args[1].equalsIgnoreCase("random")) {
                            selectedPlayer = selectRandomPlayer();
                            sender.sendMessage(languageManager.getMessage("new_random_player"));
                        }
                        else {
                            Player newSelectedPlayer = Bukkit.getPlayer(args[1]);
                            if (newSelectedPlayer != null && newSelectedPlayer.isOnline()) {
                                selectedPlayer = newSelectedPlayer;
                                sender.sendMessage(selectedPlayer.getName() + languageManager.getMessage("x_selected"));
                            }
                            else {
                                sender.sendMessage(languageManager.getMessage("selected_player_not_online"));
                            }
                        }
                    }
                    else {
                        sender.sendMessage(languageManager.getMessage("select_usage"));
                    }
                    return true;
                }
                else if (args[0].equalsIgnoreCase("skip")) {
                    if (!sender.hasPermission("has.skip")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    if (!moreThanOnePlayerOnline()) {
                        sender.sendMessage(languageManager.getMessage("not_enough_players"));
                        return true;
                    }
                    if (timerRunning) {
                        if (time > 5) {
                            time = 5;
                            Bukkit.broadcastMessage(languageManager.getMessage("timer_5"));
                        }
                        else if (time <5) {
                            sender.sendMessage(languageManager.getMessage("timer_under_5"));
                        }
                    }
                    else {
                        sender.sendMessage(languageManager.getMessage("no_timer"));
                    }
                    return true;
                }

                //Ab hier die nicht gelisteten (debug) Commands:

                else if (args[0].equalsIgnoreCase("debugtime")) {
                    if (!sender.hasPermission("has.debug")) {
                        sender.sendMessage(languageManager.getMessage("no_permission"));
                        return true;
                    }
                    String globalTimerString = "Abgelaufene Zeit: " + globalTimer;
                    sender.sendMessage(globalTimerString);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("autor")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("§4Dieses Plugin wurde programmiert von:", "§aVoidableMoon884", 10, 70, 20);
                    }
                    return true;
                }
                else {
                    if (!moreThanOnePlayerOnline()) {
                        sender.sendMessage(languageManager.getMessage("not_enough_players"));
                        return true;
                    }
                    try {
                        time = Integer.parseInt(args[0]);
                        Bukkit.broadcastMessage(languageManager.getMessage("timer_set_1") + time + languageManager.getMessage("timer_set_2"));
                        if (selectedPlayer == null || !selectedPlayer.isOnline()) {
                            selectedPlayer = selectRandomPlayer();
                        }
                        startgame();
                    }
                    catch (NumberFormatException e) {
                        sender.sendMessage(languageManager.getMessage("right_number"));
                    }
                }
            }
            else {
                if (!moreThanOnePlayerOnline()) {
                    sender.sendMessage(languageManager.getMessage("not_enough_players"));
                    return true;
                }
                time = defaultTime;
                if (selectedPlayer == null || !selectedPlayer.isOnline()) {
                    selectedPlayer = selectRandomPlayer();
                }
                startgame();
                return true;
            }

        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("has.stop")) {
                completions.add("stop");
            }
            if (sender.hasPermission("has.select")) {
                completions.add("select");
            }
            if (sender.hasPermission("has.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("has.teleportall")) {
                completions.add("teleportall");
            }
            if (sender.hasPermission("has.skip")) {
                completions.add("skip");
            }
            if (sender.hasPermission("has.version")) {
                completions.add("version");
            }
            if (sender.hasPermission("has.beep")) {
                completions.add("beep");
            }
            if (sender.hasPermission("has.endgame")) {
                completions.add("endgame");
            }
            if (sender.hasPermission("has.vote")) {
                completions.add("vote");
            }
            if (sender.hasPermission("has.addtime")) {
                completions.add("add");
            }
            if (sender.hasPermission("has.help")) {
                completions.add("help");
            }
            if (sender.hasPermission("has.autor")) {
                completions.add("autor");
            }
            if (sender.hasPermission("has.debug")) {
                completions.add("debugtime");
            }
            completions.add("language");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            List<String> completions = new ArrayList<>();
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            if (sender.hasPermission("has.select.random")) {
                completions.add("random");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player != selectedPlayer) {
            player.setGameMode(GameMode.SPECTATOR);
            checkIfSelectedPlayerKilledEveryone();
        }
        else {
            player.setGameMode(GameMode.ADVENTURE);
            checkIfSelectedPlayerKilledEveryone();
            if (timerRunning) {
                if (allPlayersDead()) {
                    gameEnd();
                    stopTimer();
                }
            }
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
                    case 200:
                    case 190:
                    case 180:
                    case 170:
                    case 160:
                    case 150:
                    case 140:
                    case 130:
                    case 120:
                    case 110:
                    case 100:
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
                        Bukkit.broadcastMessage(languageManager.getMessage("message_only") + time + (time == 1 ? languageManager.getMessage("message_second") : languageManager.getMessage("message_seconds")) + languageManager.getMessage("message_left"));
                        break;
                    case 0:
                        enablepvp();
                        startGlobalTimer();
                        break;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setLevel(time);
                }
                if (time == 0) {
                    Bukkit.broadcastMessage(languageManager.getMessage("timer_end"));
                    time = defaultTime;
                    giveItems(selectedPlayer);
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
        Bukkit.broadcastMessage(languageManager.getMessage("game_normal_end"));
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

    private void giveItems(Player player) {
        if (player != null) {
            // Schwert
            ItemStack netheriteSword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta swordMeta = netheriteSword.getItemMeta();
            swordMeta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
            netheriteSword.setItemMeta(swordMeta);
            // Bogen
            ItemStack bow = new ItemStack(Material.BOW);
            ItemMeta bowMeta = bow.getItemMeta();
            bowMeta.addEnchant(Enchantment.ARROW_DAMAGE, 3, true);
            bow.setItemMeta(bowMeta);
            // Pfeile
            ItemStack arrows = new ItemStack(Material.ARROW, 64);
            // Items geben
            player.getInventory().addItem(netheriteSword, bow, arrows);
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
            noNameTagTeam.addEntry(player.getName());
            giveEffects();
            gamerunning = true;
        }
        startTimer();
    }
    private void cancelgame() {
        for (Player player: Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            disablepvp();
            stopGlobalTimer();
            removeEffects();
            gamerunning = false;
        }
    }
    private void checkIfSelectedPlayerKilledEveryone() {
        if (selectedPlayer == null || !selectedPlayer.isOnline()) {
            // Der ausgewählte Spieler ist nicht gesetzt oder nicht online
            return;
        }

        long countAlivePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(selectedPlayer)
                        && player.isOnline()
                        && !player.getGameMode().equals(GameMode.SPECTATOR))
                .count();

        if (countAlivePlayers == 0) {
            endgame();
        }
    }

    private void endgame() {
        Bukkit.broadcastMessage(languageManager.getMessage("end_seeker") + selectedPlayer.getName() + languageManager.getMessage("end_all_found"));
        disablepvp();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(languageManager.getMessage("title_all_found"), (languageManager.getMessage("title_seeker")) + selectedPlayer.getName(), 10, 70, 20);
            removeEffects();
            stopGlobalTimer();
        }
        Bukkit.getScheduler().runTaskLater(HASPlugin.getPlugin(), () -> {
            teleportAllPlayers();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
            }
        }, 20L * 5);
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
    }
    private void enablepvp() {
        Bukkit.getWorld("world").setPVP(true);
    }
    public void playbeep(Player executor) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distance(executor.getLocation()) <= 200) { // Anpassen des Radius nach Bedarf
                player.playSound(executor.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2.0f, 1.0f);
            }
        }
    }


    public void startGlobalTimer() {
        if (globalTask != null) {
            // Der Timer läuft bereits
            return;
        }

        globalTimer = 0;
        globalTask = new BukkitRunnable() {
            @Override
            public void run() {
                globalTimer++;
                updateGlobalActionBar();
            }
        }.runTaskTimer(HASPlugin.getPlugin(), 0, 20);
    }

    public void stopGlobalTimer() {
        if (globalTask != null) {
            globalTask.cancel();
            globalTask = null;
            updateGlobalActionBar();
            globalTimer = 0;
        }
    }
    public void resetGlobalTimer() {
        stopGlobalTimer();
        globalTimer = 0;
        updateGlobalActionBar();

        // Wenn bereits ein Countdown läuft, breche ihn ab
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        // Starte einen neuen Countdown für 5 Sekunden
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                globalTimer = 0;
                updateGlobalActionBar();
            }
        }.runTaskLater(HASPlugin.getPlugin(), 5 * 20); // 5 Sekunden (20 Ticks pro Sekunde)
    }

    public void updateGlobalActionBar() {
        int minutes = globalTimer / 60;
        int remainingSeconds = globalTimer % 60;
        String minuteString = minutes > 1 ? languageManager.getMessage("message_minutes") : languageManager.getMessage("message_minute");
        String secondString = remainingSeconds > 1 ? languageManager.getMessage("message_seconds") : languageManager.getMessage("message_second");

        String timeMessage = ChatColor.GREEN + languageManager.getMessage("timer_text") + ChatColor.WHITE;

        if (minutes > 0) {
            timeMessage += minutes + minuteString + " ";
        }

        timeMessage += remainingSeconds + secondString;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(timeMessage));
        }
    }
    public void removeEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SLOW);
            player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            player.removePotionEffect(PotionEffectType.SATURATION);
        }
    }

    public void giveEffects() {
        selectedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, PotionEffect.INFINITE_DURATION, 1, true, false));
        selectedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0, true, false));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(selectedPlayer)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, PotionEffect.INFINITE_DURATION, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0, true, false));
            }
        }
    }


}
