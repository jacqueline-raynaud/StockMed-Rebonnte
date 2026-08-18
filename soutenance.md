# Soutenance — plan détaillé

Document de préparation, hors site de documentation. 25 minutes : 15 de
présentation, 10 de discussion.

## Le cadrage à garder en tête

L'évaluateur joue le **Product Owner**. Il ne demande pas si votre code est
beau, il demande si son problème est réglé.

Trois conséquences :

- **Parlez opérateur, stock, traçabilité** avant de parler `Flow`, coroutine ou
  transaction. Le vocabulaire technique arrive quand il demande le code.
- **Chaque correction se relie à une plainte.** « Vos boutons +1 fermaient
  l'application » vaut mieux que « j'ai corrigé un index hors bornes ».
- **Il peut contester vos choix.** Le blocage hors ligne est le plus exposé :
  préparez-vous à le défendre (voir plus bas).

---

# Partie 1 — Présentation (15 min)

## 1. Le point de départ (1 min 30)

Trois phrases, pas plus :

> L'application livrée compilait et s'ouvrait. Mais les boutons de stock la
> fermaient systématiquement, l'historique n'enregistrait rien, et il n'y avait
> ni compte, ni identification, ni persistance : tout disparaissait à la
> fermeture.

Puis les trois sources de remarques : l'audit technique, le service qualité, le
Product Owner. **Dites que vous êtes partie de ces trois listes, pas du code.**
C'est déjà la réponse à la troisième question de la discussion.

## 2. Démonstration de l'application (5 min)

Sur le téléphone physique, dans cet ordre. Ne cherchez rien pendant la
démonstration : le stock doit être préparé la veille.

| # | Ce que vous montrez | Ce que vous dites |
|---|---|---|
| 1 | Écran de connexion, puis identification | « Créer un compte et s'identifier : c'était absent, c'est la base du reste — sans compte, l'historique ne peut être signé par personne » |
| 2 | Écran d'accueil qui nomme la session | « Un garde-fou que vous aviez demandé : les téléphones sont partagés. À chaque démarrage on rappelle sous quel compte les mouvements seront enregistrés » |
| 3 | Liste des emplacements | « Les rayons étaient créés au hasard, "Aisle 2", "Aisle 3". Un médicament se range en standard, froid ou sécurisé : ce sont désormais des emplacements réels, amorcés au premier lancement » |
| 4 | Création d'un médicament | « Le bouton + ajoutait un médicament aléatoire. Il ouvre maintenant un formulaire, et l'emplacement se **choisit** dans une liste — on ne peut plus inventer un rayon » |
| 5 | Fiche détail : retrait de 5 unités | « Une quantité se saisit. Retirer cinquante boîtes ne demande plus cinquante appuis, et surtout ne produit plus cinquante lignes d'historique » |
| 6 | **Retrait de 500 unités → refus** | « Avant, le stock tombait à zéro sans rien dire. L'opérateur repartait en croyant avoir sorti 500 boîtes. Maintenant c'est refusé, avec le stock réel, et il faut valider le message » |
| 7 | L'historique de la fiche | « Qui, quand, de combien à combien. C'est ce que le service qualité ne pouvait pas obtenir » |
| 8 | Suppression d'un médicament | « La confirmation rappelle en rouge le stock restant. J'y reviens dans les questions ouvertes » |
| 9 | Menu Apparence → Sombre | « L'audit demandait de maintenir le mode sombre. J'ai ajouté le choix : on ne peut pas imposer un mode sans connaître les besoins visuels de l'opérateur » |
| 10 | **Mode avion → écran de blocage** | « Sans réseau, l'application ne montre rien et ne laisse rien faire. C'est un choix, je l'explique si vous voulez » |
| 11 | Déconnexion | « Demandée aussi : le téléphone passe d'un opérateur à l'autre » |

**Préparation matérielle** : téléphone chargé, application installée en version
release signée, stock réaliste (5 à 8 médicaments, des noms crédibles), session
ouverte mais **déconnectez-vous avant** pour montrer l'identification.

## 3. Le plan de tâches (2 min)

Montrez la page **Tâches réalisées** du site, ou le tableau Kanban.

