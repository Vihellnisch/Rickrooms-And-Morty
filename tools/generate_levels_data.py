"""Data for the new Backrooms levels (v0.8): dimensions, dimension types, biomes, loot, tags,
advancements and translations. Run from the project root:

    python tools/generate_levels_data.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_data import (ASSETS, DATA, IMPOSSIBLE, M, MC, advancement, chance, item, merge_tag,  # noqa: E402
                                      pool, write)

# dimension id, codec name, dimension height, ambient light, fog colour, name
LEVELS = [
    ("level_1", "level_1", 16, 0.05, 0x5A5E60, "Level 1: The Habitable Zone"),
    ("level_4", "level_4", 16, 0.1, 0x9A9480, "Level 4: The Abandoned Office"),
    ("level_188", "level_188", 64, 0.3, 0xB8B8B4, "Level 188: The Courtyard of Windows"),
    ("the_harvest", "harvest", 32, 0.1, 0x6E6038, "The Harvest"),
    ("sunlit_lobby", "sunlit_lobby", 16, 0.4, 0xF2CE86, "The Sunlit Lobby"),
    ("wooden_hotel", "wooden_hotel", 96, 0.05, 0x3A2A1C, "The Wooden Hotel"),
    ("scaffold_void", "scaffold", 144, 0.0, 0x080604, "The Scaffold Void"),
    ("glowstone_cavern", "cavern", 80, 0.15, 0x6A5A30, "The Glowstone Cavern"),
    ("poolrooms", "poolrooms", 96, 0.35, 0xD6E9E0, "Level 37: The Poolrooms"),
    ("level_fun", "level_fun", 16, 0.1, 0xD9C27A, "Level Fun =)"),
]


def dimensions():
    for dim, codec, height, light, fog, _ in LEVELS:
        write(DATA / f"dimension_type/{dim}.json", {
            "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": True,
            "ambient_light": light, "fixed_time": 6000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
            "piglin_safe": False, "bed_works": dim == "sunlit_lobby", "respawn_anchor_works": False, "has_raids": False,
            "min_y": 0, "height": height, "logical_height": height, "infiniburn": "#minecraft:infiniburn_overworld",
            "effects": "minecraft:the_nether"})
        write(DATA / f"dimension/{dim}.json", {"type": M + dim, "generator": {
            "type": M + "backrooms_level", "level": codec,
            "biome_source": {"type": "minecraft:fixed", "biome": M + dim}}})
        mood = "minecraft:ambient.cave" if dim != "sunlit_lobby" else "minecraft:ambient.basalt_deltas.additions"
        effects = {"fog_color": fog, "sky_color": fog, "water_color": 0x3F76E4, "water_fog_color": 0x050533,
                   "grass_color": 0x8DB360 if dim != "the_harvest" else 0x9A8A50, "foliage_color": 0x77AB2F,
                   "mood_sound": {"sound": mood, "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}}
        if dim in ("level_1", "level_4"):
            effects["ambient_sound"] = M + "backrooms.hum"
        if dim == "glowstone_cavern":
            effects["particle"] = {"options": {"type": "minecraft:white_ash"}, "probability": 0.002}
        write(DATA / f"worldgen/biome/{dim}.json", {"has_precipitation": False, "temperature": 0.8, "downfall": 0.0, "effects": effects,
                                                    "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})

    # Level 0 now goes down to the bottom of the pits and up to the top of the monolith
    path = DATA / "dimension_type/backrooms.json"
    level0 = json.loads(path.read_text(encoding="utf-8"))
    level0.update({"min_y": -64, "height": 128, "logical_height": 128})
    write(path, level0)


def loot():
    chests = {
        "level_1_supplies": [pool((2, 4), [item("almond_water", 10, (1, 2)), item("minecraft:bread", 8, (1, 3)),
                                            item("minecraft:torch", 8, (2, 6)), item("minecraft:iron_ingot", 4, (1, 3)),
                                            item("minecraft:bucket", 2), item("minecraft:flint_and_steel", 2),
                                            item("minecraft:rotten_flesh", 5, (1, 4))]),
                             pool(1, [item("backrooms_coordinates", 2), item("minecraft:empty", 10)])],
        "level_4_office": [pool((2, 5), [item("minecraft:paper", 10, (2, 8)), item("minecraft:book", 6, (1, 2)),
                                          item("minecraft:writable_book", 2), item("minecraft:ink_sac", 4, (1, 3)),
                                          item("almond_water", 6), item("minecraft:clock", 2), item("minecraft:compass", 2),
                                          item("minecraft:redstone", 4, (2, 6))]),
                           pool(1, [item("minecraft:map", 3), item("backrooms_coordinates", 2), item("minecraft:empty", 8)])],
        "harvest_farmhouse": [pool((3, 6), [item("minecraft:wheat", 10, (2, 6)), item("minecraft:hay_block", 4, (1, 2)),
                                             item("minecraft:bread", 8, (1, 4)), item("minecraft:pumpkin", 4),
                                             item("minecraft:carved_pumpkin", 2), item("minecraft:iron_hoe", 2),
                                             item("minecraft:shears", 2), item("minecraft:bone_meal", 6, (2, 6)),
                                             item("almond_water", 4)]),
                              pool(1, [item("minecraft:golden_carrot", 3, (1, 3)), item("minecraft:golden_apple", 1),
                                       item("minecraft:empty", 6)])],
        "wooden_hotel_room": [pool((2, 5), [item("minecraft:candle", 8, (1, 3)), item("minecraft:book", 6, (1, 2)),
                                             item("minecraft:string", 6, (2, 5)), item("minecraft:bread", 6, (1, 2)),
                                             item("minecraft:emerald", 3, (1, 3)), item("minecraft:gold_nugget", 6, (2, 8)),
                                             item("almond_water", 5), item("minecraft:lantern", 2)]),
                              pool(1, [item("minecraft:name_tag", 2), item("minecraft:music_disc_11", 1), item("minecraft:empty", 10)])],
        "cavern_castle": [pool((3, 6), [item("minecraft:iron_ingot", 8, (2, 6)), item("minecraft:gold_ingot", 6, (2, 5)),
                                         item("minecraft:glowstone_dust", 10, (4, 12)), item("minecraft:diamond", 3, (1, 2)),
                                         item("minecraft:emerald", 4, (1, 4)), item("almond_water", 6, (1, 2)),
                                         item("minecraft:rail", 6, (4, 12)), item("minecraft:iron_sword", 2)]),
                          pool(1, [item("minecraft:golden_apple", 3), item("portal_fluid", 2), item("backrooms_coordinates", 3),
                                   item("minecraft:empty", 6)])],
    }
    for name, pools in chests.items():
        write(DATA / f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": pools})

    survives = [{"condition": "minecraft:survives_explosion"}]
    simple = ["panel_light", "pipe", "hanging_cable", "office_carpet", "office_wall", "office_debris", "garage_concrete",
              "corrugated_metal", "lobby_tile", "sunlit_tile"]
    for name in simple:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}], "conditions": survives}]})
    write(DATA / "loot_table/blocks/dried_corn.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + "dried_corn"}],
         "conditions": survives + [{"condition": "minecraft:block_state_property", "block": M + "dried_corn",
                                    "properties": {"half": "lower"}}]}]})
    write(DATA / "loot_table/blocks/carpet_ledge.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + "carpet_ledge", "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": M + "carpet_ledge", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    write(DATA / "loot_table/entities/scarecrow.json", {"type": "minecraft:entity", "pools": [
        pool((1, 3), [item("minecraft:wheat", 6, (1, 3)), item("minecraft:string", 4, (1, 2))]),
        pool(1, [item("minecraft:carved_pumpkin", 1), item("minecraft:empty", 9)])]})


def tags():
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + b for b in ("panel_light", "pipe", "office_wall", "garage_concrete",
                                                                         "corrugated_metal", "lobby_tile", "sunlit_tile")])
    merge_tag(MC / "tags/block/replaceable_by_trees.json", [M + "dried_corn", M + "office_debris"])
    merge_tag(MC / "tags/block/slabs.json", [M + "carpet_ledge"])
    merge_tag(MC / "tags/item/slabs.json", [M + "carpet_ledge"])


def advancements():
    icons = {"level_fun": "balloon", "poolrooms": "pool_tile", "level_1": "garage_concrete", "level_4": "office_wall", "level_188": "minecraft:gray_stained_glass",
             "the_harvest": "dried_corn", "sunlit_lobby": "sunlit_tile", "wooden_hotel": "minecraft:stripped_oak_log",
             "scaffold_void": "minecraft:scaffolding", "glowstone_cavern": "minecraft:glowstone"}
    parents = {"level_fun": "level_1", "poolrooms": "level_188", "level_1": "backrooms", "level_4": "level_1", "level_188": "level_4", "the_harvest": "level_4",
               "sunlit_lobby": "the_harvest", "wooden_hotel": "level_188", "scaffold_void": "wooden_hotel",
               "glowstone_cavern": "scaffold_void"}
    for dim, *_ in LEVELS:
        frame = "challenge" if dim == "glowstone_cavern" else "goal" if dim == "sunlit_lobby" else "task"
        advancement(dim, icons[dim], parents[dim], {"arrived": {"trigger": "minecraft:changed_dimension", "conditions": {"to": M + dim}}}, frame)
    advancement("the_way_out", "exit_door", "sunlit_lobby", IMPOSSIBLE, "challenge")
    advancement("the_hole", "noclip_void", "backrooms", IMPOSSIBLE, hidden=True)
    advancement("it_saw_you", "minecraft:ender_eye", "wooden_hotel", IMPOSSIBLE, hidden=True)
    advancement("scarecrow", "minecraft:carved_pumpkin", "the_harvest", {"kill": {"trigger": "minecraft:player_killed_entity", "conditions": {
        "entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": M + "scarecrow"}}]}}})


LANG = {
    "block.rickrooms.exit_door": "Exit Door",
    "block.rickrooms.noclip_void": "The Hole",
    "block.rickrooms.panel_light": "Panel Light",
    "block.rickrooms.pipe": "Pipe",
    "block.rickrooms.hanging_cable": "Hanging Cable",
    "block.rickrooms.dried_corn": "Dried Corn",
    "block.rickrooms.office_carpet": "Office Carpet",
    "block.rickrooms.office_wall": "Office Drywall",
    "block.rickrooms.office_debris": "Office Debris",
    "block.rickrooms.garage_concrete": "Garage Concrete",
    "block.rickrooms.corrugated_metal": "Corrugated Metal",
    "block.rickrooms.lobby_tile": "Lobby Tile",
    "block.rickrooms.sunlit_tile": "Sunlit Tile",
    "block.rickrooms.monolith": "Monolith",
    "block.rickrooms.carpet_ledge": "Carpet Ledge",
    "entity.rickrooms.scarecrow": "Scarecrow",
    "entity.rickrooms.watcher": "The Watcher",
    "item.rickrooms.scarecrow_spawn_egg": "Scarecrow Spawn Egg",
    "item.rickrooms.watcher_spawn_egg": "Watcher Spawn Egg",
    "message.rickrooms.exit_door.home": "You're out. You're home. Probably.",
    "message.rickrooms.level_recorded": "Your Portal Gun recorded the way to %s",
    "level.rickrooms.level_0": "Level 0: The Lobby",
    "advancements.rickrooms.level_1.title": "The Habitable Zone",
    "advancements.rickrooms.level_1.description": "Fall out of Level 0 into the garage below",
    "advancements.rickrooms.level_4.title": "Out of Office",
    "advancements.rickrooms.level_4.description": "Find a way into the Abandoned Office",
    "advancements.rickrooms.level_188.title": "Rear Window",
    "advancements.rickrooms.level_188.description": "Stand in the Courtyard of Windows. Don't look up for too long",
    "advancements.rickrooms.the_harvest.title": "Children of the Corn",
    "advancements.rickrooms.the_harvest.description": "Reach The Harvest",
    "advancements.rickrooms.sunlit_lobby.title": "Golden Hour",
    "advancements.rickrooms.sunlit_lobby.description": "Find the Sunlit Lobby, the one safe place",
    "advancements.rickrooms.wooden_hotel.title": "Checking In",
    "advancements.rickrooms.wooden_hotel.description": "Enter the Wooden Hotel",
    "advancements.rickrooms.scaffold_void.title": "Mind the Gap",
    "advancements.rickrooms.scaffold_void.description": "Fall out of the Wooden Hotel and land in the Scaffold Void",
    "advancements.rickrooms.glowstone_cavern.title": "A Sky of Stone",
    "advancements.rickrooms.glowstone_cavern.description": "Fall all the way down, into the Glowstone Cavern",
    "advancements.rickrooms.the_way_out.title": "The Way Out",
    "advancements.rickrooms.the_way_out.description": "Walk out of the Backrooms through a red exit door",
    "advancements.rickrooms.the_hole.title": "The Hole",
    "advancements.rickrooms.the_hole.description": "Climb into the hole in the wall",
    "advancements.rickrooms.it_saw_you.title": "It Saw You",
    "advancements.rickrooms.it_saw_you.description": "Stare back at the Watcher",
    "advancements.rickrooms.scarecrow.title": "Don't Blink",
    "advancements.rickrooms.scarecrow.description": "Kill a scarecrow in The Harvest",
}


def lang():
    path = ASSETS / "lang/en_us.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(LANG)
    codecs = {"level_1": "level_1", "level_4": "level_4", "level_188": "level_188", "the_harvest": "harvest",
              "sunlit_lobby": "sunlit_lobby", "wooden_hotel": "wooden_hotel", "scaffold_void": "scaffold", "glowstone_cavern": "cavern"}
    for dim, codec, *_, name in LEVELS:
        data[f"level.rickrooms.{codec}"] = name
        data[f"dimension.rickrooms.{dim}"] = name
        data[f"biome.rickrooms.{dim}"] = name
    write(path, data)


# ================================================================================ v0.9

V09_LANG = {
    "block.rickrooms.pool_tile": "Pool Tile",
    "block.rickrooms.pool_tile_stairs": "Pool Tile Stairs",
    "block.rickrooms.pool_tile_slab": "Pool Tile Slab",
    "block.rickrooms.pool_floor_tile": "Pool Floor Tile",
    "block.rickrooms.pool_light": "Pool Light",
    "block.rickrooms.pool_drain": "Pool Drain",
    "block.rickrooms.scarecrow_head": "Scarecrow Head",
    "item.rickrooms.scarecrow_lantern": "Scarecrow Lantern",
    "item.rickrooms.scarecrow_lantern.tooltip": "Hold it, and the scarecrows keep their distance",
    "advancements.rickrooms.poolrooms.title": "Wading In",
    "advancements.rickrooms.poolrooms.description": "Find the Poolrooms. It's so quiet",
    "advancements.rickrooms.behind_you.title": "Right Behind You",
    "advancements.rickrooms.behind_you.description": "Never notice the Watcher, until it's whistling in your ear",
    "advancements.rickrooms.the_monolith.title": "It Came Through",
    "advancements.rickrooms.the_monolith.description": "Find the Monolith standing in the Overworld",
    "advancements.rickrooms.scarecrow_lantern.title": "Keep Your Distance",
    "advancements.rickrooms.scarecrow_lantern.description": "Make a Scarecrow Lantern",
    "subtitles.rickrooms.watcher.whistle": "Something whooshes past",
    "subtitles.rickrooms.watcher.whisper": "Something whispers",
    "subtitles.rickrooms.monolith.hum": "The Monolith hums",
    "subtitles.rickrooms.poolrooms.drip": "Water drips",
    "rickrooms.configuration.watcher": "The Watcher",
    "rickrooms.configuration.frequency": "How often it appears",
    "rickrooms.configuration.enabled": "Enabled",
}


def v09():
    # the Poolrooms' water is a still, glowing teal, and there's the drip of it somewhere
    path = DATA / "worldgen/biome/poolrooms.json"
    biome = json.loads(path.read_text(encoding="utf-8"))
    biome["effects"].update({"water_color": 0x3FE0C4, "water_fog_color": 0x2BA897, "fog_color": 0xD6E9E0, "sky_color": 0xD6E9E0,
                             "additions_sound": {"sound": M + "poolrooms.drip", "tick_chance": 0.004},
                             "mood_sound": {"sound": "minecraft:ambient.underwater.loop.additions", "tick_delay": 3000,
                                            "block_search_extent": 8, "offset": 2.0}})
    write(path, biome)

    write(DATA / "loot_table/chests/poolrooms.json", {"type": "minecraft:chest", "pools": [
        pool((2, 4), [item("almond_water", 10, (1, 3)), item("minecraft:sponge", 4, (1, 2)), item("minecraft:turtle_scute", 2),
                      item("minecraft:prismarine_shard", 6, (2, 6)), item("minecraft:nautilus_shell", 2), item("minecraft:heart_of_the_sea", 1),
                      item("minecraft:bucket", 4), item("minecraft:pufferfish", 3)]),
        pool(1, [item("backrooms_coordinates", 2), item("minecraft:trident", 1), item("minecraft:empty", 10)])]})
    write(DATA / "loot_table/chests/monolith_vault.json", {"type": "minecraft:chest", "pools": [
        pool((3, 5), [item("almond_water", 8, (1, 3)), item("portal_fluid", 6, (1, 3)), item("minecraft:ender_pearl", 6, (1, 3)),
                      item("minecraft:diamond", 4, (1, 3)), item("minecraft:emerald", 5, (2, 5)), item("minecraft:obsidian", 4, (2, 6))]),
        pool(1, [item("rick_notes_1", 1), item("rick_notes_2", 1), item("rick_notes_3", 1), item("backrooms_coordinates", 3),
                 item("minecraft:music_disc_11", 1), item("minecraft:echo_shard", 2, (1, 3))]),
        pool(1, [item("journal_page_1", 1), item("journal_page_2", 1), item("journal_page_3", 1), item("minecraft:empty", 3)])]})

    survives = [{"condition": "minecraft:survives_explosion"}]
    for name in ("pool_tile", "pool_tile_stairs", "pool_floor_tile", "pool_light", "scarecrow_head"):
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}], "conditions": survives}]})
    write(DATA / "loot_table/blocks/pool_tile_slab.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + "pool_tile_slab", "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": M + "pool_tile_slab", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    # the scarecrow sometimes leaves its head behind
    write(DATA / "loot_table/entities/scarecrow.json", {"type": "minecraft:entity", "pools": [
        pool((1, 3), [item("minecraft:wheat", 6, (1, 3)), item("minecraft:string", 4, (1, 2))]),
        pool(1, [item("scarecrow_head", 1)], [{"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                                "unenchanted_chance": 0.2, "enchanted_chance": {"type": "minecraft:linear", "base": 0.25,
                                                                                                "per_level_above_first": 0.05}}])]})

    ing = lambda n: {"item": n if ":" in n else M + n}
    write(DATA / "recipe/scarecrow_lantern.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
                                                   "pattern": [" C ", "WHW", " T "],
                                                   "key": {"C": ing("minecraft:chain"), "W": ing("minecraft:wheat"), "H": ing("scarecrow_head"),
                                                           "T": ing("minecraft:torch")},
                                                   "result": {"id": M + "scarecrow_lantern", "count": 1}})
    write(DATA / "recipe/pool_tile.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["QQ", "QQ"],
                                           "key": {"Q": ing("minecraft:smooth_quartz")}, "result": {"id": M + "pool_tile", "count": 4}})
    write(DATA / "recipe/pool_floor_tile.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                                                 "ingredients": [ing("pool_tile"), ing("minecraft:cyan_dye")], "result": {"id": M + "pool_floor_tile", "count": 1}})
    write(DATA / "recipe/pool_light.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                                            "ingredients": [ing("pool_tile"), ing("minecraft:glowstone_dust")], "result": {"id": M + "pool_light", "count": 1}})
    write(DATA / "recipe/pool_tile_stairs.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["T  ", "TT ", "TTT"],
                                                  "key": {"T": ing("pool_tile")}, "result": {"id": M + "pool_tile_stairs", "count": 4}})
    write(DATA / "recipe/pool_tile_slab.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["TTT"],
                                                "key": {"T": ing("pool_tile")}, "result": {"id": M + "pool_tile_slab", "count": 6}})
    for result, count in (("pool_tile_stairs", 1), ("pool_tile_slab", 2)):
        write(DATA / f"recipe/{result}_from_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": ing("pool_tile"),
                                                                 "result": {"id": M + result, "count": count}})

    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + b for b in ("pool_tile", "pool_tile_stairs", "pool_tile_slab", "pool_floor_tile", "pool_light")])
    merge_tag(MC / "tags/block/stairs.json", [M + "pool_tile_stairs"])
    merge_tag(MC / "tags/block/slabs.json", [M + "pool_tile_slab"])
    merge_tag(MC / "tags/item/stairs.json", [M + "pool_tile_stairs"])
    merge_tag(MC / "tags/item/slabs.json", [M + "pool_tile_slab"])

    # the Monolith: as rare as it gets, on open dry land
    write(DATA / "tags/worldgen/biome/has_monolith.json", {"values": ["minecraft:" + b for b in (
        "plains", "sunflower_plains", "snowy_plains", "desert", "savanna", "savanna_plateau", "badlands", "eroded_badlands",
        "meadow", "taiga", "snowy_taiga", "forest", "birch_forest", "cherry_grove", "ice_spikes")]})
    write(DATA / "worldgen/structure/monolith.json", {"type": M + "site", "kind": "monolith", "biomes": "#" + M + "has_monolith",
                                                     "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "beard_thin"})
    write(DATA / "worldgen/structure_set/monolith.json", {"structures": [{"structure": M + "monolith", "weight": 1}],
                                                         "placement": {"type": "minecraft:random_spread", "spacing": 200, "separation": 150,
                                                                       "salt": 610006}})

    advancement("poolrooms", "pool_tile", "level_188", {"arrived": {"trigger": "minecraft:changed_dimension", "conditions": {"to": M + "poolrooms"}}})
    advancement("behind_you", "minecraft:ender_eye", "backrooms", IMPOSSIBLE, hidden=True)
    advancement("the_monolith", "monolith", "backrooms", {"found": {"trigger": "minecraft:location", "conditions": {"player": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"location": {"structures": M + "monolith"}}}]}}}, "goal")
    advancement("scarecrow_lantern", "scarecrow_lantern", "the_harvest", {"lantern": {"trigger": "minecraft:inventory_changed",
                                                                                       "conditions": {"items": [{"items": M + "scarecrow_lantern"}]}}})

    sounds_path = ASSETS / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    ev = lambda name, pitch=1.0, volume=1.0: {"name": name, "type": "event", "pitch": pitch, "volume": volume}
    for key, entries in {"watcher.whistle": [{"name": "rickrooms:watcher/whistle", "volume": 0.8}],
                         "watcher.whisper": [ev("minecraft:ambient.cave", pitch=1.2, volume=0.6)],
                         "monolith.hum": [ev("minecraft:block.beacon.ambient", pitch=0.4, volume=0.5)],
                         "poolrooms.drip": [ev("minecraft:block.pointed_dripstone.drip_water", volume=0.5),
                                            ev("minecraft:block.pointed_dripstone.drip_water_into_cauldron", pitch=0.8, volume=0.4)]}.items():
        sounds[key] = {"subtitle": f"subtitles.rickrooms.{key}", "sounds": entries}
    write(sounds_path, sounds)

    lang_path = ASSETS / "lang/en_us.json"
    data = json.loads(lang_path.read_text(encoding="utf-8"))
    data.update(V09_LANG)
    write(lang_path, data)


# ================================================================================ v0.10

GUIDE = ["RICK'S FIELD GUIDE\n\nFor idiots who touch my stuff. That's you. Read it or don't, I'm not your dad.\n\nIf you die in a pocket dimension, that's on you.\n\n- R. Sanchez", 'THE PORTAL GUN\n\nCraft it: lime glass, an eye of ender, iron and a diamond.\n\nRight-click: shoot a portal and walk through. Sneak + right-click: the dial. Save places, pick where you go.', 'PORTAL FLUID\n\nEvery portal burns some. A bottle, an ender pearl, a slime ball and lime dye makes more. The gun refills itself from your pockets when it runs dry.\n\nDrops of it fling you to a saved place.', "WARP GATEWAYS\n\nIron, obsidian, a diamond, eyes of ender and portal fluid. Place two, then right-click one with the gun and then the other. Now they're linked, forever. Walk in one, out the other.", "UPGRADES\n\nSmithing table.\n\nTargeting Chip + gun + diamond: shots zap creatures straight through.\n\nBlender Chip + gun + mantis scythe: attack mode. Press R. Don't ask.", 'THE BACKROOMS\n\nFall wrong and you might noclip out of reality into Level 0. Or, smarter: find three torn journal pages in chests, add portal fluid, and program the coordinates into your gun.', "SANITY\n\nThe Backrooms eat your mind. Dark, alone, something nearby: it drains faster. Low sanity means things that aren't there.\n\nAlmond Water fixes it. The Sunlit Lobby too.", 'GETTING AROUND DOWN THERE\n\nRed EXIT doors lead to other levels. Pits drop you a level. A black hole in a wall throws you somewhere random.\n\nEvery level you reach, your gun remembers.', 'THE LEVELS\n\n0 the Lobby. 1 the garage. 4 the offices. 188 the windows. The Harvest. The Wooden Hotel, over the Scaffold Void, over the Glowstone Cavern. Level 37, the Poolrooms. Level Fun.', 'THE WAY OUT\n\nThe Sunlit Lobby is safe. Nothing hunts there, your head clears, beds work, and its red doors go home.\n\nFarmhouse doors in the Harvest lead there. So do the castles in the Cavern.', "THE HARVEST\n\nScarecrows don't move while you look at them. You can't look everywhere. Listen for the corn rustling.\n\nThey freeze in torchlight. Carry a torch. Better: a Scarecrow Lantern.", "SCARECROWS, YOURS\n\nThey drop their heads. Wear one if you're weird.\n\nHead, two wheat, a torch, a chain: a Lantern. Hold it, they back off.\n\nTwo hay bales, a head on top: one that minds your crops.", "THE WATCHER\n\nIt stands far off and watches. It can't hurt you. Look straight at it and it goes.\n\nIgnore it, and it gets bored, and gets closer. Much closer. Right behind you.", "LEVEL FUN =)\n\nThe way in: red doors in Level 1 (the garage) or Level 4 (the offices), or the Hole.\n\nThe Partygoers want you to stay. Don\'t. Don\'t hit one, and don\'t open every present.", "THE POOLROOMS\n\nWarm water, white tile, nobody. It's quiet. Too quiet? No, just quiet.\n\nThe ladder by the lounge pool goes up to a way out. The drain at the bottom of the deep one goes down.", "THE MONOLITH\n\nOne of the towers from Level 0 came through into our world. Rare, tall, broken.\n\nThe red door in it leads back where it came from. Try /locate if you're lazy.", "CRONENBERG WORLD\n\nMy fault. Long story. Rick's notes from chests + portal fluid = coordinates.\n\nEverything there is flesh, and it gets into you. Cure Serum: Almond Water, a mutagen sac, a golden carrot.", "THE COUNCIL\n\nSomewhere in the Overworld there's a round white chamber full of other versions of your friends. They trade.\n\nAnd a Microverse Battery powers furnaces forever. You're welcome.", "RICK\'S SPACE CRUISER\n\nGlass, iron blocks, a Microverse Battery. Space up, Alt down, click to shoot, V weapon, B repulsor, G portal.\n\nUpgrades go on with a right-click. Boosters, thrusters and drills come in Mk I to III.", "THE INFINITE IKEA\n\nA store that never ends, with everything in it. Crates refill every morning. Day: the staff ignore you. Night: run. A Staff Badge fools them.\n\nThe EXIT signs lie, until you're near a checkout.", "THE DEAD MALL\n\nTwo floors of empty shops round skylit courts. Nobody's here. Nobody's been here for years.\n\nA few doors at the back of the shops, and in the big stores, still lead somewhere.", "THE ENDLESS FIELD\n\nGrass and sky. It never gets later. Your head doesn't get worse here. Or better.\n\nA door standing on its own goes home. The hole with the stairs goes down.", "STILL LIFES, OF US\n\nIt copies people too. It gets them wrong. Too tall. Too many faces. Too many hands. Faces in planks.\n\nThey wear your skin. They stand still until you're close. Then they don't.", "MY GADGETS\n\nShrink Ray: stand under it, press it. Growth Lens: the other way.\n\nFreeze Ray: hit things.\n\nTime Crystal: everything near it runs faster.\n\nGoggles: ores, through rock."]

V10_LANG = {
    "item.rickrooms.field_guide": "Rick's Field Guide",
    "block.rickrooms.balloon": "Balloon",
    "block.rickrooms.present": "Present",
    "block.rickrooms.confetti": "Confetti",
    "entity.rickrooms.farm_scarecrow": "Farm Scarecrow",
    "entity.rickrooms.partygoer": "Partygoer",
    "item.rickrooms.farm_scarecrow_spawn_egg": "Farm Scarecrow Spawn Egg",
    "item.rickrooms.partygoer_spawn_egg": "Partygoer Spawn Egg",
    "message.rickrooms.partygoer.invite.0": "Partygoer: Come celebrate with us! =)",
    "message.rickrooms.partygoer.invite.1": "Partygoer: It's a party! Why are you leaving? =)",
    "message.rickrooms.partygoer.invite.2": "Partygoer: Stay a while. Stay forever. =)",
    "message.rickrooms.partygoer.invite.3": "Partygoer: Have some cake! =)",
    "message.rickrooms.partygoer.invite.4": "Partygoer: Everyone is here! Everyone! =)",
    "message.rickrooms.partygoer.turn": "The Partygoer isn't smiling any more. =)",
    "message.rickrooms.present.surprise": "Surprise! =)",
    "advancements.rickrooms.level_fun.title": "=)",
    "advancements.rickrooms.level_fun.description": "Find your way into Level Fun",
    "advancements.rickrooms.surprise.title": "Surprise!",
    "advancements.rickrooms.surprise.description": "Open a present with someone inside",
    "advancements.rickrooms.farm_scarecrow.title": "Minding the Crops",
    "advancements.rickrooms.farm_scarecrow.description": "Build a friendly scarecrow from two hay bales and a Scarecrow Head",
    "rickrooms.configuration.strangeEvents": "Strange events",
}


def v10():
    for i, text in enumerate(GUIDE):
        V10_LANG[f"guide.rickrooms.page.{i}"] = text

    write(DATA / "loot_table/chests/party_present.json", {"type": "minecraft:chest", "pools": [
        pool((1, 3), [item("minecraft:cake", 4), item("minecraft:cookie", 8, (3, 8)), item("minecraft:firework_rocket", 6, (2, 5)),
                      item("balloon", 6, (2, 4)), item("confetti", 6, (4, 10)), item("almond_water", 4), item("minecraft:emerald", 3, (1, 3)),
                      item("minecraft:golden_carrot", 3, (1, 2)), item("minecraft:name_tag", 1), item("minecraft:music_disc_cat", 1)])]})
    survives = [{"condition": "minecraft:survives_explosion"}]
    for name in ("balloon", "present", "confetti"):
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}], "conditions": survives}]})
    write(DATA / "loot_table/entities/partygoer.json", {"type": "minecraft:entity", "pools": [
        pool((0, 2), [item("confetti", 5, (1, 4)), item("balloon", 3), item("minecraft:firework_star", 2), item("minecraft:gold_nugget", 3, (1, 4))])]})
    write(DATA / "loot_table/entities/farm_scarecrow.json", {"type": "minecraft:entity", "pools": [
        pool(1, [item("minecraft:hay_block", 1, (2, 2))]), pool(1, [item("scarecrow_head", 1)])]})

    ing = lambda n: {"item": n if ":" in n else M + n}
    write(DATA / "recipe/field_guide.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                             "ingredients": [ing("minecraft:book"), ing("minecraft:lime_dye")], "result": {"id": M + "field_guide", "count": 1}})
    write(DATA / "recipe/balloon.json", {"type": "minecraft:crafting_shapeless", "category": "decorations",
                                         "ingredients": [ing("minecraft:string"), ing("minecraft:slime_ball"), ing("minecraft:red_dye")],
                                         "result": {"id": M + "balloon", "count": 3}})
    write(DATA / "recipe/confetti.json", {"type": "minecraft:crafting_shapeless", "category": "decorations",
                                          "ingredients": [ing("minecraft:paper"), ing("minecraft:red_dye"), ing("minecraft:yellow_dye"), ing("minecraft:blue_dye")],
                                          "result": {"id": M + "confetti", "count": 8}})

    advancement("surprise", "present", "level_fun", IMPOSSIBLE, hidden=True)
    advancement("farm_scarecrow", "scarecrow_head", "scarecrow_lantern", IMPOSSIBLE)

    lang_path = ASSETS / "lang/en_us.json"
    data = json.loads(lang_path.read_text(encoding="utf-8"))
    data.update(V10_LANG)
    write(lang_path, data)


def main():
    dimensions()
    loot()
    tags()
    advancements()
    lang()
    v09()
    v10()
    print("Backrooms level data written")


if __name__ == "__main__":
    main()
