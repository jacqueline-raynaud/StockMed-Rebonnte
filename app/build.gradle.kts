import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.sonarqube)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

/**
 * Parametres de signature lus depuis local.properties, jamais versionne.
 *
 * La CI ecrit ce fichier a partir de secrets GitHub. En local, il est absent :
 * les builds debug fonctionnent sans, et seul assembleRelease reclame une
 * signature.
 */
val signingProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingProperty(name: String): String? =
    signingProperties.getProperty(name) ?: System.getenv(name)

val keystoreFile: File? = signingProperty("storeFile")
    ?.let { file(it) }
    ?.takeIf { it.exists() }

android {
    namespace = "com.openclassrooms.rebonnte"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.openclassrooms.rebonnte"
        minSdk = 24
        targetSdk = 34
        // Chaque distribution doit porter un versionCode distinct, sinon App
        // Distribution presente la nouvelle version comme identique a la
        // precedente. La CI fournit le numero du run ; en local, 1 suffit.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"

        // Runner personnalise : Hilt doit remplacer l'Application par
        // HiltTestApplication avant que le test ne demarre.
        testInstrumentationRunner = "com.openclassrooms.rebonnte.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Declaree uniquement si le keystore est present : sans cela, toute
    // configuration Gradle echouerait sur un poste qui n'a pas la cle, y compris
    // pour un simple assembleDebug.
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = keystoreFile
                storePassword = signingProperty("storePassword")
                keyAlias = signingProperty("keyAlias")
                keyPassword = signingProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Sans ce drapeau, testDebugUnitTest ne produit aucun fichier .exec
            // et le rapport JaCoCo est vide : c'est ce qui donnait 0 % dans
            // SonarCloud alors meme que les tests passaient.
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Absente en local sans keystore : le build reste possible, l'APK
            // produit n'est simplement pas signe.
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        // StateFlowValueCalledInComposition ne signale rien ici : son detecteur
        // *plante*. Le lint de Compose fourni par le BOM 2024.04.01 embarque
        // kotlinx-metadata-jvm 2.0, qui ne sait pas lire les metadonnees des
        // classes compilees en Kotlin 2.1 — celles du projet.
        //
        // Desactivation ciblee plutot que `abortOnError = false` : les autres
        // regles continuent de bloquer la construction. A retirer lors d'une
        // montee de version du BOM Compose.
        disable += "StateFlowValueCalledInComposition"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    // Hilt
    implementation(libs.hilt.android)
    debugImplementation(libs.leakcanary)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)



    // tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

/**
 * Rapport de couverture des tests unitaires.
 *
 * Le plugin JaCoCo standard ne connait pas les variantes Android : il faut lui
 * designer explicitement les classes compilees et les donnees d'execution.
 *
 * Les tests instrumentes (connectedDebugAndroidTest) ne sont volontairement pas
 * inclus : ils exigent un emulateur que la CI ne lance pas encore. Ils seront
 * ajoutes avec le job emulateur.
 */
val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test.class",
    "**/*Test$*.class",
    // Code genere : l'inclure ferait chuter la couverture sans rien dire de la
    // qualite du code ecrit a la main.
    "**/di/**",
    "**/*Module*",
    "**/Hilt_*",
    "**/*_Hilt*",
    "**/*_Factory*",
    "**/*_MembersInjector*",
    "**/*_GeneratedInjector*",
    "**/hilt_aggregated_deps/**",
    "**/dagger/hilt/**",
    // Composables : non couvrables par des tests unitaires JVM, ils relevent
    // des tests d'interface.
    "**/*ComposableSingletons*",
    "**/ComposableSingletons*"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    description = "Generates xml coverage report for this project."
    group = JavaBasePlugin.VERIFICATION_GROUP

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            // Classes Kotlin : avec AGP 8.5 et le plugin kotlin-android
            fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
                exclude(jacocoExcludes)
            },
            fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
                exclude(jacocoExcludes)
            }
        )
    )

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
        }
    )
}

sonar {
    properties {
        property("sonar.projectKey", "jacqueline-raynaud_StockMed-Rebonnte")
        property("sonar.organization", "jacqueline-raynaud")
        property("sonar.projectName", "StockMed-Rebonnte")
        property("sonar.host.url", "https://sonarcloud.io")
        // sonar.qualitygate.wait reste retire tant que la couverture n'aura pas
        // atteint le seuil de la porte "Sonar way".
        property("sonar.androidLint.reportPaths", "")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )
    }
}

