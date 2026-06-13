package cl.pandress.modules.rankup;

import cl.pandress.Axos;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RankManager {

    private final Axos plugin = Axos.getInstance();

    // ── Archivos de configuración ──────────────────────────────────────────
    private FileConfiguration config;    // config.yml   — settings + menú
    private FileConfiguration ranks;     // ranks.yml    — definición de rangos
    private FileConfiguration messages;  // messages.yml — mensajes

    private File configFile;
    private File ranksFile;
    private File messagesFile;

    // ── Datos de progreso ──────────────────────────────────────────────────
    private FileConfiguration dataConfig;
    private File dataFile;

    private Economy econ = null;

    // ── Datos en memoria ───────────────────────────────────────────────────
    private final Map<UUID, Integer>              playerRanks    = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerProgress = new ConcurrentHashMap<>();

    // ── Dirty-flag async save ──────────────────────────────────────────────
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private int saveTaskId = -1;

    public RankManager() {
        setupConfigs();
        setupEconomy();
        loadData();
        startAsyncSaveTask();
    }

    // =========================================================
    //  TAREA ASÍNCRONA DE GUARDADO
    // =========================================================

    private void startAsyncSaveTask() {
        if (saveTaskId != -1) Bukkit.getScheduler().cancelTask(saveTaskId);
        saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Set<UUID> toSave = new HashSet<>(dirtyPlayers);
            dirtyPlayers.removeAll(toSave);
            for (UUID uuid : toSave) savePlayerDataInternal(uuid);
        }, 1200L, 1200L).getTaskId();
    }

    public void shutdown() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        for (UUID uuid : dirtyPlayers) savePlayerDataInternal(uuid);
        dirtyPlayers.clear();
    }

    // =========================================================
    //  GUARDADO INTERNO
    // =========================================================

    public void markDirty(UUID uuid) {
        dirtyPlayers.add(uuid);
    }

    private synchronized void savePlayerDataInternal(UUID uuid) {
        dataConfig.set("ranks." + uuid, getPlayerRank(uuid));

        Map<String, Integer> prog = playerProgress.get(uuid);
        if (prog != null && !prog.isEmpty()) {
            for (Map.Entry<String, Integer> entry : prog.entrySet()) {
                dataConfig.set("progress." + uuid + "." + entry.getKey(), entry.getValue());
            }
        } else {
            dataConfig.set("progress." + uuid, null);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[RankManager] Error al guardar datos de " + uuid + ": " + e.getMessage());
        }
    }

    public void savePlayerDataNow(UUID uuid) {
        dirtyPlayers.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerDataInternal(uuid));
    }

    // =========================================================
    //  CONFIGURACIÓN Y CARGA
    // =========================================================

    private void setupConfigs() {
        File folder = new File(plugin.getDataFolder(), "modules/rankup");

        // ── config.yml ──
        configFile = new File(folder, "config.yml");
        if (!configFile.exists()) plugin.saveResource("modules/rankup/config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);

        // ── ranks.yml ──
        ranksFile = new File(folder, "ranks.yml");
        if (!ranksFile.exists()) plugin.saveResource("modules/rankup/ranks.yml", false);
        ranks = YamlConfiguration.loadConfiguration(ranksFile);

        // ── messages.yml ──
        messagesFile = new File(folder, "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("modules/rankup/messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // ── rankdata.yml (datos de jugadores, no se expone como resource) ──
        dataFile = new File(folder, "rankdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[RankManager] No se pudo crear rankdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public void reloadConfig() {
        config   = YamlConfiguration.loadConfiguration(configFile);
        ranks    = YamlConfiguration.loadConfiguration(ranksFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        playerRanks.clear();
        playerProgress.clear();
        loadData();
        plugin.getLogger().info("[Axos] Configuración y datos de rankup recargados.");
    }

    public void loadData() {
        if (dataConfig.getConfigurationSection("ranks") != null) {
            for (String key : dataConfig.getConfigurationSection("ranks").getKeys(false)) {
                playerRanks.put(UUID.fromString(key), dataConfig.getInt("ranks." + key));
            }
        }
        if (dataConfig.getConfigurationSection("progress") != null) {
            for (String uuidStr : dataConfig.getConfigurationSection("progress").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Integer> prog = new HashMap<>();
                if (dataConfig.getConfigurationSection("progress." + uuidStr) != null) {
                    for (String key : dataConfig.getConfigurationSection("progress." + uuidStr).getKeys(false)) {
                        prog.put(key, dataConfig.getInt("progress." + uuidStr + "." + key));
                    }
                }
                playerProgress.put(uuid, prog);
            }
        }
    }

    // =========================================================
    //  API PÚBLICA
    // =========================================================

    public int getPlayerRank(UUID uuid) {
        return playerRanks.getOrDefault(uuid, 0);
    }

    public void setPlayerRank(UUID uuid, int rank) {
        playerRanks.put(uuid, rank);
        playerProgress.remove(uuid);
        dataConfig.set("progress." + uuid, null);
        savePlayerDataNow(uuid);
    }

    public int getProgress(UUID uuid, String category, String type) {
        String key = category + ":" + type;
        Map<String, Integer> prog = playerProgress.get(uuid);
        if (prog == null) return 0;
        return prog.getOrDefault(key, 0);
    }

    public void addProgress(UUID uuid, String category, String type, int amount) {
        String key = category + ":" + type;
        playerProgress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                      .merge(key, amount, Integer::sum);
        markDirty(uuid);
    }

    public List<Map.Entry<UUID, Integer>> getTopRanks() {
        return playerRanks.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .collect(Collectors.toList());
    }

    public void resetPlayerRank(UUID uuid) {
        playerRanks.remove(uuid);
        playerProgress.remove(uuid);
        dataConfig.set("ranks." + uuid, null);
        dataConfig.set("progress." + uuid, null);
        savePlayerDataNow(uuid);
    }

    public void resetAllRanks() {
        playerRanks.clear();
        playerProgress.clear();
        dirtyPlayers.clear();
        dataConfig.set("ranks", null);
        dataConfig.set("progress", null);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[RankManager] Error en resetAllRanks: " + e.getMessage());
        }
    }

    // ── Getters de configs ─────────────────────────────────────────────────

    /** settings + menú principal */
    public FileConfiguration getConfig()    { return config; }

    /** definición de rangos (ranks.yml) */
    public FileConfiguration getRanks()     { return ranks; }

    /** mensajes (messages.yml) */
    public FileConfiguration getMessages()  { return messages; }

    /** shortcut: obtiene un mensaje ya traducido */
    public String getMessage(String path) {
        return messages.getString("messages." + path, "&cMensaje faltante: " + path);
    }

    public Economy getEconomy() { return econ; }
}