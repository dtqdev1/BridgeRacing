package me.dtqdev.bridgeracing.command;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {
    private final BridgeRacing plugin;

    public DuelCommand(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getGuiManager().openMapSelector(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("setup")) {
            if (!player.hasPermission("bridgeracing.admin")) {
                player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            if (args.length < 3) {
                if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
                    plugin.getSetupCommand().cancelSetup(player);
                    return true;
                }
                player.sendMessage(ChatColor.RED + "Sử dụng: /duel setup <id> <schematic_name> hoặc /duel setup cancel");
                return true;
            }
            plugin.getSetupCommand().onCommand(player, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (!player.hasPermission("bridgeracing.admin")) {
                player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Sử dụng: /duel delete <id>");
                return true;
            }
            plugin.getDuelArenaManager().deleteArena(args[1], player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Lệnh không hợp lệ. Sử dụng: /duel [setup|delete]");
        return true;
    }
}