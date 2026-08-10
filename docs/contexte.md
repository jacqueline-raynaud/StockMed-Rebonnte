# Contexte et demandes

## Ce que l'application doit permettre

Le cahier des charges tient en six points :

- Créer un compte
- S'identifier avec ce compte
- Gérer les rayons du stock
- Gérer les médicaments présents par rayon
- Gérer le stock de chaque médicament
- Consulter l'historique de chaque changement

## L'état à la reprise

L'équipe précédente était spécialisée en développement web. L'application
compilait et s'ouvrait, mais l'écart avec le cahier des charges était
considérable.

| Attendu | État à la reprise |
|---|---|
| Créer un compte | **Absent** — aucun écran, aucune dépendance d'authentification |
| S'identifier | **Absent** |
| Gérer les rayons | Partiel — ajout d'un rayon « aléatoire » uniquement, en mémoire |
| Gérer les médicaments | Partiel — ajout aléatoire, ni création, ni suppression |
| Gérer le stock | **Cassé** — plantage systématique sur les boutons +1 et -1 |
| Historique | **Cassé** — les entrées n'étaient jamais enregistrées |

Absents également : toute persistance, l'injection de dépendances, les tests,
l'intégration continue et la distribution de l'application. Le dépôt livré ne
contenait aucun commit.

---

## Les retours reçus

Trois sources, aux préoccupations très différentes — et parfois deux
descriptions du même défaut.

### Audit technique

Réalisé par un prestataire externe, avec un angle **green code** et
maintenabilité.

**Consommation et performances**

- Les mises à jour de stock s'exécutent sur le thread principal
- Aucune gestion des closures asynchrones ; des fuites mémoire sont suspectées
  sur une utilisation prolongée, sans avoir été mesurées
- Le tri et le filtrage sont faits en mémoire alors que Firebase sait les
  exécuter côté serveur
- Toutes les données sont chargées d'un coup, sans chargement paresseux
- Éviter les appels réseau inutiles

**Robustesse**

- La gestion des erreurs est inexistante
- Les chargements sont trop peu indiqués à l'utilisateur

**Interface**

- L'application semble compatible avec le mode sombre : veiller à le maintenir

**Maintenabilité**

- Du code est potentiellement dupliqué ; utiliser des composants réutilisables
- **Aucun test**, ni unitaire ni d'intégration — jugé dangereux sur une
  application aussi critique
- L'application a été initiée sur un modèle MVVM, mais celui-ci n'est pas
  respecté dans les faits
- Une intégration continue permettrait de détecter les régressions

Une demande explicite accompagne l'audit : fournir une **capture annotée de
l'Android Profiler** en fin de développement.

### Service qualité

Court, et le plus sévère.

> Toutes les actions effectuées par les utilisateurs ne sont pas systématiquement
> enregistrées dans l'historique.

Conséquence : certaines actions apparaissent de façon sporadique, d'autres sont
totalement absentes. Le service ne peut pas suivre les modifications du stock,
ce qui rend **l'application inutilisable pour leurs besoins**.

