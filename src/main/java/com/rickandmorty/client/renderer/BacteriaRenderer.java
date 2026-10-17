package com.rickandmorty.client.renderer;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.BacteriaModel;
import com.rickandmorty.entity.BacteriaEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders the Bacteria, with eyes and mouth that glow in the dark. */
public class BacteriaRenderer extends MobRenderer<BacteriaEntity, BacteriaModel> {
    private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/bacteria.png");
    private static final RenderType EYES = RenderType.eyes(RickAndMortyMod.id("textures/entity/bacteria_eyes.png"));

    public BacteriaRenderer(EntityRendererProvider.Context context) {
        super(context, new BacteriaModel(context.bakeLayer(BacteriaModel.LAYER)), 0.5f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(BacteriaEntity entity) {
        return TEXTURE;
    }
}
