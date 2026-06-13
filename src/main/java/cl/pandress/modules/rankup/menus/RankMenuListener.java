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
        FileConfiguration config = manager.getConfig();

        String expectedTitle = ChatUtil.color(config.getString("settings.menu.title", "&8Menu | RankUp"));
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rankupSlot = config.getInt("settings.menu.items.rankup_button.slot", 12);
        if (event.getRawSlot() != rankupSlot) return;

        int nextRank = manager.getPlayerRank(player.getUniqueId()) + 1;
        String path  = "ranks." + nextRank;

        if (config.getConfigurationSection(path) == null) return;

        boolean canRankup = true;

        if (config.contains(path + ".requirements.money")) {
            double required = config.getDouble(path + ".requirements.money");
            if (manager.getEconomy() == null || manager.getEconomy().getBalance(player) < required) {
                canRankup = false;
            }
        }

        if (config.contains(path + ".requirements.playtime_hours")) {
            int currentHours = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000;
            if (currentHours < config.getInt(path + ".requirements.playtime_hours")) canRankup = false;
        }

        if (config.contains(path + ".requirements.player_kills")) {
            int current  = manager.getProgress(player.getUniqueId(), "general", "player_kills");
            int required = config.getInt(path + ".requirements.player_kills");
            if (current < required) canRankup = false;
        }

        if (config.getConfigurationSection(path + ".requirements.blocks_mine") != null) {
            for (String block : config.getConfigurationSection(path + ".requirements.blocks_mine").getKeys(false)) {
                int current  = manager.getProgress(player.getUniqueId(), "blocks_mine", block);
                int required = config.getInt(path + ".requirements.blocks_mine." + block);
                if (current < required) { canRankup = false; break; }
            }
        }

        if (config.getConfigurationSection(path + ".requirements.blocks_place") != null) {
            for (String block : config.getConfigurationSection(path + ".requirements.blocks_place").getKeys(false)) {
                int current  = manager.getProgress(player.getUniqueId(), "blocks_place", block);
                int required = config.getInt(path + ".requirements.blocks_place." + block);
                if (current < required) { canRankup = false; break; }
            }
        }

        if (config.getConfigurationSection(path + ".requirements.mob_kills") != null) {
            for (String mob : config.getConfigurationSection(path + ".requirements.mob_kills").getKeys(false)) {
                int current  = manager.getProgress(player.getUniqueId(), "mob_kills", mob);
                int required = config.getInt(path + ".requirements.mob_kills." + mob);
                if (current < required) { canRankup = false; break; }
            }
        }

        if (!canRankup) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatUtil.color("&c&lERROR &8» &7Aún no cumples con todos los requisitos."));
            player.closeInventory();
            return;
        }

        if (config.contains(path + ".requirements.money")) {
            manager.getEconomy().withdrawPlayer(player, config.getDouble(path + ".requirements.money"));
        }

        manager.setPlayerRank(player.getUniqueId(), nextRank);

        for (String cmd : config.getStringList(path + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendTitle(
            ChatUtil.color("&a&lRANK UP!"),
            ChatUtil.color("&fAhora eres Rango #" + nextRank),
            10, 40, 10
        );

        RankMenu.open(player);
    }
}