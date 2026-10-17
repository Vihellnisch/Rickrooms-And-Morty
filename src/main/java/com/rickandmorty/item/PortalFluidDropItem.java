package com.rickandmorty.item;

import com.rickandmorty.entity.PortalTeleporter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A single drop of portal fluid, found in Backrooms chests. Drinking it throws you to one of
 * the locations saved on a Portal Gun you're carrying. With no saved locations, you're
 * spat out at the world spawn instead. Either way: a way out.
 */
public class PortalFluidDropItem extends Item {
    private static final int DRINK_TICKS = 32;

    public PortalFluidDropItem(Properties properties) {
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
        return DRINK_TICKS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            escape(player);
            stack.consume(1, player);
        }
        return stack;
    }

    private static void escape(ServerPlayer player) {
        ServerLevel from = player.serverLevel();
        List<PortalDestinations.Destination> saved = savedLocations(player.getInventory());
        if (!saved.isEmpty()) {
            PortalDestinations.Destination dest = saved.get(player.getRandom().nextInt(saved.size()));
            ServerLevel target = player.server.getLevel(dest.dimension());
            if (target != null) {
                PortalTeleporter.teleport(player, from, target, dest.position(), dest.yaw());
                player.displayClientMessage(Component.translatable("message.rickrooms.drop.teleported",
                        Component.literal(dest.name()).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GREEN), true);
                return;
            }
        }
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, overworld.getSharedSpawnPos());
        PortalTeleporter.teleport(player, from, overworld, Vec3.atBottomCenterOf(spawn), player.getYRot());
        player.displayClientMessage(Component.translatable("message.rickrooms.drop.no_locations").withStyle(ChatFormatting.YELLOW), true);
    }

    private static List<PortalDestinations.Destination> savedLocations(Inventory inventory) {
        List<PortalDestinations.Destination> all = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof PortalGunItem) {
                all.addAll(PortalGunItem.getDestinations(stack).destinations());
            }
        }
        return all;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_fluid_drop.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_fluid_drop.2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