> 35 tâches réalisées, regroupées en six lots. Chaque remarque de vos trois
> listes est reliée à la tâche qui y répond — il y a un tableau de
> correspondance. Et ce qui n'est pas fait est écrit noir sur blanc, avec les
> raisons.

Insistez trente secondes sur le **reste à faire** : c'est contre-intuitif, mais
un backlog honnête inspire plus confiance qu'une liste toute verte.

## 4. Deux tâches en détail (5 min)

### Tâche A — La fuite mémoire du BroadcastReceiver (T-06)

Choisie parce qu'elle vient de l'audit, qu'elle est mesurée, et qu'elle produit
un livrable.

**Le problème** — montrez le code d'origine :

```kotlin
private fun startBroadcastReceiver() {
    registerReceiver(MyBroadcastReceiver(), IntentFilter("ACTION_UPDATE"))
}
// startMyBroadcast() rappelait startBroadcastReceiver() toutes les 200 ms
```

> Un nouveau receiver toutes les 200 millisecondes, jamais désenregistré.
> Chacun retenait l'écran en mémoire, et le téléphone était réveillé cinq fois
> par seconde pour afficher un message.

**Comment j'ai décidé** — c'est ce qu'ils veulent entendre :

> Trois options : désenregistrer dans `onPause`, passer par un `LifecycleObserver`,
> ou enregistrer une seule fois pour toute la vie de l'écran avec un
> désenregistrement symétrique. J'ai pris la troisième : c'est la plus simple à
> relire, et la symétrie `onCreate`/`onDestroy` se vérifie d'un coup d'œil.

**Le code produit** — `MainActivity.kt`, `registerUpdateReceiver` et
`scheduleUpdateBroadcast` :

```kotlin
ContextCompat.registerReceiver(this, myBroadcastReceiver,
    IntentFilter(ACTION_UPDATE), ContextCompat.RECEIVER_NOT_EXPORTED)
```

Deux points à signaler : `ContextCompat` applique le drapeau d'export sur
toutes les versions d'Android, et le `Handler` a été remplacé par une coroutine
liée au cycle de vie — elle ne peut plus survivre à l'écran détruit.

**La preuve** — la page Profiler : 488 instances retenues avant, aucune fuite
détectée après, 348 000 objets contre 41 000 en fin de séquence.

### Tâche B — Le retrait supérieur au stock (T-21)

Choisie parce qu'elle est **métier**, que le PO la comprend immédiatement, et
que le code tient en dix lignes.

**Le problème** :

```kotlin
val stockAfter = (medicine.stock + delta).coerceAtLeast(0)
```

> Dix boîtes en stock, cinquante demandées : le stock tombait à zéro,
> l'historique disait « de 10 à 0 », et rien ne signalait que quarante unités
> n'existaient pas.

**Comment j'ai décidé** :

> Deux options. Plafonner en signalant, ou refuser. J'ai refusé, parce que sur
> un stock pharmaceutique l'écart entre ce qui est demandé et ce qui est fait
> doit remonter, pas disparaître. Un plafonnement laisse une trace fausse dans
> le journal d'audit.

**Le code** — `MedicineRepositoryImpl.updateStock` :

```kotlin
if (stockAfter < 0) {
    return@runTransaction StockChange.Insufficient(medicine.stock)
}
```

Deux choses à dire :

- **Le contrôle est dans la transaction, pas dans l'écran** : c'est le seul
  endroit qui lit le stock réel au moment d'écrire. Un contrôle sur la valeur
  affichée travaillerait sur un chiffre peut-être périmé, si un collègue vient
  de servir le même médicament.
- **Le refus est traduit hors de la transaction** : une exception levée dedans
  serait enveloppée par Firestore et perdrait sa raison.

**Le défaut trouvé en chemin** — si vous avez le temps, c'est le meilleur
moment de la présentation :

> En testant, j'ai vu le message « 50 unités retirées » s'afficher **avant** le
> refus. L'écran confirmait sans attendre le résultat. La confirmation vient
> maintenant du ViewModel, une fois l'écriture faite. Un test verrouille ce
> comportement : un mouvement refusé ne produit aucune confirmation.

