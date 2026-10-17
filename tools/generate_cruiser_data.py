"""Data for Rick's Space Cruiser: recipes, damage types, advancements, sounds and translations.
Run from the project root:

    python tools/generate_cruiser_data.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_cronenberg_data import ASSETS, DATA, IMPOSSIBLE, M, advancement, write  # noqa: E402

ing = lambda n: {"item": n if ":" in n else M + n}


def shaped(name, pattern, key, count=1, category="equipment"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                                         "key": {k: ing(v) for k, v in key.items()}, "result": {"id": M + name, "count": count}})


def recipes():
    shaped("space_cruiser", ["GGG", "IBI", "III"], {"G": "minecraft:glass", "I": "minecraft:iron_block", "B": "microverse_battery"}, category="misc")
    write(DATA / "recipe/cruiser_remote.json", {"type": "minecraft:crafting_shapeless", "category": "tools", "ingredients": [
        ing("minecraft:iron_ingot"), ing("minecraft:redstone"), ing("minecraft:ender_pearl"), ing("minecraft:lime_dye")],
        "result": {"id": M + "cruiser_remote", "count": 1}})
    shaped("disintegrator_upgrade", [" P ", "EDE", "III"], {"P": "portal_fluid", "E": "minecraft:emerald", "D": "minecraft:diamond", "I": "minecraft:iron_ingot"})
    shaped("minigun_upgrade", ["III", "RRR", "IDI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "D": "minecraft:dispenser"})
    shaped("flamethrower_upgrade", ["IBI", "FDF", "I I"], {"I": "minecraft:iron_ingot", "B": "minecraft:blaze_rod", "F": "minecraft:fire_charge",
                                                             "D": "minecraft:dispenser"})
    shaped("rockets_upgrade", ["ITI", "FDF", "ITI"], {"I": "minecraft:iron_ingot", "T": "minecraft:tnt", "F": "minecraft:firework_rocket",
                                                        "D": "minecraft:dispenser"})
    shaped("laser_drill_upgrade", ["IAI", "APA", "IRI"], {"I": "minecraft:iron_ingot", "A": "minecraft:amethyst_shard", "P": "minecraft:diamond_pickaxe",
                                                            "R": "minecraft:redstone_block"})
    shaped("repulsor_upgrade", ["IWI", "WRW", "IWI"], {"I": "minecraft:iron_ingot", "W": "minecraft:wind_charge", "R": "minecraft:redstone_block"})
    shaped("auto_park_upgrade", ["ICI", "ROR", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:compass", "R": "minecraft:repeater",
                                                          "O": "minecraft:observer"})
    shaped("cruiser_booster_mk1", ["RGR", "GBG", "RGR"], {"R": "minecraft:redstone_block", "G": "minecraft:gold_ingot", "B": "microverse_battery"})
    shaped("cruiser_booster_mk2", ["DGD", "GBG", "DGD"], {"D": "minecraft:diamond", "G": "minecraft:gold_block", "B": "cruiser_booster_mk1"})
    shaped("cruiser_booster_mk3", ["PNP", "DBD", "PNP"], {"P": "portal_fluid", "N": "minecraft:netherite_ingot", "D": "minecraft:diamond_block",
                                                           "B": "cruiser_booster_mk2"})
    shaped("cruiser_thrusters_mk1", ["IFI", "BRB", "IFI"], {"I": "minecraft:iron_ingot", "F": "minecraft:firework_rocket", "B": "minecraft:blaze_rod",
                                                             "R": "minecraft:redstone_block"})
    shaped("cruiser_thrusters_mk2", ["DPD", "GTG", "DPD"], {"D": "minecraft:diamond", "P": "minecraft:phantom_membrane", "G": "minecraft:gold_block",
                                                             "T": "cruiser_thrusters_mk1"})
    shaped("cruiser_thrusters_mk3", ["FEF", "NTN", "FDF"], {"F": "portal_fluid", "E": "minecraft:elytra", "N": "minecraft:netherite_ingot",
                                                             "D": "minecraft:diamond_block", "T": "cruiser_thrusters_mk2"})
    shaped("laser_drill_mk2", ["DAD", "ALA", "DAD"], {"D": "minecraft:diamond", "A": "minecraft:amethyst_block", "L": "laser_drill_upgrade"})
    shaped("laser_drill_mk3", ["OFO", "NLN", "OFO"], {"O": "minecraft:crying_obsidian", "F": "portal_fluid", "N": "minecraft:netherite_ingot",
                                                       "L": "laser_drill_mk2"})
    shaped("portal_drive_upgrade", ["FDF", "DGD", "FDF"], {"F": "portal_fluid", "D": "minecraft:diamond", "G": "portal_gun"})


def damage_types():
    write(DATA / "damage_type/space_cruiser.json", {"message_id": "rickrooms.space_cruiser", "scaling": "when_caused_by_living_non_player",
                                                    "exhaustion": 0.1})
    write(DATA / "damage_type/disintegrated.json", {"message_id": "rickrooms.disintegrated", "scaling": "when_caused_by_living_non_player",
                                                    "exhaustion": 0.1})


def advancements():
    advancement("space_cruiser", "space_cruiser", "microverse", IMPOSSIBLE, "goal")
    advancement("cruiser_upgrade", "minigun_upgrade", "space_cruiser", IMPOSSIBLE)
    advancement("fully_loaded", "portal_drive_upgrade", "cruiser_upgrade", IMPOSSIBLE, "challenge")
    advancement("disintegrated", "disintegrator_upgrade", "cruiser_upgrade", IMPOSSIBLE)
    advancement("unlimited_power", "cruiser_booster_mk3", "cruiser_upgrade", IMPOSSIBLE, "challenge")
    advancement("ludicrous_speed", "cruiser_thrusters_mk3", "cruiser_upgrade", IMPOSSIBLE, "challenge")
    advancement("diamond_drill", "laser_drill_mk3", "cruiser_upgrade", IMPOSSIBLE, "goal")
    advancement("cruiser_portal", "portal_drive_upgrade", "cruiser_upgrade", IMPOSSIBLE, "goal")


UPGRADE_TEXT = {
    "disintegrator_upgrade": ("Disintegrator Gun", "A green beam. Whatever it hits, there's not much left of"),
    "minigun_upgrade": ("Laser Minigun", "Hold to fire: two barrels of little red lasers"),
    "flamethrower_upgrade": ("Frontal Flamethrowers", "Hold to fire: two jets of fire out of the front"),
    "rockets_upgrade": ("Rocket Pods", "Two rockets at a time, one from each side"),
    "laser_drill_upgrade": ("Laser Drill Mk I", "Hold to fire: cuts the block you're looking at, and picks up what it cuts"),
    "repulsor_upgrade": ("Repulsor Blast", "A shockwave that throws everything around the cruiser away"),
    "auto_park_upgrade": ("Auto Park", "Parks itself when you get out, comes when you use the remote, and keeps its passengers safe"),
    "portal_drive_upgrade": ("Portal Drive", "Opens a portal ahead and flies the whole cruiser through, to wherever your Portal Gun is set"),
}

LANG = {
    "item.rickrooms.space_cruiser": "Rick's Space Cruiser",
    "item.rickrooms.space_cruiser.tooltip": "Use it on the ground to park it. Runs on its Microverse Battery",
    "entity.rickrooms.space_cruiser": "Rick's Space Cruiser",
    "entity.rickrooms.cruiser_rocket": "Cruiser Rocket",
    "item.rickrooms.cruiser_remote": "Cruiser Remote",
    "item.rickrooms.cruiser_remote.unbound": "Use it on your cruiser to pair it",
    "item.rickrooms.cruiser_remote.bound": "Paired. Press to call your cruiser (needs Auto Park)",
    "tooltip.rickrooms.cruiser_upgrade": "Use it on a Space Cruiser to fit it",
    "item.rickrooms.cruiser_booster_mk1": "Microverse Booster Mk I",
    "item.rickrooms.cruiser_booster_mk2": "Microverse Booster Mk II",
    "item.rickrooms.cruiser_booster_mk3": "Microverse Booster Mk III",
    "tooltip.rickrooms.cruiser_booster.battery": "Battery: %s%%",
    "tooltip.rickrooms.cruiser_booster.flight": "Flying costs %s%% less",
    "tooltip.rickrooms.cruiser_booster.weapons": "Weapons and portals cost %s%% less",
    "tooltip.rickrooms.cruiser_booster.recharge": "Recharges %sx faster",
    "hud.rickrooms.cruiser.booster": "Booster Mk %s",
    "message.rickrooms.cruiser.tier_already": "It already has a %s",
    "item.rickrooms.cruiser_thrusters_mk1": "Speed Thrusters Mk I",
    "item.rickrooms.cruiser_thrusters_mk2": "Speed Thrusters Mk II",
    "item.rickrooms.cruiser_thrusters_mk3": "Speed Thrusters Mk III",
    "tooltip.rickrooms.cruiser_thrusters.speed": "Top speed: %s%%",
    "tooltip.rickrooms.cruiser_thrusters.handling": "Picks up speed and climbs faster",
    "hud.rickrooms.cruiser.thrusters": "Thrusters Mk %s",
    "item.rickrooms.laser_drill_mk2": "Laser Drill Mk II",
    "item.rickrooms.laser_drill_mk3": "Laser Drill Mk III",
    "tooltip.rickrooms.laser_drill.size": "Hold to fire: cuts %1$sx%1$s blocks at a time",
    "tooltip.rickrooms.laser_drill.obsidian": "Cuts through obsidian",
    "key.categories.rickrooms.cruiser": "Rick's Space Cruiser",
    "key.rickrooms.cruiser.descend": "Descend",
    "key.rickrooms.cruiser.next_weapon": "Next weapon",
    "key.rickrooms.cruiser.repulsor": "Repulsor blast",
    "key.rickrooms.cruiser.portal": "Portal drive",
    "hud.rickrooms.cruiser": "SPACE CRUISER",
    "hud.rickrooms.cruiser.weapon": "Weapon: %s (attack to fire)",
    "hud.rickrooms.cruiser.no_weapon": "none fitted",
    "hud.rickrooms.cruiser.keys": "Jump up · %s down · %s weapon · %s repulsor · %s portal · %s dial",
    "key.rickrooms.cruiser.dial": "Portal Gun dial",
    "message.rickrooms.cruiser.installed": "Fitted: %s",
    "message.rickrooms.cruiser.already": "It already has %s",
    "message.rickrooms.cruiser.status": "Space Cruiser: %s%% power",
    "message.rickrooms.cruiser.no_upgrades": "\n - no upgrades fitted",
    "message.rickrooms.cruiser.no_weapon": "No weapon fitted",
    "message.rickrooms.cruiser.low_power": "Not enough power. Land and let the battery charge",
    "message.rickrooms.cruiser.need_gun": "The portal drive needs a Portal Gun in your inventory",
    "message.rickrooms.cruiser.no_destination": "Set a destination on your Portal Gun first",
    "message.rickrooms.cruiser.no_room": "Not enough room to park it there",
    "message.rickrooms.cruiser.remote_bound": "Remote paired with this cruiser",
    "message.rickrooms.cruiser.remote_unbound": "Use the remote on your cruiser to pair it first",
    "message.rickrooms.cruiser.remote_far": "Can't reach your cruiser from here",
    "message.rickrooms.cruiser.remote_no_autopark": "The cruiser needs Auto Park to come when called",
    "message.rickrooms.cruiser.remote_called": "Your cruiser is on its way",
    "message.rickrooms.cruiser.keeping_safe": "Space Cruiser: Keeping %s safe.",
    "death.attack.rickrooms.space_cruiser": "%1$s was shot down by a Space Cruiser",
    "death.attack.rickrooms.space_cruiser.player": "%1$s was shot by %2$s's Space Cruiser",
    "death.attack.rickrooms.disintegrated": "%1$s was disintegrated",
    "death.attack.rickrooms.disintegrated.player": "%1$s was disintegrated by %2$s",
    "subtitles.rickrooms.space_cruiser.engine": "Space Cruiser hums",
    "advancements.rickrooms.space_cruiser.title": "Wubba Lubba Dub Dub",
    "advancements.rickrooms.space_cruiser.description": "Take Rick's Space Cruiser for a spin",
    "advancements.rickrooms.cruiser_upgrade.title": "Pimp My Ride",
    "advancements.rickrooms.cruiser_upgrade.description": "Fit an upgrade to your Space Cruiser",
    "advancements.rickrooms.fully_loaded.title": "Fully Loaded",
    "advancements.rickrooms.fully_loaded.description": "Fit every upgrade to one Space Cruiser",
    "advancements.rickrooms.disintegrated.title": "Disintegrated",
    "advancements.rickrooms.disintegrated.description": "Leave nothing but dust with the Disintegrator Gun",
    "advancements.rickrooms.unlimited_power.title": "Unlimited Power",
    "advancements.rickrooms.unlimited_power.description": "Fit a Microverse Booster Mk III to your Space Cruiser",
    "advancements.rickrooms.ludicrous_speed.title": "Ludicrous Speed",
    "advancements.rickrooms.ludicrous_speed.description": "Fit Speed Thrusters Mk III to your Space Cruiser",
    "advancements.rickrooms.diamond_drill.title": "Through Anything",
    "advancements.rickrooms.diamond_drill.description": "Fit a Laser Drill Mk III to your Space Cruiser",
    "advancements.rickrooms.cruiser_portal.title": "Portal Drive",
    "advancements.rickrooms.cruiser_portal.description": "Fly your Space Cruiser through a portal",
}


def lang():
    path = ASSETS / "lang/en_us.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update(LANG)
    for item, (name, tip) in UPGRADE_TEXT.items():
        data[f"item.rickrooms.{item}"] = name
        data[f"item.rickrooms.{item}.tooltip"] = tip
    write(path, data)


def sounds():
    path = ASSETS / "sounds.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["space_cruiser.engine"] = {"subtitle": "subtitles.rickrooms.space_cruiser.engine",
                                    "sounds": [{"name": "minecraft:block.beacon.ambient", "type": "event", "pitch": 1.2, "volume": 0.9}]}
    write(path, data)


def main():
    recipes()
    damage_types()
    advancements()
    lang()
    sounds()
    print("Space cruiser data written")


if __name__ == "__main__":
    main()
