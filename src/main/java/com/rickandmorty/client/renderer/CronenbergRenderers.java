package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.CronenbergModels;
import com.rickandmorty.entity.BehemothEntity;
import com.rickandmorty.entity.CronenbergEntity;
import com.rickandmorty.entity.CronenbergKind;
import com.rickandmorty.entity.MantisEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renderers for the Cronenbergs, the Behemoth and the Mantis People. Their eyes glow in the dark. */
public final class CronenbergRenderers {
    private CronenbergRenderers() {
    }

    private static <T extends Mob, M extends EntityModel<T>> void glowingEyes(MobRenderer<T, M> renderer, String texture) {
        RenderType eyes = RenderType.eyes(RickAndMortyMod.id("textures/entity/" + texture + "_eyes.png"));
        renderer.addLayer(new EyesLayer<T, M>((RenderLayerParent<T, M>) renderer) {
            @Override
            public RenderType renderType() {
                return eyes;
            }
        });
    }

    public static EntityRendererProvider<CronenbergEntity> cronenberg(CronenbergKind kind) {
        return context -> {
            EntityModel<CronenbergEntity> model = switch (kind) {
                case CRAWLER -> new CronenbergModels.Crawler(context.bakeLayer(CronenbergModels.CRAWLER));
                case STALKER -> new CronenbergModels.Stalker(context.bakeLayer(CronenbergModels.STALKER));
                case BLOB -> new CronenbergModels.Blob(context.bakeLayer(CronenbergModels.BLOB));
            };
            float shadow = kind == CronenbergKind.BLOB ? 0.9f : kind == CronenbergKind.CRAWLER ? 0.7f : 0.5f;
            ResourceLocation texture = RickAndMortyMod.id("textures/entity/" + kind.id() + ".png");
            MobRenderer<CronenbergEntity, EntityModel<CronenbergEntity>> renderer = new MobRenderer<>(context, model, shadow) {
                @Override
                public ResourceLocation getTextureLocation(CronenbergEntity entity) {
                    return texture;
                }
            };
            glowingEyes(renderer, kind.id());
            return renderer;
        };
    }

    public static class Behemoth extends MobRenderer<BehemothEntity, CronenbergModels.Behemoth> {
        private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/behemoth.png");
        /** The model is built at half size; this brings it up to its (enormous) real size. */
        public static final float SCALE = 2.2f;

        public Behemoth(EntityRendererProvider.Context context) {
            super(context, new CronenbergModels.Behemoth(context.bakeLayer(CronenbergModels.BEHEMOTH)), 2.0f);
            glowingEyes(this, "behemoth");
        }

        @Override
        protected void scale(BehemothEntity entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }

        @Override
        public ResourceLocation getTextureLocation(BehemothEntity entity) {
            return TEXTURE;
        }
    }

    public static class Mantis extends MobRenderer<MantisEntity, CronenbergModels.Mantis> {
        private static final ResourceLocation TEXTURE = RickAndMortyMod.id("textures/entity/mantis_person.png");

        public Mantis(EntityRendererProvider.Context context) {
            super(context, new CronenbergModels.Mantis(context.bakeLayer(CronenbergModels.MANTIS)), 0.5f);
            glowingEyes(this, "mantis_person");
        }

        @Override
        public ResourceLocation getTextureLocation(MantisEntity entity) {
            return TEXTURE;
        }
    }
}
