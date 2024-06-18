package cz.dubcat.xpboost.api.messages;

import cz.dubcat.xpboost.XPBoostMain;
import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import cz.dubcat.xpboost.utils.ActionBar;
import org.bukkit.entity.Player;

public class ExperienceNotifier {

    private final String messageSettingsPath = "settings.experienceGainedMessagesOptions";
    private final String messageReminderSettingsPath = "settings.activeBoostReminderOptions";
    private final ActionBar actionBar;

    public ExperienceNotifier() {
        this.actionBar = new ActionBar();
    }

    public void experienceGainedNotification(Player player, String message) {
        if (this.notificationsEnabled()) {
            String messageLocationRaw = XPBoostMain.getPlugin().getConfig().getString(messageSettingsPath + ".location").toUpperCase();
            MessageLocation messageLocation = MessageLocation.valueOf(messageLocationRaw);
            this.sendNotification(player, messageLocation, message);
        }
    }

    public void reminderNotification(Player player, String message) {
        String messageLocationRaw = XPBoostMain.getPlugin().getConfig().getString(messageReminderSettingsPath + ".location").toUpperCase();
        MessageLocation messageLocation = MessageLocation.valueOf(messageLocationRaw);
        this.sendNotification(player, messageLocation, message);
    }

    public boolean notificationsEnabled() {
        return XPBoostMain.getPlugin().getConfig().getBoolean(messageSettingsPath + ".enabled");
    }

    private void sendNotification(Player player, MessageLocation messageLocation, String message) {
        switch (messageLocation) {
            case ACTIONBAR:
                this.actionBar.sendActionBar(player, InternalXPBoostAPI.colorizeText(message));
                break;
            case CHAT:
                InternalXPBoostAPI.sendMessage(message, player);
                break;
        }
    }
}
