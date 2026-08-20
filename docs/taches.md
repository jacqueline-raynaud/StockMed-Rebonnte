# Tâches réalisées

Pour chaque tâche : le **problème sous-jacent**, ce qui a été fait, et le détail
d'implémentation quand il éclaire le choix.

!!! tip "Cette page est la source du livrable PDF"

    Elle s'imprime depuis le navigateur (++ctrl+p++) avec la mise en page de
    Material.

!!! warning "Les durées sont des estimations"

    La colonne « Est. » donne la charge estimée au moment du cadrage, en
    jours-homme. À remplacer par les durées réelles avant remise.

---

## Vue d'ensemble

| Lot | Objet | État |
|---|---|---|
| 0 | Dépôt et chaîne de build | Terminé |
| 1 | Crashs et fuites mémoire | Terminé |
| 2 | Architecture | Terminé |
| 3 | Persistance et fonctionnalités | Terminé |
| 4 | Qualité, CI et livrables | Terminé |
| 5 | Reprise après revue technique | Terminé |
| 6 | Demandes complémentaires | Terminé |
| 7 | Accessibilité et confort | À faire |

**Réalisé : 42 tâches**, pour 30,25 jours estimés au cadrage. Le [reste à faire](#reste-a-faire) est détaillé en fin
de page — un backlog honnête vaut mieux qu'une liste toute verte.

---

## Lot 0 — Dépôt et chaîne de build

### T-01 · Initialiser l'historique git · 0,25 j

**Problème.** Le dépôt livré ne contenait aucun commit. Sans base de référence,
impossible de distinguer le code d'origine des corrections apportées.

**Fait.** Commit initial de l'état livré avant toute modification, puis une
branche par lot de travail, chacune passant par une pull request.

### T-02 · Moderniser la chaîne de build · 0,5 j

**Problème.** Java 8, Kotlin 1.9, aucune injection de dépendances, aucun outil
de mesure.

**Fait.** Java 17, Kotlin 2.1, plugin Compose Compiler, KSP, Hilt, SonarQube,
LeakCanary en `debugImplementation`.

**Détail.** Deux versions sont volontairement figées — le BOM Firebase et
`commons-compress`. Les raisons sont dans
[Décisions d'architecture](decisions.md#contraintes-de-la-chaine-de-build).

---

## Lot 1 — Crashs et fuites mémoire

### T-04 · Le stock fermait l'application · 0,5 j

**Problème.** `medicines[medicines.size]` : l'index valide maximum étant
`size - 1`, chaque appui sur +1 ou -1 levait une `IndexOutOfBoundsException`.
C'est le « les boutons ferment involontairement l'application » du Product Owner.

**Fait.** L'opération est descendue dans le repository :
`updateStock(id, delta, userEmail)` cible le médicament désigné et borne le stock
à zéro.

**Détail.** Corriger en `size - 1` aurait supprimé le plantage mais visé **le
dernier médicament de la liste** au lieu de celui affiché : un plantage bruyant
serait devenu une corruption silencieuse. Sur un stock pharmaceutique, c'est pire.

### T-05 · L'historique n'était jamais enregistré · 0,5 j

**Problème.** `medicine.histories.toMutableList().add(...)` ajoutait l'entrée à
une **copie** aussitôt jetée. Aucune trace n'était conservée.

**Fait.** L'écriture est réelle, et dans la même opération que la modification.

**Détail.** C'est la **même ligne de code** que T-04. Le Product Owner signalait
un plantage, le service qualité un historique lacunaire : deux services
décrivaient le même bug.

### T-06 · Fuite mémoire du BroadcastReceiver · 0,5 j {#t-06}

**Problème.** `startBroadcastReceiver()` enregistrait un receiver puis
programmait à 200 ms `startMyBroadcast()`, qui rappelait
`startBroadcastReceiver()`. À chaque tour, un nouveau receiver était enregistré
et la référence du précédent perdue, sans jamais aucun `unregisterReceiver`.

Le nombre de receivers vivants croissait indéfiniment, chacun retenant
l'`Activity`. Le thread principal était réveillé cinq fois par seconde pour
afficher un `Toast` sans fonction métier.

**Fait.** Enregistrement unique dans `onCreate`, désenregistrement symétrique
dans `onDestroy`. `Handler()` remplacé par `lifecycleScope`, annulé avec
l'`Activity`. `ContextCompat.registerReceiver` applique le flag d'export sur
toutes les versions d'Android.

**Détail.** Deux défauts de sécurité sont apparus derrière le premier : le flag
d'export manquant sur la branche pré-Tiramisu faisait échouer lint, et le
broadcast implicite était visible des autres applications du téléphone — corrigé
par `setPackage`.

### T-07 · Référence statique vers l'Activity · 0,25 j

**Problème.** `companion object { lateinit var mainActivity: MainActivity }`.
Une référence statique vers une `Activity` survit à sa destruction : à chaque
rotation, l'instance précédente restait retenue avec sa hiérarchie de vues.

**Fait.** Supprimée, avec ses deux usages. Résolu mécaniquement par T-14.

**Détail.** Ce champ n'existait que pour partager un `ViewModel` entre `Activity`.
La fuite était une conséquence du découpage, pas une négligence isolée — d'où une
correction structurelle plutôt qu'un rustine.

### T-08 · Le filtre détruisait les données · 0,5 j

**Problème.** `filterByName` écrasait `_medicines.value` avec la liste filtrée.
Les médicaments non correspondants n'étaient pas masqués : ils étaient supprimés,
et effacer la recherche ne les restaurait pas.

**Fait.** La recherche et le tri sont des états de présentation du `ViewModel`,
passés en paramètres à `observeMedicines(query, sort)`. La source n'est jamais
amputée.

### T-09 · Ajout impossible sans rayon · 0,25 j

**Problème.** `aisles[Random().nextInt(aisles.size)]` levait une exception quand
aucun rayon n'existait.

**Fait.** Le cas est traité explicitement.

---

## Lot 2 — Architecture

### T-10 · Couche data et repositories · 1,5 j

**Problème.** Les `ViewModel` contenaient les données, la logique de persistance
et l'état de présentation. Rien n'était testable sans émulateur.

**Fait.** Modèles immuables avec identifiant stable, interfaces
`MedicineRepository` / `AisleRepository` / `UserRepository`, et une implémentation
en mémoire pour chacune.

**Détail.** Voir [Décisions](decisions.md#des-interfaces-de-repository) — le
passage ultérieur à Firestore n'a demandé que trois lignes.

### T-12 · Injection de dépendances · 0,5 j

**Fait.** `@HiltAndroidApp`, un module pour les services Firebase, un pour les
liaisons de repositories.

**Détail.** Le `@Singleton` sur les liaisons est indispensable : sans lui, chaque
`ViewModel` recevrait sa propre instance de repository et l'écran des rayons ne
verrait pas les médicaments ajoutés depuis l'autre onglet.

### T-13 · ViewModels propres · 1 j

**Problème.** État public et mutable (`var _medicines`), collections mutables
dans un `StateFlow`, traitements sur le thread principal.

**Fait.** État privé exposé en `StateFlow` immuable, opérations dans
`viewModelScope`, dépendances injectées par constructeur.

### T-14 · Navigation unifiée en Compose · 1 j

**Problème.** Les écrans de détail étaient des `Activity` reliées par des
`Intent`, avec des extras textuels. La bascule d'onglets empilait une destination
à chaque appui : au bout de dix allers-retours, il fallait dix retours arrière
pour quitter.

**Fait.** Quatre destinations dans un `NavHost`, `popUpTo` et `launchSingleTop`
sur les onglets, flèche de retour sur les détails.

**Détail.** Deux défauts sont apparus à l'usage et ont été corrigés :
l'écran noir après déconnexion, et le blocage au démarrage avec session ouverte.
Les deux sont expliqués dans
[Décisions](decisions.md#le-retour-ferme-lapplication-quand-la-pile-est-vide).

### T-33 · Composant de liste unique · 0,5 j

**Problème.** `MedicineItem` existait en deux versions incompatibles, avec des
signatures et des styles différents.

**Fait.** Un seul composant dans `ui/component`, avec les couleurs du thème au
lieu d'un `Color.Gray` en dur illisible en mode sombre.

---

## Lot 3 — Persistance et fonctionnalités

### T-11 · Firestore · 1 j

**Problème.** Toutes les données vivaient en mémoire : elles disparaissaient à
la fermeture de l'application.

**Fait.** `MedicineRepositoryImpl` et `AisleRepositoryImpl` derrière
les interfaces existantes. Collections `medicines`, `aisles` et `history`.

**Détail.** Les mouvements de stock passent par une transaction, pas deux
écritures successives — pour l'atomicité et pour la concurrence entre opérateurs.
Voir [Décisions](decisions.md#le-mouvement-et-sa-trace-dans-une-transaction).

### T-17 · Authentification · 1 j

**Problème.** Aucune authentification, alors que le cahier des charges demandait
création de compte et identification.

**Fait.** Écrans Compose de connexion et de création de compte, `UserRepository`
sur Firebase Authentication, validation avant tout appel réseau, messages
d'erreur traduits.

**Détail.** FirebaseUI a été écarté :
[pourquoi](decisions.md#des-ecrans-ecrits-a-la-main-plutot-que-firebaseui).

### T-18 · Déconnexion · 0,25 j

**Problème.** Impossible de se déconnecter — problématique sur des téléphones
partagés entre opérateurs.

**Fait.** Depuis la barre supérieure de toutes les listes, et depuis l'écran
d'accueil. Un écran d'accueil nomme la session ouverte et avertit avant l'accès
au stock.

### T-19 · Historique enrichi · 0,75 j

**Problème.** L'historique affichait un identifiant technique (`efeza56f1e65f`,
codé en dur), une date en chaîne de caractères, et aucun détail chiffré.

**Fait.** `HistoryDto` porte l'e-mail de l'opérateur, un horodatage `Long`, une
action typée, et les valeurs de stock avant et après. L'affichage est intégré au
contenu défilant de la fiche détail.

**Détail.** Une date en chaîne ne se trie pas et dépend de la locale de celui qui
l'a écrite.

### T-20 · Journalisation systématique · 0,5 j

**Problème.** La cause racine des incohérences signalées par le service qualité :
chaque appelant devait penser à écrire l'historique.

**Fait.** `addMedicine`, `updateStock` et `deleteMedicine` écrivent leur trace
elles-mêmes. **L'oubli devient impossible par construction**, y compris pour du
code qui n'existe pas encore.

### T-22 · Tri et filtre côté serveur · 0,75 j {#t-22}

**Problème.** Tri et filtre effectués en mémoire sur la liste complète — point
soulevé par l'audit green code.

**Fait.** `orderBy` côté Firestore pour le tri, recherche par intervalle sur un
champ en minuscules pour le filtre. Le tri offre les deux sens sur le nom et
sur le stock, et le menu coche le critère actif.

Le tri par nom porte sur le champ en minuscules : un tri lexicographique brut
placerait « Zovirax » avant « aspirine », les majuscules précédant les
minuscules. Ce n'est pas l'ordre alphabétique attendu par un opérateur.

**Détail.** Régression assumée : la recherche devient un « commence par » et non
un « contient ». Firestore ne sait pas faire de recherche textuelle.
[Explication](decisions.md#la-recherche-est-un-commence-par-pas-un-contient).

### T-43 · Règles de sécurité Firestore · 0,25 j

**Problème.** Une base en mode test est ouverte à tout Internet, et les règles du
mode test expirent au bout de 30 jours — l'application tomberait en panne sans
changement de code.

**Fait.** `firestore.rules` versionné : lecture et écriture réservées aux
utilisateurs authentifiés, suppression de rayon interdite, et **journal d'audit
en ajout seul**.

**Détail.** `update` et `delete` sont refusés à tout le monde sur `history`, et
`userEmail` doit correspondre au compte appelant. C'est ce qui distingue un
journal d'audit d'un simple log.

### T-46 · Emplacements de stockage · 0,5 j

**Problème.** Les rayons étaient créés au hasard : « Aisle 2 », « Aisle 3 ». Or
un médicament se range selon des règles précises — stockage standard, froid, ou
sécurisé pour les stupéfiants et les produits coûteux.

**Fait.** Les trois emplacements réels sont amorcés au premier lancement.
`addAisle(nom)` permet d'en ajouter d'autres, par un dialogue qui remplace la
numérotation automatique.

**Détail.** Le modèle n'a pas changé : `AisleDto` était déjà la bonne abstraction —
un emplacement de stockage. Seules les données étaient fausses. Ce sont des
**données et non une énumération** : un établissement peut avoir besoin d'un
froid négatif ou d'une quarantaine sans qu'on recompile.

!!! danger "Un bug que le double de test masquait"

    `AisleRepositoryImpl` ne créait **aucun** emplacement. L'implémentation
    en mémoire, elle, en semait un — ce qui a masqué le défaut pendant tous les
    tests.

    Sur une base Firestore neuve, la collection était donc vide, et la création
    d'un médicament s'interrompait silencieusement faute d'endroit où le ranger.

    C'est la limite des doubles de test : quand ils se comportent **mieux** que
    l'implémentation réelle, ils cachent ce qu'ils devraient révéler. Le défaut a
    été trouvé par une question métier, pas par la suite de tests.

### T-15 · Création de médicament · 0,75 j

**Problème.** Le bouton « + » ajoutait un médicament au nom et au stock
aléatoires, dans un rayon tiré au hasard. Demande explicite du Product Owner :
ouvrir un écran à remplir.

**Fait.** Un formulaire avec le nom, une **liste déroulante des emplacements** et
la quantité initiale. Validation avant écriture, champ quantité restreint aux
chiffres, nom débarrassé de ses espaces de frappe.

**Détail.** La liste déroulante est alimentée par `observeAisles()` : on choisit
parmi ce qui existe, on ne saisit pas un emplacement librement. C'est ce qui
garantit qu'un médicament est toujours rangé quelque part de valide.

### T-16 · Suppression d'un médicament · 0,5 j

**Problème.** `deleteMedicine` existait dans le repository et le `ViewModel`,
mais **aucune interface ne l'appelait**. La suppression restait impossible.

**Fait.** Depuis la fiche détail, avec une confirmation qui précise que
l'historique reste consultable et rappelle en rouge le stock restant.

**Détail.** L'historique survit bien à la suppression : il vit dans une
collection racine, pas dans le document du médicament. Le rappel du stock
restant répond à une [question ouverte](#questions-ouvertes-pour-le-product-owner)
sans la trancher.

### T-44 · Saisie d'une quantité · 0,75 j

**Problème.** Retirer cinquante boîtes demandait cinquante appuis — et produisait
**cinquante lignes d'historique**. Le service qualité cherchant « qui a retiré
50 boîtes » aurait trouvé cinquante entrées de « -1 ».

**Fait.** Un champ de quantité et deux boutons, Retirer et Ajouter. Un mouvement,
une opération, une entrée d'historique. Un message de confirmation s'affiche, et
le champ se vide.

**Détail.** Le champ est **vide** au départ et après chaque mouvement, ce qui
désactive les deux boutons. Repartir de « 1 » serait plus rapide, mais laisserait
les boutons actifs en permanence : sur un téléphone partagé, un doigt qui traîne
suffirait à produire un mouvement intempestif — **indiscernable d'un mouvement
légitime dans le journal d'audit**.

Ce défaut ne figurait dans aucune note : il est apparu **après** la correction de
T-05. Tant que l'historique n'était jamais écrit, personne ne pouvait constater
qu'il serait illisible. Corriger un défaut a rendu le suivant visible.

---

## Lot 4 — Qualité, CI et livrables

### T-25 · Tests unitaires · 1,5 j

**Problème.** Aucun test sur une application critique pour l'entreprise.

**Fait.** 51 tests sur les repositories et les cinq `ViewModel` à la fin de ce
lot — **72 aujourd'hui**, le lot 5 en ayant ajouté vingt et un.

**Détail.** Chaque test de régression correspond à un défaut réellement
rencontré : le stock qui vise le bon médicament, l'historique effectivement
écrit, la recherche qui n'ampute pas la source. Chacun échouerait sur le code
d'origine.

Couverture mesurée par JaCoCo, remontée à SonarCloud.

### T-26 · Tests d'interface · 1 j

**Fait.** 9 tests de parcours : accès verrouillé sans session, écran d'accueil
au démarrage avec session, déconnexion, comportement du bouton retour.

**Détail.** Deux d'entre eux verrouillent des bugs trouvés à la main pendant les
tests manuels — l'écran noir après déconnexion et le blocage de l'écran
d'accueil. Un faux `UserRepository` installe une session avant le lancement de
l'`Activity`, ce qui reproduit le démarrage session ouverte sans avoir à tuer le
processus.

### T-27 · Intégration continue · 1,5 j

**Fait.** Deux jobs GitHub Actions sur chaque push et chaque pull request :
compilation, tests unitaires, lint, couverture et analyse SonarCloud d'un côté ;
tests instrumentés sur émulateur de l'autre.

**Détail.** Trois obstacles ont été rencontrés et sont documentés dans
[Qualité](qualite.md) : un conflit de classpath entre AGP et le plugin Sonar, une
incompatibilité d'Espresso avec les versions récentes d'Android, et l'instabilité
des retours arrière Espresso sur un runner.

### T-29 · Mesures Android Profiler · 0,5 j

**Fait.** Captures avant et après sur les deux fuites mémoire, avec la même
séquence d'utilisation des deux côtés.

**Détail.** La métrique retenue est le nombre d'instances retenues de
`MainActivity` et de `MyBroadcastReceiver` dans le heap dump — un comptage exact
plutôt qu'une pente lue à l'œil sur un graphe.

### T-28 · Distribution de l'APK · 0,75 j

**Problème.** Le Product Owner ne pouvait pas tester l'application sans dépendre
du développeur pour lui transmettre un APK — « c'est vraiment une galère de
télécharger l'application à chaque fois que tu fais des changements ».

**Fait.** Un troisième workflow, déclenché sur un tag `v*` ou manuellement :
restauration du keystore depuis un secret, construction d'un APK de release
signé, envoi sur Firebase App Distribution au groupe `testers`.

**Détail.** La configuration de signature lit `local.properties`, jamais
versionné, et n'est déclarée **que si le keystore est présent** — sans quoi la
seule configuration de Gradle échouerait sur un poste sans clé, y compris pour un
`assembleDebug`.

Le `versionCode` est dérivé du numéro de run. Sans cela, App Distribution
présente chaque nouvel APK comme identique au précédent et les testeurs ne voient
pas la mise à jour.

Le keystore et `local.properties` sont effacés en fin de job, avec `if: always()`
pour que ça s'exécute même après un échec.

!!! note "Un secret jamais utilisé est un secret jamais validé"

    Le workflow dont celui-ci est adapté déclarait un secret `KEY_PASSWORD` mais
    écrivait en réalité le mot de passe du magasin dans les deux champs. Le
    secret existait, semblait correct, et n'avait jamais servi.

    Corriger cette incohérence a fait échouer le premier build sur
    `Given final block not properly padded` — le déchiffrement de la clé. La
    valeur était fausse depuis toujours ; rien ne pouvait le révéler tant qu'elle
    n'était pas lue.

    Deux échecs successifs ont été nécessaires — d'abord l'alias, puis le mot de
    passe de clé — tous deux dus à des valeurs de secrets approximatives. GitHub
    n'affiche jamais le contenu d'un secret : une espace finale y reste invisible
    jusqu'à ce qu'elle casse quelque chose, et aucun message d'erreur ne la
    mentionne.

### T-30 · Documentation · 0,5 j

**Fait.** Ce site, publié sur GitHub Pages par une action dédiée.

---

## Lot 5 — Reprise après revue technique

Ce lot regroupe la mise en conformité demandée lors d'une revue de code
externe, plus trois défauts découverts en la menant.

### T-34 · Externalisation des chaînes · 0,75 j

**Problème.** Tous les libellés étaient écrits en dur dans les composables.
`strings.xml` ne contenait qu'une entrée, `app_name`.

**Fait.** 89 chaînes extraites, `values/` en anglais et `values-fr/` en
français.

**Détail.** Les messages d'erreur vivaient dans les ViewModels, qui n'ont pas
de `Context` pour résoudre une ressource. Plutôt que d'y injecter un `Context`
— chemin classique vers la fuite mémoire, et fin de la testabilité hors
émulateur — l'état porte un **identifiant de ressource** :

```kotlin
@StringRes val emailError: Int? = null
```

L'écran résout le libellé. Effet de bord : trois tests qui comparaient des
phrases françaises comparent maintenant des identifiants, et ne peuvent plus
casser à cause d'une traduction.

!!! warning "Les tests d'interface auraient cassé en CI, pas en local"

    Les 24 assertions de `NavigationFlowTest` comparaient des chaînes
    françaises en dur. Elles passaient tant que `values/` était français ;
    elles auraient toutes échoué le jour du basculement en anglais, sur
    l'émulateur `en-US` de l'intégration continue.

    D'où l'ordre d'exécution : extraire à valeurs constantes, convertir les
    tests, **puis** traduire. Vérifié dans les deux langues, mêmes tests sans
    retouche.

**Contrainte découverte.** Le titre de la barre supérieure et le libellé de
l'onglet sont affichés en même temps. Un texte identique rendrait les deux
nœuds indiscernables pour les tests comme pour TalkBack — d'où « Stock des
médicaments » et « Médicaments ».

### T-47 · Structure de la couche data · 1 j

**Demande.** Nommer les modèles `Dto` pour marquer la frontière externe,
séparer les interfaces de leurs implémentations, suffixer celles-ci en `Impl`.

**Fait.**

```
data/model/          MedicineDto, AisleDto, HistoryDto, UserDto, HistoryAction
data/repository/     les interfaces
       └── impl/     MedicineRepositoryImpl, AisleRepositoryImpl, UserRepositoryImpl
                     FakeMedicineRepository, FakeAisleRepository
```

Le préfixe `Firestore` a disparu : `MedicineRepositoryImpl` ne nomme plus sa
technologie, ce qui est précisément l'intérêt d'une interface.

**Détail — l'obfuscation.** La demande initiale associait `java.io.Serializable`
à une protection contre l'obfuscation. Ce n'en est pas une : c'est un marqueur
de sérialisation Java, que R8 ignore. Le risque est réel mais ailleurs —
**Firestore remplit les modèles par réflexion sur le nom des champs** :

```kotlin
toObject(MedicineDto::class.java)
```

`isMinifyEnabled = true` renommerait `stockAfter` en `a`, la clé du document
resterait `stockAfter`, et le champ garderait sa valeur par défaut. **Sans
aucune erreur** : une liste de médicaments sans nom et à zéro.

D'où `@Keep` sur les trois classes lues par réflexion et sur l'énumération
qu'elles contiennent. Pas sur `UserDto`, construit à la main depuis
`FirebaseUser` : R8 renomme alors le champ et son appel de façon cohérente.

### T-48 · Modèles d'affichage · 1 j

**Demande.** Ne pas exposer d'objets de la base à l'affichage.

**Fait.** Quatre modèles `Ui` immuables, avec le mapping dans les ViewModels —
et non dans les dépôts, qui devraient alors connaître l'affichage.

**Détail.** Ils ne sont pas des copies : chacun retire ou ajoute quelque chose.

| Modèle | Ce qui change |
|---|---|
| `MedicineUi` | Porte `locationName` déjà résolu ; l'écran ne croise plus deux listes |
| `HistoryUi` | Porte `dateLabel` déjà formaté ; le `DateFormat` ne s'exécute plus à chaque recomposition |
| `UserUi` | Ne porte **pas** l'UID Firebase : aucun écran ne l'affiche |

Conséquence directe : `MedicineDetailScreen` n'a plus besoin de
l'`AisleViewModel`.

Les libellés de remplacement restent nullables plutôt que résolus dans le
ViewModel — sinon celui-ci devrait connaître la langue du téléphone, et le
bénéfice de T-34 serait perdu.

### T-49 · UiState, composables sans état et previews · 2 j

**Demande.** Un `UiState` par ViewModel, séparer les composables avec et sans
état, ajouter des previews et des `contentType`.

**Fait.** `MainUiState`, `MedicineUiState`, `AisleUiState` ; chaque écran se
découpe en `XScreen` (connaît le ViewModel) et `XContent` (données et lambdas) ;
neuf previews ; `contentType` sur les trois listes.

Chaque état **public** a son fichier, à côté de son ViewModel : `MainUiState.kt`,
`AuthUiState.kt`, `AisleUiState.kt`, `MedicineUiState.kt`,
`MedicineListUiState.kt`, `MedicineDetailUiState.kt`, `MedicineFormUiState.kt`.
Le `MedicineViewModel` en portait trois : la déclaration prenait le haut du
fichier avant la première ligne de logique.

Les états **internes** restent dans le ViewModel, et privés : `AccountDeletion`
n'existe que pour être fusionné dans `MainUiState`, et l'exposer laisserait
croire qu'un écran peut l'observer seul.

**Détail.** Trois flux séparés peuvent être observés dans des états qui se
contredisent — une liste déjà triée pendant que le menu affiche l'ancien
critère. Un objet unique ne le peut pas.

Un doublon a disparu au passage : le texte recherché vivait à la fois dans un
`rememberSaveable` de `MainActivity` et dans le flux du ViewModel.

Les previews portent sur les cas qu'on ne voit jamais en lançant
l'application : les quatre erreurs de saisie ensemble, une base sans
emplacement, un médicament dont l'emplacement a été supprimé, une entrée
d'historique sans auteur ni date, les listes vides.

### T-24 · États de chargement et gestion des erreurs · 1 j

**Problème.** Une erreur Firestore **tuait l'application** : le `callbackFlow`
se fermait sur l'exception, qui remontait jusqu'au collecteur. Une liste vide
voulait dire trois choses — stock vide, chargement en cours, lecture échouée.

**Fait.** Une exception métier dans la couche data, un état de chargement et
un état d'erreur dans chaque `UiState`, une fenêtre à valider pour les échecs
d'écriture.

```
Firestore → StockException(PERMISSION|NETWORK|UNAVAILABLE|INSUFFICIENT_STOCK|UNKNOWN)
          → UiState → écran
```

Un seul fichier connaît les codes d'erreur de Firestore. Au-dessus, tout le
monde raisonne sur des raisons métier — choisies parce que ce sont les
réactions possibles pour un opérateur : il n'a pas le droit, il n'a pas de
réseau, il doit réessayer, ou il faut appeler quelqu'un.

**Détail — deux natures d'erreur.** Les lectures produisent un état d'écran,
affiché à la place de la liste. Les écritures produisent un message à part,
montré dans une **fenêtre à valider** : un snackbar s'efface tout seul et rien
ne garantit qu'il ait été lu. Un mouvement de stock refusé dont l'opérateur
n'a rien vu se solde par un écart d'inventaire.

!!! danger "Le double d'essai ne pouvait pas révéler ce défaut"

    Les implémentations en mémoire ne tombent jamais en panne. Aucun test ne
    pouvait donc montrer que l'application mourait sur une erreur Firestore.

    D'où `FailingMedicineRepository`, qui échoue toujours avec la raison
    demandée. Les dix tests qu'il porte ne rateraient pas une assertion sur
    la version précédente : ils planteraient.

### T-51 · Blocage hors ligne · 0,5 j

**Problème.** Hors ligne, Firestore ne signale aucune erreur : il sert son
cache et met les écritures en attente. La panne est donc **invisible** — un
stock vide faute de cache se lit comme un stock réellement vide.

**Décision métier.** Sans réseau, l'application n'affiche aucune donnée et
n'autorise aucune action. Deux raisons, tranchées avec le métier :

- Les **transactions** Firestore, dont dépendent les mouvements de stock, ne
  fonctionnent pas hors ligne. Laisser les boutons actifs promettrait des
  opérations qui n'auraient pas lieu.
- Un comptage manuel effectué sur des chiffres périmés est pire que pas de
  comptage : il produit un écart que personne ne sait ensuite expliquer.

L'entreprise équipe ses opérateurs et fournit la couverture réseau : le
hors-ligne est un incident, pas un mode de travail.

**Détail.** Un bandeau permanent en haut, un écran d'attente en dessous, et
les barres de navigation retirées. Le `NavHost` reste composé sous une surface
opaque : la pile de navigation survit à la coupure, et l'opérateur retrouve son
écran au retour du réseau.

Le contenu masqué est retiré de l'arbre d'accessibilité — le cacher à l'œil
ne suffit pas, TalkBack continuait d'annoncer les stocks.

**Deux plantages corrigés au passage**, tous deux liés au cycle de vie des
écouteurs :

| Moment | Cause |
|---|---|
| Avant connexion | Un état d'écran observé en permanence ouvrait les écouteurs dès l'écran de connexion, où les règles refusent la lecture |
| À la déconnexion | Les états sont partagés en `WhileSubscribed(5 s)` : l'écouteur survit cinq secondes à l'écran, et la session est révoquée pendant cette fenêtre |

La correction est unique — `whileSignedIn` gèle les flux sur l'état de session
— et traite la cause plutôt que les symptômes écran par écran.

### T-32 · Thème clair, sombre et système · 0,5 j {#t-32}

**Problème.** Le thème XML était figé sur `Material.Light` : le mode sombre ne
pouvait pas fonctionner, d'où un flash blanc au démarrage et des barres
système claires au-dessus d'une interface sombre.

**Fait.** Trois états au choix de l'utilisateur, **Système par défaut**,
persistés dans les préférences et relus au démarrage. Voir le
[cadrage](#cadrage-t-32) pour la raison du défaut retenu.

**Détail.** `dynamicColor` est **supprimé** : il tirait la palette du fond
d'écran de l'utilisateur, rendant les contrastes imprévisibles et toute
conformité WCAG invérifiable. Incompatible avec T-31.

`android:Theme.DeviceDefault.DayNight` aurait été plus direct, mais il n'existe
qu'à partir de l'API 29 alors que l'application descend à l'API 24 — d'où
`values/` et `values-night/`.

### T-50 · Suppression de compte · 1 j

**Demande.** Permettre à un opérateur de supprimer son compte.

**Fait.** Depuis l'écran d'accueil, avec ré-authentification par mot de passe.

**Détail.** Firebase refuse `delete()` si la connexion n'est pas récente. Le
mot de passe lève la contrainte et sert de confirmation : sur un téléphone
laissé déverrouillé, il évite qu'un tiers supprime le compte de son
propriétaire.

**L'historique n'est pas touché**, et l'opérateur en est averti *avant* de
valider. Voir la [question ouverte](#rgpd) sur le sort de cette donnée
personnelle.

### T-21 · Retrait supérieur au stock · 0,5 j

**Problème.** Le stock était plafonné en silence :

```kotlin
val stockAfter = (medicine.stock + delta).coerceAtLeast(0)
```

Dix boîtes en stock, cinquante demandées : le stock tombait à zéro,
l'historique enregistrait « de 10 à 0 », et **rien ne signalait que quarante
unités demandées n'existaient pas**. L'opérateur repartait en croyant les avoir
sorties.

**Fait.** Le mouvement est **refusé**, avec le stock disponible dans le
message : « Retrait refusé : il ne reste que 10 unité(s) en stock. »

**Détail.** Le contrôle est dans la transaction, pas dans l'écran : c'est le
seul endroit qui lit le stock réel au moment de l'écriture. Un contrôle sur la
valeur affichée travaillerait sur un chiffre peut-être périmé, si un autre
opérateur a servi le même médicament entre-temps.

Le refus est traduit **hors** de la transaction : une exception levée dedans
serait enveloppée par Firestore et perdrait sa raison.

!!! warning "Un message de confirmation qui mentait"

    L'écran affichait « 50 unité(s) retirée(s) » **juste après avoir appelé**
    l'opération, sans attendre son résultat. Le message s'affichait donc même
    quand le retrait était refusé, et le refus n'arrivait qu'ensuite — dans un
    second message que l'opérateur pouvait ne jamais voir.

    La confirmation vient désormais du ViewModel, une fois le mouvement
    enregistré. Le champ n'est vidé qu'à ce moment : un retrait refusé conserve
    la saisie.

**Reste ouvert dans T-21** : les doublons de noms de **médicaments** et
l'absence de longueur maximale. Les emplacements, eux, sont traités par
[T-56](#t-56). Voir le [reste à faire](#reste-a-faire).

---

## Lot 6 — Demandes complémentaires

Trois demandes arrivées après la revue, en préparant la présentation.

### T-55 · Corriger la fiche d'un médicament · 0,75 j {#t-55}

**Demande.** Pouvoir rattraper une faute d'orthographe dans un nom, ou déplacer
un médicament d'un emplacement à un autre — et que la correction figure dans
l'historique.

**Fait.** Un bouton « Modifier » sur la fiche détail ouvre le formulaire de
création, prérempli, en mode correction.

**Décision — le stock n'est pas modifiable ici.** C'est le point structurant :
laisser corriger une quantité par ce formulaire contournerait la traçabilité.
Le stock ne bouge que par un mouvement, qui laisse une entrée datée et signée.
Autrement, le journal ne dirait plus d'où vient un écart d'inventaire.

Le champ de quantité disparaît donc en mode correction, et sa validation est
désactivée : un stock devenu illisible ne doit pas empêcher de corriger une
faute de frappe.

**Décision — un seul formulaire pour les deux modes.** Le mode se déduit de la
route : `medicine/new` ne porte pas d'identifiant, `medicine/{id}/edit` si, et
le ViewModel le lit dans son `SavedStateHandle`. Deux écrans séparés auraient
dupliqué la liste déroulante des emplacements et ses contrôles, avec le risque
de les voir diverger.

**Détail.** L'entrée d'historique porte l'action `UPDATE`, avec un stock avant
et après identiques — la correction se distingue d'un mouvement au premier coup
d'œil :

```
Nom modifie de « Dolipran » a « Doliprane » ; Emplacement modifie : Stockage froid
```

Sans changement réel, **rien n'est écrit** : rouvrir une fiche et enregistrer
sans rien toucher ne crée pas d'entrée. Un journal pollué de lignes « rien n'a
changé » perd sa valeur.

!!! warning "Le champ qui aurait fait disparaître le médicament"

    `nameLowercase` porte le tri et la recherche. Le renommage doit le mettre à
    jour en même temps que `name` : l'oublier aurait fait sortir le médicament
    renommé de tous les résultats de recherche, **sans aucune erreur visible**.

### T-56 · Noms d'emplacement uniques · 0,25 j {#t-56}

**Demande.** Empêcher deux emplacements de porter le même nom, et refuser un
nom fait uniquement d'espaces.

**Fait.** Le doublon est refusé à la casse et aux espaces près — « Stockage
froid », « stockage FROID » et «  Stockage froid  » sont le même emplacement.
Le message s'affiche sous le champ, en rouge, et la fenêtre reste ouverte.

**Pourquoi ça compte.** Deux « Stockage froid » seraient **indiscernables dans
la liste déroulante** du formulaire de médicament. L'opérateur ne saurait pas
lequel il choisit, et le stock se répartirait entre deux emplacements
identiques sans que personne s'en aperçoive.

**Détail.** Le nom vide était déjà refusé — le bouton de validation est
désactivé — mais silencieusement. La règle est désormais vérifiée aussi dans le
ViewModel, avec son message : une règle métier ne doit pas dépendre d'un détail
d'affichage.

La vérification lit la liste **à la source** plutôt que dans l'état de l'écran,
qui est partagé en `WhileSubscribed` et pourrait être vide si la liste n'était
pas observée au moment du contrôle.

**Changement de comportement** : la fenêtre ne se ferme plus au clic mais au
succès. Sinon un nom refusé disparaîtrait avec son message avant d'avoir été lu.

### T-23 · Chargement paresseux · 0,5 j {#t-23}

**Demande.** Ne charger une donnée qu'au moment où elle sert. Dernier point
ouvert de l'audit green code, dont [T-22](#t-22) avait traité le tri et le
filtre.

**Fait.** Deux lectures qui descendaient bien plus que nécessaire :

| Écran | Avant | Après |
|---|---|---|
| Un emplacement | Toute la liste des médicaments, filtrée en mémoire | `whereEqualTo(aisleId)` : seuls les médicaments de l'emplacement |
| Fiche d'un médicament | Tout l'historique | Les 20 entrées les plus récentes, puis 20 de plus à la demande |

**Pourquoi ça compte.** Sur deux cents références dont douze au froid, ouvrir
l'emplacement « Stockage froid » faisait descendre cent quatre-vingt-huit
documents pour rien — du réseau, de la batterie, et des lectures facturées.
L'historique d'un médicament très manipulé compte des centaines de lignes ;
l'opérateur en lit trois.

**Détail.** Le plafond de l'historique est demandé avec **une entrée de plus**
que ce qui sera affiché : c'est cette entrée excédentaire, jamais montrée, qui
dit à l'écran qu'une page plus ancienne existe. Sans elle il faudrait un
comptage séparé, donc une lecture de plus.

La fenêtre vit dans le ViewModel et non dans l'écran. L'élargir ne reconstruit
pas le flux : la composable continue de collecter le même, et la fiche ne
repasse pas par son indicateur de chargement. Elle revient à sa première page à
chaque abonnement — ouvrir une fiche ne doit pas hériter de la profondeur
atteinte sur la précédente.

!!! note "Pas d'`orderBy` sur le filtre par emplacement"

    Associer un filtre et un tri sur deux champs différents impose un index
    composite à déclarer chez Firestore. Le tri se fait donc en mémoire, sur un
    ensemble désormais restreint par construction — ce qui était précisément le
    problème avant.

---

## Reste à faire {#reste-a-faire}

Identifié, non traité. Rien n'est oublié : tout est dans le suivi des tâches.

### Demandes encore ouvertes

| | Tâche | État actuel |
|---|---|---|
| **T-45** | Journal global du stock | **Transmis au pôle web**, avec son dossier de conception. L'historique reste consultable médicament par médicament dans l'application ; le journal transverse, lui, sort du périmètre mobile — voir [ci-dessous](#t-45) et l'[ambiguïté relevée](analyse.md#une-ambiguite-dans-les-demandes) |

#### T-45 · Le journal du stock part au pôle web {#t-45}

Le service qualité veut répondre à « qui a touché au stock cette semaine ». Le
besoin est légitime et le modèle de données le permet déjà. Il n'a pourtant pas
été réalisé côté Android, et c'est une décision.

**Le contrôle d'accès est intenable côté mobile.** Le journal ne s'adresse pas
aux opérateurs mais au service qui gère les stocks. Or la fiche d'un médicament
et le journal global lisent **la même collection avec la même opération** : un
`list` sur `history`. Une règle Firestore ne sait pas distinguer les deux — elle
dit oui aux deux, ou non aux deux. Masquer l'écran dans l'application ne
protégerait donc rien : il suffit d'extraire l'APK pour interroger la base
directement.

**Le support ne correspond pas à l'usage.** Un journal d'audit se consulte
depuis un bureau : on trie, on croise des critères, on imprime, et on finit
généralement par exporter vers un tableur. C'est une page web, pas un téléphone
tenu d'une main devant une étagère.

**L'équipe compétente existe déjà.** Le pôle IT & Development est composé de
développeurs web ([voir le contexte](contexte.md#lequipe-en-place)). Une
web-app lisant Firestore **depuis son serveur** résout au passage le problème
d'accès : le navigateur ne touche jamais la base, et la frontière devient réelle.

Le dossier de transmission — modèle de données, contraintes de requête, index à
créer, pièges connus et questions à trancher — a été rédigé et remis. Ce qui
reste dans l'application est inchangé : l'historique de chaque fiche, tel que le
Product Owner l'a demandé.

### Robustesse

| | Tâche |
|---|---|
| **T-21** | Reste de la validation : doublons de noms de **médicaments** — deux « Doliprane » restent possibles — et absence de longueur maximale sur les noms |
| **T-57** | Pagination de la liste principale des médicaments, qui descend en entier. Raisonnable sur quelques centaines de références, à revoir sur plusieurs milliers — voir [T-23](#t-23) |
| **T-52** | Détecter l'accessibilité réelle du serveur, et non ce qu'Android croit du réseau. Voir la [limite connue](#limites-connues) |
| **T-58** | **Mot de passe oublié** : l'écran de connexion est un cul-de-sac. Un opérateur qui a oublié son mot de passe n'a aucune issue dans l'application. Voir la [question ouverte](#mot-de-passe-oublie) sur la procédure à retenir |

### Livrables et finition

| | Tâche |
|---|---|
| **T-31** | Accessibilité : parcours TalkBack, zones tactiles, contrastes |
| **T-53** | Monter le BOM Compose pour réactiver `StateFlowValueCalledInComposition` — voir les [limites connues](#limites-connues) |
| **T-54** | Retirer la diffusion interne, devenue du code mort — voir ci-dessous |

#### T-54 · Le `BroadcastReceiver` n'a plus d'objet {#t-54}

L'application s'envoie un message à elle-même 200 ms après le démarrage, le
reçoit, et affiche « Mise à jour reçue ». Personne d'autre n'émet cette action,
personne d'autre ne l'écoute, et le message ne transporte aucune donnée.

C'était le support de la fuite mémoire de [T-06](#t-06) :
le code livré réenregistrait un receiver toutes les 200 ms sans jamais
désenregistrer le précédent. La fuite est corrigée, le mécanisme est resté.

**Deux raisons de le retirer** : c'est une vingtaine de lignes de code mort, et
le Toast s'affiche à chaque lancement alors qu'aucune mise à jour n'a eu lieu —
un opérateur qui le voit tous les matins finira par se demander ce qui s'est
mis à jour.

**Pourquoi seulement après la soutenance.** C'est aujourd'hui la seule trace
*dans le code* du livrable « fuites mémoire » : un lecteur qui ouvre
`MainActivity` y voit l'enregistrement corrigé et le commentaire qui explique
le défaut d'origine. Une fois supprimé, la démonstration ne vit plus que dans
l'historique git, cette page et les captures du Profiler.

### Limites connues {#limites-connues}

Deux comportements assumés, à connaître avant de les rencontrer.

**Le hors-ligne dépend de ce qu'Android déclare.** `ConnectivityManager` répond
à « le téléphone a-t-il une connexion », pas à « puis-je joindre Firestore ».
Relevé trois fois sur l'émulateur de développement :

| Moment | Ce qu'Android déclarait | Réalité |
|---|---|---|
| Matin | `VALIDATED` | DNS mort, `Network is unreachable` |
| Mode avion | `none` | hors ligne — blocage correct |
| Après-midi | actif | réseau revenu |

Un wifi d'hôtel avec portail captif produit le même écart. Le signal fiable
existe et vient de Firestore lui-même : `snapshot.metadata.isFromCache`, vrai
quand les données servies viennent du cache. C'est l'objet de **T-52**.

En attendant, le délai d'attente borné des transactions protège le cas
ambigu : une écriture sans réponse du serveur est signalée comme telle, jamais
annoncée comme réussie.

**Le thème de l'écran de lancement suit le système, pas le choix manuel.** Le
thème XML colore la fenêtre avant que Compose ne dessine. Si le téléphone est
en clair et que l'opérateur force le sombre, l'arrière-plan de lancement reste
clair une fraction de seconde. Le corriger demanderait `AppCompatDelegate`,
donc la dépendance AppCompat que le projet n'a pas, ou
`UiModeManager.setApplicationNightMode`, réservé à l'API 31+.

**Une règle de lint est désactivée.** `StateFlowValueCalledInComposition` ne
signale rien : son détecteur *plante*. Le lint de Compose fourni par le BOM
`2024.04.01` embarque une bibliothèque incapable de lire les métadonnées
Kotlin 2.1. Désactivation ciblée plutôt que `abortOnError = false` — les autres
règles continuent de bloquer la construction. C'est **T-53**.

### Cadrage de T-32 — thème clair/sombre {#cadrage-t-32}

!!! success "Réalisé"

    Ce cadrage a servi de base à [T-32](#t-32). Il est conservé pour la trace
    du raisonnement.

L'audit demandait de « maintenir le respect du mode sombre ». Le thème était
alors figé sur `android:Theme.Material.Light` : le mode sombre ne pouvait pas
fonctionner.

**Le mode ne doit pas être imposé.** On ne connaît pas les besoins visuels des
opérateurs, et le sombre n'est pas universellement plus lisible : les personnes
astigmates lisent souvent moins bien du clair sur fond sombre, à cause du halo
autour des caractères. Un réglage accessible à l'utilisateur est donc nécessaire,
pas seulement un thème qui suit le système.

#### Trois états, pas deux

| État | Comportement |
|---|---|
| Système | Suit le réglage du téléphone |
| Clair | Force le thème clair |
| Sombre | Force le thème sombre |

**Défaut retenu : Système.** Un utilisateur ayant des besoins visuels
particuliers a le plus souvent déjà réglé son téléphone en conséquence — c'est
lui-même un réglage d'accessibilité. Démarrer sur « Système » honore ce choix
sans rien demander ; démarrer sur « Sombre » l'écrase, ce qui est précisément
l'imposition que l'on veut éviter.

Le réglage manuel reste disponible pour le cas où cette application-ci se lit
mieux dans un mode différent du reste du téléphone.

#### Points techniques

- **Le réglage doit être persisté** et relu au démarrage, sinon il est perdu à
  chaque lancement.
- **`dynamicColor` doit être désactivé.** Il tire les couleurs du fond d'écran
  de l'utilisateur : les contrastes deviennent imprévisibles et aucune conformité
  WCAG ne peut être garantie. Incompatible avec T-31.
- Le thème XML doit passer en `DayNight` pour que les barres système suivent.
- Prévoir un bref passage par le thème par défaut au démarrage, le temps que le
  réglage persisté soit lu.

### Questions ouvertes pour le Product Owner

Quatre points relèvent d'une **règle métier**, pas d'un choix technique. Ils sont
signalés ici plutôt que tranchés unilatéralement.

#### Mot de passe oublié : quelle procédure ? {#mot-de-passe-oublie}

L'écran de connexion n'offre aucune issue à qui a oublié son mot de passe. Le
manque est réel, mais la solution évidente — un lien « Mot de passe oublié ? »
qui envoie un courriel de réinitialisation — **suppose que l'opérateur ait accès
à sa boîte mail depuis le téléphone de l'entrepôt**. Sur des appareils partagés,
ce n'est pas acquis.

**Trois politiques possibles :**

| Politique | Conséquence |
|---|---|
| Réinitialisation par courriel dans l'application | Une demi-journée, et l'opérateur se dépanne seul. Suppose un accès à sa messagerie depuis le téléphone, et ouvre une boîte mail personnelle sur un appareil partagé |
| Réinitialisation par un administrateur | Aucun développement : la console Firebase le permet déjà. Demande une procédure écrite et quelqu'un de joignable ; l'opérateur est bloqué en attendant |
| Comptes créés et gérés par un administrateur | La création libre de compte disparaît de l'application. Le plus cohérent avec un usage professionnel, le plus coûteux à mettre en place |

**Ce que la question révèle** : l'application laisse aujourd'hui n'importe qui
créer un compte et accéder au stock. C'est ce que demandait le cahier des
charges, mais la question du mot de passe oublié montre que le modèle de gestion
des comptes n'a jamais été arbitré. La réponse à celle-ci décidera aussi de
celui-là.

#### L'adresse e-mail dans l'historique après suppression du compte {#rgpd}

Celui-ci dépasse le Product Owner : il engage la **politique RGPD de
l'entreprise**.

Chaque entrée d'historique porte l'adresse de son auteur, et les règles de
sécurité déclarent le journal en ajout seul :

```
allow update, delete: if false;
```

Supprimer un compte ne peut donc pas effacer ces traces — et ne le doit pas :
un journal d'audit dont on peut retirer son propre nom ne vaut rien. Mais
conserver l'adresse d'une personne partie est une donnée personnelle
conservée sans limite.

**Trois politiques possibles :**

| Politique | Conséquence |
|---|---|
| Conserver l'e-mail tel quel | Traçabilité intacte. État actuel, choisi comme mesure conservatoire : c'est le seul qui ne détruit rien de façon irréversible en attendant l'arbitrage |
| Anonymiser en gardant un identifiant | « Opérateur #4127 » : on sait que c'est la même personne sur toutes les lignes, sans la nommer. Demande une table de correspondance, qui est elle-même une donnée personnelle |
| Remplacer par « compte supprimé » | Le plus simple, mais le lien entre les mouvements d'un même opérateur est perdu |

L'opérateur est averti **avant** de valider la suppression que ses mouvements
resteront signés de son adresse.

#### Supprimer un médicament encore en stock

Rien n'empêche aujourd'hui de supprimer un médicament dont il reste des unités.
Sur un stock pharmaceutique, c'est discutable : les boîtes existent
physiquement, et la suppression fait disparaître la ligne sans dire ce qu'elles
sont devenues.

**Mesure conservatoire prise** : la fenêtre de confirmation rappelle désormais en
rouge le nombre d'unités restantes. La décision est donc prise en connaissance
de cause, mais elle reste possible.

**Trois politiques possibles, à arbitrer :**

| Politique | Conséquence |
|---|---|
| Interdire tant que le stock n'est pas à zéro | Force à sortir explicitement les unités, donc à les tracer. Le plus rigoureux, le plus contraignant |
| Autoriser avec l'avertissement actuel | État actuel. Rapide, mais l'écart de stock n'est pas expliqué |
| Remplacer la suppression par un archivage | Le médicament sort des listes sans disparaître. Plus fidèle à un stock réel, mais demande un champ d'état et un filtrage partout |

La troisième est la plus proche des usages en pharmacie, et la plus coûteuse.
Le choix appartient au Product Owner.

#### Portée de l'historique

Voir T-45 : « fiche détail d'un magasin » ne correspond à aucun écran de
l'application. L'interprétation retenue et la lecture alternative sont détaillées
dans l'[analyse](analyse.md#une-ambiguite-dans-les-demandes).

### Dette assumée

La porte qualité SonarCloud (`sonar.qualitygate.wait`) est **désactivée**. Elle
exige 80 % de couverture sur le code neuf ; la remettre aujourd'hui ferait échouer
tous les builds. À réactiver quand la couverture aura progressé, en ajustant le
seuil si nécessaire.
