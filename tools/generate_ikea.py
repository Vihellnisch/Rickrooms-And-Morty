"""Everything for the Infinite IKEA (SCP-3008): the staff model and its textures, the store's
blocks and items (textures, models, blockstates), and the data (the dimension, loot, recipes,
advancements, the Overworld store, sounds and translations). Run from the project root:

    python tools/generate_ikea.py

Also writes src/main/java/com/rickandmorty/client/model/StaffLayers.java, so the geometry and the
texture layout never disagree. Edit the shapes here, not there.
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import Noise, box_faces, fmt, mix, pack, rgba, shade, uv_size  # noqa: E402
from generate_cronenberg_data import ASSETS, DATA, IMPOSSIBLE, M, MC, advancement, merge_tag, write  # noqa: E402

PROJECT = Path(__file__).resolve().parent.parent
TEX = ASSETS / "textures"
JAVA = PROJECT / "src/main/java/com/rickandmorty/client/model/StaffLayers.java"

YELLOW = (238, 192, 34)
STRIPE = (52, 46, 24)
PANTS = (28, 30, 40)
SHOE = (18, 18, 20)
SKINS = [(204, 192, 158), (222, 212, 182), (210, 198, 160)]

# =============================================================================== the staff model
# Parts: (name, parent, pivot, rotation, [boxes]); a box is (x, y, z, w, h, d, uv_key).
# Model space: 16 units to a block, y down, the ground at y = 24. About 2.9 blocks tall.

staff = [
    ("right_leg", None, (-2.2, 4, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 18, 3, "leg"), (-1.5, 18, -3, 3, 2, 4, "shoe")]),
    ("left_leg", None, (2.2, 4, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 18, 3, "leg"), (-1.5, 18, -3, 3, 2, 4, "shoe")]),
    ("body", None, (0, 4, 0), (0, 0, 0), [(-4, -14, -2, 8, 14, 4, "torso"), (-2.5, -15, -2.5, 5, 2, 5, "collar")]),
    ("neck", "body", (0, -15, 0), (0, 0, 0), [(-1, -3, -1, 2, 3, 2, "neck")]),
    # the three heads (one shown, by variant): a lumpy one, a smooth egg, and a narrow one bristling with spines
    ("head_lumpy", "neck", (0, -3, 0), (0, 0, 0), [(-4, -10, -4, 8, 10, 8, "lump_head"), (-5.5, -8, -2, 2, 5, 4, "lump_a"),
                                                   (3, -11, -3, 3, 4, 5, "lump_b"), (-2, -12, -2, 5, 2, 4, "lump_c")]),
    ("head_smooth", "neck", (0, -3, 0), (0, 0, 0), [(-3, -9, -3, 6, 9, 6, "egg"), (-2.5, -11, -2.5, 5, 2, 5, "egg_top")]),
    ("head_spiky", "neck", (0, -3, 0), (0, 0, 0), [(-2.5, -11, -3, 5, 11, 6, "spike_head")]),
]
for i, (pivot, rot) in enumerate((((-2, -8, 0), (0, 0, 0.9)), ((2, -9, 0), (0, 0, -0.8)), ((0, -11, 0), (0.2, 0, 0.15)),
                                  ((-1.5, -4, -1), (-0.3, 0, 0.9)), ((1.5, -5, 1), (0.4, 0, -0.9)), ((0, -7, 3), (1.0, 0, 0)))):
    staff.append((f"spike{i}", "head_spiky", pivot, rot, [(-0.5, -6, -0.5, 1, 6, 1, "spike")]))
for side, s in (("right", -1), ("left", 1)):
    staff += [
        (f"{side}_arm", "body", (s * 5.5, -13, 0), (0, 0, 0), [(-1.5, -1, -1.5, 3, 11, 3, "upper_arm")]),
        (f"{side}_arm_spikes", f"{side}_arm", (0, 0, 0), (0, 0, 0), [(-1 + s * 2.5, 3, -0.5, 2, 1, 1, "arm_spike"),
                                                                     (-1 + s * 2.5, 7, -0.5, 2, 1, 1, "arm_spike")]),
        (f"{side}_forearm", f"{side}_arm", (0, 10, 0), (0, 0, 0), [(-1, 0, -1, 2, 10, 2, "forearm")]),
        (f"{side}_forearm_spikes", f"{side}_forearm", (0, 0, 0), (0, 0, 0), [(-1 + s * 2, 2, -0.5, 2, 1, 1, "arm_spike"),
                                                                             (-0.5, 5, 1, 1, 1, 2, "arm_spike2")]),
        (f"{side}_hand", f"{side}_forearm", (0, 10, 0), (0, 0, 0), [(-1.5, 0, -1, 3, 3, 2, "palm")]),
        (f"{side}_thumb", f"{side}_hand", (-s * 1.2, 1, -1), (-0.4, 0, 0), [(-0.5, 0, -0.5, 1, 4, 1, "thumb")]),
    ]
    for f in range(4):
        staff.append((f"{side}_finger{f}", f"{side}_hand", (-1.2 + f * 0.8, 3, 0), (0, 0, 0), [(-0.5, 0, -0.5, 1, 6, 1, "finger")]))
STAFF = {"size": (128, 64), "parts": staff}


def layout(model):
    sizes = {}
    for _, _, _, _, boxes in model["parts"]:
        for *_, w, h, d, key in boxes:
            size = (int(w), int(h), int(d))
            if key in sizes and sizes[key] != size:
                raise RuntimeError(f"uv key {key} used with two sizes")
            sizes[key] = size
    return sizes, pack([(k, *uv_size(*s)) for k, s in sizes.items()], *model["size"])


def staff_textures():
    sizes, uvs = layout(STAFF)
    for variant, skin in enumerate(SKINS):
        img = Image.new("RGBA", STAFF["size"], (0, 0, 0, 0))
        rng = random.Random(77 + variant)
        n = Noise(300 + variant, 3)

        def flesh(x, y, dark=0.0):
            c = mix(skin, shade(skin, 0.78), n(x, y) * 0.7 + dark)
            return shade(c, rng.uniform(0.95, 1.04))

        def shirt(x, y):
            c = STRIPE if x % 3 == 0 else mix(YELLOW, shade(YELLOW, 0.85), n(x, y) * 0.6)
            return c

        for key, (w, h, d) in sizes.items():
            u, v = uvs[key]
            faces = box_faces(u, v, w, h, d)
            for fname, (fx, fy, fw, fh) in faces.items():
                for y in range(fy, fy + fh):
                    for x in range(fx, fx + fw):
                        row = y - fy
                        if key == "leg":
                            c = mix(PANTS, shade(PANTS, 0.7), n(x, y))
                        elif key == "shoe":
                            c = SHOE if rng.random() > 0.1 else (40, 40, 44)
                        elif key == "collar":
                            c = (20, 20, 22)
                        elif key == "torso":
                            c = shirt(x, y)
                            if fname in ("front", "back", "left", "right") and row >= fh - 2:
                                c = (16, 16, 18) if row == fh - 1 else (26, 28, 36)          # the belt, and the top of the trousers
                        elif key == "upper_arm":
                            c = shirt(x, y) if fname == "top" or (fname not in ("bottom",) and row < 4) else flesh(x, y)
                            if fname not in ("top", "bottom") and row == 4:
                                c = shade(skin, 0.7)                                         # the shadow under the sleeve
                        elif key in ("spike", "arm_spike", "arm_spike2"):
                            c = mix(shade(skin, 1.08), (240, 232, 210), 0.4)
                        else:
                            c = flesh(x, y, 0.1 if key in ("finger", "thumb") else 0.0)
                        img.putpixel((x, y), rgba(c))
            # no face. Only the faintest dents where one should be.
            if key in ("lump_head", "egg", "spike_head"):
                fx, fy, fw, fh = faces["front"]
                for dx in (fw // 4, fw - 1 - fw // 4):
                    for dy in range(fh // 3, fh // 3 + 2):
                        x, y = fx + dx, fy + dy
                        img.putpixel((x, y), rgba(shade(img.getpixel((x, y))[:3], 0.88)))
                if key == "lump_head":                                  # the lumps of the lumpy one
                    for _ in range(40):
                        x, y = rng.randrange(u, u + 2 * (w + d)), rng.randrange(v, v + d + h)
                        img.putpixel((x, y), rgba(shade(skin, rng.uniform(0.7, 0.85))))
            if key == "torso":                                          # the name badge and the open collar
                fx, fy, fw, fh = faces["front"]
                for x in range(fx + 4, fx + 7):
                    img.putpixel((x, fy + 3), rgba((40, 80, 170)))
                    img.putpixel((x, fy + 4), rgba((250, 222, 60) if x < fx + 6 else (40, 80, 170)))
                for dy in range(2):
                    img.putpixel((fx + 3 + dy, fy + dy), rgba((20, 20, 22)))
                    img.putpixel((fx + 4 - dy, fy + dy), rgba((20, 20, 22)))
        img.save(TEX / f"entity/staff_{variant}.png")
    return uvs


def java_layer(model, uvs):
    lines = ["    public static LayerDefinition staff() {",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    var = {None: "root"}
    for pname, parent, pivot, rot, boxes in model["parts"]:
        cubes = "CubeListBuilder.create()"
        for x, y, z, w, h, d, key in boxes:
            u, v = uvs[key]
            cubes += f".texOffs({u}, {v}).addBox({fmt(x)}, {fmt(y)}, {fmt(z)}, {fmt(w)}, {fmt(h)}, {fmt(d)})"
        pose = f"PartPose.offsetAndRotation({', '.join(fmt(p) for p in pivot)}, {', '.join(fmt(r) for r in rot)})"
        var[pname] = "p_" + pname
        lines.append(f"        PartDefinition {var[pname]} = {var[parent]}.addOrReplaceChild(\"{pname}\", {cubes}, {pose});")
    w, h = model["size"]
    lines.append(f"        return LayerDefinition.create(mesh, {w}, {h});")
    lines.append("    }")
    body = "\n".join(lines)
    JAVA.write_text(f"""package com.rickandmorty.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry of the Infinite IKEA's staff. GENERATED by tools/generate_ikea.py (the same run paints
 * the matching textures), so edit the shapes there, not here.
 */
