"""Data for Cronenberg World, the Blender Upgrade and the Council: the dimension, its terrain,
biomes and features, structures, loot, recipes, advancements, tags, sounds and translations.
Run from the project root:

    python tools/generate_cronenberg_data.py [path/to/vanilla/overworld/noise_settings.json]

The terrain reuses the Overworld's noise router (read from the vanilla file, or from the
existing cronenberg.json if no path is given) with flesh instead of stone and new surface rules.
"""
import json
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
RES = PROJECT / "src/main/resources"
DATA = RES / "data/rickrooms"
MC = RES / "data/minecraft"
ASSETS = RES / "assets/rickrooms"
M = "rickrooms:"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def merge_tag(path, values):
    """Adds values to a tag file, keeping whatever it already lists."""
    existing = json.loads(path.read_text(encoding="utf-8"))["values"] if path.exists() else []
    for v in values:
        if v not in existing:
            existing.append(v)
    write(path, {"replace": False, "values": existing})


BIOMES = ["flesh_plains", "bone_forest", "blood_marsh", "tumor_wastes", "cronenberg_suburbs", "blood_sea"]


# ================================================================================ terrain

def block(name, props=None):
    state = {"Name": name}
    if props:
        state["Properties"] = props
    return {"type": "minecraft:block", "result_state": state}


def cond(if_true, then):
    return {"type": "minecraft:condition", "if_true": if_true, "then_run": then}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def biome_is(*names):
    return {"type": "minecraft:biome", "biome_is": [M + n for n in names]}


def noise(name, lo, hi=1.7976931348623157e308):
    return {"type": "minecraft:noise_threshold", "noise": name, "min_threshold": lo, "max_threshold": hi}


FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
UNDER_FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "floor"}
DRY = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}


def surface_rule():
    veined = block(M + "veined_flesh")
    rotting = block(M + "rotting_flesh")
    top = seq(
        cond(biome_is("blood_marsh"), seq(cond(noise("minecraft:surface_swamp", 0.0), block(M + "blood_clot")), veined)),
        cond(biome_is("tumor_wastes"), seq(cond(noise("minecraft:surface", 0.2), block(M + "tumor")), veined)),
        cond(biome_is("bone_forest"), seq(cond(noise("minecraft:surface", 0.4), block(M + "calcified_bone", {"axis": "y"})), veined)),
        cond(biome_is("cronenberg_suburbs"), seq(cond(noise("minecraft:surface", -0.06, 0.06), block("minecraft:coarse_dirt")), veined)),
        veined)
    return seq(
        cond({"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor",
              "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}}, block("minecraft:bedrock")),
        cond({"type": "minecraft:above_preliminary_surface"}, seq(
            cond(FLOOR, seq(cond(DRY, top), rotting)),              # dry ground grows veins; the sea floor rots
            cond(UNDER_FLOOR, rotting))),
        cond({"type": "minecraft:vertical_gradient", "random_name": "minecraft:deepslate",
              "true_at_and_below": {"absolute": 0}, "false_at_and_above": {"absolute": 8}}, block(M + "necrotic_flesh", None)))


def terrain(vanilla_path):
    target = DATA / "worldgen/noise_settings/cronenberg.json"
    source = Path(vanilla_path) if vanilla_path else target
    settings = json.loads(source.read_text(encoding="utf-8"))
    settings["default_block"] = {"Name": M + "flesh"}
    settings["ore_veins_enabled"] = False
    settings["surface_rule"] = surface_rule()
    write(target, settings)

    write(DATA / "dimension_type/cronenberg.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.0, "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
        "min_y": -64, "height": 384, "logical_height": 384, "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": M + "cronenberg", "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7}})

    full = [-1.0, 1.0]
    land = [-0.19, 1.0]

    def point(biome, temperature=full, humidity=full, continentalness=land, erosion=full, weirdness=full):
        return {"biome": M + biome, "parameters": {"temperature": temperature, "humidity": humidity, "continentalness": continentalness,
                                                  "erosion": erosion, "weirdness": weirdness, "depth": 0.0, "offset": 0.0}}
    write(DATA / "dimension/cronenberg.json", {
        "type": M + "cronenberg",
        "generator": {"type": "minecraft:noise", "settings": M + "cronenberg", "biome_source": {"type": "minecraft:multi_noise", "biomes": [
            point("blood_sea", continentalness=[-1.2, -0.19]),
            # temperature/humidity split near the vanilla noise's quantiles, so every biome gets a fair share
            point("flesh_plains", temperature=[-0.45, 0.2], humidity=[-1.0, 0.1]),
            point("cronenberg_suburbs", temperature=[-1.0, -0.45], humidity=[-1.0, 0.1]),
            point("cronenberg_suburbs", temperature=[0.2, 0.55], humidity=[-1.0, -0.1]),
            point("bone_forest", temperature=[-1.0, 0.2], humidity=[0.1, 1.0]),
            point("blood_marsh", temperature=[0.2, 1.0], humidity=[0.1, 1.0]),
            point("tumor_wastes", temperature=[0.55, 1.0], humidity=[-1.0, 0.1]),
            point("tumor_wastes", temperature=[0.2, 0.55], humidity=[-0.1, 0.1]),
        ]}}})


# ================================================================================ features

def patch(block_id, tries, spread=7, y=3):
    return {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": spread, "y_spread": y, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": block_id}}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}]}}}


