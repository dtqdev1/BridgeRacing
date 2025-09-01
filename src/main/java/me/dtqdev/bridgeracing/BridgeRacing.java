package me.dtqdev.bridgeracing;

import me.dtqdev.bridgeracing.command.DuelCommand;
import me.dtqdev.bridgeracing.command.LeaveCommand;
import me.dtqdev.bridgeracing.game.DuelGameManager;
import me.dtqdev.bridgeracing.listener.GUIListener;
import me.dtqdev.bridgeracing.listener.GameListener;
import me.dtqdev.bridgeracing.manager.*;
import me.dtqdev.fastbuilder.FastbuilderMain;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BridgeRacing extends JavaPlugin {
    private static BridgeRacing instance;
    private FastbuilderMain fastBuilderAPI;
    private ConfigManager configManager;
    private DuelArenaManager duelArenaManager;
    private EloManager eloManager;
    private HistoryManager historyManager;
    private QueueManager queueManager;
    private DuelGameManager duelGameManager;
    private GUIManager guiManager;
    private SchematicManager schematicManager;
    private DuelRecordManager duelRecordManager;

    @Override
    public void onEnable() {
        instance = this;
        Plugin fbPlugin = getServer().getPluginManager().getPlugin("FastBuilder");
        if (fbPlugin instanceof FastbuilderMain) {
            this.fastBuilderAPI = (FastbuilderMain) fbPlugin;
            getLogger().info("Successfully hooked into FastBuilder!");
        } else {
            getLogger().severe("FastBuilder not found! Disabling BridgeRacing.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.configManager = new ConfigManager(this);
        this.duelArenaManager = new DuelArenaManager(this);
        this.eloManager = new EloManager(this);
        this.historyManager = new HistoryManager(this);
        this.queueManager = new QueueManager(this);
        this.duelGameManager = new DuelGameManager(this);
        this.guiManager = new GUIManager(this);
        this.schematicManager = new SchematicManager(this);
        this.duelRecordManager = new DuelRecordManager(this);
        duelArenaManager.loadArenas();
        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BridgeRacingExpansion(this).register();
            getLogger().info("Successfully registered placeholders.");
        }
        getLogger().info("BridgeRacing has been enabled!");
    }

    @Override
    public void onDisable() {
        if (duelGameManager != null) {
            duelGameManager.endAllGames();
        }
        if (eloManager != null) {
            eloManager.saveAllPlayerData();
        }
    }

    public static BridgeRacing getInstance() { return instance; }
    public FastbuilderMain getFastBuilderAPI() { return fastBuilderAPI; }
    public ConfigManager getConfigManager() { return configManager; }
    public DuelArenaManager getDuelArenaManager() { return duelArenaManager; }
    public EloManager getEloManager() { return eloManager; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public DuelGameManager getDuelGameManager() { return duelGameManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public SchematicManager getSchematicManager() { return schematicManager; }
    public DuelRecordManager getDuelRecordManager() { return duelRecordManager; }
}