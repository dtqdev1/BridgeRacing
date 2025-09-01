package me.dtqdev.bridgeracing.command;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    private final BridgeRacing plugin;
    private final SetupCommand setupCommand;

    public DuelCommand(BridgeRacing plugin) {
        this.plugin = plugin;
        this.setupCommand = new SetupCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("setup")) {
            // Chỉ truyền các args phụ, bỏ qua "setup"
            String[] setupArgs = new String[args.length - 1];
            System.arraycopy(args, 1, setupArgs, 0, args.length - 1);
            setupCommand.onCommand(sender, setupArgs);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        Player player = (Player) sender;
        plugin.getGuiManager().openMapSelector(player);
        return true;
    }
}