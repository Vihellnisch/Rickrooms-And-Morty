package com.rickandmorty.cronenberg;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.network.MutationPayload;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModArmorMaterials;
import com.rickandmorty.registry.ModAttachments;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cronenberg World gets into you. Mutation (0..100) creeps up while you're there (faster standing
 * in its blood), when Cronenbergs hit you, and when you eat their meat. Each quarter is a stage:
 * <ol>
 *   <li>25: twitchy and quick, but always hungry;</li>
 *   <li>50: tougher skin, harder hits, eyes that see in the dark, lumps growing on you;</li>
 *   <li>75: stronger still, and the Cronenbergs think you're one of them... but the nausea;</li>
 *   <li>100: fully Cronenberged. Your body starts rejecting itself.</li>
 * </ol>
 * Survivor Armor keeps the world out (a quarter per piece). A Cure Serum, or dying, resets it.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class Mutation {
    public static final float MAX = 100.0f;
    /** Ambient gain in Cronenberg World: full after about 50 minutes. */
    private static final float AMBIENT_PER_SECOND = 1.0f / 30.0f;
    private static final float BLOOD_PER_SECOND = 0.5f;
    private static final float[] EXHAUSTION = {0.0f, 0.05f, 0.12f, 0.2f, 0.3f};

    private static final ResourceLocation SPEED = RickAndMortyMod.id("mutation_speed");
    private static final ResourceLocation DAMAGE = RickAndMortyMod.id("mutation_damage");
    private static final ResourceLocation ARMOR = RickAndMortyMod.id("mutation_armor");
    private static final ResourceLocation HEALTH = RickAndMortyMod.id("mutation_health");

    private Mutation() {
    }

    public static float get(Player player) {
        return player.getData(ModAttachments.MUTATION);
    }

    /** 0 (none) to 4 (fully Cronenberged). */
    public static int stage(float value) {
        return value >= MAX ? 4 : (int) (value / 25.0f);
    }

    public static int stage(Player player) {
        return stage(get(player));
    }

    public static void set(ServerPlayer player, float value) {
        float before = get(player);
        float after = Mth.clamp(value, 0.0f, MAX);
        player.setData(ModAttachments.MUTATION, after);
        int oldStage = stage(before), newStage = stage(after);
        if (newStage != oldStage) {
            if (newStage > oldStage) {
                player.displayClientMessage(Component.translatable("message.rickrooms.mutation.stage_" + newStage)
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
            if (newStage == 4) ModAdvancements.award(player, "cronenberged");
            applyStage(player, newStage);
        }
        // only bother the network with changes you could see on the meter
        if (newStage != oldStage || Math.abs(Mth.ceil(after / 5.0f) - Mth.ceil(before / 5.0f)) > 0 || after == 0.0f && before != 0.0f) {
            sync(player);
        }
    }

    public static void add(ServerPlayer player, float amount) {
        if (!ModConfig.MUTATION_ENABLED.get()) return;
        set(player, get(player) + amount);
    }

    /** Mutation from the world itself (air, blood, blows): Survivor Armor blocks a quarter per piece. */
    public static void expose(ServerPlayer player, float amount) {
        add(player, amount * (1.0f - 0.25f * survivorPieces(player)));
    }

    public static int survivorPieces(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor && armor.getMaterial().is(ModArmorMaterials.SURVIVOR.getKey())) pieces++;
        }
        return pieces;
    }

    static void sync(ServerPlayer player) {
        if (player instanceof FakePlayer || player.connection == null) return;
        MutationPayload payload = new MutationPayload(player.getId(), get(player));
        if (player.connection.hasChannel(MutationPayload.TYPE)) PacketDistributor.sendToPlayer(player, payload);
        // anyone near enough to see them (only connections that speak this mod's protocol)
        double range = player.serverLevel().getServer().getPlayerList().getViewDistance() * 16.0;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && !(other instanceof FakePlayer) && other.connection != null && other.connection.hasChannel(MutationPayload.TYPE)
                    && other.distanceToSqr(player) < range * range) {
                PacketDistributor.sendToPlayer(other, payload);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        float value = get(player);
        if (!ModConfig.MUTATION_ENABLED.get() && value > 0.0f) {
            set(player, 0.0f);
            value = 0.0f;
        }
        if (!player.isCreative() && !player.isSpectator() && player.level().dimension() == ModDimensions.CRONENBERG) {
            float gain = AMBIENT_PER_SECOND + (player.isInWater() ? BLOOD_PER_SECOND : 0.0f);
            expose(player, gain * ModConfig.MUTATION_RATE.get().floatValue());
            value = get(player);
        }
        int stage = stage(value);
        applyStage(player, stage);
        if (stage == 0) return;
        player.causeFoodExhaustion(EXHAUSTION[stage] * 4.0f);
        if (stage >= 2) {
            MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
            if (vision == null || vision.getDuration() < 260) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 420, 0, true, false, false));
            }
        }
        if (stage >= 3 && player.getRandom().nextInt(60) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, true, false, true));
        }
        if (stage >= 4 && player.getRandom().nextInt(45) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, true, true, true));
            player.displayClientMessage(Component.translatable("message.rickrooms.mutation.rejecting").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    /** Stat changes for each stage, as transient modifiers (recomputed, never saved). */
    private static void applyStage(ServerPlayer player, int stage) {
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED, stage >= 1 ? 0.1 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player, Attributes.ATTACK_DAMAGE, DAMAGE, stage >= 3 ? 4.0 : stage >= 2 ? 2.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.ARMOR, ARMOR, stage >= 3 ? 4.0 : stage >= 2 ? 2.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.MAX_HEALTH, HEALTH, stage >= 3 ? 4.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void modifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (amount == 0.0) {
            instance.removeModifier(id);
        } else {
            AttributeModifier existing = instance.getModifier(id);
            if (existing == null || existing.amount() != amount) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
            }
        }
    }

    /** Eating Cronenberg meat is exactly as bad an idea as it sounds. */
    @SubscribeEvent
    public static void onItemEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();
        if (item.is(ModItems.CRONENBERG_FLESH.get())) add(player, 8.0f);
        else if (item.is(ModItems.COOKED_CRONENBERG_FLESH.get())) add(player, 3.0f);
        else if (item.is(ModItems.BEHEMOTH_HEART.get())) add(player, 40.0f);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher
                && !(watcher instanceof FakePlayer) && watcher.connection != null && watcher.connection.hasChannel(MutationPayload.TYPE)) {
            PacketDistributor.sendToPlayer(watcher, new MutationPayload(target.getId(), get(target)));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyStage(player, stage(player));
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyStage(player, stage(player));
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }
}
