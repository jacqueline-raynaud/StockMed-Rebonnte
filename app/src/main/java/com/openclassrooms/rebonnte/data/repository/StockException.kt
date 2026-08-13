package com.openclassrooms.rebonnte.data.repository

/**
 * La raison d'un echec, exprimee en termes metier.
 *
 * Les depots ne laissent pas remonter les exceptions de Firebase : l'ecran
 * dependrait alors du fournisseur de base de donnees, et changer de fournisseur
 * demanderait de reecrire l'affichage des erreurs.
 *
 * Quatre raisons suffisent, parce que ce sont les quatre reactions possibles
 * pour l'operateur : il n'a pas le droit, il n'a pas de reseau, il doit
 * reessayer plus tard, ou il faut appeler quelqu'un.
 */
enum class StockErrorReason {
    /** Session expiree ou regles de securite : reessayer n'y changera rien. */
    PERMISSION,

    /** Reseau indisponible : le geste vaut la peine d'etre repete. */
    NETWORK,

    /** Service momentanement indisponible, quota atteint, ecriture concurrente. */
    UNAVAILABLE,

    /**
     * Le retrait demande depasse le stock reel.
     *
     * Ce n'est pas une panne : l'operation a ete refusee volontairement. Le
     * stock disponible accompagne l'erreur, pour que l'ecran puisse le dire.
     */
    INSUFFICIENT_STOCK,

    /** Tout le reste, y compris un index manquant — un defaut de configuration. */
    UNKNOWN
}

/** L'exception que les depots exposent, quelle que soit leur technologie. */
class StockException(
    val reason: StockErrorReason,
    cause: Throwable? = null,
    /** Renseigne pour [StockErrorReason.INSUFFICIENT_STOCK] uniquement. */
    val available: Int? = null
) : Exception(cause?.message, cause)
