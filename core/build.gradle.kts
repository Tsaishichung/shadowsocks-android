import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("com.android.library")
    id("org.mozilla.rust-android-gradle.rust-android")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

setupCore()

android {
    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")

        externalNativeBuild.ndkBuild {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            arguments("-j${Runtime.getRuntime().availableProcessors()}")
        }

        javaCompileOptions.annotationProcessorOptions.arguments(mapOf(
                "room.incremental" to "true",
                "room.schemaLocation" to "$projectDir/schemas"))
    }

    externalNativeBuild.ndkBuild.path("src/main/jni/Android.mk")

    sourceSets.getByName("androidTest") {
        assets.setSrcDirs(assets.srcDirs + files("$projectDir/schemas"))
    }
}

cargo {
    module = "src/main/rust/shadowsocks-rust"
    libname = "sslocal"
    targets = listOf("arm", "arm64", "x86", "x86_64")
    profile = findProperty("CARGO_PROFILE")?.toString() ?: currentFlavor
    extraCargoBuildArguments = listOf("--bin", libname!!)
    featureSpec.noDefaultBut(arrayOf(
            "ring-aead-ciphers",
            "sodium",
            "rc4",
            "aes-cfb",
            "aes-ctr",
            "camellia-cfb",
            "openssl-vendored",
            "single-threaded",
            "local-flow-stat",
            "local-dns-relay"))
    exec = { spec, toolchain ->
        spec.environment("RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY", "$projectDir/$module/../linker-wrapper.py")
        spec.environment("RUST_ANDROID_GRADLE_TARGET", "target/${toolchain.target}/$profile/lib$libname.so")
    }
}

tasks.whenTaskAdded {
    when (name) {
        "mergeDebugJniLibFolders", "mergeReleaseJniLibFolders" -> dependsOn("cargoBuild")
    }
}

tasks.register<Exec>("cargoClean") {
    executable("cargo")     // cargo.cargoCommand
    args("clean")
    workingDir("$projectDir/${cargo.module}")
}
tasks.clean.dependsOn("cargoClean")

dependencies {
    val coroutinesVersion = "1.4.2"
    val roomVersion = "2.2.5"
    val workVersion = "2.4.0"

    api(project(":plugin"))
    api("androidx.appcompat:appcompat:1.2.0")
    api("androidx.core:core-ktx:1.5.0-alpha05")

    api("androidx.fragment:fragment-ktx:1.3.0-beta02")
    api("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    api("androidx.lifecycle:lifecycle-livedata-core-ktx:$lifecycleVersion")
    api("androidx.preference:preference:1.1.1")
    api("androidx.room:room-runtime:$roomVersion")
    api("androidx.work:work-runtime-ktx:$workVersion")
    api("androidx.work:work-gcm:$workVersion")
    api("com.google.android.gms:play-services-oss-licenses:17.0.0")
    api("com.google.code.gson:gson:2.8.6")
    api("com.google.firebase:firebase-analytics-ktx:18.0.0")
    api("com.google.firebase:firebase-crashlytics:17.3.0")
    api("com.jakewharton.timber:timber:4.7.1")
    api("dnsjava:dnsjava:3.3.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.2")
}
