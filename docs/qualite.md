# Tests, CI et mesures

!!! info "Chiffres relevés au moment de la rédaction"

    Les valeurs de couverture évoluent à chaque commit. Les tableaux de bord
    liés depuis l'[accueil](index.md#liens) font foi.

---

## Intégration continue

Deux jobs GitHub Actions, déclenchés sur chaque `push` vers `main` et sur chaque
pull request.

| Job | Contenu | Durée |
|---|---|---|
| `build-and-test` | Compilation debug, tests unitaires, lint, couverture, analyse SonarCloud | ~3 min |
| `instrumented-tests` | Tests d'interface sur émulateur API 34 | ~5 min |

### Pourquoi deux jobs séparés

L'émulateur met plusieurs minutes à démarrer et n'a rien à partager avec
l'analyse Sonar. En parallèle, il ne retarde pas le retour sur la compilation et
les tests unitaires — c'est-à-dire sur 90 % des erreurs.

Le job émulateur est aussi le plus fragile de la chaîne. Le séparer permet, si
besoin, de le rendre non bloquant sans toucher au reste.

### Les autres workflows

| Workflow | Déclencheur | Rôle |
|---|---|---|
| Documentation | `docs/**` ou `mkdocs.yml` modifiés | Construit le site ; ne le publie que depuis `main` |
| Distribution de l'APK | Tag `v*` ou manuel | Construit un APK de release signé et l'envoie sur Firebase App Distribution |

Le workflow de documentation construit aussi sur les pull requests, sans
publier. Il a détecté un vrai défaut dès sa première exécution : les captures du
Profiler n'étaient pas versionnées, un motif `profiler/` du `.gitignore` les
excluant sans qu'on s'en aperçoive. La construction locale passait — MkDocs lit
le disque, la CI part d'un `git clone`.

### Ordonnancement

Les étapes sont volontairement distinctes plutôt que regroupées en une seule
invocation Gradle : sur la page d'un run, on voit immédiatement **laquelle** a
échoué. C'est aussi ce qui rend la capture d'écran de la CI lisible.

`jacocoTestReport` s'exécute juste avant `sonar`, dans la même invocation : le
scanner lit le XML de couverture produit par cette tâche.

---

## Difficultés rencontrées

Elles sont documentées ici parce qu'elles reviendront — et parce que trois
d'entre elles ont coûté plus de temps que le code qu'elles bloquaient.

### Conflit de classpath entre AGP et le plugin Sonar

**Symptôme.** `Execution failed for task ':app:sonar'` suivi d'une signature de
méthode, sans autre explication.

**Cause.** Le scanner Sonar appelle une méthode de `commons-compress` introduite
en 1.24. AGP apporte la 1.21 sur le classpath du projet racine, qui est le
**classloader parent** de celui du module `:app`. Par délégation parent-first, la
1.21 masque la version du plugin.

