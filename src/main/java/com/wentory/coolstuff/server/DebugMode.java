package com.wentory.coolstuff.server;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class DebugMode {
    private static final String MARKER_TAG = "coolstuff_debug_marker";
    private static volatile boolean enabled;

    private DebugMode() {
    }

    public static int set(CommandSourceStack source, boolean value) {
        enabled = value;
        if (!value) clearMarkers(source.getServer());
        source.sendSuccess(() -> Component.literal("Coolstuff debug: " + (value ? "ON" : "OFF")), true);
        return 1;
    }

    public static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Coolstuff debug: " + (enabled ? "ON" : "OFF")), false);
        return enabled ? 1 : 0;
    }

    public static void markAndAnnounce(Entity entity, String name) {
        if (!enabled || !(entity.level() instanceof ServerLevel level)) return;
        entity.getPersistentData().putBoolean(MARKER_TAG, true);
        entity.setCustomName(Component.literal("●").withStyle(ChatFormatting.RED));
        entity.setCustomNameVisible(true);

        BlockPos pos = entity.blockPosition();
        Component message = Component.literal("[Coolstuff Debug] ").withStyle(ChatFormatting.RED)
                .append(Component.literal(name + " spawned at "
                        + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.YELLOW));
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    private static void clearMarkers(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!entity.getPersistentData().getBoolean(MARKER_TAG)) continue;
                entity.getPersistentData().remove(MARKER_TAG);
                entity.setCustomName(null);
                entity.setCustomNameVisible(false);
            }
        }
    }
}
