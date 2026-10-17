package com.rickandmorty.worldgen.level;

import java.util.EnumMap;
import java.util.Map;

/** One generator per level (Level 0 keeps its own chunk generator, BackroomsChunkGenerator). */
public final class LevelGenerators {
    private static final Map<BackroomsLevel, LevelGenerator> GENERATORS = new EnumMap<>(BackroomsLevel.class);

    private LevelGenerators() {
    }

    public static synchronized LevelGenerator get(BackroomsLevel level) {
        return GENERATORS.computeIfAbsent(level, l -> switch (l) {
            case LEVEL_1 -> new GarageLevel();
            case LEVEL_4 -> new OfficeLevel();
            case LEVEL_188 -> new CourtyardLevel();
            case HARVEST -> new HarvestLevel();
            case SUNLIT_LOBBY -> new LobbyLevel();
            case WOODEN_HOTEL -> new HotelLevel();
            case SCAFFOLD -> new ScaffoldLevel();
            case CAVERN -> new CavernLevel();
            case POOLROOMS -> new PoolroomsLevel();
            case LEVEL_FUN -> new FunLevel();
            case INFINITE_IKEA -> new InfiniteIkeaLevel();
            case DEAD_MALL -> new DeadMallLevel();
            case ENDLESS_FIELD -> new EndlessFieldLevel();
            case ABANDONED_CITY -> new AbandonedCityLevel();
            case DROP_LIMIT -> new DropLimitLevel();
            case FACADE -> new FacadeLevel();
            case STAIRCASE -> new StaircaseLevel();
            case FROOPYLAND -> new FroopylandLevel();
            case RUN_FOR_YOUR_LIFE -> new RunLevel();
            case LEVEL_0 -> throw new IllegalArgumentException("Level 0 has its own generator");
        });
    }
}
