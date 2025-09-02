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
            plugin.getMessageUtil().sendMessage(player, "command.leave.success-game");
            return true;
        }
        if (plugin.getQueueManager().isPlayerInQueue(player)) {
            plugin.getQueueManager().removePlayerFromAllQueues(player);
            plugin.getMessageUtil().sendMessage(player, "command.leave.success-queue");
            return true;
        }
        plugin.getMessageUtil().sendMessage(player, "command.leave.not-in-any");
        return true;
    }
}