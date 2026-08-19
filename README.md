# StockMed — Rebonnte

[![Android CI](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions/workflows/android-ci.yml/badge.svg)](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions/workflows/android-ci.yml)
[![Distribution](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions/workflows/release.yml/badge.svg)](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions/workflows/release.yml)
[![Documentation](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions/workflows/docs.yml/badge.svg)](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions/workflows/docs.yml)
[![Quality gate](https://sonarcloud.io/api/project_badges/measure?project=jacqueline-raynaud_StockMed-Rebonnte&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jacqueline-raynaud_StockMed-Rebonnte)

Application Android de gestion de stock de médicaments, pour les équipes d'un
entrepôt pharmaceutique. Chaque mouvement de stock est **tracé** : qui, quand,
de combien à combien.

Projet OpenClassrooms repris en cours de route, dans un état volontairement
dégradé : plantages, fuites mémoire, historique jamais enregistré, données
créées au hasard. Le travail a consisté à diagnostiquer, corriger, et
reconstruire l'architecture — le détail est dans la documentation.

## 📚 Documentation

Le site du projet documente le contexte, l'analyse de l'existant, les 42 tâches
réalisées avec leur justification, les décisions d'architecture, les parcours
utilisateur et les mesures de qualité.

> **Site publié** : <https://jacqueline-raynaud.github.io/StockMed-Rebonnte/>

Sources dans [`docs/`](docs/), construites avec MkDocs Material :

```bash
mkdocs serve
```

## Fonctionnalités

| | |
|---|---|
| **Compte** | Création, connexion, déconnexion, suppression du compte (avec ré-authentification) |
| **Écran d'accueil** | Rappelle à chaque démarrage sous quel compte les mouvements seront enregistrés — les téléphones sont partagés |
| **Emplacements** | Trois emplacements réels amorcés au premier lancement (standard, froid, sécurisé), création de nouveaux, noms uniques |
| **Médicaments** | Création, correction du nom et de l'emplacement, suppression avec confirmation |
| **Recherche et tri** | Recherche par préfixe et tri sur le nom ou le stock, **exécutés par la base** |
| **Mouvements de stock** | Saisie d'une quantité, ajout ou retrait. Un retrait supérieur au stock est **refusé**, pas rabaissé à zéro |
| **Historique** | Par médicament, lu par pages de 20. Il survit à la suppression du médicament |
| **Thème** | Clair, sombre, ou celui du système — le choix est un besoin d'accessibilité, pas une préférence |
| **Hors ligne** | L'application se bloque et l'annonce. Compter du stock sur des chiffres périmés ne vaut rien |

## Architecture

**MVVM**, deux couches, sans dépendance de la couche data vers l'interface.

```
data/
  model/          Dto Firestore (@Keep : mappés par réflexion sur le nom des champs)
  repository/     Interfaces — le contrat
    impl/         Implémentations Firestore
  preferences/    Thème, persisté en SharedPreferences
  network/        Détection réseau
ui/
  <feature>/      Un ViewModel, un UiState par fichier, un écran découpé
                  en XScreen (connaît le ViewModel) et XContent (données et lambdas)
  component/      Composables partagés : barres, listes, dialogues
  navigation/     Routes, graphe de navigation, redirection de session
```

Trois principes qui se voient partout dans le code :

- **Les écrans ne décident de rien.** Ils affichent un état et remontent des
  gestes. Toute la logique est dans les ViewModels et les dépôts — c'est ce qui
  permet de tester les parcours sans émulateur.
- **Les écritures sensibles passent par une transaction**, avec leur trace dans
  la même opération. Un stock modifié sans trace serait exactement l'incohérence
  signalée par le service qualité.
- **Les données ne sont lues qu'au moment où elles servent.** Un emplacement ne
  lit que ses médicaments, une fiche ne lit que les 20 dernières entrées
  d'historique.

## Stack technique

- **Kotlin 2.1**, **Jetpack Compose** (BOM 2024.04.01), Navigation Compose
- **Hilt** pour l'injection de dépendances, **KSP** pour la génération
- **Firebase** Auth et Firestore, avec règles de sécurité versionnées
  (`firestore.rules`)
- **Coroutines** et `Flow` de bout en bout
- **R8** : obfuscation et retrait des ressources inutilisées en release
- Java 17 · `minSdk` 24 · `compileSdk` / `targetSdk` 34

## Qualité

| | |
|---|---|
| Tests unitaires | **90**, sur la JVM, sans émulateur ni réseau |
| Tests instrumentés | **9** parcours, sur doublures en mémoire — aucun n'atteint Firebase |
| Couverture de lignes | **64 %** en fusionnant les deux ; 25 % avec les seuls tests unitaires, les composables n'étant pas exécutables en JVM |
| Analyse statique | SonarCloud et Android Lint, tous deux dans la CI |

Les tests ne cherchent pas la couverture : **chacun correspond à un défaut
réellement rencontré** et échouerait sur le code d'origine. Le détail est dans
[la page Qualité](docs/qualite.md).

## Intégration continue

`android-ci.yml`, sur chaque `push` vers `main` et chaque pull request :

| Job | Contenu | Dépend de |
|---|---|---|
| `build-and-test` | Compilation debug, tests unitaires, lint | — |
| `instrumented-tests` | Tests d'interface sur émulateur API 34 | — |
| `sonar` | Couverture fusionnée et analyse SonarCloud | `instrumented-tests` |

Les deux premiers tournent en parallèle : une compilation cassée remonte en trois
minutes sans attendre l'émulateur. Le troisième attend, parce qu'il consomme la
mesure de couverture produite sur l'appareil — elle voyage d'un job à l'autre en
artefact. Si l'émulateur échoue, l'analyse remonte quand même, avec la seule
couverture unitaire.

Deux autres workflows :

| Workflow | Déclencheur | Rôle |
|---|---|---|
| `docs.yml` | `docs/**` ou `mkdocs.yml` modifiés | Construit le site ; ne le publie que depuis `main` |
| `release.yml` | Tag `v*`, ou manuellement | APK release signé et obfusqué, envoyé sur Firebase App Distribution, avec archivage du `mapping.txt` |

## Configuration pour un fork

Secrets à définir dans `Settings > Secrets and variables > Actions` :

| Secret | Usage |
|---|---|
| `SONAR_TOKEN` | Analyse SonarCloud |
| `KEYSTORE_B64` | Keystore de signature, encodé en Base64 |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Identifiants de signature |
| `FIREBASE_APP_ID`, `FIREBASE_SERVICE_ACCOUNT` | Distribution de l'APK |

Le keystore (`*.jks`, `*.keystore`) et `local.properties` sont ignorés par git.
`google-services.json` est versionné : il ne contient pas de secret, seulement
la configuration client du projet Firebase.

## Base de données (Firestore)

Trois collections. Les utilisateurs ne sont pas stockés ici : ils vivent dans
Firebase Auth, et l'historique ne conserve que leur adresse e-mail.

| Collection | Champs |
|---|---|
| `aisles` | `name` |
| `medicines` | `name`, `nameLowercase`, `stock`, `aisleId` |
| `history` | `medicineId`, `medicineName`, `userEmail`, `date`, `action`, `stockBefore`, `stockAfter`, `details` |

`nameLowercase` ne fait pas partie du modèle : c'est un champ technique qui rend
la recherche et le tri insensibles à la casse. Firestore trie sinon les
majuscules avant les minuscules — « Zovirax » passerait avant « aspirine ».

**Règles de sécurité** (`firestore.rules`) : lecture et écriture réservées aux
utilisateurs authentifiés, suppression d'emplacement interdite, et **journal
d'audit en ajout seul** — `update` et `delete` sont refusés à tout le monde sur
`history`, et `userEmail` doit correspondre au compte appelant. C'est ce qui
distingue un journal d'audit d'un simple log.

## Commandes utiles

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew connectedDebugAndroidTest
```

```bash
./gradlew lintDebug
```

Rapport de couverture, dans `app/build/reports/jacoco/`. Lancé après
`connectedDebugAndroidTest`, il fusionne les deux sources ; seul, il ne couvre
que les tests unitaires.

```bash
./gradlew jacocoTestReport
```

APK release signé, si le keystore est configuré dans `local.properties` :

```bash
./gradlew assembleRelease
```

## Reste à faire

Le backlog est tenu à jour et assumé : accessibilité TalkBack, journal global du
stock, validation des doublons de noms de médicaments, pagination de la liste
principale. Chaque point est détaillé avec sa raison dans
[la page Tâches](docs/taches.md#reste-a-faire).