def placed(feature, count=None, rarity=None, heightmap="OCEAN_FLOOR_WG"):
    mods = []
    if rarity:
        mods.append({"type": "minecraft:rarity_filter", "chance": rarity})
    if count:
        mods.append({"type": "minecraft:count", "count": count})
    mods += [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": heightmap}, {"type": "minecraft:biome"}]
    return {"feature": M + feature, "placement": mods}


def features():
    configured = {
        "ribcage": {"type": M + "ribcage", "config": {}},
        "tumor_mound": {"type": M + "tumor_mound", "config": {}},
        "flesh_grass_patch": patch(M + "flesh_grass", 48),
        "eyestalk_patch": patch(M + "eyestalk", 10, 5, 2),
        "pustule_patch": patch(M + "pustule", 8, 5, 2),
        "tooth_patch": patch(M + "tooth_spike", 14, 4, 2),
    }
    for name, data in configured.items():
        write(DATA / f"worldgen/configured_feature/{name}.json", data)
    placements = {
        "ribcage_rare": placed("ribcage", rarity=7),
        "ribcage_common": placed("ribcage", count=3),
        "ribcage_sea": placed("ribcage", rarity=3),
        "tumor_mound_rare": placed("tumor_mound", rarity=5),
        "tumor_mound_common": placed("tumor_mound", count=2),
        "flesh_grass_dense": placed("flesh_grass_patch", count=5, heightmap="MOTION_BLOCKING"),
        "flesh_grass_sparse": placed("flesh_grass_patch", count=2, heightmap="MOTION_BLOCKING"),
        "eyestalks": placed("eyestalk_patch", rarity=2, heightmap="MOTION_BLOCKING"),
        "pustules": placed("pustule_patch", count=2, heightmap="MOTION_BLOCKING"),
        "pustules_rare": placed("pustule_patch", rarity=3, heightmap="MOTION_BLOCKING"),
        "teeth": placed("tooth_patch", rarity=3, heightmap="MOTION_BLOCKING"),
        "teeth_common": placed("tooth_patch", count=2, heightmap="MOTION_BLOCKING"),
    }
    for name, data in placements.items():
        write(DATA / f"worldgen/placed_feature/{name}.json", data)


FEATURE_ORDER = ["ribcage_rare", "ribcage_common", "ribcage_sea", "tumor_mound_rare", "tumor_mound_common", "flesh_grass_dense",
                 "flesh_grass_sparse", "pustules", "pustules_rare", "teeth", "teeth_common", "eyestalks"]

ORES = ["ore_coal_upper", "ore_coal_lower", "ore_iron_upper", "ore_iron_middle", "ore_iron_small", "ore_gold", "ore_gold_lower",
        "ore_redstone", "ore_redstone_lower", "ore_diamond", "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried",
        "ore_lapis", "ore_lapis_buried", "ore_copper"]


def biomes():
    base_effects = {"sky_color": 0x6B2A2A, "water_color": 0x7A0A0A, "water_fog_color": 0x2A0303, "grass_color": 0x8A2A2A,
                    "foliage_color": 0x8A2A2A, "ambient_sound": M + "cronenberg.wind",
                    "mood_sound": {"sound": M + "cronenberg.distant", "tick_delay": 3000, "block_search_extent": 8, "offset": 2.0},
                    "additions_sound": {"sound": M + "cronenberg.heartbeat", "tick_chance": 0.004},
                    "particle": {"options": {"type": "minecraft:crimson_spore"}, "probability": 0.01}}
    fog = {"flesh_plains": 0x5A1A1C, "bone_forest": 0x5E5046, "blood_marsh": 0x3A0508, "tumor_wastes": 0x5E3A1E,
           "cronenberg_suburbs": 0x4A2E2E, "blood_sea": 0x300406}
    spawns = {  # crawler, stalker, blob, mantis
        "flesh_plains": (40, 25, 20, 30), "bone_forest": (30, 40, 10, 25), "blood_marsh": (45, 15, 35, 10),
        "tumor_wastes": (35, 20, 45, 15), "cronenberg_suburbs": (30, 30, 15, 45), "blood_sea": (10, 5, 5, 0)}
    vegetation = {
        "flesh_plains": ["ribcage_rare", "tumor_mound_rare", "flesh_grass_dense", "eyestalks", "pustules_rare", "teeth"],
        "bone_forest": ["ribcage_common", "flesh_grass_sparse", "eyestalks", "teeth_common"],
        "blood_marsh": ["tumor_mound_rare", "flesh_grass_sparse", "pustules", "eyestalks"],
        "tumor_wastes": ["tumor_mound_common", "pustules", "teeth", "eyestalks"],
        "cronenberg_suburbs": ["flesh_grass_sparse", "pustules_rare", "teeth"],
        "blood_sea": ["ribcage_sea", "tumor_mound_rare"],
    }
    for name in BIOMES:
        c, s, b, m = spawns[name]
        monsters = [{"type": M + "cronenberg_crawler", "weight": c, "minCount": 1, "maxCount": 3},
                    {"type": M + "cronenberg_stalker", "weight": s, "minCount": 1, "maxCount": 2},
                    {"type": M + "cronenberg_blob", "weight": b, "minCount": 1, "maxCount": 2}]
        if m:
            monsters.append({"type": M + "mantis_person", "weight": m, "minCount": 2, "maxCount": 4})
        steps = [[] for _ in range(11)]
        steps[1] = ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"]
        steps[6] = [f"minecraft:{o}" for o in ORES]
        steps[8] = ["minecraft:spring_lava"]
        # every biome must list shared features in the same order, or the world refuses to load
        steps[9] = [M + v for v in sorted(vegetation[name], key=FEATURE_ORDER.index)]
        write(DATA / f"worldgen/biome/{name}.json", {
            "has_precipitation": name == "blood_marsh", "temperature": 0.8, "downfall": 0.9 if name == "blood_marsh" else 0.3,
            "effects": dict(base_effects, fog_color=fog[name]),
            "spawners": {"monster": monsters, "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [],
                         "water_creature": [], "water_ambient": [], "misc": []},
            "spawn_costs": {},
            "carvers": {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]},
            "features": steps})
    write(DATA / "tags/worldgen/biome/is_cronenberg.json", {"values": [M + b for b in BIOMES]})
    land = ["flesh_plains", "bone_forest", "blood_marsh", "tumor_wastes", "cronenberg_suburbs"]
    site_biomes = {
        "has_ruined_house": ["flesh_plains", "cronenberg_suburbs", "tumor_wastes", "bone_forest"],
        "has_survivor_camp": land,
        "has_high_school": ["cronenberg_suburbs", "flesh_plains", "tumor_wastes"],
        "has_flesh_pit": land,
    }
    for tag, names in site_biomes.items():
        write(DATA / f"tags/worldgen/biome/{tag}.json", {"values": [M + n for n in names]})
    write(DATA / "tags/worldgen/biome/has_council.json", {"values": ["#minecraft:is_overworld"]})


