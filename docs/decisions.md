# Décisions d'architecture

Les choix structurants, et surtout **ce qui a été écarté et pourquoi**.

Le code dit ce qu'il fait ; il ne dit pas pourquoi on n'a pas fait autrement.
Cette page existe pour ça. C'est aussi la partie de la documentation qui ne
périme pas : une implémentation change, un raisonnement reste.

---

## Architecture

### MVVM strict, pas de clean architecture

**Trois couches et rien de plus** : `data` (modèles et repositories), `ui`
(écrans et ViewModels), `di` (injection).

Pas de couche `domain`, pas de cas d'usage, pas de conversion DTO ↔ domaine. Un
ViewModel appelle directement son repository.

Ce que ça coûte : si une règle métier devait être partagée entre plusieurs
ViewModels, elle n'aurait pas d'endroit naturel où vivre. Ce n'est pas le cas
ici — la seule règle transverse, la journalisation, est descendue dans le
repository (voir plus bas).

Ce que ça rapporte : une lecture directe. Pour comprendre ce que fait un écran,
on ouvre deux fichiers au lieu de cinq.

!!! note "Alternative écartée"

    Une clean architecture complète avec cas d'usage et modèles de domaine
    séparés. Pour une application de cette taille, elle aurait ajouté une couche
    d'indirection que rien ne justifiait.

### Des interfaces de repository

`MedicineRepository`, `AisleRepository` et `UserRepository` sont des interfaces,
avec deux implémentations chacune : une en mémoire et une Firebase.

Ce n'était pas gratuit à écrire. Voici ce que ça a rapporté, concrètement :

- **Le passage de la mémoire à Firestore a demandé trois lignes** dans
  `AppModule` — changer `InMemoryMedicineRepository` en
  `MedicineRepositoryImpl` dans les liaisons. Aucun ViewModel, aucun écran,
  aucun test n'a bougé.
- **Les implémentations en mémoire servent aujourd'hui de doubles de test.**
  Elles ne sont pas du code mort : elles font tourner les tests unitaires et les
  tests d'interface sans réseau ni émulateur Firebase.

C'est le seul endroit du projet où l'on a payé d'avance une abstraction. Elle
s'est remboursée deux fois.

---

## Modèle de données

### Un identifiant stable, pas le nom

Le code d'origine retrouvait un médicament par son nom : `find { it.name == name }`,
et le transportait entre écrans par `putExtra("nameMedicine")`.

Deux médicaments homonymes étaient indiscernables, et corriger une faute de
frappe dans un nom cassait toutes les références.

`MedicineDto` et `AisleDto` portent désormais un `id` fourni par la source de données —
l'identifiant du document Firestore. Un médicament référence son rayon par
`aisleId` et non par son libellé : **renommer un rayon ne détache plus les
médicaments qu'il contient.**

### Des modèles immuables

`data class` avec des `val`, valeurs par défaut sur toutes les propriétés.

Les valeurs par défaut ne sont pas un détail de style : Firestore a besoin d'un
constructeur sans argument pour désérialiser. En Kotlin, une `data class` dont
tous les paramètres ont une valeur par défaut en génère un automatiquement.

### L'historique vit hors du médicament

`MedicineDto` ne porte pas ses entrées d'historique. Elles sont dans une
**collection racine `history`**, avec un champ `medicineId`.

Deux raisons :

1. **Une suppression doit rester tracée.** Si l'historique vit dans le document
   du médicament, supprimer le médicament efface sa propre trace — or c'est
   l'opération que le service qualité a le plus besoin de retrouver.
2. **Un document Firestore plafonne à 1 Mio.** Un tableau d'historique embarqué
   grandit sans limite, et chaque mouvement de stock réécrirait le document
   entier.

Le contrat `observeHistory(medicineId)` masque ce choix de stockage : passer un
jour à une sous-collection ne toucherait ni les ViewModels ni les écrans.

