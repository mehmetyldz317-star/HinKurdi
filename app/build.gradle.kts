plugins { id("com.android.application") }

android {
    namespace = "com.hinkurdi.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.hinkurdi.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 20
        versionName = "0.20.0-rc2"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
