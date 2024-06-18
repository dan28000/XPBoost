package cz.dubcat.xpboost;

import cz.dubcat.xpboost.api.InternalXPBoostAPI;
import cz.dubcat.xpboost.api.messages.ExperienceNotifier;
import cz.dubcat.xpboost.commands.*;
import cz.dubcat.xpboost.config.ConfigManager;
import cz.dubcat.xpboost.constructors.Database;
import cz.dubcat.xpboost.constructors.Database.DType;
import cz.dubcat.xpboost.constructors.Debug;
import cz.dubcat.xpboost.constructors.GlobalBoost;
import cz.dubcat.xpboost.constructors.XPBoost;
import cz.dubcat.xpboost.events.*;
import cz.dubcat.xpboost.gui.ShopClickListener;
import cz.dubcat.xpboost.support.*;
import cz.dubcat.xpboost.tasks.ActionBarTask;
import cz.dubcat.xpboost.tasks.BoostTaskCheck;
import cz.dubcat.xpboost.tasks.XPBoostTask;
import cz.dubcat.xpboost.utils.DayUtil;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class XPBoostMain extends JavaPlugin {

    public static Map<UUID, XPBoost> allplayers = new ConcurrentHashMap<>();
    public static GlobalBoost GLOBAL_BOOST;
    public static Economy economy = null;
    public static Debug debug;
    public static File langFile;
    @Getter
    public static FileConfiguration lang;
    public static File boostFile;
    public static FileConfiguration boostCfg;
    @Getter
    private static XPBoostMain plugin;
    @Getter
    private static final Database db = new Database();
    @Getter
    private static Logger log;
    @Getter
    private ExperienceNotifier experienceNotifier;
    private boolean isPaperSpigot = false;

    @Override
    public void onEnable() {
        plugin = this;
        log = getLogger();
        ConfigManager cfg = new ConfigManager();
        cfg.loadDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        try {
            if (Class.forName("com.destroystokyo.paper.VersionHistoryManager$VersionData") != null) {
                this.isPaperSpigot = true;
                log.info("Detected PaperSpigot - enabling paper spigot specific features.");
            }
        } catch (ClassNotFoundException e) {
            //ignore error
        }

        if (XPBoostMain.getPlugin().getConfig().getString("database.type").equalsIgnoreCase("mysql")) {
            if (db.loadMysql()) {
                log.info("Connected to the MySQL database.");
            } else {
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        File boostFileGen = new File(XPBoostMain.getPlugin().getDataFolder() + "/boosts.yml");
        if (!boostFileGen.exists()) {
            InputStream initialStream = getClass().getResourceAsStream("/boosts.yml");
            File targetFile = new File(XPBoostMain.getPlugin().getDataFolder() + "/boosts.yml");
            this.copyInputStreamToFile(initialStream, targetFile);
        }

        this.copyLangFiles();
        experienceNotifier = new ExperienceNotifier();

        // language
        if (getConfig().contains("settings.language")) {
            langFile = new File(XPBoostMain.getPlugin().getDataFolder() + "/lang/lang_"
                    + getConfig().getString("settings.language").toUpperCase() + ".yml");
            lang = YamlConfiguration.loadConfiguration(langFile);
        } else {
            langFile = new File(getDataFolder() + "/lang/lang_ENG.yml");
            lang = YamlConfiguration.loadConfiguration(langFile);
        }

        boostFile = new File(getDataFolder() + "/boosts.yml");
        boostCfg = YamlConfiguration.loadConfiguration(boostFile);

        // load configs
        cfg.loadLangFile();
        // INITIALIZE GLOBAL BOOST
        GLOBAL_BOOST = new GlobalBoost();
        // SETUP VAULT
        if (!setupEconomy()) {
            log.severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // LOAD DEBUG
        debug = reloadDebug();

        // MCMMO
        Plugin mcmmo = this.getServer().getPluginManager().getPlugin("mcMMO");
        if (mcmmo != null || getServer().getPluginManager().getPlugin("McMMO") != null) {
            log.info("Found McMMO, enabling support.");
            getServer().getPluginManager().registerEvents(new McMMO(this), this);
        }

        // SkillAPI
        Plugin skillapi = this.getServer().getPluginManager().getPlugin("SkillAPI");
        if (skillapi != null) {
            log.info("Found SkillAPI, enabling support");
            getServer().getPluginManager().registerEvents(new SkillApi(), this);
        }

        // JobsReborn
        Plugin jobsReborn = this.getServer().getPluginManager().getPlugin("Jobs");
        if (jobsReborn != null) {
            log.info("Found Jobs, enabling support.");
            getServer().getPluginManager().registerEvents(new JobsReborn(), this);
        }

        registerCommands();
        new XPBoostTask().runTaskTimerAsynchronously(this, 0, 5);
        new ActionBarTask().runTaskTimerAsynchronously(this, 100, getConfig().getLong("settings.activeBoostReminderOptions.periodInTicks"));

        // register events
        getServer().getPluginManager().registerEvents(new PlayerExperienceChangeListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinAndQuitEvent(), this);
        getServer().getPluginManager().registerEvents(new ServerListListener(), this);
        getServer().getPluginManager().registerEvents(new CommandListener(), this);
        getServer().getPluginManager().registerEvents(new ShopClickListener(), this);
        getServer().getPluginManager().registerEvents(new SignsClickListener(), this);
        getServer().getPluginManager().registerEvents(new ExperienceRestrictions(), this);

        String version = Bukkit.getVersion();
        if (version.contains("v1_8_R3")) {
            getServer().getPluginManager().registerEvents(new XpBoostItemListener_1_8_R3(), this);
        } else {
            getServer().getPluginManager().registerEvents(new XpBoostItemListener(), this);
        }

        // Auto global boost task
        if (getConfig().getBoolean("settings.periodicalDayCheck")) {
            new BoostTaskCheck().runTaskTimer(XPBoostMain.getPlugin(), 0, 100);
        }

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String stringDay = DayUtil.getDayOfTheWeek(day);
        if (getConfig().getBoolean("settings.day." + stringDay)) {
            GLOBAL_BOOST.setEnabled(true);
            InternalXPBoostAPI.sendMessage("&2WOHOO! Today is the " + stringDay + "! " + GLOBAL_BOOST.getGlobalBoost() + " XP day!", Bukkit.getConsoleSender());
        }

        getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Saving players...");
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            InternalXPBoostAPI.savePlayer(p.getUniqueId());
        }

        if (Database.getDatabaseType() == DType.MYSQL) {
            Database.getHikariDataSource().close();
        }

        getLogger().info("Disabled.");
    }

    public boolean isPaperSpigot() {
        return this.isPaperSpigot;
    }

    public Debug reloadDebug() {
        int debug = XPBoostMain.getPlugin().getConfig().getInt("settings.debug");

        if (debug == 0)
            return Debug.OFF;
        else if (debug == 1)
            return Debug.NORMAL;
        else if (debug == 2)
            return Debug.ALL;

        return Debug.OFF;
    }

    private void registerCommands() {
        CommandHandler handler = new CommandHandler();
        handler.register("xpboost", new MainCommand());

        OpenGuiCommand oCmd = new OpenGuiCommand();
        handler.register("gui", oCmd);
        handler.register("shop", oCmd);
        handler.register("buy", oCmd);

        handler.register("info", new InfoCommand());
        handler.register("reload", new ReloadCommand(this));
        handler.register("give", new GiveBoostCommand());
        handler.register("on", new GlobalEnableCommand(this));
        handler.register("off", new GlobalDisableCommand(this));
        handler.register("clear", new ClearCommand());
        handler.register("item", new ItemCommand(this));
        handler.register("global", new GlobalCommand(this));
        handler.register("giveDefinedBoost", new GiveDefinedBoostCommand());
        handler.register("giveIfWorseBoost", new GiveIfWorseBoostCommand());

        getCommand("xpboost").setExecutor(handler);
        getCommand("xpb").setExecutor(handler);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();

        return economy != null;
    }

    private void copyLangFiles() {
        File langNl = new File(XPBoostMain.getPlugin().getDataFolder() + "/lang/lang_NL.yml");
        if (!langNl.exists()) {
            langNl.mkdirs();
            this.copyInputStreamToFile(getClass().getResourceAsStream("/lang/lang_NL.yml"),
                    new File(XPBoostMain.getPlugin().getDataFolder() + "/lang/lang_NL.yml"));
        }

        File langZhs = new File(XPBoostMain.getPlugin().getDataFolder() + "/lang/lang_ZHS.yml");
        if (!langZhs.exists()) {
            langZhs.mkdirs();
            this.copyInputStreamToFile(getClass().getResourceAsStream("/lang/lang_ZHS.yml"),
                    new File(XPBoostMain.getPlugin().getDataFolder() + "/lang/lang_ZHS.yml"));
        }
    }

    private void copyInputStreamToFile(InputStream stream, File targetFile) {
        try {
            Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            stream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
