# Démarrer

## Prérequis

!!! note "À compléter"

    Android Studio, JDK 17, un appareil ou émulateur.

## Configuration Firebase

!!! note "À compléter"

    Créer le projet, activer Email/Password, récupérer `google-services.json`,
    déployer les règles de sécurité depuis `firestore.rules`, créer l'index
    composite de l'historique : medicineId en croissant, date en décroissant

### L'index composite de l'historique

Firestore ne peut pas servir une requête combinant un filtre d'égalité et un tri
sans index dédié. La consultation de l'historique d'un médicament en réclame un :

| Collection | Champ | Sens |
|---|---|---|
| `history` | `medicineId` | Croissant |
| | `date` | **Décroissant** |

Il ne se crée pas tout seul. Au premier affichage d'une fiche détail, la requête
échoue avec un message contenant un lien : **suivez ce lien** plutôt que de créer
l'index à la main, il encode la requête exacte, sens de tri compris.

La construction prend quelques minutes. Tant que l'index est en état *Building*,
la requête continue d'échouer.

!!! warning "Les règles de sécurité ne sont pas facultatives"

    Le fichier `google-services.json` est embarqué dans l'APK : n'importe qui
    peut l'en extraire. Ce ne sont pas les clés qui protègent les données, ce
    sont les règles Firestore.

## Compiler et lancer

```bash
./gradlew assembleDebug
```

## Exécuter les tests

```bash
./gradlew testDebugUnitTest
```

Tests d'interface, sur un appareil ou un émulateur connecté :

```bash
./gradlew connectedDebugAndroidTest
```

Rapport de couverture :

```bash
./gradlew jacocoTestReport
```

## Prévisualiser cette documentation

```bash
pip install -r docs/requirements.txt
```

```bash
mkdocs serve
```

Le site est alors servi sur `http://127.0.0.1:8000` et se recharge à chaque
enregistrement.
