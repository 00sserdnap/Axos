package cl.pandress.modules.rankup.menus;

import cl.pandress.modules.rankup.RankManager;
import cl.pandress.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RankMenuListener implements Listener {

    private final RankManager manager;

    public RankMenuListener(RankManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        FileConfiguration cfg  = manager.getConfig();   // config.yml
        FileConfiguration rnks = manager.getRanks();    // ranks.yml

        String expectedTitle = ChatUtil.color(cfg.getString("menu.title", "&8Menu | RankUp"));
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        // — Slot 11: abrir preview —
        int previewSlot = cfg.getInt("menu.items.preview_button.slot", 11);
        if (slot == previewSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1f);
            RankPreviewMenu.open(player, 1);
            return;
        }

        // — Slot 13: rankup —
        int rankupSlot = cfg.getInt("menu.items.rankup_button.slot", 13);
        if (slot != rankupSlot) return;

        int nextRank = manager.getPlayerRank(player.getUniqueId()) + 1;
        String path  = "ranks." + nextRank;

        if (rnks.getConfigurationSection(path) == null) return;

        // ── Verificar todos los requisitos ────────────────────────────
        boolean canRankup = true;

        if (rnks.contains(path + ".requirements.money")) {
            double required = rnks.getDouble(path + ".requirements.money");
            if (manager.getEconomy() == null || manager.getEconomy().getBalance(player) < required)
                canRankup = false;
        }
        if (rnks.contains(path + ".requirements.playtime_hours")) {
            int currentHours = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000;
            if (currentHours < rnks.getInt(path + ".requirements.playtime_hours")) canRankup = false;
        }
        if (rnks.contains(path + ".requirements.player_kills")) {
            int current  = manager.getProgress(player.getUniqueId(), "general", "player_kills");
            if (current < rnks.getInt(path + ".requirements.player_kills")) canRankup = false;
        }
        if (rnks.getConfigurationSection(path + ".requirements.blocks_mine") != null) {
            for (String block : rnks.getConfigurationSection(path + ".requirements.blocks_mine").getKeys(false)) {
                if (manager.getProgress(player.getUniqueId(), "blocks_mine", block)
                        < rnks.getInt(path + ".requirements.blocks_mine." + block)) {
                    canRankup = false; break;
                }
            }
        }
        if (rnks.getConfigurationSection(path + ".requirements.blocks_place") != null) {
            for (String block : rnks.getConfigurationSection(path + ".requirements.blocks_place").getKeys(false)) {
                if (manager.getProgress(player.getUniqueId(), "blocks_place", block)
                        < rnks.getInt(path + ".requirements.blocks_place." + block)) {
                    canRankup = false; break;
                }
            }
        }
        if (rnks.getConfigurationSection(path + ".requirements.mob_kills") != null) {
            for (String mob : rnks.getConfigurationSection(path + ".requirements.mob_kills").getKeys(false)) {
                if (manager.getProgress(player.getUniqueId(), "mob_kills", mob)
                        < rnks.getInt(path + ".requirements.mob_kills." + mob)) {
                    canRankup = false; break;
                }
            }
        }

        if (!canRankup) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatUtil.color(manager.getMessage("rankup-fail")));
            player.closeInventory();
            return;
        }

        // ── Cobrar dinero y subir rango ───────────────────────────────
        if (rnks.contains(path + ".requirements.money")) {
            manager.getEconomy().withdrawPlayer(player, rnks.getDouble(path + ".requirements.money"));
        }

        manager.setPlayerRank(player.getUniqueId(), nextRank);

        for (String cmd : rnks.getStringList(path + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendTitle(
            ChatUtil.color(manager.getMessage("rankup-title")),
            ChatUtil.color(manager.getMessage("rankup-subtitle").replace("%rank%", String.valueOf(nextRank))),
            10, 40, 10
        );
        player.sendMessage(ChatUtil.color(manager.getMessage("rankup-success-chat").replace("%rank%", String.valueOf(nextRank))));

        RankMenu.open(player);
    }
}