## 5. L'intégration continue (2 min 30)

Montrez l'onglet Actions de GitHub, une exécution verte.

| Workflow | Ce qu'il fait |
|---|---|
| Vérification | Compilation, 72 tests unitaires, lint, couverture, analyse SonarCloud |
| Tests instrumentés | 9 parcours sur émulateur |
| Documentation | Publication du site |
| Distribution | Sur tag : APK signé, obfusqué, envoyé sur App Distribution |

Trois phrases qui font la différence :

> La chaîne tourne sur chaque proposition de fusion, donc une régression est
> détectée avant d'arriver sur la branche principale.

> Les tests instrumentés utilisent des doublures en mémoire : ils ne touchent
> jamais Firebase, sinon ils seraient lents et créeraient de vrais comptes.

> Et à chaque distribution, le fichier de correspondance de l'obfuscation est
> archivé — sans lui, un plantage remonté par un testeur serait illisible.

Enchaînez sur **App Distribution** : le tag, l'APK signé, les testeurs qui
reçoivent la version sans passer par vous — c'était une demande explicite.

---

# Partie 2 — Discussion (10 min)

## Q1. Comment avez-vous évalué les solutions possibles ?

**La méthode, en une phrase :**

> Pour chaque tâche, j'ai listé les options, je les ai jugées sur trois
> critères — le risque pour la traçabilité du stock, le coût, et la
> réversibilité — et j'ai écrit la décision **au moment de la prendre**, avec
> les alternatives écartées.

Puis montrez la page **Décisions d'architecture** : treize décisions, chacune
avec ce qui a été écarté et pourquoi.

**Trois exemples à avoir en tête :**

| Décision | Écarté | Pourquoi |
|---|---|---|
| L'historique est écrit par le dépôt, pas par l'appelant | Laisser chaque appelant journaliser | Tant qu'un appelant devait y penser, il finissait par oublier — c'est l'origine des manques signalés par le service qualité |
| Refuser le retrait excessif | Plafonner à zéro | Un plafonnement laisse une trace fausse dans le journal |
| Bloquer hors ligne | Laisser travailler en cache | Les transactions ne fonctionnent pas hors ligne, et un comptage sur des chiffres périmés produit un écart inexplicable |

Si on vous pousse : **la décision la plus coûteuse** a été les interfaces de
dépôt avec deux implémentations. Elle s'est remboursée deux fois — le passage à
Firestore a demandé trois lignes, et les implémentations en mémoire servent
aujourd'hui de doublures de test.

## Q2. Les exigences du Greencode

L'audit demandait : moins de données transférées, pas d'appels inutiles, pas de
fuites, du chargement paresseux.

**Ce qui a été fait :**

| Mesure | Effet |
|---|---|
| Tri et filtre **côté serveur** | Seuls les documents utiles descendent, au lieu de tout charger pour filtrer sur le téléphone |
| Les deux fuites mémoire corrigées | Plus de réveil du processeur cinq fois par seconde, plus d'écrans retenus en mémoire |
| Les écouteurs s'arrêtent quand l'écran n'est plus observé | `WhileSubscribed` : une liste qu'on ne regarde plus cesse de consommer du réseau |
| Les écouteurs sont annulés à la déconnexion | Plus aucun trafic pour un utilisateur parti |
| Un mouvement = une écriture | Retirer 50 boîtes produisait 50 écritures et 50 lignes ; c'est une seule opération |
| `key` et `contentType` sur les listes | Les lignes sont réutilisées au défilement au lieu d'être reconstruites |
| Obfuscation et retrait des ressources inutilisées | APK de 2,86 Mo — moins de téléchargement, moins de stockage |

**Ce qui n'a pas été fait, et dites-le :**

> Le chargement paresseux des listes n'est pas implémenté. Sur un stock de
> quelques centaines de médicaments, la requête reste raisonnable ; sur
> plusieurs milliers, il faudra paginer. C'est identifié dans le reste à faire.

Une phrase qui porte, si l'occasion se présente :

