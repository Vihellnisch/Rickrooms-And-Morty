"""Textures, block/item models and entity model code for Cronenberg World, the Blender Upgrade
and the Council. Run from the project root:

    python tools/generate_cronenberg_art.py

Writes PNGs under assets/rickrooms/textures, block and item model JSON, and
src/main/java/com/rickandmorty/client/model/CronenbergLayers.java (from cronenberg_models.py).
Replace any PNG with hand-made art whenever you like (keep the UV layout for entities).
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from cronenberg_models import MODELS  # noqa: E402

PROJECT = Path(__file__).resolve().parent.parent
ASSETS = PROJECT / "src/main/resources/assets/rickrooms"
TEX = ASSETS / "textures"
JAVA = PROJECT / "src/main/java/com/rickandmorty/client/model/CronenbergLayers.java"


# =============================================================================== colour helpers

def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(clamp(v * f) for v in c[:3])


def rgba(c, a=255):
    return (*c[:3], a)


class Noise:
    """Cheap smooth value noise, so textures get blotches and veins rather than static."""

    def __init__(self, seed, scale=4.0):
        self.rng = random.Random(seed)
        self.grid = {}
        self.scale = scale

    def _v(self, ix, iy):
        key = (ix, iy)
        if key not in self.grid:
            self.grid[key] = self.rng.random()
        return self.grid[key]

    def __call__(self, x, y):
        x, y = x / self.scale, y / self.scale
        ix, iy = math.floor(x), math.floor(y)
        fx, fy = x - ix, y - iy
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = self._v(ix, iy) + (self._v(ix + 1, iy) - self._v(ix, iy)) * fx
        b = self._v(ix, iy + 1) + (self._v(ix + 1, iy + 1) - self._v(ix, iy + 1)) * fx
        return a + (b - a) * fy


# =============================================================================== materials
# A material is f(x, y, rng) -> RGB, painted pixel by pixel over a face.

FLESH = (196, 92, 96)
FLESH_DARK = (120, 34, 44)
MUSCLE = (150, 36, 40)
SKIN = (222, 170, 150)
BRUISE = (130, 80, 120)
BONE = (222, 212, 184)
TUMOR = (214, 150, 120)
BLOOD = (110, 6, 12)


def veins(n1, n2, x, y):
    """Thin dark lines where two noise fields cross a threshold."""
    return abs(n1(x, y) - 0.5) < 0.035 or abs(n2(x + 7, y + 3) - 0.5) < 0.025


def mat_flesh(seed=1, base=FLESH, dark=FLESH_DARK):
    blotch, v1, v2 = Noise(seed, 5), Noise(seed + 1, 7), Noise(seed + 2, 4)

    def paint(x, y, rng):
        c = mix(base, dark, blotch(x, y) * 0.8 + rng.uniform(-0.08, 0.08))
        if veins(v1, v2, x, y):
            c = shade(dark, 0.7)
        if rng.random() < 0.02:
            c = mix(c, (240, 200, 170), 0.6)            # pale fatty spots
        return c
    return paint


def mat_skin(seed=3, base=SKIN):
    blotch, bruise = Noise(seed, 4), Noise(seed + 5, 6)

    def paint(x, y, rng):
        c = mix(base, shade(base, 0.8), blotch(x, y) + rng.uniform(-0.05, 0.05))
        if bruise(x, y) > 0.7:
            c = mix(c, BRUISE, (bruise(x, y) - 0.7) * 2.5)
        if rng.random() < 0.015:
            c = FLESH_DARK                               # open sores
        return c
    return paint


def mat_muscle(seed=5):
    fib = Noise(seed, 3)

    def paint(x, y, rng):
        c = mix(MUSCLE, (200, 70, 70), fib(x * 0.4, y * 2.5))
        return shade(c, rng.uniform(0.9, 1.05))
    return paint


def mat_bone(seed=7):
    n, crack = Noise(seed, 5), Noise(seed + 1, 3)

    def paint(x, y, rng):
        c = mix(BONE, (178, 160, 120), n(x, y) * 0.7 + rng.uniform(-0.05, 0.05))
        if abs(crack(x, y) - 0.5) < 0.03:
            c = (120, 100, 80)
        return c
    return paint


def mat_tumor(seed=9):
    n = Noise(seed, 2.5)

    def paint(x, y, rng):
        v = n(x, y)
        c = mix((236, 190, 140), (170, 80, 70), v)
        if v > 0.78:
            c = (246, 226, 150)                          # yellow pus heads
        return shade(c, rng.uniform(0.92, 1.05))
    return paint


def mat_solid(color, jitter=6):
    def paint(x, y, rng):
        return tuple(clamp(c + rng.randint(-jitter, jitter)) for c in color)
    return paint


def mat_chitin(seed=11):
    n = Noise(seed, 3)

    def paint(x, y, rng):
        c = mix((118, 150, 60), (60, 86, 34), n(x, y))
        if (x + y) % 5 == 0:
            c = shade(c, 0.8)                            # segment ridges
        return shade(c, rng.uniform(0.92, 1.06))
    return paint


def mat_cloth(color, seed=13, stripes=False):
    n = Noise(seed, 3)

    def paint(x, y, rng):
        c = mix(color, shade(color, 0.72), n(x, y) * 0.8)
        if stripes and y % 4 == 0:
            c = shade(c, 0.8)
        if rng.random() < 0.03:
            c = mix(c, BLOOD, 0.7)                       # it's been a rough apocalypse
        return c
    return paint


def mat_metal(seed=15):
    n = Noise(seed, 4)

    def paint(x, y, rng):
        c = mix((150, 156, 162), (86, 90, 96), n(x, y))
        return shade(c, rng.uniform(0.9, 1.08))
    return paint


# =============================================================================== face decorations
# A decoration is f(img, face_rect, rng) painting over one face of a box.

def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba(c))


def eye(img, cx, cy, rng, iris=(200, 30, 20), glow=None, r=1):
    """A bloodshot eye: white, iris, a pinprick pupil. Also marks the glow layer."""
    for dy in range(-r, r + 1):
        for dx in range(-r - 1, r + 2):
            put(img, cx + dx, cy + dy, (238, 226, 214) if rng.random() > 0.2 else (220, 120, 120))
    put(img, cx, cy, iris)
    put(img, cx + 1, cy, iris)
    put(img, cx, cy, (10, 6, 6))
    if glow is not None:
        for dx in (0, 1):
            put(glow, cx + dx, cy, (255, 60, 40))


def mouth(img, x0, y0, w, h, rng, glow=None):
    """A gaping hole ringed with teeth."""
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = y in (y0, y0 + h - 1)
            put(img, x, y, (236, 224, 190) if edge and (x + y) % 2 == 0 else (40, 4, 8) if not edge else (90, 10, 14))
    if glow is not None:
        for x in range(x0 + 1, x0 + w - 1):
            put(glow, x, y0 + h // 2, (120, 10, 10))


def teeth_row(img, x0, y0, w, rng):
    for x in range(x0, x0 + w):
        put(img, x, y0, (236, 226, 196) if x % 2 == 0 else (190, 170, 130))


def face(img, rect, rng, glow=None, wide=False):
    """A human face on flesh: two eyes (not quite level), a nose, and a screaming mouth."""
    x0, y0, w, h = rect
    ey = y0 + max(1, h // 4)
    eye(img, x0 + max(1, w // 4) - 1, ey, rng, glow=glow)
    eye(img, x0 + w - max(2, w // 4) - 1, ey + 1, rng, glow=glow)
    put(img, x0 + w // 2, ey + 2, shade(SKIN, 0.7))
    mw = max(2, w // 2 if wide else w // 3)
    mouth(img, x0 + (w - mw) // 2, y0 + h - max(2, h // 3) - 1, mw, max(2, h // 3), rng, glow=glow)


# =============================================================================== box-UV painting

def box_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def uv_size(w, h, d):
    return 2 * (w + d), d + h


def pack(requests, width, height):
    """Shelf-packs (key, w, h) rectangles. Returns key -> (u, v)."""
    placed, x, y, shelf = {}, 0, 0, 0
    for key, w, h in sorted(requests, key=lambda r: (-r[2], -r[1])):
        if x + w > width:
            x, y, shelf = 0, y + shelf, 0
        if y + h > height:
            raise RuntimeError(f"texture too small for {key}")
        placed[key] = (x, y)
        x += w
        shelf = max(shelf, h)
    return placed


def layout(model):
    """uv_key -> (w, h, d) and packed (u, v)."""
    sizes = {}
    for _, _, _, _, boxes in model["parts"]:
        for *_, w, h, d, key in boxes:
            size = (int(w), int(h), int(d))
            if key in sizes and sizes[key] != size:
                raise RuntimeError(f"uv key {key} used with two sizes")
            sizes[key] = size
    requests = [(k, *uv_size(*s)) for k, s in sizes.items()]
    return sizes, pack(requests, *model["size"])


def paint_model(name, materials, decorations=None, glow_decorations=False, seed=0):
    """Paints every uv region of a model with its material, then its decorations."""
    model = MODELS[name]
    sizes, uvs = layout(model)
    img = Image.new("RGBA", model["size"], (0, 0, 0, 0))
    glow = Image.new("RGBA", model["size"], (0, 0, 0, 0)) if glow_decorations else None
    rng = random.Random(seed)
    for key, (w, h, d) in sizes.items():
        u, v = uvs[key]
        mat = materials.get(key, materials.get("*"))
        for fname, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    img.putpixel((x, y), rgba(mat(x, y, rng)))
        if decorations and key in decorations:
            decorations[key](img, box_faces(u, v, w, h, d), rng, glow)
    return img, glow, uvs


# =============================================================================== Java model code

def fmt(v):
    v = float(v)
    return f"{v:g}f" if v != int(v) else f"{int(v)}.0f"


def java_layer(name, uvs):
    model = MODELS[name]
    lines = [f"    public static LayerDefinition {name}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    var = {None: "root"}
    for pname, parent, pivot, rot, boxes in model["parts"]:
        cubes = "CubeListBuilder.create()"
        for x, y, z, w, h, d, key in boxes:
            u, v = uvs[key]
            cubes += f".texOffs({u}, {v}).addBox({fmt(x)}, {fmt(y)}, {fmt(z)}, {fmt(w)}, {fmt(h)}, {fmt(d)})"
        pose = f"PartPose.offsetAndRotation({', '.join(fmt(p) for p in pivot)}, {', '.join(fmt(r) for r in rot)})"
        v_name = "p_" + pname
        var[pname] = v_name
        lines.append(f"        PartDefinition {v_name} = {var[parent]}.addOrReplaceChild(\"{pname}\", {cubes}, {pose});")
    w, h = model["size"]
    lines.append(f"        return LayerDefinition.create(mesh, {w}, {h});")
    lines.append("    }")
    return "\n".join(lines)


def write_java(layers):
    body = "\n\n".join(layers)
    JAVA.write_text(f"""package com.rickandmorty.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry of the Cronenberg World creatures, the mutation growths and the Council's hats.
 * GENERATED by tools/generate_cronenberg_art.py from tools/cronenberg_models.py (the same run
 * paints the matching textures), so edit the shapes there, not here.
 */
