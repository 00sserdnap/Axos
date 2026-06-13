package cl.pandress.essentials.spawn;

import cl.pandress.Axos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathSpawnListener implements Listener {

    private final SpawnManager spawnManager;

    public DeathSpawnListener(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!spawnManager.isDeathSpawnEnabled()) return;

        final Location spawn = spawnManager.getSpawn();
        if (spawn == null) return;

        event.setRespawnLocation(spawn);

        Bukkit.getScheduler().runTaskLater(Axos.getInstance(), () -> {
            if (event.getPlayer().isOnline()) {
                event.getPlayer().teleport(spawn);
            }
        }, 5L);
    }
}