> Le geste le plus écologique du projet, c'est la correction de la fuite : une
> application qui réveille le processeur cinq fois par seconde vide une batterie
> en une journée, quel que soit le soin apporté au reste.

## Q3. Comment vous êtes-vous assurée d'avoir un plan complet ?

**La méthode :**

> Je ne suis pas partie du code, mais des trois sources de demandes. Chaque
> remarque de l'audit, du service qualité et de la vôtre est reliée dans un
> tableau à la tâche qui y répond, avec son état. Une remarque sans tâche en
> face aurait sauté aux yeux.

Montrez les tableaux de correspondance de la page **Contexte et demandes**.

**Puis la deuxième moitié de la réponse, celle qui compte :**

> Un plan complet ne suffit pas : il faut qu'il reste vrai. J'ai ajouté des
> tâches en cours de route, à chaque défaut découvert en utilisant
> l'application — le retrait excessif, l'application qui se fermait sur une
> erreur réseau, le message de confirmation affiché avant l'opération. Aucun ne
> figurait dans les listes de départ.

**Et la troisième :**

> Trois points ne relèvent pas de moi : la politique RGPD sur l'historique après
> suppression d'un compte, la suppression d'un médicament encore en stock, et la
> portée exacte du journal. Ils sont écrits comme questions ouvertes, avec les
> options et leurs conséquences. Je préfère vous les poser plutôt que de
> trancher à votre place.

C'est la meilleure note possible pour finir : vous rendez la main au PO.

---

# Questions probables, et réponses courtes

**« Pourquoi bloquer l'application hors ligne ? Mes opérateurs doivent
travailler. »** — Le plus probable, et le plus légitime.

> Deux raisons. Les mouvements de stock passent par des transactions, qui ne
> fonctionnent pas sans serveur : les boutons promettraient des opérations qui
> n'auraient pas lieu. Et un comptage fait sur des chiffres périmés produit un
> écart d'inventaire que personne ne sait ensuite expliquer. Si vous préférez un
> mode consultation en lecture seule, c'est un réglage, pas une réécriture — le
> blocage passe par un seul point du code.

**« Seulement 16 % de couverture ? »**

> C'est la couverture des tests unitaires seuls. Les 9 tests de parcours ne sont
> pas comptabilisés par l'outil, et ce sont eux qui traversent les écrans. Les
> tests unitaires couvrent en priorité les dépôts et les ViewModels, là où sont
> les décisions. Je n'ai pas cherché le chiffre : chaque test correspond à un
> défaut réel et échouerait sur le code d'origine.

**« Pourquoi Firestore ? »**

> Il était déjà dans le projet, et il apporte la synchronisation temps réel et
> les règles de sécurité. Mais l'application ne le sait pas : les écrans passent
> par des interfaces. Changer de base demanderait de réécrire trois classes,
> sans toucher aux écrans ni aux tests.

**« Comment garantissez-vous que l'historique est fiable ? »**

> Trois mécanismes. L'écriture de la trace fait partie de l'opération, dans la
> même transaction — on ne peut pas modifier un stock sans laisser de trace. Les
> règles de sécurité interdisent la modification et la suppression des entrées.
> Et l'adresse enregistrée doit correspondre à celle du compte connecté, la
> règle le vérifie.

**« Que reste-t-il à faire ? »** — Ayez la liste courte en tête : accessibilité
TalkBack, chargement paresseux, journal global du stock, validation des
doublons de noms.

---

# Logistique

**À ouvrir avant de commencer :**

- le téléphone, application installée, déconnectée
- Android Studio sur `MainActivity.kt` et `MedicineRepositoryImpl.kt`
- l'onglet Actions de GitHub, sur une exécution verte
- le site de documentation, page Tâches
- Firebase App Distribution

**Plan B si le réseau tombe** : des captures d'écran de tout ce qui est en
ligne — CI verte, App Distribution, le site. Une démonstration d'application qui
exige le réseau, sans réseau, c'est le scénario à ne pas découvrir en direct.

**Répétez la démonstration deux fois en entier, chronomètre en main.** Les cinq
minutes passent très vite, et c'est la partie qu'un jury juge le plus
sévèrement quand elle patine.