@SuppressWarnings("unused")
public final class CronenbergLayers {{
    private CronenbergLayers() {{
    }}

{body}
}}
""", encoding="utf-8")


# =============================================================================== entity textures

def entity_textures():
    layers = []

    # --- Crawler: flesh body, a stretched human face, arms for legs
    def crawler_face(img, faces, rng, glow):
        face(img, faces["front"], rng, glow, wide=True)

    def jaw_teeth(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        teeth_row(img, x, y, w, rng)
        x, y, w, h = faces["top"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                put(img, xx, yy, (60, 6, 10))
        teeth_row(img, x, y + h - 1, w, rng)

    def palm(img, faces, rng, glow):
        x, y, w, h = faces["bottom"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                put(img, xx, yy, shade(SKIN, 0.85))

    skin = mat_skin(31)
    img, glow, uvs = paint_model("crawler", {"*": skin, "body": mat_flesh(32), "tumor": mat_tumor(33), "face": skin,
                                             "jaw": mat_muscle(34)},
                                 {"face": crawler_face, "jaw": jaw_teeth, "palm": palm}, True, 35)
    img.save(TEX / "entity/cronenberg_crawler.png")
    glow.save(TEX / "entity/cronenberg_crawler_eyes.png")
    layers.append(java_layer("crawler", uvs))

    # --- Stalker
    def stalker_head(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        eye(img, x + 1, y + 2, rng, glow=glow)                  # one eye, the other side is all mouth
        for yy in range(y + 1, y + h - 1):                       # a vertical mouth splitting the face
            put(img, x + w - 2, yy, (40, 4, 8))
            put(img, x + w - 3, yy, (236, 224, 190) if yy % 2 else (90, 10, 14))
            put(img, x + w - 1, yy, (236, 224, 190) if yy % 2 == 0 else (90, 10, 14))
            if glow is not None:
                put(glow, x + w - 2, yy, (110, 10, 10))

    def small_head(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        put(img, x + 1, y + 1, (40, 20, 20))
        put(img, x + 2, y + 1, (40, 20, 20))
        mouth(img, x + 1, y + 2, 2, 2, rng)

    def claws(img, faces, rng, glow):
        for f in faces.values():
            x, y, w, h = f
            for xx in range(x, x + w):
                put(img, xx, y + h - 1, (40, 30, 20))
                if h > 3:
                    put(img, xx, y + h - 2, (90, 70, 40))

    img, glow, uvs = paint_model("stalker", {"*": mat_skin(41, (206, 150, 138)), "torso": mat_flesh(42), "big_arm": mat_muscle(43),
                                             "big_fore": mat_muscle(44), "claw": mat_bone(45), "tentacle": mat_flesh(46, (170, 70, 90)),
                                             "tip": mat_flesh(47, (150, 50, 80)), "thin_arm": mat_skin(48, (190, 150, 140))},
                                 {"head": stalker_head, "small_head": small_head, "claw": claws}, True, 49)
    img.save(TEX / "entity/cronenberg_stalker.png")
    glow.save(TEX / "entity/cronenberg_stalker_eyes.png")
    layers.append(java_layer("stalker", uvs))

    # --- Blob
    def blob_body(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        mouth(img, x + 3, y + 10, 7, 3, rng, glow)
        mouth(img, x + 13, y + 7, 5, 2, rng, glow)
        for fname in ("left", "right", "back"):
            fx, fy, fw, fh = faces[fname]
            mouth(img, fx + fw // 3, fy + fh // 2, max(3, fw // 3), 2, rng, glow)

    def blob_eye(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        eye(img, x + 1, y + 1, rng, glow=glow, r=1)

    def side_eye(img, faces, rng, glow):
        for fname in ("left", "right"):
            x, y, w, h = faces[fname]
            eye(img, x + 1, y + 1, rng, iris=(220, 180, 30), glow=glow, r=1)

    def blob_jaw(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        teeth_row(img, x, y, w, rng)

    img, glow, uvs = paint_model("blob", {"*": mat_flesh(51, (206, 120, 118)), "lump": mat_tumor(52), "lump2": mat_tumor(53),
                                          "eye": mat_flesh(54), "side_eye": mat_flesh(55), "jaw": mat_muscle(56),
                                          "leg": mat_skin(57), "tentacle": mat_flesh(58, (180, 80, 110)), "tip": mat_flesh(59, (160, 60, 100))},
                                 {"body": blob_body, "eye": blob_eye, "side_eye": side_eye, "jaw": blob_jaw}, True, 60)
    img.save(TEX / "entity/cronenberg_blob.png")
    glow.save(TEX / "entity/cronenberg_blob_eyes.png")
    layers.append(java_layer("blob", uvs))

    # --- Behemoth
    def beh_head(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        mouth(img, x + 1, y + 4, w - 2, h - 4, rng, glow)       # the maw
        put(img, x + w // 2 - 1, y + 2, (50, 10, 10))
        put(img, x + w // 2 + 1, y + 2, (50, 10, 10))

    def beh_eye(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                put(img, xx, yy, (238, 220, 200))
        put(img, x, y + 1, (200, 20, 10))
        put(img, x + 1, y, (10, 5, 5))
        if glow is not None:
            put(glow, x, y + 1, (255, 50, 30))
            put(glow, x + 1, y, (255, 50, 30))

    def beh_side_face(img, faces, rng, glow):
        for fname in ("left", "right"):
            face(img, faces[fname], rng, glow)

    def beh_jaw(img, faces, rng, glow):
        x, y, w, h = faces["top"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                put(img, xx, yy, (60, 6, 10))
        teeth_row(img, x, y, w, rng)
        teeth_row(img, x, y + h - 1, w, rng)
        x, y, w, h = faces["front"]
        teeth_row(img, x, y, w, rng)

    img, glow, uvs = paint_model("behemoth", {"*": mat_flesh(61, (186, 88, 90)), "hump": mat_tumor(62), "head": mat_skin(63, (214, 160, 146)),
                                              "jaw": mat_muscle(64), "side_face": mat_skin(65), "leg": mat_skin(66, (200, 150, 140)),
                                              "arm": mat_skin(67), "fore": mat_skin(68), "hand": mat_skin(69), "eye": mat_flesh(70),
                                              "tentacle": mat_flesh(71, (170, 70, 100)), "tip": mat_flesh(72, (150, 50, 90))},
                                 {"head": beh_head, "eye": beh_eye, "side_face": beh_side_face, "jaw": beh_jaw}, True, 73)
    img.save(TEX / "entity/behemoth.png")
    glow.save(TEX / "entity/behemoth_eyes.png")
    layers.append(java_layer("behemoth", uvs))

    # --- Mantis Person: a mantis head and scythes on a body still wearing its clothes
    def mantis_eye(img, faces, rng, glow):
        for f in faces.values():
            x, y, w, h = f
            for yy in range(y, y + h):
                for xx in range(x, x + w):
                    c = (190, 220, 80) if (xx + yy) % 2 else (150, 190, 50)
                    put(img, xx, yy, c)
                    if glow is not None:
                        put(glow, xx, yy, (60, 90, 20))
        x, y, w, h = faces["front"]
        put(img, x + w // 2, y + h // 2, (20, 30, 10))           # the "pupil" that seems to follow you

    def mantis_chin(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        put(img, x + 1, y + h - 1, (40, 10, 10))

    def scythe(img, faces, rng, glow):
        for fname in ("front", "back"):
            x, y, w, h = faces[fname]
            for yy in range(y, y + h, 2):
                put(img, x, yy, (230, 236, 170))                 # serrated edge
        x, y, w, h = faces["top"]
        for xx in range(x, x + w):
            put(img, xx, y, (230, 236, 170))

    def shirt(img, faces, rng, glow):
        x, y, w, h = faces["front"]
        for yy in range(y + 6, y + 9):                           # torn, with a slash of blood
            put(img, x + 2 + (yy - y - 6), yy, BLOOD)
            put(img, x + 3 + (yy - y - 6), yy, (150, 20, 24))

    shirt_color = (70, 110, 170)
    img, glow, uvs = paint_model("mantis", {"*": mat_chitin(81), "body": mat_cloth(shirt_color, 82, stripes=True),
                                            "leg": mat_cloth((60, 70, 110), 83), "eye": mat_chitin(84),
                                            "scythe": mat_chitin(85), "mandible": mat_solid((70, 60, 30))},
                                 {"eye": mantis_eye, "chin": mantis_chin, "scythe": scythe, "body": shirt}, True, 86)
    img.save(TEX / "entity/mantis_person.png")
    glow.save(TEX / "entity/mantis_person_eyes.png")
    layers.append(java_layer("mantis", uvs))

    # --- mutation growths on players
    def growth_eye(img, faces, rng, glow):
        x, y, w, h = faces["left"]
        eye(img, x + 1, y + 1, rng, r=1)                           # a bulging, bloodshot extra eye

    img, _, uvs = paint_model("mutation", {"*": mat_tumor(91), "tumor": mat_flesh(92), "arm": mat_flesh(93), "lump2": mat_flesh(98),
                                           "spike": mat_bone(94), "spike2": mat_bone(95), "eye": mat_flesh(96)},
                              {"eye": growth_eye}, False, 97)
    img.save(TEX / "entity/mutation.png")
    layers.append(java_layer("mutation", uvs))

    # --- the Council's hats
    def band(color):
        def deco(img, faces, rng, glow):
            for fname in ("front", "back", "left", "right"):
                x, y, w, h = faces[fname]
                for xx in range(x, x + w):
                    put(img, xx, y + h - 2 if h > 2 else y, color)
        return deco

    def stars(img, faces, rng, glow):
        for f in faces.values():
            x, y, w, h = f
            for _ in range(max(1, w * h // 10)):
                put(img, x + rng.randrange(w), y + rng.randrange(h), (250, 230, 110))

    def crown_points(img, faces, rng, glow):
        for fname in ("front", "back", "left", "right"):
            x, y, w, h = faces[fname]
            for xx in range(x, x + w):
                if (xx - x) % 2 == 1:
                    put(img, xx, y, (0, 0, 0))
                    img.putpixel((xx, y), (0, 0, 0, 0))          # the crown's points are cut out
            put(img, x + w // 2, y + 1, (200, 30, 60))            # a jewel
        x, y, w, h = faces["top"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                img.putpixel((xx, yy), (0, 0, 0, 0))
        x, y, w, h = faces["bottom"]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                img.putpixel((xx, yy), (0, 0, 0, 0))

    def cone_stripes(img, faces, rng, glow):
        for fname in ("front", "back", "left", "right"):
            x, y, w, h = faces[fname]
            for xx in range(x, x + w):
                put(img, xx, y + h // 2, (240, 240, 240))

    img, _, uvs = paint_model("hats", {
        "top_brim": mat_solid((28, 26, 30)), "top_crown": mat_solid((34, 32, 38)),
        "wiz_brim": mat_solid((70, 40, 140)), "wiz1": mat_solid((84, 50, 160)), "wiz2": mat_solid((90, 56, 170)),
        "wiz3": mat_solid((96, 60, 176)), "wiz4": mat_solid((104, 66, 184)),
        "cow_brim": mat_solid((120, 80, 44)), "cow_crown": mat_solid((132, 90, 50)),
        "crown": mat_solid((236, 190, 50), 10),
        "beanie": mat_cloth((190, 40, 50), 101, stripes=True), "pompom": mat_solid((240, 240, 240)),
        "cap": mat_solid((50, 110, 220)), "stick": mat_solid((200, 200, 200)), "blade": mat_solid((230, 50, 50)),
        "blade2": mat_solid((250, 210, 40)),
        "cone_base": mat_solid((250, 110, 20)), "cone1": mat_solid((250, 120, 24)), "cone2": mat_solid((250, 120, 24)),
        "cone3": mat_solid((250, 120, 24)),
    }, {"top_crown": band((150, 20, 30)), "wiz1": stars, "wiz2": stars, "wiz3": stars, "cow_crown": band((60, 40, 20)),
        "crown": crown_points, "cone1": cone_stripes, "cone2": cone_stripes}, False, 102)
    img.save(TEX / "entity/council_hats.png")
    layers.append(java_layer("hats", uvs))
    write_java(layers)


# =============================================================================== humanoid skins

SKIN_PARTS = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "right_leg": (0, 16, 4, 12, 4), "right_pants": (0, 32, 4, 12, 4),
    "left_leg": (16, 48, 4, 12, 4), "left_pants": (0, 48, 4, 12, 4),
}


def arm_parts(slim):
    w = 3 if slim else 4
    return {"right_arm": (40, 16, w, 12, 4), "right_sleeve": (40, 32, w, 12, 4),
            "left_arm": (32, 48, w, 12, 4), "left_sleeve": (48, 48, w, 12, 4)}


def skin(slim, paint_part, seed):
    """64x64 player-layout skin; paint_part(name, face, fx, fy, local x, local y, w, h, rng) -> RGBA or None."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    parts = dict(SKIN_PARTS, **arm_parts(slim))
    for name, (u, v, w, h, d) in parts.items():
        for fname, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    c = paint_part(name, fname, x - fx, y - fy, fw, fh, rng)
                    if c is not None:
                        img.putpixel((x, y), c if len(c) == 4 else rgba(c))
    return img