!!! note "Alternative écartée"

    Une sous-collection `medicines/{id}/history`. Plus naturelle à première vue,
    mais elle rend le journal global — « qui a touché au stock cette semaine » —
    dépendant d'un `collectionGroup`, et laisse des sous-collections orphelines
    après suppression.

---

## Fiabilité de l'historique

Le service qualité ne se plaignait pas d'une ligne manquante. Il se plaignait de
**ne pas pouvoir se fier à l'historique**. Trois décisions répondent à ça.

### L'écriture est dans le repository, pas chez l'appelant

`addMedicine`, `updateStock` et `deleteMedicine` écrivent elles-mêmes leur trace.

Tant qu'un appelant doit *penser* à journaliser son opération, il finit par
oublier — c'est exactement ce qui s'était produit dans le code d'origine. En
descendant l'écriture dans l'opération elle-même, **l'oubli devient impossible
par construction**, y compris pour du code qui n'existe pas encore.

### Le mouvement et sa trace dans une transaction

`updateStock` et `deleteMedicine` utilisent `runTransaction`, `addMedicine` un
`WriteBatch`. Jamais deux écritures successives.

1. **Atomicité.** Si l'application meurt entre les deux écritures, on obtient un
   stock modifié sans trace — précisément l'incohérence qu'on prétendait
   corriger. On l'aurait déplacée, pas supprimée.
2. **Concurrence.** Sur des téléphones partagés entre opérateurs, deux personnes
   peuvent retirer une boîte au même instant. Lire le stock, calculer `stock - 1`,
   écrire : sans transaction, l'un des deux retraits disparaît. Firestore relit
   et réessaie.

Firestore impose que toutes les lectures précèdent toutes les écritures dans une
transaction — le code est structuré en conséquence.

### Des identifiants de document fixes pour l'amorçage

Les trois emplacements de stockage standards sont créés avec des identifiants
choisis — `standard`, `cold`, `secured` — et non générés.

C'est ce qui rend l'amorçage **idempotent**. L'opération est appelée à chaque
ouverture de session ; avec des identifiants aléatoires, deux appareils
démarrant en même temps sur une base vide auraient créé six emplacements au lieu
de trois. Un `set` sur un identifiant fixe écrit toujours au même endroit.

Le `merge` préserve par ailleurs un libellé qui aurait été personnalisé depuis la
console.

### Un journal en ajout seul

Les règles Firestore autorisent `create` sur `history`, et **refusent `update` et
`delete` à tout le monde**, sans exception.

C'est ce qui distingue un journal d'audit d'un simple log. Sans cette règle,
n'importe quel opérateur pourrait réécrire l'historique depuis l'application, et
la traçabilité demandée ne vaudrait rien.

La règle vérifie en outre que `userEmail` correspond au compte appelant : on ne
peut pas signer une entrée au nom de quelqu'un d'autre.

---

## Navigation

### Une seule Activity

Les écrans de détail étaient des `Activity` distinctes, reliées par des `Intent`.
Elles sont devenues des **destinations d'un `NavHost`**.

Ce n'était pas un choix esthétique. Deux `Activity` séparées ne pouvaient pas
partager le `ViewModel` de `MainActivity` autrement que par une **référence
statique vers elle** :

```kotlin
companion object { lateinit var mainActivity: MainActivity }
```

Une référence statique vers une `Activity` survit à sa destruction : à chaque
rotation, l'instance précédente restait retenue avec toute sa hiérarchie de vues.
**La seconde fuite mémoire du projet était une conséquence directe du découpage
en Activity.** Unifier la navigation l'a supprimée mécaniquement.

### Le retour ferme l'application quand la pile est vide

Chaque bascule d'authentification vide la pile de navigation : après une
déconnexion, le bouton retour ne doit pas ramener sur les écrans de stock.

Mais une pile vidée ne contient plus qu'une entrée. Le retour système la
dépilait à son tour, et le `NavHost` n'avait plus rien à afficher : **écran noir,
application vivante mais vide.**

```kotlin
BackHandler(enabled = navController.previousBackStackEntry == null) {
    activity?.finish()
}
```

