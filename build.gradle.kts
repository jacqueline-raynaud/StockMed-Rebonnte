// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    // Declare ici, applique dans :app. Sans cette ligne, le plugin Sonar vit
    // dans un classloader enfant de celui d'AGP : par delegation parent-first,
    // le commons-compress 1.21 d'AGP masque le 1.28.0 dont le scanner a besoin,
    // et la tache echoue sur TarArchiveInputStream.getNextEntry().
    alias(libs.plugins.sonarqube) apply false
}