package com.openclassrooms.rebonnte

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Substitue HiltTestApplication a RebonnteApplication pendant les tests
 * instrumentes : sans cela, Hilt ne peut pas remplacer les modules.
 *
 * Declare dans testInstrumentationRunner (app/build.gradle.kts).
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
