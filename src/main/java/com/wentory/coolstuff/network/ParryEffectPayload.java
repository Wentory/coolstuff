package com.wentory.coolstuff.network;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.client.ParryEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ParryEffectPayload(double x, double y, double z, int combo, int effect) implements CustomPacketPayload {
    public static final int PARRY = 0;
    public static final int CAKE_SPLAT = 1;

    public ParryEffectPayload(double x, double y, double z, int combo) {
        this(x, y, z, combo, PARRY);
    }
    public static final Type<ParryEffectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "parry_effect")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ParryEffectPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ParryEffectPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ParryEffectPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ParryEffectPayload value) {
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
            buffer.writeVarInt(value.combo);
            buffer.writeVarInt(value.effect);
        }
    };

    public static void handle(ParryEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.effect == CAKE_SPLAT) {
                ParryEffects.playCakeSplatAt(payload.x, payload.y, payload.z);
            } else {
                ParryEffects.playAt(payload.x, payload.y, payload.z, payload.combo);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
