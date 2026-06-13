package cl.pandress.command.admin;

import cl.pandress.essentials.spawn.SpawnManager;
import cl.pandress.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;

    public SetSpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color(spawnManager.getMessage("not-player")));
            return true;
        }

        if (!player.hasPermission("axos.admin.setspawn")) {
            player.sendMessage(ChatUtil.color(spawnManager.getMessage("no-permission")));
            return true;
        }

        spawnManager.setSpawn(player.getLocation());
        player.sendMessage(ChatUtil.color(spawnManager.getMessage("set-success")));
        return true;
    }
}