def humanoid(skin_tone, hair, shirt, pants, shoes, face_fn=None, hair_len=2, jacket=None, mutated=False, seed=1, extra=None):
    flesh = mat_flesh(seed + 7)
    blotch = Noise(seed + 3, 3)

    def paint(name, fname, x, y, w, h, rng):
        base = name.replace("_sleeve", "_arm").replace("_pants", "_leg")
        overlay = name in ("hat", "jacket", "right_sleeve", "left_sleeve", "right_pants", "left_pants")
        if extra:
            c = extra(name, fname, x, y, w, h, rng)
            if c is not None:
                return c
        if overlay:
            if name == "hat" and fname in ("top", "front", "left", "right", "back") and hair is not None:
                if fname == "top" or y < hair_len - 1:
                    return rgba(mix(hair, shade(hair, 0.7), blotch(x, y) + rng.uniform(-0.1, 0.1)))
            if name == "jacket" and jacket is not None:
                if fname == "front" and 3 <= x <= 4:
                    return None                                  # open coat
                return rgba(mat_cloth(jacket, seed + 20)(x, y, rng))
            if name in ("right_sleeve", "left_sleeve") and jacket is not None and y < 11:
                return rgba(mat_cloth(jacket, seed + 21)(x, y, rng))
            if mutated and name == "jacket" and fname == "front" and 2 <= x <= 5 and 4 <= y <= 7:
                return rgba(flesh(x, y, rng))                    # something bursting out of the chest
            return None
        if base == "head":
            if fname == "top" and hair is not None:
                return rgba(mix(hair, shade(hair, 0.7), blotch(x, y)))
            if fname in ("left", "right", "back") and hair is not None and (y < hair_len or fname == "back" and y < 6):
                return rgba(mix(hair, shade(hair, 0.7), blotch(x, y)))
            if fname == "front" and hair is not None and y < 1:
                return rgba(hair)
            c = mat_skin(seed, skin_tone)(x, y, rng) if not mutated else mix(skin_tone, FLESH, blotch(x, y) * 0.6)
            if fname == "front" and face_fn:
                f = face_fn(x, y)
                if f is not None:
                    return rgba(f)
            return rgba(c)
        if base == "body":
            return rgba(mat_cloth(shirt, seed + 30)(x, y, rng)) if y < 10 else rgba(shade(pants, 0.8))
        if base.endswith("arm"):
            if y < 4:
                return rgba(mat_cloth(shirt, seed + 31)(x, y, rng))
            return rgba(mat_skin(seed + 32, skin_tone)(x, y, rng) if not mutated else mix(skin_tone, FLESH, blotch(x + 9, y)))
        if base.endswith("leg"):
            if y >= 10:
                return rgba(shoes)
            return rgba(mat_cloth(pants, seed + 33)(x, y, rng))
        return None
    return paint


