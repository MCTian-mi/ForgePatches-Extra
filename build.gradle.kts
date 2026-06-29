plugins {
    java
    `maven-publish`
}

group = "dev.tianmi"
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
    withSourcesJar()
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

// JitPack detects maven-publish and runs `publishToMavenLocal`. The artifact is consumed as
// com.github.MCTian-mi:ForgePatches-Extra:<tag>. compileOnly deps are intentionally not published
// (the consumer — RFG/lwjgl3ify — provides them at runtime), matching the isTransitive = false setup.
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "forgepatches-extra"
            from(components["java"])
        }
    }
}
