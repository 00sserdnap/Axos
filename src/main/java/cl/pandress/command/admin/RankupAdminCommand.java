package cl.pandress.command.admin;

import cl.pandress.modules.rankup.RankManager;
import cl.pandress.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankupAdminCommand implements CommandExecutor {

    private final RankManager manager;

    private static final String PREFIX = "&8[&6Rankup&8] &f";

    public RankupAdminCommand(RankManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axos.admin.rankup")) {
            sender.sendMessage(ChatUtil.color(PREFIX + "&cNo tienes permisos para usar este comando."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /rankupadmin set <jugador> <rango>
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.color(PREFIX + "&eUso: &f/rankupadmin set <jugador> <rango>"));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatUtil.color(PREFIX + "&cJugador &e" + args[1] + " &cno encontrado."));
                    return true;
                }

                int rank;
                try {
                    rank = Integer.parseInt(args[2]);
                    if (rank < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatUtil.color(PREFIX + "&cEl rango debe ser un número positivo."));
                    return true;
                }

                manager.setPlayerRank(target.getUniqueId(), rank);
                sender.sendMessage(ChatUtil.color(PREFIX + "Rango de &e" + target.getName() + " &festablecido a &e#" + rank + "&f."));

                if (target.isOnline()) {
                    ((Player) target.getPlayer()).sendMessage(ChatUtil.color(PREFIX + "Un administrador ha establecido tu rango a &e#" + rank + "&f."));
                }
            }

            // /rankupadmin reset <jugador>
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatUtil.color(PREFIX + "&eUso: &f/rankupadmin reset <jugador|all>"));
                    return true;
                }

                if (args[1].equalsIgnoreCase("all")) {
                    manager.resetAllRanks();
                    sender.sendMessage(ChatUtil.color(PREFIX + "&cTodos los rangos han sido reseteados."));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatUtil.color(PREFIX + "&cJugador &e" + args[1] + " &cno encontrado."));
                    return true;
                }

                manager.resetPlayerRank(target.getUniqueId());
                sender.sendMessage(ChatUtil.color(PREFIX + "Rango de &e" + target.getName() + " &freseteado."));

                if (target.isOnline()) {
                    ((Player) target.getPlayer()).sendMessage(ChatUtil.color(PREFIX + "Un administrador ha reseteado tu rango."));
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.color("&8&m-----------------------------"));
        sender.sendMessage(ChatUtil.color("&6&lRankup Admin &8| &fComandos"));
        sender.sendMessage(ChatUtil.color(" &e/rankupadmin set <jugador> <rango> &8- &fEstablece el rango de un jugador"));
        sender.sendMessage(ChatUtil.color(" &e/rankupadmin reset <jugador|all> &8- &fResetea el rango de un jugador o todos"));
        sender.sendMessage(ChatUtil.color("&8&m-----------------------------"));
    }
}