def simple_face(eyes=(40, 30, 30), brows=None, mouth_color=(150, 70, 70), wide_mouth=False, stubble=None, extra_eye=False):
    def f(x, y):
        if y == 3 and x in (1, 2, 5, 6):
            return (240, 240, 240) if x in (1, 5) else eyes
        if brows and y == 2 and x in (1, 2, 5, 6):
            return brows
        if extra_eye and y in (5, 6) and x == 7:
            return (240, 230, 220) if y == 5 else (200, 20, 20)
        if y == 6 and ((2 <= x <= 5) if wide_mouth else (3 <= x <= 4)):
            return mouth_color
        if stubble and y >= 5 and x in (1, 2, 5, 6) and (x + y) % 2 == 0:
            return stubble
        return None
    return f


def survivor_skins():
    light = (230, 188, 160)
    jerry = skin(False, humanoid(light, (110, 76, 48), (80, 150, 140), (104, 92, 60), (60, 44, 30),
                                 simple_face(brows=(90, 60, 40), stubble=(170, 130, 110)), jacket=(110, 70, 40), seed=201), 201)
    beth = skin(True, humanoid(light, (226, 170, 90), (170, 40, 40), (50, 60, 90), (40, 30, 30),
                               simple_face(brows=(180, 120, 60), mouth_color=(190, 80, 90)), hair_len=5, seed=202), 202)
    summer = skin(True, humanoid(light, (220, 90, 40), (230, 110, 150), (90, 120, 180), (230, 230, 230),
                                 simple_face(brows=(190, 70, 30), mouth_color=(200, 80, 100)), hair_len=6, seed=203), 203)

    def rick_extra(name, fname, x, y, w, h, rng):
        # Rick's spiky blue-grey hair sticks out of the hat layer on top and at the sides
        if name == "hat" and fname in ("left", "right", "back") and y < 4 and (x + y) % 2 == 0:
            return rgba((150, 190, 205))
        if name == "head" and fname == "front" and y == 1 and x in (0, 1, 6, 7):
            return rgba((170, 200, 210))                          # the unibrow's spiky ends
        if name == "head" and fname == "front" and y == 2 and 1 <= x <= 6:
            return rgba((140, 170, 190))                          # the unibrow
        return None

    rick = skin(False, humanoid((210, 190, 170), (150, 190, 205), (150, 200, 220), (120, 90, 60), (50, 40, 30),
                                simple_face(eyes=(20, 20, 20), mouth_color=(120, 60, 60), wide_mouth=True, extra_eye=True),
                                jacket=(236, 236, 230), mutated=True, seed=204, extra=rick_extra), 204)
    morty = skin(False, humanoid((236, 196, 160), (110, 70, 40), (240, 210, 60), (60, 90, 170), (230, 230, 230),
                                 simple_face(eyes=(20, 20, 20), mouth_color=(160, 80, 80), extra_eye=True), mutated=True, seed=205), 205)
    for name, img in (("jerry", jerry), ("beth", beth), ("summer", summer), ("cronenberg_rick", rick), ("cronenberg_morty", morty)):
        img.save(TEX / f"entity/{name}.png")


