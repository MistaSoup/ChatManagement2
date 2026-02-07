package com.chatmanagement.storage;

import com.chatmanagement.ChatManagement2;
import com.chatmanagement.managers.MuteManager.MuteData;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    
    private final ChatManagement2 plugin;
    private Connection connection;
    private final String type;
    
    public DatabaseManager(ChatManagement2 plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfigManager().getDatabaseType().toLowerCase();
    }
    
    /**
     * Connect to the database
     */
    public boolean connect() {
        try {
            if (type.equals("sqlite")) {
                return connectSQLite();
            } else if (type.equals("mysql")) {
                return connectMySQL();
            } else {
                plugin.getLogger().warning("Unknown database type: " + type);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Connect to SQLite database
     */
    private boolean connectSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/chatmanagement.db";
        connection = DriverManager.getConnection(url);
        
        createTables();
        return true;
    }
    
    /**
     * Connect to MySQL database
     */
    private boolean connectMySQL() throws SQLException {
        String host = plugin.getConfigManager().getDatabaseHost();
        int port = plugin.getConfigManager().getDatabasePort();
        String database = plugin.getConfigManager().getDatabaseName();
        String username = plugin.getConfigManager().getDatabaseUsername();
        String password = plugin.getConfigManager().getDatabasePassword();
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, username, password);
        
        createTables();
        return true;
    }
    
    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        String createMutesTable = "CREATE TABLE IF NOT EXISTS mutes (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "end_time BIGINT NOT NULL," +
                "original_duration INT NOT NULL," +
                "is_paused BOOLEAN NOT NULL," +
                "paused_time_remaining BIGINT NOT NULL" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createMutesTable);
        }
    }
    
    /**
     * Check if connected to database
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Disconnect from database
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error disconnecting from database: " + e.getMessage());
        }
    }
    
    /**
     * Save a mute to database
     */
    public void saveMute(UUID uuid, MuteData muteData) {
        String sql = "REPLACE INTO mutes (uuid, end_time, original_duration, is_paused, paused_time_remaining) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, muteData.endTime);
            stmt.setInt(3, muteData.originalDuration);
            stmt.setBoolean(4, muteData.isPaused);
            stmt.setLong(5, muteData.pausedTimeRemaining);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving mute to database: " + e.getMessage());
        }
    }
    
    /**
     * Remove a mute from database
     */
    public void removeMute(UUID uuid) {
        String sql = "DELETE FROM mutes WHERE uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing mute from database: " + e.getMessage());
        }
    }
    
    /**
     * Load all mutes from database
     */
    public Map<UUID, MuteData> loadMutes() {
        Map<UUID, MuteData> mutes = new HashMap<>();
        String sql = "SELECT * FROM mutes";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long endTime = rs.getLong("end_time");
                int originalDuration = rs.getInt("original_duration");
                boolean isPaused = rs.getBoolean("is_paused");
                long pausedTimeRemaining = rs.getLong("paused_time_remaining");
                
                MuteData muteData = new MuteData(endTime, originalDuration, isPaused, pausedTimeRemaining);
                mutes.put(uuid, muteData);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading mutes from database: " + e.getMessage());
        }
        
        return mutes;
    }
    
    /**
     * Clean up expired mutes from database
     */
    public void cleanupExpiredMutes() {
        String sql = "DELETE FROM mutes WHERE end_time < ? AND is_paused = false";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0 && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Cleaned up " + deleted + " expired mutes from database");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cleaning up expired mutes: " + e.getMessage());
        }
    }
}
