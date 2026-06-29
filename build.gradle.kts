plugins {
    java
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.mctian-mi"
version = "1.0.0"

repositories {
    maven {
        name = "Cleanroom Maven"
        url = uri("https://maven.cleanroommc.com")
    }
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven {
        name = "GTCEu Maven"
        url = uri("https://maven.gtceu.com")
    }
    mavenCentral()
    mavenLocal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    // NOTE: do not call withSourcesJar()/withJavadocJar() here — the maven-publish
    // plugin below configures the sources and javadoc jars itself, and calling them
    // twice would register duplicate tasks.
}

dependencies {
    compileOnly(variantOf(libs.lwjgl3ify) {
        classifier("forgePatches")
    }) { isTransitive = false }
    compileOnly(libs.annotations)
    compileOnly(libs.asm.tree)
}

// The modernJavaExtraRuntimeClasspath consumer in minecraft.gradle.kts resolves this project's
// runtimeElements with variant-aware attribute matching. RFG expects these two attributes on
// the outgoing configuration; without them Gradle fails variant selection.
configurations.named("runtimeElements") {
    attributes {
        attribute(Attribute.of("com.gtnewhorizons.retrofuturagradle.obfuscation", String::class.java), "mcp")
        attribute(Attribute.of("rfgDeobfuscatorTransformed", Boolean::class.javaObjectType), true)
    }
}

// Publishing is handled by the Vanniktech maven-publish plugin, which targets the Sonatype
// Central Portal (https://central.sonatype.com), generates -sources and -javadoc jars, and signs
// every artifact. JitPack still works independently: it runs `publishToMavenLocal` and rewrites
// the coordinate to com.github.MCTian-mi:ForgePatches-Extra:<tag>. Signing is only *required* when
// a remote publish task is in the graph, so the JitPack `publishToMavenLocal` build does not need
// GPG keys.
mavenPublishing {
    // Target the Sonatype Central Portal (central.sonatype.com). Without this explicit host the
    // plugin defaults to the legacy OSSRH Nexus, which has been shut down and returns HTTP 402.
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    // Only sign when a key is actually configured. The Central Portal requires signatures, and CI
    // supplies the key via ORG_GRADLE_PROJECT_signingInMemoryKey. JitPack / local `publishToMavenLocal`
    // runs without a key and must not fail on a missing signatory, so signing is gated here.
    val hasSigningKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates("io.github.mctian-mi", "forgepatches-extra", version.toString())

    pom {
        name.set("ForgePatches-Extra")
        description.set(
            "A RetroFuturaBootstrap (RFB) plugin that removes FML's SecurityManager so Minecraft " +
                "1.12.2 Forge can run under modern Java via lwjgl3ify."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/MCTian-mi/ForgePatches-Extra")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("MCTian-mi")
                name.set("MCTian-mi")
                url.set("https://github.com/MCTian-mi")
            }
        }
        scm {
            url.set("https://github.com/MCTian-mi/ForgePatches-Extra")
            connection.set("scm:git:https://github.com/MCTian-mi/ForgePatches-Extra.git")
            developerConnection.set("scm:git:ssh://git@github.com/MCTian-mi/ForgePatches-Extra.git")
        }
    }
}
