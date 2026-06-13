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
        FileConfiguration config = manager.getConfig();

        String title   = config.getString("settings.menu.title", "&8Menu | RankUp");
        String typeStr = config.getString("settings.menu.type", "CHEST").toUpperCase();

        Inventory inv;
        if (typeStr.equals("CHEST")) {
            int rows = config.getInt("settings.menu.rows", 3);
            int size = Math.min(6, Math.max(1, rows)) * 9;
            inv = Bukkit.createInventory((InventoryHolder) null, size, ChatUtil.color(title));
        } else {
            InventoryType type = InventoryType.valueOf(typeStr);
            inv = Bukkit.createInventory((InventoryHolder) null, type, ChatUtil.color(title));
        }

        // Relleno decorativo
        if (config.getBoolean("settings.menu.items.fillers.enabled", true)) {
            String matStr = config.getString("settings.menu.items.fillers.material", "GRAY_STAINED_GLASS_PANE");
            Material mat  = Material.matchMaterial(matStr);
            if (mat != null) {
                ItemStack filler = new ItemStack(mat);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatUtil.color(config.getString("settings.menu.items.fillers.name", " ")));
                    filler.setItemMeta(meta);
                }
                for (int slot : config.getIntegerList("settings.menu.items.fillers.slots")) {
                    if (slot < inv.getSize()) inv.setItem(slot, filler);
                }
            }
        }

        // Botón de rankup
        int nextRank   = manager.getPlayerRank(player.getUniqueId()) + 1;
        int rankupSlot = config.getInt("settings.menu.items.rankup_button.slot", 12);
        if (rankupSlot < inv.getSize()) {
            inv.setItem(rankupSlot, buildRankupItem(player, nextRank, manager, config));
        }

        // Botón de top
        int topSlot = config.getInt("settings.menu.items.top_button.slot", 14);
        if (topSlot < inv.getSize()) {
            inv.setItem(topSlot, buildTopItem(manager, config));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
        player.openInventory(inv);
    }

    private static ItemStack buildRankupItem(Player player, int nextRank, RankManager manager, FileConfiguration config) {
        String pathMenu = "settings.menu.items.rankup_button.";
        Material mat    = Material.matchMaterial(config.getString(pathMenu + "material", "EMERALD"));

        ItemStack item = new ItemStack(mat != null ? mat : Material.EMERALD);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;

        String pathRank = "ranks." + nextRank;

        if (config.getConfigurationSection(pathRank) == null) {
            meta.setDisplayName(ChatUtil.color(config.getString(pathMenu + "name_max", "&a&lRango Máximo Alcanzado")));
            item.setItemMeta(meta);
            return item;
        }

        meta.setDisplayName(ChatUtil.color(
            config.getString(pathMenu + "name", "&a&lRango #%next_rank%")
                  .replace("%next_rank%", String.valueOf(nextRank))
        ));

        List<String> finalLore   = new ArrayList<>();
        List<String> rewardsLore = config.getStringList(pathRank + ".rewards_lore");

        for (String line : config.getStringList(pathMenu + "lore")) {
            if (line.contains("%rewards%")) {
                String sep = config.getString("settings.menu.reward_separator", " &8| ");
                for (String reward : rewardsLore) {
                    finalLore.add(ChatUtil.color(sep + "&a" + reward));
                }
                continue;
            }
            if (line.contains("%requirements%")) {
                appendRequirementLines(finalLore, player, manager, config, pathRank);
                continue;
            }
            finalLore.add(ChatUtil.color(line));
        }

        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    private static void appendRequirementLines(List<String> lore, Player player, RankManager manager,
                                               FileConfiguration config, String pathRank) {
        UUID uuid = player.getUniqueId();
        // Separador configurable — admite texto, color codes y emojis
        String sep = config.getString("settings.menu.requirement_separator", " &8| ");

        if (config.contains(pathRank + ".requirements.money")) {
            double req   = config.getDouble(pathRank + ".requirements.money");
            double bal   = manager.getEconomy() != null ? manager.getEconomy().getBalance(player) : 0.0;
            String color = bal >= req ? "&a" : "&c";
            lore.add(ChatUtil.color(sep + "&7Dinero: " + color + "$" + formatNumber(req)));
        }

        if (config.contains(pathRank + ".requirements.playtime_hours")) {
            int req      = config.getInt(pathRank + ".requirements.playtime_hours");
            int current  = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000;
            String color = current >= req ? "&a" : "&c";
            lore.add(ChatUtil.color(sep + "&7Horas: " + color + current + "&8/&" + color.charAt(1) + req + "h"));
        }

        if (config.contains(pathRank + ".requirements.player_kills")) {
            int req      = config.getInt(pathRank + ".requirements.player_kills");
            int current  = manager.getProgress(uuid, "general", "player_kills");
            String color = current >= req ? "&a" : "&c";
            lore.add(ChatUtil.color(sep + "&7Kills PvP: " + color + current + "&8/" + color + req));
        }

        if (config.getConfigurationSection(pathRank + ".requirements.blocks_mine") != null) {
            for (String block : config.getConfigurationSection(pathRank + ".requirements.blocks_mine").getKeys(false)) {
                int req      = config.getInt(pathRank + ".requirements.blocks_mine." + block);
                int current  = manager.getProgress(uuid, "blocks_mine", block);
                String color = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color(sep + "&7Picar " + formatName(block) + ": " + color + current + "&8/" + color + req));
            }
        }

        if (config.getConfigurationSection(pathRank + ".requirements.blocks_place") != null) {
            for (String block : config.getConfigurationSection(pathRank + ".requirements.blocks_place").getKeys(false)) {
                int req      = config.getInt(pathRank + ".requirements.blocks_place." + block);
                int current  = manager.getProgress(uuid, "blocks_place", block);
                String color = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color(sep + "&7Colocar " + formatName(block) + ": " + color + current + "&8/" + color + req));
            }
        }

        if (config.getConfigurationSection(pathRank + ".requirements.mob_kills") != null) {
            for (String mob : config.getConfigurationSection(pathRank + ".requirements.mob_kills").getKeys(false)) {
                int req      = config.getInt(pathRank + ".requirements.mob_kills." + mob);
                int current  = manager.getProgress(uuid, "mob_kills", mob);
                String color = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color(sep + "&7Matar " + formatName(mob) + ": " + color + current + "&8/" + color + req));
            }
        }
    }

    private static ItemStack buildTopItem(RankManager manager, FileConfiguration config) {
        String pathMenu = "settings.menu.items.top_button.";
        Material mat    = Material.matchMaterial(config.getString(pathMenu + "material", "BELL"));

        ItemStack item = new ItemStack(mat != null ? mat : Material.BELL);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatUtil.color(config.getString(pathMenu + "name", "&6&lTop Rangos")));

        List<String> finalLore = new ArrayList<>();

        for (String line : config.getStringList(pathMenu + "lore_header")) {
            finalLore.add(ChatUtil.color(line));
        }

        String format = config.getString(pathMenu + "lore_format", " &8| &e#%pos% &f%player% &8(&7Rango %rank%&8)");

        int pos = 1;
        for (Map.Entry<UUID, Integer> entry : manager.getTopRanks()) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Desconocido";
            finalLore.add(ChatUtil.color(
                format.replace("%pos%", String.valueOf(pos))
                      .replace("%player%", name)
                      .replace("%rank%", String.valueOf(entry.getValue()))
            ));
            pos++;
        }

        for (String line : config.getStringList(pathMenu + "lore_footer")) {
            finalLore.add(ChatUtil.color(line));
        }

        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Convierte un ID interno a nombre legible.
     * DIAMOND_ORE → Diamond Ore | ZOMBIE_PIGLIN → Zombie Piglin
     */
    private static String formatName(String id) {
        String[] words = id.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String formatNumber(double value) {
        if (value >= 1_000_000_000) return DF.format(value / 1_000_000_000) + "b";
        if (value >= 1_000_000)     return DF.format(value / 1_000_000)     + "m";
        if (value >= 1_000)         return DF.format(value / 1_000)         + "k";
        return DF.format(value);
    }
}