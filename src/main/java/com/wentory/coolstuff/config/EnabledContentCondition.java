package com.wentory.coolstuff.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public record EnabledContentCondition(String content) implements ICondition {
    public static final MapCodec<EnabledContentCondition> CODEC = Codec.STRING.fieldOf("content")
            .xmap(EnabledContentCondition::new, EnabledContentCondition::content);

    @Override
    public boolean test(IContext context) {
        return switch (content) {
            case "cake_fillings" -> BootstrapConfig.CAKE_FILLINGS;
            case "zombie_wolf" -> BootstrapConfig.ZOMBIE_WOLF;
            default -> false;
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
