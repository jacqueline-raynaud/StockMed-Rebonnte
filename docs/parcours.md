# Parcours utilisateur, du geste au code

Ce que fait l'opérateur, et ce que le code exécute à chaque étape. Trois
parcours, ceux du cahier des charges.

!!! warning "Les numéros de ligne vieillissent"

    Ils correspondent au commit **f58ace0** (13/08/2026). Un ajout de dix
    lignes en haut d'un fichier les décale tous sans que rien ne le signale.
    Les noms de fonctions, eux, restent justes : en cas de doute, cherchez le
    nom plutôt que la ligne.

---

## Parcours connexion

### Créer un compte

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | L'application démarre sur l'écran de connexion, aucune session ouverte | `MyApp` — `MainActivity.kt:177` ; route `AUTH` — `Destinations.kt:14` |
| 2 | Il touche « Créer un compte » en bas de l'écran | `toggleMode()` — `AuthViewModel.kt:55`. Un champ Nom apparaît |
| 3 | Il saisit nom, adresse e-mail, mot de passe | `onDisplayNameChange` :52, `onEmailChange` :46, `onPasswordChange` :49. Chaque frappe efface l'erreur du champ concerné |
| 4 | Il touche « Créer le compte » | `submit()` — `AuthViewModel.kt:66` |
| 5 | La validation locale s'exécute d'abord | Toujours dans `submit()` : e-mail vide ou sans `@`, mot de passe sous six caractères, nom vide. **Aucun appel réseau si un champ est fautif** |
| 6 | L'appel part vers Firebase | `signUp` — `UserRepositoryImpl.kt:41` : création du compte, `updateProfile` pour le nom, puis `reload` pour que l'instance locale le reflète |
| 7 | En cas d'échec | `messageFor` :122 traduit « already in use » en message lisible. Les libellés bruts de Firebase sont en anglais et parlent de « credential » |
| 8 | En cas de succès | Rien n'est fait ici : `currentUser` change, et l'effet de navigation `MainActivity.kt:224` redirige |

L'écran est découpé en deux : `AuthScreen` (`AuthScreen.kt:34`) connaît le
ViewModel, `AuthContent` (:52) ne reçoit que des données et des lambdas — c'est
ce qui le rend prévisualisable et testable sans Hilt.

### S'identifier

Même chemin, deux différences : `submit()` appelle `signIn`
(`UserRepositoryImpl.kt:36`), et le nom n'est pas demandé.

Après succès, l'opérateur arrive sur l'**écran d'accueil** —
`composable(WELCOME)` (`MainActivity.kt:430`), `WelcomeScreen`
(`WelcomeScreen.kt:45`) — qui nomme explicitement la session ouverte. C'est le
garde-fou demandé pour les téléphones partagés.

Il touche « OK, c'est bien moi » (:109) → `acknowledgeWelcome()`
(`MainViewModel.kt:168`), et l'effet de navigation l'envoie sur la liste des
emplacements.

### Se déconnecter

Deux points d'entrée :

- depuis l'accueil, le bouton « Se déconnecter » — `WelcomeScreen.kt:113` ;
- depuis n'importe quel écran de stock, l'icône de la barre supérieure.

Les deux appellent `signOut()` (`MainViewModel.kt:172`), qui remet l'accueil à
« non validé » **puis** délègue à `signOut` (`UserRepositoryImpl.kt:62`). Sans
cette remise à zéro, l'opérateur suivant sauterait l'avertissement.

`currentUser` passe à `null`, et deux mécanismes se déclenchent :

- l'effet de navigation (:224) renvoie sur `AUTH` **en vidant la pile** — le
  bouton retour ne doit pas ramener sur les écrans de stock ;
- `whileSignedIn` (`ui/SessionGate.kt`) annule les écouteurs Firestore. Sans
  lui, ils recevraient un refus de permission et l'application se fermerait.

