package cl.pandress.modules.quests;

import cl.pandress.Axos;
import cl.pandress.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {

    private final Axos plugin = Axos.getInstance();

    private FileConfiguration questConfig;
    private File questFile;

    private FileConfiguration dataConfig;
    private File dataFile;

    private FileConfiguration userConfig;
    private File userFile;

    private final Map<UUID, Integer> dailyLevel       = new HashMap<>();
    private final Map<UUID, Integer> questProgress    = new HashMap<>();
    private final Map<UUID, Integer> weeklyProgress   = new HashMap<>();
    private final Map<UUID, Integer> globalMilestones = new HashMap<>();
    private final Map<UUID, Long>    flyExpiry         = new HashMap<>();

    private List<String> currentActiveQuests = new ArrayList<>();

    // ── Dirty-flag async save ─────────────────────────────────────────────────
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private int saveTaskId = -1;

    public QuestManager() {
        reloadConfig();
        setupUserFile();
        setupDataFile();
        startAsyncSaveTask();
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkDailyReset, 20L * 60 * 5, 20L * 60 * 5);
        startFlyCheckTask();
    }

    // =========================================================
    //  TAREAS
    // =========================================================

    private void startAsyncSaveTask() {
        if (saveTaskId != -1) Bukkit.getScheduler().cancelTask(saveTaskId);
        saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Set<UUID> toSave = new HashSet<>(dirtyPlayers);
            dirtyPlayers.clear();
            for (UUID uuid : toSave) saveUserDataInternal(uuid);
        }, 1200L, 1200L).getTaskId();
    }

    public void shutdown() {
        if (saveTaskId != -1) Bukkit.getScheduler().cancelTask(saveTaskId);
        for (UUID uuid : dirtyPlayers) saveUserDataInternal(uuid);
        dirtyPlayers.clear();
    }

    private void startFlyCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            List<String> disabledWorlds = plugin.getConfig().getStringList("tempfly.disabled-worlds");

            for (Player p : Bukkit.getOnlinePlayers()) {
                long expiry = flyExpiry.getOrDefault(p.getUniqueId(), 0L);
                if (expiry > 0) {
                    if (now >= expiry) {
                        flyExpiry.remove(p.getUniqueId());
                        saveUserData(p.getUniqueId());
                        if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                            p.setAllowFlight(false);
                            p.setFlying(false);
                        }
                        p.sendMessage(ChatUtil.color("&c&l¡Tu Fly temporal ha expirado!"));
                    } else {
                        if (disabledWorlds.contains(p.getWorld().getName())) {
                            if (p.getAllowFlight() && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                                p.setAllowFlight(false);
                                p.setFlying(false);
                            }
                        } else {
                            if (!p.getAllowFlight()) p.setAllowFlight(true);
                        }
                    }
                }
            }
        }, 20L, 100L);
    }

    // =========================================================
    //  CONFIG Y ARCHIVOS
    // =========================================================

    public void reloadConfig() {
        if (questFile == null) questFile = new File(plugin.getDataFolder(), "modules/quests/config.yml");
        if (!questFile.exists()) plugin.saveResource("modules/quests/config.yml", false);
        questConfig = YamlConfiguration.loadConfiguration(questFile);

        if (dataFile != null) dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (userFile  != null) userConfig  = YamlConfiguration.loadConfiguration(userFile);

        plugin.getLogger().info("[Axos] Configuración y datos de misiones recargados.");
    }

    private void setupDataFile() {
        if (dataFile == null) dataFile = new File(plugin.getDataFolder(), "modules/quests/data.yml");
        if (!dataFile.exists()) {
            try { dataFile.getParentFile().mkdirs(); dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        checkDailyReset();
    }

    private void setupUserFile() {
        if (userFile == null) userFile = new File(plugin.getDataFolder(), "modules/quests/userdata.yml");
        if (!userFile.exists()) {
            try { userFile.getParentFile().mkdirs(); userFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        userConfig = YamlConfiguration.loadConfiguration(userFile);
        loadAllUserData();
    }

    // =========================================================
    //  RESET DIARIO
    // =========================================================

    private void checkDailyReset() {
        String today     = LocalDate.now(ZoneId.of("GMT-4")).toString();
        String lastReset = dataConfig.getString("last-reset", "");

        if (!today.equals(lastReset)) {
            rotateDailyQuests(today);
        } else {
            currentActiveQuests = dataConfig.getStringList("active-quests");
            if (currentActiveQuests.isEmpty()) rotateDailyQuests(today);
        }
    }

    public void forceDailyReset() {
        rotateDailyQuests(LocalDate.now(ZoneId.of("GMT-4")).toString());
    }

    private void rotateDailyQuests(String date) {
        if (questConfig.getConfigurationSection("quest-pool") == null) {
            plugin.getLogger().warning("[Axos] ERROR: No se pudo rotar las misiones — quest-pool mal formateado.");
            return;
        }

        List<String> pool = new ArrayList<>(questConfig.getConfigurationSection("quest-pool").getKeys(false));
        Collections.shuffle(pool);
        int amountToPick = Math.min(questConfig.getInt("settings.quests-per-day", 10), pool.size());
        currentActiveQuests = pool.subList(0, amountToPick);

        dataConfig.set("last-reset", date);
        dataConfig.set("active-quests", currentActiveQuests);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }

        dailyLevel.clear();
        questProgress.clear();

        userConfig = YamlConfiguration.loadConfiguration(userFile);
        if (userConfig.getConfigurationSection("players") != null) {
            for (String uuidStr : userConfig.getConfigurationSection("players").getKeys(false)) {
                userConfig.set("players." + uuidStr + ".level",    1);
                userConfig.set("players." + uuidStr + ".progress", 0);
            }
        }
        try { userConfig.save(userFile); } catch (IOException e) { e.printStackTrace(); }

        loadAllUserData();
        plugin.getLogger().info("[Axos] Misiones rotadas y progreso diario reiniciado (GMT-4).");
    }

    // =========================================================
    //  GUARDADO
    // =========================================================

    public void saveUserData(UUID uuid) {
        dirtyPlayers.add(uuid);
    }

    private synchronized void saveUserDataInternal(UUID uuid) {
        String path = "players." + uuid;
        userConfig.set(path + ".level",      getPlayerDailyLevel(uuid));
        userConfig.set(path + ".progress",   getProgress(uuid));
        userConfig.set(path + ".weekly",     weeklyProgress.getOrDefault(uuid, 0));
        userConfig.set(path + ".global",     globalMilestones.getOrDefault(uuid, 0));
        userConfig.set(path + ".fly-expiry", flyExpiry.getOrDefault(uuid, 0L));
        saveUserFile();
    }

    public void saveUserDataNow(UUID uuid) {
        dirtyPlayers.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveUserDataInternal(uuid));
    }

    private void loadAllUserData() {
        if (userConfig.getConfigurationSection("players") == null) return;
        for (String key : userConfig.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            dailyLevel.put(uuid,       userConfig.getInt("players."  + key + ".level",      1));
            questProgress.put(uuid,    userConfig.getInt("players."  + key + ".progress",   0));
            weeklyProgress.put(uuid,   userConfig.getInt("players."  + key + ".weekly",     0));
            globalMilestones.put(uuid, userConfig.getInt("players."  + key + ".global",     0));
            flyExpiry.put(uuid,        userConfig.getLong("players." + key + ".fly-expiry", 0L));
        }
    }

    private synchronized void saveUserFile() {
        try { userConfig.save(userFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // =========================================================
    //  API PÚBLICA
    // =========================================================

    public String getActiveQuestKey(int level) {
        if (level > currentActiveQuests.size() || level < 1) return null;
        return currentActiveQuests.get(level - 1);
    }

    public FileConfiguration getConfig()                { return questConfig; }
    public int  getPlayerDailyLevel(UUID uuid)          { return dailyLevel.getOrDefault(uuid, 1); }
    public void setPlayerDailyLevel(UUID uuid, int lvl) { dailyLevel.put(uuid, lvl); }
    public int  getProgress(UUID uuid)                  { return questProgress.getOrDefault(uuid, 0); }
    public long getFlyExpiry(UUID uuid)                 { return flyExpiry.getOrDefault(uuid, 0L); }
    public int  getGlobalCompleted(UUID uuid)           { return globalMilestones.getOrDefault(uuid, 0); }

    public List<Map.Entry<UUID, Integer>> getTopGlobalMissions() {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(globalMilestones.entrySet());
        list.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        return list;
    }

    public void addProgress(UUID uuid, int amount) {
        questProgress.put(uuid, getProgress(uuid) + amount);
        saveUserData(uuid);
    }

    public void resetProgress(UUID uuid) {
        questProgress.put(uuid, 0);
        saveUserData(uuid);
    }

    public void addTempFly(Player player, long durationMillis) {
        long currentExpiry = flyExpiry.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        if (currentExpiry < now) currentExpiry = now;
        flyExpiry.put(player.getUniqueId(), currentExpiry + durationMillis);
        player.setAllowFlight(true);
        saveUserData(player.getUniqueId());
    }

    public void completeQuest(Player player, int level) {
        String questKey = getActiveQuestKey(level);
        if (questKey == null) return;

        for (String cmd : questConfig.getStringList("quest-pool." + questKey + ".rewards")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.sendMessage(ChatUtil.color("&b&lMISIONES &8» &a¡Has reclamado tus recompensas!"));

        globalMilestones.put(player.getUniqueId(), getGlobalCompleted(player.getUniqueId()) + 1);

        int nextLevel = level + 1;
        setPlayerDailyLevel(player.getUniqueId(), nextLevel);
        resetProgress(player.getUniqueId());
        saveUserDataNow(player.getUniqueId());

        String rawName   = questConfig.getString("quest-pool." + questKey + ".name", questKey);
        String cleanName = ChatColor.stripColor(ChatUtil.color(rawName));
        sendWebhookLog(player, "Misión Completada",
                "Completó la misión **Nivel " + level + "** (" + cleanName + ") y recibió sus recompensas.");

        if (nextLevel == 11) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendMessage(ChatUtil.color("&b&lMISIONES &8» &e¡Felicidades! Completaste todas las misiones. ¡Reclama tu BONUS en el menú!"));
        }
    }

    public void claimDailyBonus(Player player) {
        boolean isRanked = player.hasPermission(questConfig.getString("bonus.ranked.permission", "quest.rank.rewards"));
        String bonusType = isRanked ? "VIP (Ranked)" : "Default";

        if (isRanked) {
            for (String cmd : questConfig.getStringList("bonus.ranked.commands"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            for (String msg : questConfig.getStringList("bonus.ranked.messages"))
                player.sendMessage(ChatUtil.color(msg));
        } else {
            for (String cmd : questConfig.getStringList("bonus.default.commands"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            for (String msg : questConfig.getStringList("bonus.default.messages"))
                player.sendMessage(ChatUtil.color(msg));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        setPlayerDailyLevel(player.getUniqueId(), 12);
        saveUserDataNow(player.getUniqueId());

        sendWebhookLog(player, "Bonus Reclamado",
                "Reclamó el premio final del día. Tipo: **" + bonusType + "**.");
    }

    public void resetWeeklyProgress()     { weeklyProgress.clear();   saveUserFile(); }
    public void resetMilestonesProgress() { globalMilestones.clear(); saveUserFile(); }

    // =========================================================
    //  WEBHOOK
    // =========================================================

    private void sendWebhookLog(Player player, String action, String details) {
        if (!questConfig.getBoolean("webhook.enabled", false)) return;
        String webhookUrl = questConfig.getString("webhook.url", "");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "AxosQuests-Logger");
                connection.setDoOutput(true);

                String json = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"📜 Log de Misiones\","
                        + "\"color\": 16766720,"
                        + "\"fields\": ["
                        + "{\"name\": \"Jugador\", \"value\": \"`" + player.getName() + "`\", \"inline\": true},"
                        + "{\"name\": \"Acción\", \"value\": \"" + action + "\", \"inline\": true},"
                        + "{\"name\": \"Detalles\", \"value\": \"" + details + "\", \"inline\": false}"
                        + "],"
                        + "\"footer\": {\"text\": \"Axos Quests\"},"
                        + "\"timestamp\": \"" + Instant.now() + "\""
                        + "}]"
                        + "}";

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                connection.getResponseCode();
            } catch (Exception e) {
                plugin.getLogger().warning("[Axos] No se pudo enviar el webhook de misiones: " + e.getMessage());
            }
        });
    }
}