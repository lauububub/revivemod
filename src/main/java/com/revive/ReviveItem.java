package com.revive;

import com.revive.network.OpenReviveScreenPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ReviveItem extends Item {
    public ReviveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            List<OpenReviveScreenPayload.Entry> spectators = new ArrayList<>();
            for (ServerPlayer p : serverPlayer.level().getServer().getPlayerList().getPlayers()) {
                if (p != serverPlayer && p.isSpectator()) {
                    spectators.add(new OpenReviveScreenPayload.Entry(p.getUUID(), p.getName().getString()));
                }
            }
            ServerPlayNetworking.send(serverPlayer, new OpenReviveScreenPayload(spectators));
        }
        return InteractionResult.SUCCESS;
    }
}
