package com.rickandmorty.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.client.gadget.GogglesModel;
import com.rickandmorty.gadget.GogglesItem;
import com.rickandmorty.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Draws the goggles when they're worn in Curios' head slot. Only loaded when Curios is installed. */
public final class CuriosClientBridge {
    private CuriosClientBridge() {
    }

    public static void register() {
        CuriosRendererRegistry.register(ModItems.INTERDIMENSIONAL_GOGGLES.get(), GogglesRenderer::new);
    }

    /** The same 3D goggles as the helmet version, following the head; a little bigger over a helmet so they sit on top of it. */
    static final class GogglesRenderer implements ICurioRenderer {
        private final GogglesModel<LivingEntity> model;

        GogglesRenderer() {
            model = new GogglesModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesModel.LAYER));
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
                                                                              RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
                                                                              float limbSwing, float limbSwingAmount, float partialTick,
                                                                              float ageInTicks, float netHeadYaw, float headPitch) {
            LivingEntity entity = slot.entity();
            if (parent.getModel() instanceof HumanoidModel<?> humanoid) model.head.copyFrom(humanoid.head);
            else ICurioRenderer.followHeadRotations(entity, model.head);
            float scale = entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? 1.0f : 1.17f;
            model.head.xScale = scale;
            model.head.yScale = scale;
            model.head.zScale = scale;
            model.head.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(GogglesItem.TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        }
    }
}
