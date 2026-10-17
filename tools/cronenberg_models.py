"""Model shapes for the Cronenberg World creatures, the Mantis People, the mutation growths and
the Council's hats. generate_cronenberg_art.py packs their textures and writes
client/model/CronenbergLayers.java from this, so the geometry and the UVs never disagree.

Each model: texture size and a list of parts. A part is
    (name, parent, pivot (x, y, z), rotation (x, y, z) in radians, [boxes])
and a box is (x, y, z, w, h, d, uv_key). Boxes with the same uv_key share texture space
(and must have the same size). Model space: 16 units = 1 block, y points down, ground at y=24.
"""

MODELS = {}


def model(name, width, height, parts):
    MODELS[name] = {"size": (width, height), "parts": parts}


# ---------------------------------------------------------------------------- Crawler
# Low and wide on six human arms, a face stretched across the front, an arm growing out of its back.
crawler = [
    ("body", None, (0, 17, 0), (0, 0, 0), [(-7, -3, -8, 14, 5, 16, "body")]),
    ("face", "body", (0, -0.5, -8), (0, 0, 0), [(-5, -4, -3, 10, 7, 3, "face")]),
    ("jaw", "face", (0, 3, -1.5), (0.5, 0, 0), [(-4, 0, -1.5, 8, 2, 3, "jaw")]),
    ("growths", "body", (0, 0, 0), (0, 0, 0), [(-5, -5, -3, 4, 3, 4, "tumor"), (1, -5, 2, 4, 3, 4, "tumor"), (-2, -5, -7, 4, 3, 4, "tumor")]),
    ("back_arm", "body", (2, -3, 3), (-0.3, 0, 0.2), [(-1, -8, -1, 2, 8, 2, "arm")]),
    ("back_fore", "back_arm", (0, -8, 0), (0.9, 0, 0), [(-1, -7, -1, 2, 7, 2, "arm2")]),
    ("back_hand", "back_fore", (0, -7, 0), (0, 0, 0), [(-1.5, -2, -1.5, 3, 2, 3, "hand")]),
]
for i, z in enumerate((-5, 0, 5)):
    crawler += [
        (f"right_leg{i}", "body", (-6.5, 0, z), (0, 0, 0.55), [(-9, -1, -1, 9, 2, 2, "leg")]),
        (f"right_shin{i}", f"right_leg{i}", (-8.5, 0, 0), (0, 0, -0.3), [(-1, 0, -1, 2, 11, 2, "shin")]),
        (f"right_hand{i}", f"right_shin{i}", (0, 11, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 1, 3, "palm")]),
        (f"left_leg{i}", "body", (6.5, 0, z), (0, 0, -0.55), [(0, -1, -1, 9, 2, 2, "leg")]),
        (f"left_shin{i}", f"left_leg{i}", (8.5, 0, 0), (0, 0, 0.3), [(-1, 0, -1, 2, 11, 2, "shin")]),
        (f"left_hand{i}", f"left_shin{i}", (0, 11, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 1, 3, "palm")]),
    ]
model("crawler", 64, 64, crawler)

