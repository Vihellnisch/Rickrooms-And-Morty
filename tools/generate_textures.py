"""Generates the mod's placeholder textures. Run from the project root:

    python tools/generate_textures.py

Replace any of the output PNGs with hand-made art whenever you like.
The 3D Portal Gun texture comes from tools/generate_portal_gun_model.py instead.
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/rickrooms/textures"

OUTLINE = (30, 34, 32, 255)
WHITE = (236, 240, 236, 255)
LIGHT = (200, 206, 202, 255)
GRAY = (150, 156, 152, 255)
DARK_GRAY = (100, 106, 102, 255)
CORE = (225, 255, 190, 255)
LIME = (140, 255, 70, 255)
GREEN = (60, 200, 40, 255)
DARK_GREEN = (25, 120, 25, 255)
GLASS = (190, 255, 220, 255)


def add_outline(img):
    w, h = img.size
    px = img.load()
    filled = {(x, y) for x in range(w) for y in range(h) if px[x, y][3] > 0}
    for x in range(w):
        for y in range(h):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = OUTLINE


def portal_fluid():
    """16x16 round bottle of glowing green fluid."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            r = math.hypot(cx - 8, cy - 10.2)
            if r <= 4.3:
                if cy < 7.6:
                    color = GLASS                                   # air bubble at the top
                elif (cx - 6.3) ** 2 + (cy - 8.8) ** 2 < 1.3:
                    color = CORE                                    # shine
                else:
                    shade = (cx - 8) * 0.5 + (cy - 10.2) * 0.5
                    color = LIME if shade < -0.5 else (GREEN if shade < 1.6 else DARK_GREEN)
                px[x, y] = color
            elif 6 <= x <= 9 and 3 <= y <= 5:                      # neck
                px[x, y] = GLASS
            elif 6 <= x <= 9 and 1 <= y <= 2:                      # cork
                px[x, y] = (150, 100, 55, 255) if y == 1 else (115, 75, 40, 255)
    add_outline(img)
    return img


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def portal_swirl(size=128):
    """Swirling green disc with a bright rim. The renderer squashes it into an ellipse and spins it."""
    rng = random.Random(137)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    stops = [(0.0, (15, 90, 20)), (0.45, (50, 180, 35)), (0.75, (130, 245, 70)), (1.0, (230, 255, 190))]
    for y in range(size):
        for x in range(size):
            nx = (x + 0.5) / size * 2 - 1
            ny = (y + 0.5) / size * 2 - 1
            r = math.hypot(nx, ny)
            if r > 1.0:
                continue
            theta = math.atan2(ny, nx)
            swirl = 0.5 + 0.5 * math.sin(3 * theta + 11 * r)
            swirl2 = 0.5 + 0.5 * math.sin(5 * theta - 7 * r + 1.3)
            v = 0.45 * swirl + 0.2 * swirl2 + 0.35 * (1 - r) + rng.uniform(-0.04, 0.04)
            rim = max(0.0, 1 - abs(r - 0.9) / 0.08)  # bright ring near the edge
            v = min(1.0, max(0.0, v + rim * 0.8))
            for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
                if p0 <= v <= p1:
                    color = lerp(c0, c1, (v - p0) / (p1 - p0))
                    break
            alpha = 255 if r < 0.92 else int(255 * (1 - (r - 0.92) / 0.08))
            px[x, y] = (*color, max(0, alpha))
    return img


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rim_radius(theta):
    """The portal's edge wobbles a little, like in the show."""
    return 0.9 + 0.018 * math.sin(3 * theta) + 0.012 * math.sin(7 * theta + 1.0)


def spiral_value(r, theta):
    """Spiral arms winding into the centre (0..1)."""
    return 0.5 + 0.5 * math.sin(3 * theta - 7.5 * math.log(r + 0.04))


