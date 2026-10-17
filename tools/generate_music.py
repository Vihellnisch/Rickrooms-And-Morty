"""v0.25: a theme for every level. The tracks are Kevin MacLeod's (incompetech.com, CC BY 4.0),
already converted to assets/rickrooms/sounds/music/<level>.ogg: all brought to the same loudness
(-24 LUFS, about as loud as the game's own music), faded in and out, at most five minutes. This
adds their sound events and points each level's biome at its track. Run it last, after any
generator that rewrites the biomes. From the project root:

    python tools/generate_music.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_data import ASSETS, DATA, M, write  # noqa: E402

# the level's track (sounds/music/<track>.ogg), its title, and the biomes it plays in
TRACKS = {
    "backrooms": ("Long Note Four", ["backrooms"]),
    "level_1": ("Deep Haze", ["level_1"]),
    "level_4": ("Local Forecast - Elevator", ["level_4"]),
    "level_188": ("Echoes of Time", ["level_188"]),
    "the_harvest": ("Ghost Story", ["the_harvest"]),
    "sunlit_lobby": ("Gymnopedie No 1", ["sunlit_lobby"]),
    "wooden_hotel": ("Lost Time", ["wooden_hotel"]),
    "scaffold_void": ("Ether Vox", ["scaffold_void"]),
    "glowstone_cavern": ("Ossuary 6 - Air", ["glowstone_cavern"]),
    "poolrooms": ("Dreamy Flashback", ["poolrooms"]),
    "level_fun": ("Monkeys Spinning Monkeys", ["level_fun"]),
    "infinite_ikea": ("Airport Lounge", ["infinite_ikea"]),
    "dead_mall": ("Lobby Time", ["dead_mall"]),
    "endless_field": ("Clean Soul", ["endless_field"]),
    "abandoned_city": ("Lightless Dawn", ["abandoned_city"]),
    "drop_limit": ("Floating Cities", ["drop_limit"]),
    "the_facade": ("Dark Fog", ["the_facade"]),
    "the_staircase": ("Mesmerize", ["the_staircase"]),
    "froopyland": ("Carefree", ["froopyland"]),
    "run_for_your_life": ("Volatile Reaction", ["run_for_your_life"]),
    "cronenberg": ("Darkest Child", ["blood_sea", "flesh_plains", "cronenberg_suburbs", "bone_forest", "blood_marsh", "tumor_wastes"]),
}
# a little under full: the player's own Music slider still turns it down further
VOLUME = 0.8
# quiet between plays, in ticks (20 a second); the chase plays again almost at once
DELAYS = {"run_for_your_life": (100, 400)}


def main():
    path = ASSETS / "sounds.json"
    sounds = json.loads(path.read_text(encoding="utf-8"))
    for key in TRACKS:
        sounds[f"music.{key}"] = {"sounds": [{"name": f"{M}music/{key}", "stream": True, "volume": VOLUME}]}
    write(path, sounds)
    for key, (_, biomes) in TRACKS.items():
        low, high = DELAYS.get(key, (1200, 3600))
        for biome in biomes:
            biome_path = DATA / f"worldgen/biome/{biome}.json"
            data = json.loads(biome_path.read_text(encoding="utf-8"))
            data["effects"]["music"] = {"sound": f"{M}music.{key}", "min_delay": low, "max_delay": high, "replace_current_music": True}
            write(biome_path, data)
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    lang["guide.rickrooms.page.31"] = ("CREDITS\n\nThe music on every level is by Kevin MacLeod (incompetech.com), "
                                          "licensed under Creative Commons: By Attribution 4.0.")
    write(lang_path, lang)
    print(f"{len(TRACKS)} level themes wired up")


if __name__ == "__main__":
    main()
