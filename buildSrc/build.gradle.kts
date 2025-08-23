plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.kotlinSerialization)
    implementation(libs.gradlePlugin.kotlinDokka)
    implementation(libs.gradlePlugin.kotlinxBenchmark)
    implementation(libs.gradlePlugin.mavenPublish)
}
