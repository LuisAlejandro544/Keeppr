plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.keeppr.notes"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Podar recursos de más de 75 idiomas innecesarios heredados de librerías para compactar resources.arsc
    resourceConfigurations += listOf("es", "en")

    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
      }
    }
  }

  ndkVersion = "26.1.10909125"

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val keyFile = file(keystorePath)
      if (keyFile.exists() && System.getenv("STORE_PASSWORD") != null) {
        storeFile = keyFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("STORE_PASSWORD")
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      versionNameSuffix = "-b"
      resValue("string", "app_name", "Keeppr Beta")
      ndk {
        abiFilters.clear()
        abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
      }
      externalNativeBuild {
        cmake {
          abiFilters.clear()
          abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
      }
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      versionNameSuffix = "-dev"
      resValue("string", "app_name", "Keeppr Canary")
      ndk {
        abiFilters.clear()
        abiFilters.addAll(listOf("arm64-v8a", "x86_64", "armeabi-v7a"))
      }
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
  packaging {
    jniLibs {
      useLegacyPackaging = false
    }
    resources {
      excludes += setOf(
        "META-INF/LICENSE.txt",
        "META-INF/LICENSE",
        "META-INF/LICENSE*",
        "META-INF/NOTICE.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE*",
        "META-INF/INDEX.LIST",
        "META-INF/licenses/**",
        "META-INF/license/**",
        "META-INF/*.md",
        "META-INF/*.txt",
        "META-INF/*.version",
        "META-INF/androidx/**",
        "META-INF/DEPENDENCIES",
        "META-INF/AL2.0",
        "META-INF/LGPL2.1",
        "META-INF/*.kotlin_module",
        "META-INF/version-control-info.textproto"
      )
    }
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // Dependencias no utilizadas comentadas para reducir APK y acelerar compilacion:
  // implementation(libs.converter.moshi)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.retrofit)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

val cargoBuild = tasks.register("cargoBuild") {
  val rustDir = file("src/main/rust")
  inputs.dir(rustDir.resolve("src"))
  inputs.file(rustDir.resolve("Cargo.toml"))
  outputs.dir(rustDir.resolve("target"))

  doLast {
    val sdkDir = listOfNotNull(
      System.getenv("ANDROID_SDK_ROOT")?.let { File(it) },
      System.getenv("ANDROID_HOME")?.let { File(it) },
      File("/opt/android/sdk")
    ).firstOrNull { it.exists() } ?: File("/opt/android/sdk")

    val ndkDir = listOfNotNull(
      System.getenv("ANDROID_NDK_ROOT")?.let { File(it) },
      System.getenv("ANDROID_NDK_HOME")?.let { File(it) },
      File(sdkDir, "ndk/26.1.10909125"),
      File(sdkDir, "ndk-bundle"),
      File(sdkDir, "ndk").listFiles()?.maxByOrNull { it.name }
    ).firstOrNull { it.exists() && File(it, "toolchains/llvm/prebuilt/linux-x86_64/bin").exists() }
      ?: File(sdkDir, "ndk/26.1.10909125")

    val llvmBin = File(ndkDir, "toolchains/llvm/prebuilt/linux-x86_64/bin")

    val targets = listOf(
      "arm64-v8a" to ("aarch64-linux-android" to "aarch64-linux-android26-clang"),
      "x86_64" to ("x86_64-linux-android" to "x86_64-linux-android26-clang"),
      "armeabi-v7a" to ("armv7-linux-androideabi" to "armv7a-linux-androideabi26-clang")
    )

    val cargoBin = listOf(
      File(System.getProperty("user.home"), ".cargo/bin/cargo"),
      File("/root/.cargo/bin/cargo"),
      File("/usr/local/bin/cargo"),
      File("/usr/bin/cargo")
    ).firstOrNull { it.exists() && it.canExecute() }?.absolutePath

    targets.forEach { (_, targetInfo) ->
      val (rustTarget, clangBinary) = targetInfo
      val targetReleaseDir = rustDir.resolve("target/$rustTarget/release")
      targetReleaseDir.mkdirs()
      val libFile = targetReleaseDir.resolve("libvaultnotes_rust.a")

      val linkerPath = File(llvmBin, clangBinary).absolutePath
      val arPath = File(llvmBin, "llvm-ar").absolutePath
      val rustTargetUpper = rustTarget.replace("-", "_").uppercase()

      var compiledWithRust = false
      if (cargoBin != null && llvmBin.exists()) {
        println("🦀 Compilando módulo nativo de Rust para $rustTarget...")
        val pb = ProcessBuilder(cargoBin, "build", "--target", rustTarget, "--release")
        pb.directory(rustDir)
        pb.environment()["CARGO_TARGET_${rustTargetUpper}_LINKER"] = linkerPath
        pb.environment()["CARGO_TARGET_${rustTargetUpper}_AR"] = arPath
        pb.environment()["CC_${rustTarget.replace("-", "_")}"] = linkerPath
        pb.environment()["AR_${rustTarget.replace("-", "_")}"] = arPath
        pb.environment()["PATH"] = "${llvmBin.absolutePath}:${System.getenv("PATH") ?: ""}:/root/.cargo/bin"
        pb.inheritIO()
        val process = pb.start()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
          compiledWithRust = true
          println("✅ Módulo Rust compilado exitosamente para $rustTarget")
        } else {
          println("⚠️ Cargo build devolvió código $exitCode para $rustTarget.")
        }
      }

      if (!compiledWithRust && (!libFile.exists() || libFile.lastModified() < rustDir.resolve("rust_shim.c").lastModified())) {
        println("⚙️ Compilando archivo estático nativo para $rustTarget mediante NDK Clang...")
        val shimC = rustDir.resolve("rust_shim.c")
        val clangBin = File(llvmBin, targetInfo.second).absolutePath
        val arBin = File(llvmBin, "llvm-ar").absolutePath
        val compilePb = ProcessBuilder(clangBin, "-O2", "-fPIC", "-c", shimC.absolutePath, "-o", File(targetReleaseDir, "shim.o").absolutePath)
        compilePb.inheritIO()
        val compileExit = compilePb.start().waitFor()
        if (compileExit != 0) {
          throw GradleException("Failed to compile native shim for $rustTarget with code $compileExit")
        }
        val arPb = ProcessBuilder(arBin, "rcs", libFile.absolutePath, File(targetReleaseDir, "shim.o").absolutePath)
        arPb.inheritIO()
        val arExit = arPb.start().waitFor()
        if (arExit != 0) {
          throw GradleException("Failed to archive native shim for $rustTarget with code $arExit")
        }
      }
    }
  }
}

tasks.matching { it.name.contains("CMake", ignoreCase = true) }.configureEach {
  dependsOn(cargoBuild)
}

androidComponents {
  onVariants(selector().withBuildType("release")) { variant ->
    variant.packaging.jniLibs.excludes.add("**/x86_64/**")
    variant.packaging.jniLibs.excludes.add("**/x86/**")
    variant.packaging.jniLibs.excludes.add("lib/x86_64/**")
    variant.packaging.jniLibs.excludes.add("lib/x86/**")
  }
}

tasks.matching { it.name == "mergeReleaseNativeLibs" }.configureEach {
  doLast {
    outputs.files.forEach { dir ->
      File(dir, "lib/x86_64").deleteRecursively()
      File(dir, "lib/x86").deleteRecursively()
      fileTree(dir) {
        include("**/x86_64/**", "**/x86/**")
      }.forEach { it.delete() }
    }
  }
}

tasks.matching { it.name == "stripReleaseDebugSymbols" }.configureEach {
  doLast {
    outputs.files.forEach { dir ->
      File(dir, "lib/x86_64").deleteRecursively()
      File(dir, "lib/x86").deleteRecursively()
      fileTree(dir) {
        include("**/x86_64/**", "**/x86/**")
      }.forEach { it.delete() }
    }
  }
}

tasks.matching { it.name == "packageRelease" }.configureEach {
  doLast {
    val releaseDir = file("build/outputs/apk/release")
    val targetApk = File(releaseDir, "Keeppr-v0.1.0-b-Release.apk")
    if (!targetApk.exists()) {
      val defaultApk = releaseDir.listFiles()?.firstOrNull { it.extension == "apk" }
      defaultApk?.copyTo(targetApk, overwrite = true)
    }
  }
}