La condition décrit l'intention : rien derrière, donc on sort. Sur un écran de
détail, `previousBackStackEntry` existe, le gestionnaire est désactivé et le
retour reprend son comportement normal.

---

## Authentification

### Des écrans écrits à la main plutôt que FirebaseUI

FirebaseUI Auth fournit connexion, création de compte et mot de passe oublié clés
en main. Il a pourtant été écarté.

C'est une `AppCompatActivity`. L'utiliser imposait de revenir à un thème
AppCompat et de réintroduire une `Activity` juste après en avoir supprimé deux —
et le thème AppCompat forcé en clair est précisément ce qui bloquait le mode
sombre dans la première version du projet.

Le coût : environ 200 lignes d'écrans Compose et un `AuthViewModel`. Le gain :
un graphe de navigation cohérent, une validation testable en JVM, et des
messages d'erreur en français plutôt que les libellés bruts de Firebase.

### L'écran d'accueil est revalidé à chaque démarrage

Après connexion, un écran nomme la session ouverte et propose d'en sortir avant
d'accéder au stock.

L'état « accueil validé » vit dans un `ViewModel`, pas dans un `rememberSaveable` :
il doit survivre à une **rotation d'écran** mais pas au **relancement de
l'application**. Sur un téléphone partagé entre opérateurs, chaque démarrage doit
repasser par l'avertissement.

C'est la réponse à la remarque du Product Owner : « les téléphones peuvent être
utilisés par différentes personnes ».

---

## Frontières entre les couches

### Trois familles d'objets, pas une

`MedicineDto` décrit la forme du document Firestore. `MedicineUi` décrit ce que
l'écran affiche. Le ViewModel convertit l'un en l'autre.

L'alternative — un seul modèle traversant toute l'application — coûte moins
cher à écrire et se paie plus tard : un changement de schéma de base remonte
jusqu'à l'affichage, et l'écran finit par porter des champs qui n'ont de sens
que pour la base.

Le mapping est **dans le ViewModel**, pas dans le dépôt. Un dépôt qui
fabriquerait des objets d'affichage devrait connaître l'affichage, ce qui
annulerait la séparation.

Chaque modèle `Ui` justifie son existence en retirant ou en ajoutant quelque
chose : `UserUi` ne porte pas l'UID Firebase, `HistoryUi` porte une date déjà
formatée, `MedicineUi` porte le libellé de son emplacement — que le document
Firestore ne contient pas.

### Les messages sont des identifiants de ressource, pas des chaînes

Un ViewModel n'a pas de `Context`. Lui en injecter un pour résoudre des
libellés en ferait une classe dépendante d'Android, donc non testable sans
émulateur — et un `Context` retenu par un objet à longue vie est un classique
de la fuite mémoire.

L'état porte donc `@StringRes val emailError: Int?`, et l'écran résout. Quand
un message a besoin d'un argument — « il ne reste que 10 unité(s) » —
`UiMessage(res, args)` transporte la valeur, l'écran met en forme.

### `@Keep` sur les modèles lus par réflexion

Firestore remplit les objets en comparant **le nom du champ** à la clé du
document. L'obfuscation renommerait `stockAfter` en `a` : plus aucune
correspondance, le champ garde sa valeur par défaut, et **aucune erreur n'est
levée**.

L'annotation ne va que sur les classes concernées — celles passées à
`toObject()` et l'énumération qu'elles contiennent. `UserDto`, construit à la
main depuis `FirebaseUser`, n'en a pas besoin : R8 renomme le champ et son
appel de façon cohérente.

`java.io.Serializable`, parfois cité pour cet usage, ne protège de rien ici :
c'est un mécanisme de sérialisation Java, que R8 ignore et que Firestore
n'utilise pas.

---

## Erreurs et disponibilité

### Une erreur métier, pas une exception Firebase

Les dépôts n'exposent que `StockException`, avec cinq raisons. L'écran ne
connaît donc pas Firestore, et changer de base de données ne demanderait pas de
réécrire l'affichage des erreurs.

