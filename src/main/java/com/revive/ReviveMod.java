package com.revive;

import com.revive.network.OpenReviveScreenPayload;
import com.revive.network.ReviveRequestPayload;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;

public class ReviveMod implements ModInitializer {
    public static final String MOD_ID = "revive";

    public static Item REVIVE;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // Custom item (NOT a vanilla ghast tear) so nobody can just pick it up from a ghast.
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("revive"));
        REVIVE = Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new ReviveItem(new Item.Properties()
                        .setId(key)
                        .stacksTo(1)
                        .rarity(Rarity.EPIC)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                        .component(DataComponents.LORE, new ItemLore(List.of(
                                Component.translatable("item.revive.revive.desc")
                                        .withStyle(ChatFormatting.RED))))));

        PayloadTypeRegistry.playS2C().register(OpenReviveScreenPayload.TYPE, OpenReviveScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReviveRequestPayload.TYPE, ReviveRequestPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ReviveRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer user = context.player();
            var server = context.server();

            // Must actually be holding the item.
            ItemStack held = null;
            if (user.getMainHandItem().is(REVIVE)) {
                held = user.getMainHandItem();
            } else if (user.getOffhandItem().is(REVIVE)) {
                held = user.getOffhandItem();
            }
            if (held == null) {
                return;
            }

            ServerPlayer target = server.getPlayerList().getPlayer(payload.target());
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
        });
    }
}
