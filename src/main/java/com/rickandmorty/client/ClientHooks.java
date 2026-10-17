package com.rickandmorty.client;

import com.rickandmorty.client.screen.PortalGunScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * Client-only calls made from common code. Keeping them in this class means the
 * dedicated server never loads client classes like Minecraft or Screen.
 */
public class ClientHooks {
    public static void openPortalGunScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new PortalGunScreen(hand));
    }

    /** Rick's Field Guide, as a book you can page through. */
    public static void openFieldGuide() {
        java.util.List<net.minecraft.network.chat.Component> pages = new java.util.ArrayList<>();
        for (int i = 0; i < com.rickandmorty.item.FieldGuideItem.PAGES; i++) {
            pages.add(net.minecraft.network.chat.Component.translatable("guide.rickrooms.page." + i));
        }
        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
                new net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess(pages)));
    }
}
