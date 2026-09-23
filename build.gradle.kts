// =============================================================================
// colecteur-chunky — a VaniaMetrics-<Name>-<v>.jar in build/libs/
//
// One module = one jar, loaded by the platform if — and only if — the core
// is present (`depend: [VaniaMetrics]` in plugin.yml). No third-party jar is
// bundled: everything below is compileOnly.
// =============================================================================
plugins {
    java
}

// THE VERSION IS THE ONE OF THE API this jar is compiled against: read from
// the included core's Version.java, never copied.
val vaniaCoreDir = gradle.extra["vaniaCoreDir"] as File
val versionSource = vaniaCoreDir.resolve("api/src/main/java/fr/samflix/vaniametrics/api/Version.java")
version = Regex("""VALUE = "([^"]+)"""").find(versionSource.readText())?.groupValues?.get(1)
    ?: error("unreadable version in $versionSource")

dependencies {
    // Wired through the composite build to the core repo's api/ project.
    compileOnly("fr.samflix:vania-metrics-api")
    compileOnly(libs.bundles.paper)
    compileOnly(libs.chunky)
}

tasks.withType<JavaCompile>().configureEach {
    // --release 21: the lobby targets Java 25, the proxy Java 21. The lowest wins.
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-path,-processing,-options", "-Werror"))
}

tasks.processResources {
    val v = version.toString()
    inputs.property("version", v)
    filesMatching("plugin.yml") { filter { it.replace("\${version}", v) } }
}

// THE JAR NAME COMES FROM "name:", not the entry class: the server repo's list
// identifies modules by their plugin name. One source of truth, the one Bukkit
// displays.
val pluginYml = file("src/main/resources/plugin.yml")
val displayName = Regex("""(?m)^name: VaniaMetrics-(\S+)""").find(pluginYml.readText())?.groupValues?.get(1)
    ?: error("$pluginYml: expected \"name:\" in the form VaniaMetrics-<Name>")

tasks.jar {
    archiveFileName = "VaniaMetrics-$displayName-$version.jar"
}
