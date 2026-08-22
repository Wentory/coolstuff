package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.item.EmissiveTrims;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Coolstuff.MODID, value = Dist.CLIENT)
public final class EmissiveTrimTooltip {
    private EmissiveTrimTooltip() {
    }

    @SubscribeEvent
    public static void addUpgradeLine(ItemTooltipEvent event) {
        if (!EmissiveTrims.isEmissive(event.getItemStack())) return;
        Component emissiveLine = CommonComponents.space()
                .append(Component.translatable("tooltip.coolstuff.emissive").withStyle(ChatFormatting.AQUA));
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component line = event.getToolTip().get(i);
            if (line.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals("item.minecraft.smithing_template.upgrade")) {
                event.getToolTip().add(Math.min(i + 3, event.getToolTip().size()), emissiveLine);
                return;
            }
        }
        event.getToolTip().add(emissiveLine);
    }
}
