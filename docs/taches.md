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
| 5 | Finition | À faire |

**Réalisé : 27 tâches.** Le [reste à faire](#reste-a-faire) est détaillé en fin
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

### T-06 · Fuite mémoire du BroadcastReceiver · 0,5 j

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

### T-22 · Tri et filtre côté serveur · 0,75 j

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

**Fait.** 51 tests sur les repositories et les cinq `ViewModel`.

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

## Reste à faire {#reste-a-faire}

Identifié, non traité. Rien n'est oublié : tout est dans le suivi des tâches.

### Demandes encore ouvertes

| | Tâche | État actuel |
|---|---|---|
| **T-45** | Journal global du stock | L'historique n'est consultable que médicament par médicament. Le service qualité a besoin de « qui a touché au stock cette semaine ». Le modèle le permet déjà, il ne manque qu'un écran — voir l'[ambiguïté relevée](analyse.md#une-ambiguite-dans-les-demandes) |

### Robustesse

| | Tâche |
|---|---|
| **T-21** | Validation des saisies au-delà du formulaire d'authentification |
| **T-23** | Chargement paresseux des listes |
| **T-24** | États de chargement et gestion des erreurs réseau — `NetworkRepository` est encore une coquille vide |

### Livrables et finition

| | Tâche                                                                       |
|---|-----------------------------------------------------------------------------|
| **T-31** | Accessibilité : parcours TalkBack, zones tactiles, contrastes               |
| **T-32** | Thème clair/sombre — voir le [cadrage](#cadrage-t-32) ci-dessous            |
| **T-34** | Externalisation des chaînes — `strings.xml` ne contient qu'une seule entrée |

### Cadrage de T-32 — thème clair/sombre {#cadrage-t-32}

L'audit demandait de « maintenir le respect du mode sombre ». Le thème est
aujourd'hui figé sur `android:Theme.Material.Light` : le mode sombre ne peut pas
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

Deux points relèvent d'une **règle métier**, pas d'un choix technique. Ils sont
signalés ici plutôt que tranchés unilatéralement.

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
