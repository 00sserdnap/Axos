package cl.pandress.command.admin;

import cl.pandress.essentials.gamemode.GamemodeManager;
import cl.pandress.utils.ChatUtil;
import cl.pandress.utils.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GamemodeCommand implements CommandExecutor {

    private final YamlFile messagesFile;
    private final GamemodeManager manager;

    public GamemodeCommand(GamemodeManager manager) {
        this.messagesFile = new YamlFile("essentials/gamemode/messages.yml");
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color(getMessage("not-player")));
            return true;
        }

        if (!player.hasPermission("axos.admin.gamemode")) {
            player.sendMessage(ChatUtil.color(getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtil.color(getMessage("gm-usage")));
            return true;
        }

        GameMode gm = getGameMode(args[0]);
        if (gm == null) {
            player.sendMessage(ChatUtil.color(getMessage("gm-invalid").replace("%mode%", args[0])));
            return true;
        }

        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : player;
        if (target == null) {
            player.sendMessage(ChatUtil.color(getMessage("player-not-found")));
            return true;
        }

        // Ejecutar a través del Manager
        manager.setGameMode(target, gm, player);

        String modeName = gm.name().toLowerCase();
        if (target == player) {
            player.sendMessage(ChatUtil.color(getMessage("gm-changed").replace("%mode%", modeName)));
        } else {
            player.sendMessage(ChatUtil.color(getMessage("gm-changed-other")
                    .replace("%target%", target.getName())
                    .replace("%mode%", modeName)));
        }
        return true;
    }

    private GameMode getGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "0", "survival" -> GameMode.SURVIVAL;
            case "1", "creative" -> GameMode.CREATIVE;
            case "2", "adventure" -> GameMode.ADVENTURE;
            case "3", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String getMessage(String path) {
        return messagesFile.getConfig().getString("messages." + path, "&cError: Falta " + path);
    }
}