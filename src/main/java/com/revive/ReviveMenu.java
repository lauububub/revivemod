package com.revive;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * A normal 6-row chest menu built on the server, so vanilla clients can open it.
 * Each spectator is shown as a player head; clicking one revives them.
 */
public class ReviveMenu extends AbstractContainerMenu {
    private static final int TOP_SLOTS = 54;

    private final UUID[] targets = new UUID[TOP_SLOTS];
    private final ServerPlayer user;
    private boolean used = false;

    public ReviveMenu(int containerId, Inventory inventory, ServerPlayer user, List<ServerPlayer> spectators) {
        super(MenuType.GENERIC_9x6, containerId);
        this.user = user;

        SimpleContainer container = new SimpleContainer(TOP_SLOTS);
        for (int i = 0; i < Math.min(TOP_SLOTS, spectators.size()); i++) {
            ServerPlayer target = spectators.get(i);
            targets[i] = target.getUUID();

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.CUSTOM_NAME,
                    Component.literal(target.getName().getString()).withStyle(ChatFormatting.GOLD));
            head.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("Click to revive").withStyle(ChatFormatting.GRAY))));
            container.setItem(i, head);
        }

        for (int i = 0; i < TOP_SLOTS; i++) {
            this.addSlot(new ButtonSlot(container, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 197));
        }
    }

    /** A slot that can't hold or give items; trying to pick it up counts as a button press. */
    private class ButtonSlot extends Slot {
        private final int index;

        ButtonSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.index = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            UUID target = targets[index];
            if (target != null && !used) {
                used = true;
                // Run after the current click has finished being processed.
                user.level().getServer().execute(() -> {
                    ReviveMod.revive(user, target);
                    user.closeContainer();
                });
            }
            return false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
