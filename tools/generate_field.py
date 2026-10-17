"""The Endless Field: its dimension, sky colours, loot, the lone white door, translations. Run from the project root:

    python tools/generate_field.py
"""
import json
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import rgba, shade  # noqa: E402
from generate_cronenberg_data import ASSETS, DATA, M, advancement, write  # noqa: E402

TEX = ASSETS / "textures"


def tex(name, fn, seed=1, folder="block"):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = fn(x, y, rng)
            if c is not None:
                img.putpixel((x, y), c if len(c) == 4 else rgba(c))
    img.save(TEX / f"{folder}/{name}.png")


WHITE, TRIM, SHADOW = (244, 244, 240), (226, 226, 222), (206, 206, 202)


def door_texture(top):
    def paint(x, y, r):
        if x in (0, 15) or (top and y == 0) or (not top and y == 15):
            return TRIM
        # raised panels: two up top, two below
        px = 2 <= x <= 6 or 9 <= x <= 13
        py = (3 <= y <= 13) if top else (2 <= y <= 12)
        if px and py:
            edge = x in (2, 6, 9, 13) or y in ((3, 13) if top else (2, 12))
            return SHADOW if edge else shade(WHITE, r.uniform(0.98, 1.02))
        if not top and x == 13 and y in (0, 1):
            return (190, 170, 100)                                  # the doorknob
        return shade(WHITE, r.uniform(0.97, 1.02))
    return paint


def assets():
    tex("lone_door_top", door_texture(True), 1)
    tex("lone_door_bottom", door_texture(False), 2)
    tex("lone_door", lambda x, y, r: None if not (4 <= x <= 11) else TRIM if x in (4, 11) or y in (0, 15)
        else (190, 170, 100) if (x, y) == (10, 9) else SHADOW if y in (2, 7, 9, 14) and 5 <= x <= 10 else WHITE, 3, "item")
    state = (ASSETS / "blockstates/exit_door.json").read_text(encoding="utf-8").replace("exit_door", "lone_door")
    (ASSETS / "blockstates/lone_door.json").write_text(state, encoding="utf-8")
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for opened in ("", "_open"):
                write(ASSETS / f"models/block/lone_door_{half}_{hinge}{opened}.json", {
                    "parent": f"minecraft:block/door_{half}_{hinge}{opened}", "render_type": "minecraft:cutout",
                    "textures": {"bottom": M + "block/lone_door_bottom", "top": M + "block/lone_door_top"}})
    write(ASSETS / "models/item/lone_door.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/lone_door"}})


def entry(name, weight=1, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ":" in name else M + name, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def data():
    write(DATA / "dimension_type/endless_field.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.0, "fixed_time": 3000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False, "has_raids": False,
        "min_y": 0, "height": 96, "logical_height": 96, "infiniburn": "#minecraft:infiniburn_overworld", "effects": M + "endless_field"})
    write(DATA / "dimension/endless_field.json", {"type": M + "endless_field", "generator": {
        "type": M + "backrooms_level", "level": "endless_field", "biome_source": {"type": "minecraft:fixed", "biome": M + "endless_field"}}})
    write(DATA / "worldgen/biome/endless_field.json", {
        "has_precipitation": False, "temperature": 0.7, "downfall": 0.0,
        "effects": {"fog_color": 0xC2DBF3, "sky_color": 0x5A9BEB, "water_color": 0x3FA7E4, "water_fog_color": 0x0A3A6A,
                    "grass_color": 0x86DB4C, "foliage_color": 0x3F8A26,
                    "music": {"sound": "minecraft:music.overworld.meadow", "min_delay": 2400, "max_delay": 9600, "replace_current_music": False}},
        "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})
    # what's in the chests in the little houses: a picnic, and crayons and paper, for drawing
    write(DATA / "loot_table/chests/field_house.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "entries": [
        entry("minecraft:bread", 5, 1, 3), entry("minecraft:apple", 5, 1, 4), entry("minecraft:cookie", 4, 2, 6), entry("minecraft:sweet_berries", 3, 2, 6),
        entry("minecraft:paper", 5, 2, 8), entry("minecraft:yellow_dye", 3, 1, 3), entry("minecraft:light_blue_dye", 3, 1, 3), entry("minecraft:lime_dye", 3, 1, 3),
        entry("minecraft:red_dye", 2, 1, 3), entry("minecraft:pink_dye", 2, 1, 3), entry("minecraft:painting", 2), entry("minecraft:flower_pot", 2),
        entry("minecraft:music_disc_strad", 1), entry("minecraft:music_disc_mellohi", 1), entry("minecraft:clock", 1), entry("minecraft:map", 1),
        entry("almond_water", 2), entry("minecraft:milk_bucket", 1), entry("minecraft:cake", 1)]}]})
    write(DATA / "loot_table/blocks/lone_door.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": M + "lone_door"}], "conditions": [
            {"condition": "minecraft:block_state_property", "block": M + "lone_door", "properties": {"half": "lower"}},
            {"condition": "minecraft:survives_explosion"}]}]})
    advancement("endless_field", "minecraft:grass_block", "poolrooms", {"arrived": {"trigger": "minecraft:changed_dimension",
                                                                                    "conditions": {"to": M + "endless_field"}}}, "goal")

    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang.update({
        "level.rickrooms.endless_field": "The Endless Field",
        "dimension.rickrooms.endless_field": "The Endless Field",
        "biome.rickrooms.endless_field": "The Endless Field",
        "block.rickrooms.lone_door": "Lone Door",
        "advancements.rickrooms.endless_field.title": "Bliss",
        "advancements.rickrooms.endless_field.description": "Find the field that goes on forever",
        "guide.rickrooms.page.21": "THE ENDLESS FIELD\n\nGrass and sky. It never gets later. Your head doesn't get worse here. Or better.\n\n"
                                      "A door standing on its own goes home. The hole with the stairs goes down.",
    })
    write(path, lang)


def main():
    assets()
    data()
    print("Endless Field written")


if __name__ == "__main__":
    main()
