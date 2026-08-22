package com.wentory.coolstuff.network;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.client.EventHorizonEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlackHoleImpactPayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<BlackHoleImpactPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "black_hole_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlackHoleImpactPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BlackHoleImpactPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BlackHoleImpactPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BlackHoleImpactPayload value) {
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
        }
    };

    public static void handle(BlackHoleImpactPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EventHorizonEffects.triggerExplosion(payload.x, payload.y, payload.z));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
