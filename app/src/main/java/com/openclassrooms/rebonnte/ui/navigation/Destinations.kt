package com.openclassrooms.rebonnte.ui.navigation


object Destinations {

    const val AUTH = "auth"
    const val WELCOME = "welcome"

    const val AISLE_LIST = "aisle"
    const val MEDICINE_LIST = "medicine"

    const val MEDICINE_NEW = "medicine/new"

    const val AISLE_ID_ARG = "aisleId"
    const val AISLE_DETAIL = "aisle/{$AISLE_ID_ARG}"

    const val MEDICINE_ID_ARG = "medicineId"
    const val MEDICINE_DETAIL = "medicine/{$MEDICINE_ID_ARG}"

    const val MEDICINE_EDIT = "medicine/{$MEDICINE_ID_ARG}/edit"

    fun aisleDetail(aisleId: String) = "aisle/$aisleId"

    fun medicineDetail(medicineId: String) = "medicine/$medicineId"

    fun medicineEdit(medicineId: String) = "medicine/$medicineId/edit"

    // without navigation bar and add button
    fun isDetail(route: String?) = route == AISLE_DETAIL || route == MEDICINE_DETAIL

    // screen without top bar, navigation bar and add button
    fun isOutsideApp(route: String?) = route == AUTH || route == WELCOME

    // form without top bar, navigation bar and add button
    fun isForm(route: String?) = route == MEDICINE_NEW || route == MEDICINE_EDIT
}