### Supprimer le compte

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | Il touche « Supprimer mon compte », en rouge et à l'écart des deux actions courantes | `WelcomeScreen.kt:120` |
| 2 | Une fenêtre s'ouvre. Elle avertit que **l'historique restera signé de son adresse**, et demande le mot de passe | `DeleteAccountDialog` :153 |
| 3 | Il valide | :193 → `deleteAccount(password)` — `MainViewModel.kt:131` |
| 4 | Ré-authentification, puis suppression | `UserRepositoryImpl.kt:76`. Firebase refuse `delete()` sans connexion récente : le mot de passe lève la contrainte et sert de confirmation |
| 5 | Mot de passe faux | L'erreur rouvre la fenêtre (:60) plutôt que de la fermer : la saisie n'est pas perdue |
| 6 | Succès | La session se ferme, le même effet de navigation ramène sur `AUTH` |

L'avertissement est affiché **avant** la validation, pas après. Voir la
[question ouverte](taches.md#rgpd) sur le sort de cette donnée personnelle.

---

## Parcours emplacements de stockage

### Créer un emplacement

1. Sur la liste, il touche le bouton flottant — `MainActivity.kt:397`. La route
   courante décide de l'action : `AISLE_LIST` déclenche
   `showAddAisleDialog = true` (:403).
2. La fenêtre s'affiche — appel :296, définition `AddAisleDialog.kt:22`. Le
   bouton « Créer » reste inactif tant que le nom est vide.
3. Il valide : `onConfirm(name)` (:42) → `addAisle` (`AisleViewModel.kt:59`) →
   `addAisle` (`AisleRepositoryImpl.kt:48`).
4. La liste se met à jour seule : `observeAisles` (:22) est un flux, et
   `uiState` (`AisleViewModel.kt:43`) réémet.

Cet écran remplace l'ancien bouton qui fabriquait « Aisle 2 », « Aisle 3 » : un
emplacement porte un nom choisi.

### Ouvrir un emplacement

`AisleItem` (`AisleScreen.kt:94`) remonte le clic à :85, qui appelle
`Destinations.aisleDetail(id)` (:30) — la route `aisle/{id}` — et le NavHost
sélectionne `composable(AISLE_DETAIL)` (`MainActivity.kt:457`).

`AisleDetailScreen` (`AisleDetailScreen.kt:19`) n'a **aucune composable
propre** : il restreint la liste des médicaments à l'emplacement et réutilise
`MedicineContent` (:33). Même ligne, même mise en page, seul le contenu change.

### Ouvrir un médicament depuis un emplacement

C'est la même composable que la liste principale : `MedicineContent`
(`MedicineScreen.kt:41`), clic à :65 → `Destinations.medicineDetail(id)` (:32)
→ `composable(MEDICINE_DETAIL)` (`MainActivity.kt:472`).

---

## Parcours médicaments

### Créer un médicament

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | Il touche le bouton flottant depuis la liste des médicaments | `MainActivity.kt:401` → route `MEDICINE_NEW`, déclarée **avant** `medicine/{id}` (:466) sans quoi « new » serait pris pour un identifiant |
| 2 | Il saisit le nom | `onNameChange` — `MedicineFormViewModel.kt:65` |
| 3 | Il choisit l'emplacement dans une liste déroulante | :117 → `onAisleChange` :71. **On choisit parmi ce qui existe, on ne saisit pas librement** |
| 4 | Il saisit la quantité initiale | `onStockChange` :68 — les caractères non numériques sont filtrés à la frappe |
| 5 | Il touche « Créer le médicament » | :142 → `submit()` :74 : validation des trois champs, puis `addMedicine` — `MedicineRepositoryImpl.kt:110` |
| 6 | Le médicament et sa trace de création partent dans **le même lot** | Toujours :110. Aucun des deux ne peut exister sans l'autre |
| 7 | Retour automatique à la liste | `LaunchedEffect(state.isSaved)` — `MedicineFormScreen.kt:46` |
| 8 | Si l'enregistrement échoue | Le message s'affiche **sous le bouton** et la saisie est conservée : un message éphémère disparaîtrait pendant que l'opérateur retape |

Cet écran remplace le bouton « + » qui ajoutait un médicament au nom et au stock
aléatoires, dans un emplacement tiré au hasard.

### Ouvrir un médicament

`MedicineDetailScreen` (`MedicineDetailScreen.kt:67`) appelle `observeDetail`
(`MedicineViewModel.kt:148`), qui combine trois sources : le médicament
(`MedicineRepositoryImpl.kt:81`), son historique (:92), et les libellés
d'emplacement — le document ne porte qu'un identifiant de rayon, le libellé se
résout dans le ViewModel.

L'affichage se fait par `MedicineDetailContent` (:144), les trois valeurs en
lecture par `ReadOnlyField` (:284), l'historique par `HistoryItem` (:334).

L'historique remonte en tête à chaque nouvelle entrée : sans cela, un opérateur
qui a fait défiler la liste ne verrait pas le mouvement qu'il vient de faire.

### Diminuer la quantité

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | Il saisit une quantité | Le champ est vide au départ, ce qui laisse les deux boutons **inactifs** : un doigt qui traîne ne peut pas produire un mouvement |
| 2 | Il touche « Retirer » | `MedicineDetailScreen.kt:220` → :133 → `updateStock` — `MedicineViewModel.kt:190` |
| 3 | Une transaction s'exécute | `MedicineRepositoryImpl.kt:149` : relecture du stock réel, refus si le retrait le dépasse, sinon écriture **du stock et de sa trace ensemble** |
| 4 | Si le mouvement est accepté | Confirmation par `LaunchedEffect(confirmation)` (:98). Le champ n'est vidé **qu'ici**, une fois l'écriture faite |
| 5 | Si le retrait dépasse le stock | Fenêtre à valider — `ActionErrorDialog` (`MainActivity.kt:278`) — indiquant la quantité disponible. La saisie est conservée |

Deux points qui se voient mal dans le code mais comptent à l'usage :

**Le contrôle est dans la transaction, pas dans l'écran.** C'est le seul endroit
qui lit le stock réel au moment de l'écriture ; un contrôle sur la valeur
affichée travaillerait sur un chiffre peut-être périmé.

**Un mouvement de cinquante unités produit une seule entrée d'historique.** Le
service qualité cherchant « qui a retiré 50 boîtes » ne trouve pas cinquante
lignes de « -1 ».

### Supprimer un médicament

1. Il touche « Supprimer ce médicament » — `MedicineDetailScreen.kt:242`.
2. `DeleteMedicineDialog` (:300) rappelle **en rouge le nombre d'unités
   restantes** si le stock n'est pas nul. La décision se prend en connaissance
   de cause.
3. Il valide → :135 → `deleteMedicine` (`MedicineViewModel.kt:210`) →
   `MedicineRepositoryImpl.kt:204` : suppression et trace dans la même
   transaction. **L'historique survit au médicament supprimé** — c'est
   l'opération que le service qualité a le plus besoin de retrouver.
4. `onDeleted` renvoie à la liste par `navigateUp`.

---

## Ce que ces parcours ont en commun

Trois traits reviennent dans les trois parcours, et ce sont des choix, pas des
hasards :

**Les écrans ne décident de rien.** Ils affichent un état et remontent des
gestes. Toute la logique est dans les ViewModels et les dépôts — c'est ce qui
permet de tester ces parcours sans émulateur.

**Les écritures sensibles passent par une transaction**, avec leur trace dans la
même opération. Un stock modifié sans trace serait exactement l'incohérence
signalée par le service qualité.

**Un refus se valide, une réussite s'annonce.** Les échecs ouvrent une fenêtre
qu'il faut acquitter ; les confirmations passent par un message éphémère. Voir
les [décisions d'architecture](decisions.md).