# ---------------------------------------------------------------------------- Stalker
# Tall and lopsided: a huge clawed arm dragging on the ground, a withered one, a second head.
stalker = [
    ("right_leg", None, (-2.5, 8, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 16, 3, "leg")]),
    ("left_leg", None, (2.5, 8, 0), (0, 0, 0), [(-1.5, 0, -1.5, 3, 16, 3, "leg")]),
    ("torso", None, (0, 8, 0), (0.2, 0, 0.12), [(-4, -14, -2.5, 8, 14, 5, "torso")]),
    ("head", "torso", (0, -14, 0), (0, 0, -0.2), [(-3, -8, -3, 6, 8, 6, "head")]),
    ("second_head", "torso", (3.5, -13, 0), (0, 0, 0.6), [(-2, -4, -2, 4, 4, 4, "small_head")]),
    ("big_arm", "torso", (5.5, -12.5, 0), (0, 0, -0.15), [(-2, -1, -2, 4, 11, 4, "big_arm")]),
    ("big_fore", "big_arm", (0, 10, 0), (-0.4, 0, 0), [(-2, 0, -2, 4, 11, 4, "big_fore")]),
    ("claws", "big_fore", (0, 11, 0), (0, 0, 0), [(-2, 0, -1.5, 1, 6, 1, "claw"), (-0.5, 0, -0.5, 1, 6, 1, "claw"), (1, 0, -1.5, 1, 6, 1, "claw")]),
    ("thin_arm", "torso", (-4.5, -12, 0), (0, 0, 0.1), [(-1, -1, -1, 2, 16, 2, "thin_arm")]),
]
for i, (x, rz) in enumerate(((-1.5, 0.3), (1.5, -0.3))):
    stalker += [
        (f"tentacle{i}a", "torso", (x, -10, 2.5), (-0.6, 0, rz), [(-1, -6, -1, 2, 6, 2, "tentacle")]),
        (f"tentacle{i}b", f"tentacle{i}a", (0, -6, 0), (-0.4, 0, 0), [(-1, -6, -1, 2, 6, 2, "tentacle")]),
        (f"tentacle{i}c", f"tentacle{i}b", (0, -6, 0), (-0.4, 0, 0), [(-0.5, -5, -0.5, 1, 5, 1, "tip")]),
    ]
model("stalker", 64, 64, stalker)

# ---------------------------------------------------------------------------- Blob
blob = [
    ("body", None, (0, 20, 0), (0, 0, 0), [(-11, -16, -10, 22, 16, 20, "body")]),
    ("lumps", "body", (0, 0, 0), (0, 0, 0), [(-8, -21, -7, 12, 7, 12, "lump"), (2, -19, -2, 8, 5, 8, "lump2")]),
    ("eyes", "body", (0, 0, 0), (0, 0, 0), [
        (-8, -13, -11.5, 3, 3, 2, "eye"), (-2, -15, -11.5, 3, 3, 2, "eye"), (5, -11, -11.5, 3, 3, 2, "eye"),
        (-4, -19, -8.5, 3, 3, 2, "eye"), (4, -17, -3.5, 3, 3, 2, "eye"), (10.5, -9, -3, 2, 3, 3, "side_eye"),
        (-12.5, -12, 2, 2, 3, 3, "side_eye")]),
    ("jaw", "body", (0, -4, -10), (0.3, 0, 0), [(-5, 0, -3, 10, 3, 3, "jaw")]),
]
for i, (x, z) in enumerate(((-7, -6), (7, -6), (-8, 5), (8, 5), (-2, 7), (3, -8))):
    blob.append((f"leg{i}", None, (x, 20, z), (0, 0, 0), [(-1.5, 0, -1.5, 3, 4, 3, "leg")]))
for i, (x, z) in enumerate(((-6, 4), (6, -3), (2, 7), (-3, -5))):
    blob += [
        (f"tentacle{i}a", "body", (x, -16, z), (0.3, 0, 0), [(-1, -6, -1, 2, 6, 2, "tentacle")]),
        (f"tentacle{i}b", f"tentacle{i}a", (0, -6, 0), (0.3, 0, 0), [(-1, -6, -1, 2, 6, 2, "tentacle")]),
        (f"tentacle{i}c", f"tentacle{i}b", (0, -6, 0), (0.3, 0, 0), [(-0.5, -5, -0.5, 1, 5, 1, "tip")]),
    ]
model("blob", 128, 64, blob)

