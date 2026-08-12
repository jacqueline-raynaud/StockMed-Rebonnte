package com.openclassrooms.rebonnte.ui

import com.openclassrooms.rebonnte.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * N'observe la source que tant qu'une session est ouverte, et rend [fallback]
 * sinon.
 *
 * Les regles de securite Firestore refusent toute lecture a un utilisateur non
 * authentifie, et un ecouteur refuse **fait planter l'application** : le
 * `callbackFlow` se ferme sur l'exception, qui remonte jusqu'au collecteur.
 *
 * Deux moments exposaient a ce refus :
 *
 * - **Avant la connexion**, si un ecran observait deja le stock.
 * - **A la deconnexion**, surtout : les etats sont partages en
 *   `WhileSubscribed(5_000)`, donc l'ecouteur survit cinq secondes a l'ecran
 *   qui l'observait. Firebase revoque la session pendant cette fenetre, et le
 *   refus arrive alors que plus personne n'attend de donnees.
 *
 * `flatMapLatest` annule l'ecouteur des que la session tombe : il n'y a plus
 * d'abonnement a refuser. Le probleme est traite a la source plutot qu'ecran
 * par ecran.
 *
 * Ce n'est pas une gestion d'erreurs — une panne reseau reste a traiter, c'est
 * l'objet de T-24. C'est la suppression d'une erreur que l'on provoquait
 * soi-meme.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.whileSignedIn(userRepository: UserRepository, fallback: T): Flow<T> {
    val source = this
    return userRepository.currentUser
        .map { it != null }
        .distinctUntilChanged()
        .flatMapLatest { signedIn -> if (signedIn) source else flowOf(fallback) }
}
