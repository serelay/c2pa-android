plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            consumerProguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.3")
    implementation("androidx.annotation:annotation:1.3.0")
    // For development, can remove jniLibs manually and use this, however then they will
    // not be packaged within the aar, which makes inclusion more complicated
    // The native package is solely for thumbnail usage
    // implementation(project(":android-libjpeg-turbo"))

    testImplementation("junit:junit:4.12")
    testImplementation("com.google.truth:truth:1.1.2")
    testImplementation("androidx.test.ext:junit:1.1.3")

    androidTestImplementation("com.google.truth:truth:1.1.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.annotation:annotation:1.3.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
}

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        named("main") {
            noAndroidSdkLink.set(false)
        }
    }
}