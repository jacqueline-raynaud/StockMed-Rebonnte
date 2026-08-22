# Tests, CI et mesures

info "Chiffres relevés au moment de la rédaction"

```
Les valeurs de couverture évoluent à chaque commit. Les tableaux de bord
liés depuis l'[accueil](index.md#liens) font foi.
```

---

## Intégration continue

Trois jobs GitHub Actions, déclenchés sur chaque `push` vers `main` et sur chaque pull request.

| Job | Contenu | Dépend de | Durée |
| --- | --- | --- | --- |
| `build-and-test` | Compilation debug, tests unitaires, lint | \- | ~3 min |
| `instrumented-tests` | Tests d'interface sur émulateur API 34 | \- | ~6 min |
| `sonar` | Couverture fusionnée et analyse SonarCloud | `instrumented-tests` | ~2 min |

### Pourquoi ce découpage

**Les deux premiers ne dépendent de rien** et tournent en parallèle. L'erreur courante - compilation cassée, test unitaire rouge - remonte en trois minutes, sans attendre l'émulateur.

**Le troisième attend l'émulateur parce qu'il consomme sa mesure.** La couverture d'interface ne peut être relevée que sur un appareil : ce fichier `.ec` est la seule chose que le job d'analyse ne sait pas produire lui-même, et il voyage donc en artefact d'un job à l'autre. Le reste - classes compilées, `.exec` des tests unitaires - est moins coûteux à refaire qu'à transporter.

### Pourquoi les deux types de test

| Couverture de lignes |   |
| --- | --- |
| Tests unitaires seuls | **16.9 %** |
| Fusionnée avec les tests d'interface | **52,3 %** |

La raison est structurelle : les composables représentent l'essentiel des lignes du projet et **aucun test JVM ne peut les exécuter**. Sans les tests d'interface, le tableau de bord ne mesure que la moitié mesurable en JVM, et le chiffre affiché se lit à tort comme « le projet est peu testé ».

### Les autres workflows

| Workflow | Déclencheur | Rôle |
| --- | --- | --- |
| Documentation | `docs/**` ou `mkdocs.yml` modifiés | Construit le site ; ne le publie que depuis `main` |
| Distribution de l'APK | Tag `v*` ou manuel | Construit un APK de release signé et l'envoie sur Firebase App Distribution |

### Ordonnancement

Les étapes sont volontairement distinctes plutôt que regroupées en une seule invocation Gradle : sur la page d'un run, on voit immédiatement **laquelle** a échoué. C'est aussi ce qui rend la capture d'écran de la CI lisible.

`jacocoTestReport` s'exécute juste avant `sonar`, dans la même invocation : le scanner lit le XML de couverture produit par cette tâche.

---

## Difficultés rencontrées

Elles sont documentées ici parce qu'elles reviendront - et parce que trois d'entre elles ont coûté plus de temps que le code qu'elles bloquaient.

### Conflit de classpath entre AGP et le plugin Sonar

**Symptôme.** `Execution failed for task ':app:sonar'` suivi d'une signature de méthode, sans autre explication.

**Cause.** Le scanner Sonar appelle une méthode de `commons-compress` introduite en 1.24. AGP apporte la 1.21 sur le classpath du projet racine, qui est le **classloader parent** de celui du module `:app`. Par délégation parent-first, la 1.21 masque la version du plugin.

