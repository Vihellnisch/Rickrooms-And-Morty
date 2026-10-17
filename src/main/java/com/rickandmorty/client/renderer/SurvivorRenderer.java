package com.rickandmorty.client.renderer;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.SurvivorEntity;
import com.rickandmorty.entity.SurvivorKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Jerry, Beth, Summer, and Cronenberg Rick and Morty: player-shaped, in their own skins. */
public class SurvivorRenderer extends HumanoidMobRenderer<SurvivorEntity, PlayerModel<SurvivorEntity>> {
    private final ResourceLocation texture;

    public SurvivorRenderer(EntityRendererProvider.Context context, SurvivorKind kind) {
        super(context, new PlayerModel<>(context.bakeLayer(kind.slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), kind.slim), 0.5f);
        this.texture = RickAndMortyMod.id("textures/entity/" + kind.id() + ".png");
        if (kind.mutated()) MutationLayer.attach(this, context.getModelSet(), e -> 4);
    }

    public static EntityRendererProvider<SurvivorEntity> provider(SurvivorKind kind) {
        return context -> new SurvivorRenderer(context, kind);
    }

    @Override
    public ResourceLocation getTextureLocation(SurvivorEntity entity) {
        return texture;
    }
}
