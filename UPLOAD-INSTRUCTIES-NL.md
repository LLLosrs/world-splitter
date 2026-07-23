# Upload- en gebruiksinstructies

## 1. Plugin naar GitHub uploaden

1. Pak `world-splitter-runelite-github-ready.zip` uit.
2. Maak op GitHub een openbare repository met de naam `world-splitter`.
3. Upload **de inhoud van de map `world-splitter`** naar de root van de repository.
4. Controleer dat `build.gradle`, `runelite-plugin.properties`, `icon.png` en `src/`
   direct zichtbaar zijn op de hoofdpagina van de repository.
5. Open het tabblad **Actions** en controleer of de build groen wordt.

## 2. Lokaal testen in RuneLite developer mode

1. Open de repository als Gradle-project in IntelliJ IDEA.
2. Gebruik Java 11.
3. Open het Gradle-venster en start de taak `run`.
4. De RuneLite development client opent met World Splitter geladen.

Solo mode werkt zonder server. Voor group sync moet de aparte server-ZIP in een tweede
GitHub-repository worden geplaatst en als Node.js-webservice worden gehost.

## 3. In de normale RuneLite-client krijgen

Een GitHub-upload alleen installeert de plugin niet in de normale RuneLite-client. Maak na het
testen een Plugin Hub pull request:

1. Kopieer de volledige 40-karakter commit-hash van je plugin-repository.
2. Fork `runelite/plugin-hub`.
3. Maak daar het bestand `plugins/world-splitter`.
4. Gebruik de inhoud uit `plugin-hub-manifest.txt` en vervang de commit-hash.
5. Open een pull request en verwerk eventuele CI- of reviewfeedback.

Na goedkeuring verschijnt World Splitter in RuneLite onder **Plugin Hub**.
