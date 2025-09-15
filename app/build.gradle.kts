plugins {
    alias(libs.plugins.android.application)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.gussanxz.orgafacil"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gussanxz.orgafacil"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        android.defaultConfig.vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Dependencias Firebase
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")//Analytics firebase
    implementation("com.google.firebase:firebase-auth")//Auth firebase
    implementation("com.google.firebase:firebase-firestore")//Firestore firebase. Detabase
    implementation("com.google.firebase:firebase-storage")//Storeage firebase. Armazenamento de imagem


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries

    implementation("com.github.heinrichreimer:material-intro:2.0.0")

    //added FloatActionButton
    implementation("com.github.clans:fab:1.6.4")

    //added material calendarview from: https://github.com/prolificinteractive/material-calendarview
    implementation("com.github.prolificinteractive:material-calendarview:1.6.0") {
        exclude(group = "com.android.support", module = "support-compat")
    }

    implementation("androidx.cardview:cardview:1.0.0")

    implementation ("com.google.android.gms:play-services-auth:21.0.0")

}