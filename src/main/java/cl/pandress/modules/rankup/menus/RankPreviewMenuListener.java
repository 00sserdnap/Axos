package cl.pandress.modules.rankup.menus;

import cl.pandress.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RankPreviewMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // El título siempre empieza con el prefijo del menú de preview
        if (!title.startsWith(ChatColor.stripColor(ChatUtil.color(RankPreviewMenu.TITLE_PREFIX)))
            && !title.startsWith(RankPreviewMenu.TITLE_PREFIX)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // — Botón volver (slot 49) —
        if (clicked.getType() == Material.BARRIER) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1f);
            RankMenu.open(player);
            return;
        }

        // — Botones de navegación (flechas) —
        if (clicked.getType() == Material.ARROW) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasLore()) return;

            List<String> lore = meta.getLore();
            for (String line : lore) {
                String clean = ChatColor.stripColor(line);
                if (clean.startsWith("PAGE:")) {
                    try {
                        int targetPage = Integer.parseInt(clean.replace("PAGE:", "").trim());
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1f);
                        RankPreviewMenu.open(player, targetPage);
                    } catch (NumberFormatException ignored) {}
                    return;
                }
            }
        }
    }
}