# =============================================================================== the blender

def blender_textures():
    rng = random.Random(501)
    arm = Image.new("RGBA", (32, 32))
    metal = mat_metal(502)
    for y in range(32):
        for x in range(32):
            c = metal(x, y, rng)
            if y % 8 in (0, 1):
                c = (60, 64, 70)                                  # joints between the segments
            if y % 8 == 4 and x % 8 == 3:
                c = (210, 214, 220)                               # rivets
            if 24 <= y < 28:
                c = (230, 190, 30) if (x + y) % 6 < 3 else (30, 30, 30)   # hazard stripes
            arm.putpixel((x, y), rgba(c))
    arm.save(TEX / "entity/blender_arm.png")
    saw = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for y in range(64):
        for x in range(64):
            nx, ny = (x + 0.5) / 32 - 1, (y + 0.5) / 32 - 1
            r = math.hypot(nx, ny)
            theta = math.atan2(ny, nx)
            tooth = 0.9 + 0.08 * ((theta * 24 / (2 * math.pi)) % 1.0)   # a sawtooth rim
            if r > tooth:
                continue
            if r < 0.14:
                c = (50, 52, 56)                                  # the hub
            elif r < 0.2:
                c = (200, 204, 210)
            else:
                c = mix((200, 206, 212), (130, 136, 142), r)
                if int(theta * 8 / math.pi + r * 6) % 4 == 0:
                    c = shade(c, 0.85)                            # machining marks
                if r > 0.8 and rng.random() < 0.25:
                    c = mix(c, BLOOD, 0.8)                        # never quite clean
            saw.putpixel((x, y), rgba(c))
    saw.save(TEX / "entity/blender_saw.png")


# =============================================================================== blocks

