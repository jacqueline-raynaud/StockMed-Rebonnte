package com.openclassrooms.rebonnte.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Remplace Dispatchers.Main par un dispatcher de test.
 *
 * Sans cela, tout ViewModel utilisant viewModelScope echoue en test unitaire :
 * le Main d'Android n'existe pas sur la JVM.
 */
/**
 * [dispatcher] est expose pour pouvoir etre passe a `runTest` : sans cela,
 * `backgroundScope` tourne sur le dispatcher standard de runTest et les
 * collecteurs lances dedans ne demarrent pas avant la fin du test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