def structures():
    sites = {  # kind: (biome tag, spacing, separation, salt, terrain, spawn overrides)
        "ruined_house": ("has_ruined_house", 8, 3, 610001, "beard_thin", {}),
        "survivor_camp": ("has_survivor_camp", 28, 10, 610002, "beard_thin", {}),
        "high_school": ("has_high_school", 36, 14, 610003, "beard_thin",
                        {"monster": {"bounding_box": "full", "spawns": [{"type": M + "mantis_person", "weight": 1, "minCount": 1, "maxCount": 2}]}}),
        "flesh_pit": ("has_flesh_pit", 32, 12, 610004, "none", {}),
        "council_chamber": ("has_council", 72, 30, 610005, "beard_thin", {}),
    }
    for kind, (tag, spacing, separation, salt, terrain_mode, overrides) in sites.items():
        write(DATA / f"worldgen/structure/{kind}.json", {"type": M + "site", "kind": kind, "biomes": "#" + M + tag,
                                                         "step": "surface_structures", "spawn_overrides": overrides,
                                                         "terrain_adaptation": terrain_mode})
        write(DATA / f"worldgen/structure_set/{kind}.json", {"structures": [{"structure": M + kind, "weight": 1}],
                                                             "placement": {"type": "minecraft:random_spread", "spacing": spacing,
                                                                           "separation": separation, "salt": salt}})


# ================================================================================ tags

def tags():
    flesh_family = ["flesh", "veined_flesh", "rotting_flesh", "tumor", "watching_flesh", "blood_clot"]
    write(DATA / "tags/block/flesh_ground.json", {"values": [M + b for b in flesh_family + ["necrotic_flesh"]]})
    merge_tag(MC / "tags/block/mineable/shovel.json", [M + b for b in flesh_family])
    merge_tag(MC / "tags/block/mineable/hoe.json", [M + b for b in flesh_family + ["pustule", "eyestalk", "flesh_grass"]])
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + b for b in ("necrotic_flesh", "calcified_bone", "tooth_spike")])
    merge_tag(MC / "tags/block/base_stone_overworld.json", [M + "flesh", M + "necrotic_flesh"])
    merge_tag(MC / "tags/block/stone_ore_replaceables.json", [M + "flesh"])
    merge_tag(MC / "tags/block/deepslate_ore_replaceables.json", [M + "necrotic_flesh"])
    merge_tag(MC / "tags/block/overworld_carver_replaceables.json", [M + b for b in flesh_family + ["calcified_bone"]])
    merge_tag(MC / "tags/damage_type/bypasses_armor.json", [M + "blender", M + "bleeding", M + "evil_gateway"])
    merge_tag(MC / "tags/damage_type/no_knockback.json", [M + "bleeding", M + "blender", M + "evil_gateway"])
    write(DATA / "tags/entity_type/cronenbergs.json", {"values": [M + "cronenberg_crawler", M + "cronenberg_stalker",
                                                                  M + "cronenberg_blob", M + "behemoth"]})
    for dt, msg, scaling, effects in (("blender", "rickrooms.blender", "when_caused_by_living_non_player", None),
                                      ("teeth", "rickrooms.teeth", "when_caused_by_living_non_player", "poking"),
                                      ("bleeding", "rickrooms.bleeding", "never", None),
                                      ("evil_gateway", "rickrooms.evil_gateway", "never", None)):
        data = {"message_id": msg, "exhaustion": 0.1, "scaling": scaling}
        if effects:
            data["effects"] = effects
        write(DATA / f"damage_type/{dt}.json", data)


# ================================================================================ loot

def item(name, weight=1, count=None, extra=None):
    if name == "minecraft:empty":
        return {"type": "minecraft:empty", "weight": weight}
    entry = {"type": "minecraft:item", "name": name if ":" in name else M + name, "weight": weight}
    functions = []
    if count:
        lo, hi = count
        functions.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if extra:
        functions += extra
    if functions:
        entry["functions"] = functions
    return entry


def pool(rolls, entries, conditions=None):
    p = {"rolls": rolls if isinstance(rolls, (int, float)) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
         "bonus_rolls": 0, "entries": entries}
    if conditions:
        p["conditions"] = conditions
    return p


