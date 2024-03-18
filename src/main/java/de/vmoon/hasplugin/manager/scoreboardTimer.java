package de.vmoon.hasplugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class scoreboardTimer {
    private ScoreboardManager scoreboardManager;
    private Scoreboard scoreboard;
    private Objective timerObjective;

    public scoreboardTimer() {
        scoreboardManager = Bukkit.getScoreboardManager();
        scoreboard = scoreboardManager.getNewScoreboard();
        timerObjective = scoreboard.registerNewObjective("Timer", "dummy", "Timer");
        timerObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void startTimer() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    public void stopTimer(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void deleteTimer(Player player) {
        scoreboard.resetScores((player.getName()));
    }
}
