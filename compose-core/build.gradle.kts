
import com.qmuiteam.plugin.Dep

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
    id("qmui-publish")
}

version = Dep.QMUI.composeCoreVer

android {
    compileSdk = Dep.compileSdk

    buildFeatures {
        compose = true
    }

    defaultConfig {
        minSdk = Dep.minSdk
        targetSdk = Dep.targetSdk
    }

    buildTypes {
        getByName("release"){
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = Dep.javaVersion
        targetCompatibility = Dep.javaVersion
    }
    kotlinOptions {
        jvmTarget = Dep.kotlinJvmTarget
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Dep.Compose.version
    }
}

dependencies {
    api(Dep.AndroidX.annotation)
    api(Dep.AndroidX.appcompat)
    api(Dep.AndroidX.coreKtx)
    api(Dep.Compose.ui)
    api(Dep.Compose.animation)
    api(Dep.Compose.material)
    api(Dep.Compose.compiler)
}