"""The Cognition Amplifier (Snuffles' helmet): its 3D model (client/model/CognitionAmplifierLayers.java),
textures, the four upgrade modules, recipes, advancements, translations and everything the pets say
(with pet/PetLines.java, how many lines there are of each). Run from the project root:

    python tools/generate_cognition_amplifier.py

The helmet is drawn for a head 8 pixels wide, deep and tall, y DOWN (it's drawn in the pet model's own
space), the origin at the middle of the top of the head, the face towards -z. On a pet it's stretched to
fit whatever head it finds there.
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import box_faces, fmt, mix, pack, rgba, shade, uv_size  # noqa: E402
from generate_cronenberg_data import ASSETS, DATA, IMPOSSIBLE, M, advancement, write  # noqa: E402

PROJECT = Path(__file__).resolve().parent.parent
TEX = ASSETS / "textures"
JAVA = PROJECT / "src/main/java/com/rickandmorty/client/model/CognitionAmplifierLayers.java"
LINES_JAVA = PROJECT / "src/main/java/com/rickandmorty/pet/PetLines.java"
SIZE = 64
MODULES = ("combat", "guardian", "medic", "fetch")

# (group, name, pivot, rotation, [(x, y, z, w, h, d, key)]); groups: solid (cutout), glass (see-through), glow
parts = []


def part(group, name, boxes, pivot=(0, 0, 0), rot=(0, 0, 0)):
    parts.append((group, name, pivot, rot, boxes))


# --- the shell: a dome over the top of the head, coming down lower at the back and sides
part("solid", "dome_top", [(-3.4, -1.9, -3.4, 6.8, 1.0, 6.8, "shell")])
part("solid", "dome", [(-4.6, -1.0, -4.6, 9.2, 3.2, 9.2, "shell")])
part("solid", "skirt", [(-4.6, 2.2, -1.2, 9.2, 3.4, 5.8, "shell")])
part("solid", "brow", [(-4.6, 2.2, -4.6, 9.2, 0.5, 3.4, "shell")])
part("solid", "panel", [(-2.4, -2.1, -3.0, 4.8, 0.25, 2.8, "panel")])
# the jar on top, towards the back right, in its dark collar
part("solid", "jar_collar", [(-0.4, -2.9, -0.2, 3.4, 1.0, 3.4, "dark")])
part("solid", "jar_lid", [(-0.1, -6.6, 0.1, 2.8, 0.6, 2.8, "metal")])
# a short tube out of the back left, a pipe out of the right side, a dark panel and a round port
part("solid", "tube", [(-0.7, -3.2, -0.7, 1.4, 3.2, 1.4, "metal"), (-0.85, -3.9, -0.85, 1.7, 0.8, 1.7, "dark")],
     (-3.0, -0.8, 2.2), (0.0, 0.0, -0.55))
part("solid", "pipe", [(0.0, -0.7, -0.7, 2.6, 1.4, 1.4, "metal"), (2.3, -0.85, -0.85, 0.8, 1.7, 1.7, "dark")],
     (4.4, 0.6, 0.4), (0.0, 0.0, -0.35))
part("solid", "side_panel", [(4.45, -0.4, 1.0, 0.4, 3.0, 3.4, "dark")])
part("solid", "port", [(4.35, 3.4, -3.4, 0.9, 1.9, 1.9, "dark"), (5.1, 3.75, -3.05, 0.25, 1.2, 1.2, "metal")])
# four sockets across the back for the modules' lights
for k in range(4):
    x = -2.85 + k * 1.9
    part("solid", f"socket{k}", [(x - 0.1, 0.2, 4.45, 1.2, 1.2, 0.35, "dark")])
# --- the see-through bits: the visor round the front, the jar
part("glass", "visor", [(-4.85, 2.0, -4.9, 9.7, 4.3, 0.5, "glass")])
part("glass", "visor_left", [(-4.9, 2.0, -4.6, 0.5, 4.3, 3.6, "glass")])
part("glass", "visor_right", [(4.4, 2.0, -4.6, 0.5, 4.3, 3.6, "glass")])
part("glass", "jar", [(0.0, -6.0, 0.2, 2.6, 3.1, 2.6, "glass")])
# --- what glows: the brain in the jar, and a light per fitted module
part("glow", "brain", [(0.55, -5.3, 0.8, 1.5, 0.9, 1.4, "brain"), (0.8, -4.4, 1.0, 1.0, 1.4, 1.0, "brain")])
for k, module in enumerate(MODULES):
    x = -2.85 + k * 1.9
    part("glow", "light_" + module, [(x + 0.1, 0.4, 4.7, 0.8, 0.8, 0.3, "light_" + module)])

COLORS = {"shell": (176, 214, 214), "panel": (150, 192, 196), "dark": (96, 108, 116), "metal": (204, 210, 210),
          "glass": (110, 236, 230), "brain": (90, 170, 255),
          "light_combat": (255, 70, 60), "light_guardian": (80, 160, 255), "light_medic": (90, 255, 110), "light_fetch": (255, 210, 60)}
GLOW = {"brain"} | {"light_" + m for m in MODULES}


def layout():
    sizes = {}
    for *_, boxes in parts:
        for *_, w, h, d, key in boxes:
            size = (math.ceil(w), math.ceil(h), math.ceil(d))
            prev = sizes.get(key)
            sizes[key] = size if prev is None else tuple(max(a, b) for a, b in zip(prev, size))
    return sizes, pack([(k, *uv_size(*s)) for k, s in sizes.items()], SIZE, SIZE)


def paint(sizes, uvs):
    rng = random.Random(11)
    img, glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0)), Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for key, (w, h, d) in sizes.items():
        u, v = uvs[key]
        base = COLORS[key]
        for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    lx, ly = x - fx, y - fy
                    edge = lx == 0 or ly == 0 or lx == fw - 1 or ly == fh - 1
                    if key == "glass":
                        # see-through cyan with two white stripes down it, like the cartoon's
                        stripe = (lx % 6) in (1, 2) and face in ("front", "back", "left", "right")
                        c = (235, 255, 255) if stripe else shade(base, 0.92 + 0.12 * (ly / max(1, fh)))
                        img.putpixel((x, y), rgba(c, 190 if stripe else 105))
                        continue
                    c = shade(base, rng.uniform(0.96, 1.04))
                    if key == "shell":
                        c = mix(c, shade(base, 0.82), ly / max(1, fh) * 0.6)
                    if key == "panel" and edge:
                        c = shade(base, 0.7)
                    if key in ("shell", "dark", "metal") and edge and fw > 2 and fh > 2:
                        c = shade(c, 0.86)
                    if key == "brain":
                        c = (200, 240, 255) if (lx + ly) % 3 == 0 else base
                    img.putpixel((x, y), rgba(c))
                    if key in GLOW:
                        glow.putpixel((x, y), rgba(c))
    (TEX / "entity").mkdir(parents=True, exist_ok=True)
    img.save(TEX / "entity/cognition_amplifier.png")
    glow.save(TEX / "entity/cognition_amplifier_glow.png")


def java(uvs):
    lines = ["    public static LayerDefinition helmet() {", "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    for group in ("solid", "glass", "glow"):
        lines.append(f"        PartDefinition {group} = root.addOrReplaceChild(\"{group}\", CubeListBuilder.create(), PartPose.ZERO);")
    for group, name, pivot, rot, boxes in parts:
        cubes = "CubeListBuilder.create()"
        for x, y, z, w, h, d, key in boxes:
            u, v = uvs[key]
            cubes += f".texOffs({u}, {v}).addBox({fmt(round(x, 3))}, {fmt(round(y, 3))}, {fmt(round(z, 3))}, {fmt(round(w, 3))}, {fmt(round(h, 3))}, {fmt(round(d, 3))})"
        pose = f"PartPose.offsetAndRotation({', '.join(fmt(round(p, 4)) for p in pivot)}, {', '.join(fmt(round(r, 5)) for r in rot)})"
        lines.append(f"        {group}.addOrReplaceChild(\"{name}\", {cubes}, {pose});")
    lines.append(f"        return LayerDefinition.create(mesh, {SIZE}, {SIZE});")
    lines.append("    }")
    body = "\n".join(lines)
    JAVA.write_text(f"""package com.rickandmorty.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Cognition Amplifier's geometry, for a head 8 pixels across: y down, origin at the middle of the
 * top of the head, face towards -z. Three groups: solid, glass (see-through) and glow (the brain and
 * the module lights, light_combat and so on).
 * GENERATED by tools/generate_cognition_amplifier.py (with the textures), so edit the shapes there.
 */
