package com.example.easycook

import com.example.easycook.data.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Les `uiState` des ViewModels ne calculent rien sans abonné (`WhileSubscribed`), comme à l'écran :
 * on les observe pendant tout le test, et on renvoie tout ce qu'ils émettent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.collectAll(flow: Flow<T>): List<T> {
    val received = mutableListOf<T>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { received += it } }
    return received
}

/** Repository branché sur le faux DAO, dont les traitements « en arrière-plan » s'exécutent aussitôt. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.testRepository(
    dao: FakeRecipeDao = FakeRecipeDao(),
    images: FakeImageFiles? = null
) = RecipeRepository(dao, images, UnconfinedTestDispatcher(testScheduler), clock = { 0L })
