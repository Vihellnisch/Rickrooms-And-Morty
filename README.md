# RickRooms and Morty

A parody Minecraft mod: **Rick and Morty, lost in the Backrooms.** Rick's Portal Gun, his gadgets and his Space Cruiser, thrown into endless liminal levels, things that watch you, and a few places you really should run from.

Unofficial and fan-made. Not affiliated with or endorsed by Adult Swim or the creators of Rick and Morty.

**Download:** the jar is on the [Releases page](https://github.com/Vihellnisch/Rickrooms-And-Morty/releases/latest). Drop it into your `mods` folder.

- **Minecraft** 1.21.1, **NeoForge** 21.1+
- **Optional:** [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) 9.x (the Interdimensional Goggles fit its head slot)

## What's in it

- **The Portal Gun:** shoot portals and walk through them, save places on its dial, and add upgrades (Targeting, Blender attack mode, DNA Lock).
- **The Backrooms:** twenty levels with their own rules. Among them:
  - Level 0 and the Poolrooms
  - the Infinite IKEA and the Dead Mall
  - the Endless Field and the Abandoned City
  - Drop Limit, the Facade and the Staircase
  - Froopyland and Run For Your Life
- **Things in the dark:** the Watcher, Still Lifes, scarecrows, partygoers, the Bacteria.
- **Cronenberg World**, the Council of Yous, Warp Gateways, and Rick's Space Cruiser.
- **Rick's gadgets:** the Shrink Ray, the Freeze Ray, the Time Crystal, the x-ray Interdimensional Goggles, and a Cognition Amplifier for your pets.
- **Rick's Field Guide** in game explains most of it.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. Gradle downloads Curios to compile against. It isn't bundled, and the mod runs without it.

The game tests live in `src/gametest` and don't go into the jar. Run them with:

```
./gradlew runGameTestServer
```

`tools/` holds the Python scripts that generate the textures, models and data files.

## Credits

- Mod by **Vihellnisch**.
- Level music by **Kevin MacLeod** ([incompetech.com](https://incompetech.com)), licensed under [Creative Commons: By Attribution 4.0](https://creativecommons.org/licenses/by/4.0/).

## License

The code and the mod's own art are under the [MIT License](LICENSE). The music is CC BY 4.0, as above.
