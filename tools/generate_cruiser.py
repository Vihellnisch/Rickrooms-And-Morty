"""Rick's Space Cruiser: the model (as generated Java, client/model/CruiserLayers.java), its
texture and glow texture, the dome texture, and the icons of the cruiser, its upgrades and remote.
Run from the project root:

    python tools/generate_cruiser.py

Modelled on the show's cruiser (reference/space_cruiser/): a grey saucer whose upper hull is a
cone of panels with yellow stripes, a bolted collar under a dark glass dome (the dome is drawn by
the renderer, not here), two octagonal thruster cans at the back on accordion hoses, two brass
headlights, stickers on the rim, and little wheels underneath.
Model space: 16 units = 1 block, y points down, ground at y = 24, the front is -Z.
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import ASSETS, TEX, Noise, box_faces, mix, pack, rgba, shade, sprite, uv_size  # noqa: E402

PROJECT = Path(__file__).resolve().parent.parent
JAVA = PROJECT / "src/main/java/com/rickandmorty/client/model/CruiserLayers.java"
M = "rickrooms:"
W, H = 512, 256
SEGMENTS = 16

parts = []   # (name, parent, pivot, rotation, [boxes (x, y, z, w, h, d, key)])


def tier(name, r, y0, y1, key):
    """A round slab: eight long, narrow boards turned 22.5 degrees apart, each exactly one side of a
    16-sided disc wide, so together they make a clean 16-gon (squares would make a star)."""
    half = round(r * math.tan(math.pi / SEGMENTS), 3)
    for i in range(8):
        parts.append((f"{name}_{i}", None, (0, 0, 0), (0, math.radians(22.5 * i), 0), [(-half, y0, -r, 2 * half, y1 - y0, 2 * r, key)]))


def ring(name, r, y0, y1, thickness, key):
    """A round wall: sixteen upright panels in a circle."""
    half = round(r * math.tan(math.pi / SEGMENTS) * 1.02, 3)
    for i in range(SEGMENTS):
        theta = 2 * math.pi * i / SEGMENTS
        parts.append((f"{name}_{i}", None, (round(-r * math.sin(theta), 3), y0, round(-r * math.cos(theta), 3)), (0, theta, 0),
                      [(-half, 0, -thickness / 2, 2 * half, y1 - y0, thickness, key)]))


# --- the hull
tier("belly", 13, 17, 19, "belly")
# lower hull: from the rim's underside in to the belly, facing down and out
for i in range(SEGMENTS):
    theta = 2 * math.pi * i / SEGMENTS
    r_mid, y_mid = 17.5, 15.5
    tilt = math.atan2(9, 3)   # angle from vertical
    parts.append((f"lower_{i}", None, (round(-r_mid * math.sin(theta), 3), y_mid, round(-r_mid * math.cos(theta), 3)),
                  (tilt, theta, 0), [(-4.5, -5, -0.5, 9, 10, 1, "lower")]))
tier("rim", 23, 12, 14, "rim")
# upper hull: a cone of panels from the rim up to the collar, every other one striped
for i in range(SEGMENTS):
    theta = 2 * math.pi * i / SEGMENTS
    r_mid, y_mid = 17.2, 9.0
    tilt = math.atan2(9.6, 6.0)
    parts.append((f"upper_{i}", None, (round(-r_mid * math.sin(theta), 3), y_mid, round(-r_mid * math.cos(theta), 3)),
                  (-tilt, theta, 0), [(-4.5, -5.5, -0.5, 9, 11, 1, "upper_striped" if i % 2 == 0 else "upper_plain")]))
# the collar the dome sits on: a ring, so the cockpit inside is open down to the floor
ring("collar", 12.5, 3.8, 6.4, 1.4, "collar")

# --- inside the dome, sunk into the cockpit: two seats, a dashboard and the steering wheel
for side, x in (("pilot", -5.0), ("passenger", 5.0)):
    parts.append((f"seat_{side}", None, (x, 12.0, 1.5), (0, 0, 0), [(-2.5, -1.2, -2.5, 5, 1, 5, "seat"), (-2.5, -7, 1.5, 5, 6, 1, "seat_back")]))
parts.append(("dashboard", None, (0, 12.0, -7.5), (0.35, 0, 0), [(-8, -4, -1.5, 16, 4, 3, "dash")]))
parts.append(("steering_column", None, (-5.0, 8.5, -5.5), (-0.9, 0, 0), [(-0.5, -0.5, -2.5, 1, 1, 3, "column")]))
parts.append(("steering_wheel", "steering_column", (0, 0, -2.5), (0, 0, 0), [(-2, -2, -0.5, 4, 4, 1, "wheel_ring")]))

# --- thrusters: octagonal cans at the back sides, on accordion hoses
for side, x in (("left", 16.5), ("right", -16.5)):
    # an octagonal can: four boards turned 45 degrees apart, each one side of the octagon wide
    can = round(4 * math.tan(math.pi / 8), 3)
    cap = round(4.6 * math.tan(math.pi / 8), 3)
    for j in range(4):
        parts.append((f"thruster_{side}_{j}", None, (x, 7.5, 13.5), (0, 0, j * math.pi / 4), [(-can, -4, -6, 2 * can, 8, 12, "thruster")]))
        parts.append((f"thruster_{side}_cap_{j}", None, (x, 7.5, 7.0), (0, 0, j * math.pi / 4), [(-cap, -4.6, -0.5, 2 * cap, 9.2, 1, "thruster_cap")]))
    inward = -1 if x > 0 else 1
    parts.append((f"hose_{side}", None, (x + inward * 3.5, 9.0, 8.0), (0, 0, 0),
                  [(inward * 0.0 - 1.5, -1.5, -1.5, 3, 3, 2, "hose"), (inward * 1.5 - 1.5, -1.0, -3.0, 3, 3, 2, "hose"),
                   (inward * 3.0 - 1.5, -0.5, -4.5, 3, 3, 2, "hose")]))

# --- headlights, and the wheels underneath
for side, x in (("left", 7.0), ("right", -7.0)):
    parts.append((f"lamp_{side}", None, (x, 10.6, -20.6), (0.25, 0, 0), [(-1.5, -1.5, -1.5, 3, 3, 3, "lamp"), (-1, -1, -2.4, 2, 2, 1, "lens")]))
for name, (x, z) in (("front", (0, -8)), ("rear_left", (8, 7)), ("rear_right", (-8, 7))):
    tyre = round(2.5 * math.tan(math.pi / 8), 3)
    for j in range(4):
        parts.append((f"wheel_{name}_{j}", None, (x, 21.5, z), (j * math.pi / 4, 0, 0), [(-1.5, -2.5, -tyre, 3, 5, 2 * tyre, "tyre")]))

# --- stickers on the rim, front left: a flag, "Shleemies", and a green blob
for key, angle, width in (("sticker_flag", -38, 4), ("sticker_shleemies", -27, 6), ("sticker_blob", -18, 2)):
    theta = math.radians(angle)
    r = 23.05
    parts.append((key, None, (round(-r * math.sin(theta), 3), 13.0, round(-r * math.cos(theta), 3)), (0, theta, 0),
                  [(-width / 2, -1, -0.5, width, 2, 1, key)]))


# =============================================================================== texture

def layout():
    sizes = {}
    for _, _, _, _, boxes in parts:
        for *_, w, h, d, key in boxes:
            size = (int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d)))
            if key in sizes and sizes[key] != size:
                raise RuntimeError(f"uv key {key} used with two sizes: {sizes[key]} and {size}")
            sizes[key] = size
    return sizes, pack([(k, *uv_size(*s)) for k, s in sizes.items()], W, H)


def jit(rng, c, a):
    return tuple(max(0, min(255, int(v + rng.randint(-a, a)))) for v in c[:3])


HULL, HULL_LIGHT, HULL_DARK = (132, 138, 142), (176, 182, 186), (96, 100, 106)
YELLOW, YELLOW_DARK = (250, 208, 40), (214, 160, 20)


def paint():
    sizes, uvs = layout()
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rng = random.Random(4242)
    noise = Noise(4243, 3.0)

    def fill(rect, fn):
        x0, y0, w, h = rect
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                img.putpixel((x, y), rgba(fn(x - x0, y - y0, w, h)))

    def hull_face(fx, fy, w, h, stripe=False):
        # brushed grey with a soft light band, like the show's shading
        t = abs(fy / max(1, h - 1) - 0.35)
        c = mix(HULL_LIGHT, HULL, min(1.0, t * 2.2))
        c = mix(c, HULL_DARK, noise(fx * 2, fy * 2) * 0.18)
        if stripe and abs(fx - (w - 1) / 2) <= 0.9:
            c = YELLOW if (fy + fx) % 5 else YELLOW_DARK
        return jit(rng, c, 3)

    for key, (w, h, d) in sizes.items():
        u, v = uvs[key]
        faces = box_faces(u, v, w, h, d)
        if key in ("upper_striped", "upper_plain", "lower", "rim"):
            for name, rect in faces.items():
                fill(rect, lambda fx, fy, fw, fh, name=name: hull_face(fx, fy, fw, fh, key == "upper_striped" and name == "front")
                     if name in ("front", "back", "left", "right") else jit(rng, HULL, 3))
            if key == "rim":
                # a highlight line round the rim
                for name in ("front", "back", "left", "right"):
                    x0, y0, fw, fh = faces[name]
                    for x in range(x0, x0 + fw):
                        img.putpixel((x, y0), rgba((200, 206, 208)))
        elif key == "belly":
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, mix((70, 74, 80), (54, 57, 62), noise(fx, fy)), 3))
        elif key == "collar":
            for name, rect in faces.items():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (184, 192, 184), 3))
                if name == "front":
                    x0, y0, fw, fh = rect
                    cx = x0 + fw // 2
                    for bx, by in ((cx, y0 + 1), (cx - 1, y0 + 1)):
                        img.putpixel((bx, by), rgba((86, 92, 92)))        # a bolt in each panel
        elif key == "thruster":
            for name, rect in faces.items():
                if name == "back":
                    x0, y0, fw, fh = rect
                    for y in range(y0, y0 + fh):
                        for x in range(x0, x0 + fw):
                            r = math.hypot(x - x0 - (fw - 1) / 2, y - y0 - (fh - 1) / 2) / (fw / 2)
                            img.putpixel((x, y), rgba((104, 118, 112) if r < 0.45 else (126, 142, 134) if r < 0.8 else (92, 106, 100)))   # a capped end
                else:
                    fill(rect, lambda fx, fy, fw, fh: jit(rng, (118, 134, 126) if fx % 4 else (92, 106, 100), 3))
        elif key == "thruster_cap":
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (140, 156, 148), 3))
        elif key == "hose":
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (196, 176, 112) if fy % 2 else (150, 130, 80), 4))
        elif key in ("lamp",):
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (206, 164, 62) if (fx + fy) % 3 else (160, 120, 40), 4))
        elif key == "lens":
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: (250, 248, 220))
                x0, y0, fw, fh = rect
                for y in range(y0, y0 + fh):
                    for x in range(x0, x0 + fw):
                        glow.putpixel((x, y), rgba((255, 250, 210)))
        elif key == "tyre":
            for name, rect in faces.items():
                if name in ("right", "left"):
                    x0, y0, fw, fh = rect
                    for y in range(y0, y0 + fh):
                        for x in range(x0, x0 + fw):
                            r = math.hypot(x - x0 - (fw - 1) / 2, y - y0 - (fh - 1) / 2)
                            img.putpixel((x, y), rgba((150, 156, 160) if r < 1.2 else (34, 37, 41)))
                else:
                    fill(rect, lambda fx, fy, fw, fh: jit(rng, (34, 37, 41) if fy % 2 else (46, 50, 54), 2))
        elif key in ("seat", "seat_back"):
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (94, 110, 62), 5))
        elif key == "dash":
            for name, rect in faces.items():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (60, 64, 70), 3))
                if name == "top":
                    x0, y0, fw, fh = rect
                    for x in range(x0 + 2, x0 + fw - 2, 3):
                        img.putpixel((x, y0 + 1), rgba((120, 255, 120)))
                        glow.putpixel((x, y0 + 1), rgba((120, 255, 120)))      # little screens
        elif key in ("column", "wheel_ring"):
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: jit(rng, (30, 30, 32), 2))
        elif key == "sticker_flag":
            for name, rect in faces.items():
                def flag(fx, fy, fw, fh):
                    if fx < fw // 2 and fy == 0:
                        return (40, 60, 150)
                    return (200, 40, 40) if (fy + fx // 2) % 2 == 0 else (240, 240, 240)
                fill(rect, flag if name == "front" else (lambda fx, fy, fw, fh: HULL))
        elif key == "sticker_shleemies":
            for name, rect in faces.items():
                fill(rect, (lambda fx, fy, fw, fh: (120, 210, 200) if (fx + fy) % 3 else (40, 90, 100)) if name == "front"
                     else (lambda fx, fy, fw, fh: HULL))
        elif key == "sticker_blob":
            for name, rect in faces.items():
                fill(rect, (lambda fx, fy, fw, fh: (140, 190, 60)) if name == "front" else (lambda fx, fy, fw, fh: HULL))
        else:
            for rect in faces.values():
                fill(rect, lambda fx, fy, fw, fh: HULL)
    img.save(TEX / "entity/space_cruiser.png")
    glow.save(TEX / "entity/space_cruiser_glow.png")
    return uvs


def dome_texture():
    """Dark, faintly purple glass with a streak of light down one side."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(64):
            streak = math.exp(-((x - 14) / 3.0) ** 2) * (1 - y / 40)
            c = mix((92, 86, 130), (210, 214, 240), streak * 0.9)
            a = int(95 + streak * 90)
            img.putpixel((x, y), (*c, a))
    img.save(TEX / "entity/space_cruiser_dome.png")


