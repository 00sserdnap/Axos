package cl.pandress.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class ChatUtil {
    
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static Component formatComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}