package dtqdev.BridgeRacing;

import me.dtqdev.fastbuilder.FastbuilderMain;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BridgeRacing extends JavaPlugin {

    private FastbuilderMain fastBuilder;
    private DuelArenaManager duelArenaManager;
    private DuelMatchManager duelMatchManager;
    private PvPConfigManager configManager;
    private PvPGuiManager guiManager;
    private PvPPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        // Kiểm tra FastBuilder
        fastBuilder = (FastbuilderMain) Bukkit.getPluginManager().getPlugin("FastBuilder");
        if (fastBuilder == null) {
            getLogger().severe("FastBuilder not found! Disabling BridgeRacing.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Khởi tạo các manager
        configManager = new PvPConfigManager(this);
        configManager.setup();
        duelArenaManager = new DuelArenaManager(this, configManager);
        duelArenaManager.loadArenas();
        duelMatchManager = new DuelMatchManager(this, duelArenaManager);
        guiManager = new PvPGuiManager(this, duelArenaManager, duelMatchManager);

        // Đăng ký sự kiện và placeholder
        getServer().getPluginManager().registerEvents(new PvPGameListener(this, duelMatchManager, guiManager, fastBuilder), this);
        if (fastBuilder.isHolographicDisplaysEnabled()) {
            duelArenaManager.initializeHolograms();
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new PvPPlaceholderExpansion(this, duelMatchManager);
            placeholderExpansion.register();
            getLogger().info("Hooked into PlaceholderAPI!");
        }

        getLogger().info("BridgeRacing has been enabled!");
    }

    @Override
    public void onDisable() {
        if (duelMatchManager != null) {
            duelMatchManager.endAllMatches();
        }
        if (fastBuilder != null && fastBuilder.isHolographicDisplaysEnabled()) {
            duelArenaManager.clearHolograms();
        }
        getLogger().info("BridgeRacing has been disabled!");
    }

    public FastbuilderMain getFastBuilder() {
        return fastBuilder;
    }

    public DuelArenaManager getDuelArenaManager() {
        return duelArenaManager;
    }

    public DuelMatchManager getDuelMatchManager() {
        return duelMatchManager;
    }

    public PvPGuiManager getGuiManager() {
        return guiManager;
    }
}