"""v0.23: five new levels. Drop Limit, the Facade and the Staircase (from the user's pictures),
Froopyland and Run For Your Life (Level !). Their dimensions, dimension types, biomes, the new
blocks' drops, item models and tool tags, advancements, Field Guide pages and translations. The
levels themselves are worldgen/level/{DropLimit,Facade,Staircase,Froopyland,Run}Level; the block
art lives in assets already. Run from the project root:

    python tools/generate_v23.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_data import ASSETS, DATA, M, MC, advancement, merge_tag, write  # noqa: E402

# id (dimension and lang), generator level name, height, sky effects, fixed time, beds, biome effects
LEVELS = [
    ("drop_limit", "drop_limit", 192, M + "drop_limit", 6000, True,
     {"fog_color": 0xCFE0F4, "sky_color": 0x2A5BE0, "water_color": 0x3F76E4, "water_fog_color": 0x050533, "grass_color": 0x7CC043,
      "foliage_color": 0x7BC850, "music": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 2400, "max_delay": 9600,
                                           "replace_current_music": False}}),
    ("the_facade", "facade", 384, M + "the_facade", 6000, True,
     {"fog_color": 0xB9BBBD, "sky_color": 0xB4B7BA, "water_color": 0x3F76E4, "water_fog_color": 0x050533, "grass_color": 0x7CA04A,
      "foliage_color": 0x6A9040, "ambient_sound": "minecraft:block.beacon.ambient"}),
    ("the_staircase", "staircase", 256, M + "the_staircase", 6000, True,
     {"fog_color": 0xD8E6F6, "sky_color": 0x3D6FD6, "water_color": 0x3F76E4, "water_fog_color": 0x050533, "grass_color": 0x7CC043,
      "foliage_color": 0x62B02E, "music": {"sound": "minecraft:music.overworld.flower_forest", "min_delay": 2400, "max_delay": 9600,
                                           "replace_current_music": False}}),
    ("froopyland", "froopyland", 192, M + "froopyland", 6000, True,
     {"fog_color": 0xFFE6F4, "sky_color": 0x7FC8FF, "water_color": 0xDDF4FF, "water_fog_color": 0xDDF4FF, "grass_color": 0x9BE07A,
      "foliage_color": 0x9BE07A, "music": {"sound": "minecraft:music.overworld.cherry_grove", "min_delay": 1200, "max_delay": 4800,
                                           "replace_current_music": False}}),
    ("run_for_your_life", "run_for_your_life", 64, "minecraft:the_nether", 6000, False,
     {"fog_color": 0x220404, "sky_color": 0x220404, "water_color": 0x3F76E4, "water_fog_color": 0x050533, "grass_color": 0x7CA04A,
      "foliage_color": 0x6A9040}),
]

BLOCKS = {"cloud": "shovel", "air_conditioner": "pickaxe", "froopy_turf_pink": "shovel", "froopy_turf_mint": "shovel",
          "froopy_turf_lavender": "shovel", "froopy_turf_butter": "shovel", "red_exit_sign": "pickaxe"}

LANG = {
    # the levels, by their names on arrival, on the gun's dial, and in F3
    "level.rickrooms.drop_limit": "Drop Limit", "dimension.rickrooms.drop_limit": "Drop Limit", "biome.rickrooms.drop_limit": "Drop Limit",
    "level.rickrooms.facade": "The Facade", "dimension.rickrooms.the_facade": "The Facade", "biome.rickrooms.the_facade": "The Facade",
    "level.rickrooms.staircase": "The Staircase", "dimension.rickrooms.the_staircase": "The Staircase", "biome.rickrooms.the_staircase": "The Staircase",
    "level.rickrooms.froopyland": "Froopyland", "dimension.rickrooms.froopyland": "Froopyland", "biome.rickrooms.froopyland": "Froopyland",
    "level.rickrooms.run_for_your_life": "RUN FOR YOUR LIFE", "dimension.rickrooms.run_for_your_life": "Run For Your Life",
    "biome.rickrooms.run_for_your_life": "Run For Your Life",
    # blocks
    "block.rickrooms.cloud": "Cloud", "block.rickrooms.air_conditioner": "Air Conditioner",
    "block.rickrooms.froopy_turf_pink": "Pink Froopy Turf", "block.rickrooms.froopy_turf_mint": "Mint Froopy Turf",
    "block.rickrooms.froopy_turf_lavender": "Lavender Froopy Turf", "block.rickrooms.froopy_turf_butter": "Butter Froopy Turf",
    "block.rickrooms.red_exit_sign": "Red EXIT Sign",
    # advancements
    "advancements.rickrooms.drop_limit.title": "DROP LIMIT ?????",
    "advancements.rickrooms.drop_limit.description": "Find the road that runs off the edge of the sky",
    "advancements.rickrooms.the_facade.title": "Climbing the Walls",
    "advancements.rickrooms.the_facade.description": "Find yourself on the outside of the Facade",
    "advancements.rickrooms.the_staircase.title": "Which Way Is Up?",
    "advancements.rickrooms.the_staircase.description": "Get lost on the Staircase",
    "advancements.rickrooms.froopyland.title": "Froopyland",
    "advancements.rickrooms.froopyland.description": "Visit the world Rick made for Beth",
    "advancements.rickrooms.run_for_your_life.title": "RUN FOR YOUR LIFE!",
    "advancements.rickrooms.run_for_your_life.description": "Arrive in a hall with something behind you",
    "advancements.rickrooms.made_it.title": "You Made It",
    "advancements.rickrooms.made_it.description": "Find a way out of Run For Your Life",
    # Field Guide
    "guide.rickrooms.page.26": "DROP LIMIT\n\nIslands of grass over a sea of cloud. The roads run off the edges. Read the signs.\n\n"
                                  "Clouds don't hold you up. Step off and you sink right through, and fall somewhere worse.",
    "guide.rickrooms.page.27": "THE FACADE\n\nOne wall. Windows and air conditioners, up and down into the fog, forever.\n\n"
                                  "Walk the ledges, climb the ladders, cross the catwalks. Some windows are open. Some rooms have a red door.",
    "guide.rickrooms.page.28": "THE STAIRCASE\n\nStairs in the sky, going every way at once. Landings, arches, more stairs.\n\n"
                                  "The doors in the arches mostly lead to Drop Limit. Fall, and you end up there anyway.",
    "guide.rickrooms.page.29": "FROOPYLAND\n\nI made it for Beth. Soft ground, bouncy, nobody gets hurt. The river's breathable. "
                                  "Your head clears here.\n\nDoors in the grass go home. Don't go in the hole. Tommy's hole.",
    "guide.rickrooms.page.30": "RUN FOR YOUR LIFE\n\nA maze of halls. Red light. Something behind you the second you arrive.\n\n"
                                  "Don't stop. Pick a way at the forks. Some dead ends have the one door that opens. Some don't.",
}


def dimensions():
    for dim, level, height, effects, time, beds, biome in LEVELS:
        write(DATA / f"dimension_type/{dim}.json", {
            "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": dim != "run_for_your_life",
            "has_ceiling": dim == "run_for_your_life", "ambient_light": 0.0, "fixed_time": time,
            "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "piglin_safe": False, "bed_works": beds,
            "respawn_anchor_works": False, "has_raids": False, "min_y": 0, "height": height, "logical_height": height,
            "infiniburn": "#minecraft:infiniburn_overworld", "effects": effects})
        write(DATA / f"dimension/{dim}.json", {"type": M + dim, "generator": {
            "type": M + "backrooms_level", "level": level, "biome_source": {"type": "minecraft:fixed", "biome": M + dim}}})
        write(DATA / f"worldgen/biome/{dim}.json", {"has_precipitation": False, "temperature": 0.7, "downfall": 0.0, "effects": biome,
                                                    "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})


def blocks():
    for name, tool in BLOCKS.items():
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        write(ASSETS / f"models/item/{name}.json", {"parent": f"{M}block/{name}"})
    for tool in ("shovel", "pickaxe"):
        merge_tag(MC / f"tags/block/mineable/{tool}.json", [M + n for n, t in BLOCKS.items() if t == tool])


def advancements():
    arrive = lambda dim: {"arrived": {"trigger": "minecraft:changed_dimension", "conditions": {"to": M + dim}}}
    advancement("drop_limit", "cloud", "backrooms", arrive("drop_limit"), "goal")
    advancement("the_facade", "air_conditioner", "backrooms", arrive("the_facade"))
    advancement("the_staircase", "minecraft:quartz_stairs", "backrooms", arrive("the_staircase"))
    advancement("froopyland", "froopy_turf_pink", "backrooms", arrive("froopyland"), "goal")
    advancement("run_for_your_life", "red_exit_sign", "backrooms", arrive("run_for_your_life"))
    advancement("made_it", "exit_door", "run_for_your_life", {"escaped": {"trigger": "minecraft:changed_dimension", "conditions": {
        "from": M + "run_for_your_life", "to": M + "sunlit_lobby"}}}, "challenge")


def lang():
    path = ASSETS / "lang/en_us.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(LANG)
    write(path, data)


def main():
    dimensions()
    blocks()
    advancements()
    lang()
    print("v0.23 data written")


if __name__ == "__main__":
    main()
