package cl.pandress.utils;

import cl.pandress.Axos;
import org.bukkit.Bukkit;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebhookUtil {

    public static void send(String url, String json) {
        Bukkit.getScheduler().runTaskAsynchronously(Axos.getInstance(), () -> {
            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = json.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                con.getResponseCode(); // Ejecutar
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}