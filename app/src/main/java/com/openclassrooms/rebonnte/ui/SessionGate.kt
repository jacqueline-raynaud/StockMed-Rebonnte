package com.openclassrooms.rebonnte.ui

import com.openclassrooms.rebonnte.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.whileSignedIn(userRepository: UserRepository, fallback: T): Flow<T> {
    val source = this
    return userRepository.currentUser
        .map { it != null }
        .distinctUntilChanged()
        .flatMapLatest { signedIn -> if (signedIn) source else flowOf(fallback) }
}
