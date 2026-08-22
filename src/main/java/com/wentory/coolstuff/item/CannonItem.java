package com.wentory.coolstuff.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.wentory.coolstuff.registry.ModEnchantments;
import com.wentory.coolstuff.server.CannonProjectileHandler;
import com.wentory.coolstuff.config.CoolstuffConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class CannonItem extends Item {
    public CannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!CoolstuffConfig.ENABLE_FIREBALL_LAUNCHER.get()) return InteractionResultHolder.fail(stack);
        int chargeSlot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.FIRE_CHARGE));
        if (!player.getAbilities().instabuild && chargeSlot < 0) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                player.getInventory().removeItem(chargeSlot, 1);
            }
            Vec3 look = player.getLookAngle();
            boolean accelerated = EnchantmentHelper.getItemEnchantmentLevel(
                    player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(ModEnchantments.ACCELERATION), stack) > 0;
            Vec3 movement = accelerated
                    ? look.scale(1.8)
                    : new Vec3(look.x * 0.42, 0.62 + look.y * 0.20, look.z * 0.42);
            LargeFireball fireball = new LargeFireball(level, player, look, 1);
            fireball.setPos(player.getEyePosition().add(look.scale(1.25)));
            fireball.setDeltaMovement(movement);
            fireball.accelerationPower = accelerated ? 0.12 : 0.0;
            if (!accelerated) CannonProjectileHandler.markAsLobbed(fireball);
            level.addFreshEntity(fireball);
            level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT,
                    SoundSource.PLAYERS, 1.2F, 0.85F);
            player.getCooldowns().addCooldown(this, 16);
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(ModEnchantments.ACCELERATION);
    }
}
