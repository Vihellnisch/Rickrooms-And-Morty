package com.rickandmorty.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.cronenberg.MutationClientState;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.network.GunModePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The Portal Gun's mode key: switches a gun with the Blender Upgrade between travel and attack mode. */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class ClientKeys {
    public static final KeyMapping GUN_MODE = new KeyMapping("key.rickrooms.gun_mode", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.rickrooms");

    private ClientKeys() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (GUN_MODE.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            for (InteractionHand hand : InteractionHand.values()) {
                if (mc.player.getItemInHand(hand).getItem() instanceof PortalGunItem) {
                    PacketDistributor.sendToServer(new GunModePayload(hand));
                    break;
                }
            }
        }
    }

    /** Entity ids mean nothing on the next server. */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MutationClientState.clear();
        com.rickandmorty.pet.AmplifierClientState.clear();
    }
}
