package com.rickandmorty.block.entity;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.GatewayFrameBlock;
import com.rickandmorty.block.GatewayStructure;
import com.rickandmorty.block.WarpGatewayBlock;
import com.rickandmorty.entity.BlenderGrip;
import com.rickandmorty.entity.Gore;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An evil Warp Gateway (armed with a Portal Gun in attack mode). It looks exactly like any other
 * gateway, but anything alive that comes close makes its lights flare red: mechanical tentacles
 * haul the victim up in front of the ring and saw arms tear into it, much harder than a blender
 * portal. Then:
 * <ul>
 *   <li>a mob's corpse is sucked into the ring, chewed, and spat back out as bones and its loot;</li>
 *   <li>a player (or a Council member, an alternate version of one) is swallowed whole and the gateway can't take it: it flickers red and green,
 *       faster and faster, then collapses in on itself and explodes, taking a huge bite out of the world.</li>
 * </ul>
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class EvilGateway {
    public static final int IDLE = 0, GRAB = 1, BLEND = 2, DEVOUR = 3, RETRACT = 4, MELTDOWN = 5;
    public static final int GRAB_TICKS = 16;
    public static final int BLEND_TICKS = 100;
    public static final int DEVOUR_TICKS = 50;
    public static final int RETRACT_TICKS = 14;
    public static final int MELTDOWN_TICKS = 140;
    public static final double REACH = 3.8;
    private static final float DAMAGE = 6.0f;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, RickAndMortyMod.id("evil_gateway"));
    /** Who each gateway is chewing on, so their loot can be caught when they die. */
    private static final Map<UUID, GlobalPos> VICTIMS = new ConcurrentHashMap<>();

    private EvilGateway() {
    }

    public static Vec3 center(BlockPos controller, Direction facing) {
        return GatewayStructure.toWorld(controller, facing, GatewayStructure.RING_X, GatewayStructure.RING_Y, 0.5);
    }

    public static Vec3 normal(Direction facing) {
        return new Vec3(facing.getStepX(), 0, facing.getStepZ());
    }

    /** Arm or disarm a gateway with a Portal Gun in attack mode. */
    public static void toggle(ServerLevel level, WarpGatewayBlockEntity gateway, Player player) {
        if (gateway.phase != IDLE) return;       // not while it's eating
        gateway.evil = !gateway.evil;
        gateway.owner = gateway.evil ? player.getUUID() : null;
        gateway.setChanged();
        gateway.sync();
        level.playSound(null, gateway.getBlockPos(), gateway.evil ? ModSounds.BLENDER_GRAB.get() : ModSounds.PORTAL_GUN_RELOAD.get(),
                SoundSource.BLOCKS, 1.0f, gateway.evil ? 0.6f : 1.2f);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(gateway.evil
                ? "message.rickrooms.gateway.armed" : "message.rickrooms.gateway.disarmed")
                .withStyle(gateway.evil ? net.minecraft.ChatFormatting.DARK_RED : net.minecraft.ChatFormatting.GREEN), true);
        if (gateway.evil && player instanceof ServerPlayer serverPlayer) ModAdvancements.award(serverPlayer, "evil_gateway");
    }

    // ------------------------------------------------------------------ ticking

    static void tick(ServerLevel level, BlockPos pos, BlockState state, WarpGatewayBlockEntity g) {
        Direction facing = state.getValue(WarpGatewayBlock.FACING);
        Vec3 center = center(pos, facing);
        Vec3 normal = normal(facing);
        LivingEntity victim = g.victimId >= 0 && level.getEntity(g.victimId) instanceof LivingEntity living ? living : null;
        switch (g.phase) {
            case IDLE -> {
                if (state.getValue(WarpGatewayBlock.RED)) setRed(level, pos, state, false);   // e.g. after a reload
                if (level.getGameTime() % 2 != 0) break;
                LivingEntity prey = null;
                double best = REACH * REACH;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(REACH),
                        e -> BlenderGrip.canBlend(e, g.owner))) {
                    double d = e.position().add(0, e.getBbHeight() / 2, 0).distanceToSqr(center);
                    if (d < best) {
                        best = d;
                        prey = e;
                    }
                }
                if (prey != null) {
                    g.victimId = prey.getId();
                    g.side = prey.position().subtract(center).dot(normal) >= 0 ? 1 : -1;
                    VICTIMS.put(prey.getUUID(), GlobalPos.of(level.dimension(), pos));
                    setRed(level, pos, state, true);
                    setPhase(level, g, GRAB);
                    level.playSound(null, center.x, center.y, center.z, ModSounds.PORTAL_OPEN.get(), SoundSource.HOSTILE, 1.5f, 0.5f);
                    level.playSound(null, center.x, center.y, center.z, ModSounds.BLENDER_GRAB.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
                }
            }
            case GRAB -> {
                if (lost(g, victim, center)) {
                    setPhase(level, g, RETRACT);
                    break;
                }
                BlenderGrip.hold(victim, BlenderGrip.holdPoint(center, normal, false, victim), Math.min(1.0f, (g.phaseTicks + 1) / (float) GRAB_TICKS),
                        false, level.random);
                if (g.phaseTicks >= GRAB_TICKS) {
                    setPhase(level, g, BLEND);
                    level.playSound(null, center.x, center.y, center.z, ModSounds.BLENDER_SAW.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
                }
            }
            case BLEND -> {
                if (victim != null && !victim.isAlive()) {
                    // dead: the ending depends on what it caught
                    Gore.burst(victim);
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.BLENDER_GORE.get(), SoundSource.HOSTILE, 2.5f, 0.5f);
                    if (g.owner != null && level.getPlayerByUUID(g.owner) instanceof ServerPlayer shooter) ModAdvancements.award(shooter, "blended");
                    setPhase(level, g, countsAsPlayer(victim) ? MELTDOWN : DEVOUR);
                    break;
                }
                if (lost(g, victim, center)) {
                    setPhase(level, g, RETRACT);
                    break;
                }
                BlenderGrip.hold(victim, BlenderGrip.holdPoint(center, normal, false, victim), 1.0f, true, level.random);
                if (g.phaseTicks % 5 == 0) {
                    Entity cause = g.owner != null ? level.getPlayerByUUID(g.owner) : null;
                    var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE);
                    victim.invulnerableTime = 0;
                    victim.hurt(new DamageSource(type, null, cause, center), DAMAGE);
                    Gore.splash(level, victim.position().add(0, victim.getBbHeight() * 0.5, 0), 20, victim.getBbWidth() * 0.5);
                }
                if (g.phaseTicks % 8 == 0) {
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.BLENDER_SAW.get(), SoundSource.HOSTILE, 1.6f,
                            0.7f + level.random.nextFloat() * 0.3f);
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.BLENDER_GORE.get(), SoundSource.HOSTILE, 1.3f,
                            0.6f + level.random.nextFloat() * 0.4f);
                }
                if (g.phaseTicks >= BLEND_TICKS) setPhase(level, g, RETRACT);   // too tough to finish
            }
            case DEVOUR -> devour(level, pos, state, g, center, normal, victim);
            case RETRACT -> {
                if (g.phaseTicks >= RETRACT_TICKS) {
                    if (victim != null) VICTIMS.remove(victim.getUUID());
                    g.victimId = -1;
                    g.eaten.clear();
                    setRed(level, pos, level.getBlockState(pos), false);
                    setPhase(level, g, IDLE);
                }
            }
            case MELTDOWN -> {
                if (meltdown(level, pos, g, center)) return;    // it's gone
            }
            default -> setPhase(level, g, IDLE);
        }
        g.phaseTicks++;
    }

    /**
     * Players, and Council members too: they're alternate versions of players, and the gateway
     * can't stomach them any better.
     */
    public static boolean countsAsPlayer(LivingEntity victim) {
        return victim instanceof Player || victim instanceof com.rickandmorty.entity.CouncilMemberEntity;
    }

    private static boolean lost(WarpGatewayBlockEntity g, LivingEntity victim, Vec3 center) {
        return victim == null || !BlenderGrip.canBlend(victim, g.owner) || victim.position().distanceToSqr(center) > 100.0;
    }

    /** A mob's ending: suck the corpse in, chew, spit out its bones and its loot, go back to green. */
    private static void devour(ServerLevel level, BlockPos pos, BlockState state, WarpGatewayBlockEntity g, Vec3 center, Vec3 normal,
                               LivingEntity corpse) {
        int t = g.phaseTicks;
        if (corpse != null && !corpse.isRemoved()) {
            BlenderGrip.moveTowards(corpse, center.subtract(0, corpse.getBbHeight() * 0.5, 0), 0.2f);
            level.sendParticles(Gore.BLOOD, corpse.getX(), corpse.getY() + corpse.getBbHeight() * 0.5, corpse.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            if (corpse.position().add(0, corpse.getBbHeight() * 0.5, 0).distanceToSqr(center) < 0.6) {
                corpse.discard();                      // swallowed
                level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 1.0f, 0.4f);
            }
        }
        if (t > 15 && t < 40 && t % 6 == 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.5f, 0.5f);
            level.playSound(null, center.x, center.y, center.z, ModSounds.BLENDER_SAW.get(), SoundSource.HOSTILE, 0.8f, 0.5f);
        }
        if (t >= DEVOUR_TICKS) {
            if (corpse != null && !corpse.isRemoved()) corpse.discard();
            Vec3 mouth = center.add(normal.scale(g.side * 0.8));
            int bones = 2 + level.random.nextInt(4);
            spit(level, mouth, normal.scale(g.side), new ItemStack(Items.BONE, bones));
            if (level.random.nextFloat() < 0.35f) spit(level, mouth, normal.scale(g.side), new ItemStack(Items.BONE_MEAL, 1 + level.random.nextInt(3)));
            for (ItemStack loot : g.eaten) spit(level, mouth, normal.scale(g.side), loot);
            g.eaten.clear();
            level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 2.0f, 0.6f);
            Gore.splash(level, mouth, 30, 0.4);
            if (corpse != null) VICTIMS.remove(corpse.getUUID());
            g.victimId = -1;
            setRed(level, pos, level.getBlockState(pos), false);
            setPhase(level, g, RETRACT);
        }
    }

    private static void spit(ServerLevel level, Vec3 at, Vec3 out, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack.copy());
        item.setDeltaMovement(out.x * 0.35 + (level.random.nextDouble() - 0.5) * 0.2, 0.25 + level.random.nextDouble() * 0.15,
                out.z * 0.35 + (level.random.nextDouble() - 0.5) * 0.2);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
    }

    /**
     * A player's ending. Returns true once the gateway has blown itself up. The lights flicker between
     * red and green faster and faster, everything nearby gets dragged towards it, it collapses in on
     * itself... and explodes.
     */
    private static boolean meltdown(ServerLevel level, BlockPos pos, WarpGatewayBlockEntity g, Vec3 center) {
        int t = g.phaseTicks;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.WARP_GATEWAY.get())) return true;
        int interval = Math.max(2, 18 - t / 8);
        if (t % interval == 0) {
            setRed(level, pos, state, !state.getValue(WarpGatewayBlock.RED));
            level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0f, 0.5f + t / 140.0f);
        }
        if (t % 20 == 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0f, 0.5f + t / 200.0f);
        }
        // the ring shudders and bleeds light
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 4 + t / 10, 0.8, 0.8, 0.8, 0.05);
        if (t > MELTDOWN_TICKS - 60) {
            // collapsing in on itself: everything streams into the middle, and gets dragged along with it
            for (int i = 0; i < 12; i++) {
                double a = level.random.nextDouble() * Math.PI * 2, b = level.random.nextDouble() * Math.PI - Math.PI / 2, r = 3 + level.random.nextDouble() * 4;
                level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 0,
                        Math.cos(a) * Math.cos(b) * r, Math.sin(b) * r, Math.sin(a) * Math.cos(b) * r, 1.0);
            }
            double pull = 0.04 + 0.1 * (t - (MELTDOWN_TICKS - 60)) / 60.0;
            for (Entity e : level.getEntities((Entity) null, new AABB(center, center).inflate(16), e -> !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
                Vec3 in = center.subtract(e.position()).normalize().scale(pull);
                e.push(in.x, in.y * 0.5, in.z);
                e.hurtMarked = true;
            }
        }
        if (t == MELTDOWN_TICKS - 35) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 4.0f, 0.6f);
        }
        if (t < MELTDOWN_TICKS) return false;

        // BOOM. The gateway goes with it (no drop), and the player's things are spat out into the crater.
        java.util.List<ItemStack> belongings = new java.util.ArrayList<>(g.eaten);
        g.eaten.clear();
        VICTIMS.values().removeIf(p -> p.pos().equals(pos) && p.dimension() == level.dimension());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 6.0f, 0.5f);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        float power = ModConfig.GATEWAY_EXPLOSION.get().floatValue();
        if (power > 0) level.explode(null, center.x, center.y, center.z, power, Level.ExplosionInteraction.BLOCK);
        for (ServerPlayer witness : level.getEntitiesOfClass(ServerPlayer.class, new AABB(center, center).inflate(64))) {
            ModAdvancements.award(witness, "meltdown");
        }
        for (ItemStack stack : belongings) {
            ItemEntity item = new ItemEntity(level, center.x, center.y, center.z, stack);
            item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.3, 0.3, (level.random.nextDouble() - 0.5) * 0.3);
            item.setInvulnerable(true);
            level.addFreshEntity(item);
        }
        return true;
    }

    // ------------------------------------------------------------------ state

    private static void setPhase(ServerLevel level, WarpGatewayBlockEntity g, int phase) {
        g.phase = phase;
        g.phaseTicks = 0;
        g.phaseStart = level.getGameTime();
        g.sync();
    }

    /** Turns the gateway's lights (controller and frame) red or back to green. */
    public static void setRed(Level level, BlockPos controller, BlockState state, boolean red) {
        if (!state.is(ModBlocks.WARP_GATEWAY.get()) || state.getValue(WarpGatewayBlock.RED) == red) return;
        level.setBlock(controller, state.setValue(WarpGatewayBlock.RED, red), Block.UPDATE_CLIENTS);
        Direction facing = state.getValue(WarpGatewayBlock.FACING);
        for (int i = 0; i < GatewayStructure.PARTS.length; i++) {
            BlockPos part = GatewayStructure.partPos(controller, facing, i);
            BlockState frame = level.getBlockState(part);
            if (frame.is(ModBlocks.GATEWAY_FRAME.get()) && frame.getValue(GatewayFrameBlock.PART) == i) {
                level.setBlock(part, frame.setValue(WarpGatewayBlock.RED, red), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Whatever an evil gateway kills drops its loot into the gateway instead of onto the floor. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        GlobalPos at = VICTIMS.get(dead.getUUID());
        if (at == null || !(dead.level() instanceof ServerLevel level) || level.dimension() != at.dimension()) return;
        if (!(level.getBlockEntity(at.pos()) instanceof WarpGatewayBlockEntity gateway) || gateway.victimId != dead.getId()) return;
        for (ItemEntity drop : event.getDrops()) gateway.eaten.add(drop.getItem().copy());
        event.setCanceled(true);
        if (!countsAsPlayer(dead)) VICTIMS.remove(dead.getUUID());
    }

    /** Sets off a meltdown right away, as if it had just eaten a player (for tests and the dev tour). */
    public static void forceMeltdown(ServerLevel level, WarpGatewayBlockEntity g) {
        g.evil = true;
        setRed(level, g.getBlockPos(), g.getBlockState(), true);
        setPhase(level, g, MELTDOWN);
    }

    /** For tests and the tour: how far through its current phase a gateway is. */
    public static float phaseProgress(WarpGatewayBlockEntity g) {
        return switch (g.phase) {
            case GRAB -> g.phaseTicks / (float) GRAB_TICKS;
            case BLEND -> g.phaseTicks / (float) BLEND_TICKS;
            case DEVOUR -> g.phaseTicks / (float) DEVOUR_TICKS;
            case MELTDOWN -> g.phaseTicks / (float) MELTDOWN_TICKS;
            default -> Mth.clamp(g.phaseTicks / (float) RETRACT_TICKS, 0.0f, 1.0f);
        };
    }
}
