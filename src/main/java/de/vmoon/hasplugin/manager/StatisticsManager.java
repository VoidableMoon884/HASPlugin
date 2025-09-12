package de.vmoon.hasplugin.manager;

import de.vmoon.hasplugin.HASPlugin;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class StatisticsManager {

    private final HASPlugin plugin;

    private Connection connection;

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    private boolean enabled;

    public StatisticsManager(HASPlugin plugin) {
        this.plugin = plugin;

        // Lese Config für Statistik und MySQL
        this.enabled = plugin.getConfig().getBoolean("statistics.enabled", false);
        this.host = plugin.getConfig().getString("statistics.mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("statistics.mysql.port", 3306);
        this.database = plugin.getConfig().getString("statistics.mysql.database", "hasplugin_db");
        this.username = plugin.getConfig().getString("statistics.mysql.username", "your_user");
        this.password = plugin.getConfig().getString("statistics.mysql.password", "your_password");

        if (enabled) {
            connect();
            createTablesIfNotExists();
        }
    }

    // Verbindung zu MySQL aufbauen
    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            synchronized (this) {
                if (connection != null && !connection.isClosed()) {
                    return;
                }
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Erst Verbindungs-URL ohne Datenbank (nur zum Erstellen) bauen:
                String urlCreate = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true";

                // Verbindung ohne Datenbank aufbauen
                try (Connection tempConnection = DriverManager.getConnection(urlCreate, username, password);
                     Statement stmt = tempConnection.createStatement()) {
                    // Versuch Datenbank zu erstellen, falls nicht vorhanden
                    String sqlCreateDB = "CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
                    stmt.executeUpdate(sqlCreateDB);
                }

                // Dann Verbindung mit der Datenbank aufbauen
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true";
                connection = DriverManager.getConnection(url, username, password);
                plugin.getLogger().info("MySQL connection established!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Could not connect to MySQL for StatisticsManager");
            enabled = false;
        }
    }


    // Verbindung trennen
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    // Tabellen anlegen wenn nicht vorhanden
    private void createTablesIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS player_statistics ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "name VARCHAR(16),"
                + "games_played INT DEFAULT 0,"
                + "seeker_wins INT DEFAULT 0,"
                + "times_seeker INT DEFAULT 0,"
                + "times_hider INT DEFAULT 0"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().log(Level.SEVERE, "Failed to create statistics table");
            enabled = false;
        }
    }

    // Hilfsmethode um Spieler in DB zu haben
    private void ensurePlayerInDB(Player player) {
        if (!enabled || connection == null) return;

        String sql = "INSERT IGNORE INTO player_statistics (uuid, name) VALUES (?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Spiel starten - track times seeker or hider played and games played
    public void recordPlayedGame(Player player, boolean isSeeker) {
        if (!enabled || connection == null) return;
        ensurePlayerInDB(player);

        String column = isSeeker ? "times_seeker" : "times_hider";
        String sql = "UPDATE player_statistics SET games_played = games_played + 1, " + column + " = " + column + " + 1 WHERE uuid = ?;";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Wenn Sucher gewinnt
    public void recordSeekerWin(Player player) {
        if (!enabled || connection == null) return;
        ensurePlayerInDB(player);

        String sql = "UPDATE player_statistics SET seeker_wins = seeker_wins + 1 WHERE uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Statistiken abrufen als Map
    public Map<String, Integer> getStatistics(Player player) {
        Map<String, Integer> stats = new HashMap<>();
        if (!enabled || connection == null) return stats;
        ensurePlayerInDB(player);

        String sql = "SELECT games_played, seeker_wins, times_seeker, times_hider FROM player_statistics WHERE uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats.put("games_played", rs.getInt("games_played"));
                stats.put("seeker_wins", rs.getInt("seeker_wins"));
                stats.put("times_seeker", rs.getInt("times_seeker"));
                stats.put("times_hider", rs.getInt("times_hider"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}
