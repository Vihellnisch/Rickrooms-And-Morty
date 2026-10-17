"""The Abandoned City's data: its dimension, dimension type, biome (a grey-green haze, ash drifting,
no one), advancement and translations. The city itself is worldgen/level/AbandonedCityLevel. Run from the
project root:

    python tools/generate_city.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_data import ASSETS, DATA, M, advancement, write  # noqa: E402


def data():
    write(DATA / "dimension_type/abandoned_city.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.0, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False, "has_raids": False,
        "min_y": 0, "height": 192, "logical_height": 192, "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld"})
    write(DATA / "dimension/abandoned_city.json", {"type": M + "abandoned_city", "generator": {
        "type": M + "backrooms_level", "level": "abandoned_city", "biome_source": {"type": "minecraft:fixed", "biome": M + "abandoned_city"}}})
    write(DATA / "worldgen/biome/abandoned_city.json", {
        "has_precipitation": True, "temperature": 0.75, "downfall": 0.7,
        "effects": {"fog_color": 0xBBC5B4, "sky_color": 0x97A4B0, "water_color": 0x3E6B5C, "water_fog_color": 0x2A4A40,
                    "grass_color": 0x6E9A3E, "foliage_color": 0x4E8030,
                    "particle": {"options": {"type": "minecraft:white_ash"}, "probability": 0.0025},
                    "music": {"sound": "minecraft:music.overworld.old_growth_taiga", "min_delay": 2400, "max_delay": 9600, "replace_current_music": False}},
        "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})
    advancement("abandoned_city", "minecraft:moss_block", "endless_field", {"arrived": {"trigger": "minecraft:changed_dimension",
                                                                                       "conditions": {"to": M + "abandoned_city"}}}, "goal")

    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang.update({
        "level.rickrooms.abandoned_city": "The Abandoned City",
        "dimension.rickrooms.abandoned_city": "The Abandoned City",
        "biome.rickrooms.abandoned_city": "The Abandoned City",
        "advancements.rickrooms.abandoned_city.title": "Nature Finds a Way",
        "advancements.rickrooms.abandoned_city.description": "Walk the streets of the Abandoned City",
        "guide.rickrooms.page.25": "THE ABANDONED CITY\n\nA whole city. Nobody in it. Not a dog, not a bird. The trees are winning.\n\n"
                                      "Some flats still have a red door in the front entrance. One way out of the dead mall leads here.",
    })
    write(path, lang)


def main():
    data()
    print("Abandoned City data written")


if __name__ == "__main__":
    main()
