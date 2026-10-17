"""Builds the Warp Gateway block/item models and texture. Run from the project root:

    python tools/generate_gateway_model.py

The gateway is 3 blocks wide and 3 tall. Block models can only reach one block
outside their own space, so the ring is split in two:
  - models/block/warp_gateway.json      platform, pylons and the lower half of the ring
                                        (rendered by the controller block at the bottom middle)
  - models/block/warp_gateway_top.json  upper half of the ring
                                        (rendered by the frame block at the top middle)
  - models/item/warp_gateway.json       everything, scaled down to fit an item model
Model space is for a gateway facing north; blockstates rotate it for other directions.

The numbers here must match GatewayStructure.java (ring centre and radii).
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/rickrooms"
TEXTURE_SIZE = 512
DENSITY = 2  # texture pixels per model unit

# Ring geometry (model units, 16 = one block). Keep in sync with GatewayStructure.java.
CX, CY = 8.0, 21.6          # ring centre (x, y) relative to the controller block
R_IN, R_OUT = 18.5, 23.5    # ring band
Z0, Z1 = 4.0, 12.0          # ring depth
SPLIT_Y = 24.0              # parts centred above this go in the top model (top block is 32 units up)
SEGMENTS = 16

FACES = ["north", "south", "east", "west", "up", "down"]


def arc_box(theta_deg, r0, r1, tangent, z0, z1, material, name, **opts):
    """A box sitting on the ring at angle theta, rotated to follow the circle.

    Minecraft only allows element rotations of -45/-22.5/0/22.5/45 degrees, so we pick
    whichever axis-aligned base orientation is closest and rotate by the remainder.
    """
    t = math.radians(theta_deg)
    rm = (r0 + r1) / 2
    px, py = CX + rm * math.cos(t), CY + rm * math.sin(t)
    radial, half_t = (r1 - r0) / 2, tangent / 2
    nearest = round(theta_deg / 90) * 90
    residual = theta_deg - nearest
    if residual <= -45.0001 or residual > 45:
        raise ValueError(theta_deg)
    if nearest % 180 == 0:   # left/right of the ring: radial is along x, tangent along y
        frm, to = [px - radial, py - half_t, z0], [px + radial, py + half_t, z1]
    else:                    # top/bottom: radial along y, tangent along x
        frm, to = [px - half_t, py - radial, z0], [px + half_t, py + radial, z1]
    el = {"name": name, "from": frm, "to": to, "material": material, **opts}
    if abs(residual) > 1e-6:
        el["rotation"] = {"angle": residual, "axis": "z", "origin": [px, py, (z0 + z1) / 2]}
    el["_center_y"] = py
    return el


def build_elements():
    els = []
    chord = 2 * ((R_IN + R_OUT) / 2) * math.sin(math.radians(180 / SEGMENTS)) + 0.6
    for i in range(SEGMENTS):
        theta = i * 360 / SEGMENTS
        if theta == 270:
            continue  # the bottom segment would be hidden inside the platform anyway
        els.append(arc_box(theta, R_IN, R_OUT, chord, Z0, Z1, "ring", f"ring_{i}"))
        # glowing inner lip: the edge of the event horizon
        els.append(arc_box(theta, R_IN - 0.8, R_IN, chord * 0.86, Z0 + 1, Z1 - 1, "rim", f"rim_{i}", glow=True))
    # chevrons: chunky clamps around the ring with a green light in each
    for j, theta in enumerate([0, 45, 90, 135, 180]):
        els.append(arc_box(theta, R_IN - 1.5, R_OUT + 0.5, 5.5, Z0 - 1, Z1 + 1, "chevron", f"chevron_{j}"))
        els.append(arc_box(theta, R_IN + 0.5, R_OUT - 0.5, 2.5, Z0 - 1.5, Z1 + 1.5, "glow", f"chevron_light_{j}", glow=True))

    # platform, trims and pylons (controller model only)
    platform = [
        {"name": "platform", "from": [-16, 0, 0], "to": [32, 4, 16], "material": "platform"},
        {"name": "dais", "from": [-6, 4, 2], "to": [22, 4.5, 14], "material": "trim"},
        {"name": "strip_front", "from": [-14, 3, -0.5], "to": [30, 3.6, 0], "material": "glow", "glow": True},
        {"name": "strip_back", "from": [-14, 3, 16], "to": [30, 3.6, 16.5], "material": "glow", "glow": True},
        {"name": "pylon_left", "from": [-16, 4, 2], "to": [-8, 13, 14], "material": "pylon"},
        {"name": "pylon_left_cap", "from": [-16, 13, 3], "to": [-9, 14, 13], "material": "trim"},
        {"name": "vent_left", "from": [-8, 6, 5], "to": [-7.5, 11, 11], "material": "vent", "glow": True},
        {"name": "pylon_right", "from": [24, 4, 2], "to": [32, 13, 14], "material": "pylon"},
        {"name": "pylon_right_cap", "from": [25, 13, 3], "to": [32, 14, 13], "material": "trim"},
        {"name": "vent_right", "from": [23.5, 6, 5], "to": [24, 11, 11], "material": "vent", "glow": True},
    ]
    for p in platform:
        p["_center_y"] = 0
    return platform + els


# --------------------------------------------------------------------------- painting

MATERIALS = {
    #            base colour            outline
    "ring": ((72, 82, 96, 255), (26, 30, 36, 255)),
    "chevron": ((112, 122, 138, 255), (26, 30, 36, 255)),
    "platform": ((58, 64, 74, 255), (24, 27, 32, 255)),
    "trim": ((150, 160, 172, 255), (40, 44, 50, 255)),
    "pylon": ((84, 92, 106, 255), (26, 30, 36, 255)),
    "glow": ((120, 255, 80, 255), None),
    "rim": ((110, 255, 150, 255), None),
    "vent": ((120, 255, 80, 255), None),
}


def shade(c, f):
    return (*(max(0, min(255, int(v * f))) for v in c[:3]), c[3])


def paint(img, box, material, face, rng):
    x0, y0, w, h = box
    x1, y1 = x0 + w - 1, y0 + h - 1
    d = ImageDraw.Draw(img)
    base, outline = MATERIALS[material]
    if material in ("glow", "rim"):
        d.rectangle([x0, y0, x1, y1], fill=base)
        if w > 2 and h > 2:
            d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=(200, 255, 170, 255))
        return
    if material == "vent":
        d.rectangle([x0, y0, x1, y1], fill=base)
        for y in range(y0 + 1, y1, 2):
            d.line([x0, y, x1, y], fill=(25, 60, 25, 255))
        return

    d.rectangle([x0, y0, x1, y1], fill=base if face != "down" else shade(base, 0.8))
    # a bit of grime so big faces don't look flat
    for _ in range(w * h // 10):
        px, py = rng.randrange(x0, x0 + w), rng.randrange(y0, y0 + h)
        img.putpixel((px, py), shade(img.getpixel((px, py)), rng.choice((0.9, 1.08))))
    if material == "ring" and face in ("north", "south") and h >= 6:
        mid = y0 + h // 2
        d.line([x0, mid, x1, mid], fill=shade(base, 0.7))       # panel seam
    if material == "platform" and face == "up":
        for x in range(x0 + 8, x1, 8):
            d.line([x, y0, x, y1], fill=shade(base, 0.75))      # floor plates
        for y in range(y0 + 8, y1, 8):
            d.line([x0, y, x1, y], fill=shade(base, 0.75))
    if material == "pylon" and face in ("north", "south", "east", "west"):
        for x in range(x0 + 3, x1 - 1, 4):
            d.line([x, y0 + 2, x, y1 - 2], fill=shade(base, 0.8))
    if material in ("trim", "chevron", "pylon") and h > 3:
        d.line([x0 + 1, y0 + 1, x1 - 1, y0 + 1], fill=shade(base, 1.25))
    if outline:
        d.rectangle([x0, y0, x1, y1], outline=outline)


def face_size(frm, to, face):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]


def pack(requests):
    placed, x, y, shelf = {}, 0, 0, 0
    for key, w, h in sorted(requests, key=lambda r: -r[2]):
        if x + w + 1 > TEXTURE_SIZE:
            x, y, shelf = 0, y + shelf + 1, 0
        if y + h > TEXTURE_SIZE:
            raise RuntimeError("texture too small")
        placed[key] = (x, y)
        x += w + 1
        shelf = max(shelf, h)
    return placed


def to_json(el, faces, dy=0.0, scale=1.0, pivot=(8.0, 0.0, 8.0)):
    def tr(p):
        return [round(pivot[i] + (p[i] + (dy if i == 1 else 0) - pivot[i]) * scale, 4) for i in range(3)]
    out = {"name": el["name"], "from": tr(el["from"]), "to": tr(el["to"]), "faces": faces}
    if "rotation" in el:
        r = el["rotation"]
        out["rotation"] = {"angle": r["angle"], "axis": r["axis"], "origin": tr(r["origin"])}
    if el.get("glow"):
        out["shade"] = False
    for key in ("from", "to"):
        for v in out[key]:
            if not -16 <= v <= 32:
                raise ValueError(f"{el['name']} out of model bounds: {out[key]}")
    return out


def main():
    rng = random.Random(4242)
    elements = build_elements()
    requests, sizes = [], {}
    for el in elements:
        for face in FACES:
            fw, fh = face_size(el["from"], el["to"], face)
            w, h = max(1, round(fw * DENSITY)), max(1, round(fh * DENSITY))
            sizes[(el["name"], face)] = (w, h)
            requests.append(((el["name"], face), w, h))
    positions = pack(requests)

    img = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    s = 16 / TEXTURE_SIZE
    face_data = {}
    for el in elements:
        faces = {}
        for face in FACES:
            x, y = positions[(el["name"], face)]
            w, h = sizes[(el["name"], face)]
            paint(img, (x, y, w, h), el["material"], face, rng)
            f = {"uv": [round(x * s, 4), round(y * s, 4), round((x + w) * s, 4), round((y + h) * s, 4)], "texture": "#gateway"}
            if el.get("glow"):
                f["neoforge_data"] = {"block_light": 15, "sky_light": 15}
            faces[face] = f
        face_data[el["name"]] = faces
    img.save(ASSETS / "textures/block/warp_gateway.png")

    textures = {"gateway": "rickrooms:block/warp_gateway", "particle": "rickrooms:block/warp_gateway"}
    bottom = [to_json(e, face_data[e["name"]]) for e in elements if e["_center_y"] < SPLIT_Y]
    top = [to_json(e, face_data[e["name"]], dy=-32) for e in elements if e["_center_y"] >= SPLIT_Y]
    header = {"credit": "Generated by tools/generate_gateway_model.py", "texture_size": [TEXTURE_SIZE, TEXTURE_SIZE],
              "ambientocclusion": False, "textures": textures}
    (ASSETS / "models/block/warp_gateway.json").write_text(json.dumps({**header, "elements": bottom}, indent=1))
    (ASSETS / "models/block/warp_gateway_top.json").write_text(json.dumps({**header, "elements": top}, indent=1))

    # Item: the whole gateway, scaled to 60% so it fits inside the item model bounds.
    whole = [to_json(e, face_data[e["name"]], scale=0.6) for e in elements]
    display = {
        "gui": {"rotation": [20, 200, 0], "translation": [0, -3.5, 0], "scale": [0.42, 0.42, 0.42]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -3, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.28, 0.28, 0.28]},
    }
    (ASSETS / "models/item/warp_gateway.json").write_text(json.dumps({**header, "elements": whole, "display": display}, indent=1))
    print(f"bottom {len(bottom)} / top {len(top)} elements")


if __name__ == "__main__":
    main()
