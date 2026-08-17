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

    /**
     * Correction d'une fiche existante. Le segment final la distingue du
     * detail : « medicine/abc » et « medicine/abc/edit » ne se confondent pas.
     */
    const val MEDICINE_EDIT = "medicine/{$MEDICINE_ID_ARG}/edit"

    fun aisleDetail(aisleId: String) = "aisle/$aisleId"

    fun medicineDetail(medicineId: String) = "medicine/$medicineId"

    fun medicineEdit(medicineId: String) = "medicine/$medicineId/edit"

    /** Les detail masquent la barre de navigation et le bouton d'ajout. */
    fun isDetail(route: String?) = route == AISLE_DETAIL || route == MEDICINE_DETAIL

    /**
     * Ecrans affiches hors de l'application proprement dite : ni barre
     * superieure, ni barre de navigation, ni bouton d'ajout.
     */
    fun isOutsideApp(route: String?) = route == AUTH || route == WELCOME

    /** Les formulaires masquent aussi les barres : on y entre pour une seule tache. */
    fun isForm(route: String?) = route == MEDICINE_NEW || route == MEDICINE_EDIT
}