Les raisons sont choisies sur les **réactions possibles** de l'opérateur — il
n'a pas le droit, il n'a pas de réseau, il doit réessayer, le stock est
insuffisant, ou il faut appeler quelqu'un — et non sur la taxonomie technique
du fournisseur.

### Un refus se valide, une réussite s'annonce

Les échecs d'écriture passent par une **fenêtre à valider**, les confirmations
par un message éphémère.

Un snackbar s'efface tout seul, en bas de l'écran, et rien ne garantit qu'il
ait été lu. Pour un mouvement de stock refusé, l'opérateur repartirait en
croyant son retrait enregistré, et l'écart n'apparaîtrait qu'à l'inventaire.
Une opération réussie, elle, n'a pas besoin d'être acquittée — et interrompre
cinquante mouvements par jour serait pénible.

### La confirmation vient du résultat, pas du geste

L'écran affichait « 50 unité(s) retirée(s) » juste après avoir appelé
l'opération, sans attendre. Le message s'affichait donc même quand le mouvement
était refusé.

La confirmation est maintenant émise par le ViewModel **après** l'écriture. Le
champ de saisie n'est vidé qu'à ce moment : un retrait refusé conserve la
saisie.

### Les transactions ne tolèrent pas le silence, les écritures simples si

Deux enveloppes différentes autour des appels Firestore :

| | Comportement sans réponse du serveur |
|---|---|
| Écriture simple (création) | Considérée acquise : Firestore l'a appliquée localement et la rejouera |
| Transaction (mouvement, suppression) | Considérée **échouée** : une transaction relit côté serveur, sans réponse elle n'a pas eu lieu |

Traiter les deux pareil annoncerait un mouvement de stock qui ne s'est pas
produit — c'est ce que faisait une première version de la correction, et
c'était pire que le blocage qu'elle remplaçait.

### Le contrôle de stock est dans la transaction

Un retrait supérieur au stock est refusé là où le stock réel est lu, au moment
de l'écriture. Un contrôle dans l'écran travaillerait sur la valeur affichée,
peut-être périmée si un autre opérateur a servi le même médicament entre-temps.

Refus plutôt que plafonnement : sur un stock pharmaceutique, l'écart entre ce
qui a été demandé et ce qui a été fait doit remonter, pas disparaître.

### Hors ligne, l'application se bloque

Firestore sert son cache sans rien signaler. La panne est donc invisible, et un
stock vide faute de cache se lit comme un stock réellement vide.

Deux raisons de tout bloquer plutôt que de laisser travailler :

- Les transactions ne fonctionnent pas hors ligne. Les boutons actifs
  promettraient des opérations qui n'auraient pas lieu.
- Un comptage manuel sur des chiffres périmés produit un écart d'inventaire que
  personne ne sait ensuite expliquer.

L'entreprise fournit la couverture réseau à ses opérateurs : le hors-ligne est
un incident, pas un mode de travail.

Le `NavHost` reste composé sous une surface opaque, pour que la pile de
navigation survive à la coupure — et le contenu masqué est retiré de l'arbre
d'accessibilité, faute de quoi TalkBack continuerait d'annoncer les stocks
qu'on a décidé de ne pas montrer.

### Les flux sont gelés sur l'état de session

`whileSignedIn` annule les écouteurs Firestore dès que la session tombe.

Sans cela, deux moments provoquaient un plantage : avant la connexion, et
surtout **à la déconnexion** — les états sont partagés en
`WhileSubscribed(5 s)`, donc l'écouteur survit cinq secondes à l'écran qui
l'observait, et Firebase révoque la session pendant cette fenêtre.

Une correction unique à la source, plutôt qu'un traitement écran par écran.

---

## Limites acceptées

### La recherche est un « commence par », pas un « contient »

Firestore ne sait pas faire de recherche textuelle. Seuls les intervalles sont
possibles, via un champ technique `nameLowercase` et une borne haute `\uf8ff`.

**Conséquence assumée : chercher « prane » ne trouve plus « Doliprane ».**

