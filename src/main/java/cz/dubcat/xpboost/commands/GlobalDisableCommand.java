package cz.dubcat.xpboost.commands;

import cz.dubcat.xpboost.XPBoostMain;
import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GlobalDisableCommand implements CommandInterface {

    private final XPBoostMain plugin;

    public GlobalDisableCommand(XPBoostMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
            if (!(player.hasPermission("xpboost.admin")) && !(player.hasPermission("xpboost.off"))) {
                return false;
            }
        }

        XPBoostMain.GLOBAL_BOOST.setEnabled(false);

        if (sender instanceof Player) {
            player = (Player) sender;
            InternalXPBoostAPI.sendMessage("Global &c" + XPBoostMain.GLOBAL_BOOST.getGlobalBoost() + "x Boost&f is now OFF.", player);
        } else {
            plugin.getLogger().info("Global " + XPBoostMain.GLOBAL_BOOST.getGlobalBoost() + "x Boost is now OFF.");
        }

        return false;
    }

}
