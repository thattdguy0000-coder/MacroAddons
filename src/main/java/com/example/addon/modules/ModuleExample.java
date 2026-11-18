package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class TridentLunge extends Module {
    private int originalSlot = -1;
    private int stage = 0; // 0 = idle, 1 = swapped, 2 = lunged

    public TridentLunge() {
        super(Categories.Combat, "trident-lunge", "Swap to Riptide trident, lunge, swap back instantly.");
    }

    @Override
    public void onActivate() {
        FindItemResult trident = InvUtils.find(this::isRiptideTrident);

        if (!trident.found() || trident.slot() > 8) {
            warning("No Riptide trident in hotbar.");
            toggle();
            return;
        }

        originalSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.swap(trident.slot(), true); // silent swap, no equip animation
        stage = 1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (stage == 1) {
            // Use the trident (Riptide fires on "use") — interactItem handles
            // packet + sequence for us, works in rain or standing in water
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            stage = 2;
        } else if (stage == 2) {
            InvUtils.swap(originalSlot, true);
            stage = 0;
            toggle(); // one-shot: auto-disables when done
        }
    }

    @Override
    public void onDeactivate() {
        if (stage > 0 && originalSlot != -1) {
            InvUtils.swap(originalSlot, true);
            stage = 0;
        }
    }

    private boolean isRiptideTrident(ItemStack stack) {
        if (!stack.isOf(Items.TRIDENT)) return false;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench == null) return false;
        // 1.21+: enchantments are data-driven components, so we match by ID
        return ench.getEnchantmentsMap().keySet().stream()
            .anyMatch(e -> e.getKey().map(k -> k.getValue().getPath())
                .orElse("").equals("riptide"));
    }
}
