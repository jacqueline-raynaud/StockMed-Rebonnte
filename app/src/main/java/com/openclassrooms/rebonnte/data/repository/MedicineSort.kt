package com.openclassrooms.rebonnte.data.repository

/**
 * Critere de tri demande a la source de donnees.
 *
 * Le tri est un parametre de requete, pas une mutation de la liste : l'ancien
 * code reordonnait la source de verite elle-meme. Cote Firestore, ces valeurs se
 * traduisent directement en `orderBy` avec un sens.
 *
 * Le tri par nom s'appuie sur le champ en minuscules : un tri lexicographique
 * brut placerait « Zovirax » avant « aspirine », les majuscules precedant les
 * minuscules. Ce n'est pas l'ordre alphabetique attendu par un operateur.
 */
enum class MedicineSort {
    NONE,
    NAME_ASC,
    NAME_DESC,
    STOCK_ASC,
    STOCK_DESC
}
