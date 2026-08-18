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
        // Un versionCode distinct pour chaque distribution
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"

        // Runner personnalise : HiltTestApplication avant que le test ne demarre.
        testInstrumentationRunner = "com.openclassrooms.rebonnte.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

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
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        //  A retirer lors d'une montee de version du BOM Compose.
        disable += "StateFlowValueCalledInComposition"
        disable += "CoroutineCreationDuringComposition"
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
 */
val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test.class",
    "**/*Test$*.class",
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
    mustRunAfter("connectedDebugAndroidTest")

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
            include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
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
        // atteint le seuil de la porte "Sonar way" soit 80%.
        property("sonar.androidLint.reportPaths", "")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )
    }
}

