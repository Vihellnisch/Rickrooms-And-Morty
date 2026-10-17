"""Textures, block/item models and blockstates for the new Backrooms levels (v0.8). Run from the
project root:

    python tools/generate_levels_art.py

Reuses the helpers of generate_cronenberg_art.py. Replace any PNG with hand-made art whenever
you like (keep the player UV layout for the scarecrow and the Watcher).
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import (ASSETS, TEX, Noise, block, humanoid, mix, rgba, shade, simple_face,  # noqa: E402
                                     skin, sprite, tileable)

M = "rickrooms:"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def jit(rng, c, amount):
    return tuple(max(0, min(255, int(v + rng.randint(-amount, amount)))) for v in c[:3])


# =============================================================================== blocks

def block_textures():
    n1, n2, n3 = tileable(Noise(801, 1.4)), tileable(Noise(802, 1.1)), tileable(Noise(803, 0.9))

    # --- the red exit door: painted steel, a wired window and a lit EXIT sign up top, a push bar below
    red, red_dark, frame = (170, 30, 28), (112, 18, 18), (70, 16, 16)
    exit_letters = ["XXX.X.X.X.XXX", "X...X.X.X..X.", "XX...X..X..X.", "X...X.X.X..X.", "XXX.X.X.X..X."]

    def door_base(x, y, rng):
        if x in (0, 15):
            return shade(frame, 0.9)
        c = mix(red, red_dark, n1(x, y) * 0.5 + rng.uniform(0, 0.12))
        if rng.random() < 0.04:
            c = (130, 100, 90)                                    # chipped paint
        return c

    def door_top(x, y, rng):
        if y == 0:
            return frame
        if 1 <= y <= 7 and 1 <= x <= 14:
            if y == 1 or y == 7 or x == 1 or x == 14:
                return (30, 30, 30)
            letter_y, letter_x = y - 2, x - 2
            if 0 <= letter_y < 5 and 0 <= letter_x < 12 and exit_letters[letter_y][letter_x] == "X":
                return (120, 255, 130)                            # the sign glows green
            return (20, 60, 24)
        if 9 <= y <= 14 and 4 <= x <= 11:
            if y in (9, 14) or x in (4, 11):
                return (60, 60, 60)
            return (170, 190, 200) if (x + y) % 3 else (120, 130, 140)   # wired glass
        return door_base(x, y, rng)
    block("exit_door_top", door_top, 810)

    def door_bottom(x, y, rng):
        if y == 15:
            return frame
        if y in (3, 4) and 2 <= x <= 13:
            return (190, 190, 180) if y == 3 else (120, 120, 115)          # push bar
        if y == 5 and x in (2, 13):
            return (90, 90, 90)
        if y >= 12 and 1 <= x <= 14:
            return shade(door_base(x, y, rng), 0.75)                        # kick plate
        return door_base(x, y, rng)
    block("exit_door_bottom", door_bottom, 811)

    block("noclip_void", lambda x, y, rng: (rng.randint(0, 4), rng.randint(0, 3), rng.randint(0, 6)), 812)

    def panel(x, y, rng):
        if x in (0, 15) or y in (0, 15):
            return (170, 170, 165)
        if x in (1, 14) or y in (1, 14):
            return (205, 205, 200)
        if (x + y) % 2 == 0:
            return (250, 252, 244)                                # prismatic diffuser
        return (236, 240, 230)
    block("panel_light", panel, 813)

    def pipe(x, y, rng):
        c = mix((190, 192, 186), (140, 142, 138), abs(x - 7.5) / 7.5)
        if y % 8 == 0:
            c = shade(c, 0.8)                                     # joint
        if rng.random() < 0.05:
            c = (150, 110, 70)                                    # rust spot
        return jit(rng, c, 4)
    block("pipe", pipe, 814)
    block("pipe_end", lambda x, y, rng: (60, 60, 60) if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 6 else (160, 162, 156), 815)

    def cable(x, y, rng):
        wave = 7 + round(math.sin(y / 16 * math.tau) * 0.6)
        if x in (wave, wave + 1):
            return rgba(jit(rng, (28, 28, 30), 5))
        return (0, 0, 0, 0)
    block("hanging_cable", cable, 816)

    def cable_tip(x, y, rng):
        if y < 9:
            return cable(x, y, rng)
        if 6 <= x <= 9 and 9 <= y <= 13:
            return rgba((210, 210, 200) if y == 9 else (40, 40, 44))       # the plug
        if y == 14 and x in (6, 9):
            return rgba((200, 170, 60))                                    # prongs
        return (0, 0, 0, 0)
    block("hanging_cable_tip", cable_tip, 817)

    stalk, dead_leaf, husk = (150, 124, 70), (120, 96, 56), (196, 172, 110)

    def corn(top):
        def f(x, y, rng):
            stems = (3, 8, 12)
            for s in stems:
                if x == s + (1 if top and y < 5 and s == 8 else 0):
                    return rgba(jit(rng, stalk, 8))
            # drooping, brittle leaves
            for s, dy, d in ((3, 10, 1), (8, 5, -1), (12, 12, -1), (3, 2, 1), (12, 3, -1)):
                for k in range(1, 4):
                    if x == s + d * k and y == dy + k // 2 + (0 if top else 1):
                        return rgba(jit(rng, dead_leaf, 10))
            if top and 6 <= y <= 10 and x in (9, 10):
                return rgba(jit(rng, husk, 10))                                # a withered ear
            if top and y < 2 and x in (2, 4, 7, 9, 11, 13) and rng.random() < 0.6:
                return rgba(jit(rng, (170, 140, 80), 10))                      # tassels
            return (0, 0, 0, 0)
        return f
    block("dried_corn_bottom", corn(False), 818)
    block("dried_corn_top", corn(True), 819)

    def office_carpet(x, y, rng):
        c = mix((104, 110, 118), (84, 88, 96), n2(x, y))
        if (x * 7 + y * 3) % 11 == 0:
            c = (132, 124, 110)                                   # flecks
        if rng.random() < 0.08:
            c = shade(c, 0.85)
        return jit(rng, c, 5)
    block("office_carpet", office_carpet, 820)

    def office_wall(x, y, rng):
        c = mix((214, 208, 190), (196, 188, 168), n3(x, y) * 0.8)
        if rng.random() < 0.03:
            c = (170, 160, 140)
        return jit(rng, c, 3)
    block("office_wall", office_wall, 821)

    def debris(x, y, rng):
        v = rng.random()
        if v < 0.1:
            return rgba((236, 234, 224))                          # paper
        if v < 0.16:
            return rgba((196, 192, 176))                          # ceiling tile crumbs
        if v < 0.19:
            return rgba((60, 60, 64))
        return (0, 0, 0, 0)
    block("office_debris", debris, 822)

    def garage(x, y, rng):
        c = mix((150, 150, 146), (118, 118, 116), n1(x, y))
        if n2(x, y) > 0.72:
            c = shade(c, 0.7)                                     # oil stain
        if (x == int(7 + 3 * math.sin(y / 3)) and rng.random() < 0.7):
            c = shade(c, 0.6)                                     # crack
        return jit(rng, c, 6)
    block("garage_concrete", garage, 823)

    def corrugated(x, y, rng):
        rib = (math.cos(x / 4 * math.tau) + 1) / 2
        c = mix((88, 108, 112), (150, 170, 170), rib)
        if n3(x, y) > 0.7:
            c = mix(c, (140, 84, 44), 0.6)                        # rust
        return jit(rng, c, 4)
    block("corrugated_metal", corrugated, 824)

    def tile(base, grout, bright=1.0):
        def f(x, y, rng):
            if x % 8 == 0 or y % 8 == 0:
                return shade(grout, bright)
            c = mix(base, shade(base, 0.9), n2(x, y) * 0.6)
            if (x % 8 == 1 or y % 8 == 1):
                c = shade(c, 1.05)                                # bevel
            return jit(rng, shade(c, bright), 3)
        return f
    block("lobby_tile", tile((220, 204, 170), (160, 146, 120)), 825)
    block("sunlit_tile", tile((255, 222, 150), (220, 180, 110), 1.0), 826)

    def monolith(x, y, rng):
        c = (62, 46, 36) if x % 4 in (0, 1) else (46, 34, 28)
        return jit(rng, mix(c, shade(c, 0.8), n1(x, y) * 0.5), 3)
    block("monolith", monolith, 827)


def block_models():
    cubes = ["noclip_void", "panel_light", "office_carpet", "office_wall", "garage_concrete", "corrugated_metal",
             "lobby_tile", "sunlit_tile", "monolith"]
    for name in cubes:
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M + f"block/{name}"}})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": M + f"block/{name}"}}})
        write(ASSETS / f"models/item/{name}.json", {"parent": M + f"block/{name}"})

    # --- the exit door, laid out like the vanilla iron door
    for part in ("bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
                 "top_left", "top_left_open", "top_right", "top_right_open"):
        write(ASSETS / f"models/block/exit_door_{part}.json", {"parent": f"minecraft:block/door_{part}", "render_type": "minecraft:cutout",
                                                               "textures": {"bottom": M + "block/exit_door_bottom", "top": M + "block/exit_door_top"}})
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, rot in base.items():
        for half, part in (("lower", "bottom"), ("upper", "top")):
            for hinge, extra in (("left", 90), ("right", 270)):
                for is_open in (False, True):
                    model = M + f"block/exit_door_{part}_{hinge}" + ("_open" if is_open else "")
                    y = (rot + (extra if is_open else 0)) % 360
                    v = {"model": model}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={str(is_open).lower()}"] = v
    write(ASSETS / "blockstates/exit_door.json", {"variants": variants})
    write(ASSETS / "models/item/exit_door.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/exit_door"}})

    # --- pipe: a thin column along its axis
    face = lambda tex, uv=None: {"texture": tex, **({"uv": uv} if uv else {})}
    write(ASSETS / "models/block/pipe.json", {"parent": "minecraft:block/block", "textures": {"side": M + "block/pipe", "end": M + "block/pipe_end",
                                                                                             "particle": M + "block/pipe"},
                                              "elements": [{"from": [5, 0, 5], "to": [11, 16, 11], "faces": {
                                                  "north": face("#side", [5, 0, 11, 16]), "south": face("#side", [5, 0, 11, 16]),
                                                  "east": face("#side", [5, 0, 11, 16]), "west": face("#side", [5, 0, 11, 16]),
                                                  "up": face("#end", [5, 5, 11, 11]), "down": face("#end", [5, 5, 11, 11])}}]})
    write(ASSETS / "blockstates/pipe.json", {"variants": {"axis=y": {"model": M + "block/pipe"},
                                                          "axis=z": {"model": M + "block/pipe", "x": 90},
                                                          "axis=x": {"model": M + "block/pipe", "x": 90, "y": 90}}})
    write(ASSETS / "models/item/pipe.json", {"parent": M + "block/pipe"})

    # --- plants and cables: crosses
    for name in ("hanging_cable", "hanging_cable_tip", "dried_corn_bottom", "dried_corn_top"):
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                     "textures": {"cross": M + f"block/{name}"}})
    write(ASSETS / "blockstates/hanging_cable.json", {"variants": {"tip=false": {"model": M + "block/hanging_cable"},
                                                                   "tip=true": {"model": M + "block/hanging_cable_tip"}}})
    write(ASSETS / "blockstates/dried_corn.json", {"variants": {"half=lower": {"model": M + "block/dried_corn_bottom"},
                                                                "half=upper": {"model": M + "block/dried_corn_top"}}})
    write(ASSETS / "models/item/hanging_cable.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "block/hanging_cable_tip"}})
    write(ASSETS / "models/item/dried_corn.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "block/dried_corn_top"}})

    # --- debris on the office floor: a see-through carpet layer
    write(ASSETS / "models/block/office_debris.json", {"parent": "minecraft:block/carpet", "render_type": "minecraft:cutout",
                                                       "textures": {"wool": M + "block/office_debris"}})
    write(ASSETS / "blockstates/office_debris.json", {"variants": {"": {"model": M + "block/office_debris"}}})
    write(ASSETS / "models/item/office_debris.json", {"parent": M + "block/office_debris"})

    # --- carpet ledges: the benches of Level 0, a slab of the same damp carpet
    carpet = {"bottom": M + "block/backrooms_carpet", "top": M + "block/backrooms_carpet", "side": M + "block/backrooms_carpet"}
    write(ASSETS / "models/block/carpet_ledge.json", {"parent": "minecraft:block/slab", "textures": carpet})
    write(ASSETS / "models/block/carpet_ledge_top.json", {"parent": "minecraft:block/slab_top", "textures": carpet})
    write(ASSETS / "models/block/carpet_ledge_double.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/backrooms_carpet"}})
    write(ASSETS / "blockstates/carpet_ledge.json", {"variants": {"type=bottom": {"model": M + "block/carpet_ledge"},
                                                                  "type=top": {"model": M + "block/carpet_ledge_top"},
                                                                  "type=double": {"model": M + "block/carpet_ledge_double"}}})
    write(ASSETS / "models/item/carpet_ledge.json", {"parent": M + "block/carpet_ledge"})

    for egg in ("scarecrow_spawn_egg", "watcher_spawn_egg"):
        write(ASSETS / f"models/item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})


def item_textures():
    door = sprite([
        "....KKKKKKK.....",
        "....KGGGGGK.....",
        "....KKKKKKK.....",
        "....RRRRRRR.....",
        "....RrWWWrR.....",
        "....RrWWWrR.....",
        "....RrrrrrR.....",
        "....RRRRRRR.....",
        "....RrrrrrR.....",
        "....RBBBBBR.....",
        "....RrrrrrR.....",
        "....RrrrrrR.....",
        "....RrrrrrR.....",
        "....RddddddR....",
        "....RddddddR....",
        "....KKKKKKKK....",
    ], {"K": (40, 20, 20), "G": (120, 255, 130), "R": (150, 26, 24), "r": (186, 40, 36), "W": (170, 190, 200),
        "B": (200, 200, 190), "d": (120, 22, 22)})
    door.save(TEX / "item/exit_door.png")


# =============================================================================== entities

def entity_textures():
    burlap, burlap_dark = (176, 150, 100), (134, 110, 70)
    straw = (220, 190, 100)
    coat = (26, 24, 26)
    burlap_noise = Noise(831, 1.2)

    def face(x, y):
        # stitched cross-eyes and a sewn-shut grin
        if (y, x) in ((2, 1), (4, 1), (3, 2), (2, 3), (4, 3)):
            return (20, 16, 12)
        if (y, x) in ((2, 5), (4, 5), (3, 6), (2, 7), (4, 7)):
            return (20, 16, 12)
        if y == 6 and 1 <= x <= 6:
            return (40, 24, 18)
        if y in (5, 7) and x in (2, 4, 6):
            return (40, 24, 18)                                   # stitches
        return None

    def extra(name, fname, x, y, w, h, rng):
        if name == "head":
            if fname == "front":
                f = face(x, y)
                if f is not None:
                    return rgba(f)
            return rgba(mix(burlap, burlap_dark, burlap_noise(x + len(fname) * 8, y) * 0.8 + ((x + y) % 2) * 0.1))
        if name == "hat":
            # the sack tied off at the neck, straw sticking out
            if fname not in ("top", "bottom") and y >= 6 and rng.random() < 0.5:
                return rgba(jit(rng, straw, 20))
            return None
        if name in ("right_arm", "left_arm") and y >= 10:
            return rgba(jit(rng, straw, 20))                      # straw hands
        if name in ("right_sleeve", "left_sleeve") and 8 <= y < 11 and rng.random() < 0.6:
            return rgba(jit(rng, straw, 20))                      # straw bursting from the cuffs
        return None

    scarecrow = skin(False, humanoid(burlap, None, coat, (80, 60, 40), (44, 32, 20), jacket=(34, 30, 34), seed=830, extra=extra), 830)
    scarecrow.save(TEX / "entity/scarecrow.png")

    pale = (214, 204, 196)

    def watcher_face(x, y):
        if y in (3, 4) and x in (1, 2, 5, 6):
            return (8, 8, 10)                                     # blank sockets; the eyes glow in a layer
        if y == 6 and 3 <= x <= 4:
            return (170, 150, 146)
        return None

    watcher = skin(False, humanoid(pale, (40, 34, 30), (120, 120, 116), (50, 50, 56), (30, 30, 30), watcher_face, seed=840), 840)
    watcher.save(TEX / "entity/watcher.png")
    eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x in (9, 10, 13, 14):
        eyes.putpixel((x, 11), (255, 255, 255, 255))
        eyes.putpixel((x, 12), (220, 220, 230, 255))
    eyes.save(TEX / "entity/watcher_eyes.png")


# =============================================================================== v0.9: the Poolrooms, scarecrow head and lantern

def stairs_blockstate(name):
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, rot in base.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = M + f"block/{name}" + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                if half == "bottom":
                    delta = 270 if shape.endswith("left") else 0
                else:
                    delta = 90 if shape.endswith("right") else 0
                v = {"model": model}
                y = (rot + delta) % 360
                if y:
                    v["y"] = y
                if half == "top":
                    v["x"] = 180
                if half == "top" or shape != "straight":
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def v09_textures():
    def tiles(base, grout, spread, seed_noise, glint=True):
        noise = tileable(Noise(seed_noise, 1.3))

        def f(x, y, rng):
            if x % 4 == 3 or y % 4 == 3:
                return jit(rng, grout, 3)
            c = mix(base, shade(base, spread), noise(x // 4 * 4, y // 4 * 4) * 0.8 + rng.uniform(0, 0.1))
            if glint and x % 4 == 0 and y % 4 == 0:
                c = mix(c, (255, 255, 255), 0.35)                  # a glint on each little tile
            return c
        return f
    block("pool_tile", tiles((238, 243, 236), (186, 196, 190), 0.95, 901), 910)
    block("pool_floor_tile", tiles((72, 196, 174), (40, 138, 124), 0.88, 902), 911)

    def glow(x, y, rng):
        d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
        return mix((252, 255, 250), (214, 238, 226), max(0.0, d - 0.55) / 0.45)
    block("pool_light", glow, 912)

    def drain(x, y, rng):
        if x in (0, 15) or y in (0, 15):
            return (120, 128, 126)
        if x % 3 == 1:
            return (150, 158, 156) if y % 5 else (110, 118, 116)   # grate bars
        return (14, 20, 22)
    block("pool_drain", drain, 913)

    # the scarecrow's head: sackcloth, a stitched face, tied off at the top
    burlap, burlap_dark, twine = (176, 150, 100), (134, 110, 70), (96, 72, 40)
    n = tileable(Noise(920, 1.1))

    def cloth(x, y, rng):
        c = mix(burlap, burlap_dark, n(x, y) * 0.7 + ((x + y) % 2) * 0.12)
        return jit(rng, c, 6)

    def front(x, y, rng):
        stitched = {(4, 3), (6, 3), (5, 4), (4, 5), (6, 5), (9, 3), (11, 3), (10, 4), (9, 5), (11, 5)}
        if (x, y) in stitched:
            return (22, 16, 12)
        if y == 10 and 3 <= x <= 12:
            return (44, 26, 18)                                     # the sewn-shut grin
        if y in (9, 11) and x in (4, 6, 8, 10, 12):
            return (44, 26, 18)
        return cloth(x, y, rng)
    block("scarecrow_head_front", front, 921)

    def side(x, y, rng):
        if x == 8 and y % 2 == 0:
            return twine                                            # the seam
        if y >= 14 and rng.random() < 0.5:
            return jit(rng, (220, 190, 100), 20)                    # straw poking out of the neck
        return cloth(x, y, rng)
    block("scarecrow_head_side", side, 922)

    def top(x, y, rng):
        d = abs(x - 7.5) + abs(y - 7.5)
        if d < 2.2:
            return twine
        if int(math.atan2(y - 7.5, x - 7.5) * 4) % 2 == 0 and d < 7:
            return shade(cloth(x, y, rng), 0.85)                    # gathered folds
        return cloth(x, y, rng)
    block("scarecrow_head_top", top, 923)

    lantern = sprite([
        "......KK........",
        ".....K..K.......",
        "......KK........",
        "....IIIIII......",
        "...IsssssSI.....",
        "...IsBbBbSI.....",
        "...IsFFFFSI.....",
        "...IsFoFoSI.....",
        "...IsFFFFSI.....",
        "...IsFmmFSI.....",
        "...IsFFFFSI.....",
        "...IyyyyyyI.....",
        "....IIIIII......",
        "...y.y..y.y.....",
        "....y..y........",
        "................",
    ], {"K": (60, 60, 64), "I": (52, 54, 60), "s": (255, 214, 120), "S": (230, 170, 70), "B": (176, 150, 100), "b": (134, 110, 70),
        "F": (190, 160, 105), "o": (20, 14, 10), "m": (70, 36, 20), "y": (224, 194, 96)})
    lantern.save(TEX / "item/scarecrow_lantern.png")


def v09_models():
    for name in ("pool_tile", "pool_floor_tile", "pool_light", "pool_drain"):
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M + f"block/{name}"}})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": M + f"block/{name}"}}})
        write(ASSETS / f"models/item/{name}.json", {"parent": M + f"block/{name}"})
    t = {"bottom": M + "block/pool_tile", "top": M + "block/pool_tile", "side": M + "block/pool_tile"}
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        write(ASSETS / f"models/block/pool_tile_stairs{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": t})
    write(ASSETS / "blockstates/pool_tile_stairs.json", stairs_blockstate("pool_tile_stairs"))
    write(ASSETS / "models/item/pool_tile_stairs.json", {"parent": M + "block/pool_tile_stairs"})
    write(ASSETS / "models/block/pool_tile_slab.json", {"parent": "minecraft:block/slab", "textures": t})
    write(ASSETS / "models/block/pool_tile_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    write(ASSETS / "blockstates/pool_tile_slab.json", {"variants": {"type=bottom": {"model": M + "block/pool_tile_slab"},
                                                                    "type=top": {"model": M + "block/pool_tile_slab_top"},
                                                                    "type=double": {"model": M + "block/pool_tile"}}})
    write(ASSETS / "models/item/pool_tile_slab.json", {"parent": M + "block/pool_tile_slab"})

    write(ASSETS / "models/block/scarecrow_head.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": M + "block/scarecrow_head_front", "side": M + "block/scarecrow_head_side", "top": M + "block/scarecrow_head_top"}})
    write(ASSETS / "blockstates/scarecrow_head.json", {"variants": {
        "facing=north": {"model": M + "block/scarecrow_head"}, "facing=east": {"model": M + "block/scarecrow_head", "y": 90},
        "facing=south": {"model": M + "block/scarecrow_head", "y": 180}, "facing=west": {"model": M + "block/scarecrow_head", "y": 270}}})
    write(ASSETS / "models/item/scarecrow_head.json", {"parent": M + "block/scarecrow_head"})
    write(ASSETS / "models/item/scarecrow_lantern.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": M + "item/scarecrow_lantern"}})


# =============================================================================== v0.10: the 3D lantern

def lantern_3d():
    """A 32x32 texture and an element model: iron cage, warm glowing panes, a stitched face inside, straw on top."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rng = random.Random(1001)
    iron, iron_dark = (60, 62, 70), (36, 37, 42)
    glow_hi, glow_lo = (255, 226, 150), (238, 150, 60)

    def pane(x0, y0, w, h, face=False):
        for y in range(h):
            for x in range(w):
                if x in (0, w - 1) or y in (0, h - 1):
                    c = iron
                elif (x == w // 2 and not face) or y == h // 2 and not face:
                    c = iron_dark                                         # the cage bars
                else:
                    t = abs(x - (w - 1) / 2) / (w / 2) * 0.6 + y / h * 0.4
                    c = mix(glow_hi, glow_lo, t)
                img.putpixel((x0 + x, y0 + y), rgba(c))
        if face:
            # a sackcloth face pressed up against the glass
            burlap = (182, 152, 98)
            for y in range(2, h - 1):
                for x in range(2, w - 2):
                    img.putpixel((x0 + x, y0 + y), rgba(jit(rng, burlap, 10)))
            for fx, fy in ((4, 4), (6, 4), (5, 5), (4, 6), (6, 6), (9, 4), (11, 4), (10, 5), (9, 6), (11, 6)):
                if fx < w - 2 and fy < h - 1:
                    img.putpixel((x0 + fx * (w - 4) // 12 + 1, y0 + fy * (h - 2) // 12 + 1), rgba((22, 16, 12)))
            for x in range(3, w - 3):
                img.putpixel((x0 + x, y0 + h - 4), rgba((50, 28, 18)))       # the stitched grin

    def solid(x0, y0, w, h, color, dark):
        for y in range(h):
            for x in range(w):
                img.putpixel((x0 + x, y0 + y), rgba(dark if (x + y) % 5 == 0 else color))

    pane(0, 0, 12, 16)                 # body side (6x8 units at 2px per unit)
    pane(12, 0, 12, 16, face=True)     # body front
    solid(24, 0, 8, 8, iron, iron_dark)        # body top/bottom and cap top
    solid(0, 16, 10, 4, iron, iron_dark)       # cap side
    solid(10, 16, 2, 8, (80, 82, 90), iron_dark)   # chain
    solid(12, 16, 10, 2, iron, iron_dark)      # base side
    for y in range(4):                          # straw
        for x in range(10):
            if rng.random() < 0.55 or y == 3:
                img.putpixel((x, 24 + y), rgba(jit(rng, (226, 196, 100), 18)))
    img.save(TEX / "item/scarecrow_lantern_3d.png")

    t = M + "item/scarecrow_lantern_3d"
    glow = {"block_light": 15, "sky_light": 15}

    def box(frm, to, faces, shade=True):
        e = {"from": frm, "to": to, "faces": faces}
        if not shade:
            e["shade"] = False
        return e

    def f(uv, emissive=False, cull=None):
        d = {"uv": uv, "texture": "#lantern"}
        if emissive:
            d["neoforge_data"] = glow
        return d

    side = [0, 0, 6, 8]
    elements = [
        box([5, 1, 5], [11, 9, 11], {"north": f([6, 0, 12, 8], True), "south": f(side, True), "east": f(side, True), "west": f(side, True),
                                     "up": f([12, 0, 16, 4]), "down": f([12, 0, 16, 4])}, shade=False),
        box([5.5, 0, 5.5], [10.5, 1, 10.5], {k: f([6, 8, 11, 9]) for k in ("north", "south", "east", "west")} | {"up": f([12, 0, 16, 4]), "down": f([12, 0, 16, 4])}),
        box([5.5, 9, 5.5], [10.5, 11, 10.5], {k: f([0, 8, 5, 10]) for k in ("north", "south", "east", "west")} | {"up": f([12, 0, 16, 4]), "down": f([12, 0, 16, 4])}),
        box([7.5, 11, 7.5], [8.5, 14, 8.5], {k: f([5, 8, 6, 12]) for k in ("north", "south", "east", "west", "up", "down")}),
        box([6.5, 13.5, 7.5], [9.5, 14.5, 8.5], {k: f([5, 8, 6, 9]) for k in ("north", "south", "east", "west", "up", "down")}),
        # straw sticking out from under the cap
        box([5, 9, 8], [11, 11.5, 8], {"north": f([0, 12, 5, 14]), "south": f([0, 12, 5, 14])}),
        box([8, 9, 5], [8, 11.5, 11], {"east": f([0, 12, 5, 14]), "west": f([0, 12, 5, 14])}),
    ]
    display = {
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        # hanging straight down from the fist by the chain, face forward
        "thirdperson_righthand": {"rotation": [-15, 0, 0], "translation": [0, -6.5, 0.6], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [-15, 0, 0], "translation": [0, -6.5, 0.6], "scale": [0.55, 0.55, 0.55]},
        "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "head": {"translation": [0, 10, 0]},
    }
    write(ASSETS / "models/item/scarecrow_lantern_3d.json", {"textures": {"lantern": t, "particle": t}, "render_type": "minecraft:cutout",
                                                             "elements": elements, "display": display})
    write(ASSETS / "models/item/scarecrow_lantern_flat.json", {"parent": "minecraft:item/generated",
                                                               "textures": {"layer0": M + "item/scarecrow_lantern"}})
    # a flat icon in inventories and item frames, a real 3D lantern everywhere else
    write(ASSETS / "models/item/scarecrow_lantern.json", {"loader": "neoforge:separate_transforms",
                                                          "base": {"parent": M + "item/scarecrow_lantern_3d"},
                                                          "perspectives": {"gui": {"parent": M + "item/scarecrow_lantern_flat"},
                                                                           "fixed": {"parent": M + "item/scarecrow_lantern_flat"}}})


# =============================================================================== v0.10: Level Fun, the farm scarecrow, the guide

PARTY = {0: ("red", (214, 44, 44)), 1: ("yellow", (246, 206, 48)), 2: ("blue", (58, 120, 222)), 3: ("green", (80, 196, 70))}


def v10_textures():
    rng = random.Random(1100)
    # --- balloons: glossy, with a knot and a white string
    for index, (name, color) in PARTY.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(10):
            for x in range(8):
                t = (abs(x - 3.5) / 4.0) * 0.5 + (y / 10) * 0.4
                c = mix(shade(color, 1.15), shade(color, 0.7), t)
                if x in (1, 2) and y in (1, 2, 3):
                    c = mix(c, (255, 255, 255), 0.55)                  # the shine
                img.putpixel((x, y), rgba(c))
        for y in range(8):
            for x in range(8, 16):
                d = math.hypot(x - 11.5, y - 3.5) / 5
                img.putpixel((x, y), rgba(mix(shade(color, 1.1), shade(color, 0.75), d)))
        for y in range(10, 12):
            for x in range(8, 10):
                img.putpixel((x, y), rgba(shade(color, 0.6)))
        for y in range(10, 16):
            img.putpixel((12, y), rgba((238, 238, 232)))
        img.save(TEX / f"block/balloon_{name}.png")

        # presents: wrapping paper with a gold ribbon
        gold = (240, 196, 60)

        def side(x, y, rng, color=color):
            if x in (7, 8):
                return shade(gold, 0.95 if y % 3 else 0.85)
            c = color if (x + y // 2) % 4 else shade(color, 0.82)             # a pattern on the paper
            return jit(rng, c, 5)
        block(f"present_{name}_side", side, 1110 + index)

        def top(x, y, rng, color=color):
            if x in (7, 8) or y in (7, 8):
                return shade(gold, 0.95)
            return jit(rng, color if (x // 2 + y // 2) % 2 else shade(color, 0.88), 5)
        block(f"present_{name}_top", top, 1120 + index)

    def bow(x, y, rng):
        loop = abs(abs(x - 7.5) - 4) + abs(y - 8) * 0.6
        if loop < 3.2 or (abs(x - 7.5) < 1.6 and abs(y - 8) < 1.6):
            return rgba(mix((250, 214, 80), (200, 150, 30), loop / 3.2))
        return (0, 0, 0, 0)
    block("present_bow", bow, 1130)

    colors = [(230, 50, 60), (250, 210, 60), (70, 140, 240), (90, 210, 90), (240, 110, 200), (255, 150, 40), (255, 255, 255)]

    def confetti(x, y, rng):
        if rng.random() < 0.28:
            return rgba(rng.choice(colors))
        return (0, 0, 0, 0)
    block("confetti", confetti, 1140)

    guide = sprite([
        "..KKKKKKKKKKK...",
        ".KGGGGGGGGGGGK..",
        ".KGGGGGGGGGGGK..",
        "WKGGGgggggGGGK..",
        ".KGGgLLLLLgGGK..",
        "WKGgLLllLLLgGK..",
        ".KGgLlLLlLLgGK..",
        "WKGgLLlLLlLgGK..",
        ".KGgLLLllLLgGK..",
        "WKGGgLLLLLgGGK..",
        ".KGGGgggggGGGK..",
        "WKGGGGGGGGGGGK..",
        ".KGGGGGGGGGGGK..",
        "WKGGGGGGGGGGGK..",
        ".KKKKKKKKKKKKK..",
        "................",
    ], {"K": (30, 50, 30), "G": (90, 170, 70), "g": (60, 130, 50), "L": (150, 255, 110), "l": (220, 255, 190), "W": (200, 200, 200)})
    guide.save(TEX / "item/field_guide.png")


def v10_models():
    for index, (name, color) in PARTY.items():
        tex = M + f"block/balloon_{name}"
        write(ASSETS / f"models/block/balloon_{name}.json", {"textures": {"balloon": tex, "particle": tex}, "render_type": "minecraft:cutout", "elements": [
            {"from": [4, 6, 4], "to": [12, 16, 12], "faces": {f: {"uv": [0, 0, 8, 10], "texture": "#balloon"} for f in ("north", "south", "east", "west")}
             | {"up": {"uv": [8, 0, 16, 8], "texture": "#balloon"}, "down": {"uv": [8, 0, 16, 8], "texture": "#balloon"}}},
            {"from": [7, 5, 7], "to": [9, 6, 9], "faces": {f: {"uv": [8, 10, 10, 12], "texture": "#balloon"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [7.9, 0, 7.9], "to": [8.1, 5, 8.1], "faces": {f: {"uv": [12, 10, 13, 16], "texture": "#balloon"} for f in ("north", "south", "east", "west")}},
        ]})
        side, top = M + f"block/present_{name}_side", M + f"block/present_{name}_top"
        write(ASSETS / f"models/block/present_{name}.json", {"textures": {"side": side, "top": top, "bow": M + "block/present_bow", "particle": side},
                                                            "render_type": "minecraft:cutout", "elements": [
            {"from": [2, 0, 2], "to": [14, 12, 14], "faces": {f: {"uv": [2, 4, 14, 16], "texture": "#side"} for f in ("north", "south", "east", "west")}
             | {"up": {"uv": [2, 2, 14, 14], "texture": "#top"}, "down": {"uv": [2, 2, 14, 14], "texture": "#top"}}},
            {"from": [3, 12, 8], "to": [13, 16, 8], "faces": {"north": {"uv": [3, 6, 13, 10], "texture": "#bow"}, "south": {"uv": [3, 6, 13, 10], "texture": "#bow"}}},
            {"from": [8, 12, 3], "to": [8, 16, 13], "faces": {"east": {"uv": [3, 6, 13, 10], "texture": "#bow"}, "west": {"uv": [3, 6, 13, 10], "texture": "#bow"}}},
        ]})
    write(ASSETS / "blockstates/balloon.json", {"variants": {f"color={i}": {"model": M + f"block/balloon_{n}"} for i, (n, _) in PARTY.items()}})
    write(ASSETS / "blockstates/present.json", {"variants": {f"color={i}": {"model": M + f"block/present_{n}"} for i, (n, _) in PARTY.items()}})
    write(ASSETS / "models/item/balloon.json", {"parent": M + "block/balloon_red"})
    write(ASSETS / "models/item/present.json", {"parent": M + "block/present_red"})
    write(ASSETS / "models/block/confetti.json", {"parent": "minecraft:block/carpet", "render_type": "minecraft:cutout", "textures": {"wool": M + "block/confetti"}})
    write(ASSETS / "blockstates/confetti.json", {"variants": {"": {"model": M + "block/confetti"}}})
    write(ASSETS / "models/item/confetti.json", {"parent": M + "block/confetti"})
    write(ASSETS / "models/item/field_guide.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/field_guide"}})
    for egg in ("farm_scarecrow_spawn_egg", "partygoer_spawn_egg"):
        write(ASSETS / f"models/item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})


def v10_entities():
    burlap, burlap_dark, straw = (186, 158, 104), (140, 116, 74), (224, 194, 96)
    n = Noise(1150, 1.2)

    def farm_face(x, y):
        if (x, y) in ((2, 3), (5, 3)):
            return (40, 30, 60)                                     # button eyes
        if (x, y) in ((1, 5), (6, 5)) or (y == 6 and 2 <= x <= 5):
            return (60, 30, 20)                                     # a stitched smile
        return None

    def farm_extra(name, fname, x, y, w, h, rng):
        if name == "head":
            if fname == "front":
                f = farm_face(x, y)
                if f is not None:
                    return rgba(f)
            return rgba(mix(burlap, burlap_dark, n(x + len(fname) * 8, y) * 0.7))
        if name == "hat":
            if fname == "top" or (fname != "bottom" and y < 2 and rng.random() < 0.8):
                return rgba(jit(rng, straw, 22))                    # straw hair
            return None
        if name in ("right_arm", "left_arm") and y >= 9:
            return rgba(jit(rng, straw, 20))
        if name in ("body", "right_arm", "left_arm") and (name != "body" or y < 7):
            red = (178, 44, 36) if (x // 2 + y // 2) % 2 else (122, 28, 24)
            if x % 4 == 0 or y % 4 == 0:
                red = (214, 190, 150)                               # plaid
            return rgba(red)
        if name == "body" and y >= 7 or name in ("right_leg", "left_leg") and y < 11:
            return rgba(jit(rng, (62, 88, 148), 6))                 # overalls
        return None

    skin(False, humanoid((200, 170, 110), None, (178, 44, 36), (62, 88, 148), (90, 60, 36), seed=1151, extra=farm_extra), 1151) \
        .save(TEX / "entity/farm_scarecrow.png")

    yellow = (244, 208, 52)

    def party_face(hostile):
        def f(x, y):
            if hostile:
                if (x, y) in ((1, 2), (2, 2), (5, 2), (6, 2)):
                    return (200, 20, 20)
                if 4 <= y <= 6 and 0 <= x <= 7 and not (y == 6 and x in (0, 7)):
                    if y in (4, 6) and x % 2 == 0:
                        return (245, 245, 235)                      # teeth
                    return (20, 8, 8)
                return None
            if (x, y) in ((2, 2), (5, 2)):
                return (16, 16, 16)
            if (x, y) in ((0, 4), (7, 4), (1, 5), (6, 5)) or (y == 6 and 2 <= x <= 5):
                return (16, 16, 16)                                  # a smile far too wide
            return None
        return f

    def party_extra(hostile):
        def e(name, fname, x, y, w, h, rng):
            if name in ("hat", "jacket", "right_sleeve", "left_sleeve", "right_pants", "left_pants"):
                return None
            if name == "head" and fname == "front":
                c = party_face(hostile)(x, y)
                if c is not None:
                    return rgba(c)
            base = shade(yellow, 0.85) if hostile else yellow
            if hostile and n(x * 3 + len(name), y * 3) > 0.7:
                base = shade(base, 0.8)
            return rgba(jit(rng, base, 6))
        return e

    for hostile, name in ((False, "partygoer"), (True, "partygoer_hostile")):
        skin(False, humanoid(yellow, None, yellow, yellow, yellow, seed=1160, extra=party_extra(hostile)), 1160).save(TEX / f"entity/{name}.png")


def main():
    for folder in ("entity", "block", "item"):
        (TEX / folder).mkdir(parents=True, exist_ok=True)
    block_textures()
    block_models()
    item_textures()
    entity_textures()
    v09_textures()
    v09_models()
    lantern_3d()
    v10_textures()
    v10_models()
    v10_entities()
    print("Backrooms level art written")


if __name__ == "__main__":
    main()