# =============================================================================== Java

def fmt(v):
    v = float(v)
    return f"{v:g}f" if v != int(v) else f"{int(v)}.0f"


def write_java(uvs):
    lines = []
    var = {None: "root"}
    for name, parent, pivot, rot, boxes in parts:
        cubes = "CubeListBuilder.create()"
        for x, y, z, w, h, d, key in boxes:
            u, v = uvs[key]
            cubes += f".texOffs({u}, {v}).addBox({fmt(x)}, {fmt(y)}, {fmt(z)}, {fmt(w)}, {fmt(h)}, {fmt(d)})"
        pose = f"PartPose.offsetAndRotation({', '.join(fmt(p) for p in pivot)}, {', '.join(fmt(round(r, 5)) for r in rot)})"
        var[name] = "p_" + name
        lines.append(f"        PartDefinition p_{name} = {var[parent]}.addOrReplaceChild(\"{name}\", {cubes}, {pose});")
    body = "\n".join(lines)
    JAVA.write_text(f"""package com.rickandmorty.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry of Rick's Space Cruiser. GENERATED by tools/generate_cruiser.py (the same run paints
 * the matching texture), so edit the shapes there, not here.
 */
@SuppressWarnings("unused")
public final class CruiserLayers {{
    private CruiserLayers() {{
    }}

    public static LayerDefinition cruiser() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {W}, {H});
    }}
}}
""", encoding="utf-8")


