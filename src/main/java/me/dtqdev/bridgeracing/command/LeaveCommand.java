package me.dtqdev.bridgeracing.command;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {

    private final BridgeRacing plugin;

    public LeaveCommand(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        Player player = (Player) sender;
        if (plugin.getDuelGameManager().getDuelByPlayer(player.getUniqueId()) != null) {
            plugin.getDuelGameManager().handlePlayerQuit(player, plugin.getDuelGameManager().getDuelByPlayer(player.getUniqueId()));
            player.sendMessage(ChatColor.YELLOW + "Bạn đã rời khỏi trận đấu.");
            return true;
        }
        if (plugin.getQueueManager().isPlayerInQueue(player)) {
            plugin.getQueueManager().removePlayerFromAllQueues(player);
            player.sendMessage(ChatColor.YELLOW + "Bạn đã rời khỏi hàng chờ.");
            return true;
        }
        player.sendMessage(ChatColor.RED + "Bạn không đang trong hàng chờ hoặc trận đấu nào.");
        return true;
    }
}