LOOTING = [{"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
            "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]


def chance(p):
    return [{"condition": "minecraft:random_chance", "chance": p}]


def loot():
    for name in ("flesh", "veined_flesh", "rotting_flesh", "necrotic_flesh", "tumor", "watching_flesh", "blood_clot",
                 "calcified_bone", "tooth_spike", "eyestalk"):
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            pool(1, [item(name)], [{"condition": "minecraft:survives_explosion"}])]})
    write(DATA / "loot_table/blocks/flesh_grass.json", {"type": "minecraft:block", "pools": [
        pool(1, [item("flesh_grass")], [{"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}])]})

    flesh = item("cronenberg_flesh", count=(1, 3), extra=LOOTING)
    write(DATA / "loot_table/entities/cronenberg_crawler.json", {"type": "minecraft:entity", "pools": [
        pool(1, [flesh]), pool(1, [item("bone_plate", count=(0, 1), extra=LOOTING)]),
        pool(1, [item("mutagen_sac")], [{"condition": "minecraft:killed_by_player"}] + chance(0.12))]})
    write(DATA / "loot_table/entities/cronenberg_stalker.json", {"type": "minecraft:entity", "pools": [
        pool(1, [item("cronenberg_flesh", count=(1, 4), extra=LOOTING)]), pool(1, [item("bone_plate", count=(1, 2), extra=LOOTING)]),
        pool(1, [item("mutagen_sac")], [{"condition": "minecraft:killed_by_player"}] + chance(0.2))]})
    write(DATA / "loot_table/entities/cronenberg_blob.json", {"type": "minecraft:entity", "pools": [
        pool(1, [item("cronenberg_flesh", count=(2, 5), extra=LOOTING)]),
        pool(1, [item("mutagen_sac", count=(1, 2))], [{"condition": "minecraft:killed_by_player"}] + chance(0.35))]})
    write(DATA / "loot_table/entities/mantis_person.json", {"type": "minecraft:entity", "pools": [
        pool(1, [item("cronenberg_flesh", count=(0, 2), extra=LOOTING)]),
        pool(1, [item("mutagen_sac")], [{"condition": "minecraft:killed_by_player"}] + chance(0.25)),
        pool(1, [item("mantis_scythe")], [{"condition": "minecraft:killed_by_player"},
                                          {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                           "unenchanted_chance": 0.06, "enchanted_chance": {"type": "minecraft:linear", "base": 0.09,
                                                                                             "per_level_above_first": 0.03}}])]})
    write(DATA / "loot_table/entities/behemoth.json", {"type": "minecraft:entity", "pools": [
        pool(1, [item("behemoth_heart")]), pool(1, [item("mutagen_sac", count=(4, 8))]), pool(1, [item("bone_plate", count=(6, 12))]),
        pool(1, [item("cronenberg_flesh", count=(8, 16))]), pool(1, [item("blender_chip")], chance(0.25))]})
    write(DATA / "loot_table/entities/cronenberg_rick.json", {"type": "minecraft:entity", "pools": [
        pool(1, [item("portal_fluid", count=(1, 2))])]})

    chests = {
        "cronenberg_house": [pool((3, 6), [item("minecraft:bread", 10, (1, 3)), item("minecraft:rotten_flesh", 12, (2, 5)),
                                            item("minecraft:bone", 10, (1, 4)), item("minecraft:string", 6, (1, 3)),
                                            item("minecraft:iron_nugget", 8, (2, 7)), item("minecraft:torch", 8, (2, 6)),
                                            item("cronenberg_flesh", 8, (1, 3)), item("bone_plate", 6, (1, 2)),
                                            item("minecraft:paper", 5, (1, 3)), item("minecraft:leather", 4, (1, 2))]),
                             pool(1, [item("mutagen_sac", 5), item("cure_serum", 2), item("szechuan_sauce", 1),
                                      item("minecraft:golden_apple", 1), item("minecraft:empty", 12)])],
        "survivor_camp": [pool((4, 7), [item("minecraft:cooked_beef", 10, (1, 4)), item("minecraft:bread", 10, (2, 4)),
                                         item("minecraft:arrow", 8, (4, 12)), item("minecraft:iron_ingot", 6, (1, 3)),
                                         item("minecraft:torch", 8, (4, 12)), item("bone_plate", 8, (1, 4)),
                                         item("mutagen_sac", 4, (1, 2)), item("minecraft:potato", 6, (2, 6)),
                                         item("minecraft:tnt", 2, (1, 2))]),
                          pool(1, [item("cure_serum", 3), item("survivor_helmet", 2), item("survivor_boots", 2),
                                   item("portal_fluid", 2), item("minecraft:empty", 6)])],
        "high_school": [pool((3, 6), [item("minecraft:paper", 10, (2, 6)), item("minecraft:book", 8, (1, 3)),
                                       item("bone_plate", 8, (1, 3)), item("mutagen_sac", 6, (1, 2)),
                                       item("cronenberg_flesh", 6, (1, 3)), item("minecraft:apple", 6, (1, 3)),
                                       item("minecraft:bread", 6, (1, 2)), item("minecraft:ink_sac", 4, (1, 3))]),
                        pool(1, [item("cure_serum", 4), item("mantis_scythe", 2), item("szechuan_sauce", 3),
                                 item("minecraft:golden_apple", 2), item("minecraft:empty", 6)])],
        "flesh_pit": [pool((4, 8), [item("mutagen_sac", 10, (1, 3)), item("bone_plate", 10, (2, 5)),
                                     item("minecraft:iron_ingot", 8, (2, 5)), item("minecraft:gold_ingot", 6, (1, 4)),
                                     item("minecraft:diamond", 3, (1, 2)), item("minecraft:emerald", 3, (1, 3)),
                                     item("cronenberg_flesh", 8, (2, 5))]),
                      pool(1, [item("blender_chip", 2), item("cure_serum", 4), item("minecraft:enchanted_golden_apple", 1),
                               item("portal_fluid", 3), item("minecraft:empty", 6)])],
        "council_vault": [pool((2, 4), [item("portal_fluid", 10, (1, 3)), item("minecraft:diamond", 6, (1, 3)),
                                         item("minecraft:emerald", 6, (2, 5)), item("minecraft:netherite_scrap", 2, (1, 1)),
                                         item("almond_water", 4, (1, 2))]),
                          pool(1, [item("targeting_chip", 3), item("blender_chip", 3), item("cronenberg_coordinates", 4),
                                   item("backrooms_coordinates", 4), item("microverse_battery", 2), item("minecraft:empty", 4)])],
    }
    for name, pools in chests.items():
        write(DATA / f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": pools})

    notes = {  # page: [(loot table, chance)]
        1: [("rickrooms:chests/rick_stash", 0.5), ("rickrooms:chests/backrooms_office", 0.2)],
        2: [("rickrooms:chests/backrooms_kitchen", 0.2), ("rickrooms:chests/backrooms_remnant", 0.35)],
        3: [("rickrooms:chests/rick_stash", 0.4), ("minecraft:chests/stronghold_library", 0.25), ("rickrooms:chests/backrooms_office", 0.1)],
    }
    entries = []
    for page, sources in notes.items():
        for i, (table, p) in enumerate(sources):
            name = f"rick_notes_{page}_{i}"
            write(DATA / f"loot_modifiers/{name}.json", {"type": M + "add_item", "conditions": [
                {"condition": "neoforge:loot_table_id", "loot_table_id": table}], "item": M + f"rick_notes_{page}", "chance": p})
            entries.append(M + name)
    glm = RES / "data/neoforge/loot_modifiers/global_loot_modifiers.json"
    current = json.loads(glm.read_text(encoding="utf-8"))
    current["entries"] = [e for e in current["entries"] if not e.startswith(M + "rick_notes")] + entries
    write(glm, current)


# ================================================================================ recipes

def recipes():
    ing = lambda n: {"item": n if ":" in n else M + n}
    write(DATA / "recipe/cronenberg_coordinates.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [
        ing("rick_notes_1"), ing("rick_notes_2"), ing("rick_notes_3"), ing("portal_fluid")],
        "result": {"id": M + "cronenberg_coordinates", "count": 1}})
    write(DATA / "recipe/blender_chip.json", {"type": "minecraft:crafting_shaped", "category": "misc",
                                              "pattern": ["IMI", "SFS", "IMI"],
                                              "key": {"I": ing("minecraft:iron_ingot"), "M": ing("mutagen_sac"),
                                                      "S": ing("minecraft:stonecutter"), "F": ing("portal_fluid")},
                                              "result": {"id": M + "blender_chip", "count": 1}})
    write(DATA / "recipe/portal_gun_blender.json", {"type": "minecraft:smithing_transform", "template": ing("blender_chip"),
                                                    "base": ing("portal_gun"), "addition": ing("mantis_scythe"),
                                                    "result": {"id": M + "portal_gun", "count": 1, "components": {M + "blender": True}}})
    write(DATA / "recipe/cure_serum.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [
        ing("almond_water"), ing("mutagen_sac"), ing("minecraft:golden_carrot")], "result": {"id": M + "cure_serum", "count": 1}})
    write(DATA / "recipe/bone_plate.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [
        ing("calcified_bone"), ing("calcified_bone")], "result": {"id": M + "bone_plate", "count": 3}})
    shapes = {"helmet": ["PPP", "P P"], "chestplate": ["P P", "PPP", "PPP"], "leggings": ["PPP", "P P", "P P"], "boots": ["P P", "P P"]}
    for part, pattern in shapes.items():
        write(DATA / f"recipe/bone_plate_{part}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                                                        "key": {"P": ing("bone_plate")}, "result": {"id": M + f"bone_plate_{part}", "count": 1}})
    for kind, time in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
        write(DATA / f"recipe/cooked_cronenberg_flesh_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
                                                                     "ingredient": ing("cronenberg_flesh"), "result": {"id": M + "cooked_cronenberg_flesh"},
                                                                     "experience": 0.35, "cookingtime": time})


# ================================================================================ advancements

def advancement(name, icon, parent, criteria, frame="task", hidden=False, components=None):
    icon_data = {"id": icon if ":" in icon else M + icon}
    if components:
        icon_data["components"] = components
    write(DATA / f"advancement/{name}.json", {
        "display": {"icon": icon_data, "title": {"translate": f"advancements.rickrooms.{name}.title"},
                    "description": {"translate": f"advancements.rickrooms.{name}.description"},
                    "frame": frame, "show_toast": True, "announce_to_chat": True, "hidden": hidden},
        "criteria": criteria, "parent": M + parent})


IMPOSSIBLE = {"code": {"trigger": "minecraft:impossible"}}


def advancements():
    advancement("cronenberg_research", "cronenberg_coordinates", "backrooms", {"notes": {"trigger": "minecraft:inventory_changed",
                                                                                         "conditions": {"items": [{"items": M + "cronenberg_coordinates"}]}}})
    advancement("cronenberg", "veined_flesh", "cronenberg_research", {"arrived": {"trigger": "minecraft:changed_dimension",
                                                                                   "conditions": {"to": M + "cronenberg"}}}, "goal")
    advancement("survivors", "survivor_helmet", "cronenberg", IMPOSSIBLE)
    advancement("close_rick_counters", "portal_gun", "cronenberg", IMPOSSIBLE, hidden=True)
    advancement("cronenberged", "tumor", "cronenberg", IMPOSSIBLE, "challenge", hidden=True)
    advancement("cured", "cure_serum", "cronenberged", IMPOSSIBLE)
    advancement("mantis", "mantis_scythe", "cronenberg", {"kill": {"trigger": "minecraft:player_killed_entity",
                                                                   "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                                              "predicate": {"type": M + "mantis_person"}}]}}})
    advancement("high_school", "minecraft:bookshelf", "cronenberg", {"school": {"trigger": "minecraft:location", "conditions": {"player": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"location": {"structures": M + "high_school"}}}]}}})
    advancement("behemoth", "behemoth_heart", "cronenberg", {"kill": {"trigger": "minecraft:player_killed_entity",
                                                                      "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                                                 "predicate": {"type": M + "behemoth"}}]}}}, "challenge")
    advancement("blender_upgrade", "blender_chip", "portal_fluid", {"gun": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
        {"items": M + "portal_gun", "components": {M + "blender": True}}]}}}, "goal")
    advancement("blended", "minecraft:stonecutter", "blender_upgrade", IMPOSSIBLE, hidden=True)
    advancement("council", "minecraft:quartz_pillar", "root", IMPOSSIBLE, "goal")
    advancement("evil_gateway", "warp_gateway", "blender_upgrade", IMPOSSIBLE, hidden=True)
    advancement("meltdown", "minecraft:tnt", "evil_gateway", IMPOSSIBLE, "challenge", hidden=True)


# ================================================================================ sounds and text

def sounds():
    path = ASSETS / "sounds.json"
    data = json.loads(path.read_text(encoding="utf-8"))

    def ev(*names, pitch=1.0, volume=1.0):
        return [{"name": n, "type": "event", "pitch": pitch, "volume": volume} for n in names]
    new = {
        "cronenberg.ambient": ev("minecraft:entity.zombie_villager.ambient", "minecraft:entity.ravager.ambient", pitch=0.7),
        "cronenberg.hurt": ev("minecraft:entity.zombie_villager.hurt", "minecraft:entity.slime.squish", pitch=0.8),
        "cronenberg.death": ev("minecraft:entity.zombie_villager.death", "minecraft:block.honey_block.break", pitch=0.7),
        "cronenberg.step": ev("minecraft:entity.slime.squish_small", pitch=0.8, volume=0.6),
        "behemoth.ambient": ev("minecraft:entity.ravager.ambient", "minecraft:entity.warden.ambient", pitch=0.5),
        "mantis.ambient": ev("minecraft:entity.silverfish.ambient", "minecraft:entity.bat.ambient", pitch=0.55),
        "mantis.hurt": ev("minecraft:entity.spider.hurt", pitch=1.3),
        "mantis.death": ev("minecraft:entity.spider.death", pitch=1.3),
        "cronenberg.wind": ev("minecraft:ambient.soul_sand_valley.loop", pitch=0.8),
        "cronenberg.heartbeat": ev("minecraft:entity.warden.heartbeat", pitch=0.8, volume=0.8),
        "cronenberg.distant": ev("minecraft:entity.ghast.scream", "minecraft:entity.zombie_villager.ambient", "minecraft:entity.enderman.scream",
                                 pitch=0.5, volume=0.9),
        "blender.grab": ev("minecraft:block.piston.extend", "minecraft:block.chain.place", "minecraft:entity.iron_golem.attack", pitch=0.8),
        "blender.saw": ev("minecraft:ui.stonecutter.take_result", "minecraft:block.grindstone.use", pitch=1.3),
        "blender.gore": ev("minecraft:entity.slime.squish", "minecraft:block.honey_block.break", "minecraft:entity.player.hurt_sweet_berry_bush",
                           pitch=0.7),
    }
    for key, sound_list in new.items():
        data[key] = {"subtitle": f"subtitles.rickrooms.{key}", "sounds": sound_list}
    write(path, data)


LANG = {
    "dimension.rickrooms.cronenberg": "Cronenberg World",
    "biome.rickrooms.flesh_plains": "Flesh Plains", "biome.rickrooms.bone_forest": "Bone Forest",
    "biome.rickrooms.blood_marsh": "Blood Marsh", "biome.rickrooms.tumor_wastes": "Tumor Wastes",
    "biome.rickrooms.cronenberg_suburbs": "Cronenberg Suburbs", "biome.rickrooms.blood_sea": "Blood Sea",
    # blocks
    "block.rickrooms.flesh": "Flesh", "block.rickrooms.veined_flesh": "Veined Flesh", "block.rickrooms.rotting_flesh": "Rotting Flesh",
    "block.rickrooms.necrotic_flesh": "Necrotic Flesh", "block.rickrooms.tumor": "Tumor", "block.rickrooms.watching_flesh": "Watching Flesh",
    "block.rickrooms.blood_clot": "Blood Clot", "block.rickrooms.calcified_bone": "Calcified Bone", "block.rickrooms.pustule": "Pustule",
    "block.rickrooms.tooth_spike": "Tooth Spike", "block.rickrooms.flesh_grass": "Sinew Tuft", "block.rickrooms.eyestalk": "Eyestalk",
    # items
    "item.rickrooms.rick_notes_1": "Rick's Notes: Potion #9 (I)", "item.rickrooms.rick_notes_2": "Rick's Notes: Potion #9 (II)",
    "item.rickrooms.rick_notes_3": "Rick's Notes: Potion #9 (III)", "item.rickrooms.cronenberg_coordinates": "Cronenberg World Coordinates",
    "item.rickrooms.cronenberg_flesh": "Cronenberg Flesh", "item.rickrooms.cooked_cronenberg_flesh": "Cooked Cronenberg Flesh",
    "item.rickrooms.mutagen_sac": "Mutagen Sac", "item.rickrooms.bone_plate": "Bone Plate", "item.rickrooms.mantis_scythe": "Mantis Scythe",
    "item.rickrooms.cure_serum": "Cure Serum", "item.rickrooms.behemoth_heart": "Behemoth Heart", "item.rickrooms.blender_chip": "Blender Upgrade",
    "item.rickrooms.bone_plate_helmet": "Bone Plate Helmet", "item.rickrooms.bone_plate_chestplate": "Bone Plate Chestplate",
    "item.rickrooms.bone_plate_leggings": "Bone Plate Leggings", "item.rickrooms.bone_plate_boots": "Bone Plate Boots",
    "item.rickrooms.survivor_helmet": "Survivor Goggles", "item.rickrooms.survivor_chestplate": "Survivor Jacket",
    "item.rickrooms.survivor_leggings": "Survivor Trousers", "item.rickrooms.survivor_boots": "Survivor Boots",
    "item.rickrooms.cronenberg_crawler_spawn_egg": "Cronenberg Crawler Spawn Egg", "item.rickrooms.cronenberg_stalker_spawn_egg": "Cronenberg Stalker Spawn Egg",
    "item.rickrooms.cronenberg_blob_spawn_egg": "Cronenberg Blob Spawn Egg", "item.rickrooms.behemoth_spawn_egg": "Cronenberg Behemoth Spawn Egg",
    "item.rickrooms.mantis_person_spawn_egg": "Mantis Person Spawn Egg", "item.rickrooms.jerry_spawn_egg": "Jerry Spawn Egg",
    "item.rickrooms.beth_spawn_egg": "Beth Spawn Egg", "item.rickrooms.summer_spawn_egg": "Summer Spawn Egg",
    "item.rickrooms.cronenberg_rick_spawn_egg": "Cronenberg Rick Spawn Egg", "item.rickrooms.cronenberg_morty_spawn_egg": "Cronenberg Morty Spawn Egg",
    "item.rickrooms.council_member_spawn_egg": "Council Member Spawn Egg",
    # entities
    "entity.rickrooms.cronenberg_crawler": "Cronenberg Crawler", "entity.rickrooms.cronenberg_stalker": "Cronenberg Stalker",
    "entity.rickrooms.cronenberg_blob": "Cronenberg Blob", "entity.rickrooms.behemoth": "Cronenberg Behemoth",
    "entity.rickrooms.mantis_person": "Mantis Person", "entity.rickrooms.jerry": "Jerry", "entity.rickrooms.beth": "Beth",
    "entity.rickrooms.summer": "Summer", "entity.rickrooms.cronenberg_rick": "Cronenberg Rick", "entity.rickrooms.cronenberg_morty": "Cronenberg Morty",
    "entity.rickrooms.council_member": "Council Member",
    "effect.rickrooms.bleeding": "Bleeding",
    # tooltips
    "tooltip.rickrooms.rick_notes_1.1": "\"Potion #9, for Morty's dance. Vole DNA, a", "tooltip.rickrooms.rick_notes_1.2": "pheromone carrier... it's airborne. It's in",
    "tooltip.rickrooms.rick_notes_1.3": "the flu. Oh no. Oh, that's bad.\"",
    "tooltip.rickrooms.rick_notes_2.1": "\"Praying mantis DNA to cancel it out. Everyone", "tooltip.rickrooms.rick_notes_2.2": "turned into mantises instead. So I cancelled",
    "tooltip.rickrooms.rick_notes_2.3": "THAT out. They're... Cronenbergs now.\"",
    "tooltip.rickrooms.rick_notes_3.1": "\"Dimension's a write-off. Coordinates below in", "tooltip.rickrooms.rick_notes_3.2": "case I ever need to go back. I won't. Needs",
    "tooltip.rickrooms.rick_notes_3.3": "portal fluid to calibrate. Don't think about it. -R\"",
    "tooltip.rickrooms.cronenberg_coordinates.1": "The way back to the dimension Rick ruined.",
    "tooltip.rickrooms.cronenberg_coordinates.2": "Use it with a Portal Gun in your other hand.",
    "tooltip.rickrooms.cronenberg_flesh": "Still warm. Still twitching. Mutagenic.",
    "tooltip.rickrooms.cooked_cronenberg_flesh": "Cooking gets most of the mutagen out. Most.",
    "tooltip.rickrooms.mutagen_sac": "Whatever's in here is what made them like this.",
    "tooltip.rickrooms.mantis_scythe": "Serrated. Its cuts keep bleeding.",
    "tooltip.rickrooms.cure_serum": "Rick's fix for Potion #9. Undoes all mutation.",
    "tooltip.rickrooms.behemoth_heart": "Enormous strength, and a big dose of mutation.",
    "tooltip.rickrooms.blender_chip": "Smithing template: Portal Gun + Mantis Scythe = attack mode.",
    "tooltip.rickrooms.portal_gun.mode_travel": "Blender Upgrade: travel mode",
    "tooltip.rickrooms.portal_gun.mode_attack": "Blender Upgrade: ATTACK MODE",
    "tooltip.rickrooms.portal_gun.help_mode": "%s: switch travel / attack mode (attack mode + right-click a gateway: arm it)",
    "key.rickrooms.gun_mode": "Portal Gun: Switch Mode", "key.categories.rickrooms": "Rick and Morty",
    "screen.rickrooms.portal_gun.mode_travel": "Mode: Travel", "screen.rickrooms.portal_gun.mode_attack": "Mode: ATTACK",
    "screen.rickrooms.portal_gun.mode_tooltip": "Attack mode shoots blender portals. Anything that gets close gets grabbed... and blended.",
    # messages
    "message.rickrooms.coordinates.already_known": "This Portal Gun already knows the way to %s.",
    "message.rickrooms.coordinates.programmed": "%s has been added to your Portal Gun's dial!",
    "message.rickrooms.mutation.stage_1": "Your skin itches. You feel... quick. And so hungry.",
    "message.rickrooms.mutation.stage_2": "Something is growing on you. You see better in the dark.",
    "message.rickrooms.mutation.stage_3": "The Cronenbergs don't see you as food anymore. They see family.",
    "message.rickrooms.mutation.stage_4": "You're fully Cronenberged. Find a cure.",
    "message.rickrooms.mutation.rejecting": "Your body is rejecting itself...",
    "message.rickrooms.cure_serum": "It burns... then it's gone. You're you again.",
    "message.rickrooms.no_blender": "This gun has no Blender Upgrade.",
    "message.rickrooms.mode.attack": "ATTACK MODE: blender portals",
    "message.rickrooms.mode.travel": "Travel mode: portals",
    # deaths
    "death.attack.rickrooms.blender": "%1$s was put through a portal blender",
    "death.attack.rickrooms.blender.player": "%1$s was put through %2$s's portal blender",
    "death.attack.rickrooms.teeth": "%1$s was chewed up by the ground",
    "death.attack.rickrooms.teeth.player": "%1$s was chewed up by the ground while fleeing %2$s",
    "death.attack.rickrooms.bleeding": "%1$s bled out",
    "death.attack.rickrooms.bleeding.player": "%1$s bled out after a fight with %2$s",
    # NPCs
    "npc.rickrooms.chat": "<%s> %s",
    "npc.rickrooms.jerry.1": "Welcome to the apocalypse! Turns out I'm GREAT at this.",
    "npc.rickrooms.jerry.2": "I've got gear, I've got food. Bring me bone plates, I'll set you up.",
    "npc.rickrooms.jerry.3": "Rick left us here. Best thing that ever happened to me.",
    "npc.rickrooms.jerry.4": "Don't eat the raw meat. Trust me. Don't.",
    "npc.rickrooms.jerry.5": "Mantis People come out at night. That's when we lock the gate.",
    "npc.rickrooms.beth.1": "I'm a surgeon. Horse surgeon. Same thing, out here.",
    "npc.rickrooms.beth.2": "If you start growing extra parts, come see me. I've got serum.",
    "npc.rickrooms.beth.3": "Mutagen, bone plates... I can make that into a cure.",
    "npc.rickrooms.beth.4": "Don't tell Jerry, but he's actually been... useful.",
    "npc.rickrooms.summer.1": "You want weapons? I've got weapons.",
    "npc.rickrooms.summer.2": "The school's crawling with Mantis People. I'm not going back there.",
    "npc.rickrooms.summer.3": "This is so much better than high school.",
    "npc.rickrooms.summer.4": "Bring me flesh. Lots of it. Don't ask what for.",
    "npc.rickrooms.cronenberg_rick.greeting": "Well, well. A tourist! Don't mind the face, it's a dimension thing.",
    "npc.rickrooms.cronenberg_rick.1": "You've got a portal gun too? Cute. Wanna trade?",
    "npc.rickrooms.cronenberg_rick.2": "Every Rick ruins a dimension eventually. This one's mine.",
    "npc.rickrooms.cronenberg_rick.3": "The Blender Upgrade? My own design. Nobody else thinks that big.",
    "npc.rickrooms.cronenberg_rick.4": "I'm not staying long. Nobody stays long.",
    "npc.rickrooms.cronenberg_rick.5": "Burp. The mutagen's actually pretty good once you get used to it.",
    "npc.rickrooms.cronenberg_morty.greeting": "Aw geez, h-hey! Another one! Rick, look!",
    "npc.rickrooms.cronenberg_morty.1": "I-I've got stuff, if you want it!",
    "npc.rickrooms.cronenberg_morty.2": "My face? Aw man, don't look at my face.",
    "npc.rickrooms.cronenberg_morty.3": "Rick says I'm his favourite Morty. H-he says that to all of us.",
    "npc.rickrooms.cronenberg_morty.4": "Don't go near the pits. There's something big down there.",
    "npc.rickrooms.council.1": "The Council will hear you, %s. For a price, naturally.",
    "npc.rickrooms.council.2": "Portal fluid is the only currency that crosses dimensions.",
    "npc.rickrooms.council.3": "Every one of us is someone from your world. Just... not quite.",
    "npc.rickrooms.council.4": "In my dimension, you were the weird one, %s.",
    "npc.rickrooms.council.5": "The Council has seen a thousand of you. You're one of the better ones.",
    "npc.rickrooms.council.6": "Don't stare. It's rude. We're all you, more or less.",
    "npc.rickrooms.council.original": "So you're the original %s. I thought you'd be taller.",
    # advancements
    "advancements.rickrooms.cronenberg_research.title": "Rick Potion #9",
    "advancements.rickrooms.cronenberg_research.description": "Piece together Rick's notes and work out where they lead",
    "advancements.rickrooms.cronenberg.title": "Don't Think About It",
    "advancements.rickrooms.cronenberg.description": "Step into Cronenberg World",
    "advancements.rickrooms.survivors.title": "Welcome to the Apocalypse",
    "advancements.rickrooms.survivors.description": "Trade with the survivors at the Smith house",
    "advancements.rickrooms.close_rick_counters.title": "Close Rick-counters",
    "advancements.rickrooms.close_rick_counters.description": "Meet Cronenberg Rick and Morty",
    "advancements.rickrooms.cronenberged.title": "Cronenberged",
    "advancements.rickrooms.cronenberged.description": "Let the mutation take you completely",
    "advancements.rickrooms.cured.title": "The Cure",
    "advancements.rickrooms.cured.description": "Drink a Cure Serum while badly mutated",
    "advancements.rickrooms.mantis.title": "Pray For Mantis",
    "advancements.rickrooms.mantis.description": "Kill a Mantis Person",
    "advancements.rickrooms.high_school.title": "Harry Herpson High",
    "advancements.rickrooms.high_school.description": "Go back to school. It's worse now",
    "advancements.rickrooms.behemoth.title": "Flesh Titan",
    "advancements.rickrooms.behemoth.description": "Kill the Cronenberg Behemoth in its Flesh Pit",
    "advancements.rickrooms.blender_upgrade.title": "Attack Mode",
    "advancements.rickrooms.blender_upgrade.description": "Fit the Blender Upgrade to a Portal Gun",
    "advancements.rickrooms.blended.title": "Puree",
    "advancements.rickrooms.blended.description": "Put something through a blender portal",
    "advancements.rickrooms.council.title": "Council of Yous",
    "advancements.rickrooms.council.description": "Find the Council and speak with an alternate version of someone you know",
    # evil gateways
    "message.rickrooms.gateway.armed": "Gateway armed. It looks exactly the same... but it's hungry now.",
    "message.rickrooms.gateway.disarmed": "Gateway disarmed. It's just a gateway again.",
    "message.rickrooms.gateway.armed_status": "This gateway is armed. Anyone else who gets close gets eaten.",
    "death.attack.rickrooms.evil_gateway": "%1$s was eaten by a Warp Gateway",
    "death.attack.rickrooms.evil_gateway.player": "%1$s was eaten by %2$s's Warp Gateway",
    "advancements.rickrooms.evil_gateway.title": "It's Not a Trap",
    "advancements.rickrooms.evil_gateway.description": "Arm a Warp Gateway with a Portal Gun in attack mode",
    "advancements.rickrooms.meltdown.title": "Indigestion",
    "advancements.rickrooms.meltdown.description": "Be there when an evil gateway eats a player and can't keep it down",
    "rickrooms.configuration.blender": "Blender Upgrade",
    "rickrooms.configuration.gatewayExplosion": "Evil gateway blast size",
    # config
    "rickrooms.configuration.cronenberg": "Cronenberg World",
    "rickrooms.configuration.mutation": "Mutation",
    "rickrooms.configuration.mutationRate": "Mutation speed",
}


def lang():
    path = ASSETS / "lang/en_us.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(LANG)
    for key in json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8")):
        sub = f"subtitles.rickrooms.{key}"
        if sub not in data:
            data[sub] = SUBTITLES.get(key, key)
    write(path, data)


SUBTITLES = {
    "cronenberg.ambient": "Cronenberg groans", "cronenberg.hurt": "Cronenberg squelches", "cronenberg.death": "Cronenberg bursts",
    "cronenberg.step": "Something wet moves", "behemoth.ambient": "Behemoth rumbles", "mantis.ambient": "Mantis Person clicks",
    "mantis.hurt": "Mantis Person hurts", "mantis.death": "Mantis Person dies", "cronenberg.wind": "Hot, wet wind",
    "cronenberg.heartbeat": "Something's heart beats", "cronenberg.distant": "Distant screaming",
    "blender.grab": "Mechanical arms lunge", "blender.saw": "Saw blades whine", "blender.gore": "Something is blended",
}


def main():
    terrain(sys.argv[1] if len(sys.argv) > 1 else None)
    features()
    biomes()
    structures()
    tags()
    loot()
    recipes()
    advancements()
    sounds()
    lang()
    print("Cronenberg data written")


if __name__ == "__main__":
    main()
