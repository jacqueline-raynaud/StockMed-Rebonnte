package com.openclassrooms.rebonnte.data.model

import androidx.annotation.Keep

/**
 * Un medicament du stock.
 *
 * L'identifiant vient de la source de donnees (id du document Firestore) et
 * n'est pas deduit du nom : deux medicaments peuvent etre homonymes, et un nom
 * mal saisi doit pouvoir etre corrige sans casser les references existantes.
 *
 * Le rayon est reference par son identifiant et non par son libelle, pour que
 * renommer un rayon ne detache pas les medicaments qu'il contient.
 *
 * L'historique n'est volontairement pas porte ici : voir MedicineRepository.
 *
 * Toutes les proprietes ont une valeur par defaut : Firestore a besoin d'un
 * constructeur sans argument pour deserialiser.
 *
 * [Keep] protege les noms de champs de l'obfuscation. Firestore remplit cette
 * classe par reflexion, en comparant le nom du champ a la cle du document : si
 * R8 renomme `stock` en `a`, la correspondance est perdue et le champ garde sa
 * valeur par defaut, **sans aucune erreur**. La liste s'afficherait vide.
 */
@Keep
data class MedicineDto(
    val id: String = "",
    val name: String = "",
    val stock: Int = 0,
    val aisleId: String = ""
)
