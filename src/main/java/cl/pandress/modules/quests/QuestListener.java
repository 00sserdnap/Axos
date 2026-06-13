package cl.pandress.modules.quests;

import cl.pandress.Axos;
import cl.pandress.modules.quests.menus.QuestMenu;
import cl.pandress.utils.ChatUtil;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class QuestListener implements Listener {

    private final Axos plugin = Axos.getInstance();
    private static final String PLACED_BLOCK_META = "quest_placed_block";

    private boolean isModuleEnabled() {
        QuestManager manager = plugin.getQuestManager();
        if (manager == null || manager.getConfig() == null) return false;
        return manager.getConfig().getBoolean("settings.enabled", true);
    }

    private void checkAndNotify(Player player, QuestManager manager, int level, int added) {
        String questKey = manager.getActiveQuestKey(level);
        if (questKey == null) return;

        int required = manager.getConfig().getInt("quest-pool." + questKey + ".action-amount");
        int current  = manager.getProgress(player.getUniqueId());

        if (current >= required && (current - added) < required) {
            player.sendTitle(
                ChatUtil.color("&e&lOBJETIVO COMPLETADO"),
                ChatUtil.color("&fUsa &b/misiones &fpara reclamar"),
                10, 40, 10
            );
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            player.sendMessage(ChatUtil.color("&b&lMISIONES &8» &fHas terminado el objetivo. ¡Reclama tu premio en el menú!"));
        }
    }

    // ── Fly temporal ──────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        QuestManager manager = plugin.getQuestManager();
        if (manager == null) return;

        // Actualizar caché de nombres para el top global
        QuestMenu.cachePlayerName(player.getUniqueId(), player.getName());

        long expiry = manager.getFlyExpiry(player.getUniqueId());
        if (expiry > System.currentTimeMillis()) {
            List<String> disabledWorlds = plugin.getConfig().getStringList("tempfly.disabled-worlds");
            if (disabledWorlds.contains(player.getWorld().getName())) {
                player.sendMessage(ChatUtil.color("&eTu Fly temporal sigue activo, pero está &cdesactivado &een este mundo."));
            } else {
                player.setAllowFlight(true);
                player.sendMessage(ChatUtil.color("&aTu Fly temporal sigue activo."));
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        QuestManager manager = plugin.getQuestManager();
        if (manager == null) return;

        long expiry = manager.getFlyExpiry(player.getUniqueId());
        if (expiry > System.currentTimeMillis()) {
            List<String> disabledWorlds = plugin.getConfig().getStringList("tempfly.disabled-worlds");
            if (disabledWorlds.contains(player.getWorld().getName())) {
                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(ChatUtil.color("&cEl Fly temporal está desactivado en este mundo."));
                }
            } else {
                player.setAllowFlight(true);
                player.sendMessage(ChatUtil.color("&aFly temporal reactivado."));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        QuestManager manager = plugin.getQuestManager();
        if (manager != null) manager.saveUserDataNow(event.getPlayer().getUniqueId());
    }

    // ── Eventos de misiones ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isModuleEnabled()) return;
        event.getBlock().setMetadata(PLACED_BLOCK_META, new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isModuleEnabled()) return;

        Block block = event.getBlock();
        boolean isPlaced = block.hasMetadata(PLACED_BLOCK_META);

        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() != ageable.getMaximumAge()) return;
            isPlaced = false;
        }

        // Anti-abuso: ignorar bloques colocados por jugadores
        if (isPlaced) return;

        Player player = event.getPlayer();
        QuestManager manager = plugin.getQuestManager();
        if (manager == null) return;

        int level = manager.getPlayerDailyLevel(player.getUniqueId());
        if (level > 10) return;

        String questKey = manager.getActiveQuestKey(level);
        if (questKey == null) return;

        String path       = "quest-pool." + questKey;
        String actionType = manager.getConfig().getString(path + ".action-type");

        if ("MINE".equalsIgnoreCase(actionType) || "BREAK".equalsIgnoreCase(actionType)) {
            boolean match = false;

            if (manager.getConfig().isList(path + ".action-target")) {
                for (String t : manager.getConfig().getStringList(path + ".action-target")) {
                    if (block.getType().name().contains(t.toUpperCase())) { match = true; break; }
                }
            } else {
                String targetStr = manager.getConfig().getString(path + ".action-target");
                if (targetStr != null && block.getType().name().contains(targetStr.toUpperCase())) match = true;
            }

            if (match) {
                manager.addProgress(player.getUniqueId(), 1);
                checkAndNotify(player, manager, level, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isModuleEnabled()) return;
        if (event.getEntity().getKiller() == null) return;

        Player player = event.getEntity().getKiller();
        QuestManager manager = plugin.getQuestManager();
        if (manager == null) return;

        int level = manager.getPlayerDailyLevel(player.getUniqueId());
        if (level > 10) return;

        String questKey = manager.getActiveQuestKey(level);
        if (questKey == null) return;

        String path = "quest-pool." + questKey;
        if ("KILL".equalsIgnoreCase(manager.getConfig().getString(path + ".action-type"))) {
            String target = manager.getConfig().getString(path + ".action-target");
            if (target != null && event.getEntity().getType().name().equalsIgnoreCase(target)) {
                manager.addProgress(player.getUniqueId(), 1);
                checkAndNotify(player, manager, level, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!isModuleEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        QuestManager manager = plugin.getQuestManager();
        if (manager == null) return;

        int level = manager.getPlayerDailyLevel(player.getUniqueId());
        if (level > 10) return;

        String questKey = manager.getActiveQuestKey(level);
        if (questKey == null) return;

        String path = "quest-pool." + questKey;
        if ("PICKUP".equalsIgnoreCase(manager.getConfig().getString(path + ".action-type"))) {
            String target = manager.getConfig().getString(path + ".action-target");
            if (target != null && event.getItem().getItemStack().getType().name().equalsIgnoreCase(target)) {
                int amount = event.getItem().getItemStack().getAmount();
                manager.addProgress(player.getUniqueId(), amount);
                checkAndNotify(player, manager, level, amount);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!isModuleEnabled()) return;
        if (!(event.getBreeder() instanceof Player player)) return;

        QuestManager manager = plugin.getQuestManager();
        if (manager == null) return;

        int level = manager.getPlayerDailyLevel(player.getUniqueId());
        if (level > 10) return;

        String questKey = manager.getActiveQuestKey(level);
        if (questKey == null) return;

        String path = "quest-pool." + questKey;
        if ("BREED".equalsIgnoreCase(manager.getConfig().getString(path + ".action-type"))) {
            String target = manager.getConfig().getString(path + ".action-target");
            if (target != null && event.getEntity().getType().name().equalsIgnoreCase(target)) {
                manager.addProgress(player.getUniqueId(), 1);
                checkAndNotify(player, manager, level, 1);
            }
        }
    }
}