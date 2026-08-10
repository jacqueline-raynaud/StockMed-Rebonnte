package com.openclassrooms.rebonnte.ui.navigation

/**
 * Les destinations de l'application.
 *
 * Les ecrans de detail etaient auparavant des Activity distinctes, jointes par
 * des Intent et des extras textuels. Elles ne pouvaient pas partager le
 * ViewModel de MainActivity autrement qu'a travers une reference statique vers
 * elle — d'ou la fuite memoire. En destinations Compose, elles vivent dans le
 * meme graphe et recoivent simplement leur identifiant en argument de route.
 */
object Destinations {

    const val AUTH = "auth"
    const val WELCOME = "welcome"

    const val AISLE_LIST = "aisle"
    const val MEDICINE_LIST = "medicine"

    /** Formulaire de creation. Declare avant medicine/{id} pour ne pas etre
     *  capture comme un identifiant. */
    const val MEDICINE_NEW = "medicine/new"

    const val AISLE_ID_ARG = "aisleId"
    const val AISLE_DETAIL = "aisle/{$AISLE_ID_ARG}"

    const val MEDICINE_ID_ARG = "medicineId"
    const val MEDICINE_DETAIL = "medicine/{$MEDICINE_ID_ARG}"

    fun aisleDetail(aisleId: String) = "aisle/$aisleId"

    fun medicineDetail(medicineId: String) = "medicine/$medicineId"

    /** Les detail masquent la barre de navigation et le bouton d'ajout. */
    fun isDetail(route: String?) = route == AISLE_DETAIL || route == MEDICINE_DETAIL

    /**
     * Ecrans affiches hors de l'application proprement dite : ni barre
     * superieure, ni barre de navigation, ni bouton d'ajout.
     */
    fun isOutsideApp(route: String?) = route == AUTH || route == WELCOME

    /** Le formulaire masque aussi les barres : on y entre pour une seule tache. */
    fun isForm(route: String?) = route == MEDICINE_NEW
}
