package com.rickandmorty.entity.cruiser;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** What the Space Cruiser's upgrades do. All of it happens on the server. */
public final class CruiserWeapons {
    public static final ResourceKey<DamageType> SHOT = ResourceKey.create(Registries.DAMAGE_TYPE, RickAndMortyMod.id("space_cruiser"));
    public static final ResourceKey<DamageType> DISINTEGRATED = ResourceKey.create(Registries.DAMAGE_TYPE, RickAndMortyMod.id("disintegrated"));
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.35f, 1.0f, 0.3f), 1.4f);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.1f), 0.8f);
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.4f, 0.9f, 1.0f), 1.0f);

    private CruiserWeapons() {
    }

    private static DamageSource damage(SpaceCruiserEntity cruiser, @Nullable Player shooter, ResourceKey<DamageType> type) {
        var holder = cruiser.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return new DamageSource(holder, cruiser, shooter != null ? shooter : cruiser);
    }

    // ------------------------------------------------------------------ the controls

    public static void firePressed(SpaceCruiserEntity cruiser, ServerPlayer pilot) {
        CruiserUpgrade weapon = cruiser.selectedWeapon();
        if (weapon == null) {
            pilot.displayClientMessage(Component.translatable("message.rickrooms.cruiser.no_weapon").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (weapon.continuous) cruiser.setFiring(true);
        else shoot(cruiser, pilot, weapon, pilot.getLookAngle());
    }

    public static void fireReleased(SpaceCruiserEntity cruiser) {
        cruiser.setFiring(false);
    }

    /** Every tick the trigger's held down. */
    static void fireHeld(SpaceCruiserEntity cruiser, ServerPlayer pilot) {
        CruiserUpgrade weapon = cruiser.selectedWeapon();
        if (weapon == null || !weapon.continuous) {
            cruiser.setFiring(false);
            return;
        }
        shoot(cruiser, pilot, weapon, pilot.getLookAngle());
    }

    /** Fires {@code weapon} towards {@code aim}. False if it's cooling down or there isn't the power. */
    public static boolean shoot(SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot, CruiserUpgrade weapon, Vec3 aim) {
        if (!cruiser.has(weapon) || !cruiser.ready(weapon)) return false;
        if (!cruiser.usePower(weapon.cost)) {
            if (pilot != null && cruiser.tickCount % 20 == 0) {
                pilot.displayClientMessage(Component.translatable("message.rickrooms.cruiser.low_power").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        cruiser.cool(weapon);
        ServerLevel level = (ServerLevel) cruiser.level();
        switch (weapon) {
            case DISINTEGRATOR -> disintegrate(level, cruiser, pilot, aim);
            case MINIGUN -> minigun(level, cruiser, pilot, aim);
            case FLAMETHROWER -> flames(level, cruiser, pilot, aim);
            case ROCKETS -> rockets(level, cruiser, pilot, aim);
            case LASER_DRILL -> drill(level, cruiser, pilot, aim);
            default -> {
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ hitting things

    private record Hit(Vec3 end, @Nullable Entity entity, @Nullable BlockHitResult block) {
    }

    private static Hit trace(SpaceCruiserEntity cruiser, Vec3 from, Vec3 dir, double range) {
        Vec3 to = from.add(dir.scale(range));
        BlockHitResult block = cruiser.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cruiser));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(cruiser.level(), cruiser, from, end, new AABB(from, end).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !cruiser.hasPassenger(e) && e != cruiser, 0.3f);
        if (entity != null) return new Hit(entity.getLocation(), entity.getEntity(), null);
        return new Hit(end, null, block.getType() == HitResult.Type.BLOCK ? block : null);
    }

    private static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double step) {
        Vec3 d = to.subtract(from);
        int n = (int) (d.length() / step);
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) Math.max(1, n)));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void disintegrate(ServerLevel level, SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot, Vec3 aim) {
        Vec3 from = cruiser.muzzle();
        Hit hit = trace(cruiser, from, aim, 48);
        beam(level, from, hit.end(), GREEN, 0.35);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1.4f, 1.9f);
        if (hit.entity() instanceof LivingEntity target) {
            boolean dead = target.hurt(damage(cruiser, pilot, DISINTEGRATED), 26.0f) && !target.isAlive();
            if (dead || !target.isAlive()) {
                // nothing left but dust
                level.sendParticles(GREEN, target.getX(), target.getY(0.5), target.getZ(), 60, target.getBbWidth() / 2, target.getBbHeight() / 2,
                        target.getBbWidth() / 2, 0.02);
                level.sendParticles(ParticleTypes.ASH, target.getX(), target.getY(0.5), target.getZ(), 40, 0.4, 0.6, 0.4, 0.01);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SAND_BREAK, SoundSource.NEUTRAL, 1.2f, 0.6f);
                if (pilot != null) ModAdvancements.award(pilot, "disintegrated");
            }
        }
    }

    private static void minigun(ServerLevel level, SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot, Vec3 aim) {
        RandomSource random = cruiser.getRandom();
        double side = (cruiser.tickCount / 3) % 2 == 0 ? 0.6 : -0.6;              // the two barrels take turns
        Vec3 from = cruiser.muzzle().add(cruiser.forward().yRot(Mth.HALF_PI).scale(side));
        // both barrels converge on whatever is straight ahead of the aim, with a little spread
        Vec3 aimedAt = trace(cruiser, cruiser.muzzle(), aim, 40).end();
        Vec3 dir = aimedAt.subtract(from).normalize()
                .add((random.nextDouble() - 0.5) * 0.03, (random.nextDouble() - 0.5) * 0.03, (random.nextDouble() - 0.5) * 0.03).normalize();
        Hit hit = trace(cruiser, from, dir, 40);
        beam(level, from, hit.end(), RED, 0.5);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.NEUTRAL, 0.5f, 1.9f + random.nextFloat() * 0.2f);
        if (hit.entity() instanceof LivingEntity target) {
            target.invulnerableTime = 0;
            target.hurt(damage(cruiser, pilot, SHOT), 3.5f);
        } else if (hit.block() != null) {
            Vec3 at = hit.end();
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private static void flames(ServerLevel level, SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot, Vec3 aim) {
        RandomSource random = cruiser.getRandom();
        for (double side : new double[]{0.75, -0.75}) {
            Vec3 from = cruiser.muzzle().add(cruiser.forward().yRot(Mth.HALF_PI).scale(side)).add(0, -0.25, 0);
            for (int i = 0; i < 4; i++) {
                Vec3 v = aim.add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.25).scale(0.7 + random.nextDouble() * 0.4);
                level.sendParticles(random.nextInt(5) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
            }
        }
        if (cruiser.tickCount % 6 == 0) {
            Vec3 m = cruiser.muzzle();
            level.playSound(null, m.x, m.y, m.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.6f, 0.7f + random.nextFloat() * 0.3f);
        }
        // anything in the jets catches fire
        Vec3 from = cruiser.muzzle();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from.add(aim.scale(10))).inflate(3),
                e -> e.isAlive() && !cruiser.hasPassenger(e))) {
            Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0).subtract(from);
            if (to.length() > 10 || to.normalize().dot(aim) < 0.82) continue;
            target.igniteForSeconds(5.0f);
            if (cruiser.tickCount % 4 == 0) target.hurt(damage(cruiser, pilot, SHOT), 2.0f);
        }
        if (random.nextInt(8) == 0 && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            Hit hit = trace(cruiser, from, aim, 9);
            if (hit.block() != null) {
                BlockPos fire = hit.block().getBlockPos().relative(hit.block().getDirection());
                if (level.isEmptyBlock(fire)) level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
            }
        }
    }

    private static void rockets(ServerLevel level, SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot, Vec3 aim) {
        for (double side : new double[]{1.0, -1.0}) {
            Vec3 from = cruiser.muzzle().add(cruiser.forward().yRot(Mth.HALF_PI).scale(side)).add(0, -0.1, 0);
            CruiserRocket rocket = new CruiserRocket(ModEntities.CRUISER_ROCKET.get(), level);
            rocket.setOwner(pilot != null ? pilot : cruiser);
            rocket.moveTo(from.x, from.y, from.z, cruiser.getYRot(), 0.0f);
            rocket.setDeltaMovement(aim.scale(1.6));
            level.addFreshEntity(rocket);
        }
        Vec3 m = cruiser.muzzle();
        level.playSound(null, m.x, m.y, m.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.2f, 0.7f);
    }

    private static void drill(ServerLevel level, SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot, Vec3 aim) {
        Vec3 from = cruiser.muzzle();
        Hit hit = trace(cruiser, from, aim, 24);
        beam(level, from, hit.end(), CYAN, 0.3);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.GRINDSTONE_USE, SoundSource.NEUTRAL, 0.35f, 1.7f);
        if (hit.entity() instanceof LivingEntity target) {
            target.hurt(damage(cruiser, pilot, SHOT), 4.0f);
            return;
        }
        if (hit.block() == null || pilot == null) return;
        BlockPos center = hit.block().getBlockPos();
        Direction.Axis axis = hit.block().getDirection().getAxis();
        int mark = cruiser.tier(CruiserTier.DRILL);
        int reach = CruiserTier.drillSize(mark) / 2;               // Mk I: just the block, Mk II: 3x3, Mk III: 5x5
        float hardest = CruiserTier.drillHardness(mark);           // Mk III cuts obsidian
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.end().x, hit.end().y, hit.end().z, 8 + reach * 6, 0.3 + reach * 0.5, 0.3 + reach * 0.5,
                0.3 + reach * 0.5, 0.1);
        for (int a = -reach; a <= reach; a++) {
            for (int b = -reach; b <= reach; b++) {
                BlockPos pos = switch (axis) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (state.isAir() || hardness < 0 || hardness > hardest || !level.mayInteract(pilot, pos) || pilot.blockActionRestricted(level, pos, pilot.gameMode.getGameModeForPlayer())) continue;
                if (!cruiser.usePower(CruiserUpgrade.LASER_DRILL.cost)) return;
                level.destroyBlock(pos, true, pilot);
            }
        }
    }

    // ------------------------------------------------------------------ the other upgrades

    public static void repulse(SpaceCruiserEntity cruiser, @Nullable ServerPlayer pilot) {
        CruiserUpgrade repulsor = CruiserUpgrade.REPULSOR;
        if (!cruiser.has(repulsor) || !cruiser.ready(repulsor)) return;
        if (!cruiser.usePower(repulsor.cost)) {
            if (pilot != null) pilot.displayClientMessage(Component.translatable("message.rickrooms.cruiser.low_power").withStyle(ChatFormatting.RED), true);
            return;
        }
        cruiser.cool(repulsor);
        ServerLevel level = (ServerLevel) cruiser.level();
        Vec3 center = cruiser.position().add(0, 0.8, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 0, Math.cos(a) * 1.2, 0.05, Math.sin(a) * 1.2, 1.0);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.0f, 1.5f);
        for (Entity entity : level.getEntities(cruiser, new AABB(center, center).inflate(10), e -> !cruiser.hasPassenger(e) && !(e instanceof SpaceCruiserEntity))) {
            Vec3 away = entity.position().subtract(center);
            double d = away.length();
            if (d > 10) continue;
            Vec3 push = (d < 0.01 ? new Vec3(0, 1, 0) : away.normalize()).scale(0.6 + 1.8 * (1 - d / 10)).add(0, 0.5, 0);
            entity.setDeltaMovement(entity.getDeltaMovement().add(push));
            entity.hurtMarked = true;
            if (entity instanceof LivingEntity living) living.hurt(damage(cruiser, pilot, SHOT), 4.0f);
        }
    }

    /** The portal drive: a portal opens ahead, and the whole cruiser goes through to wherever your Portal Gun is set. */
    public static void portalJump(SpaceCruiserEntity cruiser, ServerPlayer pilot) {
        CruiserUpgrade drive = CruiserUpgrade.PORTAL_DRIVE;
        if (!cruiser.has(drive) || !cruiser.ready(drive) || cruiser.portalJumpPending()) return;
        ItemStack gun = findGun(pilot);
        if (gun.isEmpty()) {
            pilot.displayClientMessage(Component.translatable("message.rickrooms.cruiser.need_gun").withStyle(ChatFormatting.RED), true);
            return;
        }
        PortalDestinations dial = PortalGunItem.getDestinations(gun);
        ServerLevel level = pilot.serverLevel();
        Optional<PortalDestinations.Destination> destination = dial.selectedDimension()
                .flatMap(dim -> ModDimensions.randomDestination(level.getServer(), dim, cruiser.getRandom()))
                .or(dial::selectedDestination);
        if (destination.isEmpty()) {
            pilot.displayClientMessage(Component.translatable("message.rickrooms.cruiser.no_destination").withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean creative = pilot.getAbilities().instabuild;
        int charges = PortalGunItem.getCharges(gun);
        if (!creative && charges <= 0) {
            pilot.displayClientMessage(Component.translatable("message.rickrooms.out_of_fluid").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!cruiser.usePower(drive.cost)) {
            pilot.displayClientMessage(Component.translatable("message.rickrooms.cruiser.low_power").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!creative) gun.set(ModDataComponents.PORTAL_CHARGES.get(), charges - 1);
        cruiser.cool(drive);
        PortalDestinations.Destination to = destination.get();
        ServerLevel target = level.getServer().getLevel(to.dimension());
        if (target == null) return;
        // the portal opens in front of it
        Vec3 ahead = cruiser.position().add(cruiser.forward().scale(4.0)).add(0, 1.0, 0);
        level.playSound(null, ahead.x, ahead.y, ahead.z, com.rickandmorty.registry.ModSounds.PORTAL_OPEN.get(), SoundSource.NEUTRAL, 1.5f, 0.8f);
        for (int i = 0; i < 80; i++) {
            double a = i / 80.0 * Math.PI * 2, r = 1.6;
            Vec3 side = cruiser.forward().yRot(Mth.HALF_PI);
            Vec3 at = ahead.add(side.scale(Math.cos(a) * r)).add(0, Math.sin(a) * r, 0);
            level.sendParticles(GREEN, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        level.sendParticles(ParticleTypes.PORTAL, ahead.x, ahead.y, ahead.z, 120, 0.6, 1.0, 0.6, 0.6);
        cruiser.setDeltaMovement(cruiser.forward().scale(0.3));
        cruiser.schedulePortalJump(30, () -> {
            if (cruiser.isRemoved()) return;
            target.getChunkAt(BlockPos.containing(to.x(), to.y(), to.z()));
            double y = Math.max(to.y(), target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(to.x()), (int) Math.floor(to.z())) + 0.5);
            level.playSound(null, cruiser.getX(), cruiser.getY(), cruiser.getZ(), com.rickandmorty.registry.ModSounds.PORTAL_TRAVEL.get(), SoundSource.NEUTRAL, 1.5f, 0.9f);
            travel(cruiser, target, new Vec3(to.x(), y, to.z()));
            target.playSound(null, to.x(), y, to.z(), com.rickandmorty.registry.ModSounds.PORTAL_TRAVEL.get(), SoundSource.NEUTRAL, 1.5f, 1.1f);
            ModAdvancements.award(pilot, "cruiser_portal");
        });
    }

    /**
     * Moves the cruiser, and everyone in it, to {@code to}. The pilot's own game is the one flying
     * the cruiser and it ignores the server moving it (in single player it would just fly it back),
     * so the pilot is told where the cruiser is now, and their player teleported, which also makes
     * the server ignore any movement from before the jump that's still on its way.
     */
    static void travel(SpaceCruiserEntity cruiser, ServerLevel target, Vec3 to) {
        float yaw = cruiser.getYRot();
        if (target == cruiser.level()) {
            List<Entity> riders = List.copyOf(cruiser.getPassengers());
            cruiser.teleportTo(target, to.x, to.y, to.z, Set.of(), yaw, 0.0f);
            cruiser.setDeltaMovement(Vec3.ZERO);
            for (Entity rider : riders) {
                if (rider instanceof ServerPlayer player && player.getVehicle() == cruiser) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket(cruiser));
                    player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                }
            }
        } else {
            // another world: changing dimension takes the passengers along and seats them again
            cruiser.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(target, to, Vec3.ZERO, yaw, 0.0f,
                    net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        }
    }

    /** For the dial while flying: puts a Portal Gun from the pilot's pockets into their hand (swapping with whatever's there). */
    public static void gunToHand(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof PortalGunItem || player.getOffhandItem().getItem() instanceof PortalGunItem) return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.items.get(i).getItem() instanceof PortalGunItem) {
                ItemStack gun = inventory.items.get(i), held = inventory.items.get(inventory.selected);
                inventory.items.set(inventory.selected, gun);
                inventory.items.set(i, held);
                inventory.setChanged();
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.need_gun").withStyle(ChatFormatting.RED), true);
    }

    private static ItemStack findGun(Player player) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.getItem() instanceof PortalGunItem) return stack;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof PortalGunItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** With Auto Park and nobody at the controls: shoot at anything that threatens whoever's sitting inside. */
    static void keepSafe(SpaceCruiserEntity cruiser) {
        if (cruiser.tickCount % 10 != 0) return;
        var threats = cruiser.threats(20);
        if (threats.isEmpty()) return;
        LivingEntity target = threats.stream().min((a, b) -> Double.compare(a.distanceToSqr(cruiser), b.distanceToSqr(cruiser))).get();
        Vec3 aim = target.getEyePosition().subtract(cruiser.muzzle()).normalize();
        for (CruiserUpgrade weapon : new CruiserUpgrade[]{CruiserUpgrade.DISINTEGRATOR, CruiserUpgrade.MINIGUN, CruiserUpgrade.ROCKETS}) {
            if (cruiser.has(weapon) && shoot(cruiser, null, weapon, aim)) {
                cruiser.announceKeepingSafe();
                return;
            }
        }
    }
}
