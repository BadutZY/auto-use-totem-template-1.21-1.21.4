package com.example.autototem.mixin;

import com.example.autototem.AutoTotemMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(
            method = "getStackInHand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetStackInHand(Hand hand, CallbackInfoReturnable<ItemStack> cir) {
        if (!AutoTotemMod.isModEnabled()) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;

        // Hanya untuk player (safe cast, no NPE)
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        // Hanya handle offhand
        if (hand != Hand.OFF_HAND) {
            return;
        }

        // Cek apakah player health rendah (di bawah 1 heart = 2.0F)
        if (player.getHealth() <= 2.0F) {
            PlayerInventory inventory = player.getInventory();
            ItemStack offhandItem = inventory.getStack(PlayerInventory.OFF_HAND_SLOT);

            // Jika belum ada totem di offhand
            if (!offhandItem.isOf(Items.TOTEM_OF_UNDYING)) {
                int totemSlot = findTotemInInventory(inventory);

                if (totemSlot != -1) {
                    ItemStack totem = inventory.getStack(totemSlot);

                    // Tambahan: Pastiin totem stack size 1 (standar)
                    if (totem.getCount() < 1) {
                        return;  // Skip kalau invalid
                    }

                    ItemStack previousOffhand = offhandItem.copy();

                    // Swap stacks (1.21+ compatible, no crash)
                    inventory.setStack(PlayerInventory.OFF_HAND_SLOT, totem.copy());
                    inventory.setStack(totemSlot, previousOffhand);

                    // Optional: Log buat debug (remove di release)
                    AutoTotemMod.LOGGER.info("AutoTotem: Swapped totem to offhand for player " + player.getName().getString());

                    // Return totem untuk mencegah kematian
                    cir.setReturnValue(totem.copy());
                } else {
                    // Optional: Log kalau gak ada totem
                    AutoTotemMod.LOGGER.warn("AutoTotem: No totem found in inventory for " + player.getName().getString());
                }
            }
        }
    }

    private int findTotemInInventory(PlayerInventory inventory) {
        // Check hotbar dulu (prioritas cepat access, slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING) && stack.getCount() > 0) {
                return i;
            }
        }

        // Check main inventory (slots 9-35, exclude armor/offhand)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING) && stack.getCount() > 0) {
                return i;
            }
        }

        return -1;
    }
}