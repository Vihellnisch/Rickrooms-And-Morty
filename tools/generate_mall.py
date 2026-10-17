"""The Dead Mall: textures, models and data. Run from the project root:

    python tools/generate_mall.py
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import mix, rgba, shade  # noqa: E402
from generate_cronenberg_data import ASSETS, DATA, M, MC, advancement, merge_tag, write  # noqa: E402
from generate_levels_art import stairs_blockstate  # noqa: E402

TEX = ASSETS / "textures"
TONES = [(232, 222, 200), (150, 150, 148), (92, 160, 158), (150, 128, 170), (226, 196, 112), (170, 82, 70)]


def tex(name, fn, size=16, seed=1, folder="block"):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            c = fn(x, y, rng)
            if c is not None:
                img.putpixel((x, y), c if len(c) == 4 else rgba(c))
    img.save(TEX / f"{folder}/{name}.png")


def speckle(base, amount, rng):
    return shade(base, rng.uniform(1 - amount, 1 + amount))


def b(name):
    return M + "block/" + name


def model(name, data):
    write(ASSETS / f"models/block/{name}.json", data)


def blockstate(name, data):
    write(ASSETS / f"blockstates/{name}.json", data)


def item_model(name, parent=None, layer=None):
    write(ASSETS / f"models/item/{name}.json", {"parent": parent} if parent else {"parent": "minecraft:item/generated", "textures": {"layer0": layer}})


def element(frm, to, textures):
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        t = textures.get(face, textures.get("*"))
        if t is None:
            continue
        uv = {"north": [16 - to[0], 16 - to[1], 16 - frm[0], 16 - frm[1]], "south": [frm[0], 16 - to[1], to[0], 16 - frm[1]],
              "east": [16 - to[2], 16 - to[1], 16 - frm[2], 16 - frm[1]], "west": [frm[2], 16 - to[1], to[2], 16 - frm[1]],
              "up": [frm[0], frm[2], to[0], to[2]], "down": [frm[0], 16 - to[2], to[0], 16 - frm[2]]}[face]
        faces[face] = {"texture": t, "uv": [max(0, min(16, v)) for v in uv]}
    return {"from": list(frm), "to": list(to), "faces": faces}


FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def facing_state(name):
    blockstate(name, {"variants": {f"facing={f}": ({"model": b(name), "y": y} if y else {"model": b(name)}) for f, y in FACINGS.items()}})


def simple(name, data):
    model(name, data)
    blockstate(name, {"variants": {"": {"model": b(name)}}})
    item_model(name, b(name))


# =============================================================================== textures

def textures():
    for i, base in enumerate(TONES):
        # big glossy tiles, two to a block, with grout lines and a little speckle
        tex(f"mall_tile_{i}", lambda x, y, r, c=base: shade(c, 0.82) if x % 8 == 0 or y % 8 == 0 else
            (mix(c, (255, 255, 255), 0.18) if (x + y) % 11 == 3 and r.random() < 0.4 else speckle(c, 0.03, r)), seed=80 + i)
    tex("mall_wall", lambda x, y, r: speckle((226, 212, 186), 0.025, r), seed=90)
    tex("mall_ceiling", lambda x, y, r: (196, 194, 188) if x in (0, 15) or y in (0, 15) else
        ((170, 168, 162) if r.random() < 0.06 else speckle((236, 234, 228), 0.02, r)), seed=91)
    tex("mall_skylight", lambda x, y, r: (214, 230, 238) if x in (0, 15) or y in (0, 15) else speckle((250, 252, 255), 0.01, r), seed=92)

    def railing(x, y, r):
        if y <= 1:
            return (172, 176, 180)                                          # the steel handrail
        if y == 15:
            return (120, 124, 128)                                          # the shoe it stands in
        return (200, 226, 232, 60) if (x + y) % 9 else (230, 244, 248, 110)  # one long glass panel, with a glint
    tex("mall_railing", railing, seed=93)
    tex("mall_escalator", lambda x, y, r: (60, 62, 66) if x % 2 == 0 else (120, 124, 128), seed=94)
    tex("mall_escalator_side", lambda x, y, r: (40, 40, 44) if y < 3 else (176, 180, 186), seed=95)
    tex("mall_bench_wood", lambda x, y, r: (110, 72, 40) if y % 4 == 3 else speckle((170, 118, 66), 0.06, r), seed=96)
    tex("mall_bench_metal", lambda x, y, r: speckle((70, 72, 76), 0.05, r), seed=97)
    tex("mall_trash_can", lambda x, y, r: (150, 120, 80) if y < 3 else speckle((214, 200, 170), 0.03, r) if y % 5 else (190, 176, 146), seed=98)
    tex("gumball_base", lambda x, y, r: speckle((200, 30, 34), 0.04, r) if y % 6 else (160, 20, 24), seed=99)

    colors = [(230, 60, 60), (250, 210, 40), (70, 150, 230), (90, 200, 70), (240, 130, 40), (230, 100, 190), (250, 250, 250)]

    def globe(x, y, r):
        if y >= 9:
            return colors[(x // 2 + y // 2 * 3 + (x // 2) * (y // 2)) % len(colors)]   # gumballs heaped in the bottom
        return (220, 236, 245, 70)                                          # glass
    tex("gumball_globe", globe, seed=100)

    def gumball(x, y, r):
        d = math.hypot(x - 7.5, y - 8.5)
        if d > 5.2:
            return None
        if d < 1.6 and x < 7 and y < 8:
            return (255, 220, 225)
        return shade((236, 64, 120), 1.0 - d / 14)
    tex("gumball", gumball, seed=101, folder="item")


# =============================================================================== models

def models():
    variants = {}
    for i in range(len(TONES)):
        model(f"mall_tile_{i}", {"parent": "minecraft:block/cube_all", "textures": {"all": b(f"mall_tile_{i}")}})
        variants[f"tone={i}"] = {"model": b(f"mall_tile_{i}")}
    blockstate("mall_tile", {"variants": variants})
    item_model("mall_tile", b("mall_tile_0"))
    for name in ("mall_wall", "mall_ceiling", "mall_skylight"):
        simple(name, {"parent": "minecraft:block/cube_all", "textures": {"all": b(name)}})
    simple("mall_railing", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": b("mall_railing")}})

    # the escalator: stairs with steel treads and dark sides
    tx = {"bottom": b("mall_escalator_side"), "top": b("mall_escalator"), "side": b("mall_escalator_side")}
    model("mall_escalator", {"parent": "minecraft:block/stairs", "textures": tx})
    model("mall_escalator_inner", {"parent": "minecraft:block/inner_stairs", "textures": tx})
    model("mall_escalator_outer", {"parent": "minecraft:block/outer_stairs", "textures": tx})
    blockstate("mall_escalator", stairs_blockstate("mall_escalator"))
    item_model("mall_escalator", b("mall_escalator"))

    model("mall_bench", {"parent": "minecraft:block/block", "textures": {"particle": b("mall_bench_wood"), "w": b("mall_bench_wood"), "m": b("mall_bench_metal")},
                         "elements": [element((0, 5, 3), (16, 7, 13), {"*": "#w"}), element((1, 0, 4), (3, 5, 12), {"*": "#m"}),
                                      element((13, 0, 4), (15, 5, 12), {"*": "#m"})]})
    facing_state("mall_bench")
    item_model("mall_bench", b("mall_bench"))
    simple("mall_trash_can", {"parent": "minecraft:block/block", "textures": {"particle": b("mall_trash_can"), "t": b("mall_trash_can"),
                                                                              "lid": "minecraft:block/brown_terracotta"},
                              "elements": [element((4, 0, 4), (12, 13, 12), {"*": "#t"}), element((3.5, 13, 3.5), (12.5, 14, 12.5), {"*": "#lid"})]})
    simple("gumball_machine", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                               "textures": {"particle": b("gumball_base"), "base": b("gumball_base"), "globe": b("gumball_globe")},
                               "elements": [element((5, 0, 5), (11, 6, 11), {"*": "#base"}), element((3, 6, 3), (13, 15, 13), {"*": "#globe"}),
                                            element((6, 15, 6), (10, 16, 10), {"*": "#base"})]})
    item_model("gumball", layer=M + "item/gumball")


# =============================================================================== data

MALL_BLOCKS = ["mall_tile", "mall_wall", "mall_ceiling", "mall_skylight", "mall_railing", "mall_escalator", "mall_bench", "mall_trash_can", "gumball_machine"]


def entry(name, weight=1, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ":" in name else M + name, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def ing(n):
    return {"tag": n[1:]} if n.startswith("#") else {"item": n if ":" in n else M + n}


def shaped(name, pattern, key, count=1):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern,
                                         "key": {k: ing(v) for k, v in key.items()}, "result": {"id": M + name, "count": count}})


def data():
    write(DATA / "dimension_type/dead_mall.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": True,
        "ambient_light": 0.12, "fixed_time": 6000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
        "min_y": 0, "height": 32, "logical_height": 32, "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:the_nether"})
    write(DATA / "dimension/dead_mall.json", {"type": M + "dead_mall", "generator": {
        "type": M + "backrooms_level", "level": "dead_mall", "biome_source": {"type": "minecraft:fixed", "biome": M + "dead_mall"}}})
    write(DATA / "worldgen/biome/dead_mall.json", {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.0,
        "effects": {"fog_color": 0xCDBFA3, "sky_color": 0xCDBFA3, "water_color": 0x3F76E4, "water_fog_color": 0x050533,
                    "grass_color": 0x8DB360, "foliage_color": 0x77AB2F, "ambient_sound": M + "backrooms.hum",
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}},
        "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})

    write(DATA / "loot_table/chests/mall_shop.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "entries": [
        entry("minecraft:music_disc_mellohi", 1), entry("minecraft:music_disc_far", 1), entry("minecraft:music_disc_mall", 2), entry("minecraft:music_disc_chirp", 1),
        entry("minecraft:leather_chestplate", 3), entry("minecraft:leather_leggings", 3), entry("minecraft:leather_boots", 3), entry("minecraft:leather_helmet", 2),
        entry("minecraft:book", 3, 1, 3), entry("minecraft:paper", 3, 2, 6), entry("minecraft:cookie", 4, 2, 6), entry("minecraft:sugar", 3, 1, 4),
        entry("gumball", 4, 2, 8), entry("minecraft:iron_nugget", 4, 2, 8), entry("minecraft:gold_nugget", 3, 1, 5), entry("minecraft:clock", 1),
        entry("minecraft:compass", 1), entry("minecraft:spyglass", 1), entry("minecraft:name_tag", 1), entry("minecraft:painting", 2),
        entry("minecraft:candle", 2, 1, 3), entry("minecraft:bundle", 1), entry("almond_water", 2), entry("lingonberry_juice", 1)]}]})
    for name in MALL_BLOCKS:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    shaped("mall_tile", ["TT", "TT"], {"T": "minecraft:white_terracotta"}, 4)
    shaped("mall_wall", ["SS", "SS"], {"S": "minecraft:smooth_sandstone"}, 4)
    shaped("mall_ceiling", ["WW", "WW"], {"W": "minecraft:white_wool"}, 4)
    shaped("mall_skylight", ["GGG", "GLG", "GGG"], {"G": "minecraft:glass", "L": "minecraft:glowstone"}, 4)
    shaped("mall_railing", ["NNN", "GGG"], {"N": "minecraft:iron_nugget", "G": "minecraft:glass"}, 3)
    shaped("mall_escalator", ["I  ", "II ", "III"], {"I": "minecraft:iron_ingot"}, 4)
    shaped("mall_bench", ["SSS", "I I"], {"S": "#minecraft:wooden_slabs", "I": "minecraft:iron_ingot"}, 2)
    shaped("mall_trash_can", ["I I", "ISI", "III"], {"I": "minecraft:iron_nugget", "S": "minecraft:smooth_sandstone"})
    shaped("gumball_machine", [" G ", "GSG", " R "], {"G": "minecraft:glass", "S": "minecraft:sugar", "R": "minecraft:red_concrete"})

    advancement("dead_mall", "gumball_machine", "level_188", {"arrived": {"trigger": "minecraft:changed_dimension", "conditions": {"to": M + "dead_mall"}}})
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + b for b in ("mall_tile", "mall_wall", "mall_skylight", "mall_escalator", "mall_trash_can")])
    merge_tag(MC / "tags/block/mineable/axe.json", [M + "mall_bench"])
    merge_tag(MC / "tags/block/stairs.json", [M + "mall_escalator"])
    merge_tag(MC / "tags/item/stairs.json", [M + "mall_escalator"])

    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang.update({
        "level.rickrooms.dead_mall": "The Dead Mall",
        "dimension.rickrooms.dead_mall": "The Dead Mall",
        "biome.rickrooms.dead_mall": "The Dead Mall",
        "block.rickrooms.mall_tile": "Mall Tile",
        "block.rickrooms.mall_wall": "Mall Wall",
        "block.rickrooms.mall_ceiling": "Drop Ceiling",
        "block.rickrooms.mall_skylight": "Skylight",
        "block.rickrooms.mall_railing": "Glass Railing",
        "block.rickrooms.mall_escalator": "Escalator",
        "block.rickrooms.mall_bench": "Mall Bench",
        "block.rickrooms.mall_trash_can": "Trash Can",
        "block.rickrooms.gumball_machine": "Gumball Machine",
        "item.rickrooms.gumball": "Gumball",
        "message.rickrooms.gumball.coin": "Insert coin (an iron or gold nugget)",
        "advancements.rickrooms.dead_mall.title": "Last Call",
        "advancements.rickrooms.dead_mall.description": "Wander into the Dead Mall",
        "commands.rickrooms.goto.done": "Sent %s player(s) to %s",
        "commands.rickrooms.goto.unknown": "There's no place called %s",
        "guide.rickrooms.page.20": "THE DEAD MALL\n\nTwo floors of empty shops round skylit courts. Nobody's here. Nobody's been here for years.\n\n"
                                      "A few doors at the back of the shops, and in the big stores, still lead somewhere.",
    })
    write(path, lang)


def main():
    textures()
    models()
    data()
    print("Dead Mall written")


if __name__ == "__main__":
    main()