Un vrai « contient » demanderait un service de recherche externe — Algolia,
Typesense — c'est-à-dire une dépendance de plus, un coût, et des données
dupliquées hors de Firestore. Disproportionné pour un champ de recherche sur un
catalogue de cette taille.

Le tri, lui, est bien exécuté côté serveur. Quand une recherche est active,
Firestore impose que le premier `orderBy` porte sur le champ de l'intervalle : le
tri demandé s'applique alors sur le résultat déjà restreint.

---

## Tests

### Des doubles écrits à la main, pas une bibliothèque de mocks

Aucune dépendance de mocking n'a été ajoutée. Les tests utilisent
`InMemoryMedicineRepository`, `InMemoryAisleRepository` et un `FakeUserRepository`
écrit à la main.

Les implémentations en mémoire sont de **vraies implémentations du contrat**.
Un test qui passe contre elles valide un comportement réellement possible ; un
mock, lui, répond ce qu'on lui a dit de répondre — y compris des choses que
l'implémentation réelle ne fera jamais.

!!! warning "La limite de l'approche"

    Un double reste un double. `InMemoryAisleRepository` créait un emplacement
    de stockage au démarrage ; `AisleRepositoryImpl` n'en créait aucun.
    Tous les tests passaient, et l'application était pourtant inutilisable sur
    une base neuve — impossible de créer un médicament faute d'endroit où le
    ranger.

    **Quand un double se comporte mieux que l'implémentation réelle, il cache ce
    qu'il devrait révéler.** Le défaut a été trouvé par une question métier sur
    les règles de stockage, pas par la suite de tests.

### Le retour arrière passe par le dispatcher, pas par Espresso

Dans les tests d'interface, le bouton retour est simulé ainsi :

```kotlin
scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
```

`Espresso.pressBack()` avait été utilisé d'abord, avec une assertion élégante :
l'exception `NoActivityResumedException` signale que l'application s'est fermée.

Elle passait en local et échouait un run sur deux en intégration continue.
Espresso exige que la fenêtre ait le focus avant d'injecter un événement, et sur
un émulateur de CI ce focus arrive parfois après son délai de dix secondes.

Le dispatcher emprunte exactement le même chemin que le bouton système —
`BackHandler` compris, donc le test vérifie toujours la bonne chose — sans
dépendre du focus.

!!! warning "Un test intermittent est pire qu'un test absent"

    On prend l'habitude de relancer le job, puis d'ignorer le rouge — et le jour
    où c'est un vrai bug, on passe à côté.

---

## Contraintes de la chaîne de build

Deux versions sont figées volontairement. Sans ces notes, la tentation de les
« mettre à jour » reviendra.

### Firebase BOM bloqué en 33.x

Le BOM 34.x embarque `firebase-auth` 24.x, compilé avec des métadonnées Kotlin
2.3 que le compilateur Kotlin 2.1 du projet ne sait pas lire. La compilation
échoue.

Monter Kotlin en 2.3 entraînerait le plugin Compose Compiler, KSP et Hilt avec
lui. Le BOM 33.x est aligné sur Kotlin 2.1 et suffit.

### `commons-compress` forcé sur le classpath racine

```kotlin
buildscript {
    configurations.classpath {
        resolutionStrategy { force("org.apache.commons:commons-compress:1.28.0") }
    }
}
```

Le plugin Sonar appelle une méthode introduite dans `commons-compress` 1.24. AGP
apporte la 1.21 sur le classpath racine, qui est le **classloader parent** de
celui du module `:app` : par délégation parent-first, la 1.21 masque la version
du plugin et la tâche `sonar` échoue sur une méthode introuvable.

!!! note "Alternative écartée"

    Déclarer le plugin Sonar dans le bloc `plugins` racine règle aussi le
    problème — mais fait monter **toutes** ses dépendances transitives sur le
    classpath d'AGP. `bcprov` passe alors en 1.84 pendant que `bcpkix` et
    `bcutil` restent en 1.77, panachage que BouncyCastle ne supporte pas : la
    génération du keystore de debug casse en CI. Cette piste a été essayée avant
    d'être abandonnée.
