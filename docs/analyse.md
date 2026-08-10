# Analyse de l'existant

Les défauts du code livré, **regroupés par cause plutôt que par symptôme**.

C'est le parti pris de cette page, et il n'est pas cosmétique : deux services
différents signalaient des problèmes qui n'avaient qu'une seule origine. Traiter
les symptômes aurait produit deux correctifs là où il en fallait un — et laissé
la cause en place.

---

## Une ligne de code, deux plaintes

`ui/medicine/MedicineDetailActivity.kt`, lignes 90 et 114 :

```kotlin
medicines[medicines.size].histories.toMutableList().add(
    History(
        medicine.name,
        "efeza56f1e65f",
        Date().toString(),
        "Updated medicine details"
    )
)
```

Cette expression concentre **trois défauts distincts**.

### 1. L'index est hors bornes

`medicines[medicines.size]` : l'index valide maximum d'une liste est `size - 1`.
L'accès lève une `IndexOutOfBoundsException` à *chaque* appui, quelle que soit la
taille de la liste.

C'est le « les boutons +1/-1 ferment involontairement l'application » du Product
Owner.

### 2. La correction évidente est un piège

Corriger en `medicines[medicines.size - 1]` supprime le plantage. C'est
précisément ce qu'il ne fallait pas faire.

Cet index désigne le **dernier médicament de la liste**, pas celui affiché à
l'écran. On consulte « Doliprane », on incrémente, et l'historique part sur un
autre médicament.

!!! danger "Un plantage bruyant vaut mieux qu'une corruption silencieuse"

    Le plantage se voit et se signale. Une écriture sur le mauvais médicament ne
    se voit pas — et sur un stock pharmaceutique, elle se découvre à
    l'inventaire, des semaines plus tard.

La bonne référence, `medicine`, était pourtant disponible quelques lignes plus
haut.

### 3. L'écriture est un no-op

`toMutableList()` retourne une **copie** de la liste. L'entrée d'historique y est
ajoutée, puis la copie est abandonnée : elle n'est jamais réaffectée au
médicament, jamais publiée dans le `StateFlow`.

Aucune trace n'était donc jamais conservée. **C'est la cause du problème remonté
par le service qualité** — « certaines actions apparaissent de manière sporadique
ou sont totalement manquantes ».

### Ce que ça révèle

Le Product Owner décrivait un plantage. Le service qualité décrivait un
historique lacunaire. Les deux regardaient la même expression.

Un quatrième défaut se cachait derrière : même sans plantage, le stock n'aurait
pas été conservé. Il vivait dans un `remember` local au composable —

```kotlin
var stock by remember { mutableStateOf(medicine.stock) }
```

— donc perdu à la sortie de l'écran. C'est le « les données disparaissent » du
Product Owner. **Aucune correction sur place ne pouvait fonctionner** : il fallait
faire remonter l'opération jusqu'à une source de vérité.

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

Quatre conséquences :

- **Une boucle infinie** cadencée à 200 ms, active tant que le processus vit.
- À chaque tour, `myBroadcastReceiver` est réaffecté : la référence vers
  l'instance précédente est perdue, mais celle-ci **reste enregistrée auprès du
  système**. Aucun `unregisterReceiver` n'existait, ni aucun `onDestroy`. Le
  nombre de receivers vivants croissait linéairement, et le nombre de `Toast`
  déclenchés de façon quadratique.
- Chaque receiver retenait l'`Activity` via la référence statique (voir plus bas)
  et empêchait sa libération.
- `Handler()` sans `Looper` explicite est déprécié depuis l'API 30.

**L'impact green code est direct** : le thread principal était réveillé cinq fois
par seconde, sans aucune contrepartie fonctionnelle. Le broadcast n'affichait
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
rotation ou recréation de configuration, l'instance précédente restait retenue
**avec toute sa hiérarchie de vues**.

Ce champ n'était pas une négligence isolée. Il servait de contournement pour
partager le `MedicineViewModel` entre plusieurs `Activity` :

```kotlin
val viewModel = ViewModelProvider(MainActivity.mainActivity)[MedicineViewModel::class.java]
```

— dans `MedicineDetailActivity` et dans `AisleDetailActivity`.

!!! info "La fuite était une conséquence de l'architecture"

    Les écrans de détail étaient des `Activity` distinctes alors que le reste de
    l'application utilisait déjà Navigation Compose. Deux `Activity` ne peuvent
    pas partager un `ViewModel` sans passer par quelque chose de global.

    Supprimer la ligne n'aurait rien réglé : il fallait supprimer le besoin.
    C'est ce qu'a fait le passage des détails en destinations Compose.

