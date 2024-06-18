package cz.dubcat.xpboost.commands;

import cz.dubcat.xpboost.api.Condition;
import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import cz.dubcat.xpboost.api.XPBoostAPI;
import cz.dubcat.xpboost.constructors.Debug;
import cz.dubcat.xpboost.constructors.XPBoost;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class GiveBoostCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // format /xpb give <player/all> <boost> <time> [coditions]
        if (sender.hasPermission("xpboost.admin")) {
            if (args.length == 4 || args.length == 5) {

                int boostDuration = 0;
                double boost = 0;
                try {
                    boostDuration = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    InternalXPBoostAPI.sendMessage("This is not an integer: &c" + args[3], sender);
                    return true;
                }

                try {
                    boost = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    InternalXPBoostAPI.sendMessage("Please provide a number. '" + args[2] + "' is not a number.", sender);
                    return true;
                }

                Set<Condition> condToApply = new HashSet<>();

                if (args.length == 5) {
                    String[] potenConditions = args[4].split(",");

                    for (String condition : potenConditions) {
                        try {
                            condToApply.add(Condition.valueOf(condition.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            InternalXPBoostAPI.sendMessage(
                                    "Parameter '" + condition
                                            + "' does not exist. Use &aVANILLA,SKILLAPI,MCMMO,RPGME,HEROES,JOBS,MYPET",
                                    sender);
                            return true;
                        }
                    }

                    InternalXPBoostAPI.debug("Found " + condToApply.size() + " conditons. " + condToApply,
                            Debug.NORMAL);
                }

                if (args[1].equalsIgnoreCase("all")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        this.giveBoost(player, boost, boostDuration, condToApply);
                    }

                    InternalXPBoostAPI.sendMessage("You have given &c" + boost + "x Boost &fto &call online players &ffor &c"
                            + boostDuration + " seconds. &6" + condToApply, sender);
                } else {
                    Player playerToGive = Bukkit.getServer().getPlayer(args[1]);
                    if (playerToGive == null || !playerToGive.isOnline()) {
                        InternalXPBoostAPI.sendMessage("Player " + args[1] + " is not online.", sender);

                        return true;
                    }

                    this.giveBoost(playerToGive, boost, boostDuration, condToApply);
                    InternalXPBoostAPI.sendMessage(
                            "You have given &c" + boost + "x Boost &fto &c" + playerToGive.getName() +
                                    " &ffor &c" + boostDuration + " seconds. &6" + condToApply, sender);
                }

            } else {
                InternalXPBoostAPI.sendMessage(
                        "Usage: &c/xpboost give <player/all> <boost> <time> [VANILLA,SKILLAPI,MCMMO,RPGME,HEROES]",
                        sender);
            }
        }
        return true;
    }

    private void giveBoost(Player player, double boost, int boostDuration, Set<Condition> conditions) {
        XPBoost xpb = XPBoostAPI.setPlayerBoost(player.getUniqueId(), boost, boostDuration);

        if (conditions != null && conditions.size() > 0) {
            for (Condition c : Condition.values()) {
                xpb.putCondition(c, conditions.contains(c));
            }
        }
    }

}
