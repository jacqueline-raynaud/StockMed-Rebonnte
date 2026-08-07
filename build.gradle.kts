// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    configurations.classpath {
        resolutionStrategy {
            // Le plugin Sonar, applique dans :app, appelle
            // TarArchiveInputStream.getNextEntry() avec le type de retour
            // introduit par commons-compress 1.24. AGP apporte la 1.21 sur le
            // classpath racine, qui est le classloader PARENT de celui de :app :
            // par delegation parent-first, la 1.21 masque la version du plugin
            // et la tache sonar echoue sur une methode introuvable.
            //
            // On aligne donc uniquement cette bibliotheque a la racine. Declarer
            // le plugin Sonar dans le bloc plugins racine reglerait aussi le
            // probleme, mais ferait monter toutes ses dependances transitives
            // sur le classpath d'AGP : bcprov passerait alors en 1.84 pendant
            // que bcpkix et bcutil resteraient en 1.77, panachage que
            // BouncyCastle ne supporte pas et qui casse validateSigningDebug.
            force("org.apache.commons:commons-compress:1.28.0")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}