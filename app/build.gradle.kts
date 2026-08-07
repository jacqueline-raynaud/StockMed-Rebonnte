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

android {
    namespace = "com.openclassrooms.rebonnte"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.openclassrooms.rebonnte"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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



    // tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
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
    description = "Genere le rapport de couverture des tests unitaires."
    group = JavaBasePlugin.VERIFICATION_GROUP

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            // Classes Kotlin : avec AGP 8.5 et le plugin kotlin-android, elles
            // atterrissent ici et non dans intermediates/built_in_kotlinc.
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

