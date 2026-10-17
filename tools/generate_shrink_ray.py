"""The Shrink Ray's 3D model (rendered by ShrinkRayRenderer): geometry, textures, and the generated
client/model/ShrinkRayLayers.java. Run from the project root:

    python tools/generate_shrink_ray.py

Coordinates are in pixels, y UP, the origin at the middle of the block's floor, the front towards -z.
The ray stands about 3.5 blocks tall; the emitter hangs over the middle of the block in front, its
tip just over 2 blocks up, so someone full size fits underneath. The two arm lengths are made to
reach exactly from joint to joint (the renderer turns them with the angles written here).
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import box_faces, fmt, mix, pack, rgba, shade, uv_size  # noqa: E402

PROJECT = Path(__file__).resolve().parent.parent
TEX = PROJECT / "src/main/resources/assets/rickrooms/textures/entity"
JAVA = PROJECT / "src/main/java/com/rickandmorty/client/model/ShrinkRayLayers.java"

# the joints
BASE_JOINT = (0.0, 5.0, 3.0)
ELBOW = (0.0, 30.0, 11.0)
HEAD_JOINT = (0.0, 49.0, -9.0)
HEAD = -16.0                          # the head is over the middle of the block in front

parts = []                            # (name, pivot, rotation (x, y, z), [boxes (x, y, z, w, h, d, key)])


def octagon(name, x, y, z, w, h, d, key):
    parts.append((name, (0, 0, 0), (0, 0, 0), [(x, y, z, w, h, d, key)]))
    cx, cz = x + w / 2, z + d / 2
    parts.append((name + "_45", (cx, 0, cz), (0, math.pi / 4, 0), [(x - cx, y, z - cz, w, h, d, key)]))


def segment(name, start, end, width, key, offset=(0.0, 0.0)):
    """A straight piece from start to end: a box along +y turned about x."""
    dy, dz = end[1] - start[1], end[2] - start[2]
    length = math.hypot(dy, dz)
    angle = math.atan2(dz, dy)
    ox, oz = offset
    parts.append((name, start, (angle, 0, 0), [(ox - width / 2, 0, oz - width / 2, width, length, width, key)]))


# --- the base: an octagon on four castors, a dark top with a ring of lights
octagon("base", -7.5, 1, -7.5, 15, 3.5, 15, "body")
octagon("top", -5.5, 4.5, -5.5, 11, 0.6, 11, "dark")
for k in range(6):
    a = k * math.pi / 3 + math.pi / 6
    parts.append((f"light{k}", (0, 0, 0), (0, 0, 0), [(6.2 * math.cos(a) - 0.6, 4.4, 6.2 * math.sin(a) - 0.6, 1.2, 1.2, 1.2, "light")]))
for k, (x, z) in enumerate(((-6, -6), (6, -6), (-6, 6), (6, 6))):
    parts.append((f"castor{k}", (0, 0, 0), (0, 0, 0), [(x - 1, 0, z - 1, 2, 1.2, 2, "dark"), (x - 0.5, 1, z - 0.5, 1, 1, 1, "body")]))
parts.append(("socket", (0, 0, 0), (0, 0, 0), [(-1.8, 4.5, 1.2, 3.6, 2, 3.6, "body")]))

# --- the arm: up and back to the elbow, then up and forward to the head; cables alongside
segment("lower_arm", BASE_JOINT, ELBOW, 1.8, "arm")
segment("lower_cable", BASE_JOINT, ELBOW, 0.6, "dark", (1.3, 0.4))
parts.append(("elbow", ELBOW, (0, 0, 0), [(-1.7, -1.7, -1.7, 3.4, 3.4, 3.4, "body")]))
segment("upper_arm", ELBOW, HEAD_JOINT, 1.8, "arm")
segment("upper_cable", ELBOW, HEAD_JOINT, 0.6, "dark", (-1.3, -0.4))
parts.append(("head_joint", HEAD_JOINT, (0, 0, 0), [(-1.6, -1.6, -1.6, 3.2, 3.2, 3.2, "body")]))

# --- the head: the cap, a knob on top, the glowing core in its dark ring, the cone and its rings
octagon("cap", -7, 47, HEAD - 7, 14, 5, 14, "body")
parts.append(("knob", (0, 0, 0), (0, 0, 0), [(-2, 52, HEAD - 2, 4, 2.5, 4, "arm"), (-1, 54.5, HEAD - 1, 2, 1.4, 2, "core")]))
octagon("ring", -5.2, 45.6, HEAD - 5.2, 10.4, 1.4, 10.4, "dark")
octagon("core", -4.4, 41, HEAD - 4.4, 8.8, 4.6, 8.8, "core")
for k, (r, y0, h) in enumerate(((3.2, 37.5, 3.5), (2.2, 35, 2.5), (1.2, 33, 2))):
    parts.append((f"cone{k}", (0, 0, 0), (0, 0, 0), [(-r, y0, HEAD - r, 2 * r, h, 2 * r, "core")]))
for k, (r, y) in enumerate(((3.8, 39.0), (2.8, 36.4))):
    parts.append((f"coil{k}", (0, 0, 0), (0, 0, 0), [(-r, y, HEAD - r, 2 * r, 0.6, 2 * r, "body")]))
parts.append(("tip", (0, 0, 0), (0, 0, 0), [(-0.5, 31.6, HEAD - 0.5, 1, 1.4, 1, "light")]))

# --- the control box with its three knobs, beside the base
parts.append(("console", (0, 0, 0), (0, 0, 0), [(8, 0, 3, 4, 9, 4, "body2")] +
              [(8.6 + i * 1.1, 9, 4.8, 0.4, 2.2, 0.4, "arm") for i in range(3)] +
              [(8.3 + i * 1.1, 11.2, 4.5, 1, 1, 1, "orange") for i in range(3)]))

# --- the beam, shown only while it fires
parts.append(("beam", (0, 0, 0), (0, 0, 0), [(-2, 0, HEAD - 2, 4, 31.6, 4, "beam")]))

GLOW = {"light", "core", "beam"}
COLORS = {"body": (46, 110, 94), "body2": (78, 154, 112), "arm": (156, 214, 202), "dark": (40, 48, 50), "orange": (240, 110, 60),
          "light": (130, 255, 100), "core": (176, 240, 96), "beam": (150, 255, 120)}
GROW = {"core": (255, 176, 80), "beam": (255, 170, 70), "light": (255, 200, 110)}


def layout():
    sizes = {}
    for _, _, _, boxes in parts:
        for *_, w, h, d, key in boxes:
            size = (math.ceil(w), math.ceil(h), math.ceil(d))
            prev = sizes.get(key)
            sizes[key] = size if prev is None else tuple(max(a, b) for a, b in zip(prev, size))
    return sizes, pack([(k, *uv_size(*s)) for k, s in sizes.items()], 128, 128)


def paint(sizes, uvs, grow):
    rng = random.Random(7)
    img, glow = Image.new("RGBA", (128, 128), (0, 0, 0, 0)), Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for key, (w, h, d) in sizes.items():
        u, v = uvs[key]
        base = (GROW if grow else {}).get(key, COLORS[key])
        for name, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    c = shade(base, rng.uniform(0.94, 1.05))
                    if key == "arm":
                        c = mix(base, shade(base, 0.8), abs((x - fx) - fw / 2) / max(1, fw))
                    if key == "core" and (x + y) % 6 == 0:
                        c = mix(base, (255, 255, 230), 0.6)
                    if key == "beam":
                        img.putpixel((x, y), rgba(base, 0))
                        glow.putpixel((x, y), rgba(shade(base, 0.55 + 0.45 * ((y + x) % 4 == 0)), 255))
                        continue
                    img.putpixel((x, y), rgba(c))
                    if key in GLOW:
                        glow.putpixel((x, y), rgba(c))
    suffix = "_grow" if grow else ""
    img.save(TEX / f"shrink_ray{suffix}.png")
    glow.save(TEX / f"shrink_ray{suffix}_glow.png")


def java(uvs):
    lines = ["    public static LayerDefinition shrinkRay() {", "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    for name, pivot, rot, boxes in parts:
        cubes = "CubeListBuilder.create()"
        for x, y, z, w, h, d, key in boxes:
            u, v = uvs[key]
            cubes += f".texOffs({u}, {v}).addBox({fmt(round(x, 3))}, {fmt(round(y, 3))}, {fmt(round(z, 3))}, {fmt(round(w, 3))}, {fmt(round(h, 3))}, {fmt(round(d, 3))})"
        pose = f"PartPose.offsetAndRotation({', '.join(fmt(round(p, 4)) for p in pivot)}, {', '.join(fmt(round(r, 5)) for r in rot)})"
        lines.append(f"        root.addOrReplaceChild(\"{name}\", {cubes}, {pose});")
    lines.append("        return LayerDefinition.create(mesh, 128, 128);")
    lines.append("    }")
    body = "\n".join(lines)
    JAVA.write_text(f"""package com.rickandmorty.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Shrink Ray's geometry, y up, in pixels from the middle of its block's floor, front towards -z.
 * GENERATED by tools/generate_shrink_ray.py (with the textures), so edit the shapes there.
 */
@SuppressWarnings("unused")
public final class ShrinkRayLayers {{
    private ShrinkRayLayers() {{
    }}

{body}
}}
""", encoding="utf-8")


def main():
    sizes, uvs = layout()
    paint(sizes, uvs, False)
    paint(sizes, uvs, True)
    java(uvs)
    print(f"Shrink Ray written: {len(parts)} parts")


if __name__ == "__main__":
    main()
