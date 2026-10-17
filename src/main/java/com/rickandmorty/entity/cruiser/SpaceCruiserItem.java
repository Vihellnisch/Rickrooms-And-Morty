package com.rickandmorty.entity.cruiser;

import com.rickandmorty.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Rick's Space Cruiser, folded up in your pocket. Use it on the ground to park it there. */
public class SpaceCruiserItem extends Item {
    public SpaceCruiserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        Vec3 at = hit.getLocation();
        if (level instanceof ServerLevel server) {
            SpaceCruiserEntity cruiser = ModEntities.SPACE_CRUISER.get().create(server);
            if (cruiser == null) return InteractionResultHolder.fail(stack);
            cruiser.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0f);
            if (!level.noCollision(cruiser, cruiser.getBoundingBox())) {
                player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.no_room").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            cruiser.setOwner(player.getUUID());
            server.addFreshEntity(cruiser);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rickrooms.space_cruiser.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
