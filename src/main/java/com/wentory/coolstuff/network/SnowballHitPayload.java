package com.wentory.coolstuff.network;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.client.SnowballScreenEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SnowballHitPayload(int seed) implements CustomPacketPayload {
    public static final Type<SnowballHitPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "snowball_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SnowballHitPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.seed),
                    buffer -> new SnowballHitPayload(buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SnowballHitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SnowballScreenEffects.trigger(payload.seed));
    }
}
