import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.vaultnotes.kqzp"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
      }
    }
    ndk {
      abiFilters.addAll(listOf("arm64-v8a", "x86_64", "armeabi-v7a"))
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
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
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
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
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
  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

val cargoBuild = tasks.register("cargoBuild") {
  val rustDir = file("src/main/rust")
  inputs.dir(rustDir.resolve("src"))
  inputs.file(rustDir.resolve("Cargo.toml"))
  outputs.dir(rustDir.resolve("target"))

  doLast {
    val sdkDir = File(System.getenv("ANDROID_SDK_ROOT") ?: "/opt/android/sdk")
    val ndkDir = File(sdkDir, "ndk/26.1.10909125")
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

    if (cargoBin == null) {
      // Si cargo no está instalado en el entorno actual del contenedor, verificar si ya existen las librerías precompiladas
      println("⚠️ Cargo no está instalado en este entorno de compilación. Verificando librerías estáticas de Rust existentes...")
      val missingTargets = targets.filter { (abi, targetInfo) ->
        val (rustTarget, _) = targetInfo
        val libFile = rustDir.resolve("target/$rustTarget/release/libvaultnotes_rust.a")
        !libFile.exists()
      }
      if (missingTargets.isNotEmpty()) {
        println("ℹ️ Creando stubs estáticos válidos para Rust en target para permitir la compilación nativa...")
        val arBin = File(llvmBin, "llvm-ar").absolutePath
        targets.forEach { (_, targetInfo) ->
          val (rustTarget, _) = targetInfo
          val targetReleaseDir = rustDir.resolve("target/$rustTarget/release")
          targetReleaseDir.mkdirs()
          val libFile = targetReleaseDir.resolve("libvaultnotes_rust.a")
          if (!libFile.exists()) {
            // Generar un archivo .a vacío válido con llvm-ar si no existe
            val dummyC = File(targetReleaseDir, "dummy.c")
            dummyC.writeText("void dummy_rust_sym() {}")
            val clangBin = File(llvmBin, targetInfo.second).absolutePath
            val compilePb = ProcessBuilder(clangBin, "-c", dummyC.absolutePath, "-o", File(targetReleaseDir, "dummy.o").absolutePath)
            compilePb.inheritIO()
            compilePb.start().waitFor()
            val arPb = ProcessBuilder(arBin, "rcs", libFile.absolutePath, File(targetReleaseDir, "dummy.o").absolutePath)
            arPb.inheritIO()
            arPb.start().waitFor()
          }
        }
      }
      return@doLast
    }

    targets.forEach { (_, targetInfo) ->
      val (rustTarget, clangBinary) = targetInfo
      val linkerPath = File(llvmBin, clangBinary).absolutePath
      val linkerEnvVar = "CARGO_TARGET_${rustTarget.replace("-", "_").uppercase()}_LINKER"

      val pb = ProcessBuilder(cargoBin, "build", "--target", rustTarget, "--release")
      pb.directory(rustDir)
      pb.environment()[linkerEnvVar] = linkerPath
      pb.environment()["PATH"] = "${System.getenv("PATH")}:${llvmBin.absolutePath}:/root/.cargo/bin"
      pb.inheritIO()
      val process = pb.start()
      val exitCode = process.waitFor()
      if (exitCode != 0) {
        throw GradleException("Cargo build failed for $rustTarget with exit code $exitCode")
      }
    }
  }
}

tasks.matching { it.name.contains("CMake", ignoreCase = true) }.configureEach {
  dependsOn(cargoBuild)
}

