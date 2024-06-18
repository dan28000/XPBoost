package cz.dubcat.xpboost.events;

import cz.dubcat.xpboost.XPBoostMain;
import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import cz.dubcat.xpboost.constructors.GlobalBoost;
import cz.dubcat.xpboost.constructors.XPBoost;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinAndQuitEvent implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (XPBoostMain.getPlugin().getConfig().getBoolean("settings.globalboost.notification")) {
            GlobalBoost gl = XPBoostMain.GLOBAL_BOOST;
            if (gl.isEnabled()) {
                InternalXPBoostAPI.sendMessage(
                        XPBoostMain.getLang().getString("lang.joinnotmsg").replaceAll("%boost%", gl.getGlobalBoost() + ""),
                        player);
            }
        }

        XPBoost boost = InternalXPBoostAPI.loadPlayer(player.getUniqueId());
        if (boost != null) {
            XPBoostMain.allplayers.put(player.getUniqueId(), boost);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        InternalXPBoostAPI.savePlayer(player.getUniqueId());
        XPBoostMain.allplayers.remove(player.getUniqueId());
    }

}