@SuppressWarnings("unused")
public final class StaffLayers {{
    private StaffLayers() {{
    }}

{body}
}}
""", encoding="utf-8")


# =============================================================================== block textures

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


FONT = {"E": ["###", "#..", "##.", "#..", "###"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
        "I": ["###", ".#.", ".#.", ".#.", "###"], "T": ["###", ".#.", ".#.", ".#.", ".#."]}


def block_textures():
    tex("store_floor", lambda x, y, r: speckle((178, 180, 178), 0.04, r) if x and y else (150, 152, 150), seed=1)
    tex("store_walkway", lambda x, y, r: speckle((236, 236, 232), 0.02, r), seed=2)
    tex("store_partition", lambda x, y, r: speckle((240, 240, 238), 0.015, r) if y else (214, 214, 212), seed=3)

    # the hanging EXIT sign: yellow on blue, the arrow to the right (front) or left (back)
    sign_yellow = rgba((250, 210, 40))
    for name, right in (("exit_sign_front", True), ("exit_sign_back", False)):
        img = Image.new("RGBA", (32, 32), rgba((24, 76, 170)))
        for y in range(32):
            for x in range(32):
                if y < 6 or y > 25 or x in (0, 31) or y in (6, 25):
                    img.putpixel((x, y), rgba((16, 52, 120)))
        x0 = 3 if right else 14
        for i, ch in enumerate("EXIT"):
            for row in range(5):
                for col in range(3):
                    if FONT[ch][row][col] == "#":
                        for sy in range(2):
                            img.putpixel((x0 + i * 4 + col, 11 + row * 2 + sy), sign_yellow)
        ax = 21 if right else 2
        for dx in range(8):                                                # the shaft
            for dy in (-1, 0, 1):
                img.putpixel((ax + dx, 15 + dy), sign_yellow)
        for k in range(5):                                                 # the head
            tip = ax + 8 - k if right else ax - 1 + k
            for dy in range(-k, k + 1):
                img.putpixel((tip, 15 + dy), sign_yellow)
        img.save(TEX / f"block/{name}.png")
    tex("exit_sign_edge", lambda x, y, r: (20, 64, 150), seed=4)

    tex("store_light", lambda x, y, r: (250, 240, 190) if 4 <= x <= 11 and 4 <= y <= 11 else (232, 196, 40) if x % 15 and y % 15 else (90, 90, 90), seed=5)

    # sofa fabrics
    for i, base in enumerate([(132, 136, 140), (54, 86, 150), (166, 52, 48), (72, 120, 70), (220, 176, 52), (206, 190, 160)]):
        tex(f"sofa_{i}", lambda x, y, r, c=base: shade(c, 0.82) if x in (0, 15) or y in (0, 15) else speckle(c, 0.05, r), seed=10 + i)

    tex("table_top", lambda x, y, r: speckle((244, 244, 240), 0.015, r) if 0 < x < 15 and 0 < y < 15 else (220, 220, 216), seed=20)
    tex("table_leg", lambda x, y, r: speckle((120, 124, 128), 0.05, r), seed=21)
    tex("lamp_shade", lambda x, y, r: speckle((248, 240, 220), 0.03, r) if y % 5 else (230, 220, 196), seed=22)

    def cubby(x, y, r):
        if x in (0, 1, 7, 8, 14, 15) or y in (0, 1, 7, 8, 14, 15):
            return speckle((242, 242, 240), 0.02, r)
        if (x < 7 and y < 7) or (x > 8 and y > 8):
            return speckle((40, 40, 44), 0.05, r)                          # empty cubbies
        return speckle((206, 170, 110), 0.05, r) if y > 3 and y < 7 or y > 10 else speckle((40, 40, 44), 0.05, r)
    tex("cube_shelf_front", cubby, seed=23)
    tex("cube_shelf_side", lambda x, y, r: speckle((240, 240, 238), 0.02, r) if x and y and x < 15 and y < 15 else (222, 222, 220), seed=24)

    def cabinet(x, y, r):
        if y < 2:
            return (210, 210, 206)
        if x in (0, 7, 8, 15) or y in (2, 15):
            return (200, 200, 196)
        if y == 4 and x in (5, 10):
            return (70, 70, 74)                                            # handles
        return speckle((246, 246, 244), 0.015, r)
    tex("cabinet_front", cabinet, seed=25)
    tex("countertop", lambda x, y, r: speckle((168, 132, 92), 0.07, r), seed=26)

    def crate_side(x, y, r):
        if 6 <= x <= 9:
            return speckle((214, 196, 150), 0.03, r)                       # tape
        if 2 <= y <= 4 and 2 <= x <= 13:
            return (40, 60, 140) if y == 3 else (245, 245, 240)            # the label
        if x in (0, 15) or y in (0, 15):
            return (150, 112, 70)
        return speckle((188, 146, 96), 0.05, r)
    tex("stock_crate_side", crate_side, seed=27)
    tex("stock_crate_top", lambda x, y, r: speckle((214, 196, 150), 0.03, r) if 6 <= x <= 9 else (150, 112, 70) if x in (0, 15) or y in (0, 15)
        else speckle((188, 146, 96), 0.05, r), seed=28)

    def counter_top(x, y, r):
        if x in (0, 15) or y in (0, 15):
            return (150, 154, 158)
        if 2 <= x <= 13 and 3 <= y <= 12:
            for cx, cy in ((5, 6), (10, 7), (7, 10)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= 3:
                    return speckle((120, 72, 40), 0.1, r)                  # meatballs
            if (x - 11) ** 2 + (y - 11) ** 2 <= 1:
                return (190, 30, 40)                                       # lingonberries
            return speckle((236, 214, 150), 0.05, r)                       # mash
        return speckle((196, 200, 204), 0.04, r)
    tex("food_counter_top", counter_top, seed=29)
    tex("steel", lambda x, y, r: speckle((186, 190, 194), 0.04, r) if y % 8 else (160, 164, 168), seed=30)
    tex("checkout_top", lambda x, y, r: ((30, 30, 32) if y % 4 else (60, 60, 64)) if 3 <= x <= 12 else speckle((170, 172, 176), 0.04, r), seed=31)

    digits = {"1": [".#.", "##.", ".#.", ".#.", "###"], "4": ["#.#", "#.#", "###", "..#", "..#"]}

    def aisle(x, y, r):
        if x in (0, 15) or y in (0, 15):
            return (150, 20, 24)
        for i, d in enumerate("14"):
            col, row = x - 4 - i * 4, y - 5
            if 0 <= col < 3 and 0 <= row < 5 and digits[d][row][col] == "#":
                return (250, 250, 250)
        return speckle((206, 28, 34), 0.04, r)
    tex("aisle_marker", aisle, seed=32)
    tex("pallet_top", lambda x, y, r: speckle((176, 140, 92), 0.08, r) if x % 5 != 4 else (90, 70, 46), seed=33)
    tex("pallet_side", lambda x, y, r: None if 6 <= y <= 10 and (3 <= x <= 6 or 9 <= x <= 12) else speckle((176, 140, 92), 0.08, r), seed=34)

    def flat_side(x, y, r):
        if 5 <= y <= 7:
            return (40, 70, 150) if x < 9 else (240, 200, 40)              # the stripe on the box
        if x in (0, 15) or y in (0, 15):
            return (150, 112, 70)
        return speckle((196, 156, 104), 0.05, r)
    tex("flat_pack_side", flat_side, seed=35)
    tex("flat_pack_top", lambda x, y, r: (214, 196, 150) if y in (7, 8) else speckle((196, 156, 104), 0.05, r), seed=36)
    tex("paper_lamp", lambda x, y, r: (226, 214, 186) if y % 3 == 0 else speckle((252, 248, 232), 0.02, r), seed=37)
    tex("rack_upright", lambda x, y, r: (24, 40, 80) if x % 4 == 2 and y % 3 == 1 else speckle((40, 70, 140), 0.05, r), seed=38)
    tex("rack_beam", lambda x, y, r: speckle((232, 108, 30), 0.05, r) if 2 <= y <= 13 else (180, 80, 20), seed=39)
    tex("rack_deck", lambda x, y, r: (90, 92, 96) if x % 4 == 0 or y % 4 == 0 else speckle((150, 152, 156), 0.04, r), seed=40)

    def door(top):
        def paint(x, y, r):
            if x in (0, 1, 14, 15) or (top and y in (0, 1)) or (not top and y in (14, 15)):
                return (210, 214, 218)                                     # the frame
            if not top and y == 1 and 3 <= x <= 12:
                return (250, 210, 40)                                      # the push bar
            if 3 <= x <= 12 and ((top and y == 13) or (not top and y == 3)):
                return (24, 76, 170)
            return (200, 230, 245, 90)                                     # glass
        return paint
    tex("store_door_top", door(True), seed=41)
    tex("store_door_bottom", door(False), seed=42)


# =============================================================================== item textures

def item_textures():
    def plate(x, y, r):
        d = math.hypot(x - 7.5, y - 8.5)
        if d > 7.2:
            return None
        if d > 6.2:
            return (210, 210, 206)
        for cx, cy in ((5, 7), (9, 6), (7, 10), (10, 10)):
            if math.hypot(x - cx, y - cy) <= 1.6:
                return speckle((120, 70, 38), 0.12, r)
        if math.hypot(x - 4, y - 11) <= 1.2:
            return (190, 26, 40)
        return speckle((245, 245, 242), 0.02, r)
    tex("meatball_plate", plate, seed=50, folder="item")

    def juice(x, y, r):
        if not ((5 <= x <= 10 and 6 <= y <= 14) or (6 <= x <= 9 and 3 <= y <= 5)):
            return None
        if y <= 3:
            return (150, 110, 70)                                          # the cork
        if x in (5, 10) or y == 14:
            return (200, 220, 230)
        return speckle((196, 30, 50), 0.08, r) if y >= 8 else (220, 236, 240, 150)
    tex("lingonberry_juice", juice, seed=51, folder="item")

    def badge(x, y, r):
        if not (1 <= x <= 14 and 4 <= y <= 11):
            return None
        if x in (1, 14) or y in (4, 11):
            return (30, 70, 160)
        if y == 6 and 3 <= x <= 12:
            return (30, 30, 30)
        if y == 8 and 3 <= x <= 9:
            return (60, 60, 60)
        return (246, 206, 40)
    tex("staff_badge", badge, seed=52, folder="item")

    def door_item(x, y, r):
        if not (4 <= x <= 11):
            return None
        if x in (4, 11) or y in (0, 15):
            return (210, 214, 218)
        if y == 9 and 6 <= x <= 9:
            return (250, 210, 40)
        return (190, 224, 240)
    tex("store_door", door_item, seed=53, folder="item")


# =============================================================================== models and blockstates

def model(name, data):
    write(ASSETS / f"models/block/{name}.json", data)


def blockstate(name, data):
    write(ASSETS / f"blockstates/{name}.json", data)


def item_model(name, parent=None, layer=None):
    data = {"parent": parent} if parent else {"parent": "minecraft:item/generated", "textures": {"layer0": layer}}
    write(ASSETS / f"models/item/{name}.json", data)


def b(name):
    return M + "block/" + name


FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def facing_state(name, model_name=None):
    blockstate(name, {"variants": {f"facing={f}": ({"model": b(model_name or name), "y": y} if y else {"model": b(model_name or name)})
                                   for f, y in FACINGS.items()}})


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


def simple_model(name, data):
    model(name, data)
    blockstate(name, {"variants": {"": {"model": b(name)}}})
    item_model(name, b(name))


def block_models():
    for name in ("store_floor", "store_walkway", "store_partition", "aisle_marker"):
        simple_model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": b(name)}})

    # the hanging EXIT sign: a thin panel, a different face each side
    model("exit_sign", {"parent": "minecraft:block/block", "textures": {"particle": b("exit_sign_front"), "front": b("exit_sign_front"),
                                                                        "back": b("exit_sign_back"), "edge": b("exit_sign_edge")},
                        "elements": [{"from": [0, 3, 7], "to": [16, 13, 9], "faces": {
                            "north": {"texture": "#front", "uv": [0, 3, 16, 13]}, "south": {"texture": "#back", "uv": [0, 3, 16, 13]},
                            "east": {"texture": "#edge"}, "west": {"texture": "#edge"}, "up": {"texture": "#edge"}, "down": {"texture": "#edge"}}}]})
    facing_state("exit_sign")
    item_model("exit_sign", b("exit_sign"))

    model("store_light", {"parent": "minecraft:block/block", "textures": {"particle": b("store_light"), "t": b("store_light")},
                          "elements": [element((5, 0, 4), (11, 5, 12), {"*": "#t"})]})
    facing_state("store_light")
    item_model("store_light", b("store_light"))

    # the sofa: four shapes (alone, left end, middle, right end) in six fabrics
    feet = "minecraft:block/dark_oak_planks"
    for part in ("single", "left", "middle", "right"):
        els = [element((0, 1, 0), (16, 7, 16), {"*": "#fabric"}), element((0, 7, 11), (16, 16, 16), {"*": "#fabric"}),
               element((1, 0, 1), (3, 1, 3), {"*": "#feet"}), element((13, 0, 1), (15, 1, 3), {"*": "#feet"}),
               element((1, 0, 13), (3, 1, 15), {"*": "#feet"}), element((13, 0, 13), (15, 1, 15), {"*": "#feet"})]
        if part in ("single", "left"):
            els.append(element((0, 7, 0), (3, 12, 11), {"*": "#fabric"}))
        if part in ("single", "right"):
            els.append(element((13, 7, 0), (16, 12, 11), {"*": "#fabric"}))
        model(f"sofa_{part}_template", {"parent": "minecraft:block/block", "textures": {"particle": "#fabric"}, "elements": els})
        for color in range(6):
            model(f"sofa_{part}_{color}", {"parent": b(f"sofa_{part}_template"), "textures": {"fabric": b(f"sofa_{color}"), "feet": feet}})
    variants = {}
    for f, y in FACINGS.items():
        for part in ("single", "left", "middle", "right"):
            for color in range(6):
                v = {"model": b(f"sofa_{part}_{color}")}
                if y:
                    v["y"] = y
                variants[f"color={color},facing={f},part={part}"] = v
    blockstate("sofa", {"variants": variants})
    item_model("sofa", b("sofa_single_0"))

    wood, seat = "minecraft:block/birch_planks", "minecraft:block/white_wool"
    model("store_chair", {"parent": "minecraft:block/block", "textures": {"particle": wood, "wood": wood, "seat": seat}, "elements": [
        element((3, 0, 3), (5, 8, 5), {"*": "#wood"}), element((11, 0, 3), (13, 8, 5), {"*": "#wood"}),
        element((3, 0, 11), (5, 8, 13), {"*": "#wood"}), element((11, 0, 11), (13, 8, 13), {"*": "#wood"}),
        element((3, 8, 3), (13, 10, 13), {"*": "#seat"}), element((3, 10, 11), (13, 20, 13), {"*": "#wood"})]})
    facing_state("store_chair")
    item_model("store_chair", b("store_chair"))

    legs = [element((1, 0, 1), (3, 13, 3), {"*": "#leg"}), element((13, 0, 1), (15, 13, 3), {"*": "#leg"}),
            element((1, 0, 13), (3, 13, 15), {"*": "#leg"}), element((13, 0, 13), (15, 13, 15), {"*": "#leg"})]
    simple_model("dining_table", {"parent": "minecraft:block/block", "textures": {"particle": b("table_top"), "top": b("table_top"), "leg": b("table_leg")},
                                  "elements": [element((0, 13, 0), (16, 16, 16), {"*": "#top"})] + legs})
    simple_model("coffee_table", {"parent": "minecraft:block/block", "textures": {"particle": wood, "top": wood, "leg": feet}, "elements": [
        element((1, 5, 1), (15, 7, 15), {"*": "#top"}), element((2, 0, 2), (4, 5, 4), {"*": "#leg"}), element((12, 0, 2), (14, 5, 4), {"*": "#leg"}),
        element((2, 0, 12), (4, 5, 14), {"*": "#leg"}), element((12, 0, 12), (14, 5, 14), {"*": "#leg"})]})

    for name, front, side, top in (("cube_shelf", "cube_shelf_front", "cube_shelf_side", "cube_shelf_side"),
                                   ("kitchen_cabinet", "cabinet_front", "cube_shelf_side", "countertop"),
                                   ("checkout_counter", "steel", "steel", "checkout_top"),
                                   ("food_counter", "steel", "steel", "food_counter_top")):
        model(name, {"parent": "minecraft:block/orientable", "textures": {"front": b(front), "side": b(side), "top": b(top)}})
        facing_state(name)
        item_model(name, b(name))

    simple_model("floor_lamp", {"parent": "minecraft:block/block", "textures": {"particle": b("lamp_shade"), "shade": b("lamp_shade"),
                                                                                "pole": "minecraft:block/black_concrete"}, "elements": [
        element((6, 0, 6), (10, 1, 10), {"*": "#pole"}), element((7.5, 1, 7.5), (8.5, 11, 8.5), {"*": "#pole"}),
        element((4, 10, 4), (12, 16, 12), {"*": "#shade"})]})
    simple_model("stock_crate", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": b("stock_crate_side"), "top": b("stock_crate_top"), "bottom": b("stock_crate_top")}})
    simple_model("store_pallet", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                  "textures": {"particle": b("pallet_top"), "top": b("pallet_top"), "side": b("pallet_side")},
                                  "elements": [element((0, 0, 0), (16, 4, 16), {"up": "#top", "down": "#top", "north": "#side", "south": "#side",
                                                                                "east": "#side", "west": "#side"})]})
    simple_model("flat_pack_box", {"parent": "minecraft:block/block", "textures": {"particle": b("flat_pack_side"), "top": b("flat_pack_top"),
                                                                                   "side": b("flat_pack_side")},
                                   "elements": [element((0, 0, 0), (16, 16, 16), {"up": "#top", "down": "#top", "north": "#side", "south": "#side",
                                                                                  "east": "#side", "west": "#side"})]})
    simple_model("paper_lamp", {"parent": "minecraft:block/block", "textures": {"particle": b("paper_lamp"), "p": b("paper_lamp"),
                                                                                "cord": "minecraft:block/black_concrete"}, "elements": [
        element((3, 2, 3), (13, 12, 13), {"*": "#p"}), element((7.5, 12, 7.5), (8.5, 16, 8.5), {"*": "#cord"})]})
    simple_model("rack_upright", {"parent": "minecraft:block/block", "textures": {"particle": b("rack_upright"), "t": b("rack_upright")},
                                  "elements": [element((5, 0, 5), (11, 16, 11), {"*": "#t"})]})
    simple_model("rack_shelf", {"parent": "minecraft:block/block", "textures": {"particle": b("rack_beam"), "beam": b("rack_beam"), "deck": b("rack_deck")},
                                "elements": [element((0, 13, 2), (16, 16, 14), {"*": "#deck"}), element((0, 13, 0), (16, 16, 2), {"*": "#beam"}),
                                             element((0, 13, 14), (16, 16, 16), {"*": "#beam"})]})

    # the store's glass doors: the exit door's blockstate, with its own models
    door_state = (ASSETS / "blockstates/exit_door.json").read_text(encoding="utf-8").replace("exit_door", "store_door")
    (ASSETS / "blockstates/store_door.json").write_text(door_state, encoding="utf-8")
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for opened in ("", "_open"):
                model(f"store_door_{half}_{hinge}{opened}", {"parent": f"minecraft:block/door_{half}_{hinge}{opened}",
                                                              "render_type": "minecraft:translucent",
                                                              "textures": {"bottom": b("store_door_bottom"), "top": b("store_door_top")}})
    item_model("store_door", layer=M + "item/store_door")

    for name in ("meatball_plate", "lingonberry_juice", "staff_badge"):
        item_model(name, layer=M + "item/" + name)
    item_model("staff_spawn_egg", "minecraft:item/template_spawn_egg")


# =============================================================================== v2: pillars, arrows, department signs, new departments

ICONS = [
    # living: a sofa
    ["............", "............", "..########..", "..#......#..", "############", "#.########.#", "############", ".#........#.", "............", "............"],
    # bedroom: a bed
    ["............", "#...........", "#...........", "###.........", "############", "############", "#..........#", "#..........#", "............", "............"],
    # kitchen: a pot
    ["............", "....####....", "..########..", "#.########.#", "############", "..########..", "..########..", "...######...", "............", "............"],
    # office: a desk and a lamp
    ["........##..", ".......#..#.", "........##..", ".........#..", "############", "#.........##", "#........#.#", "#........#.#", "#........###", "............"],
    # market hall: a shopping bag
    ["............", "....####....", "...#....#...", "..########..", "..########..", "..########..", "..########..", "..########..", "............", "............"],
    # warehouse: boxes
    ["............", "....####....", "....#..#....", "....####....", "..########..", "..#..##..#..", "..########..", "............", "............", "............"],
    # restaurant: a fork and a knife
    ["............", "..#.#.#..#..", "..#.#.#..##.", "..#####..##.", "...###...##.", "....#....#..", "....#....#..", "....#....#..", "....#....#..", "............"],
    # checkout: a trolley
    ["#...........", ".#..........", ".##########.", ".#########..", "..########..", "..#######...", "..#.....#...", "..#######...", "...#...#....", "............"],
    # kids: a shark
    ["............", "......#.....", ".....##.....", "..#######..#", ".#########.#", "###########.", ".#########.#", "...##......#", "............", "............"],
    # bathroom: a bath
    ["#...........", "#...........", "##..........", "#...........", "############", "############", ".##########.", "..########..", "..#......#..", "............"],
    # lighting: a bulb
    ["....####....", "...#....#...", "..#......#..", "..#......#..", "...#....#...", "....#..#....", "....####....", "....####....", ".....##.....", "............"],
    # plants: a potted plant
    ["....#..#....", "...##.##....", "..###.###...", "...##.##....", ".....#......", "...#####....", "...#####....", "....###.....", "............", "............"],
    # textiles: a rolled rug
    ["............", "..#########.", ".#.#......#.", "#.#.#.....#.", ".#.#......#.", "..#########.", "............", "............", "............", "............"],
    # bargains: a percent sign
    ["............", ".##......#..", ".##.....#...", "........#...", ".......#....", "......#.....", ".....#......", "....#....##.", "...#.....##.", "............"],
    # hardware: a hammer
    ["............", ".#######....", ".#######....", ".#######....", "....##......", "....##......", "....##......", "....##......", "....##......", "............"],
    # backyard: a garden umbrella
    [".....##.....", "...######...", "..########..", ".##########.", "......#.....", "......#.....", "......#.....", "....#####...", "............", "............"],
    # nature: a pine tree
    [".....##.....", "....####....", "...######...", "....####....", "..########..", ".##########.", ".....##.....", ".....##.....", "............", "............"],
    # seasonal: a snowflake
    [".....#......", "..#..#..#...", "...#.#.#....", "....###.....", ".#########..", "....###.....", "...#.#.#....", "..#..#..#...", ".....#......", "............"],
]


def department_sign_textures():
    for i, icon in enumerate(ICONS):
        img = Image.new("RGBA", (32, 32), rgba((248, 204, 40)))
        for y in range(32):
            for x in range(32):
                if y < 4 or y > 27:
                    img.putpixel((x, y), (0, 0, 0, 0))
                elif y in (4, 27) or x in (0, 31) or y in (5, 26) or x in (1, 30):
                    img.putpixel((x, y), rgba((24, 76, 170)))
        for row, line in enumerate(icon):
            for col, ch in enumerate(line):
                if ch == "#":
                    for dy in range(2):
                        for dx in range(2):
                            img.putpixel((4 + col * 2 + dx, 6 + row * 2 + dy), rgba((24, 60, 140)))
        img.save(TEX / f"block/department_sign_{i}.png")


def v2_art():
    tex("store_pillar", lambda x, y, r: (196, 198, 198) if x in (0, 15) else speckle((226, 226, 224), 0.02, r), seed=60)

    def arrow(x, y, r):
        if 6 <= x <= 9 and 6 <= y <= 13:
            return (40, 90, 200)                                           # the shaft (pointing north, up the texture)
        if 2 <= y <= 6 and abs(x - 7.5) <= (y - 1.5):
            return (40, 90, 200)                                           # the head
        return None
    tex("floor_arrow", arrow, seed=61)

    def balls(x, y, r):
        colors = [(220, 40, 40), (250, 210, 40), (60, 150, 230), (90, 200, 60), (240, 120, 30), (230, 90, 170)]
        cell = (x // 3 + y // 3 * 7 + (x // 3) * (y // 3)) % len(colors)
        if (x % 3 == 2 and y % 3 == 2):
            return shade(colors[cell], 0.7)
        return shade(colors[cell], 1.0 if (x + y) % 3 else 1.15)
    tex("ball_pit", balls, seed=62)

    tex("shark_body", lambda x, y, r: (245, 245, 245) if y >= 11 else speckle((96, 136, 186), 0.04, r), seed=63)

    def shark_face(x, y, r):
        if y >= 11:
            return (245, 245, 245)
        if y in (7, 8) and x in (2, 13):
            return (20, 20, 24)                                            # eyes
        if y == 12 and 4 <= x <= 11:
            return (90, 60, 70)                                            # the smile
        return speckle((96, 136, 186), 0.04, r)
    tex("shark_face", shark_face, seed=64)
    tex("ceramic", lambda x, y, r: speckle((246, 248, 250), 0.015, r) if x and y and x < 15 and y < 15 else (222, 226, 230), seed=65)

    def toilet_top(x, y, r):
        d = ((x - 7.5) / 4.0) ** 2 + ((y - 8.0) / 5.0) ** 2
        if d < 0.6:
            return (110, 170, 200)                                         # water
        if d < 1.0:
            return (230, 232, 236)
        return speckle((246, 248, 250), 0.015, r)
    tex("toilet_top", toilet_top, seed=66)
    tex("bath_water", lambda x, y, r: speckle((120, 180, 220), 0.05, r), seed=67)

    department_sign_textures()

    # the roof: steel decking and trusses, and now and then a light panel
    tex("store_ceiling", lambda x, y, r: (70, 72, 76) if x % 8 == 0 or y % 8 == 0 else speckle((112, 114, 118), 0.04, r), seed=68)
    tex("store_ceiling_light", lambda x, y, r: (150, 152, 156) if x in (0, 15) or y in (0, 15) else (255, 252, 236), seed=69)
    for name in ("store_ceiling", "store_ceiling_light"):
        simple_model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": b(name)}})

    model("store_pillar", {"parent": "minecraft:block/cube_all", "textures": {"all": b("store_pillar")}})
    blockstate("store_pillar", {"variants": {"": {"model": b("store_pillar")}}})
    item_model("store_pillar", b("store_pillar"))

    model("floor_arrow", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                          "textures": {"particle": b("floor_arrow"), "a": b("floor_arrow")},
                          "elements": [{"from": [0, 0, 0], "to": [16, 0.25, 16], "faces": {"up": {"texture": "#a"}, "down": {"texture": "#a"}}}]})
    facing_state("floor_arrow")
    item_model("floor_arrow", layer=b("floor_arrow"))

    variants = {}
    for i in range(len(ICONS)):
        model(f"department_sign_{i}", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                        "textures": {"particle": b(f"department_sign_{i}"), "icon": b(f"department_sign_{i}"), "edge": b("exit_sign_edge")},
                                        "elements": [{"from": [0, 2, 7], "to": [16, 14, 9], "faces": {
                                            "north": {"texture": "#icon", "uv": [0, 2, 16, 14]}, "south": {"texture": "#icon", "uv": [16, 2, 0, 14]},
                                            "east": {"texture": "#edge"}, "west": {"texture": "#edge"}, "up": {"texture": "#edge"},
                                            "down": {"texture": "#edge"}}}]})
        for f, y in FACINGS.items():
            v = {"model": b(f"department_sign_{i}")}
            if y:
                v["y"] = y
            variants[f"facing={f},icon={i}"] = v
    blockstate("department_sign", {"variants": variants})
    item_model("department_sign", b("department_sign_8"))

    simple_model("loaded_pallet", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                   "textures": {"particle": b("flat_pack_side"), "top": b("pallet_top"), "side": b("pallet_side"),
                                                "box": b("flat_pack_side"), "boxtop": b("flat_pack_top")},
                                   "elements": [element((0, 0, 0), (16, 4, 16), {"down": "#top", "north": "#side", "south": "#side", "east": "#side", "west": "#side"}),
                                                element((0, 4, 0), (16, 16, 16), {"up": "#boxtop", "north": "#box", "south": "#box", "east": "#box", "west": "#box"})]})
    simple_model("ball_pit", {"parent": "minecraft:block/block", "textures": {"particle": b("ball_pit"), "t": b("ball_pit")},
                              "elements": [element((0, 0, 0), (16, 8, 16), {"*": "#t"})]})

    model("shark_plush", {"parent": "minecraft:block/block", "textures": {"particle": b("shark_body"), "body": b("shark_body"), "face": b("shark_face")},
                          "elements": [element((5, 0, 2), (11, 6, 13), {"north": "#face", "*": "#body"}),
                                       element((6, 1, 13), (10, 5, 15), {"*": "#body"}),
                                       element((7, 0, 15), (9, 7, 16), {"*": "#body"}),
                                       element((7.5, 6, 6), (8.5, 9, 9), {"*": "#body"}),
                                       element((3, 0, 6), (5, 1, 9), {"*": "#body"}), element((11, 0, 6), (13, 1, 9), {"*": "#body"})]})
    facing_state("shark_plush")
    item_model("shark_plush", b("shark_plush"))

    model("toilet", {"parent": "minecraft:block/block", "textures": {"particle": b("ceramic"), "c": b("ceramic"), "top": b("toilet_top")},
                     "elements": [element((4, 0, 3), (12, 5, 11), {"*": "#c"}), element((3, 5, 2), (13, 8, 12), {"up": "#top", "*": "#c"}),
                                  element((2, 8, 11), (14, 18, 15), {"*": "#c"})]})
    facing_state("toilet")
    item_model("toilet", b("toilet"))

    model("bathtub", {"parent": "minecraft:block/block", "textures": {"particle": b("ceramic"), "c": b("ceramic"), "w": b("bath_water"), "tap": "minecraft:block/iron_block"},
                      "elements": [element((0, 0, 0), (16, 2, 16), {"*": "#c"}), element((0, 2, 0), (16, 10, 2), {"*": "#c"}),
                                   element((0, 2, 14), (16, 10, 16), {"*": "#c"}), element((0, 2, 2), (2, 10, 14), {"*": "#c"}),
                                   element((14, 2, 2), (16, 10, 14), {"*": "#c"}), element((2, 2, 2), (14, 7, 14), {"up": "#w"}),
                                   element((7, 10, 13), (9, 12, 15), {"*": "#tap"})]})
    facing_state("bathtub")
    item_model("bathtub", b("bathtub"))


# =============================================================================== data

STORE_BLOCKS = ["store_floor", "store_walkway", "store_partition", "store_light", "exit_sign", "sofa", "store_chair", "dining_table",
                "coffee_table", "cube_shelf", "floor_lamp", "kitchen_cabinet", "stock_crate", "food_counter", "checkout_counter",
                "aisle_marker", "store_pallet", "flat_pack_box", "paper_lamp", "rack_upright", "rack_shelf"]


def entry(name, weight=1, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ":" in name else M + name, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def chest(name, lo, hi, entries):
    write(DATA / f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}]})


def dimension():
    write(DATA / "dimension_type/infinite_ikea.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.0, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False, "has_raids": False,
        "min_y": 0, "height": 128, "logical_height": 128, "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": M + "infinite_ikea"})
    write(DATA / "dimension/infinite_ikea.json", {"type": M + "infinite_ikea", "generator": {
        "type": M + "backrooms_level", "level": "infinite_ikea",
        "biome_source": {"type": "minecraft:fixed", "biome": M + "infinite_ikea"}}})
    write(DATA / "worldgen/biome/infinite_ikea.json", {
        "has_precipitation": False, "temperature": 0.7, "downfall": 0.0,
        "effects": {"fog_color": 0xC6C9CC, "sky_color": 0xAEB2B6, "water_color": 0x3F76E4, "water_fog_color": 0x050533,
                    "grass_color": 0x8DB360, "foliage_color": 0x77AB2F,
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}},
        "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})


def loot():
    chest("ikea_warehouse", 3, 6, [
        entry("sofa", 6, 1, 2), entry("store_chair", 6, 2, 4), entry("dining_table", 4, 1, 2), entry("coffee_table", 4),
        entry("cube_shelf", 5, 1, 3), entry("floor_lamp", 4), entry("kitchen_cabinet", 4, 1, 3), entry("paper_lamp", 3, 1, 2),
        entry("flat_pack_box", 6, 2, 6), entry("store_pallet", 3, 1, 2), entry("minecraft:white_bed", 2), entry("minecraft:blue_bed", 1),
        entry("minecraft:lantern", 3, 1, 2), entry("minecraft:item_frame", 2, 1, 3), entry("minecraft:painting", 2), entry("staff_badge", 1)])
    chest("ikea_market", 3, 6, [
        entry("minecraft:candle", 6, 2, 5), entry("minecraft:white_candle", 4, 1, 4), entry("minecraft:flower_pot", 5, 1, 3),
        entry("minecraft:lantern", 4, 1, 2), entry("minecraft:azalea", 3), entry("minecraft:fern", 3, 1, 2), entry("minecraft:decorated_pot", 2),
        entry("minecraft:white_carpet", 3, 2, 4), entry("minecraft:light_gray_carpet", 3, 2, 4), entry("minecraft:blue_carpet", 2, 1, 3),
        entry("minecraft:item_frame", 2, 1, 2), entry("minecraft:painting", 2), entry("meatball_plate", 4, 1, 2),
        entry("lingonberry_juice", 3, 1, 2), entry("almond_water", 1), entry("staff_badge", 1)])
    chest("ikea_kids", 2, 5, [
        entry("shark_plush", 8, 1, 3), entry("ball_pit", 4, 2, 6), entry("balloon", 4, 1, 3), entry("minecraft:cookie", 4, 2, 6),
        entry("minecraft:sweet_berries", 3, 2, 5), entry("minecraft:painting", 2), entry("minecraft:white_carpet", 2, 1, 3),
        entry("lingonberry_juice", 2), entry("staff_badge", 1)])
    chest("ikea_hardware", 3, 6, [
        entry("minecraft:cobblestone", 8, 8, 24), entry("minecraft:stone", 5, 4, 12), entry("minecraft:iron_ingot", 5, 1, 4),
        entry("minecraft:coal", 6, 2, 8), entry("minecraft:copper_ingot", 4, 1, 4), entry("minecraft:torch", 5, 4, 12),
        entry("minecraft:stone_pickaxe", 3), entry("minecraft:stone_axe", 2), entry("minecraft:stone_shovel", 2), entry("minecraft:iron_pickaxe", 2),
        entry("minecraft:iron_axe", 1), entry("minecraft:iron_shovel", 1), entry("minecraft:bucket", 3), entry("minecraft:shears", 2),
        entry("minecraft:flint_and_steel", 2), entry("minecraft:furnace", 2), entry("minecraft:crafting_table", 2), entry("minecraft:chest", 2),
        entry("minecraft:glass", 3, 4, 12), entry("minecraft:sand", 3, 4, 12), entry("minecraft:gravel", 2, 4, 8), entry("minecraft:redstone", 3, 2, 6),
        entry("minecraft:ladder", 2, 4, 8), entry("minecraft:rail", 1, 4, 16), entry("minecraft:iron_bars", 1, 2, 6), entry("minecraft:anvil", 1),
        entry("minecraft:lapis_lazuli", 1, 2, 6), entry("minecraft:gold_ingot", 1, 1, 2), entry("minecraft:diamond", 1), entry("minecraft:emerald", 1),
        entry("minecraft:obsidian", 1, 1, 2), entry("minecraft:string", 2, 2, 5), entry("minecraft:leather", 2, 1, 3), entry("minecraft:book", 1, 1, 3)])
    chest("ikea_garden", 3, 6, [
        entry("minecraft:wheat_seeds", 6, 2, 8), entry("minecraft:carrot", 4, 1, 4), entry("minecraft:potato", 4, 1, 4),
        entry("minecraft:beetroot_seeds", 3, 2, 6), entry("minecraft:pumpkin_seeds", 2, 1, 4), entry("minecraft:melon_seeds", 2, 1, 4),
        entry("minecraft:oak_sapling", 4, 1, 3), entry("minecraft:birch_sapling", 3, 1, 3), entry("minecraft:spruce_sapling", 2, 1, 3),
        entry("minecraft:cherry_sapling", 2, 1, 2), entry("minecraft:bone_meal", 4, 2, 8), entry("minecraft:stone_hoe", 2), entry("minecraft:iron_hoe", 1),
        entry("minecraft:bucket", 2), entry("minecraft:composter", 1), entry("minecraft:sweet_berries", 3, 2, 6), entry("minecraft:egg", 3, 1, 3),
        entry("minecraft:wheat", 3, 2, 6), entry("minecraft:dirt", 3, 4, 12), entry("minecraft:grass_block", 2, 2, 6), entry("minecraft:sugar_cane", 2, 1, 3),
        entry("minecraft:bamboo", 1, 1, 4), entry("minecraft:shears", 1), entry("minecraft:lead", 1), entry("minecraft:name_tag", 1)])
    chest("ikea_seasonal", 3, 6, [
        entry("minecraft:cookie", 6, 3, 8), entry("minecraft:pumpkin_pie", 4, 1, 2), entry("minecraft:cake", 2), entry("minecraft:snowball", 4, 4, 16),
        entry("minecraft:red_wool", 3, 1, 4), entry("minecraft:green_wool", 3, 1, 4), entry("minecraft:lantern", 3, 1, 3), entry("minecraft:candle", 3, 1, 4),
        entry("minecraft:glow_berries", 3, 2, 6), entry("minecraft:gold_nugget", 3, 2, 6), entry("minecraft:emerald", 1), entry("minecraft:diamond", 1),
        entry("minecraft:firework_rocket", 2, 1, 4), entry("minecraft:ice", 2, 2, 6), entry("minecraft:packed_ice", 2, 2, 6),
        entry("minecraft:spruce_sapling", 2, 1, 2), entry("minecraft:milk_bucket", 1), entry("minecraft:honey_bottle", 2, 1, 2)])
    for name in STORE_BLOCKS + ["floor_arrow", "department_sign", "loaded_pallet", "ball_pit", "shark_plush", "toilet", "bathtub"]:
        write(DATA / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    supplies = [entry("minecraft:cobblestone", 10, 2, 6), entry("minecraft:coal", 8, 1, 3), entry("minecraft:raw_iron", 6, 1, 2),
                entry("minecraft:iron_ingot", 3), entry("minecraft:copper_ingot", 4, 1, 3), entry("minecraft:redstone", 4, 1, 4),
                entry("minecraft:lapis_lazuli", 3, 1, 3), entry("minecraft:gold_nugget", 3, 2, 5), entry("minecraft:flint", 3),
                entry("minecraft:string", 3, 1, 3), entry("minecraft:leather", 3), entry("minecraft:bone", 2), {"type": "minecraft:empty", "weight": 6}]
    write(DATA / "loot_table/entities/staff.json", {"type": "minecraft:entity", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": supplies,
         "functions": [{"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]},
        {"rolls": 1, "entries": [entry("minecraft:diamond")], "conditions": [
            {"condition": "minecraft:killed_by_player"},
            {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting", "unenchanted_chance": 0.025,
             "enchanted_chance": {"type": "minecraft:linear", "base": 0.04, "per_level_above_first": 0.015}}]},
        {"rolls": 1, "entries": [entry("staff_badge")], "conditions": [
            {"condition": "minecraft:killed_by_player"},
            {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting", "unenchanted_chance": 0.06,
             "enchanted_chance": {"type": "minecraft:linear", "base": 0.09, "per_level_above_first": 0.03}}]},
        {"rolls": 1, "entries": [entry("meatball_plate")], "conditions": [{"condition": "minecraft:random_chance", "chance": 0.12}]}]})


def ing(n):
    if n.startswith("#"):
        return {"tag": n[1:]}
    return {"item": n if ":" in n else M + n}


def shaped(name, pattern, key, count=1, category="building"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                                         "key": {k: ing(v) for k, v in key.items()}, "result": {"id": M + name, "count": count}})


def shapeless(name, items, count=1, category="misc"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                         "ingredients": [ing(i) for i in items], "result": {"id": M + name, "count": count}})


def recipes():
    shaped("sofa", ["W  ", "WWW", "P P"], {"W": "#minecraft:wool", "P": "#minecraft:planks"}, 2)
    shaped("store_chair", ["P  ", "PPP", "S S"], {"P": "minecraft:birch_planks", "S": "minecraft:stick"}, 2)
    shaped("dining_table", ["CCC", "S S", "S S"], {"C": "minecraft:white_concrete", "S": "minecraft:stick"}, 2)
    shaped("coffee_table", ["SSS", "T T"], {"S": "minecraft:birch_slab", "T": "minecraft:stick"})
    shaped("cube_shelf", ["CPC", "P P", "CPC"], {"C": "minecraft:white_concrete", "P": "minecraft:birch_planks"}, 2)
    shaped("floor_lamp", ["W", "T", "S"], {"W": "minecraft:white_wool", "T": "minecraft:torch", "S": "minecraft:stick"})
    shaped("kitchen_cabinet", ["SSS", "CBC", "CCC"], {"S": "minecraft:smooth_stone_slab", "C": "minecraft:white_concrete", "B": "minecraft:barrel"}, 2)
    shaped("paper_lamp", [" S ", "PTP", " P "], {"S": "minecraft:string", "P": "minecraft:paper", "T": "minecraft:torch"}, 2)
    shaped("flat_pack_box", ["PPP", "PWP", "PPP"], {"P": "minecraft:paper", "W": "#minecraft:planks"}, 4)
    shaped("stock_crate", ["PPP", "PCP", "PPP"], {"P": "minecraft:paper", "C": "minecraft:chest"})
    shaped("store_pallet", ["SSS", "T T"], {"S": "#minecraft:wooden_slabs", "T": "minecraft:stick"}, 2)
    shaped("store_walkway", ["CC", "CC"], {"C": "minecraft:white_concrete"}, 4)
    shaped("store_floor", ["CC", "CC"], {"C": "minecraft:light_gray_concrete"}, 4)
    shaped("store_partition", ["CP", "PC"], {"C": "minecraft:white_concrete", "P": "minecraft:birch_planks"}, 4)
    shaped("store_light", [" I ", "IGI"], {"I": "minecraft:iron_nugget", "G": "minecraft:glowstone"}, 2)
    shaped("exit_sign", ["BBB", "BGB"], {"B": "minecraft:blue_concrete", "G": "minecraft:glowstone_dust"}, 2)
    shaped("aisle_marker", ["RRR", "RWR", "RRR"], {"R": "minecraft:red_concrete", "W": "minecraft:white_dye"}, 4)
    shaped("rack_upright", ["I", "B", "I"], {"I": "minecraft:iron_ingot", "B": "minecraft:blue_dye"}, 4)
    shaped("rack_shelf", ["III", "O O"], {"I": "minecraft:iron_ingot", "O": "minecraft:orange_dye"}, 4)
    shaped("checkout_counter", ["BBB", "III"], {"B": "minecraft:black_carpet", "I": "minecraft:iron_ingot"}, 2)
    shaped("food_counter", ["BBB", "III", "III"], {"B": "minecraft:bowl", "I": "minecraft:iron_ingot"})
    shaped("shark_plush", [" B ", "BBB", "WWB"], {"B": "minecraft:blue_wool", "W": "minecraft:white_wool"})
    shaped("toilet", ["C  ", "CQC", " C "], {"C": "minecraft:white_concrete", "Q": "minecraft:water_bucket"})
    shaped("bathtub", ["C C", "CWC"], {"C": "minecraft:white_concrete", "W": "minecraft:water_bucket"})
    shapeless("ball_pit", ["minecraft:red_wool", "minecraft:yellow_wool", "minecraft:light_blue_wool", "minecraft:lime_wool"], 4, "building")
    shaped("floor_arrow", ["B", "P"], {"B": "minecraft:blue_dye", "P": "minecraft:paper"}, 4)
    shaped("department_sign", ["YYY", "YBY"], {"Y": "minecraft:yellow_concrete", "B": "minecraft:blue_dye"}, 2)
    shapeless("loaded_pallet", ["store_pallet", "flat_pack_box"], 1, "building")
    shapeless("lingonberry_juice", ["minecraft:sweet_berries", "minecraft:sweet_berries", "minecraft:sweet_berries", "minecraft:sugar",
                                    "minecraft:glass_bottle"], category="misc")
    shapeless("meatball_plate", ["minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:baked_potato", "minecraft:bowl"], category="misc")


def structure():
    write(DATA / "tags/worldgen/biome/has_store.json", {"values": ["minecraft:" + b for b in (
        "plains", "sunflower_plains", "meadow", "forest", "flower_forest", "birch_forest", "savanna", "desert", "snowy_plains", "taiga")]})
    write(DATA / "worldgen/structure/store.json", {"type": M + "site", "kind": "store", "biomes": "#" + M + "has_store",
                                                   "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "beard_thin"})
    write(DATA / "worldgen/structure_set/store.json", {"structures": [{"structure": M + "store", "weight": 1}],
                                                       "placement": {"type": "minecraft:random_spread", "spacing": 64, "separation": 32,
                                                                     "salt": 300808}})


def advancements():
    advancement("infinite_ikea", "flat_pack_box", "level_4", {"arrived": {"trigger": "minecraft:changed_dimension",
                                                                          "conditions": {"to": M + "infinite_ikea"}}})
    advancement("the_store", "store_door", "backrooms", {"found": {"trigger": "minecraft:location", "conditions": {"player": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"location": {"structures": M + "store"}}}]}}})
    advancement("store_closed", "minecraft:clock", "infinite_ikea", IMPOSSIBLE, "challenge")
    advancement("thank_you_for_shopping", "store_door", "infinite_ikea", IMPOSSIBLE, "goal")
    advancement("employee_of_the_month", "staff_badge", "infinite_ikea", {"badge": {"trigger": "minecraft:inventory_changed",
                                                                                     "conditions": {"items": [{"items": M + "staff_badge"}]}}})


def tags():
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + b for b in ("store_floor", "store_walkway", "store_light", "exit_sign",
                                                                         "food_counter", "checkout_counter", "aisle_marker", "rack_upright", "rack_shelf")])
    merge_tag(MC / "tags/block/mineable/pickaxe.json", [M + "toilet", M + "bathtub"])
    merge_tag(MC / "tags/block/mineable/axe.json", [M + b for b in ("loaded_pallet", "store_partition", "store_chair", "dining_table", "coffee_table", "cube_shelf",
                                                                     "kitchen_cabinet", "stock_crate", "store_pallet", "flat_pack_box")])


LANG = {
    "level.rickrooms.infinite_ikea": "The Infinite IKEA",
    "dimension.rickrooms.infinite_ikea": "The Infinite IKEA",
    "biome.rickrooms.infinite_ikea": "The Infinite IKEA",
    "block.rickrooms.store_pillar": "Store Pillar",
    "block.rickrooms.store_ceiling": "Store Roof",
    "block.rickrooms.store_ceiling_light": "Store Roof Light",
    "block.rickrooms.floor_arrow": "Floor Arrow",
    "block.rickrooms.department_sign": "Department Sign",
    "block.rickrooms.loaded_pallet": "Loaded Pallet",
    "block.rickrooms.ball_pit": "Ball Pit Balls",
    "block.rickrooms.shark_plush": "Shark Plush",
    "block.rickrooms.toilet": "Toilet",
    "block.rickrooms.bathtub": "Bathtub",
    "entity.rickrooms.staff": "Staff",
    "entity.rickrooms.seat": "Seat",
    "item.rickrooms.staff_spawn_egg": "Staff Spawn Egg",
    "item.rickrooms.meatball_plate": "Plate of Meatballs",
    "item.rickrooms.lingonberry_juice": "Lingonberry Juice",
    "tooltip.rickrooms.lingonberry_juice": "A little sweet, a little sour. Steadies the nerves",
    "item.rickrooms.staff_badge": "Staff Badge",
    "tooltip.rickrooms.staff_badge": "Hold it, and after closing the staff take you for one of them",
    "block.rickrooms.store_floor": "Store Floor",
    "block.rickrooms.store_walkway": "Store Walkway",
    "block.rickrooms.store_partition": "Showroom Partition",
    "block.rickrooms.store_light": "Showroom Spotlight",
    "block.rickrooms.exit_sign": "Hanging EXIT Sign",
    "block.rickrooms.sofa": "Sofa",
    "block.rickrooms.store_chair": "Chair",
    "block.rickrooms.dining_table": "Table",
    "block.rickrooms.coffee_table": "Coffee Table",
    "block.rickrooms.cube_shelf": "Cube Shelf",
    "block.rickrooms.floor_lamp": "Floor Lamp",
    "block.rickrooms.kitchen_cabinet": "Kitchen Cabinet",
    "block.rickrooms.stock_crate": "Stock Crate",
    "container.rickrooms.stock_crate": "Stock Crate",
    "block.rickrooms.food_counter": "Food Counter",
    "block.rickrooms.checkout_counter": "Checkout Counter",
    "block.rickrooms.aisle_marker": "Aisle Marker",
    "block.rickrooms.store_pallet": "Pallet",
    "block.rickrooms.flat_pack_box": "Flat-Pack Box",
    "block.rickrooms.paper_lamp": "Paper Lamp",
    "block.rickrooms.rack_upright": "Rack Upright",
    "block.rickrooms.rack_shelf": "Rack Shelf",
    "block.rickrooms.store_door": "Store Entrance",
    "message.rickrooms.food_counter.served": "Enjoy your meal! (%s left today)",
    "message.rickrooms.food_counter.sold_out": "Sold out for today. Come back tomorrow!",
    "message.rickrooms.store.closed": "Attention, shoppers: the store is now closed. Please make your way to the nearest exit.",
    "message.rickrooms.store.open": "Good morning, shoppers! The store is now open.",
    "message.rickrooms.staff.day.0": "Hello! Can I help you find something?",
    "message.rickrooms.staff.day.1": "The store closes at nine.",
    "message.rickrooms.staff.day.2": "Have you seen our new range?",
    "message.rickrooms.staff.day.3": "The exit? Just follow the signs.",
    "message.rickrooms.staff.night.0": "The store is closed.",
    "message.rickrooms.staff.night.1": "Please make your way to the exit.",
    "message.rickrooms.staff.night.2": "You can't be here.",
    "message.rickrooms.staff.night.3": "There is no exit.",
    "subtitles.rickrooms.staff.ambient": "Staff mumbles",
    "subtitles.rickrooms.staff.closed": "Staff groans",
    "subtitles.rickrooms.staff.hurt": "Staff hurts",
    "subtitles.rickrooms.staff.death": "Staff dies",
    "subtitles.rickrooms.store.chime": "Store announcement",
    "advancements.rickrooms.infinite_ikea.title": "A Perfectly Normal Store",
    "advancements.rickrooms.infinite_ikea.description": "Find your way into the Infinite IKEA",
    "advancements.rickrooms.the_store.title": "Regular Old IKEA",
    "advancements.rickrooms.the_store.description": "Find a store standing on its own in the Overworld",
    "advancements.rickrooms.store_closed.title": "The Store Is Now Closed",
    "advancements.rickrooms.store_closed.description": "Be in the Infinite IKEA from closing time until it opens again",
    "advancements.rickrooms.thank_you_for_shopping.title": "Thank You for Shopping",
    "advancements.rickrooms.thank_you_for_shopping.description": "Find a checkout, and leave the Infinite IKEA",
    "advancements.rickrooms.employee_of_the_month.title": "Employee of the Month",
    "advancements.rickrooms.employee_of_the_month.description": "Get a Staff Badge",
}

GUIDE_PAGE = ("THE INFINITE IKEA\n\nA store that never ends, with everything in it. Crates refill every morning. Day: the staff ignore you. "
              "Night: run. A Staff Badge fools them.\n\nThe EXIT signs lie, until you're near a checkout.")


def lang():
    path = ASSETS / "lang/en_us.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(LANG)
    data["guide.rickrooms.page.19"] = GUIDE_PAGE
    write(path, data)


def sounds():
    path = ASSETS / "sounds.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    ev = lambda name, pitch=1.0, volume=1.0: {"name": name, "type": "event", "pitch": pitch, "volume": volume}
    for key, entries in {"staff.ambient": [ev("minecraft:entity.villager.ambient", 0.5, 0.8)],
                         "staff.closed": [ev("minecraft:entity.zombie_villager.ambient", 0.55, 0.9)],
                         "staff.hurt": [ev("minecraft:entity.villager.hurt", 0.55)],
                         "staff.death": [ev("minecraft:entity.zombie_villager.death", 0.6)],
                         "store.chime": [ev("minecraft:block.note_block.chime", 1.0, 1.0)]}.items():
        data[key] = {"subtitle": f"subtitles.rickrooms.{key}", "sounds": entries}
    write(path, data)


def main():
    (TEX / "entity").mkdir(parents=True, exist_ok=True)
    uvs = staff_textures()
    java_layer(STAFF, uvs)
    block_textures()
    item_textures()
    block_models()
    v2_art()
    dimension()
    loot()
    recipes()
    structure()
    advancements()
    tags()
    lang()
    sounds()
    print(f"Infinite IKEA written: staff model with {len(STAFF['parts'])} parts")


if __name__ == "__main__":
    main()
