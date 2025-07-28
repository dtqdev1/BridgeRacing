package dtqdev.BridgeRacing;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class PvPConfigManager {
    private final BridgeRacing plugin;
    private File configFile;
    private FileConfiguration config;

    public PvPConfigManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        configFile = new File(plugin.getDataFolder(), "duels.yml");
        if (!configFile.exists()) {
            plugin.saveResource("duels.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save duels.yml: " + e.getMessage());
        }
    }
}