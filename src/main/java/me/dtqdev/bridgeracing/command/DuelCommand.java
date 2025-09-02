package me.dtqdev.bridgeracing.command;
import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.Bukkit;
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
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "setup":
                if (!player.hasPermission("bridgeracing.admin")) {
                    plugin.getMessageUtil().sendMessage(player, "error.no-permission");
                    return true;
                }
                if (args.length < 3) {
                    if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
                        plugin.getSetupCommand().cancelSetup(player);
                        return true;
                    }
                    plugin.getMessageUtil().sendMessage(player, "command.setup.invalid-args");
                    return true;
                }
                plugin.getSetupCommand().onCommand(player, args);
                break;
            case "delete":
                if (!player.hasPermission("bridgeracing.admin")) {
                    plugin.getMessageUtil().sendMessage(player, "error.no-permission");
                    return true;
                }
                if (args.length != 2) {
                    plugin.getMessageUtil().sendMessage(player, "command.delete.usage");
                    return true;
                }
                plugin.getDuelArenaManager().deleteArena(args[1], player);
                break;
            case "spectate":
                if (args.length < 2) {
                    plugin.getMessageUtil().sendMessage(player, "command.spectate.usage");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageUtil().sendMessage(player, "player-not-found");
                    return true;
                }
                if (target.equals(player)) {
                    plugin.getMessageUtil().sendMessage(player, "command.spectate.self");
                    return true;
                }
                if (plugin.getDuelGameManager().getDuelByPlayer(target.getUniqueId()) == null) {
                    plugin.getMessageUtil().sendMessage(player, "command.spectate.not-in-game");
                    return true;
                }
                plugin.getSpectateManager().startSpectating(player, target);
                break;
            default:
                plugin.getGuiManager().openMapSelector(player);
                break;
        }
        return true;
    }
}