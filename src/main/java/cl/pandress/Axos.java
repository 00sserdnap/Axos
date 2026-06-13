package cl.pandress;

import cl.pandress.command.admin.GamemodeCommand;
import cl.pandress.command.admin.SetSpawnCommand;
import cl.pandress.command.admin.RankupAdminCommand;
import cl.pandress.command.player.SpawnCommand;
import cl.pandress.command.player.RankupCommand;
import cl.pandress.command.player.QuestCommand;
import cl.pandress.essentials.gamemode.GamemodeManager;
import cl.pandress.essentials.spawn.DeathSpawnListener;
import cl.pandress.essentials.spawn.SpawnManager;
import cl.pandress.modules.rankup.RankListener;
import cl.pandress.modules.rankup.RankManager;
import cl.pandress.modules.rankup.menus.RankMenuListener;
import cl.pandress.modules.rankup.menus.RankPreviewMenuListener;
import cl.pandress.modules.rankup.menus.RankTopMenuListener;
import cl.pandress.modules.rankup.placeholderapi.RankPlaceholder;
import cl.pandress.modules.quests.QuestListener;
import cl.pandress.modules.quests.QuestManager;
import cl.pandress.modules.quests.menus.QuestMenuListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Axos extends JavaPlugin {

    private static Axos instance;

    private SpawnManager  spawnManager;
    private RankManager   rankManager;
    private QuestManager  questManager;

    // ── Estado de módulos ─────────────────────────────────────────────────
    private boolean moduleSpawnOk    = false;
    private boolean moduleGamemodeOk = false;
    private boolean moduleRankupOk   = false;
    private boolean moduleQuestsOk   = false;

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onEnable() {
        instance = this;

        printHeader();

        // ── Módulos ───────────────────────────────────────────────────
        loadModuleSpawn();
        loadModuleGamemode();
        loadModuleRankup();
        loadModuleQuests();

        // ── PlaceholderAPI ────────────────────────────────────────────
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (moduleRankupOk) {
                new RankPlaceholder(rankManager).register();
                getLogger().info("[Axos] PlaceholderAPI detectado — placeholders de rankup registrados.");
            }
        }

        // ── Resumen de módulos ────────────────────────────────────────
        printModuleSummary();

        printFooter("iniciado");
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onDisable() {
        printFooter("apagando");

        if (rankManager  != null) rankManager.shutdown();
        if (questManager != null) questManager.shutdown();
    }

    // =========================================================
    //  CARGA DE MÓDULOS
    // =========================================================

    private void loadModuleSpawn() {
        try {
            spawnManager = new SpawnManager();

            getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
            getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));

            getServer().getPluginManager().registerEvents(new DeathSpawnListener(spawnManager), this);

            moduleSpawnOk = true;
        } catch (Exception e) {
            getLogger().severe("[Axos] Error al cargar el módulo SPAWN: " + e.getMessage());
        }
    }

    private void loadModuleGamemode() {
        try {
            GamemodeManager gamemodeManager = new GamemodeManager();
            getCommand("gamemode").setExecutor(new GamemodeCommand(gamemodeManager));

            moduleGamemodeOk = true;
        } catch (Exception e) {
            getLogger().severe("[Axos] Error al cargar el módulo GAMEMODE: " + e.getMessage());
        }
    }

    private void loadModuleRankup() {
        if (!getConfig().getBoolean("modules.rankup.enabled", true)) return;

        try {
            rankManager = new RankManager();

            getCommand("rankup").setExecutor(new RankupCommand(rankManager));
            getCommand("rankupadmin").setExecutor(new RankupAdminCommand(rankManager));

            getServer().getPluginManager().registerEvents(new RankListener(rankManager), this);
            getServer().getPluginManager().registerEvents(new RankMenuListener(rankManager), this);
            getServer().getPluginManager().registerEvents(new RankPreviewMenuListener(), this);
            getServer().getPluginManager().registerEvents(new RankTopMenuListener(), this);

            moduleRankupOk = true;
        } catch (Exception e) {
            getLogger().severe("[Axos] Error al cargar el módulo RANKUP: " + e.getMessage());
        }
    }

    private void loadModuleQuests() {
        if (!getConfig().getBoolean("modules.quests.enabled", true)) return;

        try {
            questManager = new QuestManager();

            getCommand("misiones").setExecutor(new QuestCommand());

            getServer().getPluginManager().registerEvents(new QuestListener(), this);
            getServer().getPluginManager().registerEvents(new QuestMenuListener(), this);

            moduleQuestsOk = true;
        } catch (Exception e) {
            getLogger().severe("[Axos] Error al cargar el módulo QUESTS: " + e.getMessage());
        }
    }

    // =========================================================
    //  LOGS DE CONSOLA
    // =========================================================

    private void printHeader() {
        getLogger().info("+==============================================+");
        getLogger().info("|          AXOS CORE  v" + padRight(getDescription().getVersion(), 22) + "|");
        getLogger().info("|          Desarrollado por: pandress          |");
        getLogger().info("+==============================================+");
    }

    private void printModuleSummary() {
        getLogger().info("+------------ ESTADO DE MÓDULOS --------------+");
        printModuleStatus("Spawn      (essentials)", moduleSpawnOk,
            getConfig().getBoolean("modules.spawn.enabled", true));
        printModuleStatus("Gamemode   (essentials)", moduleGamemodeOk,
            getConfig().getBoolean("modules.gamemode.enabled", true));
        printModuleStatus("Rankup     (module)    ", moduleRankupOk,
            getConfig().getBoolean("modules.rankup.enabled", true));
        printModuleStatus("Quests     (module)    ", moduleQuestsOk,
            getConfig().getBoolean("modules.quests.enabled", true));
        getLogger().info("+---------------------------------------------+");
    }

    private void printModuleStatus(String name, boolean loaded, boolean enabled) {
        String status;
        if (!enabled) {
            status = "[ DESACTIVADO ]";
        } else if (loaded) {
            status = "[      OK      ]";
        } else {
            status = "[    ERROR     ]";
        }
        getLogger().info("|  " + status + "  " + name + "  |");
    }

    private void printFooter(String action) {
        getLogger().info("+==============================================+");
        getLogger().info("|  Axos Core — sistema " + padRight(action + ".", 24) + "|");
        getLogger().info("+==============================================+");
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    // =========================================================
    //  GETTERS
    // =========================================================

    public static Axos getInstance()       { return instance; }
    public SpawnManager  getSpawnManager() { return spawnManager; }
    public RankManager   getRankManager()  { return rankManager; }
    public QuestManager  getQuestManager() { return questManager; }
}