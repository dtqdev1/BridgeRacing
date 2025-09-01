package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final BridgeRacing plugin;
    private FileConfiguration duelsConfig, ranksConfig, playerDataConfig, historyConfig;
    private File duelsFile, ranksFile, playerdataFile, historyFile;

    public ConfigManager(BridgeRacing plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Setup config.yml (có sẵn)
        plugin.saveDefaultConfig();

        // Setup các file tùy chỉnh
        duelsFile = new File(plugin.getDataFolder(), "duels.yml");
        ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        playerdataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        historyFile = new File(plugin.getDataFolder(), "history.yml");

        if (!duelsFile.exists()) saveResource("duels.yml");
        if (!ranksFile.exists()) saveResource("ranks.yml");
        if (!playerdataFile.exists()) createFile(playerdataFile);
        if (!historyFile.exists()) createFile(historyFile);

        duelsConfig = YamlConfiguration.loadConfiguration(duelsFile);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        playerDataConfig = YamlConfiguration.loadConfiguration(playerdataFile);
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);
    }

    public void reload() {
        plugin.reloadConfig();
        duelsConfig = YamlConfiguration.loadConfiguration(duelsFile);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        playerDataConfig = YamlConfiguration.loadConfiguration(playerdataFile);
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);
    }

    // --- Getters ---
    public FileConfiguration getDuelsConfig() { return duelsConfig; }
    public FileConfiguration getRanksConfig() { return ranksConfig; }
    public FileConfiguration getPlayerDataConfig() { return playerDataConfig; }
    public FileConfiguration getHistoryConfig() { return historyConfig; }

    // --- Savers ---
    public void savePlayerData() {
        try {
            playerDataConfig.save(playerdataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
        }
    }

    public void saveHistory() {
        try {
            historyConfig.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save history.yml!");
        }
    }
    
    // --- Helpers ---
    private void createFile(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveResource(String resourcePath) {
        plugin.saveResource(resourcePath, false);
    }
    public void saveDuelsConfig() {
        try {
            duelsConfig.save(duelsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save duels.yml!");
        }
    }
}