def block(name, fn, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = fn(x, y, rng)
            img.putpixel((x, y), c if len(c) == 4 else rgba(c))
    img.save(TEX / f"block/{name}.png")
    return img


def tileable(noise_fn):
    """Samples noise on a torus so a 16x16 block texture tiles without seams."""
    def f(x, y):
        a, b = x / 16 * math.tau, y / 16 * math.tau
        return noise_fn(8 + 4 * math.cos(a), 8 + 4 * math.sin(a) + 20 * math.cos(b) + 3 * math.sin(b))
    return f


def block_textures():
    n1, n2, n3 = Noise(301, 1.6), Noise(302, 1.2), Noise(303, 1.0)
    t1, t2, t3 = tileable(n1), tileable(n2), tileable(n3)

    def flesh(x, y, rng):
        c = mix((200, 96, 100), (130, 40, 50), t1(x, y) + rng.uniform(-0.1, 0.1))
        if abs(t2(x, y) - 0.5) < 0.05:
            c = (110, 26, 36)
        return c

    def veined(x, y, rng):
        c = mix((176, 64, 72), (118, 30, 42), t1(x, y) * 0.9 + rng.uniform(-0.05, 0.05))
        if abs(t2(x, y) - 0.5) < 0.06:
            c = (74, 8, 22)                                       # thick dark veins
        elif abs(t3(x, y) - 0.5) < 0.035:
            c = (96, 30, 70)                                      # thinner purple ones
        return c

    def rotting(x, y, rng):
        c = mix((130, 70, 50), (80, 44, 32), t1(x, y) + rng.uniform(-0.1, 0.1))
        if rng.random() < 0.05:
            c = (150, 150, 80)
        return c

    def necrotic(x, y, rng):
        c = mix((80, 40, 60), (40, 18, 30), t1(x, y) + rng.uniform(-0.1, 0.1))
        if abs(t2(x, y) - 0.5) < 0.04:
            c = (20, 8, 14)
        return c

    def tumor(x, y, rng):
        lumps = [(4, 4), (11, 5), (6, 11), (13, 12), (1, 9)]
        d = min(min(math.hypot(x - a, y - b), math.hypot(x - a - 16, y - b), math.hypot(x - a, y - b - 16)) for a, b in lumps)
        c = mix((208, 128, 116), (132, 50, 56), d / 5 + rng.uniform(-0.06, 0.06))   # swollen lumps, darker between
        if d < 1.0:
            c = (232, 206, 120)                                   # a yellow head of pus on each
        elif abs(t2(x, y) - 0.5) < 0.04:
            c = (100, 20, 34)
        return c

    def watching(x, y, rng):
        c = flesh(x, y, rng)
        for ex, ey in ((4, 5), (11, 4), (8, 11), (13, 12), (2, 12)):
            dx, dy = x - ex, y - ey
            if abs(dx) <= 1 and abs(dy) <= 1:
                c = (236, 226, 214)
                if dx in (0, 1) and dy == 0:
                    c = (190, 20, 20)
                if dx == 0 and dy == 0:
                    c = (10, 5, 5)
        return c

    def clot(x, y, rng):
        c = mix((120, 8, 16), (60, 0, 6), t1(x, y) + rng.uniform(-0.1, 0.1))
        if rng.random() < 0.04:
            c = (170, 30, 40)                                     # a glossy sheen
        return c

    def bone_side(x, y, rng):
        c = mix((226, 214, 186), (190, 170, 130), t2(x, y) * 0.8 + rng.uniform(-0.06, 0.06))
        if x in (0, 15):
            c = (170, 150, 110)
        if abs(t3(x, y) - 0.5) < 0.03:
            c = (130, 110, 80)                                    # cracks
        return c

    def bone_top(x, y, rng):
        r = math.hypot(x - 7.5, y - 7.5)
        c = mix((226, 214, 186), (150, 120, 90), r / 9)
        if r < 3.2:
            c = mix((150, 60, 60), (90, 20, 30), rng.random())    # marrow
        return c

    for name, fn, seed in (("flesh", flesh, 1), ("veined_flesh", veined, 2), ("rotting_flesh", rotting, 3),
                           ("necrotic_flesh", necrotic, 4), ("tumor", tumor, 5), ("watching_flesh", watching, 6),
                           ("blood_clot", clot, 7), ("calcified_bone", bone_side, 8), ("calcified_bone_top", bone_top, 9)):
        block(name, fn, seed)

    def pustule(x, y, rng):
        c = mix((236, 210, 120), (190, 120, 70), math.hypot(x - 8, y - 8) / 9 + rng.uniform(-0.1, 0.1))
        if rng.random() < 0.05:
            c = (250, 250, 190)
        return c

    def tooth(x, y, rng):
        c = mix((240, 234, 214), (200, 190, 160), y / 16 + rng.uniform(-0.05, 0.05))
        if y > 13:
            c = mix((150, 60, 60), (100, 20, 30), rng.random())   # the bloody gum it grows from
        return c

    def stalk(x, y, rng):
        c = mix((200, 110, 110), (140, 50, 60), rng.random() * 0.6)
        return c

    def stalk_eye(x, y, rng):
        c = (236, 226, 214)
        if 5 <= x <= 10 and 5 <= y <= 10:
            c = (60, 150, 60) if math.hypot(x - 7.5, y - 7.5) > 1.5 else (10, 5, 5)
        return c

    block("pustule", pustule, 10)
    block("tooth_spike", tooth, 11)
    block("eyestalk", stalk, 12)
    block("eyestalk_eye", stalk_eye, 13)

    def grass(x, y, rng):
        # sinewy tufts: thin red strands, transparent between them
        strand = (x * 7 + 3) % 5 == 0 and y > (x * 5) % 7 + 2
        return rgba(mix((190, 50, 60), (110, 20, 30), y / 16)) if strand else (0, 0, 0, 0)
    block("flesh_grass", grass, 14)


def block_models():
    def write(path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2), encoding="utf-8")

    cubes = ["flesh", "veined_flesh", "rotting_flesh", "necrotic_flesh", "tumor", "watching_flesh", "blood_clot"]
    for name in cubes:
        write(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"rickrooms:block/{name}"}})
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"rickrooms:block/{name}"}}})
        write(ASSETS / f"models/item/{name}.json", {"parent": f"rickrooms:block/{name}"})
    write(ASSETS / "models/block/calcified_bone.json", {"parent": "minecraft:block/cube_column",
                                                         "textures": {"end": "rickrooms:block/calcified_bone_top", "side": "rickrooms:block/calcified_bone"}})
    write(ASSETS / "models/block/calcified_bone_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                                    "textures": {"end": "rickrooms:block/calcified_bone_top", "side": "rickrooms:block/calcified_bone"}})
    write(ASSETS / "blockstates/calcified_bone.json", {"variants": {
        "axis=y": {"model": "rickrooms:block/calcified_bone"},
        "axis=z": {"model": "rickrooms:block/calcified_bone_horizontal", "x": 90},
        "axis=x": {"model": "rickrooms:block/calcified_bone_horizontal", "x": 90, "y": 90}}})
    write(ASSETS / "models/item/calcified_bone.json", {"parent": "rickrooms:block/calcified_bone"})

    def element(frm, to, tex):
        return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}}

    write(ASSETS / "models/block/pustule.json", {"parent": "minecraft:block/block", "textures": {"all": "rickrooms:block/pustule",
                                                                                               "particle": "rickrooms:block/pustule"},
                                                 "elements": [element([4, 0, 4], [12, 6, 12], "#all"), element([5, 6, 5], [11, 8, 11], "#all"),
                                                              element([9, 0, 2], [13, 3, 6], "#all")]})
    write(ASSETS / "models/block/tooth_spike.json", {"parent": "minecraft:block/block", "textures": {"all": "rickrooms:block/tooth_spike",
                                                                                                   "particle": "rickrooms:block/tooth_spike"},
                                                     "elements": [element([4, 0, 4], [12, 4, 12], "#all"), element([5, 4, 5], [11, 9, 11], "#all"),
                                                                  element([6, 9, 6], [10, 12, 10], "#all"), element([7, 12, 7], [9, 14, 9], "#all")]})
    write(ASSETS / "models/block/eyestalk.json", {"parent": "minecraft:block/block", "textures": {"stalk": "rickrooms:block/eyestalk",
                                                                                                "eye": "rickrooms:block/eyestalk_eye",
                                                                                                "particle": "rickrooms:block/eyestalk"},
                                                  "elements": [element([7, 0, 7], [9, 10, 9], "#stalk"), element([6, 3, 7], [7, 7, 8], "#stalk"),
                                                               dict(element([5.5, 10, 5.5], [10.5, 15, 10.5], "#eye"), shade=False)]})
    write(ASSETS / "models/block/flesh_grass.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                     "textures": {"cross": "rickrooms:block/flesh_grass"}})
    for name in ("pustule", "tooth_spike", "eyestalk", "flesh_grass"):
        write(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"rickrooms:block/{name}"}}})
    write(ASSETS / "models/item/pustule.json", {"parent": "rickrooms:block/pustule"})
    write(ASSETS / "models/item/tooth_spike.json", {"parent": "rickrooms:block/tooth_spike"})
    write(ASSETS / "models/item/eyestalk.json", {"parent": "rickrooms:block/eyestalk"})
    write(ASSETS / "models/item/flesh_grass.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "rickrooms:block/flesh_grass"}})


# =============================================================================== items

def sprite(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), rgba(palette[ch]))
    return img


