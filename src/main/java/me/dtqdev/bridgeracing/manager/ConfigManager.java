package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final BridgeRacing plugin;
    private FileConfiguration duelsConfig, ranksConfig, playerDataConfig, messagesConfig;
    private File duelsFile, ranksFile, playerdataFile, messagesFile;

    public ConfigManager(BridgeRacing plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        plugin.saveDefaultConfig();

        duelsFile = new File(plugin.getDataFolder(), "duels.yml");
        ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        playerdataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml"); // Thêm messages.yml

        if (!duelsFile.exists()) saveResource("duels.yml");
        if (!ranksFile.exists()) saveResource("ranks.yml");
        if (!messagesFile.exists()) saveResource("messages.yml"); // Thêm messages.yml
        if (!playerdataFile.exists()) createFile(playerdataFile);

        duelsConfig = YamlConfiguration.loadConfiguration(duelsFile);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        playerDataConfig = YamlConfiguration.loadConfiguration(playerdataFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile); // Thêm messages.yml
    }

    public void reload() {
        plugin.reloadConfig();
        duelsConfig = YamlConfiguration.loadConfiguration(duelsFile);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        playerDataConfig = YamlConfiguration.loadConfiguration(playerdataFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile); // Thêm messages.yml
    }

    public FileConfiguration getDuelsConfig() { return duelsConfig; }
    public FileConfiguration getRanksConfig() { return ranksConfig; }
    public FileConfiguration getPlayerDataConfig() { return playerDataConfig; }
    public FileConfiguration getMessagesConfig() { return messagesConfig; } // Thêm getter cho messages

    public void savePlayerData() {
        try {
            playerDataConfig.save(playerdataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
        }
    }
    
    // Đã xóa saveHistory()

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