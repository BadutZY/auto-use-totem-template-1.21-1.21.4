package com.example.autototem;

import com.example.autototem.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;  // FIX: Tambah import PlayerEntity
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTotemMod implements ModInitializer {
    public static final String MOD_ID = "auto-use-totem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleKey;
    public static boolean inGameEnabled = true;

    // Shared debounce (sync mixin + tick)
    private static int equipCooldown = 0;

    // Track pop
    private static boolean lastOffhandWasTotem = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Auto Totem Mod");

        ModConfig.load();
        inGameEnabled = ModConfig.isGlobalEnabled();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.auto-totem.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.auto-totem"
        ));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            // Decrement shared cooldown
            if (equipCooldown > 0) equipCooldown--;

            // Keybind toggle
            if (ModConfig.isGlobalEnabled() && ModConfig.isKeybindEnabled() && toggleKey.wasPressed()) {
                inGameEnabled = !inGameEnabled;
                String status = inGameEnabled ? "§a Active" : "§c Inactive";
                player.sendMessage(Text.literal("§6[Auto Totem] §fMod " + status), true);
                LOGGER.debug("In-game state flipped to: {}", inGameEnabled);
            }

            ItemStack offhand = player.getOffHandStack();
            boolean currentOffhandTotem = offhand.isOf(Items.TOTEM_OF_UNDYING);

            // Pop detect: Re-equip kalau low HP & cooldown OK
            if (lastOffhandWasTotem && !currentOffhandTotem && ModConfig.isGlobalEnabled() && inGameEnabled && player.getHealth() <= 2.0F && equipCooldown <= 0) {
                checkAndEquipTotem(player);
                equipCooldown = 2;
                LOGGER.debug("Tick detected totem pop, re-equipping...");
            }

            // Main tick equip: Low HP, no totem, cooldown OK
            if (ModConfig.isGlobalEnabled() && inGameEnabled && player.getHealth() <= 2.0F && !currentOffhandTotem && equipCooldown <= 0) {
                checkAndEquipTotem(player);
                equipCooldown = 2;
            }

            lastOffhandWasTotem = currentOffhandTotem;
        });
    }

    // FIX: Ubah signature ke PlayerEntity (umum), cast internal ke ClientPlayerEntity
    // Aman di client context (mixin & tick sama-sama client-side)
    public static void checkAndEquipTotem(PlayerEntity player) {
        if (!(player instanceof ClientPlayerEntity clientPlayer)) {  // FIX: Safe cast + null-check
            LOGGER.warn("checkAndEquipTotem called on non-client player, skipping.");
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerInteractionManager interactionManager = mc.interactionManager;
        if (interactionManager == null) return;

        PlayerInventory inventory = clientPlayer.getInventory();
        // FIX 1.21.5: Ganti inventory.offHand.get(0) ke inventory.getStack(PlayerInventory.OFF_HAND_SLOT)
        if (inventory.getStack(PlayerInventory.OFF_HAND_SLOT).isOf(Items.TOTEM_OF_UNDYING)) return;

        int totemSlot = findTotemInInventory(inventory);
        if (totemSlot != -1) {
            int syncId = clientPlayer.playerScreenHandler.syncId;
            int offhandSlotId = 45;  // Container slot ID untuk offhand (tetap 45 di screen handler)

            interactionManager.clickSlot(syncId, totemSlot, 0, SlotActionType.PICKUP, clientPlayer);
            interactionManager.clickSlot(syncId, offhandSlotId, 0, SlotActionType.PICKUP, clientPlayer);
            interactionManager.clickSlot(syncId, totemSlot, 0, SlotActionType.PICKUP, clientPlayer);

            clientPlayer.playerScreenHandler.syncState();
            LOGGER.debug("Equipped totem via {} (shared packet)", Thread.currentThread().getStackTrace()[2].getMethodName());
        }
    }

    private static int findTotemInInventory(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        for (int i = 9; i < 36; i++) {  // FIX: Hanya sampai 35 (main inventory), exclude armor/offhand
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    public static boolean isModEnabled() {
        return ModConfig.isGlobalEnabled() && inGameEnabled;
    }
}