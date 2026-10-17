package com.rickandmorty.item;

import com.rickandmorty.cronenberg.Mutation;
import com.rickandmorty.registry.ModAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Rick's fix for Rick Potion #9, and this time it actually works (mostly). Undoes all mutation,
 * at the price of a very bad few seconds.
 */
public class CureSerumItem extends Item {
    public CureSerumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            if (Mutation.get(player) >= 50.0f) ModAdvancements.award(player, "cured");
            Mutation.set(player, 0.0f);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8f, 1.2f);
            player.displayClientMessage(Component.translatable("message.rickrooms.cure_serum").withStyle(ChatFormatting.GREEN), true);
        }
        if (entity instanceof Player player && !player.hasInfiniteMaterials()) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) return bottle;
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.cure_serum").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
