package com.rickandmorty.entity.cruiser;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * The Space Cruiser's remote. Use it on your cruiser to pair it; after that, with Auto Park fitted,
 * press it anywhere nearby and the cruiser flies over and parks next to you.
 */
public class CruiserRemoteItem extends Item {
    private static final String KEY = RickAndMortyMod.MOD_ID + ":cruiser";

    public CruiserRemoteItem(Properties properties) {
        super(properties);
    }

    public static void bind(ItemStack stack, SpaceCruiserEntity cruiser) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putUUID(KEY, cruiser.getUUID());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static UUID bound(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(KEY) ? tag.getUUID(KEY) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        UUID id = bound(stack);
        if (id == null) {
            player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.remote_unbound").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!(server.getEntity(id) instanceof SpaceCruiserEntity cruiser) || cruiser.distanceToSqr(player) > 512 * 512) {
            player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.remote_far").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        Vec3 spot = player.position().add(Vec3.directionFromRotation(0.0f, player.getYRot()).scale(4.0)).add(0.0, 1.0, 0.0);
        if (!cruiser.summon(spot)) {
            player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.remote_no_autopark").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.8f, 1.8f);
        player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.remote_called").withStyle(ChatFormatting.GREEN), true);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(bound(stack) == null ? "item.rickrooms.cruiser_remote.unbound" : "item.rickrooms.cruiser_remote.bound")
                .withStyle(ChatFormatting.GRAY));
    }
}
