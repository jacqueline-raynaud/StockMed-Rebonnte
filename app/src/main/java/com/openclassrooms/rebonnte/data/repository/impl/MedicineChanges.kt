package com.openclassrooms.rebonnte.data.repository.impl

import com.openclassrooms.rebonnte.data.model.MedicineDto

/**
 * Decrit une correction de fiche, ou rend `null` si rien n'a change.
 *
 * Partage par les deux implementations : le texte d'une entree d'historique ne
 * doit pas dependre de la base utilisee. Les deux depots produiraient sinon des
 * journaux differents pour la meme operation, et les tests ne prouveraient plus
 * rien de la production.
 *
 * Le texte est en francais et sans accent, comme les autres entrees deja en
 * base — c'est une **donnee**, pas un libelle d'interface : elle n'est pas
 * traduite, et elle doit rester lisible telle qu'elle a ete ecrite le jour de
 * l'operation.
 *
 * Limite assumee : le changement d'emplacement ne nomme que la destination. Le
 * depot des medicaments ne connait pas la collection des emplacements, et
 * l'appelant ne transmet que le libelle choisi. L'emplacement precedent se
 * retrouve dans l'entree d'historique anterieure.
 */
internal fun describeChanges(
    current: MedicineDto,
    newName: String,
    newAisleId: String,
    newAisleName: String
): String? {
    val changes = buildList {
        if (newName != current.name) {
            add("Nom modifie de « ${current.name} » a « $newName »")
        }
        if (newAisleId != current.aisleId) {
            add("Emplacement modifie : $newAisleName")
        }
    }
    return changes.takeIf { it.isNotEmpty() }?.joinToString(" ; ")
}
