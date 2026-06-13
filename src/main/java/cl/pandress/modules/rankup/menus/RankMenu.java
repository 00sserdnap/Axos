package cl.pandress.modules.rankup.menus;

import cl.pandress.Axos;
import cl.pandress.modules.rankup.RankManager;
import cl.pandress.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankMenu {

    private static final DecimalFormat DF = new DecimalFormat("#.#");

    public static void open(Player player) {
        RankManager manager = Axos.getInstance().getRankManager();
        FileConfiguration cfg   = manager.getConfig();   // config.yml  — menú
        FileConfiguration rnks  = manager.getRanks();    // ranks.yml   — rangos

        String title   = cfg.getString("menu.title", "&8Menu | RankUp");
        String typeStr = cfg.getString("menu.type", "CHEST").toUpperCase();

        Inventory inv;
        if (typeStr.equals("CHEST")) {
            int rows = cfg.getInt("menu.rows", 3);
            int size = Math.min(6, Math.max(1, rows)) * 9;
            inv = Bukkit.createInventory((InventoryHolder) null, size, ChatUtil.color(title));
        } else {
            InventoryType type = InventoryType.valueOf(typeStr);
            inv = Bukkit.createInventory((InventoryHolder) null, type, ChatUtil.color(title));
        }

        // — Relleno decorativo —
        if (cfg.getBoolean("menu.items.fillers.enabled", true)) {
            Material mat = Material.matchMaterial(cfg.getString("menu.items.fillers.material", "GRAY_STAINED_GLASS_PANE"));
            if (mat != null) {
                ItemStack filler = new ItemStack(mat);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatUtil.color(cfg.getString("menu.items.fillers.name", " ")));
                    filler.setItemMeta(meta);
                }
                for (int slot : cfg.getIntegerList("menu.items.fillers.slots")) {
                    if (slot < inv.getSize()) inv.setItem(slot, filler);
                }
            }
        }

        // — Slot 11: botón preview —
        int previewSlot = cfg.getInt("menu.items.preview_button.slot", 11);
        if (previewSlot < inv.getSize()) inv.setItem(previewSlot, buildPreviewButton(cfg));

        // — Slot 13: botón rankup —
        int rankupSlot = cfg.getInt("menu.items.rankup_button.slot", 13);
        if (rankupSlot < inv.getSize()) {
            int nextRank = manager.getPlayerRank(player.getUniqueId()) + 1;
            inv.setItem(rankupSlot, buildRankupItem(player, nextRank, manager, cfg, rnks));
        }

        // — Slot 15: botón top —
        int topSlot = cfg.getInt("menu.items.top_button.slot", 15);
        if (topSlot < inv.getSize()) inv.setItem(topSlot, buildTopItem(manager, cfg));

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
        player.openInventory(inv);
    }

    // ── Botón preview (slot 11) ───────────────────────────────────────────

    private static ItemStack buildPreviewButton(FileConfiguration cfg) {
        String path  = "menu.items.preview_button.";
        Material mat = Material.matchMaterial(cfg.getString(path + "material", "BOOK"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.BOOK);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatUtil.color(cfg.getString(path + "name", "&b&lVer Todos los Rangos")));
        List<String> lore = new ArrayList<>();
        List<String> cfgLore = cfg.getStringList(path + "lore");
        if (cfgLore.isEmpty()) {
            lore.add(ChatUtil.color("&7Explora todos los rangos disponibles,"));
            lore.add(ChatUtil.color("&7sus requisitos y recompensas."));
            lore.add(""); lore.add(ChatUtil.color("&eClick para abrir"));
        } else {
            for (String l : cfgLore) lore.add(ChatUtil.color(l));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Botón rankup (slot 13) ────────────────────────────────────────────

    private static ItemStack buildRankupItem(Player player, int nextRank, RankManager manager,
                                             FileConfiguration cfg, FileConfiguration rnks) {
        String pathMenu = "menu.items.rankup_button.";
        Material mat    = Material.matchMaterial(cfg.getString(pathMenu + "material", "EMERALD"));
        ItemStack item  = new ItemStack(mat != null ? mat : Material.EMERALD);
        ItemMeta  meta  = item.getItemMeta();
        if (meta == null) return item;

        String pathRank = "ranks." + nextRank;
        if (rnks.getConfigurationSection(pathRank) == null) {
            meta.setDisplayName(ChatUtil.color(cfg.getString(pathMenu + "name_max", "&a&lRango Máximo Alcanzado")));
            item.setItemMeta(meta);
            return item;
        }

        meta.setDisplayName(ChatUtil.color(
            cfg.getString(pathMenu + "name", "&a&lRango #%next_rank%")
               .replace("%next_rank%", String.valueOf(nextRank))
        ));

        List<String> finalLore   = new ArrayList<>();
        List<String> rewardsLore = rnks.getStringList(pathRank + ".rewards_lore");

        for (String line : cfg.getStringList(pathMenu + "lore")) {
            if (line.contains("%rewards%")) {
                String sep = cfg.getString("menu.reward_separator", " &8| ");
                for (String reward : rewardsLore) finalLore.add(ChatUtil.color(sep + "&a" + reward));
            } else if (line.contains("%requirements%")) {
                appendRequirementLines(finalLore, player, manager, cfg, rnks, pathRank);
            } else {
                finalLore.add(ChatUtil.color(line));
            }
        }

        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    static void appendRequirementLines(List<String> lore, Player player, RankManager manager,
                                       FileConfiguration cfg, FileConfiguration rnks, String pathRank) {
        UUID   uuid = player.getUniqueId();
        String sep  = cfg.getString("menu.requirement_separator", " &8| ");

        if (rnks.contains(pathRank + ".requirements.money")) {
            double req   = rnks.getDouble(pathRank + ".requirements.money");
            double bal   = manager.getEconomy() != null ? manager.getEconomy().getBalance(player) : 0.0;
            String color = bal >= req ? "&a" : "&c";
            lore.add(ChatUtil.color(sep + "&7Dinero: " + color + "$" + formatNumber(req)));
        }
        if (rnks.contains(pathRank + ".requirements.playtime_hours")) {
            int req     = rnks.getInt(pathRank + ".requirements.playtime_hours");
            int current = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000;
            String col  = current >= req ? "&a" : "&c";
            lore.add(ChatUtil.color(sep + "&7Horas: " + col + current + "&8/&" + col.charAt(1) + req + "h"));
        }
        if (rnks.contains(pathRank + ".requirements.player_kills")) {
            int req     = rnks.getInt(pathRank + ".requirements.player_kills");
            int current = manager.getProgress(uuid, "general", "player_kills");
            String col  = current >= req ? "&a" : "&c";
            lore.add(ChatUtil.color(sep + "&7Kills PvP: " + col + current + "&8/" + col + req));
        }
        if (rnks.getConfigurationSection(pathRank + ".requirements.blocks_mine") != null) {
            for (String block : rnks.getConfigurationSection(pathRank + ".requirements.blocks_mine").getKeys(false)) {
                int req     = rnks.getInt(pathRank + ".requirements.blocks_mine." + block);
                int current = manager.getProgress(uuid, "blocks_mine", block);
                String col  = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color(sep + "&7Picar " + formatName(block) + ": " + col + current + "&8/" + col + req));
            }
        }
        if (rnks.getConfigurationSection(pathRank + ".requirements.blocks_place") != null) {
            for (String block : rnks.getConfigurationSection(pathRank + ".requirements.blocks_place").getKeys(false)) {
                int req     = rnks.getInt(pathRank + ".requirements.blocks_place." + block);
                int current = manager.getProgress(uuid, "blocks_place", block);
                String col  = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color(sep + "&7Colocar " + formatName(block) + ": " + col + current + "&8/" + col + req));
            }
        }
        if (rnks.getConfigurationSection(pathRank + ".requirements.mob_kills") != null) {
            for (String mob : rnks.getConfigurationSection(pathRank + ".requirements.mob_kills").getKeys(false)) {
                int req     = rnks.getInt(pathRank + ".requirements.mob_kills." + mob);
                int current = manager.getProgress(uuid, "mob_kills", mob);
                String col  = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color(sep + "&7Matar " + formatName(mob) + ": " + col + current + "&8/" + col + req));
            }
        }
    }

    // ── Botón top (slot 15) ───────────────────────────────────────────────

    private static ItemStack buildTopItem(RankManager manager, FileConfiguration cfg) {
        String path = "menu.items.top_button.";
        Material mat = Material.matchMaterial(cfg.getString(path + "material", "BELL"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.BELL);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatUtil.color(cfg.getString(path + "name", "&6&lTop Rangos")));
        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList(path + "lore_header")) lore.add(ChatUtil.color(line));

        String format = cfg.getString(path + "lore_format", " &8| &e#%pos% &f%player% &8(&7Rango %rank%&8)");
        int pos = 1;
        for (Map.Entry<UUID, Integer> entry : manager.getTopRanks()) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Desconocido";
            lore.add(ChatUtil.color(
                format.replace("%pos%", String.valueOf(pos))
                      .replace("%player%", name)
                      .replace("%rank%", String.valueOf(entry.getValue()))
            ));
            pos++;
        }
        for (String line : cfg.getStringList(path + "lore_footer")) lore.add(ChatUtil.color(line));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Utilidades compartidas ────────────────────────────────────────────

    static String formatName(String id) {
        StringBuilder sb = new StringBuilder();
        for (String w : id.toLowerCase().split("_")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    static String formatNumber(double value) {
        if (value >= 1_000_000_000) return DF.format(value / 1_000_000_000) + "b";
        if (value >= 1_000_000)     return DF.format(value / 1_000_000)     + "m";
        if (value >= 1_000)         return DF.format(value / 1_000)         + "k";
        return DF.format(value);
    }
}