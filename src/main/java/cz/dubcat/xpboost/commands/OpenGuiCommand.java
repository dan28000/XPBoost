package cz.dubcat.xpboost.commands;

import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenGuiCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player = (Player) sender;
        if (player.hasPermission("xpboost.use") || player.hasPermission("xpboost.gui")) {
            InternalXPBoostAPI.openXpBoostShop(player);
        }
        return false;
    }
}
