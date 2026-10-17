package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.BacteriaEntity;
import com.rickandmorty.entity.BehemothEntity;
import com.rickandmorty.entity.CouncilMemberEntity;
import com.rickandmorty.entity.CronenbergEntity;
import com.rickandmorty.entity.CronenbergKind;
import com.rickandmorty.entity.MantisEntity;
import com.rickandmorty.entity.SurvivorEntity;
import com.rickandmorty.entity.SurvivorKind;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import com.rickandmorty.entity.MeeseeksEntity;
import com.rickandmorty.entity.MimicEntity;
import com.rickandmorty.entity.MimicKind;
import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.entity.PortalProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<PortalEntity>> PORTAL = ENTITIES.register("portal",
            () -> EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.MISC)
                    .sized(PortalEntity.WIDTH, PortalEntity.HEIGHT)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("portal"));

    public static final DeferredHolder<EntityType<?>, EntityType<PortalProjectile>> PORTAL_PROJECTILE = ENTITIES.register("portal_projectile",
            () -> EntityType.Builder.<PortalProjectile>of(PortalProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("portal_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<BacteriaEntity>> BACTERIA = ENTITIES.register("bacteria",
            () -> EntityType.Builder.of(BacteriaEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.6f)
                    .eyeHeight(2.35f)
                    .clientTrackingRange(10)
                    .build("bacteria"));

    public static final DeferredHolder<EntityType<?>, EntityType<MeeseeksEntity>> MEESEEKS = ENTITIES.register("meeseeks",
            () -> EntityType.Builder.of(MeeseeksEntity::new, MobCategory.CREATURE)
                    .sized(0.55f, 2.1f)
                    .clientTrackingRange(10)
                    .build("meeseeks"));

    /** One "Still Life" entity type per copied vanilla mob. */
    public static final Map<MimicKind, DeferredHolder<EntityType<?>, EntityType<MimicEntity>>> MIMICS = new EnumMap<>(MimicKind.class);

    static {
        for (MimicKind kind : MimicKind.values()) {
            MIMICS.put(kind, ENTITIES.register(kind.id(),
                    () -> EntityType.Builder.<MimicEntity>of((type, level) -> new MimicEntity(type, level, kind), MobCategory.MONSTER)
                            .sized(kind.width, kind.height)
                            .clientTrackingRange(10)
                            .build(kind.id())));
        }
    }

    /** Still Lifes of people, in players' skins. */
    public static final Map<com.rickandmorty.entity.StillLifeForm, DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.StillLifeEntity>>> STILL_LIVES =
            new EnumMap<>(com.rickandmorty.entity.StillLifeForm.class);

    static {
        for (com.rickandmorty.entity.StillLifeForm form : com.rickandmorty.entity.StillLifeForm.values()) {
            STILL_LIVES.put(form, ENTITIES.register(form.id(),
                    () -> EntityType.Builder.<com.rickandmorty.entity.StillLifeEntity>of((type, level) -> new com.rickandmorty.entity.StillLifeEntity(type, level, form), MobCategory.MONSTER)
                            .sized(form.width, form.height)
                            .clientTrackingRange(10)
                            .build(form.id())));
        }
    }

    // --- Cronenberg World
    public static final Map<CronenbergKind, DeferredHolder<EntityType<?>, EntityType<CronenbergEntity>>> CRONENBERGS = new EnumMap<>(CronenbergKind.class);
    public static final Map<SurvivorKind, DeferredHolder<EntityType<?>, EntityType<SurvivorEntity>>> SURVIVORS = new EnumMap<>(SurvivorKind.class);

    static {
        for (CronenbergKind kind : CronenbergKind.values()) {
            CRONENBERGS.put(kind, ENTITIES.register(kind.id(),
                    () -> EntityType.Builder.<CronenbergEntity>of((type, level) -> new CronenbergEntity(type, level, kind), MobCategory.MONSTER)
                            .sized(kind.width, kind.height)
                            .clientTrackingRange(10)
                            .build(kind.id())));
        }
        for (SurvivorKind kind : SurvivorKind.values()) {
            SURVIVORS.put(kind, ENTITIES.register(kind.id(),
                    () -> EntityType.Builder.<SurvivorEntity>of((type, level) -> new SurvivorEntity(type, level, kind), MobCategory.MISC)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build(kind.id())));
        }
    }

    public static final DeferredHolder<EntityType<?>, EntityType<BehemothEntity>> BEHEMOTH = ENTITIES.register("behemoth",
            () -> EntityType.Builder.of(BehemothEntity::new, MobCategory.MONSTER)
                    .sized(2.8f, 3.4f)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("behemoth"));

    public static final DeferredHolder<EntityType<?>, EntityType<MantisEntity>> MANTIS = ENTITIES.register("mantis_person",
            () -> EntityType.Builder.of(MantisEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 2.0f)
                    .clientTrackingRange(10)
                    .build("mantis_person"));

    public static final DeferredHolder<EntityType<?>, EntityType<CouncilMemberEntity>> COUNCIL_MEMBER = ENTITIES.register("council_member",
            () -> EntityType.Builder.of(CouncilMemberEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("council_member"));

    // --- the other Backrooms levels
    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.ScarecrowEntity>> SCARECROW = ENTITIES.register("scarecrow",
            () -> EntityType.Builder.of(com.rickandmorty.entity.ScarecrowEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.1f)
                    .clientTrackingRange(10)
                    .build("scarecrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.cruiser.SpaceCruiserEntity>> SPACE_CRUISER = ENTITIES.register("space_cruiser",
            () -> EntityType.Builder.<com.rickandmorty.entity.cruiser.SpaceCruiserEntity>of(com.rickandmorty.entity.cruiser.SpaceCruiserEntity::new, MobCategory.MISC)
                    .sized(3.4f, 1.8f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("space_cruiser"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.cruiser.CruiserRocket>> CRUISER_ROCKET = ENTITIES.register("cruiser_rocket",
            () -> EntityType.Builder.<com.rickandmorty.entity.cruiser.CruiserRocket>of(com.rickandmorty.entity.cruiser.CruiserRocket::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("cruiser_rocket"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.FarmScarecrowEntity>> FARM_SCARECROW = ENTITIES.register("farm_scarecrow",
            () -> EntityType.Builder.of(com.rickandmorty.entity.FarmScarecrowEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 2.1f)
                    .clientTrackingRange(10)
                    .build("farm_scarecrow"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.PartygoerEntity>> PARTYGOER = ENTITIES.register("partygoer",
            () -> EntityType.Builder.of(com.rickandmorty.entity.PartygoerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("partygoer"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(com.rickandmorty.entity.WatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(16)
                    .build("watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.StaffEntity>> STAFF = ENTITIES.register("staff",
            () -> EntityType.Builder.of(com.rickandmorty.entity.StaffEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.9f)
                    .eyeHeight(2.6f)
                    .clientTrackingRange(10)
                    .build("staff"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.rickandmorty.entity.SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<com.rickandmorty.entity.SeatEntity>of(com.rickandmorty.entity.SeatEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .noSummon()
                    .build("seat"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(BACTERIA.get(), BacteriaEntity.createAttributes().build());
        event.put(MEESEEKS.get(), MeeseeksEntity.createAttributes().build());
        MIMICS.forEach((kind, type) -> event.put(type.get(), MimicEntity.createAttributes(kind).build()));
        STILL_LIVES.forEach((form, type) -> event.put(type.get(), com.rickandmorty.entity.StillLifeEntity.createAttributes(form).build()));
        CRONENBERGS.forEach((kind, type) -> event.put(type.get(), CronenbergEntity.createAttributes(kind).build()));
        SURVIVORS.forEach((kind, type) -> event.put(type.get(), SurvivorEntity.createAttributes().build()));
        event.put(BEHEMOTH.get(), BehemothEntity.createAttributes().build());
        event.put(MANTIS.get(), MantisEntity.createAttributes().build());
        event.put(COUNCIL_MEMBER.get(), CouncilMemberEntity.createAttributes().build());
        event.put(SCARECROW.get(), com.rickandmorty.entity.ScarecrowEntity.createAttributes().build());
        event.put(WATCHER.get(), com.rickandmorty.entity.WatcherEntity.createAttributes().build());
        event.put(FARM_SCARECROW.get(), com.rickandmorty.entity.FarmScarecrowEntity.createAttributes().build());
        event.put(PARTYGOER.get(), com.rickandmorty.entity.PartygoerEntity.createAttributes().build());
        event.put(STAFF.get(), com.rickandmorty.entity.StaffEntity.createAttributes().build());
    }

    /** Natural spawning rules for Cronenberg World's biomes (see their spawner lists). */
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        CRONENBERGS.values().forEach(type -> event.register(type.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CronenbergEntity::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        event.register(MANTIS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
