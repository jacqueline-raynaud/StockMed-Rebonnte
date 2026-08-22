# Analyse de l'existant

Les défauts du code original, **regroupés par cause**.

---

## Historique des mouvements de stocks

`ui/medicine/MedicineDetailActivity.kt` :

```
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

### 1\. L'index est hors bornes

`medicines[medicines.size]` : l'index maximum d'une liste est `size - 1`.  
L'accès lève une `IndexOutOfBoundsException` à _chaque_ appui, quelle que soit la  
taille de la liste.

Correspond à : « les boutons +1/-1 ferment involontairement l'application » du Product  
Owner.

### 2\. le piège de cet index

Corriger en `medicines[medicines.size - 1]` ou `medicine.last()` supprime le plantage.

Pour autant, cet index désigne le **dernier médicament de la liste**, pas celui affiché à  
l'écran. On consulte « Doliprane », on incrémente le stock, et l'historique part sur un  
autre médicament.

### 3\. L'écriture est une opération nulle

`toMutableList()` retourne une **copie** de la liste histories. l'appel à `.add()`modifie cette  
copie et... plus rien.  
Sans doute **la cause du problème remonté par le service qualité** -> « certaines actions apparaissent de manière sporadique ou sont totalement manquantes »

Impossible de vérifier le problème, l'historique n'apparait pas du tout sur le build

### 4\. le stock disparait

Le stock est dans une variable locale au composable.  
Il est perdu dès que l'on quitte la composition (rotation, navigation)

```
var stock by remember { mutableStateOf(medicine.stock) }
```

Il devrait remonter dans un viewModel.

---

## Fuite mémoire n°1   La boucle

`MainActivity.kt`.

```
onCreate()
  > startBroadcastReceiver()
    > registerReceiver(receiver #1)
    > Handler().postDelayed(200 ms) > startMyBroadcast()
                                        > sendBroadcast()
                         boucle -->     > startBroadcastReceiver()
                                            > registerReceiver(#2)
                                            > postDelayed(200 ms) ─> …
```

Conséquences :

*   **Une boucle infinie** toutes les 0.2 seconde, active tant que le processus vit.
*   À chaque tour, `myBroadcastReceiver` est réaffecté
*   Aucun `unregisterReceiver` ou `onDestroy`n'existe. L nombre de receivers vivants croit ainsi que le nombre de `Toast` déclenchés.
*   Chaque receiver retient l'`Activity` via la référence statique (voir plus bas)  
    et empêchait sa libération.

**Impact green code** : le thread principal est réveillé cinq fois par seconde, sans aucune fonctionnalité. Le broadcast n'affiche qu'un `Toast` « Update reçu ».

C'est la fuite la plus visible sur l'Android Profiler, et celle qui a été retenue  
pour la démonstration avant/après.

---

## Fuite mémoire n°2   la référence statique

```
companion object {
    lateinit var mainActivity: MainActivity
}
```

Une référence statique vers une `Activity` survit à sa destruction. À chaque  
rotation ou recréation de configuration, l'instance précédente est retenue  
**avec toute sa hiérarchie de vues**.

Le companion permet de partager le `MedicineViewModel` entre plusieurs `Activity` :

```
val viewModel = ViewModelProvider(MainActivity.mainActivity)[MedicineViewModel::class.java]
```

  dans `MedicineDetailActivity` et dans `AisleDetailActivity`.

!!! info "note d'architecture"

```
Les écrans de détail sont des `Activity`il faut les transformer en destination .
```

---

## Le filtre des médicaments détruit les données

Ce problème n'est pas documenté mais apparaît lors du test de l'application fournie.  
Le champ de recherche, affiche la liste correspondante mais l'effacement du filtre ne rétablit pas  
la liste d'origine.

`ui/medicine/MedicineViewModel.kt` :

```
fun filterByName(name: String) {
    val currentMedicines: List<Medicine> = medicines.value
    val filteredMedicines: MutableList<Medicine> = ArrayList()
    for (medicine in currentMedicines) {
        if (medicine.name.lowercase().contains(name.lowercase())) {
            filteredMedicines.add(medicine)
        }
    }
    _medicines.value = filteredMedicines   // la source est écrasée
}
```

Les médicaments non correspondants à la recherche sont **supprimés**.  
Même remarque pour `sortByName` et `sortByStock`, qui réordonne la source d'origine

Une demande est active pour utiliser Firestore pour réaliser le tri et le filtre. Cette partie sera  
traitée à ce moment-là.

---

## Autres défauts relevés

Moins spectaculaires, mais relevés à la lecture.

### Modèles

| Défaut | Conséquence |
| --- | --- |
| `History.date` en `String` | Ne se trie pas, dépend de la locale de celui qui l'a écrite |
| `History.userId` codé en dur : `"efeza56f1e65f"` | L'identifiant illisible signalé par le Product Owner |

---

## Une ambiguïté dans les demandes

Une note du Product Owner mentionne :

> Intégrer l'historique dans le contenu scrollable de la fiche détail d'un  
> magasin serait appréciable.

**L'application ne comporte pas de notion de « magasin »**   seulement des rayons  
et des médicaments.

L'interprétation retenue est celle du **médicament** : dans le code d'origine,  
l'historique est bien affiché dans la fiche détail d'un médicament, sous les  
boutons de stock, ce qui correspond à « peu esthétique et en bas de la liste ».

!!! note "Une seconde lecture reste ouverte"

```
« Magasin » pourrait désigner le stock dans son ensemble, c'est-à-dire un
**journal global** plutôt que par médicament. 
À arbitrer avec le Product Owner.
```