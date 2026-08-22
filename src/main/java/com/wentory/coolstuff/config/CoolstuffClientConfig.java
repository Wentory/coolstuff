package com.wentory.coolstuff.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CoolstuffClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PARRY_SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue PARRY_VISUAL_INTENSITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("parryEffects");
        PARRY_SOUND_VOLUME = builder
                .comment("Parry and phase-transition sound volume. 0 disables these sounds and 1 is full volume.")
                .defineInRange("soundVolume", 1.0, 0.0, 1.0);
        PARRY_VISUAL_INTENSITY = builder
                .comment("Intensity of parry particles, flash and camera shake. Floating PARRY text is unaffected.")
                .defineInRange("visualIntensity", 1.0, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private CoolstuffClientConfig() {
    }
}
