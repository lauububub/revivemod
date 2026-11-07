package com.revive;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;

/**
 * Server-side only. The Revive item is a ghast tear with special item components
 * (custom name, lore, item_model, custom_data). Only the crafting recipe produces it.
 */
public class ReviveMod implements ModInitializer {
    public static final String MOD_ID = "revive";

    // Kept so leftover files from the old client-side version still compile. Safe to delete those later.
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** A real Revive item = ghast tear carrying custom_data {revive:1}. Normal ghast tears don't count. */
    public static boolean isRevive(ItemStack stack) {
        if (!stack.is(Items.GHAST_TEAR)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains("revive");
    }

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!isRevive(stack)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer user) {
                openMenu(user);
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static void openMenu(ServerPlayer user) {
        List<ServerPlayer> spectators = new ArrayList<>();
        for (ServerPlayer p : user.level().getServer().getPlayerList().getPlayers()) {
            if (p != user && p.isSpectator()) {
                spectators.add(p);
            }
        }
        if (spectators.isEmpty()) {
            user.sendSystemMessage(Component.literal("No players are in spectator mode.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        user.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new ReviveMenu(containerId, inventory, user, spectators),
                Component.literal("Revive a Player")));
    }

    /** Server re-checks everything before reviving. */
    public static void revive(ServerPlayer user, UUID targetId) {
        var server = user.level().getServer();

        // Must still be holding the item.
        ItemStack held = null;
        if (isRevive(user.getMainHandItem())) {
            held = user.getMainHandItem();
        } else if (isRevive(user.getOffhandItem())) {
            held = user.getOffhandItem();
        }
        if (held == null) {
            return;
        }

        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        if (target == null || target == user || !target.isSpectator()) {
            user.sendSystemMessage(Component.literal("That player can't be revived right now.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        ServerLevel level = (ServerLevel) user.level();

        target.setGameMode(GameType.SURVIVAL);
        target.teleportTo(level, user.getX(), user.getY(), user.getZ(), Set.of(),
                user.getYRot(), user.getXRot(), true);
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);

        if (!user.getAbilities().instabuild) {
            held.shrink(1);
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        server.getPlayerList().broadcastSystemMessage(
                Component.literal(user.getName().getString() + " revived " + target.getName().getString() + "!")
                        .withStyle(ChatFormatting.GOLD),
                false);
    }
}
