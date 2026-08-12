package com.openclassrooms.rebonnte.ui.model

import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.data.model.MedicineDto

/**
 * Un medicament tel que l'ecran l'affiche.
 *
 * Le [MedicineDto] porte un `aisleId`, qui ne veut rien dire pour un operateur.
 * Le modele d'affichage porte le **libelle** de l'emplacement, deja resolu :
 * l'ecran n'a plus a croiser deux listes pour savoir ou ranger une boite.
 *
 * [locationName] est nullable et non « Emplacement inconnu » : le libelle de
 * remplacement est une ressource, il se resout dans la langue du telephone au
 * moment de l'affichage. Le ViewModel n'a pas a le connaitre.
 *
 * [Immutable] indique a Compose que rien ne changera derriere son dos : il peut
 * alors sauter la recomposition des lignes inchangees.
 */
@Immutable
data class MedicineUi(
    val id: String,
    val name: String,
    val stock: Int,
    val aisleId: String,
    val locationName: String?
)

fun MedicineDto.toUi(locationName: String?): MedicineUi = MedicineUi(
    id = id,
    name = name,
    stock = stock,
    aisleId = aisleId,
    locationName = locationName
)
