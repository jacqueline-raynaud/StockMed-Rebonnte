# Démarrer

## Prérequis

```
Android Studio, JDK 17, un appareil ou émulateur.
```

## Configuration Firebase

```
Créer le projet, activer Email/Password, récupérer `google-services.json`,
déployer les règles de sécurité depuis `firestore.rules`, créer l'index
composite de l'historique : medicineId en croissant, date en décroissant
```

warning "Les règles de sécurité ne sont pas facultatives"

```
Le fichier `google-services.json` est embarqué dans l'APK : n'importe qui
peut l'en extraire. Ce ne sont pas les clés qui protègent les données, ce
sont les règles Firestore.
```

## Compiler et lancer

```
./gradlew assembleDebug
```

## Exécuter les tests

```
./gradlew testDebugUnitTest
```

Tests d'interface, sur un appareil ou un émulateur connecté :

```
./gradlew connectedDebugAndroidTest
```

Rapport de couverture :

```
./gradlew jacocoTestReport
```

## Prévisualiser cette documentation

```
pip install -r docs/requirements.txt
```

```
mkdocs serve
```

Le site est alors servi sur `http://127.0.0.1:8000` et se recharge à chaque  
enregistrement.