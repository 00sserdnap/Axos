package cl.pandress.command.player;

import cl.pandress.Axos;
import cl.pandress.modules.rankup.menus.RankMenu;
import cl.pandress.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankupCommand implements CommandExecutor {

    private final cl.pandress.modules.rankup.RankManager rankManager;

    public RankupCommand(cl.pandress.modules.rankup.RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color("&cSolo jugadores pueden usar este comando."));
            return true;
        }

        if (!rankManager.getConfig().getBoolean("settings.enabled", true)) {
            player.sendMessage(ChatUtil.color("&cEl sistema de rangos está desactivado actualmente."));
            return true;
        }

        RankMenu.open(player);
        return true;
    }
}