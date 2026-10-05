plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.apollographql.apollo")
}

android {
    namespace = "example.uitestdsl"
    compileSdk = 36
    defaultConfig {
        applicationId = "example.uitestdsl"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("test").java.srcDir("src/sharedTest/kotlin")
    sourceSets.getByName("androidTest").java.srcDir("src/sharedTest/kotlin")
}
kotlin { jvmToolchain(17) }
apollo { service("demo") { packageName.set("example.uitestdsl.graphql") } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("io.insert-koin:koin-android:4.2.1")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("com.apollographql.apollo:apollo-runtime:4.4.3")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:5.3.0")
    androidTestImplementation("com.apollographql.apollo:apollo-testing-support:4.4.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.0")
}
