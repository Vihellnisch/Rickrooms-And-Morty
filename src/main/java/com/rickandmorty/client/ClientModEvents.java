package com.rickandmorty.client;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.client.model.BacteriaModel;
import com.rickandmorty.client.model.CronenbergLayers;
import com.rickandmorty.client.model.CronenbergModels;
import com.rickandmorty.client.renderer.BacteriaRenderer;
import com.rickandmorty.client.renderer.CouncilMemberRenderer;
import com.rickandmorty.client.renderer.CronenbergRenderers;
import com.rickandmorty.client.renderer.MutationLayer;
import com.rickandmorty.client.renderer.SurvivorRenderer;
import com.rickandmorty.cronenberg.MutationClientState;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.rickandmorty.client.renderer.MeeseeksRenderer;
import com.rickandmorty.client.renderer.MimicRenderer;
import com.rickandmorty.client.renderer.PortalProjectileRenderer;
import com.rickandmorty.client.renderer.PortalRenderer;
import com.rickandmorty.client.renderer.WarpGatewayRenderer;
import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModItems;

@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PORTAL.get(), PortalRenderer::new);
        event.registerEntityRenderer(ModEntities.PORTAL_PROJECTILE.get(), PortalProjectileRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WARP_GATEWAY.get(), WarpGatewayRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SHRINK_RAY.get(), com.rickandmorty.client.gadget.ShrinkRayRenderer::new);
        event.registerEntityRenderer(ModEntities.BACTERIA.get(), BacteriaRenderer::new);
        event.registerEntityRenderer(ModEntities.MEESEEKS.get(), MeeseeksRenderer::new);
        ModEntities.MIMICS.forEach((kind, type) -> event.registerEntityRenderer(type.get(), MimicRenderer.provider(kind)));
        ModEntities.CRONENBERGS.forEach((kind, type) -> event.registerEntityRenderer(type.get(), CronenbergRenderers.cronenberg(kind)));
        ModEntities.SURVIVORS.forEach((kind, type) -> event.registerEntityRenderer(type.get(), SurvivorRenderer.provider(kind)));
        event.registerEntityRenderer(ModEntities.BEHEMOTH.get(), CronenbergRenderers.Behemoth::new);
        event.registerEntityRenderer(ModEntities.MANTIS.get(), CronenbergRenderers.Mantis::new);
        event.registerEntityRenderer(ModEntities.COUNCIL_MEMBER.get(), CouncilMemberRenderer::new);
        event.registerEntityRenderer(ModEntities.SCARECROW.get(), com.rickandmorty.client.renderer.LevelMobRenderers.Scarecrow::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), com.rickandmorty.client.renderer.LevelMobRenderers.Watcher::new);
        event.registerEntityRenderer(ModEntities.FARM_SCARECROW.get(), com.rickandmorty.client.renderer.LevelMobRenderers.FarmScarecrow::new);
        event.registerEntityRenderer(ModEntities.PARTYGOER.get(), com.rickandmorty.client.renderer.LevelMobRenderers.Partygoer::new);
        event.registerEntityRenderer(ModEntities.SPACE_CRUISER.get(), com.rickandmorty.client.renderer.SpaceCruiserRenderer::new);
        event.registerEntityRenderer(ModEntities.STAFF.get(), com.rickandmorty.client.renderer.StaffRenderer::new);
        ModEntities.STILL_LIVES.values().forEach(type -> event.registerEntityRenderer(type.get(), com.rickandmorty.client.renderer.StillLifeRenderer::new));
        event.registerEntityRenderer(ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CRUISER_ROCKET.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 1.4f, true));
    }

    /**
     * Lumps and extra eyes on mutated players (stage from the synced mutation), and the Cognition
     * Amplifier on any creature wearing one (every kind gets the layer, so other mods' pets do too).
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.EntityType<?> type : event.getEntityTypes()) {
            if (event.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> living) {
                com.rickandmorty.client.pet.CognitionAmplifierLayer.attach(living);
            }
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                MutationLayer.attach(renderer, event.getEntityModels(),
                        player -> com.rickandmorty.cronenberg.Mutation.stage(MutationClientState.get(player.getId())));
            }
        }
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(RickAndMortyMod.id("cronenberg"), new CronenbergSky());
        event.register(RickAndMortyMod.id("infinite_ikea"), new StoreSky());
        event.register(RickAndMortyMod.id("endless_field"), new FieldSky());
        event.register(RickAndMortyMod.id("drop_limit"), new SkyLevelClient.HighSky());
        event.register(RickAndMortyMod.id("the_facade"), new SkyLevelClient.GreySky());
        event.register(RickAndMortyMod.id("the_staircase"), new SkyLevelClient.HighSky(250.0f));
        event.register(RickAndMortyMod.id("froopyland"), new SkyLevelClient.HighSky(170.0f));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.GUN_MODE);
        event.register(com.rickandmorty.client.cruiser.CruiserControls.DESCEND);
        event.register(com.rickandmorty.client.cruiser.CruiserControls.NEXT_WEAPON);
        event.register(com.rickandmorty.client.cruiser.CruiserControls.REPULSOR);
        event.register(com.rickandmorty.client.cruiser.CruiserControls.PORTAL);
        event.register(com.rickandmorty.client.cruiser.CruiserControls.DIAL);
        event.register(com.rickandmorty.client.gadget.GogglesSight.TOGGLE);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (com.rickandmorty.compat.Curios.loaded()) com.rickandmorty.client.compat.CuriosClientBridge.register();
            ItemProperties.register(ModItems.PORTAL_GUN.get(), RickAndMortyMod.id("targeting"),
                    (stack, level, entity, seed) -> PortalGunItem.hasTargeting(stack) ? 1.0f : 0.0f);
            // how full the capsule looks (see the portal_gun_fluid_* models)
            ItemProperties.register(ModItems.PORTAL_GUN.get(), RickAndMortyMod.id("fluid"),
                    (stack, level, entity, seed) -> PortalGunItem.getCharges(stack) / (float) PortalGunItem.MAX_CHARGES);
            // 0 = no Blender Upgrade, 0.5 = installed (travel mode), 1 = attack mode (the fluid turns red)
            ItemProperties.register(ModItems.PORTAL_GUN.get(), RickAndMortyMod.id("blender"),
                    (stack, level, entity, seed) -> PortalGunItem.isAttackMode(stack) ? 1.0f : PortalGunItem.hasBlender(stack) ? 0.5f : 0.0f);
        });
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BacteriaModel.LAYER, BacteriaModel::createBodyLayer);
        event.registerLayerDefinition(com.rickandmorty.client.renderer.SpaceCruiserRenderer.LAYER, com.rickandmorty.client.renderer.SpaceCruiserRenderer::layer);
        event.registerLayerDefinition(com.rickandmorty.client.renderer.StaffRenderer.LAYER, com.rickandmorty.client.renderer.StaffRenderer::layer);
        event.registerLayerDefinition(com.rickandmorty.client.gadget.GogglesModel.LAYER, com.rickandmorty.client.gadget.GogglesModel::layer);
        event.registerLayerDefinition(com.rickandmorty.client.gadget.ShrinkRayRenderer.LAYER, com.rickandmorty.client.gadget.ShrinkRayRenderer::layer);
        event.registerLayerDefinition(com.rickandmorty.client.pet.AmplifierHelmet.LAYER, com.rickandmorty.client.pet.AmplifierHelmet::layer);
        event.registerLayerDefinition(MeeseeksRenderer.LAYER, MeeseeksRenderer::createLayer);
        event.registerLayerDefinition(com.rickandmorty.client.model.PickleModel.LAYER, com.rickandmorty.client.model.PickleModel::createLayer);
        event.registerLayerDefinition(CronenbergModels.CRAWLER, CronenbergLayers::crawler);
        event.registerLayerDefinition(CronenbergModels.STALKER, CronenbergLayers::stalker);
        event.registerLayerDefinition(CronenbergModels.BLOB, CronenbergLayers::blob);
        event.registerLayerDefinition(CronenbergModels.BEHEMOTH, CronenbergLayers::behemoth);
        event.registerLayerDefinition(CronenbergModels.MANTIS, CronenbergLayers::mantis);
        event.registerLayerDefinition(CronenbergModels.MUTATION, CronenbergLayers::mutation);
        event.registerLayerDefinition(CronenbergModels.HATS, CronenbergLayers::hats);
    }

    /** Holding the Scarecrow Lantern out in front of you (third person). */
    @SubscribeEvent
    public static void registerClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
                                                                           net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack) {
                return com.rickandmorty.client.LanternArmPose.pose();
            }
        }, com.rickandmorty.registry.ModItems.SCARECROW_LANTERN.get());
        // the goggles, worn, as a proper 3D visor
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private com.rickandmorty.client.gadget.GogglesModel<net.minecraft.world.entity.LivingEntity> model;

            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity,
                                                                                    net.minecraft.world.item.ItemStack stack,
                                                                                    net.minecraft.world.entity.EquipmentSlot slot,
                                                                                    net.minecraft.client.model.HumanoidModel<?> original) {
                if (model == null) {
                    model = new com.rickandmorty.client.gadget.GogglesModel<>(net.minecraft.client.Minecraft.getInstance().getEntityModels()
                            .bakeLayer(com.rickandmorty.client.gadget.GogglesModel.LAYER));
                }
                return model;
            }
        }, com.rickandmorty.registry.ModItems.INTERDIMENSIONAL_GOGGLES.get());
        // the shrink ray in your hand: the real model, small
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private com.rickandmorty.client.gadget.ShrinkRayRenderer.ItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.rickandmorty.client.gadget.ShrinkRayRenderer.ItemRenderer();
                return renderer;
            }
        }, com.rickandmorty.registry.ModItems.SHRINK_RAY.get());
        // the Cognition Amplifier in your hand: the real helmet
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private com.rickandmorty.client.pet.AmplifierHelmet.ItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.rickandmorty.client.pet.AmplifierHelmet.ItemRenderer();
                return renderer;
            }
        }, com.rickandmorty.registry.ModItems.COGNITION_AMPLIFIER.get());
    }
}
