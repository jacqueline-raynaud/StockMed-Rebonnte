# StockMed - Rebonnte

Application Android de gestion des stocks de médicaments : rayons, médicaments,  
mouvements de stock et historique des modifications.

Ce site documente la **reprise** de l'application. Le code livré au départ était  
fonctionnellement incomplet et comportait plusieurs défauts bloquants ; ces pages  
expliquent ce qui n'allait pas, ce qui a été corrigé, et les  
choix qui ont été faits ainsi.

## Par où commencer

[**Contexte et demandes**](contexte.md)

Ce que l'entreprise attendait, et les retours de l'audit, du service qualité  
et du Product Owner.

[**Analyse de l'existant**](analyse.md)

Les défauts du code livré et leurs causes racines, pièce par pièce.

[**Tâches réalisées**](taches.md)

Le listing des tâches : problème, correction, détail d'implémentation.

[**Décisions d'architecture**](decisions.md)

Les choix structurants et les alternatives écartées.

[**Parcours utilisateur**](parcours.md)

Chaque geste de l'opérateur, relié au code qui l'exécute.

## État du projet

Les défauts bloquants sont corrigés, l'architecture est en place et les données  
sont persistées. Il reste principalement des **fonctionnalités demandées par le**  
**Product Owner** et la finition.

### Le cahier des charges

| Attendu | À la reprise | Aujourd'hui |
| --- | --- | --- |
| Créer un compte | Absent | :material-check: Fait |
| S'identifier | Absent | :material-check: Fait |
| Gérer les rayons | Partiel, en mémoire | :material-check: Emplacements réels : standard, froid, sécurisé |
| Gérer les médicaments | Partiel, en mémoire | :material-check: Création guidée et suppression |
| Gérer le stock | Plantage systématique | :material-check: Fait - saisie par quantité, retrait excessif refusé |
| Historique | Jamais enregistré | :material-check: Fait, fiable |

### Au-delà du cahier des charges

Une revue technique a conduit à un lot de mise en conformité, et trois  
défauts sérieux ont été trouvés en la menant - une application qui mourait sur  
la moindre erreur Firestore, un stock plafonné en silence, un message de succès  
affiché avant l'opération. Le détail est dans le [lot 5](taches.md#lot-5-reprise-apres-revue-technique).

### Les quatre livrables

| Livrable | État |
| --- | --- |
| Capture de l'exécution de la CI | :material-check: Disponible |
| Listing des tâches + Kanban | :material-check: [Page dédiée](taches.md) |
| Capture annotée de l'Android Profiler | :material-check: [Page dédiée](profiler.md) - mesures avant/après |
| Capture de l'APK sur Firebase App Distribution | :material-check: Chaîne opérationnelle |

**Les quatre livrables ont désormais leur support.** Ce qu'il reste tient à des  
captures d'écran et à un export PDF, plus aux durées réelles à renseigner.

### Qualité

| Tests unitaires | 84 |  
| Tests d'interface | 9 |  
| Couverture (indicateur SonarCloud) | 52 % |  
| Intégration continue | Trois workflows : vérification, tests instrumentés, documentation |  
| Livraison continue | APK signé envoyé sur Firebase App Distribution |  
| Règles de sécurité Firestore | Déployées, journal d'audit en ajout seul |

Le détail et les limites de ces chiffres sont dans [Tests, CI et](qualite.md) [mesures](qualite.md).

## Liens

| Ce site | [jacqueline-raynaud.github.io/StockMed-Rebonnte](https://jacqueline-raynaud.github.io/StockMed-Rebonnte/) |  
| Dépôt | [github.com/jacqueline-raynaud/StockMed-Rebonnte](https://github.com/jacqueline-raynaud/StockMed-Rebonnte) |  
| Intégration continue | [github.com/jacqueline-raynaud/StockMed-Rebonnte/actions](https://github.com/jacqueline-raynaud/StockMed-Rebonnte/actions) |  
| Qualité du code | [sonarcloud.io/project/overview?id=jacqueline-raynaud\_StockMed-Rebonnte](https://sonarcloud.io/project/overview?id=jacqueline-raynaud_StockMed-Rebonnte) |  
| Suivi des tâches | [site notion](https://delicate-burn-9be.notion.site/3c17a4d984d68048b7f0def2255d1521?v=3c17a4d984d680c8972d000caadf82b3&source=copy_link) |