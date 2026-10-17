"""Rick's gadgets: the Shrink Ray, the Freeze Ray, the Time Crystal and the Interdimensional Goggles.
3D block/item models (from the reference pictures), textures, the worn goggles' texture, effect
icons, recipes, loot, sounds, translations and advancements. Run from the project root:

    python tools/generate_gadgets.py
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import box_faces, mix, rgba, shade  # noqa: E402
from generate_cronenberg_data import ASSETS, DATA, IMPOSSIBLE, M, MC, advancement, merge_tag, write  # noqa: E402

TEX = ASSETS / "textures"
FACES = ("north", "south", "east", "west", "up", "down")


def tex(name, fn, size=16, seed=1, folder="block"):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            c = fn(x, y, rng)
            if c is not None:
                img.putpixel((x, y), c if len(c) == 4 else rgba(c))
    (TEX / folder).mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"{folder}/{name}.png")


def speckle(c, amount, rng):
    return shade(c, rng.uniform(1 - amount, 1 + amount))


def el(frm, to, texture, rot=None, glow=False):
    """A model element; every face uses the texture, with UVs kept inside the texture."""
    size = [to[i] - frm[i] for i in range(3)]
    dims = {"north": (size[0], size[1]), "south": (size[0], size[1]), "east": (size[2], size[1]), "west": (size[2], size[1]),
            "up": (size[0], size[2]), "down": (size[0], size[2])}
    faces = {f: {"texture": "#" + texture, "uv": [0, 0, round(min(16, max(0.5, w)), 3), round(min(16, max(0.5, h)), 3)]} for f, (w, h) in dims.items()}
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if rot:
        e["rotation"] = {"origin": rot[0], "axis": rot[1], "angle": rot[2]}
    if glow:
        e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return e


def octagon(frm, to, texture, glow=False):
    cx, cy, cz = (frm[0] + to[0]) / 2, (frm[1] + to[1]) / 2, (frm[2] + to[2]) / 2
    return [el(frm, to, texture, glow=glow), el(frm, to, texture, ([cx, cy, cz], "y", 45), glow)]


def model(name, data):
    write(ASSETS / f"models/{name}.json", data)


def b(name):
    return M + "block/" + name


# =============================================================================== the Shrink Ray

def shrink_ray():
    tex("shrink_ray_body", lambda x, y, r: speckle((46, 110, 94), 0.05, r), seed=1)
    tex("shrink_ray_body2", lambda x, y, r: speckle((78, 154, 112), 0.05, r) if x % 15 and y % 15 else (56, 120, 86), seed=2)
    tex("shrink_ray_arm", lambda x, y, r: mix((168, 222, 210), (120, 180, 170), abs(x - 7.5) / 8), seed=3)
    tex("shrink_ray_dark", lambda x, y, r: speckle((40, 48, 50), 0.06, r), seed=4)
    tex("shrink_ray_core", lambda x, y, r: mix((200, 255, 120), (120, 210, 60), y / 15) if (x + y) % 7 else (230, 255, 190), seed=5)
    tex("shrink_ray_core_grow", lambda x, y, r: mix((255, 200, 110), (240, 110, 40), y / 15) if (x + y) % 7 else (255, 235, 190), seed=6)
    tex("shrink_ray_light", lambda x, y, r: (230, 255, 220) if 5 <= x <= 10 and 5 <= y <= 10 else (110, 255, 90), seed=7)
    tex("shrink_ray_orange", lambda x, y, r: speckle((240, 110, 60), 0.05, r), seed=8)

    els = []
    # the wheeled base, its dark top, and the ring of green lights
    els += octagon([1, 1, 1], [15, 4, 15], "body")
    els += octagon([3, 4, 3], [13, 4.5, 13], "dark")
    els.append(el([7, 4.5, 7], [9, 4.7, 9], "arm"))
    for k in range(6):
        a = k * math.pi / 3 + math.pi / 6
        x, z = 8 + 5.6 * math.cos(a), 8 + 5.6 * math.sin(a)
        els.append(el([x - 0.6, 4, z - 0.6], [x + 0.6, 5.2, z + 0.6], "light", glow=True))
    for x, z in ((1.6, 1.6), (12.4, 1.6), (1.6, 12.4), (12.4, 12.4)):
        els.append(el([x, 0, z], [x + 2, 1.4, z + 2], "dark"))
        els.append(el([x + 0.6, 1, z + 0.6], [x + 1.4, 2, z + 1.4], "body"))
    # the arm: up and back to the elbow, then up and forward over the block in front
    els.append(el([7.25, 4, 9], [8.75, 20, 10.5], "arm", ([8, 4, 9.75], "x", 22.5)))
    els.append(el([8.8, 4.5, 9.6], [9.3, 19, 10.1], "dark", ([9, 4, 9.85], "x", 22.5)))         # a cable along it
    els.append(el([6.75, 17.5, 14.6], [9.25, 20.1, 17.2], "body"))
    els.append(el([7.25, 18.05, -2.6], [8.75, 19.55, 15.9], "arm", ([8, 18.8, 15.9], "x", -22.5)))
    els.append(el([6.7, 19.6, -2.6], [7.2, 20.1, 15.9], "dark", ([7, 18.8, 15.9], "x", -22.5)))
    els.append(el([7, 24.5, -2.2], [9, 27, 0.6], "body"))
    # the head: a cap, a glowing core in a dark ring, and the cone with rings round it
    els += octagon([2, 25, -12], [14, 29, 0], "body")
    els.append(el([6.5, 29, -7.5], [9.5, 30.5, -4.5], "arm"))
    els.append(el([7.25, 30.5, -6.75], [8.75, 31.6, -5.25], "core", glow=True))
    els += octagon([4, 21, -10], [12, 22, -2], "dark")
    els += octagon([4.5, 22, -9.5], [11.5, 25, -2.5], "core", glow=True)
    els.append(el([5.5, 19, -8.5], [10.5, 21, -3.5], "core", glow=True))
    els.append(el([5, 18.6, -9], [11, 19.1, -3], "body"))
    els.append(el([6.5, 17, -7.5], [9.5, 19, -4.5], "core", glow=True))
    els.append(el([6, 16.6, -8], [10, 17.1, -4], "body"))
    els.append(el([7.25, 15.5, -6.75], [8.75, 17, -5.25], "core", glow=True))
    els.append(el([7.6, 14.2, -6.4], [8.4, 15.5, -5.6], "light", glow=True))
    # the control box with its three knobs
    els.append(el([12.5, 0, 12.5], [16, 8, 16], "body2"))
    for x in (13.3, 14.25, 15.2):
        els.append(el([x - 0.15, 8, 14.1], [x + 0.15, 10, 14.4], "arm"))
        els.append(el([x - 0.5, 10, 13.75], [x + 0.5, 11, 14.75], "orange"))

    textures = {"particle": b("shrink_ray_body"), "body": b("shrink_ray_body"), "body2": b("shrink_ray_body2"), "arm": b("shrink_ray_arm"),
                "dark": b("shrink_ray_dark"), "core": b("shrink_ray_core"), "light": b("shrink_ray_light"), "orange": b("shrink_ray_orange")}
    display = {"gui": {"rotation": [30, 160, 0], "translation": [0, -3.5, 0], "scale": [0.42, 0.42, 0.42]},
               "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
               "fixed": {"rotation": [0, 180, 0], "translation": [0, -3, 0], "scale": [0.45, 0.45, 0.45]},
               "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
               "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
               "firstperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}
    # the ray itself is drawn by ShrinkRayRenderer (tools/generate_shrink_ray.py); the block model is only for its particles
    model("block/shrink_ray", {"textures": {"particle": b("shrink_ray_body")}})
    write(ASSETS / "blockstates/shrink_ray.json", {"variants": {"": {"model": b("shrink_ray")}}})
    model("item/shrink_ray", {"parent": "minecraft:builtin/entity", "gui_light": "front", "display": {
        "gui": {"rotation": [25, 155, 0], "translation": [0, -1.5, 0], "scale": [1.0, 1.0, 1.0]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -2, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 1, 1], "scale": [0.6, 0.6, 0.6]},
        "thirdperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 1, 1], "scale": [0.6, 0.6, 0.6]},
        "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]}}})

    def lens(x, y, r):
        d = math.hypot(x - 7.5, y - 7.5)
        if d > 6.8:
            return None
        if d > 5.6:
            return (90, 70, 40)                                           # the brass rim
        if (x - 5) ** 2 + (y - 5) ** 2 < 3:
            return (255, 255, 240, 230)
        return (255, 170, 80, 170)
    tex("growth_lens", lens, seed=9, folder="item")
    model("item/growth_lens", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/growth_lens"}})


# =============================================================================== the Freeze Ray

def freeze_ray():
    tex("freeze_ray_body", lambda x, y, r: mix((40, 42, 48), (20, 20, 24), abs(x - 7.5) / 8) if y % 6 else (70, 72, 80), seed=11, folder="item")
    tex("freeze_ray_grip", lambda x, y, r: (28, 28, 32) if (x + y) % 3 else (48, 48, 54), seed=12, folder="item")
    tex("freeze_ray_band", lambda x, y, r: speckle((150, 156, 164), 0.05, r), seed=13, folder="item")
    tex("freeze_ray_claw", lambda x, y, r: mix((60, 62, 70), (24, 24, 28), y / 15), seed=14, folder="item")
    tex("freeze_ray_lens", lambda x, y, r: (240, 252, 255) if 3 <= x <= 6 and 3 <= y <= 6 else (150, 220, 250), seed=15, folder="item")
    i = lambda n: M + "item/" + n
    els = [el([7, 0, 7], [9, 5.5, 9], "grip"), el([6.7, 5.5, 6.7], [9.3, 6.5, 9.3], "band"),
           el([6.5, 6.5, 6.5], [9.5, 12, 9.5], "body"), el([9.5, 8, 7.1], [9.9, 10.6, 8.9], "lens", glow=True),
           el([6.8, 12, 6.8], [9.2, 13, 9.2], "band"),
           el([6.4, 13, 7.5], [7.2, 16, 8.5], "claw", ([6.8, 13, 8], "z", 22.5)),
           el([8.8, 13, 7.5], [9.6, 16, 8.5], "claw", ([9.2, 13, 8], "z", -22.5)),
           el([7.5, 13, 6.4], [8.5, 15.6, 7.2], "claw", ([8, 13, 6.8], "x", -22.5)),
           el([7.5, 13, 8.8], [8.5, 15.6, 9.6], "claw", ([8, 13, 9.2], "x", 22.5)),
           el([7.4, 13.3, 7.4], [8.6, 14.5, 8.6], "lens", glow=True)]
    model("item/freeze_ray", {"textures": {"particle": i("freeze_ray_body"), "body": i("freeze_ray_body"), "grip": i("freeze_ray_grip"),
                                           "band": i("freeze_ray_band"), "claw": i("freeze_ray_claw"), "lens": i("freeze_ray_lens")},
                               "elements": els, "gui_light": "front",
                               "display": {"gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
                                           "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                                           "fixed": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [1, 1, 1]},
                                           "thirdperson_righthand": {"rotation": [-25, 0, 0], "translation": [0, 3, 1], "scale": [0.85, 0.85, 0.85]},
                                           "thirdperson_lefthand": {"rotation": [-25, 0, 0], "translation": [0, 3, 1], "scale": [0.85, 0.85, 0.85]},
                                           "firstperson_righthand": {"rotation": [-35, 0, 8], "translation": [1.5, 2.2, -1], "scale": [0.5, 0.5, 0.5]},
                                           "firstperson_lefthand": {"rotation": [-35, 0, -8], "translation": [1.5, 2.2, -1], "scale": [0.5, 0.5, 0.5]}}})


# =============================================================================== the Time Crystal

def time_crystal():
    def crystal(x, y, r):
        facet = (x + 2 * y) % 9 in (0, 1)
        shine = abs((x - y) - 2) <= 1
        c = (255, 235, 252) if shine else (255, 150, 230) if facet else (226, 70, 180)
        return (*c, 215 if shine else 185)
    tex("time_crystal", crystal, seed=21)
    tex("time_crystal_core", lambda x, y, r: (255, 245, 255), seed=22)
    tex("time_crystal_base", lambda x, y, r: speckle((34, 30, 46), 0.06, r), seed=23)
    tex("time_crystal_band", lambda x, y, r: speckle((96, 58, 44), 0.06, r), seed=24)
    els = []
    els += octagon([3, 0, 3], [13, 1.5, 13], "base")
    for x0, z0, x1, z1 in ((2.4, 6.5, 3.4, 9.5), (12.6, 6.5, 13.6, 9.5), (6.5, 2.4, 9.5, 3.4), (6.5, 12.6, 9.5, 13.6)):
        els.append(el([x0, 0, z0], [x1, 1, z1], "base"))                    # the ring's loops underneath
    els += octagon([4.2, 1.5, 4.2], [11.8, 2.5, 11.8], "band")
    els.append(el([7, 3.5, 7], [9, 10.5, 9], "core", glow=True))
    els += octagon([5.5, 2.5, 5.5], [10.5, 11, 10.5], "crystal", glow=True)
    els += octagon([6.25, 11, 6.25], [9.75, 12.5, 9.75], "crystal", glow=True)
    els += octagon([7, 12.5, 7], [9, 13.8, 9], "crystal", glow=True)
    els.append(el([7.6, 13.8, 7.6], [8.4, 14.8, 8.4], "crystal", glow=True))
    els.append(el([9.6, 2.5, 6.9], [11.2, 8.5, 8.5], "crystal", ([10.4, 2.5, 7.7], "z", -22.5), glow=True))
    els.append(el([4.8, 2.5, 7.6], [6.2, 7, 9], "crystal", ([5.5, 2.5, 8.3], "z", 22.5), glow=True))
    textures = {"particle": b("time_crystal"), "crystal": b("time_crystal"), "core": b("time_crystal_core"), "base": b("time_crystal_base"),
                "band": b("time_crystal_band")}
    model("block/time_crystal", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": textures, "elements": els,
                                 "display": {"gui": {"rotation": [25, 30, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
                                             "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                                             "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
                                             "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
                                             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                                             "firstperson_lefthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}}})
    write(ASSETS / "blockstates/time_crystal.json", {"variants": {"": {"model": b("time_crystal")}}})
    model("item/time_crystal", {"parent": b("time_crystal")})


# =============================================================================== the Interdimensional Goggles

VISOR, VISOR_DARK, CAP, STRAP, BUTTON = (22, 84, 66), (12, 52, 40), (126, 146, 160), (34, 40, 54), (255, 110, 40)


def goggles():
    i = lambda n: M + "item/" + n
    tex("goggles_visor", lambda x, y, r: mix(VISOR, VISOR_DARK, y / 15) if not (11 <= x <= 13 and 2 <= y <= 4) else (150, 230, 220), seed=31, folder="item")
    tex("goggles_cap", lambda x, y, r: speckle(CAP, 0.04, r), seed=32, folder="item")
    tex("goggles_strap", lambda x, y, r: speckle(STRAP, 0.06, r) if y % 5 else (60, 66, 80), seed=33, folder="item")
    tex("goggles_button", lambda x, y, r: BUTTON, seed=34, folder="item")
    tex("goggles_buckle", lambda x, y, r: (90, 96, 100) if x in (0, 15) or y in (0, 15) else (60, 66, 70), seed=35, folder="item")
    els = [el([2, 5, 4], [14, 10, 8], "visor"), el([0.8, 5.5, 4.5], [2, 9.5, 7.5], "cap"), el([14, 5.5, 4.5], [15.2, 9.5, 7.5], "cap"),
           el([15.2, 7, 5.5], [15.7, 8, 6.5], "button"),
           el([1.5, 6.5, 13], [14.5, 8.5, 13.6], "strap"), el([1.2, 6.5, 7.5], [1.8, 8.5, 13.6], "strap"),
           el([14.2, 6.5, 7.5], [14.8, 8.5, 13.6], "strap"), el([6, 6.2, 13.4], [8.5, 8.8, 13.9], "buckle")]
    model("item/interdimensional_goggles", {
        "textures": {"particle": i("goggles_visor"), "visor": i("goggles_visor"), "cap": i("goggles_cap"), "strap": i("goggles_strap"),
                     "button": i("goggles_button"), "buckle": i("goggles_buckle")},
        "elements": els, "gui_light": "front",
        "display": {"gui": {"rotation": [20, 160, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
                    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
                    "thirdperson_righthand": {"rotation": [10, 180, 0], "translation": [0, 2, 2], "scale": [0.6, 0.6, 0.6]},
                    "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
                    "firstperson_lefthand": {"rotation": [0, 200, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]}}})

    # the worn model's texture (layout in GogglesModel)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    rng = random.Random(36)

    def paint(u, v, w, h, d, fn):
        for name, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    img.putpixel((x, y), rgba(fn(name, x - fx, y - fy, fw, fh)))
    paint(0, 0, 9, 4, 2, lambda f, x, y, w, h: (150, 230, 220) if f == "front" and x in (6, 7) and y == 0 else
          mix(VISOR, VISOR_DARK, y / max(1, h - 1)) if f in ("front", "back") else VISOR_DARK)
    paint(0, 8, 8, 2, 8, lambda f, x, y, w, h: speckle(STRAP, 0.05, rng))
    paint(24, 0, 2, 3, 3, lambda f, x, y, w, h: speckle(CAP, 0.04, rng))
    paint(36, 0, 1, 1, 1, lambda f, x, y, w, h: BUTTON)
    (TEX / "models/armor").mkdir(parents=True, exist_ok=True)
    img.save(TEX / "models/armor/interdimensional_goggles.png")


# =============================================================================== effects, data, words

def effect_icons():
    def icon(name, fn):
        tex(name, fn, size=18, folder="mob_effect")
    icon("shrunk", lambda x, y, r: (110, 230, 80) if (abs(x - 9) <= 1 and 4 <= y <= 13) or (y == 13 - abs(x - 9) and 3 <= x <= 15 and y >= 9) else None)
    icon("enlarged", lambda x, y, r: (255, 140, 50) if (abs(x - 9) <= 1 and 4 <= y <= 13) or (y == 4 + abs(x - 9) and y <= 8) else None)
    icon("frozen", lambda x, y, r: (170, 225, 250) if x == 9 or y == 9 or abs(x - y) == 0 or x + y == 18 else None)


def ing(n):
    return {"tag": n[1:]} if n.startswith("#") else {"item": n if ":" in n else M + n}


def shaped(name, pattern, key, count=1, category="misc"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                                         "key": {k: ing(v) for k, v in key.items()}, "result": {"id": M + name, "count": count}})


def data():
    shaped("shrink_ray", [" G ", "ISI", "RBR"], {"G": "minecraft:lime_stained_glass", "I": "minecraft:iron_ingot", "S": "minecraft:slime_ball",
                                                 "R": "minecraft:redstone", "B": "minecraft:iron_block"})
    shaped("growth_lens", [" D ", "BGB", " B "], {"D": "minecraft:diamond", "B": "minecraft:bone_meal", "G": "minecraft:glass_pane"})
    shaped("freeze_ray", ["  P", "IG ", "BI "], {"P": "minecraft:blue_ice", "I": "minecraft:iron_ingot", "G": "minecraft:glass",
                                                 "B": "minecraft:black_dye"}, category="equipment")
    shaped("time_crystal", [" A ", "ACA", "ORO"], {"A": "minecraft:amethyst_shard", "C": "minecraft:clock", "O": "minecraft:obsidian",
                                                   "R": "minecraft:redstone_block"})
    shaped("interdimensional_goggles", ["GIG", "ITI", " B "], {"G": "minecraft:glass", "I": "minecraft:iron_ingot", "T": "time_crystal",
                                                               "B": "microverse_battery"}, category="equipment")
    for name in ("shrink_ray", "time_crystal"):
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": M + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + "shrink_ray", M + "time_crystal"])
    merge_tag(MC / "tags/item/head_armor.json", [M + "interdimensional_goggles"])
    advancement("honey_i_shrunk", "shrink_ray", "microverse", IMPOSSIBLE)
    advancement("time_crystal", "time_crystal", "microverse", {"has": {"trigger": "minecraft:inventory_changed",
                                                                       "conditions": {"items": [{"items": M + "time_crystal"}]}}})
    advancement("interdimensional_goggles", "interdimensional_goggles", "time_crystal", {"has": {"trigger": "minecraft:inventory_changed",
                                                                                                 "conditions": {"items": [{"items": M + "interdimensional_goggles"}]}}}, "goal")
    advancement("freeze_ray", "freeze_ray", "microverse", {"has": {"trigger": "minecraft:inventory_changed",
                                                                   "conditions": {"items": [{"items": M + "freeze_ray"}]}}})

    sounds_path = ASSETS / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    ev = lambda name, pitch=1.0, volume=1.0: {"name": name, "type": "event", "pitch": pitch, "volume": volume}
    for key, entries in {"shrink_ray.fire": [ev("minecraft:block.beacon.power_select", 1.6), ev("minecraft:block.beacon.activate", 1.8, 0.8)],
                         "freeze_ray.hit": [ev("minecraft:block.glass.break", 1.4, 0.8), ev("minecraft:block.powder_snow.place", 0.8)],
                         "time_crystal.hum": [ev("minecraft:block.amethyst_block.resonate", 1.4, 0.6), ev("minecraft:block.amethyst_block.chime", 1.2, 0.6)]}.items():
        sounds[key] = {"subtitle": f"subtitles.rickrooms.{key}", "sounds": entries}
    write(sounds_path, sounds)

    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang.update({
        "block.rickrooms.shrink_ray": "Shrink Ray",
        "item.rickrooms.growth_lens": "Growth Lens",
        "tooltip.rickrooms.growth_lens": "Fit it to a Shrink Ray to make things bigger too",
        "item.rickrooms.freeze_ray": "Freeze Ray",
        "tooltip.rickrooms.freeze_ray": "Don't shoot it. Hit someone with it",
        "block.rickrooms.time_crystal": "Time Crystal",
        "item.rickrooms.interdimensional_goggles": "Interdimensional Goggles",
        "tooltip.rickrooms.interdimensional_goggles": "Wear them: the rock disappears, the ores don't. Night vision included",
        "tooltip.rickrooms.interdimensional_goggles.toggle": "Press %s to switch the x-ray on or off",
        "key.rickrooms.goggles_xray": "Goggles: X-Ray On/Off",
        "message.rickrooms.goggles.xray_on": "X-ray on",
        "message.rickrooms.goggles.xray_off": "X-ray off (night vision only)",
        "effect.rickrooms.shrunk": "Shrunk",
        "effect.rickrooms.enlarged": "Enlarged",
        "effect.rickrooms.frozen": "Frozen",
        "message.rickrooms.shrink_ray.needs_lens": "It only shrinks. Fit a Growth Lens to make it grow things too",
        "message.rickrooms.shrink_ray.lens_fitted": "Growth Lens fitted: it makes things bigger now (sneak-use to switch)",
        "message.rickrooms.shrink_ray.grow": "Shrink Ray: GROW",
        "message.rickrooms.shrink_ray.shrink": "Shrink Ray: SHRINK",
        "subtitles.rickrooms.shrink_ray.fire": "Shrink Ray fires",
        "subtitles.rickrooms.freeze_ray.hit": "Something freezes",
        "subtitles.rickrooms.time_crystal.hum": "Time Crystal hums",
        "advancements.rickrooms.honey_i_shrunk.title": "Honey, I Shrunk Myself",
        "advancements.rickrooms.honey_i_shrunk.description": "Stand under a Shrink Ray when it fires",
        "advancements.rickrooms.time_crystal.title": "Time Is Relative",
        "advancements.rickrooms.time_crystal.description": "Make a Time Crystal",
        "advancements.rickrooms.interdimensional_goggles.title": "I Can See Forever",
        "advancements.rickrooms.interdimensional_goggles.description": "Make a pair of Interdimensional Goggles",
        "advancements.rickrooms.freeze_ray.title": "Chill Out",
        "advancements.rickrooms.freeze_ray.description": "Make a Freeze Ray",
        "guide.rickrooms.page.23": "MY GADGETS\n\nShrink Ray: stand under it, press it. Growth Lens: the other way.\n\nFreeze Ray: hit things.\n\n"
                                      "Time Crystal: everything near it runs faster.\n\nGoggles: ores, through rock.",
    })
    write(path, lang)


def main():
    shrink_ray()
    freeze_ray()
    time_crystal()
    goggles()
    effect_icons()
    data()
    print("Gadgets written")


if __name__ == "__main__":
    main()