# ---------------------------------------------------------------------------- Behemoth (rendered 2.2x)
behemoth = [
    ("body", None, (0, 16, 0), (0, 0, 0), [(-10, -12, -8, 20, 12, 16, "body")]),
    ("hump", "body", (0, 0, 0), (0, 0, 0), [(-7, -17, -3, 14, 6, 10, "hump")]),
    ("head", "body", (0, -5, -8), (0.1, 0, 0), [(-6, -4, -6, 12, 8, 6, "head")]),
    ("jaw", "head", (0, 4, -1), (0.4, 0, 0), [(-5.5, 0, -5, 11, 3, 6, "jaw")]),
    ("eyes", "head", (0, 0, 0), (0, 0, 0), [(-4.5, -3, -6.8, 2, 2, 1, "eye"), (-0.5, -3.5, -6.8, 2, 2, 1, "eye"), (3, -2, -6.8, 2, 2, 1, "eye")]),
    ("faces", "body", (0, 0, 0), (0, 0, 0), [(-12, -10, -2, 2, 5, 5, "side_face"), (10, -9, 1, 2, 5, 5, "side_face")]),
]
for i, (x, z) in enumerate(((-8, -5), (8, -5), (-8.5, 0), (8.5, 0), (-8, 5), (8, 5))):
    behemoth.append((f"leg{i}", None, (x, 16, z), (0, 0, 0), [(-1.5, 0, -1.5, 3, 8, 3, "leg")]))
for i, (x, z, rx, rz) in enumerate(((-5, 0, 0.2, 0.5), (5, 2, -0.1, -0.5), (-2, 5, 0.5, 0.1), (3, -1, -0.4, -0.1))):
    behemoth += [
        (f"arm{i}", "body", (x, -16, z), (rx, 0, rz), [(-1, -9, -1, 2, 9, 2, "arm")]),
        (f"fore{i}", f"arm{i}", (0, -9, 0), (0.6, 0, 0), [(-1, -8, -1, 2, 8, 2, "fore")]),
        (f"hand{i}", f"fore{i}", (0, -8, 0), (0, 0, 0), [(-1.5, -2, -1.5, 3, 2, 3, "hand")]),
    ]
for i, x in enumerate((-5, 0, 5)):
    behemoth += [
        (f"tentacle{i}a", "body", (x, -6, 8), (0.3, 0, 0), [(-1, -1, 0, 2, 2, 7, "tentacle")]),
        (f"tentacle{i}b", f"tentacle{i}a", (0, 0, 7), (0.3, 0, 0), [(-1, -1, 0, 2, 2, 7, "tentacle")]),
        (f"tentacle{i}c", f"tentacle{i}b", (0, 0, 7), (0.3, 0, 0), [(-0.5, -0.5, 0, 1, 1, 6, "tip")]),
    ]
model("behemoth", 128, 128, behemoth)

# ---------------------------------------------------------------------------- Mantis Person
mantis = [
    ("right_leg", None, (-2, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, "leg")]),
    ("left_leg", None, (2, 12, 0), (0, 0, 0), [(-2, 0, -2, 4, 12, 4, "leg")]),
    ("body", None, (0, 0, 0), (0, 0, 0), [(-4, 0, -2, 8, 12, 4, "body")]),
    ("head", None, (0, 0, 0), (0, 0, 0), [
        (-3.5, -8, -2.5, 7, 3, 5, "head_top"), (-2.5, -5, -2.5, 5, 2, 5, "head_mid"), (-1.5, -3, -2.5, 3, 3, 5, "chin"),
        (-5, -8.5, -2, 2, 3, 3, "eye"), (3, -8.5, -2, 2, 3, 3, "eye"),
        (-1.5, -0.5, -3.5, 1, 2, 1, "mandible"), (0.5, -0.5, -3.5, 1, 2, 1, "mandible")]),
    ("right_antenna", "head", (-1.5, -8, -1), (-0.5, 0, -0.3), [(-0.5, -7, -0.5, 1, 7, 1, "antenna")]),
    ("left_antenna", "head", (1.5, -8, -1), (-0.5, 0, 0.3), [(-0.5, -7, -0.5, 1, 7, 1, "antenna")]),
    ("right_arm", None, (-5, 2, 0), (-1.2, 0, 0.1), [(-2, -1, -1.5, 3, 7, 3, "arm")]),
    ("right_scythe", "right_arm", (-0.5, 6, 0), (-2.2, 0, 0), [(-1, 0, -1, 2, 11, 2, "scythe")]),
    ("left_arm", None, (5, 2, 0), (-1.2, 0, -0.1), [(-1, -1, -1.5, 3, 7, 3, "arm")]),
    ("left_scythe", "left_arm", (0.5, 6, 0), (-2.2, 0, 0), [(-1, 0, -1, 2, 11, 2, "scythe")]),
]
model("mantis", 64, 64, mantis)

