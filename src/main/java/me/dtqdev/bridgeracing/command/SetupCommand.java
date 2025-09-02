package me.dtqdev.bridgeracing.command;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.data.DataException;
import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SetupCommand implements Listener {
    private final BridgeRacing plugin;
    private final Map<UUID, SetupSession> setupSessions = new HashMap<>();
    private final ItemStack wand;

    private enum SetupStep {
        SPAWN_1, CORNER_1, CORNER_2, CHECKPOINTS_1, END_PLATE_1, FINISHED
    }

    private static class SetupSession {
        Player admin;
        String id, schematicName;
        SetupStep currentStep = SetupStep.SPAWN_1;
        Location p1_spawn, p1_corner1, p1_corner2, p1_endPlate;
        List<Location> p1_checkpoints = new ArrayList<>();
        SetupSession(Player admin, String id, String schematicName) {
            this.admin = admin;
            this.id = id;
            this.schematicName = schematicName;
        }
    }

    public SetupCommand(BridgeRacing plugin) {
        this.plugin = plugin;
        this.wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "BridgeRacing Setup Wand");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Chuột phải: Đặt điểm chính");
        lore.add(ChatColor.YELLOW + "Chuột trái: Đặt góc / Hoàn tất");
        meta.setLore(lore);
        wand.setItemMeta(meta);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi.");
            return;
        }
        if (!sender.hasPermission("bridgeracing.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền.");
            return;
        }
        Player player = (Player) sender;
        String id = args[1];
        String schematicName = args[2];
        setupSessions.put(player.getUniqueId(), new SetupSession(player, id, schematicName));
        player.getInventory().addItem(wand.clone());
        sendInstruction(player);
    }

    public void cancelSetup(Player player) {
        if (setupSessions.containsKey(player.getUniqueId())) {
            setupSessions.remove(player.getUniqueId());
            player.getInventory().remove(wand);
            player.sendMessage(ChatColor.YELLOW + "Đã hủy phiên thiết lập map.");
        } else {
            player.sendMessage(ChatColor.RED + "Bạn không đang trong phiên thiết lập nào.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!setupSessions.containsKey(player.getUniqueId())) return;
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || !itemInHand.isSimilar(wand)) return;
        event.setCancelled(true);
        SetupSession session = setupSessions.get(player.getUniqueId());
        Action action = event.getAction();
        if (event.getClickedBlock() == null && (session.currentStep != SetupStep.SPAWN_1 && session.currentStep != SetupStep.CHECKPOINTS_1)) {
            player.sendMessage(ChatColor.RED + "Bạn phải click vào một block!");
            return;
        }
        Location clickedLoc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();
        switch (session.currentStep) {
            case SPAWN_1:
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    session.p1_spawn = player.getLocation().clone(); // Lấy trực tiếp vị trí và hướng nhìn của người chơi
                    player.sendMessage(ChatColor.GREEN + "Đã đặt điểm spawn cho Làn 1 tại vị trí bạn đang đứng.");
                    advanceStep(session);
                }
                break;
            case CORNER_1:
                if (action == Action.LEFT_CLICK_BLOCK) {
                    session.p1_corner1 = clickedLoc;
                    advanceStep(session);
                }
                break;
            case CORNER_2:
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    session.p1_corner2 = clickedLoc;
                    advanceStep(session);
                }
                break;
            case CHECKPOINTS_1:
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    session.p1_checkpoints.add(clickedLoc);
                    player.sendMessage(ChatColor.GREEN + "Đã thêm checkpoint " + session.p1_checkpoints.size() + ". Chuột phải để thêm, chuột trái khi đã thêm xong.");
                } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    advanceStep(session);
                }
                break;
            case END_PLATE_1:
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    session.p1_endPlate = clickedLoc;
                    advanceStep(session);
                    finalizeSetup(session);
                }
                break;
        }
    }

    private void advanceStep(SetupSession session) {
        session.currentStep = SetupStep.values()[session.currentStep.ordinal() + 1];
        sendInstruction(session.admin);
    }

    private void sendInstruction(Player player) {
        SetupSession session = setupSessions.get(player.getUniqueId());
        if (session == null) return;
        ChatColor p = ChatColor.LIGHT_PURPLE;
        ChatColor a = ChatColor.AQUA;
        switch (session.currentStep) {
            case SPAWN_1:
            player.sendMessage(p + "Bước 1/5: " + a + "Đứng tại vị trí spawn, nhìn đúng hướng và CHUỘT PHẢI để đặt ĐIỂM SPAWN cho Làn 1.");
                break;
            case CORNER_1:
                player.sendMessage(p + "Bước 2/5: " + a + "Chuột trái vào block để đặt GÓC THỨ NHẤT của bounding box.");
                break;
            case CORNER_2:
                player.sendMessage(p + "Bước 3/5: " + a + "Chuột phải vào block để đặt GÓC THỨ HAI của bounding box.");
                break;
            case CHECKPOINTS_1:
                player.sendMessage(p + "Bước 4/5: " + a + "Chuột phải để thêm CHECKPOINT, chuột trái khi đã thêm xong.");
                break;
            case END_PLATE_1:
                player.sendMessage(p + "Bước 5/5: " + a + "Chuột phải vào block để đặt GOLD PLATE kết thúc.");
                break;
        }
    }

    private void finalizeSetup(SetupSession session) {
        Player admin = session.admin;
        admin.sendMessage(ChatColor.GOLD + "Đang xử lý và tạo map...");
        double offsetX = plugin.getConfig().getDouble("lane-offset-x", 100.0);
        double offsetY = plugin.getConfig().getDouble("lane-offset-y", 0.0);
        double offsetZ = plugin.getConfig().getDouble("lane-offset-z", 0.0);
        Location p2_spawn = session.p1_spawn.clone().add(offsetX, offsetY, offsetZ);
        Location p2_corner1 = session.p1_corner1.clone().add(offsetX, offsetY, offsetZ);
        Location p2_corner2 = session.p1_corner2.clone().add(offsetX, offsetY, offsetZ);
        Location p2_endPlate = session.p1_endPlate.clone().add(offsetX, offsetY, offsetZ);
        List<Location> p2_checkpoints = session.p1_checkpoints.stream()
                .map(loc -> loc.clone().add(offsetX, offsetY, offsetZ))
                .collect(Collectors.toList());
        try {
            plugin.getSchematicManager().paste(session.schematicName, session.p1_spawn);
            plugin.getSchematicManager().paste(session.schematicName, p2_spawn);
        } catch (MaxChangedBlocksException | DataException | IOException e) {
            admin.sendMessage(ChatColor.RED + "Lỗi khi dán schematic: " + e.getMessage());
            setupSessions.remove(admin.getUniqueId());
            admin.getInventory().remove(wand);
            return;
        }
        ConfigManager cfgManager = plugin.getConfigManager();
        FileConfiguration duelsConfig = cfgManager.getDuelsConfig();
        String basePath = "duel-arenas." + session.id;
        duelsConfig.set(basePath + ".display-name", "&aArena #" + session.id);
        duelsConfig.set(basePath + ".gui-item", "IRON_SWORD");
        duelsConfig.set(basePath + ".player1.spawn", locationToString(session.p1_spawn));
        duelsConfig.set(basePath + ".player1.corner1", locationToString(session.p1_corner1));
        duelsConfig.set(basePath + ".player1.corner2", locationToString(session.p1_corner2));
        duelsConfig.set(basePath + ".player1.end-plate", locationToString(session.p1_endPlate));
        duelsConfig.set(basePath + ".player1.checkpoints", locationsToStringList(session.p1_checkpoints));
        duelsConfig.set(basePath + ".player2.spawn", locationToString(p2_spawn));
        duelsConfig.set(basePath + ".player2.corner1", locationToString(p2_corner1));
        duelsConfig.set(basePath + ".player2.corner2", locationToString(p2_corner2));
        duelsConfig.set(basePath + ".player2.end-plate", locationToString(p2_endPlate));
        duelsConfig.set(basePath + ".player2.checkpoints", locationsToStringList(p2_checkpoints));
        cfgManager.saveDuelsConfig();
        plugin.getDuelArenaManager().loadArenas();
        admin.sendMessage(ChatColor.GREEN + "Đã thiết lập thành công map '" + session.id + "'!");
        setupSessions.remove(admin.getUniqueId());
        admin.getInventory().remove(wand);
    }

    private String locationToString(Location loc) {
        if (loc == null) return "";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private List<String> locationsToStringList(List<Location> locations) {
        List<String> list = new ArrayList<>();
        for (Location loc : locations) {
            list.add(locationToString(loc));
        }
        return list;
    }
}