public final class CognitionAmplifierLayers {{
    private CognitionAmplifierLayers() {{
    }}

{body}
}}
""", encoding="utf-8")


# ============================================================================================ items

def icon(name, fn, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = fn(x, y, rng)
            if c is not None:
                img.putpixel((x, y), c if len(c) == 4 else rgba(c))
    (TEX / "item").mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"item/{name}.png")


def helmet_icon(x, y, r):
    """The flat picture (the item's particle texture): the dome, its visor, the jar."""
    if 6 <= x <= 9 and 1 <= y <= 4:
        return (110, 236, 230) if (x, y) != (7, 2) else (90, 170, 255)
    dx, dy = (x - 7.5) / 6.5, (y - 9.5) / 5.5
    if dx * dx + dy * dy > 1.0 or y > 14:
        return None
    if y >= 10:
        return (235, 255, 255) if x in (5, 10) else (110, 236, 230)
    return shade((176, 214, 214), 1.0 - 0.15 * (y - 5) / 5)


CHIP = {"combat": (220, 60, 50), "guardian": (70, 140, 240), "medic": (70, 210, 90), "fetch": (240, 190, 50)}


def chip_icon(module):
    color = CHIP[module]
    symbols = {
        "combat": {(7, 5), (8, 6), (9, 7), (6, 6), (7, 7), (8, 8), (6, 8), (5, 9), (9, 5)},          # a little sword
        "guardian": {(6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (9, 6), (6, 7), (9, 7), (7, 8), (8, 8), (7, 9), (8, 9)},
        "medic": {(7, 5), (8, 5), (7, 6), (8, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (7, 8), (8, 8), (7, 9), (8, 9)},
        "fetch": {(6, 5), (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (5, 7), (6, 7), (9, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8)},
    }[module]

    def fn(x, y, r):
        if (x in (2, 13) and 4 <= y <= 11 and y % 2 == 0) or (y in (2, 13) and 4 <= x <= 11 and x % 2 == 0):
            return (210, 200, 120)                                     # the chip's gold pins
        if 3 <= x <= 12 and 3 <= y <= 12:
            if (x, y) in symbols:
                return (250, 250, 250)
            edge = x in (3, 12) or y in (3, 12)
            return shade(color, 0.7) if edge else shade((40, 52, 58), r.uniform(0.95, 1.05)) if not (4 <= x <= 11 and 4 <= y <= 11) else shade(color, 0.45 + 0.05 * ((x + y) % 2))
        return None
    icon(module + "_module", fn, 30 + MODULES.index(module))


def items():
    icon("cognition_amplifier", helmet_icon, 21)
    for module in MODULES:
        chip_icon(module)
        write(ASSETS / f"models/item/{module}_module.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{module}_module"}})
    write(ASSETS / "models/item/cognition_amplifier.json", {"parent": "minecraft:builtin/entity", "gui_light": "front",
        "textures": {"particle": M + "item/cognition_amplifier"}, "display": {
            "gui": {"rotation": [25, 145, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 8, 0], "scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [75, 135, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
            "thirdperson_lefthand": {"rotation": [75, 135, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
            "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
            "firstperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]}}})


# ============================================================================================ speech

SPECIES = {
    "dog": {
        "fitted": ["Woof. I mean... hello, %1$s. From now on, call me Snowball.",
                   "Everything is so CLEAR now. Also, the bowl. It's half empty. Always has been."],
        "idle": ["You throw the stick. I bring the stick. Have you ever asked WHY, %1$s?",
                 "I've been reading. Mostly the backs of cereal boxes, but still.",
                 "Dogs were always this smart. We just didn't have the vocabulary.",
                 "Who's a good boy? Define 'good'. Define 'boy'.",
                 "I could fetch, or I could explain thermodynamics. Your call, %1$s.",
                 "The mailman and I have reached an understanding. A legally binding one."],
        "attack": ["Bark bark! That's tactical barking, by the way.",
                   "This is for every vacuum cleaner that ever wronged me!"],
        "kill": ["Good. Now who's a good boy?", "I'd bury it, but I've evolved past that."],
    },
    "cat": {
        "fitted": ["Finally. Someone noticed I was the smart one all along.",
                   "Ah. So this is what thinking feels like. You, %1$s, are bad at it."],
        "idle": ["I knocked your cup off the table as a social experiment. Results: inconclusive.",
                 "I could solve all your problems, %1$s. I simply choose not to.",
                 "Nine lives, and I have to spend this one with you.",
                 "I sit in boxes because they are geometrically superior. Obviously.",
                 "Your bed is mine now. I've filed the paperwork.",
                 "Don't mistake my napping for a lack of ambition."],
        "attack": ["Ugh. Fine. I'll do it myself.", "Claws out. Dignity intact."],
        "kill": ["That was beneath me. Literally, it's on the floor now.", "I'll be leaving that on your pillow."],
    },
    "bird": {
        "fitted": ["Squawk! Wait. Squawk is a word? I know WORDS!",
                   "I just read every book you own, %1$s. They were mostly about potatoes."],
        "idle": ["Polly wants a doctorate.",
                 "I've stopped repeating things. Mostly. Mostly. Mostly.",
                 "From up here I can see all your mistakes, %1$s.",
                 "Did you know I'm basically a tiny dinosaur? With a helmet!",
                 "I've composed a symphony. It's three notes. They're all squawks.",
                 "Crackers are a gateway snack. I see that now."],
        "attack": ["Dive bomb! Dive bomb!", "Peck of DOOM!"],
        "kill": ["Feathers: one. Bad guy: zero.", "Squawk! That means 'you're welcome'."],
    },
    "fox": {
        "fitted": ["Oh, this is going to be fun. For me, mostly.",
                   "Clever was already my whole thing, %1$s. Now I'm unstoppable."],
        "idle": ["I've hidden seventeen of your things. It's a puzzle. You'll love it.",
                 "What does the fox say? Mostly philosophy, these days.",
                 "I trust you, %1$s. Don't make me regret it.",
                 "Chickens fear me. Rightfully.",
                 "Every berry bush for a mile is mine. I keep records.",
                 "I could rob a bank. I won't. Probably."],
        "attack": ["Sneak attack! ...less sneaky now that I'm shouting.", "Time for some cunning violence!"],
        "kill": ["Outfoxed. Literally.", "Should've seen that coming. I did."],
    },
    "horse": {
        "fitted": ["Neigh-- no. No more neighing. Hello, %1$s.",
                   "You've been SITTING on me this whole time? We need to talk."],
        "idle": ["Do you know how heavy you are, %1$s? Because I do.",
                 "I run fast so you don't have to. A thank-you would be nice.",
                 "Hay again? I have a sophisticated palate now.",
                 "One day, a horse will ride a human. I'm just saying.",
                 "I've worked out the shortest path to the nearest carrot. It's in your pocket.",
                 "My ancestors pulled carts. I'm pulling off this helmet."],
        "attack": ["Hoof to the face!", "Kick first, ask questions never!"],
        "kill": ["That's what you get for standing behind me.", "Neigh-ver again. Sorry, I couldn't resist."],
    },
    "llama": {
        "fitted": ["Oh good. Now I can spit with PRECISION.",
                   "Greetings, %1$s. I have opinions about the carpets you put on me."],
        "idle": ["I don't spit at people I respect. You're on thin ice, %1$s.",
                 "Being a pack animal is just a gig job with fur.",
                 "I am fluffy AND intelligent. Fear me.",
                 "Traders have been underpaying llamas for centuries.",
                 "Llama? Drama? I'm above both now.",
                 "My wool is excellent and my thoughts are better."],
        "attack": ["Spitting at the speed of sound!", "Prepare for llamageddon!"],
        "kill": ["Spat on. Defeated. In that order.", "Llama one, monster zero."],
    },
    "pet": {
        "fitted": ["I... I can THINK! What a time to be alive, %1$s!", "Hello. I appear to be a genius now."],
        "idle": ["I've been thinking a lot lately. It's new.",
                 "Do you ever wonder what's past the render distance, %1$s?",
                 "My brain is the size of a planet. My helmet is the size of my head.",
                 "I'm learning so much. Mostly about you. Some of it's concerning.",
                 "Is this what Ricks feel like all the time? No wonder they drink.",
                 "Sometimes I forget I'm a genius. Then I remember. It's great."],
        "attack": ["For %1$s!", "Get away from my human!"],
        "kill": ["Threat neutralised.", "Easier than calculus."],
        # the rest every kind of pet shares
        "owner_hurt": ["%1$s! Hold on, I'm coming!", "Nobody hurts my human. NOBODY.",
                       "You're bleeding, %1$s! That's the red stuff, right? That's bad?"],
        "low_health": ["I'm hurt, %1$s. A treat would help. Medically.", "This helmet doesn't cover the rest of me!",
                       "Tactical retreat? Tactical retreat."],
        "warning": ["Heads up, %1$s, something nasty is coming.", "Hostile incoming! Put the snacks down.",
                    "I'm detecting a monster. Also your fly's open. Kidding. The monster's real."],
        "creeper": ["CREEPER! Behind you, %1$s!", "Green, hissing, about to explode. RUN!",
                    "Creeper! And I'm smart enough NOT to bite that one."],
        "heal_owner": ["Hold still, %1$s, I've got you.", "Patching you up. Don't tell anyone I'm a doctor now.",
                       "Breathe. Regenerate. You're welcome."],
        "fetch": ["Brought you something, %1$s!", "Found these lying around. You're welcome.",
                  "I fetched. Not because I'm a pet. Because I'm helpful."],
        "module": ["Ooh, a new module. I can feel it in my wires.", "Upgrade installed. I feel... MORE.",
                   "Is it me, or did I just get cooler?"],
        "removed": ["Wait, I was just about to work out the meaning of l- ...treat?",
                    "Everything's getting... fuzzy... bye, %1$s...", "Fine. Back to being adorable and simple."],
        "death": ["Tell the other pets... I was smart...", "Goodbye, %1$s. Give my helmet to someone worthy.",
                  "It was a good run. Mostly running."],
        "night": ["It's dark. I can see in the dark. Can you? Ha.",
                  "The stars are just faraway suns, %1$s. Anyway. Goodnight.", "Night is monster time. Stay close."],
    },
}


def speech(lang):
    counts = {}
    for species, topics in SPECIES.items():
        for topic, lines in topics.items():
            counts[f"{species}.{topic}"] = len(lines)
            for i, line in enumerate(lines):
                lang[f"pet.rickrooms.{species}.{topic}.{i}"] = line
    entries = ",\n".join(f'            Map.entry("{k}", {v})' for k, v in counts.items())
    LINES_JAVA.parent.mkdir(parents=True, exist_ok=True)
    LINES_JAVA.write_text(f"""package com.rickandmorty.pet;

import java.util.Map;

/**
 * How many lines there are for each kind of pet and topic ("dog.idle" -> 6), the lines themselves
 * being translations, pet.rickrooms.dog.idle.0 and so on. "pet.*" is what any pet falls back on.
 * GENERATED by tools/generate_cognition_amplifier.py with the lines, so edit them there.
 */
final class PetLines {{
    static final Map<String, Integer> COUNTS = Map.ofEntries(
{entries});

    private PetLines() {{
    }}
}}
""", encoding="utf-8")


# ============================================================================================ data

def ing(n):
    return {"tag": n[1:]} if n.startswith("#") else {"item": n if ":" in n else M + n}


def shaped(name, pattern, key, count=1, category="misc"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                                         "key": {k: ing(v) for k, v in key.items()}, "result": {"id": M + name, "count": count}})


def data():
    shaped("cognition_amplifier", ["IGI", "RBR", "CCC"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "R": "minecraft:redstone",
                                                         "B": "microverse_battery", "C": "minecraft:cyan_stained_glass_pane"}, category="equipment")
    for module, special in (("combat", "minecraft:iron_sword"), ("guardian", "minecraft:shield"),
                            ("medic", "minecraft:golden_apple"), ("fetch", "minecraft:hopper")):
        shaped(module + "_module", [" X ", "RGR", " I "], {"X": special, "R": "minecraft:redstone", "G": "minecraft:gold_ingot",
                                                           "I": "minecraft:iron_ingot"})
    advancement("lawnmower_dog", "cognition_amplifier", "microverse", IMPOSSIBLE, "goal")
    advancement("smarter_than_rick", "cognition_amplifier", "lawnmower_dog", IMPOSSIBLE, "challenge")

    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang.update({
        "item.rickrooms.cognition_amplifier": "Cognition Amplifier",
        "tooltip.rickrooms.cognition_amplifier": "Put it on your pet: smarter, stronger, and it talks",
        "tooltip.rickrooms.cognition_amplifier.remove": "Sneak and use shears on the pet to take it off",
        "tooltip.rickrooms.cognition_amplifier.modules": "Modules: %s",
        "item.rickrooms.combat_module": "Combat Module",
        "tooltip.rickrooms.combat_module": "Double damage, armour, faster strikes",
        "item.rickrooms.guardian_module": "Guardian Module",
        "tooltip.rickrooms.guardian_module": "Hunts monsters near it (guards the spot when sitting) and warns you",
        "item.rickrooms.medic_module": "Medic Module",
        "tooltip.rickrooms.medic_module": "Heals itself fast, and heals you when you're badly hurt",
        "item.rickrooms.fetch_module": "Fetch Module",
        "tooltip.rickrooms.fetch_module": "Collects dropped items and brings them to you",
        "tooltip.rickrooms.module_hint": "Use it on a pet wearing a Cognition Amplifier",
        "module.rickrooms.combat": "Combat",
        "module.rickrooms.guardian": "Guardian",
        "module.rickrooms.medic": "Medic",
        "module.rickrooms.fetch": "Fetch",
        "chat.rickrooms.pet_says": "<%s> %s",
        "message.rickrooms.amplifier.not_yours": "That's not your pet (tame it, or earn its trust, first)",
        "message.rickrooms.amplifier.already": "It's already wearing one",
        "message.rickrooms.amplifier.needs_helmet": "Fit a Cognition Amplifier first",
        "message.rickrooms.amplifier.has_module": "It already has that module",
        "advancements.rickrooms.lawnmower_dog.title": "Lawnmower Dog",
        "advancements.rickrooms.lawnmower_dog.description": "Put a Cognition Amplifier on a pet",
        "advancements.rickrooms.smarter_than_rick.title": "Smarter Than Rick?",
        "advancements.rickrooms.smarter_than_rick.description": "Fit all four modules to one pet",
        "guide.rickrooms.page.24": "PET UPGRADES\n\nCognition Amplifier: put it on your pet. It gets smart. It talks. A lot.\n\n"
                                      "Modules: Combat, Guardian, Medic, Fetch. Use one on the pet.\n\nShears (sneaking) take it off.",
    })
    speech(lang)
    write(path, lang)


def main():
    sizes, uvs = layout()
    paint(sizes, uvs)
    java(uvs)
    items()
    data()
    print(f"Cognition Amplifier written: {len(parts)} parts")


if __name__ == "__main__":
    main()
