package cz.dubcat.xpboost.events;

import cz.dubcat.xpboost.XPBoostMain;
import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import cz.dubcat.xpboost.api.XPBoostAPI;
import cz.dubcat.xpboost.constructors.Debug;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class XpBoostItemListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler
    public void rightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getItemInHand();

            if (item != null && item.getType() == Material.getMaterial(XPBoostMain.getPlugin().getConfig().getString("settings.itemmaterial"))) {
                EquipmentSlot e = event.getHand();

                if (!e.equals(EquipmentSlot.HAND)) {
                    return;
                }

                this.processPlayerInteractEvent(event);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void processPlayerInteractEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().hasLore()) {
            String itemName = item.getItemMeta().getDisplayName();
            List<String> lore = item.getItemMeta().getLore();

            int i = 0;
            double boost = 0;
            Integer time = 0;

            try {
                for (String l : lore) {
                    String str = InternalXPBoostAPI.stripColours(l);
                    str = str.replaceAll("[^\\d.]", "");
                    if (i == 0) {
                        boost = Double.parseDouble(str);
                    } else if (i == 1) {
                        time = Integer.parseInt(str);
                        break;
                    }
                    i++;
                }
            } catch (NumberFormatException ex) {
                InternalXPBoostAPI.debug("Cannot recognise XPBoost item.", Debug.NORMAL);
                return;
            }

            String name = InternalXPBoostAPI.colorizeText(XPBoostMain.getLang().getString("lang.itemname")
                    .replace("%boost%", String.valueOf(boost)).replace("%time%", String.valueOf(time)));

            if (itemName.equals(name) || itemName.contains(InternalXPBoostAPI.stripColours(name)) || itemName.contains(name)) {
                if (XPBoostAPI.hasBoost(player.getUniqueId())) {
                    InternalXPBoostAPI.sendMessage(XPBoostMain.getLang().getString("lang.boostactive"), player);
                    event.setCancelled(true);
                    return;
                }

                XPBoostAPI.setPlayerBoost(player.getUniqueId(), boost, time);
                InternalXPBoostAPI.sendMessage(XPBoostMain.getLang().getString("lang.xpbuy").replace("%boost%", "" + boost)
                        .replace("%time%", "" + time).replace("%money%", ""), player);

                player.setItemInHand(null);
                player.updateInventory();
                event.setCancelled(true);
            }
        }
    }
}
