package com.rickandmorty.item;

import com.rickandmorty.entity.MeeseeksEntity;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;

/** Press the button: a Mr. Meeseeks pops out to help you for a minute. A few presses per box. */
public class MeeseeksBoxItem extends Item {
    private static final int COOLDOWN = 100;

    public MeeseeksBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            MeeseeksEntity meeseeks = ModEntities.MEESEEKS.get().create(server);
            if (meeseeks == null) return InteractionResultHolder.fail(box);
            Vec3 front = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
            boolean roomInFront = server.noCollision(meeseeks.getType().getSpawnAABB(front.x, front.y, front.z));
            Vec3 at = roomInFront ? front : player.position();
            meeseeks.moveTo(at.x, at.y, at.z, player.getYRot() + 180.0f, 0.0f);
            meeseeks.setOwner(player);
            EventHooks.finalizeMobSpawn(meeseeks, server, server.getCurrentDifficultyAt(meeseeks.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            server.addFreshEntity(meeseeks);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1.0f, 1.2f);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.7f);
            player.displayClientMessage(Component.translatable("message.rickrooms.meeseeks.hello").withStyle(ChatFormatting.AQUA), true);
            box.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.meeseeks_box").withStyle(ChatFormatting.GRAY));
    }
}
