package com.revive.network;

import com.revive.ReviveMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: "open the revive menu with these spectators". */
public record OpenReviveScreenPayload(List<Entry> players) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenReviveScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ReviveMod.id("open_screen"));

    public record Entry(UUID id, String name) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Entry::id,
                ByteBufCodecs.STRING_UTF8, Entry::name,
                Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenReviveScreenPayload> CODEC =
            StreamCodec.composite(
                    Entry.CODEC.apply(ByteBufCodecs.list()), OpenReviveScreenPayload::players,
                    OpenReviveScreenPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
