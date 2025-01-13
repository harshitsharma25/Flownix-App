import com.android.build.api.dsl.Packaging

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.flownix"
    compileSdk = 34

   packaging{
       resources.excludes.add("META-INF/DEPENDENCIES")
   }

    defaultConfig {
        applicationId = "com.example.flownix"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    // firesbase bom
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth:23.1.0")
    // firebase firestore
    implementation("com.google.firebase:firebase-firestore:25.1.1")

    //firbase oAuth (for notification)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // Circle image dependency
    implementation("de.hdodenhof:circleimageview:3.0.1")

    // glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    // dexter permissions
    implementation ("com.karumi:dexter:6.2.3")

    //firebase storage
    implementation("com.google.firebase:firebase-storage:21.0.1")

    // Activity Result API
    implementation ("androidx.activity:activity-ktx:1.7.0")
    implementation ("androidx.fragment:fragment-ktx:1.5.7")

    // firebase cloud messaging
    implementation("com.google.firebase:firebase-messaging:24.1.0")

    // retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ok Http
    implementation("com.squareup.okhttp3:okhttp:4.12.0")


    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("com.google.gms:google-services:4.4.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}