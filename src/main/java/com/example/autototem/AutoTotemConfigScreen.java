package com.example.autototem;

import com.example.autototem.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.Supplier;  // Udah ada, buat Supplier<Boolean>

public class AutoTotemConfigScreen {
    public static Screen create(Screen parent) {
        // Builder Cloth Config (mirip Unity UI Builder, clean & responsive)
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Auto Use Totem Config"));  // Judul screen

        // Entry builder untuk widgets
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Category utama
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Settings"));  // Kategori "Settings"

        // Tombol 1: Global Enable (master toggle, persistent)
        category.addEntry(
                entryBuilder.startBooleanToggle(Text.literal("Enable Auto Use Totem"), ModConfig.isGlobalEnabled())
                        .setDefaultValue(true)  // Default on
                        .setSaveConsumer(newValue -> {
                            ModConfig.setGlobalEnabled(newValue);  // Save global ke config
                            // UPDATE: Force lock keybind jika global off (static disable via state force)
                            if (!newValue) {
                                ModConfig.setKeybindEnabled(false);
                            }
                            // Force sync in-game state (disable semuanya jika off)
                            AutoTotemMod.inGameEnabled = newValue;
                            AutoTotemMod.LOGGER.info("Global mod toggled to: {}", newValue ? "Active" : "Inactive");
                        })
                        .setTooltip(Text.literal("Enable/disable entire mod."))  // Tooltip hover
                        .build()
        );

        // Tombol 2: Keybind Enable (slave toggle, persistent, static lock via global callback)
        // FIX 1: Ganti ke .setRequirement() - dynamic disable/enable based on global (Cloth API standard)
        // Supplier re-eval tiap render, sync otomatis tanpa manual refresh
        category.addEntry(
                entryBuilder.startBooleanToggle(Text.literal("Enable Toggel Auto Use Totem"), ModConfig.isKeybindEnabled())
                        .setDefaultValue(true)  // Default on
                        .setRequirement(() -> ModConfig.isGlobalEnabled())  // KEY FIX: Disable kalau global off, enable kalau on
                        .setSaveConsumer(newValue -> {
                            // FIX 2: No need .booleanValue() - autounbox Boolean ke boolean otomatis (clean & no error)
                            ModConfig.setKeybindEnabled(newValue);  // Direct pass, Java handles unboxing
                            AutoTotemMod.LOGGER.info("Keybind toggle toggled to: {}", newValue ? "Active" : "Inactive");
                        })
                        .setTooltip(Text.literal("Enable/disable toggle Auto Use Totem (Disabled when Auto Use Totem off)."))  // Tooltip hover (static hint dependency)
                        .build()
        );

        // Bangun screen (mirip Instantiate UI di Unity, no leak)
        builder.setSavingRunnable(ModConfig::save);  // Auto-save saat close
        return builder.build();
    }
}