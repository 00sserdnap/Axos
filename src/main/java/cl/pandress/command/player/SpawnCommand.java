package cl.pandress.command.player;

import cl.pandress.Axos;
import cl.pandress.essentials.spawn.SpawnManager;
import cl.pandress.utils.ChatUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;
    private final Set<UUID> teleportingPlayers = new HashSet<>();

    public SpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color(spawnManager.getMessage("not-player")));
            return true;
        }

        if (teleportingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatUtil.color(spawnManager.getMessage("already-teleporting")));
            return true;
        }

        Location spawnLoc = spawnManager.getSpawn();
        if (spawnLoc == null) {
            player.sendMessage(ChatUtil.color(spawnManager.getMessage("spawn-not-set")));
            return true;
        }

        if (spawnManager.isOnCooldown(player)) {
            String time = String.valueOf(spawnManager.getRemainingCooldown(player));
            player.sendMessage(ChatUtil.color(spawnManager.getMessage("cooldown-active").replace("%time%", time)));
            return true;
        }

        int delay = spawnManager.getDelayForPlayer(player);

        if (delay <= 0) {
            player.teleport(spawnLoc);
            player.sendMessage(ChatUtil.color(spawnManager.getMessage("teleport-success")));
            player.sendActionBar(ChatUtil.formatComponent(spawnManager.getMessage("teleport-success-actionbar")));
            spawnManager.playSound(player, "teleport-success"); // SONIDO DE ÉXITO
            spawnManager.setCooldown(player);
            return true;
        }

        player.sendMessage(ChatUtil.color(spawnManager.getMessage("teleport-start")));
        spawnManager.playSound(player, "teleport-start"); // SONIDO DE INICIO
        Location startLoc = player.getLocation();
        teleportingPlayers.add(player.getUniqueId());

        new BukkitRunnable() {
            int timeLeft = delay;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    teleportingPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (startLoc.distanceSquared(player.getLocation()) > 0.5) {
                    player.sendMessage(ChatUtil.color(spawnManager.getMessage("teleport-cancel")));
                    player.sendActionBar(ChatUtil.formatComponent(spawnManager.getMessage("teleport-cancel-actionbar")));
                    spawnManager.playSound(player, "teleport-cancel"); // SONIDO CANCELADO
                    teleportingPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    player.teleport(spawnLoc);
                    player.sendMessage(ChatUtil.color(spawnManager.getMessage("teleport-success")));
                    player.sendActionBar(ChatUtil.formatComponent(spawnManager.getMessage("teleport-success-actionbar")));
                    spawnManager.playSound(player, "teleport-success"); // SONIDO DE ÉXITO
                    spawnManager.setCooldown(player);
                    teleportingPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                String actionBarMsg = spawnManager.getMessage("teleport-actionbar").replace("%time%", String.valueOf(timeLeft));
                player.sendActionBar(ChatUtil.formatComponent(actionBarMsg));
                spawnManager.playSound(player, "teleport-tick"); // SONIDO DE CADA SEGUNDO
                
                timeLeft--;
            }
        }.runTaskTimer(Axos.getInstance(), 0L, 20L);

        return true;
    }
}