import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.theskillguru"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.theskillguru"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures{
        viewBinding=true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.nearby)
    implementation(libs.androidx.ui.desktop)
    implementation(libs.firebase.functions.ktx)
    testImplementation(libs.junit)
    implementation(libs.ccp)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation (libs.whiteboard.android)
    implementation (libs.full.sdk)
//    implementation 'com.github.AgoraIO-Community.VideoUIKit-Android:final:v4.0.1'
//    implementation 'com.github.AgoraIO-Community:Android-UIKit:v2.0.0'
//    implementation 'com.github.AgoraIO-Community:VideoUIKit-Android:v4.0.1'
    implementation("commons-codec:commons-codec:1.15")
    implementation (libs.okhttp.v490)
    implementation (libs.squareup.logging.interceptor)

    implementation (libs.androidx.cardview)
    implementation (libs.holocolorpicker)
    implementation ("com.airbnb.android:lottie:6.3.0")
    // json parse moshi



// retrofit api library with retrofit moshi


// gson for json conversion

    implementation (libs.converter.gson)

// kotlin coroutines

    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android.v164)


//    implementation (libs.squareup.okhttp)
    implementation (libs.gson)
    implementation (libs.firebase.messaging.ktx)

    implementation (libs.squareup.okhttp)

//    implementation (libs.firebase.admin)

    implementation (libs.jetbrains.kotlinx.coroutines.android)
    implementation (libs.kotlinx.coroutines.play.services)

    implementation (libs.google.auth.library.oauth2.http)

    // Retrofit
    implementation (libs.retrofit)
    implementation (libs.converter.gson)




//    implementation (libs.play.services.base)
//    implementation (libs.gms.play.services.auth.v2060)



}