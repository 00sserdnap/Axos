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
import cl.pandress.modules.rankup.menus.RankTopMenuListener;
import cl.pandress.modules.rankup.placeholderapi.RankPlaceholder;
import cl.pandress.modules.quests.QuestListener;
import cl.pandress.modules.quests.QuestManager;
import cl.pandress.modules.quests.menus.QuestMenuListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Axos extends JavaPlugin {

    private static Axos instance;
    private SpawnManager spawnManager;
    private RankManager  rankManager;
    private QuestManager questManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("=======================================");
        getLogger().info("  Axos Core | Iniciando sistema...     ");
        getLogger().info("  Versión: " + getDescription().getVersion());
        getLogger().info("=======================================");

        // ── Managers ──────────────────────────────────────────────────────
        spawnManager = new SpawnManager();
        rankManager  = new RankManager();
        questManager = new QuestManager();

        // ── Comandos player ───────────────────────────────────────────────
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
        getCommand("rankup").setExecutor(new RankupCommand(rankManager));
        getCommand("misiones").setExecutor(new QuestCommand());

        // ── Comandos admin ────────────────────────────────────────────────
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));
        getCommand("gamemode").setExecutor(new GamemodeCommand(new GamemodeManager()));
        getCommand("rankupadmin").setExecutor(new RankupAdminCommand(rankManager));

        // ── Listeners ─────────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new DeathSpawnListener(spawnManager), this);
        getServer().getPluginManager().registerEvents(new RankListener(rankManager), this);
        getServer().getPluginManager().registerEvents(new RankMenuListener(rankManager), this);
        getServer().getPluginManager().registerEvents(new RankTopMenuListener(), this);
        getServer().getPluginManager().registerEvents(new QuestListener(), this);
        getServer().getPluginManager().registerEvents(new QuestMenuListener(), this);

        // ── PlaceholderAPI ────────────────────────────────────────────────
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RankPlaceholder(rankManager).register();
            getLogger().info("[Axos] PlaceholderAPI encontrado, placeholders de rankup registrados.");
        }
    }

    @Override
    public void onDisable() {
        if (rankManager  != null) rankManager.shutdown();
        if (questManager != null) questManager.shutdown();

        getLogger().info("=======================================");
        getLogger().info("  Axos Core | Apagando sistema...      ");
        getLogger().info("=======================================");
    }

    public static Axos getInstance()       { return instance; }
    public RankManager  getRankManager()   { return rankManager; }
    public SpawnManager getSpawnManager()  { return spawnManager; }
    public QuestManager getQuestManager()  { return questManager; }
}