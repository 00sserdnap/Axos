package cl.pandress.modules.rankup.placeholderapi;

import cl.pandress.modules.rankup.RankManager;
import cl.pandress.utils.ChatUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankPlaceholder extends PlaceholderExpansion {

    private final RankManager manager;

    public RankPlaceholder(RankManager manager) {
        this.manager = manager;
    }

    @Override public @NotNull String getIdentifier() { return "axos"; }
    @Override public @NotNull String getAuthor()     { return "pandress"; }
    @Override public @NotNull String getVersion()    { return "1.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // %axos_rank% → número del rango actual
        if (params.equalsIgnoreCase("rank")) {
            return String.valueOf(manager.getPlayerRank(player.getUniqueId()));
        }

        // %axos_rank_prefix% → prefijo definido en config
        if (params.equalsIgnoreCase("rank_prefix")) {
            int rank = manager.getPlayerRank(player.getUniqueId());
            FileConfiguration config = manager.getConfig();

            if (rank == 0) {
                return ChatUtil.color(config.getString("settings.default_prefix", "&7[Usuario]"));
            }
            return ChatUtil.color(config.getString("ranks." + rank + ".prefix", "&7[Rango " + rank + "]"));
        }

        // %axos_top_name_1% → nombre del jugador en posición N del top
        // %axos_top_rank_1% → rango del jugador en posición N del top
        if (params.startsWith("top_")) {
            List<Map.Entry<UUID, Integer>> top = manager.getTopRanks();
            String[] parts = params.split("_");

            if (parts.length < 3) return "---";

            try {
                int index = Integer.parseInt(parts[2]) - 1;
                if (index < 0 || index >= top.size()) return "---";

                if (parts[1].equalsIgnoreCase("name")) {
                    String name = Bukkit.getOfflinePlayer(top.get(index).getKey()).getName();
                    return name != null ? name : "Desconocido";
                }

                if (parts[1].equalsIgnoreCase("rank")) {
                    return String.valueOf(top.get(index).getValue());
                }

            } catch (NumberFormatException e) {
                return "---";
            }
        }

        return null;
    }
}