package cl.pandress.modules.quests.menus;

import cl.pandress.Axos;
import cl.pandress.modules.quests.QuestManager;
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

public class QuestMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Misiones Diarias")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();
        QuestManager manager = Axos.getInstance().getQuestManager();

        // Slot 12 — Misión actual o Bonus
        if (slot == 12) {
            int level = manager.getPlayerDailyLevel(player.getUniqueId());

            if (level >= 12) return;

            if (level == 11) {
                manager.claimDailyBonus(player);
                player.closeInventory();
                return;
            }

            String questKey = manager.getActiveQuestKey(level);
            if (questKey == null) return;

            int currentProgress = manager.getProgress(player.getUniqueId());
            int requiredAmount  = manager.getConfig().getInt("quest-pool." + questKey + ".action-amount");

            if (currentProgress >= requiredAmount) {
                manager.completeQuest(player, level);
                player.closeInventory();
            } else {
                player.sendMessage(ChatUtil.color("&c¡Aún no has completado el objetivo!"));
            }
        }
        // Slot 14 — Top con paginación
        else if (slot == 14 && clickedItem.getType() == Material.BELL) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasLore()) return;

            List<String> lore = meta.getLore();
            int currentPage = 1;

            for (String line : lore) {
                String uncolored = ChatColor.stripColor(line);
                if (uncolored.startsWith("PAGE:")) {
                    try { currentPage = Integer.parseInt(uncolored.replace("PAGE:", "")); }
                    catch (NumberFormatException ignored) {}
                    break;
                }
            }

            int newPage = event.isLeftClick() ? currentPage + 1
                        : event.isRightClick() ? currentPage - 1
                        : currentPage;

            if (newPage != currentPage) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1f);
                QuestMenu.open(player, newPage);
            }
        }
    }
}