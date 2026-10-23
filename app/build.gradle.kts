import java.net.URI

plugins {
    alias(libs.plugins.android.application)
}

// The ISMLTS website the app opens. The live site is the default; point a test build somewhere else with
// ./gradlew assembleDebug -Pismlts.siteUrl=https://<another site>  (or set ismlts.siteUrl in gradle.properties)
val siteUrl: String = providers.gradleProperty("ismlts.siteUrl")
    .orElse("https://rbiwil-gda3haasc2eye8e2.southafricanorth-01.azurewebsites.net")
    .get()
    .trim()
    .trimEnd('/')

android {
    namespace = "com.pantech.ismltsapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pantech.ismltsapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SITE_URL", "\"$siteUrl\"")
        // Used by the App Links intent filter in AndroidManifest.xml
        manifestPlaceholders["siteHost"] = URI(siteUrl).host
    }

    buildTypes {
        release {
            // R8 shrinks and obfuscates release builds; the app has no reflection or JavaScript bridge to keep
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