def item_textures():
    items = {}
    items["cronenberg_flesh"] = sprite([
        "................",
        "................",
        "......kkkk......",
        "....kkRRRRkk....",
        "...kRRrRRRRRk...",
        "..kRRRRRrRRRRk..",
        "..kRrRRRRRRwRk..",
        ".kRRRRRRrRRRRRk.",
        ".kRRwRRRRRRRrRk.",
        ".kRRRRRrRRRRRRk.",
        "..kRRRRRRRwRRk..",
        "..kkRRrRRRRRkk..",
        "....kkRRRRkk....",
        "......kkkk......",
        "................",
        "................"], {"k": (70, 10, 16), "R": (190, 60, 66), "r": (120, 20, 34), "w": (236, 200, 170)})
    items["cooked_cronenberg_flesh"] = sprite([
        "................",
        "................",
        "......kkkk......",
        "....kkBBBBkk....",
        "...kBBbBBBBBk...",
        "..kBBBBBbBBBBk..",
        "..kBbBBBBBBwBk..",
        ".kBBBBBBbBBBBBk.",
        ".kBBwBBBBBBBbBk.",
        ".kBBBBBbBBBBBBk.",
        "..kBBBBBBBwBBk..",
        "..kkBBbBBBBBkk..",
        "....kkBBBBkk....",
        "......kkkk......",
        "................",
        "................"], {"k": (50, 26, 12), "B": (140, 80, 44), "b": (100, 50, 26), "w": (200, 160, 110)})
    items["mutagen_sac"] = sprite([
        "................",
        ".......kk.......",
        "......kGGk......",
        ".....kGggGk.....",
        "....kGgGGgGk....",
        "...kGGGyGGGGk...",
        "..kGGgGGGGgGGk..",
        "..kGGGGGyGGGGk..",
        "..kGyGGGGGGGGk..",
        "..kGGGGgGGGyGk..",
        "...kGGGGGGGGk...",
        "....kGGyGGGk....",
        ".....kkGGkk.....",
        ".......kk.......",
        "................",
        "................"], {"k": (40, 60, 20), "G": (150, 210, 60), "g": (110, 160, 40), "y": (240, 250, 150)})
    items["bone_plate"] = sprite([
        "................",
        "................",
        "...kkkkkkkkkk...",
        "..kWWWWWWWWWWk..",
        "..kWwWWWWWWwWk..",
        "..kWWWWcWWWWWk..",
        "..kWWWWcWWWWWk..",
        "..kWwWWWcWWWWk..",
        "..kWWWWWcWWwWk..",
        "..kWWWWWWWWWWk..",
        "..kWwWWWWWWWWk..",
        "..kkWWWWWWWWkk..",
        "....kkkkkkkk....",
        "................",
        "................",
        "................"], {"k": (110, 90, 60), "W": (226, 214, 186), "w": (190, 170, 130), "c": (140, 110, 80)})
    items["mantis_scythe"] = sprite([
        "................",
        "..........ggg...",
        ".........gGGgk..",
        "........gGGGk...",
        ".......gGGGk....",
        "......gGGGk.....",
        ".....gGGGk......",
        "....gGGGk.......",
        "...kGGGk........",
        "..bkGGk.........",
        ".bBbkk..........",
        "bBb.............",
        "Bb..............",
        "b...............",
        "................",
        "................"], {"k": (40, 60, 20), "G": (130, 170, 60), "g": (230, 236, 170), "b": (70, 50, 30), "B": (110, 80, 50)})
    items["cure_serum"] = sprite([
        "................",
        ".......kk.......",
        "......kwwk......",
        "......kwwk......",
        "......kkkk......",
        ".....kCCCCk.....",
        ".....kCcCCk.....",
        ".....kCCCCk.....",
        ".....kCCcCk.....",
        ".....kCCCCk.....",
        ".....kcCCCk.....",
        ".....kCCCCk.....",
        ".....kCCCCk.....",
        "......kkkk......",
        "................",
        "................"], {"k": (30, 40, 50), "w": (200, 200, 210), "C": (80, 220, 200), "c": (220, 255, 250)})
    items["behemoth_heart"] = sprite([
        "................",
        "....kk....kk....",
        "...kRRk..kRRk...",
        "..kRRRRkkRRRRk..",
        "..kRrRRRRRRvRk..",
        "..kRRRvRRRRRRk..",
        "..kRRRRRrRRRRk..",
        "...kRRRRRRvRk...",
        "...kRvRRRRRRk...",
        "....kRRRRrRk....",
        ".....kRRRRk.....",
        "......kRRk......",
        ".......kk.......",
        "................",
        "................",
        "................"], {"k": (60, 6, 12), "R": (180, 30, 40), "r": (230, 90, 90), "v": (90, 30, 90)})
    items["blender_chip"] = sprite([
        "................",
        "..kkkkkkkkkkkk..",
        "..kGGGGGGGGGGk..",
        "..kGyGkkkkGyGk..",
        "..kGGkSSSSkGGk..",
        "..kGkSSssSSkGk..",
        "..kGkSsRRsSkGk..",
        "..kGkSsRRsSkGk..",
        "..kGkSSssSSkGk..",
        "..kGGkSSSSkGGk..",
        "..kGyGkkkkGyGk..",
        "..kGGGGGGGGGGk..",
        "..kkkkkkkkkkkk..",
        "...y..y..y..y...",
        "................",
        "................"], {"k": (30, 30, 34), "G": (40, 110, 60), "y": (230, 190, 60), "S": (200, 206, 212), "s": (140, 146, 152), "R": (200, 20, 20)})

    def note(page):
        rng = random.Random(400 + page)
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(2, 15):
            for x in range(2, 14):
                if (page == 1 and x + y > 25) or (page == 2 and y < 4 and x > 9) or (page == 3 and x < 4 and y > 11):
                    continue                                       # torn corners
                img.putpixel((x, y), rgba((226, 226, 214)))
        for y in range(4, 14, 2):                                  # formulas in blue ink
            for x in range(3, 13):
                if rng.random() < 0.6:
                    img.putpixel((x, y), rgba((60, 80, 200)))
        for x, y in ((9, 5), (10, 5), (9, 6), (10, 7), (9, 8)):    # a big red "9"
            img.putpixel((x, y), rgba((200, 30, 30)))
        return img

    for page in (1, 2, 3):
        items[f"rick_notes_{page}"] = note(page)
    coords = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 15):
        for x in range(2, 14):
            coords.putpixel((x, y), rgba((236, 220, 220)))
    rng = random.Random(410)
    for y in range(4, 13, 2):
        for x in range(3, 13):
            if rng.random() < 0.7:
                coords.putpixel((x, y), rgba((220, 60, 90)))
    for x in range(10, 14):
        coords.putpixel((x, 2), rgba((150, 20, 40)))
        coords.putpixel((13, x - 8), rgba((150, 20, 40)))
    items["cronenberg_coordinates"] = coords

    def armor_icon(kind, part):
        base = (226, 214, 186) if kind == "bone_plate" else (130, 88, 50)
        dark = (150, 130, 96) if kind == "bone_plate" else (80, 52, 30)
        accent = (120, 30, 40) if kind == "bone_plate" else (160, 164, 170)
        shapes = {
            "helmet": ["................", "................", "....kkkkkkkk....", "...kBBBBBBBBk...", "..kBBaBBBBaBBk..", "..kBBBBBBBBBBk..",
                       "..kBBkkkkkkBBk..", "..kBk......kBk..", "..kBk......kBk..", "...kk......kk...", "................",
                       "................", "................", "................", "................", "................"],
            "chestplate": ["................", "..kkk......kkk..", ".kBBBkkkkkkBBBk.", ".kBBBBBBBBBBBBk.", ".kkkBBaBBaBBkkk.", "...kBBBBBBBBk...",
                           "...kBBaBBaBBk...", "...kBBBBBBBBk...", "...kBBaBBaBBk...", "...kBBBBBBBBk...", "...kkkkkkkkkk...",
                           "................", "................", "................", "................", "................"],
            "leggings": ["................", "................", "...kkkkkkkkkk...", "...kBBBBBBBBk...", "...kBBaBBaBBk...", "...kBBBkkBBBk...",
                         "...kBBk..kBBk...", "...kBak..kaBk...", "...kBBk..kBBk...", "...kBBk..kBBk...", "...kBBk..kBBk...",
                         "...kkkk..kkkk...", "................", "................", "................", "................"],
            "boots": ["................", "................", "................", "................", "................", "................",
                      "...kkk....kkk...", "...kBk....kBk...", "...kBk....kBk...", "..kBak....kaBk..", ".kBBBk....kBBBk.",
                      ".kkkkk....kkkkk.", "................", "................", "................", "................"],
        }
        return sprite(shapes[part], {"k": dark, "B": base, "a": accent})

    for kind in ("bone_plate", "survivor"):
        for part in ("helmet", "chestplate", "leggings", "boots"):
            items[f"{kind}_{part}"] = armor_icon(kind, part)

    for name, img in items.items():
        img.save(TEX / f"item/{name}.png")
        path = ASSETS / f"models/item/{name}.json"
        parent = "minecraft:item/handheld" if name == "mantis_scythe" else "minecraft:item/generated"
        path.write_text(json.dumps({"parent": parent, "textures": {"layer0": f"rickrooms:item/{name}"}}, indent=2), encoding="utf-8")
    eggs = ["cronenberg_crawler", "cronenberg_stalker", "cronenberg_blob", "behemoth", "mantis_person", "jerry", "beth", "summer",
            "cronenberg_rick", "cronenberg_morty", "council_member"]
    for egg in eggs:
        (ASSETS / f"models/item/{egg}_spawn_egg.json").write_text(json.dumps({"parent": "minecraft:item/template_spawn_egg"}, indent=2),
                                                                   encoding="utf-8")


