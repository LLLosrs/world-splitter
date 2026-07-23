# World Splitter

World Splitter is a RuneLite Plugin Hub plugin that divides an eligible world pool into
consecutive blocks and highlights the block assigned to you in the in-game world switcher.

## Features

- Solo allocation using total people, your position and worlds per person.
- Optional live group mode using a temporary six-character group code.
- Automatic rebalancing when group members join, leave or time out.
- Filters for members, free-to-play and dangerous worlds.
- Configurable world range and highlight colour.
- Network functionality is disabled by default and requires explicit opt-in.

## Repository contents

This repository is only the RuneLite plugin. The optional Node.js sync server belongs in a
separate repository and must be hosted independently.

## Local development

Requirements:

- IntelliJ IDEA Community Edition
- Java 11 (Eclipse Temurin recommended)
- Gradle 8.10, or let IntelliJ use its configured Gradle installation

Open this directory as a Gradle project and run the `run` Gradle task. The included
`WorldSplitterPluginTest` class loads the plugin into a RuneLite development client.

Command-line checks:

```bash
gradle clean test
gradle run
```

GitHub Actions also runs `gradle clean test` automatically on every push and pull request.

## Solo mode

Solo mode works without any external server:

1. Open the World Splitter configuration.
2. Set the world range and world filters.
3. Set **Total people**, **My position** and **Worlds per person**.
4. Open the in-game world switcher to see your worlds highlighted.

## Optional group sync

Group mode requires the separate `worldsplitter-server` project:

1. Deploy the server over HTTPS.
2. In the plugin settings, enable **Group sync** and accept the privacy warning.
3. Enter the server base URL, without an API path.
4. Use the sidebar panel to create or join a group.

The plugin sends the server the temporary group code and a random member identifier. The web
server and hosting provider will also receive normal connection metadata such as the user's IP
address. No RuneScape username, password or gameplay data is sent by this plugin.

## Upload to GitHub

Create a public repository named `world-splitter`, then upload the contents of this directory
so `build.gradle`, `runelite-plugin.properties` and `src` are located at the repository root.
Do not upload the outer ZIP as a single file.

Example with Git:

```bash
git init
git add .
git commit -m "Initial World Splitter release"
git branch -M master
git remote add origin https://github.com/LLLosrs/world-splitter.git
git push -u origin master
```

Change the repository URL if your GitHub account or repository name differs.

## Submit to the RuneLite Plugin Hub

Uploading to GitHub alone does not add the plugin to the normal RuneLite client. To make it
installable from RuneLite's Plugin Hub:

1. Confirm the plugin works through the `run` Gradle task.
2. Push the final code to a public GitHub repository.
3. Copy the full 40-character commit hash.
4. Fork `runelite/plugin-hub`.
5. Add a file named `plugins/world-splitter` containing:

```text
repository=https://github.com/LLLosrs/world-splitter.git
commit=YOUR_FULL_COMMIT_HASH
```

6. Open a pull request to `runelite/plugin-hub` and resolve any CI or reviewer feedback.

A ready-to-edit marker is included as `plugin-hub-manifest.txt`.

## Notes

- `build=standard` is used, so Plugin Hub replaces the local Gradle build files during its
  submission build. No custom runtime dependency is bundled.
- The root `icon.png` is used by the Plugin Hub. The copy under `src/main/resources` is used by
  the sidebar navigation button.
- Group state is temporary. If the self-hosted server restarts, users need to create or join a
  new group.

## License

BSD 2-Clause. See `LICENSE`.
