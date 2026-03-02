package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static Properties props = new Properties();

    static {
        try {
            InputStream input = ConfigLoader.class
                    .getResourceAsStream("/config.properties");
            if (input != null) {
                props.load(input);
                System.out.println("✅ config.properties chargé !");
            } else {
                System.err.println("❌ config.properties introuvable !");
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement config : " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }
}