**Correctif.** Un `resolutionStrategy.force` sur le classpath racine. La piste
consistant à déclarer le plugin Sonar à la racine a été essayée et abandonnée :
elle déplace le problème sur BouncyCastle. Voir
[Décisions](decisions.md#commons-compress-force-sur-le-classpath-racine).

### Métadonnées Kotlin trop récentes dans Firebase

**Symptôme.** `kspDebugKotlin` échoue sans message d'erreur visible.

**Cause.** `firebase-auth` 24.x, tiré du BOM 34.x, est compilé avec des
métadonnées Kotlin 2.3 que le compilateur 2.1 du projet ne sait pas lire.

**Correctif.** BOM Firebase figé en 33.x. Monter Kotlin entraînerait le plugin
Compose Compiler, KSP et Hilt avec lui.

!!! warning "L'erreur était masquée par un filtre de lecture"

    Le message apparaissait dans les lignes `kotlin_module`, celles-là mêmes qui
    étaient filtrées pour masquer le bruit de Firebase Analytics. Le diagnostic a
    pris cinq allers-retours pour une cause visible dès la première ligne.

### Espresso incompatible avec les versions récentes d'Android

**Symptôme.** Les sept tests d'interface échouent sur
`NoSuchMethodException: android.hardware.input.InputManager.getInstance`.

**Cause.** Espresso 3.6.1 appelle cette méthode par réflexion ; elle n'existe plus
sur les Android récents.

**Correctif.** Espresso 3.7.0 et `androidx.test.ext:junit` 1.3.0.

### Retour arrière Espresso instable en CI

**Symptôme.** Trois tests verts en local et sur un run, rouges sur le suivant :
`RootViewWithoutFocusException ... has-window-focus=false`.

**Cause.** Espresso exige que la fenêtre ait le focus avant d'injecter un
événement. Sur un émulateur de CI, ce focus arrive parfois après son délai de dix
secondes. Les cinq tests purement Compose passaient — seuls ceux utilisant
`pressBack()` échouaient.

**Correctif.** Le retour passe désormais par `onBackPressedDispatcher`, qui
emprunte le même chemin que le bouton système sans dépendre du focus. Détail dans
[Décisions](decisions.md#le-retour-arriere-passe-par-le-dispatcher-pas-par-espresso).

!!! danger "Un test intermittent est pire qu'un test absent"

    On prend l'habitude de relancer le job, puis d'ignorer le rouge — et le jour
    où c'est un vrai bug, on passe à côté. Un échec intermittent doit être traité
    comme un échec, pas comme un aléa.

### Secrets de signature jamais validés

**Symptôme.** Deux échecs successifs du workflow de distribution :
`No key with alias ... found in keystore`, puis
`Get Key failed: Given final block not properly padded`.

**Cause.** Le workflow dont celui-ci est adapté écrivait le mot de passe du
magasin dans les deux champs de signature, alors qu'un secret `KEY_PASSWORD`
distinct était déclaré. Ce secret existait, semblait correct — et n'était jamais
lu. Sa valeur était fausse depuis toujours.

**Correctif.** Utiliser le bon secret pour le bon champ, et corriger sa valeur.

!!! note "Un secret jamais utilisé est un secret jamais validé"

    Rien ne signalait l'erreur tant que la valeur n'était pas lue. Et GitHub
    n'affiche jamais le contenu d'un secret : une espace finale y reste
    invisible jusqu'à ce qu'elle casse quelque chose, sans qu'aucun message
    d'erreur ne la mentionne.

### Couverture bloquée à 0 %

**Symptôme.** SonarCloud affiche 0 % alors que les tests passent.

**Deux causes successives.**

1. `enableUnitTestCoverage` n'était pas activé dans le type de build `debug` :
   `testDebugUnitTest` ne produisait aucun fichier `.exec`, donc le rapport JaCoCo
   était vide quelles que soient les corrections de chemins.
2. Une fois corrigé, la couverture restait à 0 % **sur les pull requests** — c'est
   normal : une PR ne mesure que le code neuf, et celle qui branchait JaCoCo ne
   modifiait aucune ligne de Kotlin.

---

## Tests unitaires

**84 tests** sur les repositories et les cinq `ViewModel`, exécutés sur la JVM,
sans émulateur ni réseau.

| Classe testée | Tests |
|---|---|
| `InMemoryMedicineRepository` | 19 |
| `MainViewModel` | 14 |
| `AuthViewModel` | 11 |
| `AisleViewModel` | 10 |
| `MedicineFormViewModel` | 10 |
| `MedicineViewModel` | 10 |
| `MedicineViewModel` — chemins d'échec | 10 |

### Ce qu'ils verrouillent

Ce ne sont pas des tests de couverture : **chacun correspond à un défaut
réellement rencontré**, et échouerait sur le code d'origine.

| Test | Défaut verrouillé |
|---|---|
| `updateStock updates the requested medicine and leaves the others untouched` | L'index hors bornes, **et** sa fausse correction en `size - 1` |
| `updateStock records the operation with its before and after values` | L'historique ajouté à une copie jetée |
| `clearing the search restores the full list` | Le filtre qui écrasait la source de vérité |
| `deleting a medicine keeps its history` | La trace qui disparaissait avec le médicament |
| `a stock change is signed with the signed in operator` | L'identifiant technique codé en dur |
| `signing out resets the welcome acknowledgement` | Le garde-fou des téléphones partagés |
| `a failed stock movement is reported without crashing` | L'exception Firestore qui tuait le processus |
| `a removal larger than the stock is refused` | Le plafonnement silencieux à zéro |
| `a refused movement produces no confirmation` | Le message de succès affiché avant l'opération |
| `losing the network switches the application to offline` | Le stock périmé pris pour un stock réel |

### Un double d'essai qui échoue exprès

Les implémentations en mémoire ne tombent jamais en panne — c'est leur intérêt,
et c'est aussi pourquoi elles n'ont jamais révélé que l'application mourait sur
une erreur Firestore.

`FailingMedicineRepository` échoue toujours, avec la raison demandée. Les neuf
tests qu'il porte ne rateraient pas une assertion sur la version précédente :
ils **planteraient**.

Même logique pour `FakeNetworkMonitor` : aucun test unitaire ne peut couper le
wifi, d'où l'interface `NetworkMonitor`.

### Deux pièges de mise en place

**`Dispatchers.Main` n'existe pas sur la JVM.** Tout `ViewModel` utilisant
`viewModelScope` échoue sans une règle qui le remplace par un dispatcher de test.

**Un `StateFlow` en `WhileSubscribed` n'émet pas sans collecteur.** Les premiers
tests assertaient sur la valeur initiale — ils passaient en ne vérifiant rien. Il
faut lancer un collecteur *et* faire tourner le test sur le même dispatcher, sans
quoi le collecteur ne démarre jamais.

!!! note "Le pire test est celui qui passe sans rien vérifier"

    Il donne une fausse assurance et ne se signale jamais.

---

## Tests d'interface

**9 tests de parcours**, exécutés sur émulateur.

| Test | Ce qu'il vérifie |
|---|---|
| `withoutSession_theSignInScreenIsShown` | L'accès est verrouillé sans session |
| `withSession_theWelcomeScreenIsShownAndItsButtonsRespond` | Le démarrage avec session ouverte, et **que l'interface répond** |
| `signingOutFromTheWelcomeScreen_returnsToSignIn` | Déconnexion depuis l'accueil |
| `signingOutFromTheStock_returnsToSignIn` | Déconnexion depuis la barre supérieure |
| `backFromTheStockRoot_closesTheApp` | Le retour ferme l'application au lieu d'un écran noir |
| `backFromTheSignInScreen_closesTheApp` | Idem depuis l'écran de connexion |
| `backFromAMedicineDetail_returnsToTheList` | Le gestionnaire de retour n'est pas trop large |

Deux d'entre eux verrouillent des bugs trouvés à la main pendant les tests
manuels : l'écran noir après déconnexion et le blocage de l'écran d'accueil.

### Montage

Hilt ne fonctionne pas en test instrumenté sans un runner qui substitue
`HiltTestApplication` à l'application réelle. Un module annoté `@TestInstallIn`
remplace les liaisons de repositories par des doubles : **aucun test n'atteint
Firebase**, ce serait lent, dépendant du réseau, et créerait de vrais comptes.

L'`Activity` est lancée à la main plutôt que par une règle, pour pouvoir
installer une session **avant** son démarrage — c'est ce qui permet de reproduire
le cas « application relancée avec session ouverte » sans tuer le processus.

---

## Couverture

| Mesure | Valeur |
|---|---|
| Lignes couvertes | 24,3 % |
| Branches couvertes | 7,5 % |
| **Indicateur SonarCloud** | **16,2 %** |

### Pourquoi trois chiffres différents

SonarCloud ne mesure pas la même chose que JaCoCo : il **combine lignes et
branches** en un seul indicateur. Un projet à 24 % de lignes et 7 % de branches
affiche donc 16 % dans Sonar, sans qu'aucun chiffre ne soit faux.

Ce sont les branches qui pèsent : chaque `if`, chaque `when`, chaque `?:` compte
pour deux chemins, et les tests n'en empruntent qu'un.

### Ce que le chiffre ne dit pas

**Les composables sont à 0 % et le resteront.** Ils ne se testent pas en JVM ;
c'est le rôle des tests d'interface, dont la couverture n'est pas remontée à
Sonar — les deux jobs CI ne partagent pas leur espace de travail.

**Le dénominateur grossit plus vite que le numérateur.** Chaque fonctionnalité
ajoute des lignes non couvertes : la couverture a mécaniquement baissé en
ajoutant Firestore, sans qu'aucun test ne disparaisse.

**Le pourcentage n'est pas un objectif.** Six tests bien choisis, qui verrouillent
six régressions réelles, valent mieux que trente tests écrits pour faire monter un
chiffre.

### Porte qualité

`sonar.qualitygate.wait` est **désactivé**. La porte « Sonar way » exige 80 % de
couverture sur le code neuf ; l'activer aujourd'hui ferait échouer tous les
builds.

C'est une dette assumée, pas un oubli. À réactiver quand la couverture aura
progressé, en ajustant le seuil si nécessaire.

---

## Mesures Android Profiler

Deux fuites mémoire ont été mesurées avant et après correction, avec **la même
séquence d'utilisation** des deux côtés : ajout, ouverture d'un détail, retour,
deux bascules d'écran, changement d'onglet, ajout, détail, retour.

### La métrique retenue

Le **nombre d'instances retenues** dans le heap dump, et non la pente du graphe
de mémoire.

Un comptage exact — « `MainActivity` : 3 instances vivantes alors qu'une seule
devrait exister » — est vérifiable et se lit sans interprétation. Une pente
dépend de l'échelle, du moment de la capture et de l'œil du lecteur.

### Ce que montrait l'ancien code

Sur l'enregistrement de consommation, la ligne des objets alloués **redescendait
à chaque passage du ramasse-miettes, mais jamais au niveau précédent**. C'est la
signature d'une fuite : une application saine revient à son plancher.

Après correction, le plancher est plat et le nombre d'instances retenues revient
à un.

---

## Analyse statique

### Lint

Exécuté à chaque build de CI, il fait échouer le job sur les erreurs.

Il a détecté deux défauts de sécurité que la relecture avait manqués :

- un `registerReceiver` sans flag d'export sur la branche pré-Tiramisu ;
- un broadcast implicite vers un composant non exporté, donc **visible des autres
  applications du téléphone**.

Les deux ont été corrigés avec T-06.

### SonarCloud

Analyse déclenchée à chaque run, avec `fetch-depth: 0` — le scanner a besoin de
l'historique git complet pour attribuer les lignes.

Sur une pull request, Sonar ne mesure que le **code neuf**. C'est ce qui répond à
la question utile — « ce que j'ajoute est-il testé ? » — mais pas à « où en est le
projet ? », qui se lit sur le tableau de bord de `main`.
