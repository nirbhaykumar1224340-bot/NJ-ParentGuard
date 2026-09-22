plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.kapt")
}
android {
    namespace="com.nj.parentguard"
    compileSdk=35
    defaultConfig { applicationId="com.nj.parentguard"; minSdk=26; targetSdk=35; versionCode=1; versionName="0.1.0" }
    buildFeatures { compose=true }
    kotlinOptions { jvmTarget="17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.0-rc03")
    implementation("androidx.room:room-ktx:2.7.0-rc03")
    kapt("androidx.room:room-compiler:2.7.0-rc03")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
}
