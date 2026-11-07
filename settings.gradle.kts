pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required for unityandroidpermissions.aar inside unityLibrary/libs
        flatDir {
            dirs("${rootProject.projectDir}/unityLibrary/libs")
        }
    }
}

rootProject.name = "LumineApp"

// Unity's export writes a unityLibrary/build.gradle that uses APIs removed in Gradle 9 /
// AGP 9, so it's patched here on every sync, before the subprojects are evaluated. Each
// fix only applies while the original text is still there, so re-running is harmless.
run {
    val unityGradle = rootProject.projectDir.resolve("unityLibrary/build.gradle")
    if (unityGradle.exists()) {
        // Normalise to LF so all multi-line replace() calls work on Windows (CRLF) too
        var src = unityGradle.readText().replace("\r\n", "\n")
        var dirty = false

        // 1. Guard unityStreamingAssets - property not always set, crashes sync
        if (src.contains("+ unityStreamingAssets.tokenize(', ')") &&
            !src.contains("project.hasProperty('unityStreamingAssets')")
        ) {
            src = src.replace(
                "+ unityStreamingAssets.tokenize(', ')",
                "+ (project.hasProperty('unityStreamingAssets') ? unityStreamingAssets.tokenize(', ') : [])"
            )
            dirty = true
        }

        // 2. Promote def BuildIl2Cpp() to ext closure (Gradle 9 scoping fix)
        if (src.contains("def BuildIl2Cpp(String workingDir, String configuration") &&
            !src.contains("ext.BuildIl2Cpp = {")
        ) {
            src = src.replace(
                "def BuildIl2Cpp(String workingDir, String configuration, String architecture, String abi, String[] staticLibraries) {",
                "ext.BuildIl2Cpp = { String workingDir, String configuration, String architecture, String abi, String[] staticLibraries ->"
            )
            dirty = true
        }

        // 3. android.ndkDirectory is gone in AGP 9, use the NDK path directly. Checks for the old
        // token because the NDK path already appears at the top of the file.
        if (src.contains("android.ndkDirectory")) {
            src = src.replace(
                "\"--tool-chain-path=\" + android.ndkDirectory",
                "\"--tool-chain-path=\" + \"C:/Program Files/Unity/Hub/Editor/2022.3.62f3/Editor/Data/PlaybackEngines/AndroidPlayer/NDK\""
            )
            dirty = true
        }

        // 4. Replace exec {} with ProcessBuilder (exec(Closure) removed in Gradle 9)
        if (src.contains("    exec {") && !src.contains("new ProcessBuilder")) {
            src = src.replace(
                "    exec {\n" +
                "        executable workingDir + \"/src/main/Il2CppOutputProject/IL2CPP/build/deploy/il2cpp\" + executableExtension\n" +
                "        args commandLineArgs\n" +
                "        environment \"ANDROID_SDK_ROOT\", getSdkDir()\n" +
                "    }",
                "    def il2cppExe = workingDir + \"/src/main/Il2CppOutputProject/IL2CPP/build/deploy/il2cpp\" + executableExtension\n" +
                "    def pb = new ProcessBuilder([il2cppExe] + commandLineArgs)\n" +
                "    pb.environment().put(\"ANDROID_SDK_ROOT\", getSdkDir())\n" +
                "    pb.inheritIO()\n" +
                "    def proc = pb.start()\n" +
                "    proc.waitFor()\n" +
                "    if (proc.exitValue() != 0) throw new GradleException(\"il2cpp failed\")"
            )
            dirty = true
        }

        // 5. Call sites: BuildIl2Cpp() -> project.ext.BuildIl2Cpp()
        if (src.contains("              BuildIl2Cpp(") &&
            !src.contains("project.ext.BuildIl2Cpp(")
        ) {
            src = src.replace(
                "              BuildIl2Cpp(",
                "              project.ext.BuildIl2Cpp("
            )
            dirty = true
        }

        if (dirty) {
            unityGradle.writeText(src)
            println("[LumineApp] Auto-patched unityLibrary/build.gradle for Gradle 9 compatibility")
        }
    }

    // proguard-unity.txt: -ignorewarnings breaks the app's ProGuard step
    val proguard = rootProject.projectDir.resolve("unityLibrary/proguard-unity.txt")
    if (proguard.exists()) {
        val pg = proguard.readText()
        if (pg.contains("-ignorewarnings")) {
            proguard.writeText(pg.replace("-ignorewarnings\n", "").replace("-ignorewarnings", ""))
            println("[LumineApp] Removed -ignorewarnings from proguard-unity.txt")
        }
    }

    // xrmanifest: targetSdkVersion 22 is below minSdkVersion 26
    val xrGradle = rootProject.projectDir.resolve("unityLibrary/xrmanifest.androidlib/build.gradle")
    if (xrGradle.exists()) {
        val xr = xrGradle.readText()
        if (xr.contains("targetSdkVersion 22")) {
            xrGradle.writeText(xr.replace("targetSdkVersion 22", "targetSdkVersion 36"))
            println("[LumineApp] Fixed xrmanifest targetSdkVersion 22 -> 36")
        }
    }

    // Unity pins buildToolsVersion 34, which AGP 9 ignores with a warning on every build
    for (path in listOf("unityLibrary/build.gradle", "unityLibrary/xrmanifest.androidlib/build.gradle")) {
        val file = rootProject.projectDir.resolve(path)
        if (!file.exists()) continue
        val text = file.readText()
        val cleaned = text.replace(Regex("""(?m)^[ \t]*buildToolsVersion\s+'[\d.]+'\r?\n"""), "")
        if (cleaned != text) {
            file.writeText(cleaned)
            println("[LumineApp] Removed Unity's buildToolsVersion from $path")
        }
    }
}

include(":app")
include(":unityLibrary")
// Unity's XR manifest is a submodule of unityLibrary
include(":unityLibrary:xrmanifest.androidlib")