def show_portal(size=256):
    """Rick-style portal: green spiral arms into a bright centre, a thick wobbly yellow-green rim."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    base, dark, light, core = (84, 214, 44), (28, 120, 30), (200, 255, 122), (236, 255, 190)
    rim_in, rim_hot, rim_out = (150, 240, 60), (218, 255, 96), (110, 220, 50)
    for y in range(size):
        for x in range(size):
            nx = (x + 0.5) / size * 2 - 1
            ny = (y + 0.5) / size * 2 - 1
            r = math.hypot(nx, ny)
            theta = math.atan2(ny, nx)
            edge = rim_radius(theta)
            rn = r / edge
            if rn > 1.1:
                continue
            if rn < 0.84:
                v = spiral_value(r, theta)
                color = mix(dark, base, 0.35 + 0.65 * v)
                if v > 0.86:
                    color = mix(color, light, (v - 0.86) / 0.14)
                color = mix(color, core, max(0.0, 1 - rn / 0.28))     # glowing centre
                alpha = 255
            elif rn < 0.98:
                t = (rn - 0.84) / 0.14                                 # thick bright rim
                color = mix(rim_in, rim_hot, 1 - abs(t - 0.45) * 2)
                alpha = 255
            else:
                color = rim_out
                alpha = int(255 * max(0.0, 1 - (rn - 0.98) / 0.12))   # soft outer edge
            px[x, y] = (*color, alpha)
    return img


def show_portal_swirl(size=256):
    """Just the spiral, fading out before the rim; spun the other way on top for a vortex."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        for x in range(size):
            nx = (x + 0.5) / size * 2 - 1
            ny = (y + 0.5) / size * 2 - 1
            r = math.hypot(nx, ny)
            if r >= 0.95:
                continue
            v = 0.5 + 0.5 * math.sin(3 * math.atan2(ny, nx) - 7.5 * math.log(r + 0.04) + 1.6)  # same winding as the base
            fade = max(0.0, 1 - r / 0.95) ** 0.6
            if v > 0.6:
                px[x, y] = (*mix((120, 240, 70), (230, 255, 170), (v - 0.6) / 0.4), int(200 * fade * (v - 0.6) / 0.4))
    return img


