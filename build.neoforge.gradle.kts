plugins {
    id("net.neoforged.moddev")
}

val mc = stonecutter.current.version
val modId = property("mod.id") as String
val selftest = providers.gradleProperty("tp.selftest").orNull
val selftestMode = providers.gradleProperty("tp.selftest.mode").getOrElse("full")
val mcNext = mc.split(".").let { "${it[0]}.${it[1].toInt() + 1}" }

version = "${property("mod.version")}+$mc-neoforge"
group = property("mod.group") as String
base.archivesName = modId

neoForge {
    version = property("deps.neoforge") as String

    runs {
        register("client") {
            client()
            gameDirectory = file("run/")
            programArguments.addAll("--username", "PocketTester")
            if (selftest != null) {
                systemProperty("tidypockets.selftest", selftest)
                systemProperty("tidypockets.selftest.mode", selftestMode)
            }
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.processResources {
    exclude("fabric.mod.json", "META-INF/mods.toml")
    if (mc == "26.1.2") exclude("META-INF/services/dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint") // ControlifyCompat is a stub below 26.2
    val props = mapOf(
        "version" to project.version.toString(),
        "mc" to mc,
        "mc_next" to mcNext,
        "name" to project.property("mod.name"),
        "description" to project.property("mod.description"),
        "author" to project.property("mod.author"),
        "homepage" to project.property("mod.homepage"),
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE"))
}

repositories {
    mavenCentral()
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
}

dependencies {
    compileOnly("maven.modrinth:controlify:${property("deps.controlify")}") { isTransitive = false }
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// 26.1.2 is older than the 26.2 names the sources use: dev/renames.json + renames-common.json entries for "<26.2"
if (mc == "26.1.2") {
    extra["mcVersion"] = mc
    apply(from = rootProject.file("../../tools/Backport/renames.gradle.kts"))
}

// The shared Profet settings screen, nested in the jar (tools/ProfetSettings/STATUS.md)
apply(from = rootProject.file("../../tools/ProfetSettings/mod/consumer.gradle.kts"))
