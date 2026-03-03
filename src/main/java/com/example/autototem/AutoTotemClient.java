package com.example.autototem;

import com.example.autototem.AutoTotemConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;  // TAMBAHAN INI: Import untuk return type
import net.minecraft.client.gui.screen.Screen;

public class AutoTotemClient implements ClientModInitializer, ModMenuApi {
    @Override
    public void onInitializeClient() {
        // Client init (tetep, no changes)
        AutoTotemMod.LOGGER.info("Auto Totem client initialized with ModMenu integration!");
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {  // FIX: Return ConfigScreenFactory<?> (match interface)
        // Return factory untuk config screen (ModMenu API modern, wildcard generic safe)
        return AutoTotemConfigScreen::create;
    }
}