---

## Le filtre détruisait les données

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
    _medicines.value = filteredMedicines   // ← la source de vérité est écrasée
}
```

Les médicaments non correspondants n'étaient pas **masqués** : ils étaient
**supprimés**. Effacer le champ de recherche ne les restaurait pas, puisque le
filtre s'appliquait alors à une liste déjà amputée.

Même remarque pour `sortByName` et `sortByStock`, qui réordonnaient la source de
vérité.

**La cause de fond est conceptuelle** : le tri et le filtrage sont des
préoccupations de *présentation*. Ce sont des paramètres de requête, pas des
mutations du modèle. Tant qu'ils modifiaient la source, aucune correction locale
n'était stable.

---

## Les autres défauts relevés

Moins spectaculaires, mais relevés à la lecture.

### Encapsulation et état

| Défaut | Conséquence |
|---|---|
| `var _medicines` déclaré **public** | L'état interne du `ViewModel` est modifiable de l'extérieur |
| `MutableStateFlow<MutableList<…>>` | Une collection mutable dans un flux d'état immuable |
| `aisles[Random().nextInt(aisles.size)]` | Exception quand aucun rayon n'existe |

### Modèles

| Défaut | Conséquence |
|---|---|
| `Medicine` sans identifiant | Retrouvé par son nom (`find { it.name == name }`), transporté par `putExtra`. Deux homonymes indiscernables |
| `Aisle` et `History` en classes ordinaires avec `var` | Pas d'égalité structurelle, donc pas de clés stables pour les listes |
| `History.date` en `String` | Ne se trie pas, dépend de la locale de celui qui l'a écrite |
| `History.userId` codé en dur : `"efeza56f1e65f"` | L'identifiant illisible signalé par le Product Owner |

### Navigation et recomposition

| Défaut | Conséquence |
|---|---|
| `navigate()` sans `popUpTo` ni `launchSingleTop` | La back-stack s'empile indéfiniment à chaque bascule d'onglet |
| `currentRoute()` recollecte la back-stack | Recompositions inutiles, alors que la valeur était déjà disponible |
| `EmbeddedSearchBar` avec un `rememberSaveable` interne **en plus** de l'état hissé | Deux sources de vérité pour la même saisie |
| `items(medicines)` sans paramètre `key` | Recompositions inutiles à chaque changement de liste |
| `?: return` au milieu d'un composable | Écran blanc silencieux si le médicament est introuvable |

### Interface et accessibilité

| Défaut | Conséquence |
|---|---|
| `contentDescription = null` sur les icônes actionnables | Inutilisables sous TalkBack |
| `contentDescription = "Arrow"` | Libellé sans valeur pour un lecteur d'écran |
| `Color.Gray` codé en dur | Illisible en mode sombre, contraste non conforme |
| Thème parent `Theme.Material.Light` figé | Le mode sombre ne peut pas fonctionner |
| Tous les libellés dans le code | `strings.xml` ne contenait que le nom de l'application |
| `MedicineItem` défini **deux fois** | Deux signatures et deux styles incompatibles |

### Chaîne de build et outillage

| Défaut | Conséquence |
|---|---|
| Java 8, Kotlin 1.9 | Chaîne obsolète |
| Aucun `signingConfig` en release | Bloquant pour la distribution d'un APK |
| Receiver déclaré dans le manifeste **et** enregistré dynamiquement | Doublon, et une classe imbriquée dans une `Activity` exposée au système |
| Aucun test, aucune intégration continue | Aucun filet |

---

## Une ambiguïté dans les demandes

Une note du Product Owner mentionne :

> Intégrer l'historique dans le contenu scrollable de la fiche détail d'un
> magasin serait appréciable.

**L'application ne comporte pas de notion de « magasin »** — seulement des rayons
et des médicaments.

L'interprétation retenue est celle du **médicament** : dans le code d'origine,
l'historique était bien affiché dans la fiche détail d'un médicament, sous les
boutons de stock, ce qui correspond à « peu esthétique et en bas de la liste ».
La description matérielle du défaut ne correspond à aucun autre écran.

!!! note "Une seconde lecture reste ouverte"

    « Magasin » pourrait désigner le stock dans son ensemble, c'est-à-dire un
    **journal global** plutôt que par médicament. C'est d'ailleurs ce dont le
    service qualité a besoin : « qui a touché au stock cette semaine ».

    Le modèle de données le permet déjà — `history` est une collection racine
    avec un champ `medicineId`, interrogeable dans les deux sens. Il ne manque
    qu'un écran. À arbitrer avec le Product Owner.
