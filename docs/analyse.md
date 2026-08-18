# Analyse de l'existant

Les défauts du code original, **regroupés par cause**.

---

## Historique des mouvements de stocks

`ui/medicine/MedicineDetailActivity.kt` :

```kotlin
medicines[medicines.size].histories.toMutableList().add(
    History(
        medicine.name,
        "efeza56f1e65f",
        Date().toString(),
        "Updated medicine details"
    )
)
stock++
```

Cette expression montre **plusieurs défauts **.

### 1. L'index est hors bornes

`medicines[medicines.size]` : l'index valide maximum d'une liste est `size - 1`.
L'accès lève une `IndexOutOfBoundsException` à *chaque* appui, quelle que soit la
taille de la liste.

Correspond à :  « les boutons +1/-1 ferment involontairement l'application » du Product
Owner.

### 2. l'index est un piège

Corriger en `medicines[medicines.size - 1]` ou `medicine.last()` supprime le plantage.

Pour autant, cet index désigne le **dernier médicament de la liste**, pas celui affiché à
l'écran. On consulte « Doliprane », on incrémente, et l'historique part sur un
autre médicament.

### 3. L'écriture est une opération nulle

`toMutableList()` retourne une **copie** de la liste histories. l'appel à `.addd()`modifie cette
copie puis la copie est abandonnée

Sans doute **C'est la cause du problème remonté
par le service qualité** — « certaines actions apparaissent de manière sporadique
ou sont totalement manquantes »

`val updatedMedicine = medicine.copy(
    histories = medicine.histories + History(
        medicine.name,
        "efeza56f1e65f",
        Date().toString(),
        "Updated medicine details"
    )`

Impossible de vérifier le problème, l'historique n'apparait pas du tout sur le build

### 4. le stock disparait

Le stock est dans une variable locale au composable.
Il est donc perdu dès que l'on quitte la composition (rotation, navigation)

```kotlin
var stock by remember { mutableStateOf(medicine.stock) }
```

Il devrait remonter dans un viewModel.

---

## Fuite mémoire n°1 — l'enregistrement récursif

`MainActivity.kt`. Le cycle :

```
onCreate()
  └─> startBroadcastReceiver()
        ├─> registerReceiver(receiver #1)
        └─> Handler().postDelayed(200 ms) ─> startMyBroadcast()
                                               ├─> sendBroadcast()
                                               └─> startBroadcastReceiver()   ← récursion
                                                     ├─> registerReceiver(#2)
                                                     └─> postDelayed(200 ms) ─> …
```

Conséquences :

- **Une boucle infinie** cadencée à 200 ms, active tant que le processus vit.
- À chaque tour, `myBroadcastReceiver` est réaffecté : la référence vers
  l'instance précédente est perdue, mais celle-ci **reste enregistrée auprès du
  système**. Aucun `unregisterReceiver` ou `onDestroy`n'existe. Le
  nombre de receivers vivants croit ainsi que le nombre de `Toast` déclenchés.
- Chaque receiver retient l'`Activity` via la référence statique (voir plus bas)
  et empêchait sa libération.

**L'impact green code est direct** : le thread principal est réveillé cinq fois
par seconde, sans aucune fonctionnalité. Le broadcast n'affiche
qu'un `Toast` « Update reçu ».

C'est la fuite la plus visible sur l'Android Profiler, et celle qui a été retenue
pour la démonstration avant/après.

---

## Fuite mémoire n°2 — la référence statique

```kotlin
companion object {
    lateinit var mainActivity: MainActivity
}
```

Une référence statique vers une `Activity` survit à sa destruction. À chaque
rotation ou recréation de configuration, l'instance précédente est retenue
**avec toute sa hiérarchie de vues**.

Le companion permet de partager le `MedicineViewModel` entre plusieurs `Activity` :

```kotlin
val viewModel = ViewModelProvider(MainActivity.mainActivity)[MedicineViewModel::class.java]
```

— dans `MedicineDetailActivity` et dans `AisleDetailActivity`.

!!! info "note d'architecture"

    Les écrans de détail sont des `Activity` distinctes alors que le reste de
    l'application utilise Navigation Compose. 
    Il n'y a pas d'intérêt à avoir plusieurs activité. Les détails doivent devenir
    des destinations Compose.

---

## Le filtre des médicaments détruit les données