# ---------------------------------------------------------------------------- mutation growths (on players)
# Attached to the player model's body, head and right arm (their poses are copied at render time).
mutation = [
    ("body", None, (0, 0, 0), (0, 0, 0), [(-5, 1, 1.5, 5, 5, 4, "tumor"), (1, 5, -3.6, 4, 4, 2, "lump"), (1.5, 0, 1.8, 3, 3, 3, "small"),
                                          (-4.6, 7, -3.2, 3, 3, 2, "lump2")]),
    ("head", None, (0, 0, 0), (0, 0, 0), [(-6.2, -7, -2.5, 3, 4, 4, "lump3"), (0, -10, -1.5, 4, 3, 4, "cap"), (3.6, -6.5, -3.2, 2, 3, 3, "eye")]),
    ("arm", None, (0, 0, 0), (0, 0, 0), [(-4.8, 2, -2.5, 4, 7, 4, "arm")]),
    ("spikes", None, (0, 0, 0), (0, 0, 0), [(-2.5, 2, 2, 1, 1, 6, "spike"), (-0.5, 4, 2, 1, 1, 7, "spike2"), (1.5, 6, 2, 1, 1, 6, "spike")]),
]
model("mutation", 64, 32, mutation)

# ---------------------------------------------------------------------------- the Council's hats (on the head)
hats = [
    ("top_hat", None, (0, 0, 0), (0, 0, 0), [(-5, -9, -5, 10, 1, 10, "top_brim"), (-3, -16, -3, 6, 7, 6, "top_crown")]),
    ("wizard", None, (0, 0, 0), (0, 0, 0), [(-5, -9, -5, 10, 1, 10, "wiz_brim"), (-4, -12, -4, 8, 3, 8, "wiz1"), (-3, -15, -3, 6, 3, 6, "wiz2")]),
    ("wizard_tip", "wizard", (0, -15, 0), (-0.35, 0, 0.1), [(-2, -3, -2, 4, 3, 4, "wiz3"), (-1, -6, -1, 2, 3, 2, "wiz4")]),
    ("cowboy", None, (0, 0, 0), (0, 0, 0), [(-6, -9, -6, 12, 1, 12, "cow_brim"), (-3.5, -13, -3.5, 7, 4, 7, "cow_crown")]),
    ("crown", None, (0, 0, 0), (0, 0, 0), [(-4.5, -11, -4.5, 9, 3, 9, "crown")]),
    ("beanie", None, (0, 0, 0), (0, 0, 0), [(-4.5, -10, -4.5, 9, 3, 9, "beanie"), (-1, -12, -1, 2, 2, 2, "pompom")]),
    ("propeller", None, (0, 0, 0), (0, 0, 0), [(-4.5, -9.5, -4.5, 9, 2, 9, "cap"), (-0.5, -11.5, -0.5, 1, 2, 1, "stick")]),
    ("propeller_blades", "propeller", (0, -11.5, 0), (0, 0, 0), [(-6, -0.5, -1, 12, 1, 2, "blade"), (-1, -0.5, -6, 2, 1, 12, "blade2")]),
    ("traffic_cone", None, (0, 0, 0), (0, 0, 0), [(-5, -9, -5, 10, 1, 10, "cone_base"), (-3, -12, -3, 6, 3, 6, "cone1"),
                                                   (-2, -15, -2, 4, 3, 4, "cone2"), (-1, -18, -1, 2, 3, 2, "cone3")]),
]
model("hats", 128, 64, hats)
