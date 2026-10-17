"""Builds the Portal Gun item models and texture. Run from the project root:

    python tools/generate_portal_gun_model.py

Modelled on the show's gun: a flat white body, a handle angled back and down, a dark knob
on the side, a red screen on top, and a glass capsule of glowing portal fluid on a round base
at the front. The fluid level in the capsule follows the gun's charges, so the script writes:
  - models/item/portal_gun.json                 full capsule, plus the overrides below
  - models/item/portal_gun_fluid_<0..8>.json    capsule filled to 0/8 .. 8/8
  - models/item/portal_gun_targeting_fluid_<0..8>.json   same, with the Targeting Upgrade scope
  - ..._blender_... / ..._attack_...          with the Blender Upgrade's saw on the side; in attack mode
                                              the fluid runs blood red
  - textures/item/portal_gun.png
ClientModEvents registers the "rickrooms:fluid", "rickrooms:targeting" and "rickrooms:blender" properties.

Model space: 16 units = 1 block. The muzzle points north (-Z), the top is +Y.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/rickrooms"
TEXTURE_SIZE = 256
DENSITY = 4  # texture pixels per model unit
FLUID_LEVELS = 8

HANDLE_ROTATION = {"angle": -45, "axis": "x", "origin": [8, 8.5, 12.5]}
# the fluid column inside the glass: bottom and full height
FLUID_BOTTOM, FLUID_TOP = 12.6, 19.2

# name, from, to, material, options
ELEMENTS = [
    # --- flat body with softened edges
    ("body", [5, 8, 1], [11, 11.5, 15], "body", {}),
    ("body_round", [4.7, 8.3, 1.3], [11.3, 11.2, 14.7], "body", {}),
    ("body_top", [5.3, 11.5, 1.3], [10.7, 11.75, 14.7], "body", {}),
    # --- front: dark panel with three green lights
    ("front_panel", [5.5, 8.4, 0.6], [10.5, 11.1, 1.0], "dark", {}),
    ("light_left", [5.9, 9.1, 0.3], [7.1, 10.3, 0.6], "emitter", {"glow": True}),
    ("light_mid", [7.4, 9.1, 0.3], [8.6, 10.3, 0.6], "emitter", {"glow": True}),
    ("light_right", [8.9, 9.1, 0.3], [10.1, 10.3, 0.6], "emitter", {"glow": True}),
    # --- handle, angled back and down
    ("handle", [6.25, 1.5, 11], [9.75, 8.5, 14], "handle", {"rotation": HANDLE_ROTATION}),
    ("handle_end", [6.0, 1.0, 10.75], [10.0, 1.75, 14.25], "handle", {"rotation": HANDLE_ROTATION}),
    # --- the dial knob on the side and the red screen on top
    ("knob", [3.7, 8.4, 11.2], [4.7, 11.1, 13.9], "knob", {}),
    ("knob_cap", [3.3, 9.1, 11.9], [3.7, 10.4, 13.2], "knob", {}),
    ("screen", [6.3, 11.75, 8.6], [9.7, 11.95, 12.2], "screen", {"glow": True}),
    # --- the capsule: base, glass, dome (fluid is added separately, by level)
    ("capsule_base", [5.9, 11.75, 2.3], [10.1, 12.6, 6.5], "metal", {}),
    ("capsule_ring", [6.3, 12.6, 2.7], [9.7, 12.9, 6.1], "metal", {}),
]
FLUID = ("fluid", [6.95, FLUID_BOTTOM, 3.35], [9.05, FLUID_TOP, 5.45], "fluid", {"glow": True})
CORE = ("fluid_core", [7.5, 14.6, 3.9], [8.5, 16.0, 4.9], "core", {"glow": True})
GLASS = [
    ("glass", [6.6, 12.9, 3.0], [9.4, 19.5, 5.8], "glass", {}),
    ("glass_dome", [7.1, 19.5, 3.5], [8.9, 20.2, 5.3], "glass", {}),
]
FLUID_RED = ("fluid_red", FLUID[1], FLUID[2], "blood", {"glow": True})
CORE_RED = ("fluid_core_red", CORE[1], CORE[2], "blood_core", {"glow": True})
# Only on guns with the Blender Upgrade: a little circular saw on the right side, and its guard.
BLENDER_ELEMENTS = [
    ("saw_hub", [11.0, 9.2, 6.9], [11.6, 10.4, 8.1], "knob", {}),
    ("saw", [11.6, 8.3, 6.0], [11.9, 11.3, 9.0], "saw", {}),
    ("saw_guard", [11.0, 11.3, 5.8], [12.1, 11.6, 9.2], "dark", {}),
]
# Only on guns with the Targeting Upgrade: a little scope with a red lens.
TARGETING_ELEMENTS = [
    ("scope", [7.2, 11.75, 12.4], [8.8, 12.8, 14.4], "knob", {}),
    ("scope_lens", [7.45, 12.0, 12.1], [8.55, 12.6, 12.4], "laser", {"glow": True}),
]

FACES = ["north", "south", "east", "west", "up", "down"]

# --------------------------------------------------------------------------- painting

OUTLINE = (52, 56, 62, 255)
MATERIALS = {
    # base colour, outline colour (None = no outline)
    "body": ((232, 233, 236, 255), OUTLINE),
    "handle": ((214, 216, 220, 255), OUTLINE),
    "dark": ((64, 70, 74, 255), (30, 33, 36, 255)),
    "metal": ((196, 199, 204, 255), OUTLINE),
    "knob": ((58, 60, 64, 255), (22, 23, 25, 255)),
    "screen": ((226, 52, 44, 255), (120, 20, 18, 255)),
    "emitter": ((120, 255, 80, 255), None),
    "laser": ((255, 60, 50, 255), None),
    "fluid": ((84, 236, 58, 255), None),
    "core": ((226, 255, 190, 255), None),
    "glass": ((215, 255, 235, 55), None),
    "blood": ((200, 20, 28, 255), None),
    "blood_core": ((255, 150, 140, 255), None),
    "saw": ((200, 204, 210, 255), (90, 94, 100, 255)),
}


def shade(color, factor):
    return (*(max(0, min(255, int(c * factor))) for c in color[:3]), color[3])


def paint_face(img, box, material, face, name, rng):
    x0, y0, w, h = box
    draw = ImageDraw.Draw(img)
    base, outline = MATERIALS[material]
    x1, y1 = x0 + w - 1, y0 + h - 1

    if material in ("emitter", "laser", "core", "blood_core"):
        cx, cy = x0 + w / 2, y0 + h / 2
        bright = (240, 255, 220) if material not in ("laser", "blood_core") else (255, 200, 190)
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                t = min(1.0, math.hypot((x + 0.5 - cx) / (w / 2), (y + 0.5 - cy) / (h / 2)))
                img.putpixel((x, y), (*[int(a + (b - a) * t) for a, b in zip(bright, base[:3])], 255))
        return
    if material in ("fluid", "blood"):
        draw.rectangle([x0, y0, x1, y1], fill=base)
        light, dark = ((200, 255, 170, 255), (40, 170, 40, 255)) if material == "fluid" else ((255, 120, 110, 255), (110, 0, 8, 255))
        for _ in range(max(1, w * h // 14)):   # bubbles and energy streaks
            px, py = rng.randrange(x0, x0 + w), rng.randrange(y0, y0 + h)
            img.putpixel((px, py), light if rng.random() < 0.7 else dark)
        return
    if material == "saw" and face in ("east", "west"):
        draw.rectangle([x0, y0, x1, y1], fill=base)
        for x in range(x0, x1 + 1, 2):                                        # teeth round the rim
            img.putpixel((x, y0), (60, 64, 70, 255))
            img.putpixel((x, y1), (60, 64, 70, 255))
        img.putpixel((x0 + w // 2, y0 + h // 2), (150, 20, 20, 255))          # a spot of old blood
        return
    if material == "glass" and name == "glass":
        # The capsule around the fluid: clear, with a solid rim and a glint. No see-through
        # tint here, because a semi-transparent face in front of the fluid can hide it (shader
        # packs draw these faces in whatever order they like).
        draw.rectangle([x0, y0, x1, y1], outline=(226, 248, 238, 255))
        if face in ("north", "south", "east", "west") and w > 3:
            draw.line([x0 + 2, y0 + 2, x0 + 2, y1 - 2], fill=(255, 255, 255, 255))
        return
    if material == "glass":
        draw.rectangle([x0, y0, x1, y1], fill=base)
        draw.rectangle([x0, y0, x1, y1], outline=(240, 255, 248, 130))
        if face in ("north", "south", "east", "west") and w > 3:   # a highlight streak down the glass
            draw.line([x0 + 2, y0 + 2, x0 + 2, y1 - 2], fill=(255, 255, 255, 150))
        return

    draw.rectangle([x0, y0, x1, y1], fill=base if face != "down" else shade(base, 0.86))
    if material in ("body", "handle") and face != "down" and h > 3:
        draw.line([x0 + 1, y0 + 1, x1 - 1, y0 + 1], fill=shade(base, 1.06))   # soft highlight
    if name == "body" and face in ("east", "west"):
        seam = y0 + h // 2
        draw.line([x0 + 2, seam, x1 - 2, seam], fill=shade(base, 0.86))      # panel seam
    if material == "knob" and face in ("north", "south", "up", "down"):
        for x in range(x0 + 1, x1, 2):
            draw.line([x, y0 + 1, x, y1 - 1], fill=shade(base, 1.5))          # knurling
    if material == "screen":
        for y in range(y0 + 2, y1, 3):
            draw.line([x0 + 1, y, x1 - 1, y], fill=shade(base, 0.8))          # scan lines
    if outline:
        draw.rectangle([x0, y0, x1, y1], outline=outline)


# --------------------------------------------------------------------------- layout

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


def main():
    rng = random.Random(137)
    all_elements = ELEMENTS + [FLUID, CORE, FLUID_RED, CORE_RED] + GLASS + TARGETING_ELEMENTS + BLENDER_ELEMENTS
    requests, sizes = [], {}
    for name, frm, to, material, opts in all_elements:
        for face in FACES:
            fw, fh = face_size(frm, to, face)
            w, h = max(1, round(fw * DENSITY)), max(1, round(fh * DENSITY))
            sizes[(name, face)] = (w, h)
            requests.append(((name, face), w, h))
    positions = pack(requests)

    img = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    built = {}
    scale = 16 / TEXTURE_SIZE
    for name, frm, to, material, opts in all_elements:
        faces = {}
        for face in FACES:
            x, y = positions[(name, face)]
            w, h = sizes[(name, face)]
            paint_face(img, (x, y, w, h), material, face, name, rng)
            data = {"uv": [round(x * scale, 4), round(y * scale, 4), round((x + w) * scale, 4), round((y + h) * scale, 4)],
                    "texture": "#gun"}
            if opts.get("glow"):
                data["neoforge_data"] = {"block_light": 15, "sky_light": 15}
            faces[face] = data
        element = {"name": name, "from": frm, "to": to, "faces": faces}
        if "rotation" in opts:
            element["rotation"] = opts["rotation"]
        if opts.get("glow"):
            element["shade"] = False
        built[name] = element

    def fluid_elements(level, red):
        """The fluid column at a fill level (0..FLUID_LEVELS); the bright core only shows when covered."""
        if level == 0:
            return []
        top = FLUID_BOTTOM + (FLUID_TOP - FLUID_BOTTOM) * level / FLUID_LEVELS
        fluid = dict(built["fluid_red" if red else "fluid"], to=[FLUID[2][0], round(top, 3), FLUID[2][2]])
        return [fluid] + ([built["fluid_core_red" if red else "fluid_core"]] if top >= CORE[2][1] else [])

    def model(level, targeting, blender=0, overrides=None):
        # glass last, so it's drawn over the fluid; blender 1 = upgrade fitted, 2 = attack mode
        elements = [built[e[0]] for e in ELEMENTS] + fluid_elements(level, blender == 2) + [built[g[0]] for g in GLASS]
        if targeting:
            elements += [built[e[0]] for e in TARGETING_ELEMENTS]
        if blender:
            elements += [built[e[0]] for e in BLENDER_ELEMENTS]
        m = {"credit": "Generated by tools/generate_portal_gun_model.py", "texture_size": [TEXTURE_SIZE, TEXTURE_SIZE],
             "textures": {"gun": "rickrooms:item/portal_gun", "particle": "rickrooms:item/portal_gun"},
             "elements": elements, "display": DISPLAY}
        if overrides:
            m["overrides"] = overrides
        return m

    # The last override whose values are all <= the item's properties wins, so list plain guns first
    # and upgraded ones after, each from empty to full.
    overrides = []
    for targeting in (0, 1):
        for blender, suffix in ((0, ""), (1, "_blender"), (2, "_attack")):
            for level in range(FLUID_LEVELS + 1):
                name = f"portal_gun{'_targeting' if targeting else ''}{suffix}_fluid_{level}"
                (ASSETS / f"models/item/{name}.json").write_text(json.dumps(model(level, targeting, blender), indent=1))
                overrides.append({"predicate": {"rickrooms:targeting": targeting, "rickrooms:blender": blender / 2,
                                                  "rickrooms:fluid": level / FLUID_LEVELS},
                                  "model": f"rickrooms:item/{name}"})
    (ASSETS / "models/item/portal_gun.json").write_text(json.dumps(model(FLUID_LEVELS, False, 0, overrides), indent=1))
    old = ASSETS / "models/item/portal_gun_targeting.json"
    if old.exists():
        old.unlink()
    img.save(ASSETS / "textures/item/portal_gun.png")
    print(f"Wrote {len(overrides)} fluid/upgrade variants")


# How the model is positioned in each view (hands, inventory, ground, item frame).
DISPLAY = {
    "firstperson_righthand": {"rotation": [0, 8, 0], "translation": [-1.5, 3.0, -2], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, 8, 0], "translation": [-1.5, 3.0, -2], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [72, 0, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [72, 0, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
    "gui": {"rotation": [25, 125, 0], "translation": [0, -2, 0], "scale": [0.8, 0.8, 0.8]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, -2, 0], "scale": [0.85, 0.85, 0.85]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}

if __name__ == "__main__":
    main()
