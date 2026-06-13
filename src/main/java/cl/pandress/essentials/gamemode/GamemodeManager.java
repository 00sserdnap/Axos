package cl.pandress.essentials.gamemode;

import cl.pandress.utils.WebhookUtil;
import cl.pandress.utils.YamlFile;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeManager {

    private final YamlFile configFile;

    public GamemodeManager() {
        this.configFile = new YamlFile("essentials/gamemode/config.yml");
    }

    public void setGameMode(Player target, GameMode mode, CommandSender actor) {
        target.setGameMode(mode);
        
        // Log al Webhook
        if (configFile.getConfig().getBoolean("webhook.enabled")) {
            String url = configFile.getConfig().getString("webhook.url");
            String username = configFile.getConfig().getString("webhook.username", "Axos Logs");
            
            String content = String.format("{\"username\": \"%s\", \"content\": \"**Log Gamemode:** %s cambió el modo de %s a %s\"}", 
                             username, actor.getName(), target.getName(), mode.name());
            
            WebhookUtil.send(url, content);
        }
    }
}