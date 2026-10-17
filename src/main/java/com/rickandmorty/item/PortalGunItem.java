package com.rickandmorty.item;

import com.rickandmorty.block.GatewayStructure;
import com.rickandmorty.block.entity.WarpGatewayBlockEntity;
import com.rickandmorty.client.ClientHooks;
import com.rickandmorty.entity.PortalProjectile;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Right-click: shoot a portal to the destination selected on the dial.
 * Shift + right-click: open the dial to save / select / delete destinations.
 * Right-click a Warp Gateway: link it to another gateway (sneak to unlink).
 * With the Blender Upgrade, the mode key (R) switches to attack mode, where shots open blender portals
 * and right-clicking a Warp Gateway arms it as an evil gateway (or disarms it).
 */
public class PortalGunItem extends Item {
    public static final int MAX_CHARGES = 8;
    /** How far the gun can reach to put a portal on a surface. */
    public static final double MAX_RANGE = 48.0;
    /** When aiming at nothing, the portal opens in mid-air this far away. */
    public static final double AIR_PORTAL_DISTANCE = 4.0;
    private static final int FIRE_COOLDOWN = 20;
    /** Attack mode, aiming at nothing: the blender portal opens a bit further out than a travel portal. */
    private static final double BLENDER_AIR_DISTANCE = 6.0;

    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                ClientHooks.openPortalGunScreen(hand);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide) {
            fire((ServerLevel) level, player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Right-click a Warp Gateway to link it (first gateway, then the second). Sneak to unlink. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        Optional<BlockPos> controller = GatewayStructure.findController(level, context.getClickedPos());
        if (controller.isEmpty() || player == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (isAttackMode(context.getItemInHand())) {
                // attack mode turns the gateway into a trap (and back)
                if (level.getBlockEntity(controller.get()) instanceof WarpGatewayBlockEntity gateway) {
                    com.rickandmorty.block.entity.EvilGateway.toggle(serverLevel, gateway, player);
                }
            } else {
                linkGateway(serverLevel, player, context.getItemInHand(), GlobalPos.of(level.dimension(), controller.get()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void linkGateway(ServerLevel level, Player player, ItemStack gun, GlobalPos gateway) {
        MinecraftServer server = level.getServer();
        GlobalPos pending = gun.get(ModDataComponents.GATEWAY_LINK.get());

        if (player.isShiftKeyDown()) {
            WarpGatewayBlockEntity be = WarpGatewayBlockEntity.get(server, gateway);
            if (be != null && be.getPartner() != null) {
                be.disconnect(server);
                player.displayClientMessage(Component.translatable("message.rickrooms.gateway.unlinked").withStyle(ChatFormatting.YELLOW), true);
            } else {
                player.displayClientMessage(Component.translatable("message.rickrooms.gateway.not_linked").withStyle(ChatFormatting.YELLOW), true);
            }
            if (gateway.equals(pending)) gun.remove(ModDataComponents.GATEWAY_LINK.get());
            return;
        }

        if (pending != null && !pending.equals(gateway) && WarpGatewayBlockEntity.get(server, pending) != null) {
            WarpGatewayBlockEntity.link(server, pending, gateway);
            gun.remove(ModDataComponents.GATEWAY_LINK.get());
            player.displayClientMessage(Component.translatable("message.rickrooms.gateway.linked").withStyle(ChatFormatting.GREEN), true);
        } else {
            gun.set(ModDataComponents.GATEWAY_LINK.get(), gateway);
            level.playSound(null, gateway.pos(), ModSounds.PORTAL_GUN_RELOAD.get(), SoundSource.PLAYERS, 0.8f, 1.6f);
            player.displayClientMessage(Component.translatable("message.rickrooms.gateway.marked").withStyle(ChatFormatting.AQUA), true);
        }
    }

    private void fire(ServerLevel level, Player player, ItemStack stack) {
        if (isAttackMode(stack)) {
            fireBlender(level, player, stack);
            return;
        }
        PortalDestinations dial = getDestinations(stack);
        if (!dial.hasTarget()) {
            player.displayClientMessage(Component.translatable("message.rickrooms.no_destination").withStyle(ChatFormatting.RED), true);
            return;
        }
        // Known dimensions (like the Backrooms) land somewhere new every time.
        Optional<PortalDestinations.Destination> destination = dial.selectedDimension().isPresent()
                ? ModDimensions.randomDestination(level.getServer(), dial.selectedDimension().get(), level.random)
                : dial.selectedDestination();
        if (destination.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.rickrooms.unreachable").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!useCharge(level, player, stack)) return;

        // Aim at a surface within range; otherwise the portal opens in the air in front of the player.
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(MAX_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = hit.getType() == HitResult.Type.MISS ? AIR_PORTAL_DISTANCE : MAX_RANGE + 2.0;
        if (hasTargeting(stack)) {
            // an upgraded gun aimed at a creature should fly all the way to it
            Vec3 rayEnd = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
            EntityHitResult aimed = ProjectileUtil.getEntityHitResult(level, player, eye, rayEnd,
                    player.getBoundingBox().expandTowards(rayEnd.subtract(eye)).inflate(1.0),
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
            if (aimed != null) maxDistance = MAX_RANGE + 2.0;
        }

        PortalProjectile projectile = new PortalProjectile(level, player, destination.get(), maxDistance, hasTargeting(stack));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 0.0f);
        level.addFreshEntity(projectile);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_GUN_FIRE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);
    }

    /** Attack mode: a shot that opens a blender portal where it lands. Needs no destination. */
    private void fireBlender(ServerLevel level, Player player, ItemStack stack) {
        if (!useCharge(level, player, stack)) return;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(MAX_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = hit.getType() == HitResult.Type.MISS ? BLENDER_AIR_DISTANCE : MAX_RANGE + 2.0;
        PortalProjectile projectile = PortalProjectile.blender(level, player, maxDistance);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 0.0f);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_GUN_FIRE.get(), SoundSource.PLAYERS, 1.0f, 0.6f);
        player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);
    }

    /** Spends one charge (reloading from Portal Fluid in the inventory if empty). False if there's nothing to shoot. */
    private boolean useCharge(ServerLevel level, Player player, ItemStack stack) {
        boolean creative = player.getAbilities().instabuild;
        int charges = getCharges(stack);
        if (!creative && charges <= 0) {
            if (!reloadFromInventory(player)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_GUN_EMPTY.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable("message.rickrooms.out_of_fluid").withStyle(ChatFormatting.RED), true);
                player.getCooldowns().addCooldown(this, 10);
                return false;
            }
            charges = MAX_CHARGES;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PORTAL_GUN_RELOAD.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        if (!creative) {
            stack.set(ModDataComponents.PORTAL_CHARGES.get(), charges - 1);
        }
        return true;
    }

    /** Switches between travel and attack mode (only with the Blender Upgrade). */
    public static void toggleMode(Player player, ItemStack stack) {
        if (!hasBlender(stack)) {
            player.displayClientMessage(Component.translatable("message.rickrooms.no_blender").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean attack = !isAttackMode(stack);
        stack.set(ModDataComponents.ATTACK_MODE.get(), attack);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                attack ? ModSounds.BLENDER_GRAB.get() : ModSounds.PORTAL_GUN_RELOAD.get(), SoundSource.PLAYERS, 0.8f, attack ? 1.6f : 1.2f);
        player.displayClientMessage(Component.translatable(attack ? "message.rickrooms.mode.attack" : "message.rickrooms.mode.travel")
                .withStyle(attack ? ChatFormatting.RED : ChatFormatting.GREEN, ChatFormatting.BOLD), true);
    }

    public static boolean hasBlender(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.BLENDER.get()));
    }

    public static boolean isAttackMode(ItemStack stack) {
        return hasBlender(stack) && Boolean.TRUE.equals(stack.get(ModDataComponents.ATTACK_MODE.get()));
    }

    /** Uses up one Portal Fluid from the inventory. Returns false if the player has none. */
    private static boolean reloadFromInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.is(ModItems.PORTAL_FLUID.get())) {
                item.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!inventory.add(bottle)) {
                    player.drop(bottle, false);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean hasDnaLock(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.DNA_LOCK.get()));
    }

    /**
     * Rick's own gun: every upgrade, a full tank, and every level the server has on the dial
     * (for /rickrooms gun and the creative tab; with no server, just the upgrades).
     */
    public static ItemStack fullyUpgraded(@javax.annotation.Nullable MinecraftServer server) {
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.TARGETING.get(), true);
        gun.set(ModDataComponents.BLENDER.get(), true);
        gun.set(ModDataComponents.DNA_LOCK.get(), true);
        gun.set(ModDataComponents.PORTAL_CHARGES.get(), MAX_CHARGES);
        if (server != null) {
            PortalDestinations dial = PortalDestinations.EMPTY;
            for (var level : com.rickandmorty.worldgen.level.BackroomsLevel.values()) {
                if (server.getLevel(level.key()) != null) dial = dial.withLearnedDimension(level.key());
            }
            if (server.getLevel(ModDimensions.CRONENBERG) != null) dial = dial.withLearnedDimension(ModDimensions.CRONENBERG);
            gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), dial);
        }
        return gun;
    }

    public static boolean hasTargeting(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.TARGETING.get()));
    }

    public static int getCharges(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PORTAL_CHARGES.get(), MAX_CHARGES);
    }

    public static PortalDestinations getDestinations(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PORTAL_DESTINATIONS.get(), PortalDestinations.EMPTY);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getCharges(stack) / MAX_CHARGES);
    }

    /** The charge bar turns blood red in attack mode, like the fluid in the capsule. */
    @Override
    public int getBarColor(ItemStack stack) {
        return isAttackMode(stack) ? 0xE0202A : 0x5CFF3B;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.charges", getCharges(stack), MAX_CHARGES)
                .withStyle(ChatFormatting.GREEN));
        if (hasTargeting(stack)) {
            tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.targeting").withStyle(ChatFormatting.RED));
        }
        if (hasDnaLock(stack)) {
            tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.dna_lock").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (hasBlender(stack)) {
            tooltip.add(Component.translatable(isAttackMode(stack) ? "tooltip.rickrooms.portal_gun.mode_attack" : "tooltip.rickrooms.portal_gun.mode_travel")
                    .withStyle(isAttackMode(stack) ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN));
        }
        PortalDestinations dial = getDestinations(stack);
        if (dial.selectedDimension().isPresent()) {
            tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.destination",
                    PortalDestinations.dimensionName(dial.selectedDimension().get()).copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.random_landing").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            dial.selectedDestination().ifPresentOrElse(
                    dest -> {
                        tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.destination",
                                Component.literal(dest.name()).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
                        tooltip.add(dest.describe().withStyle(ChatFormatting.DARK_GRAY));
                    },
                    () -> tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.no_destination").withStyle(ChatFormatting.GRAY)));
        }
        GlobalPos pending = stack.get(ModDataComponents.GATEWAY_LINK.get());
        if (pending != null) {
            tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.gateway_pending",
                    pending.pos().getX(), pending.pos().getY(), pending.pos().getZ()).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.help_fire").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.help_dial").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.help_gateway").withStyle(ChatFormatting.DARK_GRAY));
        if (hasBlender(stack)) {
            tooltip.add(Component.translatable("tooltip.rickrooms.portal_gun.help_mode", Component.keybind("key.rickrooms.gun_mode")).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