**Correctif.** Un `resolutionStrategy.force` sur le classpath racine. La piste consistant à déclarer le plugin Sonar à la racine a été essayée et abandonnée : elle déplace le problème sur BouncyCastle. Voir [Décisions](decisions.md#commons-compress-force-sur-le-classpath-racine).

### Métadonnées Kotlin trop récentes dans Firebase

**Symptôme.** `kspDebugKotlin` échoue sans message d'erreur visible.

**Cause.** `firebase-auth` 24.x, tiré du BOM 34.x, est compilé avec des métadonnées Kotlin 2.3 que le compilateur 2.1 du projet ne sait pas lire.

**Correctif.** BOM Firebase figé en 33.x. Monter Kotlin entraînerait le plugin Compose Compiler, KSP et Hilt avec lui.


### Espresso incompatible avec les versions récentes d'Android

**Symptôme.** Les sept tests d'interface échouent sur `NoSuchMethodException: android.hardware.input.InputManager.getInstance`.

**Cause.** Espresso 3.6.1 appelle cette méthode par réflexion ; elle n'existe plus sur les Android récents.

**Correctif.** Espresso 3.7.0 et `androidx.test.ext:junit` 1.3.0.

### Retour arrière Espresso instable en CI

**Symptôme.** Trois tests verts en local et sur un run, rouges sur le suivant : `RootViewWithoutFocusException ... has-window-focus=false`.

**Cause.** Espresso exige que la fenêtre ait le focus avant d'injecter un événement. Sur un émulateur de CI, ce focus arrive parfois après son délai de dix secondes. Les cinq tests purement Compose passent - seuls ceux utilisant `pressBack()` échouaient.

**Correctif.** Le retour passe désormais par `onBackPressedDispatcher`, qui emprunte le même chemin que le bouton système sans dépendre du focus. Détail dans [Décisions](decisions.md#le-retour-arriere-passe-par-le-dispatcher-pas-par-espresso).


### Secrets de signature jamais validés

**Symptôme.** Deux échecs successifs du workflow de distribution : `No key with alias ... found in keystore`, puis `Get Key failed: Given final block not properly padded`.

**Cause.** Le workflow dont celui-ci est adapté écrivait le mot de passe du magasin dans les deux champs de signature, alors qu'un secret `KEY_PASSWORD` distinct était déclaré. Ce secret existait, semblait correct - et n'était jamais lu. 

**Correctif.** Utiliser le bon secret pour le bon champ, et corriger sa valeur.


### Couverture bloquée à 0 %

**Symptôme.** SonarCloud affiche 0 % alors que les tests passent.

**Deux causes successives.**

1.  `enableUnitTestCoverage` n'était pas activé dans le type de build `debug` : `testDebugUnitTest` ne produisait aucun fichier `.exec`, donc le rapport JaCoCo était vide quelles que soient les corrections de chemins.
2.  Une fois corrigé, la couverture restait à 0 % **sur les pull requests** - c'est normal : une PR ne mesure que le code neuf, et celle qui branchait JaCoCo ne modifiait aucune ligne de Kotlin.

---

## Tests unitaires

**90 tests** sur les repositories et les cinq `ViewModel`, exécutés sur la JVM, sans émulateur ni réseau.

| Classe testée | Tests |
| --- | --- |
| `FakeMedicineRepository` | 21 |
| `MedicineViewModel` | 14 |
| `MainViewModel` | 14 |
| `AuthViewModel` | 11 |
| `AisleViewModel` | 10 |
| `MedicineFormViewModel` | 10 |
| `MedicineViewModel` - chemins d'échec | 10 |

### Ce qu'ils verrouillent

Ce ne sont pas des tests de couverture : **chacun correspond à un défaut réellement rencontré**, et échouerait sur le code d'origine.

| Test | Défaut verrouillé |
| --- | --- |
| `updateStock updates the requested medicine and leaves the others untouched` | L'index hors bornes |
| `updateStock records the operation with its before and after values` | L'historique ajouté à une copie jetée |
| `clearing the search restores the full list` | Le filtre qui écrasait la source de vérité |
| `deleting a medicine keeps its history` | La trace qui disparaissait avec le médicament |
| `a stock change is signed with the signed in operator` | L'identifiant technique codé en dur |
| `signing out resets the welcome acknowledgement` | Le garde-fou des téléphones partagés |
| `a failed stock movement is reported without crashing` | L'exception Firestore qui tuait le processus |
| `a removal larger than the stock is refused` | Le plafonnement silencieux à zéro |
| `a refused movement produces no confirmation` | Le message de succès affiché avant l'opération |
| `losing the network switches the application to offline` | Le stock périmé pris pour un stock réel |
| `an aisle only exposes its own medicines` | Tout le stock téléchargé pour afficher un emplacement |
| `the history window resets on the next card` | La profondeur d'historique héritée d'une fiche à l'autre |

### Un double d'essai qui échoue exprès

Les implémentations en mémoire ne tombent jamais en panne - c'est leur intérêt, et c'est aussi pourquoi elles n'ont jamais révélé que l'application mourait sur une erreur Firestore.

`FailingMedicineRepository` échoue toujours, avec la raison demandée. Les neuf tests qu'il porte ne rateraient pas une assertion sur la version précédente : ils **planteraient**.

Même logique pour `FakeNetworkMonitor` : aucun test unitaire ne peut couper le wifi, d'où l'interface `NetworkMonitor`.

### Deux pièges de mise en place

`**Dispatchers.Main**` **n'existe pas sur la JVM.** Tout `ViewModel` utilisant `viewModelScope` échoue sans une règle qui le remplace par un dispatcher de test.

**Un** `**StateFlow**` **en** `**WhileSubscribed**` **n'émet pas sans collecteur.** Les premiers tests assertaient sur la valeur initiale - ils passaient en ne vérifiant rien. Il faut lancer un collecteur _et_ faire tourner le test sur le même dispatcher, sans quoi le collecteur ne démarre jamais.


## Tests d'interface

**9 tests de parcours**, exécutés sur émulateur.

| Test | Ce qu'il vérifie |
| --- | --- |
| `withoutSession_theSignInScreenIsShown` | L'accès est verrouillé sans session |
| `withSession_theWelcomeScreenIsShownAndItsButtonsRespond` | Le démarrage avec session ouverte, et **que l'interface répond** |
| `signingOutFromTheWelcomeScreen_returnsToSignIn` | Déconnexion depuis l'accueil |
| `signingOutFromTheStock_returnsToSignIn` | Déconnexion depuis la barre supérieure |
| `backFromTheStockRoot_closesTheApp` | Le retour ferme l'application au lieu d'un écran noir |
| `backFromTheSignInScreen_closesTheApp` | Idem depuis l'écran de connexion |
| `backFromAMedicineDetail_returnsToTheList` | Le gestionnaire de retour n'est pas trop large |

Deux d'entre eux verrouillent des bugs trouvés à la main pendant les tests manuels : l'écran noir après déconnexion et le blocage de l'écran d'accueil.

### Montage

Hilt ne fonctionne pas en test instrumenté sans un runner qui substitue `HiltTestApplication` à l'application réelle. Un module annoté `@TestInstallIn` remplace les liaisons de repositories par des doubles : **aucun test n'atteint Firebase**, ce serait lent, dépendant du réseau, et créerait de vrais comptes.

L'`Activity` est lancée à la main plutôt que par une règle, pour pouvoir installer une session **avant** son démarrage - c'est ce qui permet de reproduire le cas « application relancée avec session ouverte » sans tuer le processus.

---


## Porte qualité

`sonar.qualitygate.wait` est **désactivé**. La porte « Sonar way » exige 80 % de couverture sur le code neuf ; l'activer aujourd'hui ferait échouer tous les builds.

C'est une dette assumée, pas un oubli. À réactiver quand la couverture aura progressé. Le seuil n'est pas pmodifiable sur une version non payante.

---

## Mesures Android Profiler

Deux fuites mémoire ont été mesurées avant et après correction, avec **la même séquence d'utilisation** des deux côtés : ajout, ouverture d'un détail, retour, deux bascules d'écran, changement d'onglet, ajout, détail, retour.

### La métrique retenue

Le **nombre d'instances retenues** dans le heap dump, et non la pente du graphe de mémoire.

Un comptage exact - « `MainActivity` : 3 instances vivantes alors qu'une seule devrait exister » - est vérifiable et se lit sans interprétation. Une pente dépend de l'échelle, du moment de la capture et de l'œil du lecteur.

### Ce que montrait l'ancien code

Sur l'enregistrement de consommation, la ligne des objets alloués **redescendait à chaque passage du ramasse-miettes, mais jamais au niveau précédent**. C'est la signature d'une fuite : une application saine revient à son plancher.

Après correction, le plancher est plat et le nombre d'instances retenues revient à un.

---

## Analyse statique

### Lint

Exécuté à chaque build de CI, il fait échouer le job sur les erreurs.

Il a détecté deux défauts de sécurité que la relecture avait manqués :

*   un `registerReceiver` sans flag d'export sur la branche pré-Tiramisu ;
*   un broadcast implicite vers un composant non exporté, donc **visible des autres applications du téléphone**.

Les deux ont été corrigés avec T-06.

### SonarCloud

Analyse déclenchée à chaque run, avec `fetch-depth: 0` - le scanner a besoin de l'historique git complet pour attribuer les lignes.
