package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.cake.CakeFilling;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.entity.ThrownCakeEntity;
import com.wentory.coolstuff.registry.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class ThrownCakeHandler {
    private ThrownCakeHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!RestartRequiredConfig.cakeFillings() || !event.getItemStack().is(Items.CAKE)) return;
        CakeFilling filling = CakeFilling.fromStack(event.getItemStack());
        if (filling == CakeFilling.NONE) return;
        event.getToolTip().add(Component.translatable("tooltip.coolstuff.cake_filling",
                Component.translatable("cake_filling.coolstuff." + filling.id()).withStyle(filling.color()))
                .withStyle(filling.color()));
    }

    @SubscribeEvent
    public static void onRightClickCake(PlayerInteractEvent.RightClickItem event) {
        if (!CoolstuffConfig.ENABLE_THROWABLE_CAKES.get() || !event.getItemStack().is(Items.CAKE)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        throwCake(event.getEntity(), event.getHand(), event.getItemStack());
    }

    private static void throwCake(Player player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        player.swing(hand, true);
        if (player.level().isClientSide()) return;

        ThrownCakeEntity cake = new ThrownCakeEntity(ModEntities.THROWN_CAKE.get(), player.level());
        cake.setFilling(RestartRequiredConfig.cakeFillings() ? CakeFilling.fromStack(stack) : CakeFilling.NONE);
        cake.setOwner(player);
        cake.setPos(player.getX(), player.getEyeY() - 0.18, player.getZ());
        cake.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.15F, 0.7F);
        player.level().addFreshEntity(cake);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8F, 0.72F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(Items.CAKE, 10);
    }
}
