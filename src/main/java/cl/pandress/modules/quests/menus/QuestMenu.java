package cl.pandress.modules.quests.menus;

import cl.pandress.Axos;
import cl.pandress.modules.quests.QuestManager;
import cl.pandress.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class QuestMenu {

    // Caché de nombres UUID→nombre para evitar lecturas de disco en hilo principal
    private static final Map<UUID, String> nameCache = new HashMap<>();

    public static void cachePlayerName(UUID uuid, String name) {
        nameCache.put(uuid, name);
    }

    private static String getCachedName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            nameCache.put(uuid, online.getName());
            return online.getName();
        }
        if (nameCache.containsKey(uuid)) return nameCache.get(uuid);
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null) name = "Desconocido";
        nameCache.put(uuid, name);
        return name;
    }

    public static void open(Player player) {
        open(player, 1);
    }

    public static void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.color("&8Misiones Diarias"));
        QuestManager manager = Axos.getInstance().getQuestManager();
        int currentLevel = manager.getPlayerDailyLevel(player.getUniqueId());

        inv.setItem(12, getQuestItem(currentLevel, player, manager));
        inv.setItem(14, getStatsItem(player, manager, page));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
        player.openInventory(inv);
    }

    private static ItemStack getStatsItem(Player player, QuestManager manager, int page) {
        ItemStack item = new ItemStack(Material.BELL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatUtil.color("&e&lTop Misiones & Estadísticas"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtil.color("&7Total global de misiones completadas."));
        lore.add("");

        List<Map.Entry<UUID, Integer>> allTop = manager.getTopGlobalMissions();
        if (allTop.size() > 30) allTop = allTop.subList(0, 30);

        int maxPages = Math.max(1, (int) Math.ceil(allTop.size() / 10.0));
        page = Math.max(1, Math.min(page, maxPages));

        lore.add(ChatUtil.color("&6&lTOP GLOBAL (Pág " + page + "/" + maxPages + "):"));

        if (allTop.isEmpty()) {
            lore.add(ChatUtil.color("&cNo hay misiones completadas aún."));
        } else {
            int startIndex = (page - 1) * 10;
            int endIndex   = Math.min(startIndex + 10, allTop.size());
            for (int i = startIndex; i < endIndex; i++) {
                Map.Entry<UUID, Integer> entry = allTop.get(i);
                String playerName = getCachedName(entry.getKey());
                lore.add(ChatUtil.color("&e" + (i + 1) + ". &f" + playerName + " &8- &a" + entry.getValue()));
            }
        }

        lore.add("");
        lore.add(ChatUtil.color("&e&lTUS ESTADÍSTICAS:"));
        lore.add(ChatUtil.color("&fCompletadas: &a" + manager.getGlobalCompleted(player.getUniqueId())));
        lore.add("");
        lore.add(ChatUtil.color("&7Click Izquierdo &8» &fAvanzar Pág"));
        lore.add(ChatUtil.color("&7Click Derecho &8» &fRetroceder Pág"));
        lore.add(ChatUtil.color("&0PAGE:" + page));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack getQuestItem(int level, Player player, QuestManager manager) {
        FileConfiguration config = manager.getConfig();
        ItemStack item;
        ItemMeta meta;

        if (level == 11) {
            item = new ItemStack(Material.NETHER_STAR);
            meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.color("&d&l¡Recompensa Bonus!"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatUtil.color("&7Has completado todas las misiones de hoy."));
                lore.add("");
                lore.add(ChatUtil.color("&fRecompensa:"));
                lore.add(ChatUtil.color("&d★ &7¡Reclama para saber qué ganaste!"));
                lore.add("");
                lore.add(ChatUtil.color("&a¡Click para reclamar!"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        if (level >= 12) {
            item = new ItemStack(Material.EMERALD);
            meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.color(config.getString("messages.completed-all", "&a&l¡Completadas!")));
                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList("messages.completed-lore"))
                    lore.add(ChatUtil.color(line));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        String questKey = manager.getActiveQuestKey(level);
        if (questKey == null) return new ItemStack(Material.PAPER);

        String path   = "quest-pool." + questKey;
        String matStr = config.getString(path + ".material", "PAPER");
        item = new ItemStack(Material.matchMaterial(matStr) != null ? Material.valueOf(matStr) : Material.PAPER);

        meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.color("&8[&e" + level + "&8/&e10&8] " + config.getString(path + ".name")));
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList(path + ".lore"))
                lore.add(ChatUtil.color(line));

            int progress = manager.getProgress(player.getUniqueId());
            int required = config.getInt(path + ".action-amount");
            lore.add("");
            lore.add(ChatUtil.color("&7Progreso: &e" + progress + "&8/&e" + required));
            lore.add(ChatUtil.color(progress >= required ? "&a¡Click para reclamar!" : "&cMisión en progreso..."));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}