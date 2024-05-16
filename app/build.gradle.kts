@file:Suppress("UnstableApiUsage")

    plugins {
        alias(libs.plugins.kotlin)
        alias(libs.plugins.agp.app)
        id("com.google.gms.google-services")
    }

    android {
        compileSdk = 34
        namespace = "com.fabiosilva.packconvert"

        defaultConfig {
            minSdk = 26
            targetSdk = 34
            versionCode = 6
            versionName = "1.6"
            applicationId = namespace
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                isCrunchPngs = false
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        kotlinOptions {
            jvmTarget = "17"
        }
        buildFeatures {
            viewBinding = true
        }
        android.packagingOptions.jniLibs.useLegacyPackaging = true
    }

    dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.lifecycle.common)

        implementation(libs.google.guava)
        implementation(libs.google.material)
        implementation(libs.google.protobuf.java)

        implementation(libs.bcprov.jdk15on)
        implementation(libs.android.tools.zipflinger)
        implementation(libs.android.tools.signflinger)
        implementation(libs.android.tools.bundletool)
        implementation(libs.play.services.ads)

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.test.junit)
        androidTestImplementation(libs.androidx.test.espresso.core)
    }
