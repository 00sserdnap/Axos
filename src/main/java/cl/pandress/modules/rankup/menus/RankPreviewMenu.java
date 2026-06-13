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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class RankPreviewMenu {

    private static final DecimalFormat DF = new DecimalFormat("#.#");

    // El título incluye el número de página para que el listener pueda detectarlo
    public static final String TITLE_PREFIX = "§8Vista de Rangos §7» ";

    // Slots donde aparecen los rangos dentro de un inventario 54
    // Evitando las columnas 0 y 8 (bordes)
    private static final int[] RANK_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public static void open(Player player, int page) {
        RankManager manager = Axos.getInstance().getRankManager();
        FileConfiguration rnks = manager.getRanks();  // ranks.yml
        FileConfiguration msgs = manager.getMessages(); // messages.yml

        // Contar rangos totales
        int totalRanks = 0;
        while (rnks.getConfigurationSection("ranks." + (totalRanks + 1)) != null) totalRanks++;

        int ranksPerPage = RANK_SLOTS.length;
        int maxPages     = Math.max(1, (int) Math.ceil((double) totalRanks / ranksPerPage));
        page = Math.max(1, Math.min(page, maxPages));

        int playerRank = manager.getPlayerRank(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(
            (InventoryHolder) null, 54,
            TITLE_PREFIX + page + "§7/§8" + maxPages
        );

        // — Bordes —
        ItemStack border = makeBorder();
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // — Rangos de esta página —
        int startRank = (page - 1) * ranksPerPage + 1;
        for (int i = 0; i < ranksPerPage; i++) {
            int rankNum = startRank + i;
            if (rnks.getConfigurationSection("ranks." + rankNum) == null) break;
            inv.setItem(RANK_SLOTS[i], buildRankItem(player, rankNum, playerRank, manager, rnks, msgs));
        }

        // — Navegación —
        if (page > 1) {
            inv.setItem(45, makeNavButton(
                ChatUtil.color(msgs.getString("messages.preview-prev-page", "&e← Página anterior")), page - 1));
        }
        if (page < maxPages) {
            inv.setItem(53, makeNavButton(
                ChatUtil.color(msgs.getString("messages.preview-next-page", "&ePágina siguiente →")), page + 1));
        }

        // — Volver —
        inv.setItem(49, makeBackButton(
            ChatUtil.color(msgs.getString("messages.preview-back-button", "&c← Volver al menú principal"))));

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
        player.openInventory(inv);
    }

    // ── Construcción del ítem de rango ────────────────────────────────────

    private static ItemStack buildRankItem(Player player, int rankNum, int playerRank,
                                           RankManager manager, FileConfiguration rnks,
                                           FileConfiguration msgs) {
        String path   = "ranks." + rankNum;
        String prefix = rnks.getString(path + ".prefix", "&7[Rango " + rankNum + "]");

        boolean done    = playerRank >= rankNum;
        boolean current = playerRank + 1 == rankNum;

        Material mat;
        String statusLine;
        if (done) {
            mat        = Material.LIME_DYE;
            statusLine = msgs.getString("messages.preview-rank-reached", "&a✔ Rango alcanzado");
        } else if (current) {
            mat        = Material.EMERALD;
            statusLine = msgs.getString("messages.preview-rank-next", "&e▶ Rango siguiente");
        } else {
            mat        = Material.GRAY_DYE;
            statusLine = msgs.getString("messages.preview-rank-locked", "&8✘ Bloqueado");
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatUtil.color("&7[&eRango " + rankNum + "&7] " + prefix));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtil.color("&8" + "─".repeat(24)));
        lore.add(ChatUtil.color(statusLine));
        lore.add("");

        // Requisitos
        lore.add(ChatUtil.color("&7Requisitos:"));
        appendRequirements(lore, player, rankNum, manager, rnks);

        lore.add("");

        // Recompensas
        lore.add(ChatUtil.color("&7Recompensas:"));
        List<String> rewards = rnks.getStringList(path + ".rewards_lore");
        if (rewards.isEmpty()) {
            lore.add(ChatUtil.color("  &8• &f" + msgs.getString("messages.preview-no-rewards", "Ver al subir de rango")));
        } else {
            for (String r : rewards) lore.add(ChatUtil.color("  &8• &f" + r));
        }

        lore.add(ChatUtil.color("&8" + "─".repeat(24)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void appendRequirements(List<String> lore, Player player, int rankNum,
                                           RankManager manager, FileConfiguration rnks) {
        String path = "ranks." + rankNum;

        if (rnks.contains(path + ".requirements.money")) {
            double req   = rnks.getDouble(path + ".requirements.money");
            double bal   = manager.getEconomy() != null ? manager.getEconomy().getBalance(player) : 0;
            String col   = bal >= req ? "&a" : "&c";
            lore.add(ChatUtil.color("  &8• &7Dinero: " + col + "$" + RankMenu.formatNumber(req)));
        }
        if (rnks.contains(path + ".requirements.playtime_hours")) {
            int req     = rnks.getInt(path + ".requirements.playtime_hours");
            int current = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000;
            String col  = current >= req ? "&a" : "&c";
            lore.add(ChatUtil.color("  &8• &7Horas: " + col + current + "&8/" + col + req + "h"));
        }
        if (rnks.contains(path + ".requirements.player_kills")) {
            int req     = rnks.getInt(path + ".requirements.player_kills");
            int current = manager.getProgress(player.getUniqueId(), "general", "player_kills");
            String col  = current >= req ? "&a" : "&c";
            lore.add(ChatUtil.color("  &8• &7Kills PvP: " + col + current + "&8/" + col + req));
        }
        if (rnks.getConfigurationSection(path + ".requirements.blocks_mine") != null) {
            for (String block : rnks.getConfigurationSection(path + ".requirements.blocks_mine").getKeys(false)) {
                int req     = rnks.getInt(path + ".requirements.blocks_mine." + block);
                int current = manager.getProgress(player.getUniqueId(), "blocks_mine", block);
                String col  = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color("  &8• &7Picar " + RankMenu.formatName(block) + ": " + col + current + "&8/" + col + req));
            }
        }
        if (rnks.getConfigurationSection(path + ".requirements.blocks_place") != null) {
            for (String block : rnks.getConfigurationSection(path + ".requirements.blocks_place").getKeys(false)) {
                int req     = rnks.getInt(path + ".requirements.blocks_place." + block);
                int current = manager.getProgress(player.getUniqueId(), "blocks_place", block);
                String col  = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color("  &8• &7Colocar " + RankMenu.formatName(block) + ": " + col + current + "&8/" + col + req));
            }
        }
        if (rnks.getConfigurationSection(path + ".requirements.mob_kills") != null) {
            for (String mob : rnks.getConfigurationSection(path + ".requirements.mob_kills").getKeys(false)) {
                int req     = rnks.getInt(path + ".requirements.mob_kills." + mob);
                int current = manager.getProgress(player.getUniqueId(), "mob_kills", mob);
                String col  = current >= req ? "&a" : "&c";
                lore.add(ChatUtil.color("  &8• &7Matar " + RankMenu.formatName(mob) + ": " + col + current + "&8/" + col + req));
            }
        }
    }

    // ── Helpers de items de navegación ───────────────────────────────────

    private static ItemStack makeBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private static ItemStack makeNavButton(String name, int targetPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add("§8PAGE:" + targetPage);   // tag interno, sin & para que no lo traduzca
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeBackButton(String name) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}