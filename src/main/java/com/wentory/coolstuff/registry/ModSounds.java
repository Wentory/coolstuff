package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Coolstuff.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_SPAWN =
            SOUND_EVENTS.register("blackhole_spawn", () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "blackhole_spawn"), 200.0F));

    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_FORMATION =
            SOUND_EVENTS.register("blackhole_formation", () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "blackhole_formation"), 96.0F));

    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_AMBIENT =
            SOUND_EVENTS.register("blackhole_ambient", () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "blackhole_ambient"), 128.0F));

    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_HOLE_EXPLOSION =
            SOUND_EVENTS.register("blackhole_explosion", () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "blackhole_explosion"), 200.0F));

    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_FART =
            SOUND_EVENTS.register("spore_fart", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "spore_fart")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CAKE_SPLAT =
            SOUND_EVENTS.register("cake_splat", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "cake_splat")));

    private ModSounds() {
    }
}
