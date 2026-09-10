# Selene

Selene is a client-side Minecraft mod for Fabric. It adds a custom menu, combat and movement modules, visual settings, and movable HUD elements.

## Requirements

- Minecraft 1.21.11
- Fabric Loader
- Fabric API
- Java 21

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API.
3. Put the Selene jar in your Minecraft `mods` folder.
4. Start Minecraft using the Fabric profile.

Make sure there is only one Selene jar in the `mods` folder.

## Use Selene

Press **Right Shift** to open the menu.

From the menu you can:

- Turn modules on and off
- Search for modules
- Change module settings
- Set keybinds
- Change the menu appearance
- Enable and move HUD elements
- Resize HUD elements while the Minecraft chat screen is open

Settings are saved automatically.

Selene also includes optional configuration, friend, target, and map features. Some features may only work on specific servers.

## Build

You need Java 21 installed. Run this command in the project folder:

```bash
./gradlew build
```

On Windows, use:

```bat
gradlew.bat build
```

The finished jar will be in `build/libs/`.

## License

Selene is licensed under the GNU General Public License v3. See [LICENSE](LICENSE).

You can use, change, and share Selene. If you share a modified version, it must stay under GPL v3 and the source code must be provided with it. This keeps distributed forks open source.

Use the mod according to the rules of the server you are playing on.
