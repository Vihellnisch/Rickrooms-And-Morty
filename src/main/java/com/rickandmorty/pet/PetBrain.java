package com.rickandmorty.pet;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.network.PetAmplifierPayload;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A pet wearing a Cognition Amplifier: tougher, faster, careful where it walks, opens doors, fights
 * for its owner whatever kind of animal it is (see the goals in pet/goal), and talks
 * ({@link PetSpeech}). Which pets: anything with an owner (tamed wolves, cats, parrots, horses,
 * llamas, camels, and other mods' pets built the same way) and foxes that trust you.
 * <p>
 * Kept in the pet's own saved data, so it survives reloads, shoulder rides and other dimensions.
 */
public final class PetBrain {
    private static final String KEY = RickAndMortyMod.MOD_ID + ":amplifier";
    public static final int MAX_CARRIED = 9;
    private static final ResourceLocation HEALTH = RickAndMortyMod.id("amplifier_health"), SPEED = RickAndMortyMod.id("amplifier_speed"),
            FLYING = RickAndMortyMod.id("amplifier_flying"), ARMOR = RickAndMortyMod.id("amplifier_armor"),
            KNOCKBACK = RickAndMortyMod.id("amplifier_knockback"), FOLLOW = RickAndMortyMod.id("amplifier_follow");
    /** Where a smart pet won't walk: into fire or next to it, onto cactus and berry bushes, into powder snow. */
    private static final PathType[] CAREFUL = {PathType.DAMAGE_FIRE, PathType.DANGER_FIRE, PathType.DAMAGE_OTHER, PathType.DANGER_OTHER,
            PathType.POWDER_SNOW, PathType.DANGER_POWDER_SNOW, PathType.DAMAGE_CAUTIOUS};

    private PetBrain() {
    }

    /** Can this wear a Cognition Amplifier at all? */
    public static boolean canWear(Entity entity) {
        return entity instanceof Mob && !(entity instanceof Player) && (entity instanceof OwnableEntity || entity instanceof Fox) && entity.isAlive();
    }

    public static boolean isAmplified(Entity entity) {
        return entity instanceof Mob && entity.getPersistentData().contains(KEY, Tag.TAG_COMPOUND);
    }

    private static CompoundTag data(Entity entity) {
        return entity.getPersistentData().getCompound(KEY);
    }

    /** Its modules, or -1 if it isn't wearing a helmet. */
    public static int modules(Entity entity) {
        return isAmplified(entity) ? data(entity).getInt("modules") : -1;
    }

    public static boolean has(Entity entity, AmplifierModule module) {
        int modules = modules(entity);
        return modules >= 0 && module.in(modules);
    }

    /** Whose pet it is: who fitted the helmet (or, before that, its owner). */
    @Nullable
    public static UUID ownerId(Mob mob) {
        CompoundTag data = data(mob);
        if (data.hasUUID("owner")) return data.getUUID("owner");
        return mob instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
    }

    /** Its owner, if they're in the same world. */
    @Nullable
    public static Player owner(Mob mob) {
        UUID id = ownerId(mob);
        return id == null ? null : mob.level().getPlayerByUUID(id);
    }

    public static boolean ownedBy(Mob mob, Player player) {
        return player.getUUID().equals(ownerId(mob));
    }

    /** Is it this player's to put a helmet on: tamed by them, or (a fox) trusting them? */
    public static boolean belongsTo(Mob mob, Player player) {
        if (mob instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) return true;
        return mob instanceof Fox fox && fox.trusts(player.getUUID());
    }

    /** Puts a helmet with these modules on the pet. */
    public static void fit(Mob mob, Player owner, int modules) {
        CompoundTag data = new CompoundTag();
        data.putInt("modules", modules & AmplifierModule.ALL);
        data.putUUID("owner", owner.getUUID());
        mob.getPersistentData().put(KEY, data);
        if (!mob.hasCustomName()) mob.setCustomName(Component.literal(PetSpeech.chooseName(mob)));
        mob.setPersistenceRequired();
        refresh(mob);
        mob.setHealth(mob.getMaxHealth());
        sync(mob);
        if (owner instanceof ServerPlayer player) {
            ModAdvancements.award(player, "lawnmower_dog");
            if ((modules & AmplifierModule.ALL) == AmplifierModule.ALL) ModAdvancements.award(player, "smarter_than_rick");
        }
        PetSpeech.say(mob, PetSpeech.Topic.FITTED, true);
    }

    /** Takes the helmet off (dropping whatever it was carrying) and returns it, modules and all. */
    public static ItemStack remove(Mob mob) {
        int modules = modules(mob);
        if (modules < 0) return ItemStack.EMPTY;
        for (ItemStack stack : carried(mob)) mob.spawnAtLocation(stack);
        mob.getPersistentData().remove(KEY);
        refresh(mob);
        sync(mob);
        ItemStack helmet = new ItemStack(ModItems.COGNITION_AMPLIFIER.get());
        if (modules != 0) helmet.set(ModDataComponents.AMPLIFIER_MODULES.get(), modules);
        return helmet;
    }

    /** Fits a module; false if it already has it (or has no helmet). */
    public static boolean addModule(Mob mob, AmplifierModule module) {
        int modules = modules(mob);
        if (modules < 0 || module.in(modules)) return false;
        CompoundTag data = data(mob);
        data.putInt("modules", modules | module.bit());
        mob.getPersistentData().put(KEY, data);
        refresh(mob);
        sync(mob);
        if ((modules | module.bit()) == AmplifierModule.ALL && owner(mob) instanceof ServerPlayer player) {
            ModAdvancements.award(player, "smarter_than_rick");
        }
        return true;
    }

    /** Sets its stats and pathfinding for whether it's amplified: on fitting, removing, and whenever it loads. */
    public static void refresh(Mob mob) {
        int modules = modules(mob);
        boolean on = modules >= 0, combat = on && AmplifierModule.COMBAT.in(modules);
        modifier(mob, Attributes.MAX_HEALTH, HEALTH, on ? 0.5 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(mob, Attributes.MOVEMENT_SPEED, SPEED, on ? 0.15 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(mob, Attributes.FLYING_SPEED, FLYING, on ? 0.15 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(mob, Attributes.ARMOR, ARMOR, on ? (combat ? 8 : 3) : 0, AttributeModifier.Operation.ADD_VALUE);
        modifier(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, combat ? 0.4 : 0, AttributeModifier.Operation.ADD_VALUE);
        modifier(mob, Attributes.FOLLOW_RANGE, FOLLOW, on ? 16 : 0, AttributeModifier.Operation.ADD_VALUE);
        if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
        if (mob.getNavigation() instanceof GroundPathNavigation ground) ground.setCanOpenDoors(on);
        if (on) {
            // only ever more careful than it was (-1 is "never"), so a mob's own cautions stay
            for (PathType type : CAREFUL) {
                float now = mob.getPathfindingMalus(type);
                boolean damaging = type == PathType.DAMAGE_FIRE || type == PathType.DAMAGE_OTHER || type == PathType.POWDER_SNOW;
                if (now >= 0) mob.setPathfindingMalus(type, damaging ? -1.0f : Math.max(now, 16.0f));
            }
        }
    }

    private static void modifier(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    /** How hard it hits: half as hard again as it would (at least 3 to start with), twice that with the Combat Module. */
    public static float damage(Mob mob) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double base = Math.max(3.0, attack == null ? 0.0 : attack.getValue());
        return (float) (base * 1.5 * (has(mob, AmplifierModule.COMBAT) ? 2.0 : 1.0));
    }

    /** Tells everyone who can see it whether it's wearing a helmet (and which lights to show). */
    public static void sync(Mob mob) {
        sendNear(mob, new PetAmplifierPayload(mob.getId(), modules(mob)), 160.0);
    }

    /** Sends to the players near enough to see the pet (who have this mod; new arrivals catch up as they see it). */
    static void sendNear(Mob mob, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload, double range) {
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (ServerPlayer player : level.players()) {
            if (player instanceof net.neoforged.neoforge.common.util.FakePlayer || player.connection == null
                    || !player.connection.hasChannel(payload.type()) || player.distanceToSqr(mob) > range * range) continue;
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    // ------------------------------------------------------------------ the Fetch Module's pockets

    public static List<ItemStack> carried(Mob mob) {
        List<ItemStack> items = new ArrayList<>();
        ListTag list = data(mob).getList("carried", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(mob.registryAccess(), list.getCompound(i));
            if (!stack.isEmpty()) items.add(stack);
        }
        return items;
    }

    private static void setCarried(Mob mob, List<ItemStack> items) {
        if (!isAmplified(mob)) return;
        CompoundTag data = data(mob);
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) list.add(stack.save(mob.registryAccess()));
        }
        data.put("carried", list);
        mob.getPersistentData().put(KEY, data);
    }

    /** Is there room for (some of) this? */
    public static boolean hasRoomFor(Mob mob, ItemStack stack) {
        List<ItemStack> items = carried(mob);
        if (items.size() < MAX_CARRIED) return true;
        for (ItemStack held : items) {
            if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) return true;
        }
        return false;
    }

    /** Picks up as much of the item as it has room for. */
    public static void pickUp(Mob mob, ItemEntity item) {
        List<ItemStack> items = carried(mob);
        ItemStack stack = item.getItem().copy();
        for (ItemStack held : items) {
            if (stack.isEmpty()) break;
            if (ItemStack.isSameItemSameComponents(held, stack)) {
                int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                held.grow(moved);
                stack.shrink(moved);
            }
        }
        if (!stack.isEmpty() && items.size() < MAX_CARRIED) {
            items.add(stack);
            stack = ItemStack.EMPTY;
        }
        mob.take(item, item.getItem().getCount() - stack.getCount());
        if (stack.isEmpty()) item.discard();
        else item.setItem(stack);
        setCarried(mob, items);
    }

    /** Hands everything it's carrying to the player (what doesn't fit lands at their feet). */
    public static void deliver(Mob mob, Player player) {
        for (ItemStack stack : carried(mob)) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        setCarried(mob, List.of());
    }

    // ------------------------------------------------------------------ the Medic Module's cooldown

    public static long medicAt(Mob mob) {
        return data(mob).getLong("medic_at");
    }

    public static void setMedicAt(Mob mob, long time) {
        if (!isAmplified(mob)) return;
        CompoundTag data = data(mob);
        data.putLong("medic_at", time);
        mob.getPersistentData().put(KEY, data);
    }
}
