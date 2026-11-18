package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteororbit.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class TridentLunge extends Module {
    private int originalSlot = -1;
    private int stage = 0; // 0 = idle, 1 = swapped, 2 = fired

    public TridentLunge() {
        super(Categories.Combat, "trident-lunge", "Swap to Riptide trident, lunge, swap back instantly.");
    }

    @Override
    public void onActivate() {
        // Find a trident in the hotbar that has Riptide
        FindItemResult trident = InvUtils.find(itemStack ->
            itemStack.getItem() == Items.TRIDENT && hasRiptide(itemStack)
        );

        if (!trident.found() || trident.slot() > 8) {
            warning("No Riptide trident in hotbar.");
            toggle();
            return;
        }

        originalSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.swap(trident.slot(), true); // silent = true, no swing animation
        stage = 1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (stage == 1) {
            // Fire the trident use packet (Riptide activates on "use")
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                0,                              // sequence, fine as 0 for this
                mc.player.getYaw(), mc.player.getPitch()
            ));
            stage = 2;
        } else if (stage == 2) {
            // Swap back the same tick progression — instant to the eye
            InvUtils.swap(originalSlot, true);
            stage = 0;
            toggle(); // one-shot: turns itself off after completing
        }
    }

    @Override
    public void onDeactivate() {
        // If toggled off mid-lunge, make sure we restore the slot
        if (stage > 0 && originalSlot != -1) {
            InvUtils.swap(originalSlot, true);
            stage = 0;
        }
    }

    private boolean hasRiptide(ItemStack stack) {
        return EnchantmentHelper.getLevel(Enchantments.RIPTIDE, stack) > 0;
    }
}
