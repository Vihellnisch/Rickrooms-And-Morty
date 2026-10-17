package com.rickandmorty.client.cruiser;

import com.mojang.blaze3d.platform.InputConstants;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.cruiser.CruiserTier;
import com.rickandmorty.entity.cruiser.CruiserUpgrade;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import com.rickandmorty.network.CruiserActionPayload;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Flying Rick's Space Cruiser: the movement keys and the mouse steer, jump climbs, the descend key
 * drops, the attack button fires the selected weapon, and there are keys for the next weapon,
 * the repulsor and the portal drive. Also the power gauge on screen, and the engine's hum.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class CruiserControls {
    private static final String CATEGORY = "key.categories.rickrooms.cruiser";
    public static final KeyMapping DESCEND = new KeyMapping("key.rickrooms.cruiser.descend", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping NEXT_WEAPON = new KeyMapping("key.rickrooms.cruiser.next_weapon", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping REPULSOR = new KeyMapping("key.rickrooms.cruiser.repulsor", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping PORTAL = new KeyMapping("key.rickrooms.cruiser.portal", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping DIAL = new KeyMapping("key.rickrooms.cruiser.dial", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    /** Ticks left to wait for the Portal Gun to arrive in your hand before opening its dial. */
    private static int dialWait;

    private static boolean wasFiring;
    private static final Set<Integer> HUMMING = new HashSet<>();

    private CruiserControls() {
    }

    public static SpaceCruiserEntity piloting() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof SpaceCruiserEntity cruiser && cruiser.getControllingPassenger() == mc.player) {
            return cruiser;
        }
        return null;
    }

    /** In the cockpit your hands are on the controls, out of the way of the view. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof SpaceCruiserEntity) event.setCanceled(true);
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new CruiserActionPayload(action));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        SpaceCruiserEntity cruiser = piloting();
        if (cruiser == null || mc.screen != null) {
            if (wasFiring) send(CruiserActionPayload.CEASE_FIRE);
            wasFiring = false;
        } else {
            boolean firing = mc.options.keyAttack.isDown();
            if (firing != wasFiring) send(firing ? CruiserActionPayload.FIRE : CruiserActionPayload.CEASE_FIRE);
            wasFiring = firing;
            while (NEXT_WEAPON.consumeClick()) send(CruiserActionPayload.NEXT_WEAPON);
            while (REPULSOR.consumeClick()) send(CruiserActionPayload.REPULSE);
            while (PORTAL.consumeClick()) send(CruiserActionPayload.PORTAL);
            while (DIAL.consumeClick()) {
                if (!openDial(mc)) {
                    send(CruiserActionPayload.DIAL);                     // fetch it from your pockets first
                    dialWait = 20;
                }
            }
        }
        if (dialWait > 0 && --dialWait >= 0 && mc.screen == null && openDial(mc)) dialWait = 0;
        // start the hum for any cruiser nearby that doesn't have one yet
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof SpaceCruiserEntity nearby && nearby.distanceToSqr(mc.player) < 48 * 48 && HUMMING.add(nearby.getId())) {
                mc.getSoundManager().play(new Hum(nearby));
            }
        }
    }

    /** Opens the Portal Gun's dial if there's one in either hand. */
    private static boolean openDial(Minecraft mc) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            if (mc.player.getItemInHand(hand).getItem() instanceof com.rickandmorty.item.PortalGunItem) {
                mc.setScreen(new com.rickandmorty.client.screen.PortalGunScreen(hand));
                return true;
            }
        }
        return false;
    }

    /** While you're flying, the attack button fires the cruiser's weapon instead of punching. */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (piloting() != null && (event.isAttack() || event.isPickBlock())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        SpaceCruiserEntity cruiser = piloting();
        Minecraft mc = Minecraft.getInstance();
        if (cruiser == null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = 8, y = 8, w = 110;
        float power = Math.min(1.0f, cruiser.power() / (float) cruiser.maxPower());
        var title = Component.translatable("hud.rickrooms.cruiser").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" " + Math.round(power * 100) + "%").withStyle(ChatFormatting.WHITE));
        if (cruiser.booster() > 0) {
            title.append(Component.literal("  ").append(Component.translatable("hud.rickrooms.cruiser.booster", CruiserTier.roman(cruiser.booster())))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        int thrusters = cruiser.tier(CruiserTier.THRUSTERS);
        if (thrusters > 0) {
            title.append(Component.literal("  ").append(Component.translatable("hud.rickrooms.cruiser.thrusters", CruiserTier.roman(thrusters)))
                    .withStyle(ChatFormatting.AQUA));
        }
        g.drawString(mc.font, title, x, y, 0xFFFFFF);
        g.fill(x, y + 11, x + w, y + 17, 0xAA000000);
        int color = power > 0.5f ? 0xFF6CFF4A : power > 0.2f ? 0xFFFFD23A : 0xFFFF4A3A;
        g.fill(x + 1, y + 12, x + 1 + (int) ((w - 2) * power), y + 16, color);
        CruiserUpgrade weapon = cruiser.selectedWeapon();
        Component weaponName = weapon == null ? Component.translatable("hud.rickrooms.cruiser.no_weapon")
                : Component.translatable(cruiser.upgradeKey(weapon));
        g.drawString(mc.font, Component.translatable("hud.rickrooms.cruiser.weapon", weaponName), x, y + 21, 0xDDDDDD);
        g.drawString(mc.font, Component.translatable("hud.rickrooms.cruiser.keys", DESCEND.getTranslatedKeyMessage(),
                NEXT_WEAPON.getTranslatedKeyMessage(), REPULSOR.getTranslatedKeyMessage(), PORTAL.getTranslatedKeyMessage(),
                DIAL.getTranslatedKeyMessage()), x, y + 32, 0x999999);
    }

    /** The engine: a low hum that rises with speed, and fades when it's parked. */
    private static final class Hum extends AbstractTickableSoundInstance {
        private final SpaceCruiserEntity cruiser;

        Hum(SpaceCruiserEntity cruiser) {
            super(ModSounds.CRUISER_ENGINE.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.cruiser = cruiser;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = cruiser.getX();
            this.y = cruiser.getY();
            this.z = cruiser.getZ();
        }

        @Override
        public void tick() {
            if (cruiser.isRemoved()) {
                HUMMING.remove(cruiser.getId());
                stop();
                return;
            }
            x = cruiser.getX();
            y = cruiser.getY() + 0.8;
            z = cruiser.getZ();
            double speed = cruiser.getDeltaMovement().length();
            boolean running = cruiser.power() > 0 && (!cruiser.onGround() || cruiser.getControllingPassenger() != null);
            float target = running ? (float) Mth.clamp(0.35 + speed * 0.6, 0.0, 1.0) : 0.0f;
            volume = Mth.lerp(0.1f, volume, target);
            pitch = (float) Mth.clamp(0.7 + speed * 0.6, 0.5, 1.6);
        }
    }
}
