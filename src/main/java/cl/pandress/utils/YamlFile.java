package cl.pandress.utils;

import cl.pandress.Axos;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlFile {

    private final File file;
    private final YamlConfiguration config;

    public YamlFile(String path) {
        this.file = new File(Axos.getInstance().getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            Axos.getInstance().saveResource(path, false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}