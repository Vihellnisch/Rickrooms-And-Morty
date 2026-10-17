"""v0.22: the DNA Lock Upgrade for the Portal Gun (chip art, recipes, translations) and the
/rickrooms gun command's text. Run from the project root:

    python tools/generate_v22.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_art import sprite  # noqa: E402
from generate_cronenberg_data import ASSETS, DATA, M, write  # noqa: E402

TEX = ASSETS / "textures"

CHIP = sprite([
    "................",
    "..kkkkkkkkkkkk..",
    "..kGGGGGGGGGGk..",
    "..kGyGGGGGGyGk..",
    "..kGGkkkkkkGGk..",
    "..kGkPbbbbpkGk..",
    "..kGkbPbbpbkGk..",
    "..kGkbbwwbbkGk..",
    "..kGkbbwwbbkGk..",
    "..kGkbpbbPbkGk..",
    "..kGkpbbbbPkGk..",
    "..kGGkkkkkkGGk..",
    "..kGyGGGGGGyGk..",
    "..kkkkkkkkkkkk..",
    "...y..y..y..y...",
    "................"], {"k": (30, 30, 34), "G": (40, 110, 60), "y": (230, 190, 60), "b": (24, 20, 40),
                          "P": (230, 90, 200), "p": (120, 200, 255), "w": (240, 240, 255)})

LANG = {
    "item.rickrooms.dna_lock_chip": "DNA Lock Upgrade",
    "tooltip.rickrooms.dna_lock_chip": "Smithing template: Portal Gun + Totem of Undying = it comes back with you when you die.",
    "tooltip.rickrooms.portal_gun.dna_lock": "DNA Lock: stays with you when you die",
    "commands.rickrooms.gun.done": "Gave a fully upgraded Portal Gun to %s player(s)",
    "guide.rickrooms.page.4": "UPGRADES\n\nSmithing table.\n\nTargeting + diamond: shots zap creatures through.\n\nBlender + mantis scythe: attack mode. Press R.\n\nDNA Lock + totem: it comes back when you die.",
}


def ing(name):
    return {"item": name if ":" in name else M + name}


def main():
    CHIP.save(TEX / "item/dna_lock_chip.png")
    write(ASSETS / "models/item/dna_lock_chip.json", {"parent": "minecraft:item/generated",
                                                      "textures": {"layer0": "rickrooms:item/dna_lock_chip"}})
    write(DATA / "recipe/dna_lock_chip.json", {"type": "minecraft:crafting_shaped", "category": "misc",
                                               "pattern": ["GEG", "EFE", "GCG"],
                                               "key": {"G": ing("minecraft:gold_ingot"), "E": ing("minecraft:echo_shard"),
                                                       "F": ing("portal_fluid"), "C": ing("minecraft:recovery_compass")},
                                               "result": {"id": M + "dna_lock_chip", "count": 1}})
    write(DATA / "recipe/portal_gun_dna_lock.json", {"type": "minecraft:smithing_transform", "template": ing("dna_lock_chip"),
                                                     "base": ing("portal_gun"), "addition": ing("minecraft:totem_of_undying"),
                                                     "result": {"id": M + "portal_gun", "count": 1,
                                                                "components": {M + "dna_lock": True}}})
    path = ASSETS / "lang/en_us.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(LANG)
    write(path, data)


if __name__ == "__main__":
    main()
