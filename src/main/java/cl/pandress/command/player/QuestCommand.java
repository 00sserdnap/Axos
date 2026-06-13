package cl.pandress.command.player;

import cl.pandress.Axos;
import cl.pandress.modules.quests.menus.QuestMenu;
import cl.pandress.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color("&cSolo jugadores pueden usar este comando."));
            return true;
        }

        if (!Axos.getInstance().getQuestManager().getConfig().getBoolean("settings.enabled", true)) {
            player.sendMessage(ChatUtil.color("&cEl sistema de misiones está desactivado actualmente."));
            return true;
        }

        QuestMenu.open(player);
        return true;
    }
}