!!! info "Ce n'est pas une demande de fonctionnalité"

    Le service qualité ne demandait pas une ligne manquante, mais de pouvoir
    **se fier** à l'historique. C'est ce qui a orienté les décisions de
    [journalisation systématique](decisions.md#lecriture-est-dans-le-repository-pas-chez-lappelant)
    et de [journal en ajout seul](decisions.md#un-journal-en-ajout-seul).

### Product Owner

La liste la plus concrète, orientée usage.

**Fonctionnalités mal pensées**

- Le bouton « + » ajoute un médicament totalement aléatoire ; il devrait ouvrir
  un écran de détail vide à remplir
- Il est impossible de supprimer un médicament du stock

**Ergonomie**

- Les boutons +1/-1 ferment involontairement l'application
- L'historique affiche des informations incomplètes : il manque l'utilisateur, la
  date et le détail de la modification
- L'historique est peu esthétique et relégué en bas de liste ; il devrait être
  intégré au contenu défilant de la fiche détail
- Il affiche un identifiant technique pour l'opérateur, difficilement
  exploitable : afficher au minimum l'adresse e-mail

**Données**

- La gestion des stocks doit être plus robuste, avec des validations de saisie
- Les données disparaissent quand on entre dans les écrans de détail : il faut
  les persister

**Accès et accessibilité**

- Une fois identifié, il faut pouvoir se déconnecter — les téléphones peuvent
  être utilisés par différentes personnes
- L'application manque de support pour TalkBack et les contrastes de couleurs

**Testabilité**

- Mettre en place la production d'un APK et son envoi automatique sur Firebase
  App Distribution, pour pouvoir tester sans dépendre du développeur

**Justification du travail**

- Établir une liste détaillée de tout ce qui a été fait, avec les apports, les
  raisons et le temps passé

---

## Correspondance demandes → tâches

Chaque remarque reçue est reliée à la ou les tâches qui y répondent. Les tâches
non traitées sont signalées comme telles : voir le
[reste à faire](taches.md#reste-a-faire).

### Audit technique

| Remarque | Tâches | État |
|---|---|---|
| Traitements sur le thread principal | T-13 | Fait |
| Closures asynchrones et fuites mémoire | T-06, T-07, T-29 | Fait |
| Tri et filtre à déporter côté serveur | T-22 | Fait |
| Appels réseau inutiles | T-06, T-22 | Fait |
| Code dupliqué | T-33 | Fait |
| MVVM non respecté | T-10, T-12, T-13, T-14 | Fait |
| Absence de tests | T-25, T-26 | Fait |
| Intégration continue | T-27 | Fait |
| Capture Android Profiler | T-29 | Fait |
| Chargement paresseux | T-23 | **À faire** |
| Gestion des erreurs et indicateurs de chargement | T-24 | **À faire** |
| Maintien du mode sombre | T-32 | **À faire** |

### Service qualité

| Remarque | Tâches | État |
|---|---|---|
| Actions non enregistrées dans l'historique | T-05, T-20 | Fait |
| Historique sporadique et non fiable | T-05, T-19, T-20, T-43 | Fait |

### Product Owner

| Remarque | Tâches | État |
|---|---|---|
| Boutons +1/-1 qui ferment l'application | T-04 | Fait |
| Historique incomplet (utilisateur, date, détails) | T-19 | Fait |
| Identifiant technique au lieu de l'e-mail | T-19 | Fait |
| Historique à intégrer dans la fiche détail | T-19 | Fait |
| Données non persistées | T-11 | Fait |
| Déconnexion | T-18 | Fait |
| Liste détaillée du travail effectué | T-30 | Fait |
| Ajout de médicament aléatoire | T-15 | Fait |
| Suppression d'un médicament | T-16 | Fait |
| Validation des saisies | T-21 | Partiel — formulaires de connexion et de création |
| Accessibilité (TalkBack, contrastes) | T-31 | **À faire** |
| APK sur Firebase App Distribution | T-28 | Fait |

### Ajouté en cours de projet

| Constat | Tâche | État |
|---|---|---|
| Retirer 50 boîtes demande 50 clics et produit 50 lignes d'historique | T-44 | Fait |
| Historique consultable seulement médicament par médicament | T-45 | **À faire** |
| Rayons créés au hasard, sans rapport avec les règles de stockage | T-46 | Fait |
| Base de données ouverte sans règles de sécurité | T-43 | Fait |

!!! note "T-44 ne figurait dans aucune note"

    Le défaut est apparu à l'usage : la correction de T-05 rend l'historique
    fiable, mais un mouvement de 50 unités y produit 50 lignes. Le service
    qualité chercherait « qui a retiré 50 boîtes » et trouverait cinquante
    entrées de « -1 ». Une partie du bénéfice de T-20 est annulée tant que la
    saisie se fait clic par clic.