# =============================================================================== icons and item models

UPGRADES = {
    "disintegrator_upgrade": ((80, 230, 90), "D"),
    "minigun_upgrade": ((230, 60, 60), "M"),
    "flamethrower_upgrade": ((255, 140, 30), "F"),
    "rockets_upgrade": ((200, 200, 210), "R"),
    "laser_drill_upgrade": ((80, 200, 255), "L"),
    "repulsor_upgrade": ((190, 120, 255), "P"),
    "auto_park_upgrade": ((250, 210, 50), "A"),
    "portal_drive_upgrade": ((120, 255, 110), "G"),
}


def icons():
    write = lambda path, data: (path.parent.mkdir(parents=True, exist_ok=True), path.write_text(json.dumps(data, indent=2), encoding="utf-8"))
    cruiser = sprite([
        "................",
        "......DDDD......",
        ".....DdddDD.....",
        "....DddddddD....",
        "....RRRRRRRR....",
        "..TTHHHHHHHHTT..",
        ".TTHHYHHHHYHHTT.",
        "..HHHYHHHHYHHH..",
        ".HHHHYHHHHYHHHH.",
        "HHHLHHHHHHHHLHHH",
        "SSSSSSSSSSSSSSSS",
        ".BBBBBBBBBBBBBB.",
        "...BBBBBBBBBB...",
        "....W......W....",
        "................",
        "................",
    ], {"D": (58, 54, 92), "d": (110, 110, 160), "R": (184, 192, 184), "H": HULL, "Y": YELLOW, "T": (118, 134, 126),
        "L": (250, 240, 180), "S": (176, 182, 186), "B": (80, 84, 90), "W": (34, 37, 41)})
    cruiser.save(TEX / "item/space_cruiser.png")
    write(ASSETS / "models/item/space_cruiser.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/space_cruiser"}})

    for name, (color, letter) in UPGRADES.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(3, 13):
            for x in range(2, 14):
                edge = x in (2, 13) or y in (3, 12)
                img.putpixel((x, y), rgba((40, 44, 50) if edge else (70, 76, 84) if (x + y) % 2 else (64, 70, 78)))
        for x in (4, 6, 9, 11):                                    # connector pins
            img.putpixel((x, 13), rgba((210, 180, 70)))
            img.putpixel((x, 14), rgba((210, 180, 70)))
        for y in range(5, 11):                                     # a coloured core
            for x in range(5, 11):
                d = math.hypot(x - 7.5, y - 7.5) / 3.5
                if d <= 1:
                    img.putpixel((x, y), rgba(mix((255, 255, 255), color, d)))
        img.save(TEX / f"item/{name}.png")
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})

    # Microverse Boosters: a glass battery cell with a tiny green universe inside, and a pip per tier
    for tier, glow in ((1, (110, 255, 90)), (2, (70, 220, 255)), (3, (255, 200, 60))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(2, 15):
            for x in range(4, 12):
                if y == 2 and x not in range(6, 10):
                    continue                                        # the terminal on top
                edge = x in (4, 11) or y in (3, 14) or (y == 2)
                if edge:
                    img.putpixel((x, y), rgba((190, 196, 200) if y == 2 else (60, 64, 72)))
                else:
                    d = math.hypot(x - 7.5, (y - 8.5) * 0.8) / 4.2
                    img.putpixel((x, y), rgba(mix(glow, (24, 30, 44), min(1.0, d)), 255))
        for i in range(tier):                                      # the tier pips down the side
            img.putpixel((13, 12 - i * 3), rgba(glow))
            img.putpixel((13, 11 - i * 3), rgba(glow))
        img.putpixel((7, 7), rgba((255, 255, 255)))                 # a star in the little universe
        img.putpixel((9, 10), rgba((255, 255, 255)))
        name = f"cruiser_booster_mk{tier}"
        img.save(TEX / f"item/{name}.png")
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})

    # Speed Thrusters: a rocket nozzle with a flame, and a pip per mark
    for tier, flame in ((1, (255, 170, 60)), (2, (120, 200, 255)), (3, (200, 120, 255))):
        img = sprite([
            "................",
            "......SSSS......",
            ".....SHHHHS.....",
            ".....SHHHHS.....",
            ".....SHYYHS.....",
            ".....SHHHHS.....",
            "....SSHHHHSS....",
            "....SHHHHHHS....",
            "...SSDDDDDDSS...",
            "...SDDDDDDDDS...",
            "....FFFFFFFF....",
            ".....FffffF.....",
            "......FffF......",
            ".......FF.......",
            "................",
            "................",
        ], {"S": (60, 64, 72), "H": HULL, "Y": YELLOW, "D": (70, 76, 84), "F": flame, "f": (255, 250, 220)})
        for i in range(tier):
            img.putpixel((14, 12 - i * 3), rgba(flame))
            img.putpixel((14, 11 - i * 3), rgba(flame))
        name = f"cruiser_thrusters_mk{tier}"
        img.save(TEX / f"item/{name}.png")
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})

    # Laser Drills Mk II and III: a housing with a glowing drill bit
    for tier, glow in ((2, (80, 200, 255)), (3, (160, 90, 255))):
        img = sprite([
            "................",
            "................",
            "..SSSSSS........",
            "..SHHHHS........",
            "..SHYYHSSS......",
            "..SHHHHSBBB.....",
            "..SHHHHSBgBB....",
            "..SHHHHSBBgBB...",
            "..SHYYHSSSBgBB..",
            "..SHHHHS...BBgB.",
            "..SSSSSS....BBB.",
            "...PPPP.......B.",
            "................",
            "................",
            "................",
            "................",
        ], {"S": (60, 64, 72), "H": HULL, "Y": YELLOW, "B": glow, "g": (255, 255, 255), "P": (210, 180, 70)})
        for i in range(tier):
            img.putpixel((2 + i * 2, 13), rgba(glow))
            img.putpixel((2 + i * 2, 14), rgba(glow))
        name = f"laser_drill_mk{tier}"
        img.save(TEX / f"item/{name}.png")
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})

    remote = sprite([
        "................",
        "......KK........",
        ".....KggK.......",
        "......KK........",
        "......KK........",
        ".....KKKK.......",
        "....KSSSSK......",
        "....KSrgSK......",
        "....KSSSSK......",
        "....KSybSK......",
        "....KSSSSK......",
        "....KSSSSK......",
        "....KSSSSK......",
        ".....KKKK.......",
        "................",
        "................",
    ], {"K": (30, 32, 36), "g": (120, 255, 120), "S": (150, 156, 160), "r": (230, 60, 60), "y": (250, 208, 40), "b": (60, 120, 230)})
    remote.save(TEX / "item/cruiser_remote.png")
    write(ASSETS / "models/item/cruiser_remote.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/cruiser_remote"}})


def main():
    (TEX / "entity").mkdir(parents=True, exist_ok=True)
    (TEX / "item").mkdir(parents=True, exist_ok=True)
    uvs = paint()
    dome_texture()
    write_java(uvs)
    icons()
    print(f"Space cruiser written: {len(parts)} parts")


if __name__ == "__main__":
    main()