def armor_layers():
    """64x32 armour textures in the vanilla layout (layer 1: helmet, chest, boots; layer 2: leggings)."""
    for kind, base, dark, accent, seed in (("bone_plate", (226, 214, 186), (130, 100, 70), (120, 30, 40), 601),
                                           ("survivor", (130, 88, 50), (70, 46, 26), (160, 164, 170), 602)):
        rng = random.Random(seed)
        n = Noise(seed, 3)
        for layer in (1, 2):
            img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
            regions = [(0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)] if layer == 1 \
                else [(16, 16, 8, 12, 4), (0, 16, 4, 12, 4)]
            for (u, v, w, h, d) in regions:
                for fname, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
                    for y in range(fy, fy + fh):
                        for x in range(fx, fx + fw):
                            ly = y - fy
                            if layer == 1 and u == 0 and v == 16 and ly < 8:
                                continue                           # boots only cover the feet
                            c = mix(base, shade(base, 0.8), n(x, y))
                            if kind == "bone_plate" and (ly % 4 == 0 or (x - fx) % 5 == 0):
                                c = dark                           # sinew binding the plates
                            if kind == "survivor" and rng.random() < 0.06:
                                c = accent                         # scrap metal patches and rivets
                            img.putpixel((x, y), rgba(c))
            if layer == 1 and kind == "survivor":                  # goggles on the helmet
                for x, y in ((9, 11), (10, 11), (13, 11), (14, 11)):
                    img.putpixel((x, y), rgba((120, 200, 220)))
            img.save(TEX / f"models/armor/{kind}_layer_{layer}.png")


# =============================================================================== GUI

def gui_textures():
    icons = Image.new("RGBA", (27, 9), (0, 0, 0, 0))

    def cell(ox, fill):
        for y in range(9):
            for x in range(9):
                r = math.hypot(x - 4, y - 4.3)
                if r > 4.2:
                    continue
                if r > 3.3:
                    icons.putpixel((ox + x, y), (40, 10, 20, 255))
                elif fill == 0 or (fill == 1 and x > 4):
                    icons.putpixel((ox + x, y), (70, 30, 40, 255))
                else:
                    c = (200, 60, 90) if (x + y) % 3 else (240, 120, 150)
                    if abs(x - 4) <= 1 and abs(y - 4) <= 1:
                        c = (120, 200, 60)                         # the mutating nucleus
                    icons.putpixel((ox + x, y), rgba(c))

    cell(0, 2)
    cell(9, 1)
    cell(18, 0)
    icons.save(TEX / "gui/mutation.png")
    vignette = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    n1, n2 = Noise(701, 10), Noise(702, 7)
    for y in range(256):
        for x in range(256):
            nx, ny = x / 128 - 1, y / 128 - 1
            edge = max(0.0, math.hypot(nx, ny) - 0.55) / 0.85
            vein = abs(n1(x, y) - 0.5) < 0.03 + 0.02 * edge or abs(n2(x, y) - 0.5) < 0.02 + 0.015 * edge
            a = min(1.0, edge * 1.4) * (1.0 if vein else 0.35)
            if a > 0.02:
                c = (120, 0, 20) if vein else (80, 0, 10)
                vignette.putpixel((x, y), (*c, int(255 * a)))
    vignette.save(TEX / "gui/mutation_vignette.png")
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y in range(18):
        for x in range(18):
            cx, cy = x - 9, y - 10
            inside = (cy >= 0 and math.hypot(cx, cy) < 6) or (cy < 0 and abs(cx) < (cy + 9) * 0.7)
            if inside:
                icon.putpixel((x, y), rgba((200, 20, 30) if (x - 7) ** 2 + (y - 10) ** 2 > 2 else (255, 120, 120)))
    (TEX / "mob_effect").mkdir(parents=True, exist_ok=True)
    icon.save(TEX / "mob_effect/bleeding.png")


def main():
    for folder in ("entity", "block", "item", "gui", "models/armor", "mob_effect"):
        (TEX / folder).mkdir(parents=True, exist_ok=True)
    entity_textures()
    survivor_skins()
    blender_textures()
    block_textures()
    block_models()
    item_textures()
    armor_layers()
    gui_textures()
    print("Cronenberg art written; model code in", JAVA.relative_to(PROJECT))


if __name__ == "__main__":
    main()
