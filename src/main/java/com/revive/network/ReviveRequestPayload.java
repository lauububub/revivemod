package com.revive.network;

import com.revive.ReviveMod;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: "revive this player". The server re-validates everything. */
public record ReviveRequestPayload(UUID target) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReviveRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ReviveMod.id("request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReviveRequestPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, ReviveRequestPayload::target,
                    ReviveRequestPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