Ce problème n'est pas documenté mais apparaît lors du test de l'application fournie
Le champ de recherche, affiche la liste correspondante mais l'effacement du filtre ne rétablit pas
la liste d'origine.

`ui/medicine/MedicineViewModel.kt` :

```kotlin
fun filterByName(name: String) {
    val currentMedicines: List<Medicine> = medicines.value
    val filteredMedicines: MutableList<Medicine> = ArrayList()
    for (medicine in currentMedicines) {
        if (medicine.name.lowercase().contains(name.lowercase())) {
            filteredMedicines.add(medicine)
        }
    }
    _medicines.value = filteredMedicines   // ← la source est écrasée
}
```

Les médicaments non correspondants à la recherche sont **supprimés**.
Même remarque pour `sortByName` et `sortByStock`, qui réordonne la source d'origine

Une demande est active pour utiliser Firestore pour réaliser le tri et le filtre. Cette partie sera
traitée à ce moment-là.


---

## Les autres défauts relevés

Moins spectaculaires, mais relevés à la lecture.

### Modèles

| Défaut                                                | Conséquence                                                                                                 |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `Medicine` sans identifiant                           | Retrouvé par son nom (`find { it.name == name }`), transporté par `putExtra`. Deux homonymes indiscernables |
| `Aisle` et `History` en classes ordinaires avec `var` | Pas d'égalité structurelle, donc pas de clés stables pour les listes                                        |
| `History.date` en `String`                            | Ne se trie pas, dépend de la locale de celui qui l'a écrite                                                 |
| `History.userId` codé en dur : `"efeza56f1e65f"`      | L'identifiant illisible signalé par le Product Owner                                                        |

### Navigation et recomposition

| Défaut                                                                             | Conséquence                                                        |
|------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `navigate()` sans `popUpTo` ni `launchSingleTop`                                   | La back-stack s'empile indéfiniment à chaque bascule d'onglet      |
| `currentRoute()` recollecte la back-stack                                          | Recompositions inutiles, alors que la valeur était déjà disponible |
| `items(medicines)` sans paramètre `key`                                            | Recompositions inutiles à chaque changement de liste               |
| `?: return` au milieu d'un composable                                              | Écran blanc silencieux si le médicament est introuvable            |

### Interface et accessibilité

| Défaut                                                  | Conséquence                                           |
|---------------------------------------------------------|-------------------------------------------------------|
| `contentDescription = null` sur les icônes actionnables | Inutilisables sous TalkBack                           |
| `contentDescription = "Arrow"`                          | Libellé sans valeur pour un lecteur d'écran           |
| `Color.Gray` codé en dur                                | Illisible en mode sombre, contraste non conforme      |
| Thème parent `Theme.Material.Light` figé                | Le mode sombre ne peut pas fonctionner                |
| Tous les libellés dans le code                          | `strings.xml` ne contient que le nom de l'application |
| `MedicineItem` défini **deux fois**                     | Deux signatures et deux styles incompatibles          |

### Chaîne de build et outillage

| Défaut                                                             | Conséquence                                                             |
|--------------------------------------------------------------------|-------------------------------------------------------------------------|
| Java 8, Kotlin 1.9                                                 | Chaîne obsolète                                                         |
| Aucun `signingConfig` en release                                   | Bloquant pour la distribution d'un APK                                  |
| Receiver déclaré dans le manifeste **et** enregistré dynamiquement | Doublon, et une classe imbriquée dans une `Activity` exposée au système |
| Aucun test, aucune intégration continue                            | Aucun filet                                                             |

---

## Une ambiguïté dans les demandes

Une note du Product Owner mentionne :

> Intégrer l'historique dans le contenu scrollable de la fiche détail d'un
> magasin serait appréciable.

**L'application ne comporte pas de notion de « magasin »** — seulement des rayons
et des médicaments.

L'interprétation retenue est celle du **médicament** : dans le code d'origine,
l'historique est bien affiché dans la fiche détail d'un médicament, sous les
boutons de stock, ce qui correspond à « peu esthétique et en bas de la liste ».

!!! note "Une seconde lecture reste ouverte"

    « Magasin » pourrait désigner le stock dans son ensemble, c'est-à-dire un
    **journal global** plutôt que par médicament. 
    Le modèle de données le permet déjà — `history` est une collection racine
    avec un champ `medicineId`, interrogeable dans les deux sens. Il ne manque
    qu'un écran. À arbitrer avec le Product Owner.
