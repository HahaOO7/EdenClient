EdenClient
===========

A fully client-side Minecraft Fabric mod (multi-module Gradle build) named EdenClient. The mod is fully clientside and is exclusively managed with commands.

This repository contains three main modules:

- `EdenClient/` - The client mod source and build (main mod project).
- `EcAnnotations/` - Annotation-only module used by the project.
- `EcProcessors/` - Annotation processors used at compile time.

Overview
--------
EdenClient is built with Fabric Loom and targets modern Java (Java 21). The project uses Gradle with the included Gradle wrapper. Annotations and annotation processors are provided in separate subprojects and are wired into the main mod's build.

Prerequisites
-------------
- Java 21 (JDK 21) installed and JAVA_HOME pointing to it.
- Git (optional, for cloning and contributing).
- No global Gradle install is required — the project provides a Gradle wrapper (`gradlew`).

Key files
---------
- `EdenClient/build.gradle` - Main mod build script (uses `fabric-loom`).
- `EcAnnotations/` - Annotations module.
- `EcProcessors/` - Annotation processor module.
- `settings.gradle` - Gradle settings for the multi-project build.

Quick build and run
-------------------
Run these commands from the repository root:

Build the whole multi-project and produce jars:

```
./gradlew build
```

Run the client in a Loom development environment (starts a Minecraft client with the mod loaded):

```
./gradlew runClient
```

Where to find artifacts
-----------------------
- Built mod jars: `build/libs/` (e.g. `edenclient-<version>.jar`).
- Dev jars and generated sources are present under `EdenClient/build/`.

Development notes
-----------------
- The project uses Fabric Loom for Minecraft development. The `EdenClient/build.gradle` references Fabric API and Fabric Loader.
- Lombok is used as a compile-only dependency with annotation processing enabled in the build script (`org.projectlombok:lombok:1.18.36`). If using an IDE, install a Lombok plugin or enable annotation processing in the IDE.
- Annotation types live in `EcAnnotations/` and processors in `EcProcessors/`. Those subprojects are wired into the main project as `implementation` and `annotationProcessor` dependencies respectively.
- Adventure (Kyori) components are included for chat/serialization integrations.

Contributing
------------
- Fork the repository, create a branch, and open a pull request.
- Keep changes focused and include a short description of why the change is needed.
- If you modify public APIs or resources, include any required migration notes.

Troubleshooting
---------------
- If the build fails due to Java version issues, verify `java -version` and `JAVA_HOME` point to JDK 21.
- If annotation processing doesn't run in the IDE, enable annotation processing for the project and ensure Lombok plugin is installed.
- For Fabric/Loom runtime issues, check the Loom and Fabric versions in `EdenClient/build.gradle` and `gradle.properties`.

License
-------
This project is licensed under the MIT License — see the `LICENSE.txt` file at the repository root for the full text.

Further reading
---------------
- Fabric Loom documentation: https://fabricmc.net/wiki/tutorial:setup
- Fabric API: https://fabricmc.net/
- Kyori Adventure: https://github.com/KyoriPowered/adventure
