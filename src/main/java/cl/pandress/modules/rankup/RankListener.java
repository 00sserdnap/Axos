package cl.pandress.modules.rankup;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class RankListener implements Listener {

    private final RankManager manager;

    public RankListener(RankManager manager) {
        this.manager = manager;
    }

    private void checkAndAddProgress(Player player, String category, String type, int amount) {
        FileConfiguration rnks = manager.getRanks();  // ranks.yml
        int nextRank = manager.getPlayerRank(player.getUniqueId()) + 1;
        String path  = "ranks." + nextRank + ".requirements." + category + "." + type;

        if (!rnks.contains(path)) return;

        int required = rnks.getInt(path);
        int current  = manager.getProgress(player.getUniqueId(), category, type);

        if (current < required) {
            manager.addProgress(player.getUniqueId(), category, type, amount);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        checkAndAddProgress(event.getPlayer(), "blocks_mine", event.getBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkAndAddProgress(event.getPlayer(), "blocks_place", event.getBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();

        if (event.getEntity() instanceof Player) {
            FileConfiguration rnks = manager.getRanks();
            int nextRank = manager.getPlayerRank(killer.getUniqueId()) + 1;
            String path  = "ranks." + nextRank + ".requirements.player_kills";
            if (rnks.contains(path)) {
                int required = rnks.getInt(path);
                int current  = manager.getProgress(killer.getUniqueId(), "general", "player_kills");
                if (current < required) manager.addProgress(killer.getUniqueId(), "general", "player_kills", 1);
            }
        } else {
            checkAndAddProgress(killer, "mob_kills", event.getEntity().getType().name(), 1);
        }
    }
}