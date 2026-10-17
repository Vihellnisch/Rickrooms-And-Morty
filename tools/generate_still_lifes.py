"""Still Lifes of people: sounds, names, spawn eggs, the advancement. Run from the project root:

    python tools/generate_still_lifes.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_data import ASSETS, DATA, M, advancement, write  # noqa: E402

FORMS = {"captain": "The Captain", "three_faces": "Three Faces", "twice": "Twice", "folded": "The Folded One", "many_hands": "Many Hands",
         "commuter": "The Commuter", "lopsided": "Lopsided", "noodle_arms": "Noodle Arms", "plank_face": "Plank Face", "gaping": "The Gaping One"}


def main():
    for form in FORMS:
        write(ASSETS / f"models/item/still_life_{form}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    write(DATA / "tags/entity_type/human_still_lifes.json", {"values": [M + f"still_life_{f}" for f in FORMS]})
    advancement("thats_not_you", "minecraft:player_head", "backrooms", {"kill": {"trigger": "minecraft:player_killed_entity", "conditions": {
        "entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "#" + M + "human_still_lifes"}}]}}})

    sounds_path = ASSETS / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    ev = lambda name, pitch=1.0, volume=1.0: {"name": name, "type": "event", "pitch": pitch, "volume": volume}
    for key, entries in {
        "still_life.voice": [ev("minecraft:entity.villager.ambient", 0.5, 0.8), ev("minecraft:entity.villager.no", 0.45, 0.8),
                             ev("minecraft:entity.wandering_trader.ambient", 0.5, 0.7)],
        "still_life.wake": [ev("minecraft:entity.villager.hurt", 0.4, 1.0)],
        "still_life.hurt": [ev("minecraft:entity.player.hurt", 0.6)],
        "still_life.death": [ev("minecraft:entity.player.death", 0.5)],
        "still_life.click": [ev("minecraft:block.bone_block.step", 0.6, 0.8), ev("minecraft:block.wood.hit", 0.5, 0.6)],
    }.items():
        sounds[key] = {"subtitle": f"subtitles.rickrooms.{key}", "sounds": entries}
    write(sounds_path, sounds)

    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    for form, name in FORMS.items():
        lang[f"entity.rickrooms.still_life_{form}"] = f"Still Life ({name})"
        lang[f"item.rickrooms.still_life_{form}_spawn_egg"] = f"Still Life ({name}) Spawn Egg"
    lang.update({
        "subtitles.rickrooms.still_life.voice": "Still Life mumbles",
        "subtitles.rickrooms.still_life.wake": "Still Life wakes",
        "subtitles.rickrooms.still_life.hurt": "Still Life hurts",
        "subtitles.rickrooms.still_life.death": "Still Life dies",
        "subtitles.rickrooms.still_life.click": "Something clicks",
        "advancements.rickrooms.thats_not_you.title": "That's Not You",
        "advancements.rickrooms.thats_not_you.description": "Kill a Still Life wearing someone's face",
        "guide.rickrooms.page.22": "STILL LIFES, OF US\n\nIt copies people too. It gets them wrong. Too tall. Too many faces. Too many hands. Faces in planks.\n\n"
                                      "They wear your skin. They stand still until you're close. Then they don't.",
    })
    write(path, lang)
    print("Still Lifes written")


if __name__ == "__main__":
    main()
