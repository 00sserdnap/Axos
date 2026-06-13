package cl.pandress.essentials.spawn;

import cl.pandress.utils.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnManager {

    private final YamlFile configFile;
    private final YamlFile messagesFile;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SpawnManager() {
        this.configFile = new YamlFile("essentials/spawn/config.yml");
        this.messagesFile = new YamlFile("essentials/spawn/messages.yml");
    }

    public void setSpawn(Location loc) {
        configFile.getConfig().set("location.world", loc.getWorld().getName());
        configFile.getConfig().set("location.x", loc.getX());
        configFile.getConfig().set("location.y", loc.getY());
        configFile.getConfig().set("location.z", loc.getZ());
        configFile.getConfig().set("location.yaw", loc.getYaw());
        configFile.getConfig().set("location.pitch", loc.getPitch());
        configFile.save();
    }

    public Location getSpawn() {
        if (!configFile.getConfig().contains("location.world")) return null;

        World world = Bukkit.getWorld(configFile.getConfig().getString("location.world"));
        if (world == null) return null;

        double x = configFile.getConfig().getDouble("location.x");
        double y = configFile.getConfig().getDouble("location.y");
        double z = configFile.getConfig().getDouble("location.z");
        float yaw = (float) configFile.getConfig().getDouble("location.yaw");
        float pitch = (float) configFile.getConfig().getDouble("location.pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public int getDelayForPlayer(Player player) {
        if (player.hasPermission("axos.spawn.delay.bypass")) return 0;

        int lowestDelay = configFile.getConfig().getInt("settings.default-delay", 5);
        ConfigurationSection perms = configFile.getConfig().getConfigurationSection("delay-permissions");
        
        if (perms != null) {
            for (String perm : perms.getKeys(false)) {
                if (player.hasPermission(perm)) {
                    int delay = perms.getInt(perm);
                    if (delay < lowestDelay) {
                        lowestDelay = delay;
                    }
                }
            }
        }
        return lowestDelay;
    }

    public int getCooldownForPlayer(Player player) {
        if (player.hasPermission("axos.spawn.cooldown.bypass")) return 0;

        int lowestCooldown = configFile.getConfig().getInt("settings.default-cooldown", 30);
        ConfigurationSection perms = configFile.getConfig().getConfigurationSection("cooldown-permissions");
        
        if (perms != null) {
            for (String perm : perms.getKeys(false)) {
                if (player.hasPermission(perm)) {
                    int cooldown = perms.getInt(perm);
                    if (cooldown < lowestCooldown) {
                        lowestCooldown = cooldown;
                    }
                }
            }
        }
        return lowestCooldown;
    }

    public boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;
        long timePassed = (System.currentTimeMillis() - cooldowns.get(player.getUniqueId())) / 1000;
        return timePassed < getCooldownForPlayer(player);
    }

    public long getRemainingCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return 0;
        long timePassed = (System.currentTimeMillis() - cooldowns.get(player.getUniqueId())) / 1000;
        return getCooldownForPlayer(player) - timePassed;
    }

    public void setCooldown(Player player) {
        if (getCooldownForPlayer(player) > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public boolean isDeathSpawnEnabled() {
        return configFile.getConfig().getBoolean("settings.death-spawn", false);
    }

    public String getMessage(String path) {
        return messagesFile.getConfig().getString("messages." + path, "&cError: Mensaje " + path + " falta.");
    }

    // --- NUEVO SISTEMA DE SONIDOS ---
    public void playSound(Player player, String path) {
        if (!configFile.getConfig().getBoolean("sounds." + path + ".enabled", false)) return;

        String soundName = configFile.getConfig().getString("sounds." + path + ".sound");
        if (soundName == null) return;

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) configFile.getConfig().getDouble("sounds." + path + ".volume", 1.0);
            float pitch = (float) configFile.getConfig().getDouble("sounds." + path + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Axos] El sonido '" + soundName + "' en config.yml no existe en la version 1.21.");
        }
    }
}