package com.rickandmorty.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.CognitionAmplifierLayers;
import com.rickandmorty.pet.AmplifierModule;
import com.rickandmorty.pet.CognitionAmplifierItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Cognition Amplifier: on a pet's head (stretched to fit it, see {@link PetHeads}) and as
 * the item. The shell, then the glowing brain and a light per module, then the see-through visor
 * and jar.
 */
public final class AmplifierHelmet {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RickAndMortyMod.id("cognition_amplifier"), "main");
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/cognition_amplifier.png"),
            GLOW = RickAndMortyMod.id("textures/entity/cognition_amplifier_glow.png");
    private static ModelPart model;

    private AmplifierHelmet() {
    }

    public static LayerDefinition layer() {
        return CognitionAmplifierLayers.helmet();
    }

    private static ModelPart model() {
        if (model == null) model = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER);
        return model;
    }

    /** Draws it on an 8-pixel head whose top middle is at the pose's origin (y down, face towards -z). */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int modules) {
        ModelPart root = model();
        root.getChild("solid").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        ModelPart glow = root.getChild("glow");
        for (AmplifierModule module : AmplifierModule.values()) glow.getChild("light_" + module.id()).visible = module.in(modules);
        glow.render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, overlay);
        root.getChild("glass").render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), light, overlay);
    }

    /** Draws it on the pet's head, stretched to the head's size; nothing if no head can be found. */
    static void drawOn(EntityModel<?> petModel, LivingEntity pet, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int modules) {
        PetHeads.Head head = PetHeads.find(petModel).orElse(null);
        if (head == null) return;
        pose.pushPose();
        if (PetHeads.poseOnHead(head, petModel, pose)) {
            float width = head.maxX() - head.minX(), depth = head.maxZ() - head.minZ(), height = head.maxY() - head.minY();
            pose.translate((head.minX() + head.maxX()) / 32.0f, head.minY() / 16.0f, (head.minZ() + head.maxZ()) / 32.0f);
            // as tall as the head, but never squashed flatter than it is narrow, nor taller than it is long
            float tall = Math.max(Math.min(height, Math.max(width, depth)), Math.min(width, depth));
            pose.scale(width / 8.0f, tall / 8.0f, depth / 8.0f);
            draw(pose, buffers, light, overlay, modules);
        }
        pose.popPose();
    }

    /** The helmet in your hand and inventory, its module lights on. */
    public static class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(0.5f, 0.62f, 0.5f);
            pose.scale(-1.35f, -1.35f, 1.35f);
            pose.translate(0.0f, -0.1f, 0.0f);
            draw(pose, buffers, light, overlay, CognitionAmplifierItem.modules(stack));
            pose.popPose();
        }
    }
}
