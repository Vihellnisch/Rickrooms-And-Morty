package com.rickandmorty.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.pet.AmplifierClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

import java.util.List;

/** What an amplified pet just said, in a white bubble over its head, facing you. */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, value = Dist.CLIENT)
public final class PetSpeechBubbles {
    private static final int WIDTH = 170, TEXT = 0xFF1E2A2E, BUBBLE = 0xF2F6FFFE;

    private PetSpeechBubbles() {
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Component line = AmplifierClientState.saying(entity.getId());
        Minecraft mc = Minecraft.getInstance();
        if (line == null || mc.options.hideGui || mc.player == null || mc.player.distanceToSqr(entity) > 32 * 32) return;
        Font font = mc.font;
        List<FormattedCharSequence> lines = font.split(line, WIDTH);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() + 0.4, 0.0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = pose.last().pose();
        int widest = 0;
        for (FormattedCharSequence text : lines) widest = Math.max(widest, font.width(text));
        int top = -lines.size() * 10 - 4, x0 = -widest / 2 - 4, x1 = widest / 2 + 4, y0 = top - 3, y1 = top + lines.size() * 10 + 1;
        // the bubble: a box with its corners clipped, and a stepped tail pointing down at the pet
        VertexConsumer bubble = event.getMultiBufferSource().getBuffer(RenderType.textBackground());
        rect(bubble, matrix, x0 + 1, y0, x1 - 1, y1);
        rect(bubble, matrix, x0, y0 + 1, x1, y1 - 1);
        rect(bubble, matrix, -4, y1, 3, y1 + 2);
        rect(bubble, matrix, -2, y1 + 2, 1, y1 + 4);
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence text = lines.get(i);
            font.drawInBatch(text, -font.width(text) / 2.0f, top + i * 10, TEXT, false, matrix, event.getMultiBufferSource(),
                    Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    private static void rect(VertexConsumer out, Matrix4f matrix, float x0, float y0, float x1, float y1) {
        out.addVertex(matrix, x0, y0, 0.0f).setColor(BUBBLE).setLight(LightTexture.FULL_BRIGHT);
        out.addVertex(matrix, x0, y1, 0.0f).setColor(BUBBLE).setLight(LightTexture.FULL_BRIGHT);
        out.addVertex(matrix, x1, y1, 0.0f).setColor(BUBBLE).setLight(LightTexture.FULL_BRIGHT);
        out.addVertex(matrix, x1, y0, 0.0f).setColor(BUBBLE).setLight(LightTexture.FULL_BRIGHT);
    }
}
