package net.pcal.splitscreen.fabric;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record PlayerPositionsPacket(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<PlayerPositionsPacket> TYPE = new Type<>(
        net.minecraft.resources.Identifier.fromNamespaceAndPath("splitscreen", "player_positions")
    );

    public static final StreamCodec<FriendlyByteBuf, PlayerPositionsPacket> CODEC = Entry.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(PlayerPositionsPacket::new, PlayerPositionsPacket::entries)
        .cast();

    @Override
    public Type<?> type() {
        return TYPE;
    }

    public record Entry(UUID uuid, double x, double y, double z) {
        public static final StreamCodec<FriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Entry::uuid,
            ByteBufCodecs.DOUBLE, Entry::x,
            ByteBufCodecs.DOUBLE, Entry::y,
            ByteBufCodecs.DOUBLE, Entry::z,
            Entry::new
        );
    }
}
