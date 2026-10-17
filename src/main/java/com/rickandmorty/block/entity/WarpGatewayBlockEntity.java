package com.rickandmorty.block.entity;

import com.rickandmorty.block.GatewayStructure;
import com.rickandmorty.block.WarpGatewayBlock;
import com.rickandmorty.entity.PortalTeleporter;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Stores which gateway this one is linked to. Both gateways of a pair point at each other;
 * while linked, the blockstate is ACTIVE and anything walking into the ring is sent across.
 */
public class WarpGatewayBlockEntity extends BlockEntity {
    @Nullable
    private GlobalPos partner;

    /** Client only: game time when the portal was first seen open, for the opening animation. */
    public long clientOpenedAt = -1;

    // --- evil gateways (see EvilGateway): armed, whose it is, and what it's doing (the last three are synced)
    boolean evil;
    @Nullable
    UUID owner;
    int phase = EvilGateway.IDLE;
    int phaseTicks;
    long phaseStart;
    int victimId = -1;
    int side = 1;
    /** Loot (or belongings) of whatever it's eating, to spit out later. */
    final List<ItemStack> eaten = new ArrayList<>();

    public WarpGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARP_GATEWAY.get(), pos, state);
    }

    public @Nullable GlobalPos getPartner() {
        return partner;
    }

    public GlobalPos here() {
        return GlobalPos.of(level.dimension(), worldPosition);
    }

    /** Looks up a gateway anywhere on the server (loads its chunk if needed). */
    public static @Nullable WarpGatewayBlockEntity get(MinecraftServer server, GlobalPos pos) {
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null) return null;
        return level.getBlockEntity(pos.pos()) instanceof WarpGatewayBlockEntity gateway ? gateway : null;
    }

    /** Links two gateways, disconnecting whatever they were linked to before. */
    public static boolean link(MinecraftServer server, GlobalPos a, GlobalPos b) {
        WarpGatewayBlockEntity first = get(server, a);
        WarpGatewayBlockEntity second = get(server, b);
        if (first == null || second == null || a.equals(b)) return false;
        first.disconnect(server);
        second.disconnect(server);
        first.setPartner(b);
        second.setPartner(a);
        return true;
    }

    /** Unlinks this gateway and its partner. */
    public void disconnect(MinecraftServer server) {
        breakLink(server);
        setPartner(null);
    }

    /** Called when this gateway is being destroyed: only tells the partner, since our own block is going away. */
    public void breakLink(MinecraftServer server) {
        if (partner == null) return;
        WarpGatewayBlockEntity other = get(server, partner);
        if (other != null && here().equals(other.partner)) {
            other.setPartner(null);
        }
        partner = null;
    }

    private void setPartner(@Nullable GlobalPos newPartner) {
        partner = newPartner;
        setChanged();
        BlockState state = getBlockState();
        boolean active = newPartner != null;
        if (state.getValue(WarpGatewayBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(WarpGatewayBlock.ACTIVE, active), Block.UPDATE_ALL);
            level.playSound(null, worldPosition, active ? ModSounds.PORTAL_OPEN.get() : ModSounds.PORTAL_GUN_EMPTY.get(),
                    SoundSource.BLOCKS, 1.0f, active ? 0.7f : 0.6f);
        }
    }

    public Component describeLink() {
        if (partner == null) {
            return Component.translatable("message.rickrooms.gateway.not_linked").withStyle(ChatFormatting.YELLOW);
        }
        BlockPos p = partner.pos();
        return Component.translatable("message.rickrooms.gateway.status",
                Component.translatable("dial.rickrooms.location", PortalDestinations.dimensionName(partner.dimension()),
                        p.getX(), p.getY(), p.getZ())).withStyle(ChatFormatting.GREEN);
    }

    public boolean isEvil() {
        return evil;
    }

    public boolean isOwner(net.minecraft.world.entity.player.Player player) {
        return player.getUUID().equals(owner);
    }

    public int evilPhase() {
        return phase;
    }

    /** Client: ticks (with partial tick) since the evil gateway's current phase began. */
    public float evilPhaseAge(float partialTick) {
        return level == null ? 0 : level.getGameTime() - phaseStart + partialTick;
    }

    public @Nullable LivingEntity evilVictim() {
        return level != null && victimId >= 0 && level.getEntity(victimId) instanceof LivingEntity living ? living : null;
    }

    /** Sends the evil state to clients (for the arms). */
    void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WarpGatewayBlockEntity gateway) {
        if (gateway.evil) {
            EvilGateway.tick((ServerLevel) level, pos, state, gateway);   // an evil gateway sends nobody anywhere
            return;
        }
        if (gateway.partner == null || !state.getValue(WarpGatewayBlock.ACTIVE)) return;
        ServerLevel from = (ServerLevel) level;
        Direction facing = state.getValue(WarpGatewayBlock.FACING);
        List<Entity> touching = level.getEntities((Entity) null, GatewayStructure.portalZone(pos, facing), Entity::isAlive);
        if (touching.isEmpty()) return;

        // Make sure the other side still exists before sending anyone through.
        WarpGatewayBlockEntity other = get(from.getServer(), gateway.partner);
        if (other == null || !gateway.here().equals(other.partner)) {
            gateway.setPartner(null);
            return;
        }
        ServerLevel target = (ServerLevel) other.level;
        Direction exitFacing = other.getBlockState().getValue(WarpGatewayBlock.FACING);
        Vec3 arrival = GatewayStructure.arrivalPoint(other.worldPosition, exitFacing);
        for (Entity entity : touching) {
            if (PortalTeleporter.canTeleport(entity, from, target)) {
                PortalTeleporter.teleport(entity, from, target, arrival, exitFacing.toYRot());
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Evil", evil);
        tag.putInt("Phase", phase);
        tag.putLong("PhaseStart", phaseStart);
        tag.putInt("Victim", victimId);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readSync(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        readSync(packet.getTag());
    }

    private void readSync(CompoundTag tag) {
        evil = tag.getBoolean("Evil");
        phase = tag.getInt("Phase");
        phaseStart = tag.getLong("PhaseStart");
        victimId = tag.contains("Victim") ? tag.getInt("Victim") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (evil) tag.putBoolean("Evil", true);
        if (owner != null) tag.putUUID("Owner", owner);
        if (partner != null) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, partner).result().ifPresent(t -> tag.put("Partner", t));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        evil = tag.getBoolean("Evil");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        partner = tag.contains("Partner")
                ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Partner")).result().orElse(null)
                : null;
    }
}
