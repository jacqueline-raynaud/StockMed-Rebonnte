package com.openclassrooms.rebonnte.data.repository

/**
 * Critere de tri demande a la source de donnees.
 *
 * Le tri est un parametre de requete, pas une mutation de la liste : l'ancien
 * code reordonnait la source de verite elle-meme. Cote Firestore, ces valeurs
 * se traduiront directement en `orderBy`.
 */
enum class MedicineSort {
    NONE,
    NAME,
    STOCK
}
