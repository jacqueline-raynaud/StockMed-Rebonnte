package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 * Nature de l'operation tracee dans l'historique.
 *
 * [Keep] pour la meme raison que [History], et elle est ici encore plus
 * sensible : une enumeration est stockee sous la forme de son nom de constante.
 * `STOCK_CHANGE` renomme en `a` ne correspondrait plus a rien, et toutes les
 * entrees retomberaient sur la valeur par defaut du champ.
 */
@Keep
enum class HistoryAction {
    CREATE,
    UPDATE,
    STOCK_CHANGE,
    DELETE
}