def portal_glow(size=128):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        for x in range(size):
            r = math.hypot((x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1)
            if r < 1:
                px[x, y] = (150, 255, 90, int(255 * (1 - r) ** 2))
    return img


def to_gold(img):
    """Evil Morty's portal: the same portal, recoloured by brightness into golds."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            if lum < 0.85:
                color = mix((60, 32, 0), (245, 170, 30), (lum * 1.25) ** 1.4)
            else:
                color = mix((255, 200, 60), (255, 245, 190), (lum - 0.85) / 0.15)
            px[x, y] = (*color, a)
    return out


def projectile_orb(size=32):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        for x in range(size):
            r = math.hypot((x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1)
            if r > 1:
                continue
            color = lerp((235, 255, 210), (90, 255, 50), min(1, r / 0.6))
            alpha = int(255 * (1 - r) ** 0.8)
            px[x, y] = (*color, alpha)
    return img


# --------------------------------------------------------------------------- Backrooms blocks

def jitter(rng, color, amount):
    return tuple(max(0, min(255, c + rng.randint(-amount, amount))) for c in color[:3]) + (255,)


def backrooms_wallpaper():
    """Old mustard-yellow wallpaper: vertical stripes with a faint diamond pattern."""
    rng = random.Random(1)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            base = (196, 176, 92) if x % 4 in (0, 1) else (206, 188, 104)
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                base = (184, 164, 84)                         # diamond lattice
            px[x, y] = jitter(rng, base, 5)
    for _ in range(3):                                        # damp stains
        sx, sy = rng.randrange(16), rng.randrange(16)
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                if rng.random() < 0.7:
                    c = px[(sx + dx) % 16, (sy + dy) % 16]
                    px[(sx + dx) % 16, (sy + dy) % 16] = (int(c[0] * 0.9), int(c[1] * 0.88), int(c[2] * 0.8), 255)
    return img


def backrooms_carpet():
    """Moist, stained, brownish-yellow carpet."""
    rng = random.Random(2)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = jitter(rng, (164, 142, 82), 10)
    for _ in range(2):                                        # faint wet patches
        cx, cy, r = rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(1.5, 2.5)
        for y in range(16):
            for x in range(16):
                d = min(math.hypot(x - cx + dx, y - cy + dy) for dx in (-16, 0, 16) for dy in (-16, 0, 16))
                if d < r:
                    c = px[x, y]
                    px[x, y] = (int(c[0] * 0.92), int(c[1] * 0.9), int(c[2] * 0.85), 255)
    return img


def backrooms_ceiling():
    """Off-white acoustic ceiling tile with a grid edge and pinholes."""
    rng = random.Random(3)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x == 0 or y == 0
            px[x, y] = (170, 164, 140, 255) if edge else jitter(rng, (218, 211, 182), 4)
    for _ in range(14):
        px[rng.randrange(2, 15), rng.randrange(2, 15)] = (182, 175, 150, 255)
    return img


def fluorescent_light(lit):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = (176, 172, 156)
            elif x in (1, 14) or y in (1, 14):
                c = (205, 201, 186)
            elif lit:
                c = (255, 255, 244) if y % 5 != 3 else (236, 238, 226)   # three tubes behind the diffuser
            else:
                c = (132, 131, 122) if y % 5 != 3 else (118, 117, 110)
            px[x, y] = (*c, 255)
    return img


# --------------------------------------------------------------------------- research items

PAPER = (232, 221, 188)
INK = (70, 60, 50)


def journal_page(page):
    """A torn scrap of paper with scribbles. Each page has a different tear and doodle."""
    rng = random.Random(10 + page)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(2, 15):
        for x in range(3, 13):
            torn = (y == 2 and rng.random() < 0.4) or (x == 12 and rng.random() < 0.35)
            if not torn:
                px[x, y] = jitter(rng, PAPER, 6)
    for row in range(4, 13, 2):                               # handwriting
        length = rng.randint(4, 8)
        for x in range(4, 4 + length):
            if rng.random() < 0.8 and px[x, row][3]:
                px[x, row] = (*INK, 255)
    if page == 1:                                             # a hole in the floor
        for x, y in ((9, 10), (10, 10), (9, 11), (10, 11)):
            px[x, y] = (30, 30, 30, 255)
    elif page == 2:                                           # a yellow smear
        for x, y in ((8, 7), (9, 7), (9, 8), (10, 8), (8, 8)):
            px[x, y] = (214, 190, 80, 255)
    else:                                                     # green portal-ink circle
        for x, y in ((8, 9), (9, 9), (10, 10), (10, 11), (9, 12), (8, 12), (7, 11), (7, 10)):
            px[x, y] = (80, 230, 50, 255)
    add_outline(img)
    return img


def backrooms_coordinates():
    """A clean sheet with glowing green coordinates and a yellow corner."""
    rng = random.Random(42)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(2, 15):
        for x in range(3, 13):
            px[x, y] = jitter(rng, (240, 236, 220), 3)
    for row in (4, 6, 8, 10):
        for x in range(4, 12):
            if rng.random() < 0.75:
                px[x, row] = (70, 220, 60, 255)
    for x, y in ((11, 12), (12, 12), (12, 13), (11, 13), (12, 11)):
        px[x, y] = (214, 190, 80, 255)
    add_outline(img)
    return img


# --------------------------------------------------------------------------- the Bacteria

# Box-UV layout (u, v, width, height, depth) of each part in BacteriaModel.java
BACTERIA_PARTS = {
    "head": (0, 0, 5, 9, 5),
    "body": (20, 0, 6, 14, 3),
    "right_arm": (40, 0, 2, 22, 2),
    "left_arm": (48, 0, 2, 22, 2),
    "right_leg": (0, 20, 3, 18, 3),
    "left_leg": (12, 20, 3, 18, 3),
}


def box_faces(u, v, w, h, d):
    """Texture rectangles (x, y, width, height) of a box's faces, Minecraft box-UV style."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def bacteria():
    """Near-black, slightly glossy skin with bony ridges; the face has hollow eyes and a slit mouth."""
    rng = random.Random(66)
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    sp, ep = skin.load(), eyes.load()
    for name, (u, v, w, h, d) in BACTERIA_PARTS.items():
        for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    base = 14 + rng.randint(0, 9)
                    if name.endswith(("arm", "leg")) and (x - fx) == fw // 2 and rng.random() < 0.6:
                        base += 12                                   # a bony ridge down each limb
                    if name == "body" and face == "front" and (y - fy) % 3 == 1 and (y - fy) < 9:
                        base += 10                                   # ribs
                    sp[x, y] = (base, base, base + 3, 255)
    fx, fy, _, _ = box_faces(*BACTERIA_PARTS["head"])["front"]
    for dx, dy in ((1, 2), (3, 2)):                                  # pinprick white eyes
        sp[fx + dx, fy + dy] = (235, 235, 225, 255)
        ep[fx + dx, fy + dy] = (255, 255, 245, 255)
    teeth = ((0, 5), (2, 5), (4, 5), (1, 8), (3, 8))                  # a wide, jagged grin
    throat = ((0, 6), (1, 6), (2, 6), (3, 6), (4, 6), (1, 7), (2, 7), (3, 7), (1, 5), (3, 5), (0, 7), (4, 7))
    for dx, dy in throat:
        sp[fx + dx, fy + dy] = (45, 4, 6, 255)
        ep[fx + dx, fy + dy] = (70, 8, 10, 255)
    for dx, dy in teeth:
        sp[fx + dx, fy + dy] = (205, 200, 185, 255)
        ep[fx + dx, fy + dy] = (120, 118, 110, 255)
    return skin, eyes


def portal_fluid_drop():
    """A single glowing droplet."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            if cy >= 9:
                inside = math.hypot(cx - 8, cy - 10) <= 3.6
            else:
                inside = abs(cx - 8) <= (cy - 2.5) * 0.55                # pointed top
            if inside and cy > 2.5:
                shine = (cx - 6.6) ** 2 + (cy - 9.4) ** 2 < 1.3
                px[x, y] = CORE if shine else (LIME if cx < 8.5 else GREEN)
    add_outline(img)
    return img


# --------------------------------------------------------------------------- Rick and Morty extras

def slide():
    """Glossy yellow playground plastic with a lighter streak."""
    rng = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = (236, 192, 40) if (x + y) % 11 not in (0, 1) else (252, 222, 110)
            px[x, y] = jitter(rng, c, 4)
    for x in range(16):
        px[x, 0] = (200, 150, 20, 255)
        px[x, 15] = (200, 150, 20, 255)
    return img


def meeseeks():
    """64x32 humanoid skin: all blue, with a simple Meeseeks face."""
    rng = random.Random(8)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    blue = (96, 178, 232)
    parts = [(0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)]
    for u, v, w, h, d in parts:
        for fx, fy, fw, fh in box_faces(u, v, w, h, d).values():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    px[x, y] = jitter(rng, blue, 5)
    fx, fy, _, _ = box_faces(0, 0, 8, 8, 8)["front"]
    for dx, dy in ((1, 2), (2, 2), (5, 2), (6, 2)):          # big round eyes
        px[fx + dx, fy + dy] = (250, 250, 250, 255)
    px[fx + 2, fy + 3] = (250, 250, 250, 255)
    px[fx + 5, fy + 3] = (250, 250, 250, 255)
    px[fx + 1, fy + 3] = (20, 20, 30, 255)
    px[fx + 6, fy + 3] = (20, 20, 30, 255)
    for dx in range(3, 5):                                    # nose
        px[fx + dx, fy + 4] = (70, 150, 210, 255)
    for dx in range(2, 6):                                    # wide smile
        px[fx + dx, fy + 6] = (40, 30, 60, 255)
    px[fx + 1, fy + 5] = (40, 30, 60, 255)
    px[fx + 6, fy + 5] = (40, 30, 60, 255)
    return img


def item_sprite(rows, palette):
    """Builds a 16x16 sprite from a list of 16 strings; '.' is transparent."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch]
    return img


def meeseeks_box():
    return item_sprite([
        "................",
        "................",
        "......kkkk......",
        "......kwwk......",
        "...kkkkkkkkkk...",
        "...kBBBBBBBBk...",
        "...kBbbbbbbBk...",
        "...kBbBBBBbBk...",
        "...kBbBBBBbBk...",
        "...kBbBBBBbBk...",
        "...kBbBBBBbBk...",
        "...kBbbbbbbBk...",
        "...kBBBBBBBBk...",
        "...kddddddddk...",
        "...kkkkkkkkkk...",
        "................"],
        {"k": (25, 35, 60, 255), "w": (235, 235, 235, 255), "B": (70, 150, 225, 255), "b": (120, 190, 245, 255), "d": (45, 100, 170, 255)})


def plumbus():
    return item_sprite([
        "................",
        "........kk......",
        ".......kppk.....",
        "......kpPPk.....",
        ".....kpPPpk.....",
        "....kkpppkkk....",
        "...kppkkkppk....",
        "..kpPPpppPPpk...",
        "..kpPPPPPPPpk...",
        "..kppPPPPPppk...",
        "...kkppppkkk....",
        ".....kkbbk......",
        ".....kbbbbk.....",
        "....kbbbbbbk....",
        "....kkkkkkkk....",
        "................"],
        {"k": (70, 30, 50, 255), "p": (220, 120, 160, 255), "P": (245, 170, 200, 255), "b": (200, 150, 120, 255)})


def ricks_flask():
    return item_sprite([
        "................",
        "......kkk.......",
        "......kgk.......",
        ".....kkkkk......",
        "....kssssSk.....",
        "...ksssssSSk....",
        "...kssWssSSk....",
        "...ksWssssSk....",
        "...kssssssSk....",
        "...ksssssSSk....",
        "...kssssSSSk....",
        "...kssssSSSk....",
        "....kSSSSSk.....",
        ".....kkkkk......",
        "................",
        "................"],
        {"k": (40, 45, 50, 255), "g": (120, 120, 120, 255), "s": (190, 198, 205, 255), "S": (140, 150, 160, 255), "W": (245, 250, 255, 255)})


def szechuan_sauce():
    return item_sprite([
        "................",
        "................",
        "................",
        "....kkkkkkkk....",
        "...kwwwwwwwwk...",
        "...kwrrwwrrwk...",
        "...kkkkkkkkkk...",
        "...krrrrrrrrk...",
        "...krRRRRRRrk...",
        "...krRyyyyRrk...",
        "...krRyRRyRrk...",
        "...krRRRRRRrk...",
        "....krrrrrrk....",
        ".....kkkkkk.....",
        "................",
        "................"],
        {"k": (60, 25, 20, 255), "w": (240, 235, 225, 255), "r": (170, 40, 30, 255), "R": (205, 70, 40, 255), "y": (250, 200, 60, 255)})


# --------------------------------------------------------------------------- v0.5 additions

def pickle_rick():
    """32x32 skin for PickleModel: bumpy green pickle with Rick's face on the front."""
    rng = random.Random(9)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    parts = [(0, 0, 6, 12, 6), (0, 18, 4, 2, 4), (16, 18, 4, 2, 4), (24, 0, 1, 3, 2), (24, 5, 1, 3, 2)]
    for u, v, w, h, d in parts:
        for fx, fy, fw, fh in box_faces(u, v, w, h, d).values():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    c = (96, 150, 52) if rng.random() > 0.18 else (70, 118, 38)   # warty spots
                    if rng.random() < 0.06:
                        c = (140, 190, 80)
                    px[x, y] = jitter(rng, c, 5)
    fx, fy, _, _ = box_faces(0, 0, 6, 12, 6)["front"]
    for dx in range(0, 6):
        px[fx + dx, fy + 2] = (60, 70, 60, 255)                       # the unibrow
    for dx in (1, 4):
        px[fx + dx, fy + 3] = (245, 245, 240, 255)                    # eyes
        px[fx + dx, fy + 4] = (245, 245, 240, 255)
    px[fx + 1, fy + 4] = (25, 25, 25, 255)
    px[fx + 4, fy + 4] = (25, 25, 25, 255)
    for dx in range(1, 5):                                            # "I'm Pickle Riiick!"
        px[fx + dx, fy + 7] = (230, 225, 210, 255)
        px[fx + dx, fy + 8] = (120, 30, 30, 255)
    return img


def sanity_icons():
    """27x9: full eye, half eye, empty eye."""
    img = Image.new("RGBA", (27, 9), (0, 0, 0, 0))
    px = img.load()
    shape = ["..kkkkk..", ".kwwwwwk.", "kwwiiiwwk", "kwiippiwk", "kwiippiwk", "kwwiiiwwk", ".kwwwwwk.", "..kkkkk..", "........."]
    for i, mode in enumerate(("full", "half", "empty")):
        for y, row in enumerate(shape):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                if ch == "k":
                    c = (30, 20, 40, 255)
                elif mode == "empty" or (mode == "half" and x < 4):
                    c = (70, 60, 80, 200)
                else:
                    c = {"w": (235, 235, 225, 255), "i": (140, 80, 200, 255), "p": (20, 10, 30, 255)}[ch]
                px[i * 9 + x, y] = c
    return img


def sanity_vignette(size=256):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        for x in range(size):
            r = math.hypot((x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1) / 1.1
            a = max(0.0, min(1.0, (r - 0.45) / 0.55)) ** 1.6
            px[x, y] = (0, 0, 0, int(255 * a))
    return img


def jumpscare(size=256):
    """The Bacteria's face, right up close."""
    rng = random.Random(13)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        for x in range(size):
            nx, ny = (x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1
            head = (nx / 0.72) ** 2 + (ny / 0.98) ** 2
            if head > 1:
                continue
            g = 8 + rng.randint(0, 14)
            c = (g, g, g + 3)
            for ex in (-0.3, 0.3):                                       # eyes
                if ((nx - ex) / 0.13) ** 2 + ((ny + 0.28) / 0.1) ** 2 < 1:
                    c = (250, 250, 240)
            mouth = (nx / 0.5) ** 2 + ((ny - 0.35) / 0.3) ** 2
            if mouth < 1:
                c = (70 + rng.randint(0, 20), 4, 8)
                if abs(ny - 0.07) < 0.06 or abs(ny - 0.63) < 0.06:        # rows of teeth
                    if int((nx + 1) * 18) % 2 == 0:
                        c = (220, 214, 196)
            px[x, y] = (*c, 255)
    return img


def almond_water():
    return item_sprite([
        "................",
        "......kkkk......",
        "......kcck......",
        "......kkkk......",
        ".......kgk......",
        "......kggk......",
        ".....kggggk.....",
        "....kwwwwwwk....",
        "....kwWwwwwk....",
        "....kwwwwwwk....",
        "....kWwwwwwk....",
        "....kwwwwwmk....",
        "....kwwwwmmk....",
        ".....kmmmmk.....",
        "......kkkk......",
        "................"],
        {"k": (50, 50, 60, 255), "c": (150, 110, 70, 255), "g": (210, 235, 240, 255),
         "w": (240, 232, 205, 255), "W": (255, 252, 240, 255), "m": (215, 200, 165, 255)})


def targeting_chip():
    return item_sprite([
        "................",
        "................",
        "...y.y.y.y.y....",
        "..kkkkkkkkkkk...",
        "y.kgggggggggk.y.",
        "..kgGGGGGGGgk...",
        "y.kgGkkkkkGgk.y.",
        "..kgGkrrkkGgk...",
        "y.kgGkrrkkGgk.y.",
        "..kgGkkkkkGgk...",
        "y.kgGGGGGGGgk.y.",
        "..kgggggggggk...",
        "..kkkkkkkkkkk...",
        "...y.y.y.y.y....",
        "................",
        "................"],
        {"k": (25, 40, 30, 255), "g": (40, 120, 60, 255), "G": (90, 200, 90, 255),
         "r": (255, 60, 50, 255), "y": (220, 180, 60, 255)})


def microverse_battery_side():
    rng = random.Random(21)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = (60, 64, 72) if y > 1 else (40, 42, 48)
            if y in (5, 11):
                c = (46, 49, 56)
            px[x, y] = jitter(rng, c, 4)
    for x in range(2, 14):                                   # yellow warning stripe
        px[x, 13] = (220, 180, 40, 255) if (x // 2) % 2 == 0 else (30, 30, 30, 255)
    return img


def microverse_battery_top():
    rng = random.Random(22)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = jitter(rng, (54, 58, 66), 4)
    for x, y in ((4, 4), (5, 4), (10, 4), (11, 4)):
        px[x, y] = (200, 60, 50, 255) if x < 8 else (60, 90, 200, 255)
    return img


def microverse_window():
    """A tiny planet in a tiny sky."""
    rng = random.Random(23)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = (10, 12, 40)
            if rng.random() < 0.06:
                c = (230, 230, 255)
            r = math.hypot(x - 7.5, y - 8.5)
            if r < 4.5:
                c = mix((80, 200, 110), (40, 110, 200), (x + y) / 30) if (x * 3 + y) % 5 else (230, 240, 230)
            elif r < 5.2:
                c = (120, 255, 150)
            px[x, y] = (*c, 255)
    return img


def microverse_wire():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = (190, 110, 60) if (x + y) % 4 else (150, 80, 45)
            if 6 <= y <= 9:
                c = (120, 255, 140) if 7 <= y <= 8 else (60, 180, 90)
            px[x, y] = (*c, 255)
    return img


def main():
    for folder in ("item", "entity", "block"):
        (ROOT / folder).mkdir(parents=True, exist_ok=True)
    portal_fluid().save(ROOT / "item/portal_fluid.png")
    for suffix, image in (("", show_portal()), ("_swirl", show_portal_swirl()), ("_glow", portal_glow())):
        image.save(ROOT / f"entity/portal{suffix}.png")
        to_gold(image).save(ROOT / f"entity/portal_gold{suffix}.png")
    projectile_orb().save(ROOT / "entity/portal_projectile.png")
    backrooms_wallpaper().save(ROOT / "block/backrooms_wallpaper.png")
    backrooms_carpet().save(ROOT / "block/backrooms_carpet.png")
    backrooms_ceiling().save(ROOT / "block/backrooms_ceiling.png")
    fluorescent_light(True).save(ROOT / "block/fluorescent_light.png")
    fluorescent_light(False).save(ROOT / "block/fluorescent_light_off.png")
    for page in (1, 2, 3):
        journal_page(page).save(ROOT / f"item/journal_page_{page}.png")
    backrooms_coordinates().save(ROOT / "item/backrooms_coordinates.png")
    portal_fluid_drop().save(ROOT / "item/portal_fluid_drop.png")
    skin, eyes = bacteria()
    skin.save(ROOT / "entity/bacteria.png")
    eyes.save(ROOT / "entity/bacteria_eyes.png")
    slide().save(ROOT / "block/slide.png")
    meeseeks().save(ROOT / "entity/meeseeks.png")
    meeseeks_box().save(ROOT / "item/meeseeks_box.png")
    plumbus().save(ROOT / "item/plumbus.png")
    ricks_flask().save(ROOT / "item/ricks_flask.png")
    szechuan_sauce().save(ROOT / "item/szechuan_sauce.png")
    (ROOT / "gui").mkdir(parents=True, exist_ok=True)
    pickle_rick().save(ROOT / "entity/pickle_rick.png")
    sanity_icons().save(ROOT / "gui/sanity.png")
    sanity_vignette().save(ROOT / "gui/sanity_vignette.png")
    jumpscare().save(ROOT / "gui/jumpscare.png")
    almond_water().save(ROOT / "item/almond_water.png")
    targeting_chip().save(ROOT / "item/targeting_chip.png")
    microverse_battery_side().save(ROOT / "block/microverse_battery.png")
    microverse_battery_top().save(ROOT / "block/microverse_battery_top.png")
    microverse_window().save(ROOT / "block/microverse_window.png")
    microverse_wire().save(ROOT / "block/microverse_wire.png")
    print("Textures written to", ROOT)


if __name__ == "__main__":
    main()
