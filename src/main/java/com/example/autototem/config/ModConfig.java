package com.example.autototem.config;

import com.example.autototem.AutoTotemMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "auto-totem.json"
    );

    // Fields persistent
    public boolean globalEnabled = true;  // Tombol 1: Global mod enable (persistent)
    public boolean keybindEnabled = true;  // Tombol 2: Keybind toggle enable (persistent)

    // Static instance lazy-loaded
    private static ModConfig INSTANCE;

    // EMPTY CONSTRUCTOR untuk Gson (no load() di sini!)
    public ModConfig() {
        // Kosong - hindari recursion
    }

    // Static load method (panggil dari mod init)
    public static ModConfig load() {
        if (INSTANCE != null) return INSTANCE;

        INSTANCE = new ModConfig();  // Buat empty instance dulu

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    INSTANCE.globalEnabled = loaded.globalEnabled;
                    INSTANCE.keybindEnabled = loaded.keybindEnabled;
                }
            } catch (IOException e) {
                AutoTotemMod.LOGGER.error("Failed to load config: {}", e.getMessage());
            }
        } else {
            save();  // Buat default file
        }
        return INSTANCE;
    }

    public static void save() {
        if (INSTANCE == null) return;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            AutoTotemMod.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    // Getters/Setters untuk global
    public static boolean isGlobalEnabled() {
        return load().globalEnabled;
    }

    public static void setGlobalEnabled(boolean value) {
        load().globalEnabled = value;
        save();
    }

    public static void toggleGlobalEnabled() {
        setGlobalEnabled(!isGlobalEnabled());
    }

    // Getters/Setters untuk keybind
    public static boolean isKeybindEnabled() {
        return load().keybindEnabled;
    }

    public static void setKeybindEnabled(boolean value) {
        load().keybindEnabled = value;
        save();
    }

    public static void toggleKeybindEnabled() {
        setKeybindEnabled(!isKeybindEnabled());
    }

    public static ModConfig getInstance() {
        return load();
    }
}