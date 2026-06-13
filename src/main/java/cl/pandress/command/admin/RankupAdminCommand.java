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

    public RankupAdminCommand(RankManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axos.admin.rankup")) {
            sender.sendMessage(ChatUtil.color(manager.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatUtil.color("&8[&e!&8] &eUso: &f/rankupadmin set <jugador> <rango>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatUtil.color(manager.getMessage("admin-player-not-found").replace("%player%", args[1])));
                    return true;
                }
                int rank;
                try {
                    rank = Integer.parseInt(args[2]);
                    if (rank < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatUtil.color(manager.getMessage("admin-invalid-rank")));
                    return true;
                }
                manager.setPlayerRank(target.getUniqueId(), rank);
                sender.sendMessage(ChatUtil.color(
                    manager.getMessage("admin-set-success")
                        .replace("%player%", target.getName())
                        .replace("%rank%", String.valueOf(rank))
                ));
                if (target.isOnline()) {
                    ((Player) target.getPlayer()).sendMessage(ChatUtil.color(
                        manager.getMessage("admin-set-notify").replace("%rank%", String.valueOf(rank))
                    ));
                }
            }

            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatUtil.color("&8[&e!&8] &eUso: &f/rankupadmin reset <jugador|all>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    manager.resetAllRanks();
                    sender.sendMessage(ChatUtil.color(manager.getMessage("admin-reset-all")));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatUtil.color(manager.getMessage("admin-player-not-found").replace("%player%", args[1])));
                    return true;
                }
                manager.resetPlayerRank(target.getUniqueId());
                sender.sendMessage(ChatUtil.color(
                    manager.getMessage("admin-reset-success").replace("%player%", target.getName())
                ));
                if (target.isOnline()) {
                    ((Player) target.getPlayer()).sendMessage(ChatUtil.color(manager.getMessage("admin-reset-notify")));
                }
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.color("&8&m-----------------------------"));
        sender.sendMessage(ChatUtil.color("&6&lRankup Admin &8| &fComandos"));
        sender.sendMessage(ChatUtil.color(" &e/rankupadmin set <jugador> <rango> &8- &fEstablece el rango"));
        sender.sendMessage(ChatUtil.color(" &e/rankupadmin reset <jugador|all> &8- &fResetea el rango"));
        sender.sendMessage(ChatUtil.color("&8&m-----------------------------"));
    }
}