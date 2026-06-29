# ForgePatches-Extra

[![Release](https://jitpack.io/v/MCTian-mi/ForgePatches-Extra.svg)](https://jitpack.io/#MCTian-mi/ForgePatches-Extra)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A tiny standalone [RetroFuturaBootstrap (RFB)](https://github.com/GTNewHorizons/RetroFuturaBootstrap)
plugin for **Minecraft 1.12.2 Forge** that neutralizes FML's `SecurityManager` installation so the
game can run under **modern Java** via [lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify).

## Overview

When a 1.12.2 Forge instance is launched on a modern JVM (Java 18+) through lwjgl3ify, FML's startup
sequence tries to install a `SecurityManager`. On those JVMs the `SecurityManager` has been degraded
to a no-op or removed entirely, and the call either throws or aborts the boot. ForgePatches-Extra
removes that single call at class-load time, letting the launch continue cleanly — no game code is
otherwise modified.

## How it works

The plugin registers a single RFB class transformer with id `no-fml-security-manager`:

1. It targets exactly one class: `net.minecraftforge.fml.common.launcher.FMLTweaker`.
2. Inside that class's no-arg constructor (`<init>()V`) it scans for the bytecode instruction
   `INVOKESTATIC java/lang/System.setSecurityManager (Ljava/lang/SecurityManager;)V`.
3. Each matching call is rewritten to a `POP`, discarding the argument already on the stack instead
   of invoking the method. Stack frames are recomputed (`computeMaxs`) only if a change was made.

The RFB plugin descriptor loads after the `java` plugin and before everything else
(`loadAfter=java`, `loadBefore=*`), so the patch is in place before FML is touched.

## Installation

ForgePatches-Extra is available from two remote sources:

- **Maven Central** — `io.github.mctian-mi:forgepatches-extra` (published from GitHub Releases; see
  [Releasing to Maven Central](#releasing-to-maven-central)).
- **JitPack** — `com.github.MCTian-mi:ForgePatches-Extra`, built on demand from any git tag/commit.

Pick whichever fits your project. Maven Central needs no extra repository; JitPack requires adding
the `https://jitpack.io` repository.

### Via Maven Central

`build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    // pick the configuration appropriate for your RFB/lwjgl3ify setup
    implementation("io.github.mctian-mi:forgepatches-extra:1.0.0")
}
```

### Via JitPack

`build.gradle.kts`:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // pick the configuration appropriate for your RFB/lwjgl3ify setup
    implementation("com.github.MCTian-mi:ForgePatches-Extra:1.0.0")
}
```

`build.gradle` (Groovy):

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.MCTian-mi:ForgePatches-Extra:1.0.0'
}
```

### Via local Maven (`mavenLocal`)

Run `./gradlew publishToMavenLocal` to install `io.github.mctian-mi:forgepatches-extra:1.0.0` into
your `~/.m2` repository, then consume it from any project that has `mavenLocal()` in its repositories:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("io.github.mctian-mi:forgepatches-extra:1.0.0")
}
```

> **Note on dependencies:** lwjgl3ify, ASM, and the JetBrains annotations are declared `compileOnly`
> and are intentionally **not** transitive — the RFG / lwjgl3ify runtime environment already provides
> them. The published POM therefore lists no dependencies.

## Building from source

Requirements:

- **JDK 17 or newer** to *run* Gradle 9.6.1 (the project bundles its own wrapper).
- The compiled bytecode targets **Java 8** (`sourceCompatibility`/`targetCompatibility = 1.8`).

```bash
./gradlew build
```

The artifact is produced at `build/libs/forgePatchesExtra-1.0.0.jar` and contains both compiled
classes plus the `META-INF/rfb-plugin/...` descriptor.

To install it into your local Maven repository (`~/.m2`) as
`io.github.mctian-mi:forgepatches-extra:1.0.0`:

```bash
./gradlew publishToMavenLocal
```

## Releasing to Maven Central

Releases are published automatically to the [Sonatype Central Portal](https://central.sonatype.com)
by the [`Publish to Maven Central`](.github/workflows/publish.yml) GitHub Actions workflow whenever a
**GitHub Release is published** (it can also be run manually from the Actions tab). The workflow runs
`./gradlew publishAndReleaseToMavenCentral`, which builds, signs, uploads, and auto-releases the
deployment.

Publishing is configured with the
[`com.vanniktech.maven.publish`](https://github.com/vanniktech/gradle-maven-publish-plugin) plugin,
which generates the `-sources` and `-javadoc` jars and signs every artifact. Signing is only enabled
when a key is present, so `publishToMavenLocal` (and JitPack) keep working without GPG keys.

### One-time setup

1. **Verify the namespace.** Claim `io.github.mctian-mi` at central.sonatype.com (GitHub-backed
   namespaces are verified by creating the temporary public repo the Portal asks for).
2. **Generate a User Token** (Account → Generate User Token) — a username/password pair.
3. **Create a GPG key** and publish its public half to a keyserver.
4. **Add these GitHub repository secrets** (Settings → Secrets and variables → Actions):

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_USERNAME` | Central Portal user token name |
   | `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
   | `SIGNING_KEY` | ASCII-armored GPG private key (full block) |
   | `SIGNING_KEY_PASSWORD` | passphrase for that key (empty if none) |

### Cutting a release

1. Bump `version` in `build.gradle.kts`.
2. Commit, tag, and publish a GitHub Release for that version.
3. The workflow runs and the artifact appears on Maven Central within a few minutes.

To publish locally instead, set the same values as Gradle properties / `ORG_GRADLE_PROJECT_*` env
vars and run `./gradlew publishAndReleaseToMavenCentral`.

## Project structure

```
forgePatchesExtra/
├── build.gradle.kts          # java + Vanniktech maven-publish, group io.github.mctian-mi
├── settings.gradle.kts       # standalone (no included builds)
├── jitpack.yml               # JDK 17 + publishToMavenLocal for JitPack
├── gradle/
│   └── libs.versions.toml     # lwjgl3ify / annotations / asm versions
└── src/main/
    ├── java/dev/tianmi/rfbplugins/
    │   ├── NoFmlSecurityManagerPlugin.java          # RfbPlugin entry point
    │   └── FmlTweakerSecurityManagerTransformer.java # the ASM transformer
    └── resources/META-INF/rfb-plugin/
        └── rfb-remove-fml-sercuritymanager.properties # RFB plugin descriptor
```

## License

Released under the [MIT License](LICENSE).
