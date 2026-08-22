# Parcours utilisateur, du geste au code

Ce que fait l'opérateur, et ce que le code exécute à chaque étape. Trois
parcours, ceux du cahier des charges.

!!! warning "Les numéros de ligne vieillissent"

    Ils correspondent au commit **e985659**. Un ajout de dix
    lignes en haut d'un fichier les décale tous sans que rien ne le signale.
    Les noms de fonctions, eux, restent justes : en cas de doute, cherchez le
    nom plutôt que la ligne.

---

## Parcours connexion

### Créer un compte

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | L'application démarre sur l'écran de connexion, aucune session ouverte | `MyApp` - `MainActivity.kt:119` ; route `AUTH` - `Destinations.kt:6` |
| 2 | Il touche « Créer un compte » en bas de l'écran | `toggleMode()` - `AuthViewModel.kt:33`. Un champ Nom apparaît |
| 3 | Il saisit nom, adresse e-mail, mot de passe | `onDisplayNameChange` :30, `onEmailChange` :24, `onPasswordChange` :27. Chaque frappe efface l'erreur du champ concerné |
| 4 | Il touche « Créer le compte » | `submit()` - `AuthViewModel.kt:44` |
| 5 | La validation locale s'exécute d'abord | Toujours dans `submit()` : e-mail vide ou sans `@`, mot de passe sous six caractères, nom vide. **Aucun appel réseau si un champ est fautif** |
| 6 | L'appel part vers Firebase | `signUp` - `UserRepositoryImpl.kt:36` : création du compte, `updateProfile` pour le nom, puis `reload` pour que l'instance locale le reflète |
| 7 | En cas d'échec | `messageFor` :99 traduit « already in use » en message lisible. Les libellés bruts de Firebase sont en anglais et parlent de « credential » |
| 8 | En cas de succès | Rien n'est fait ici : `currentUser` change, et `SessionRedirect` (`StockNavGraph.kt:39`) redirige |

L'écran est découpé en deux : `AuthScreen` (`AuthScreen.kt:36`) connaît le
ViewModel, `AuthContent` (:54) ne reçoit que des données et des lambdas - c'est
ce qui le rend prévisualisable et testable sans Hilt.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant UI as AuthScreen
    participant VM as AuthViewModel
    participant Repo as UserRepositoryImpl
    participant FB as Firebase Auth

    UI->>U: Formulaire de connexion (route AUTH)
    U->>UI: Touche « Créer un compte »
    UI->>VM: toggleMode() (:33)
    VM-->>UI: Mode inscription, le champ Nom apparaît
    U->>UI: Saisit nom, adresse, mot de passe
    UI->>VM: onDisplayNameChange(), onEmailChange(), onPasswordChange()
    U->>UI: Touche « Créer le compte »
    UI->>VM: submit() (:44)

    alt Validation locale échouée
        VM-->>UI: Erreurs sous les champs, aucun appel réseau
    else Champs valides
        VM->>Repo: signUp(email, mot de passe, nom) (:36)
        Repo->>FB: createUserWithEmailAndPassword()
        alt Échec
            FB-->>Repo: exception
            Repo-->>VM: Result.failure
            Note over VM: messageFor() traduit « already in use » (:99)
            VM-->>UI: Message sous le bouton, saisie conservée
        else Succès
            FB-->>Repo: FirebaseUser
            Repo->>FB: updateProfile(nom) puis reload()
            Note over VM: Le ViewModel ne navigue pas.<br/>currentUser change, SessionRedirect fait le reste (:39)
            VM-->>U: Écran d'accueil, pas le stock
        end
    end
```

### S'identifier

Même chemin, deux différences : `submit()` appelle `signIn`
(`UserRepositoryImpl.kt:31`), et le nom n'est pas demandé.

Après succès, l'opérateur arrive sur l'**écran d'accueil** -
`composable(WELCOME)` (`StockNavGraph.kt:89`), `WelcomeScreen`
(`WelcomeScreen.kt:62`) - qui nomme explicitement la session ouverte. C'est le
garde-fou demandé pour les téléphones partagés.

Il touche « OK, c'est bien moi » (:122) → `acknowledgeWelcome()`
(`MainViewModel.kt:116`), et l'effet de navigation l'envoie sur la liste des
emplacements.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant VM as AuthViewModel
    participant Repo as UserRepositoryImpl
    participant FB as Firebase Auth
    participant Nav as SessionRedirect

    U->>VM: submit() (:44)
    Note over VM: Validation locale : arobase et point,<br/>mot de passe d'au moins six caractères
    VM->>Repo: signIn(adresse, mot de passe) (:31)
    Repo->>FB: signInWithEmailAndPassword()

    alt Échec
        FB-->>Repo: exception
        Repo-->>VM: Result.failure
        VM-->>U: « Adresse ou mot de passe incorrect » (:99)
    else Succès
        FB->>Repo: AuthStateListener émet le nouvel utilisateur
        Repo-->>Nav: MainUiState.user change
        Nav->>Nav: user non nul, accueil non validé (:39)
        Nav->>U: Écran d'accueil
    end
```

### Se déconnecter

Deux points d'entrée :

- depuis l'accueil, le bouton « Se déconnecter » - `WelcomeScreen.kt:126` ;
- depuis n'importe quel écran de stock, l'icône de la barre supérieure
  `SignOutIcon` (`AppBars.kt:175`).

Les deux appellent `signOut()` (`MainViewModel.kt:120`), qui remet l'accueil à
« non validé » **puis** délègue à `signOut` (`UserRepositoryImpl.kt:54`). Sans
cette remise à zéro, l'opérateur suivant sauterait l'avertissement.

`currentUser` passe à `null`, et deux mécanismes se déclenchent :

- `SessionRedirect` renvoie sur `AUTH` **en vidant la pile** - le
  bouton retour ne doit pas ramener sur les écrans de stock ;
- `whileSignedIn` (`ui/SessionGate.kt`) annule les écouteurs Firestore. Sans
  lui, ils recevraient un refus de permission et l'application se fermerait.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant VM as MainViewModel
    participant Repo as UserRepositoryImpl
    participant Gate as whileSignedIn
    participant FS as Écouteurs Firestore

    U->>VM: signOut() (:120)
    VM->>VM: welcomeAcknowledged = false (:121)
    Note over VM: D'abord la remise à zéro : sans elle, l'opérateur<br/>suivant sauterait l'avertissement
    VM->>Repo: signOut() (:54)

    par Deux mécanismes
        Repo-->>Gate: currentUser passe à null
        Gate->>FS: flux débranchés, awaitClose retire les écouteurs
        Note over FS: Sans cela : PERMISSION_DENIED,<br/>et l'application se ferme
    and
        Repo-->>VM: MainUiState.user = null
        VM-->>U: SessionRedirect ramène sur AUTH, pile vidée
    end

    Note over U: À la racine, le retour ferme l'application<br/>FinishOnBackFromRoot (:68) — sinon écran noir
```

### Supprimer le compte

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | Il touche « Supprimer mon compte », en rouge et à l'écart des deux actions courantes | `WelcomeScreen.kt:131` |
| 2 | Une fenêtre s'ouvre. Elle avertit que **l'historique restera signé de son adresse**, et demande le mot de passe | `DeleteAccountDialog` :154 |
| 3 | Il valide | :194 → `deleteAccount(password)` - `MainViewModel.kt:83` |
| 4 | Ré-authentification, puis suppression | `UserRepositoryImpl.kt:61`. Firebase refuse `delete()` sans connexion récente : le mot de passe lève la contrainte et sert de confirmation |
| 5 | Mot de passe faux | L'erreur s'affiche sous le champ (:182) et la fenêtre reste ouverte : la saisie n'est pas perdue |
| 6 | Succès | La session se ferme, le même effet de navigation ramène sur `AUTH` |

L'avertissement est affiché **avant** la validation, pas après. Voir la
[question ouverte](taches.md#rgpd) sur le sort de cette donnée personnelle.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant Dlg as DeleteAccountDialog
    participant VM as MainViewModel
    participant Repo as UserRepositoryImpl
    participant FB as Firebase Auth

    U->>Dlg: « Supprimer mon compte » (:131)
    Note over Dlg: Avertit AVANT validation que l'historique<br/>restera signé de son adresse
    U->>Dlg: Saisit son mot de passe et valide (:194)
    Dlg->>VM: deleteAccount(mot de passe) (:83)
    VM->>Repo: deleteAccount(mot de passe) (:61)
    Repo->>FB: reauthenticate(credential)
    Note over Repo,FB: Firebase refuse delete() sans connexion récente.<br/>Le mot de passe lève la contrainte et sert de confirmation

    alt Mot de passe faux
        FB-->>Repo: exception
        Repo-->>VM: Result.failure
        VM->>VM: deletionMessageFor() (:101)
        VM-->>Dlg: Erreur sous le champ, fenêtre rouverte
    else Accepté
        Repo->>FB: delete()
        FB-->>Repo: compte supprimé, session fermée
        VM-->>U: SessionRedirect ramène sur AUTH
        Note over FB: L'historique reste : journal en ajout seul
    end
```

---

## Parcours emplacements de stockage

### Créer un emplacement

1. Sur la liste, il touche le bouton flottant - `StockFab` (`AppBars.kt:143`). La route
   courante décide de l'action : `AISLE_LIST` déclenche
   `showAddAisleDialog = true` (`MainActivity.kt:180`).
2. La fenêtre s'affiche - appel via `AddAisleDialogHost`, définition `AddAisleDialog.kt:19`. Le
   bouton « Créer » reste inactif tant que le nom est vide.
3. Il valide : `onConfirm(name)` (:45) → `addAisle` (`AisleViewModel.kt:53`) →
   `addAisle` (`AisleRepositoryImpl.kt:62`).
4. La liste se met à jour seule : `observeAisles` (:29) est un flux, et
   `uiState` (`AisleViewModel.kt:35`) réémet.

Un nom déjà pris est refusé à la casse et aux espaces près, et le message
s'affiche **sous le champ, fenêtre ouverte** : deux emplacements homonymes
seraient indiscernables dans la liste déroulante du formulaire de médicament.

Cet écran remplace l'ancien bouton qui fabriquait « Aisle 2 », « Aisle 3 » : un
emplacement porte un nom choisi.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant Fab as StockFab
    participant Host as AddAisleDialogHost
    participant VM as AisleViewModel
    participant Repo as AisleRepositoryImpl

    U->>Fab: Bouton flottant (AppBars.kt:143)
    Note over Fab: C'est l'onglet courant qui décide de ce qu'on ajoute
    Fab->>Host: showAddAisleDialog = true (MainActivity.kt:180)
    U->>Host: Saisit un nom et valide (AddAisleDialog.kt:45)
    Host->>VM: addAisle(nom) (:53)

    alt Nom vide ou fait d'espaces
        VM-->>Host: form_error_name_required (:56)
    else Nom déjà pris
        Note over VM: Comparaison à la casse et aux espaces près
        VM-->>Host: aisle_error_duplicate (:66)
    else Nom accepté
        VM->>Repo: addAisle(nom) (:62)
        Repo-->>VM: AisleDto
        VM->>VM: aisleCreated = true (:73)
        VM-->>Host: LaunchedEffect(aisleCreated) ferme la fenêtre
        Repo-->>U: La liste se met à jour seule, observeAisles est un flux
    end
```

La fenêtre se ferme **au succès, jamais au clic** : sinon un nom refusé
disparaîtrait avec son message avant d'avoir été lu.

### Ouvrir un emplacement

`AisleItem` (`AisleScreen.kt:94`) remonte le clic à :85, qui appelle
`Destinations.aisleDetail(id)` (:22) - la route `aisle/{aisleId}` - et le
NavHost sélectionne `composable(AISLE_DETAIL)` (`StockNavGraph.kt:127`).

`AisleDetailScreen` (`AisleDetailScreen.kt:23`) n'a **aucune composable
propre** : il réutilise `MedicineContent` (:33). Même ligne, même mise en page,
seul le contenu change.

Le contenu, justement, ne vient pas de la liste principale. L'écran ouvre son
propre flux - `observeMedicinesInAisle` (`MedicineViewModel.kt:67`) →
`MedicineRepositoryImpl.kt:68` - dont le filtre `whereEqualTo("aisleId", …)`
est **exécuté par la base**. Auparavant tout le stock descendait pour n'afficher
qu'une poignée de lignes.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant List as AisleScreen
    participant Screen as AisleDetailScreen
    participant VM as MedicineViewModel
    participant FS as Firestore

    U->>List: Touche un emplacement (AisleItem :94)
    List->>Screen: Destinations.aisleDetail(id), composable AISLE_DETAIL
    Screen->>VM: observeMedicinesInAisle(aisleId) (:67)
    VM->>FS: whereEqualTo("aisleId", …) (MedicineRepositoryImpl.kt:68)
    Note over FS: Le filtre est exécuté PAR LA BASE.<br/>Avant, tout le stock descendait pour afficher<br/>une poignée de lignes
    FS-->>VM: seulement les médicaments de cet emplacement
    VM-->>Screen: MedicineListUiState
    Screen->>U: Réutilise MedicineContent (:33)
```

### Ouvrir un médicament depuis un emplacement

C'est la même composable que la liste principale : `MedicineContent`
(`MedicineScreen.kt:37`), clic à :61 → `Destinations.medicineDetail(id)` (:24)
→ `composable(MEDICINE_DETAIL)` (`StockNavGraph.kt:142`).

---

## Parcours médicaments

### Créer un médicament

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | Il touche le bouton flottant depuis la liste des médicaments | `StockFab` (`AppBars.kt:143`) → route `MEDICINE_NEW`, déclarée **avant** `medicine/{id}` (`StockNavGraph.kt:136` contre :142) sans quoi « new » serait pris pour un identifiant |
| 2 | Il saisit le nom | `onNameChange` - `MedicineFormViewModel.kt:74` |
| 3 | Il choisit l'emplacement dans une liste déroulante | `MedicineFormScreen.kt:107` → `onAisleChange` :80. **On choisit parmi ce qui existe, on ne saisit pas librement** |
| 4 | Il saisit la quantité initiale | `onStockChange` :77 - les caractères non numériques sont filtrés à la frappe |
| 5 | Il touche « Créer le médicament » | `MedicineFormScreen.kt:143` → `submit()` :83 : validation des trois champs, puis `addMedicine` - `MedicineRepositoryImpl.kt:116` |
| 6 | Le médicament et sa trace de création partent dans **le même lot** | Toujours :116. Aucun des deux ne peut exister sans l'autre |
| 7 | Retour automatique à la liste | `LaunchedEffect(state.isSaved)` - `MedicineFormScreen.kt:46` |
| 8 | Si l'enregistrement échoue | Le message s'affiche **sous le bouton** et la saisie est conservée : un message éphémère disparaîtrait pendant que l'opérateur retape |

Cet écran remplace le bouton « + » qui ajoutait un médicament au nom et au stock
aléatoires, dans un emplacement tiré au hasard.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant UI as MedicineFormScreen
    participant VM as MedicineFormViewModel
    participant Repo as MedicineRepositoryImpl
    participant FS as Firestore

    U->>UI: Saisit le nom, choisit l'emplacement, la quantité
    Note over UI: L'emplacement se choisit dans une liste (:107).<br/>On ne peut plus inventer un rayon
    U->>UI: Touche « Créer le médicament » (:143)
    UI->>VM: submit() (:83)

    alt Un champ invalide
        VM-->>UI: Erreurs sous les champs, aucun appel réseau
    else Champs valides
        VM->>Repo: addMedicine(nom, stock, aisleId, e-mail) (:116)
        Repo->>FS: WriteBatch : set(médicament) + set(entrée CREATE)
        Note over FS: Un lot, pas deux écritures successives :<br/>aucun des deux ne peut exister sans l'autre
        alt Échec
            FS-->>Repo: exception
            Repo-->>VM: StockException traduite
            VM-->>UI: Message sous le bouton, saisie conservée
        else Succès
            FS-->>Repo: lot appliqué
            VM->>VM: isSaved = true (:130)
            VM-->>UI: LaunchedEffect(isSaved) (:46)
            UI->>U: Retour automatique à la liste
        end
    end
```

### Ouvrir un médicament

`MedicineDetailScreen` (`MedicineDetailScreen.kt:59`) appelle `observeDetail`
(`MedicineViewModel.kt:111`), qui combine trois sources : le médicament
(`MedicineRepositoryImpl.kt:85`), son historique (:96), et les libellés
d'emplacement - le document ne porte qu'un identifiant de rayon, le libellé se
résout dans le ViewModel.

L'historique est lu **par pages** : vingt entrées, pas les centaines qu'accumule
un médicament très manipulé. Une entrée de plus est demandée sans jamais être
affichée - c'est elle qui dit à l'écran qu'une page plus ancienne existe
(`detail` — `MedicineViewModel.kt:123`). Le bouton « Voir les entrées plus anciennes »
(`MedicineDetailScreen.kt:268`) appelle `showMoreHistory()`
(`MedicineViewModel.kt:139`), qui élargit la fenêtre sans reconstruire le flux :
la fiche ne repasse pas par son indicateur de chargement.

L'affichage se fait par `MedicineDetailContent` (:150), les trois valeurs en
lecture par `ReadOnlyField` (:280), l'historique par `HistoryItem` (:327).

L'historique remonte en tête à chaque nouvelle entrée (:161) : sans cela, un
opérateur qui a fait défiler la liste ne verrait pas le mouvement qu'il vient de
faire.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant UI as MedicineDetailScreen
    participant VM as MedicineViewModel
    participant Repo as MedicineRepositoryImpl

    U->>UI: Touche un médicament
    UI->>VM: observeDetail(medicineId) (:111)

    par Trois sources combinées
        VM->>Repo: observeMedicine(id) (:85)
    and
        VM->>Repo: observeHistory(id, limit = 20 + 1) (:96)
        Note over Repo: Une entrée de plus que ce qui sera affiché :<br/>c'est elle, jamais montrée, qui dit qu'une<br/>page plus ancienne existe
    and
        VM->>VM: aisleNames — le document ne porte qu'un identifiant
    end

    Repo-->>VM: le médicament et 21 entrées
    VM->>VM: take(20), hasMoreHistory = true (:123)
    VM-->>UI: MedicineDetailUiState
    UI->>U: Fiche, 20 entrées, bouton « Voir plus »

    U->>UI: « Voir les entrées plus anciennes » (:268)
    UI->>VM: showMoreHistory() (:139)
    Note over VM: La fenêtre vit dans le ViewModel : l'élargir ne<br/>reconstruit pas le flux, la fiche ne repasse pas<br/>par son indicateur de chargement
    VM-->>UI: 40 entrées
```

### Diminuer la quantité

| # | Ce que fait l'utilisateur | Code |
|---|---|---|
| 1 | Il saisit une quantité | Le champ est vide au départ, ce qui laisse les deux boutons **inactifs** : un doigt qui traîne ne peut pas produire un mouvement |
| 2 | Il touche « Retirer » | `MedicineDetailScreen.kt:213` → :111 → `updateStock` - `MedicineViewModel.kt:155` |
| 3 | Une transaction s'exécute | `MedicineRepositoryImpl.kt:204` : relecture du stock réel, refus si le retrait le dépasse, sinon écriture **du stock et de sa trace ensemble** |
| 4 | Si le mouvement est accepté | Confirmation par `LaunchedEffect(confirmation)` (:79). Le champ n'est vidé **qu'ici**, une fois l'écriture faite |
| 5 | Si le retrait dépasse le stock | Fenêtre à valider - `ActionErrorHost` (`MainActivity.kt:213`) - indiquant la quantité disponible. La saisie est conservée |

Deux points qui se voient mal dans le code mais comptent à l'usage :

**Le contrôle est dans la transaction, pas dans l'écran.** C'est le seul endroit
qui lit le stock réel au moment de l'écriture ; un contrôle sur la valeur
affichée travaillerait sur un chiffre peut-être périmé.

**Un mouvement de cinquante unités produit une seule entrée d'historique.** Le
service qualité cherchant « qui a retiré 50 boîtes » ne trouve pas cinquante
lignes de « -1 ».
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant UI as MedicineDetailScreen
    participant VM as MedicineViewModel
    participant Repo as MedicineRepositoryImpl
    participant FS as Firestore

    U->>UI: Saisit une quantité, touche « Retirer » (:213)
    UI->>VM: updateStock(id, -quantité) (:155)
    VM->>Repo: updateStock(id, delta, e-mail) (:204)

    rect rgb(245, 245, 245)
        Note over Repo,FS: runTransaction : lire, décider, écrire
        Repo->>FS: get(medicine) — toutes les lectures d'abord
        FS-->>Repo: le stock réel
        alt stock + delta inférieur à zéro
            Repo-->>Repo: StockChange.Insufficient(disponible)
        else Mouvement possible
            Repo->>FS: update(stock) + set(entrée d'historique)
            Note over FS: Les deux écritures ensemble, ou aucune
        end
    end

    alt Refus
        Repo-->>VM: StockException(INSUFFICIENT_STOCK, disponible)
        VM->>VM: actionError = toUiMessage() (:168)
        VM-->>U: Fenêtre à valider, saisie conservée
    else Succès
        Repo-->>VM: écriture confirmée par le serveur
        VM->>VM: movementConfirmed (:159)
        VM-->>UI: LaunchedEffect(confirmation) (:79)
        UI->>U: Snackbar, et le champ se vide seulement ici
    end
```

### Corriger une fiche

« Modifier ce médicament » (`MedicineDetailScreen.kt:236`) ouvre
`composable(MEDICINE_EDIT)` (`StockNavGraph.kt:152`) - le même formulaire que la
création, qui lit son identifiant dans son `SavedStateHandle`.

`updateMedicine` (`MedicineRepositoryImpl.kt:157`) corrige le nom et
l'emplacement, jamais le stock : celui-ci ne bouge que par un mouvement tracé.
La correction produit sa propre entrée d'historique, avec un stock avant et
après identiques.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant Detail as MedicineDetailScreen
    participant VM as MedicineFormViewModel
    participant Repo as MedicineRepositoryImpl
    participant FS as Firestore

    U->>Detail: « Modifier ce médicament » (:236)
    Detail->>VM: composable MEDICINE_EDIT (StockNavGraph.kt:152)
    Note over VM: L'identifiant vient du SavedStateHandle (:41) :<br/>c'est la route qui distingue création et correction
    VM->>Repo: observeMedicine(id).first() (:58)
    Repo-->>VM: les valeurs actuelles
    VM-->>U: Formulaire pré-rempli, le même écran que la création

    U->>VM: Corrige le nom ou l'emplacement, valide
    VM->>Repo: updateMedicine(...) (:157)

    rect rgb(245, 245, 245)
        Repo->>FS: get(medicine) puis relecture
        alt Aucun changement réel
            Repo-->>Repo: rien n'est écrit, pas d'entrée d'historique
        else Changement
            Repo->>FS: update(name + nameLowercase + aisleId)
            Repo->>FS: set(entrée UPDATE, stock avant = stock après)
            Note over FS: nameLowercase mis à jour EN MÊME TEMPS : l'oublier<br/>ferait sortir le médicament de la recherche,<br/>sans aucune erreur visible
        end
    end

    Repo-->>U: Retour à la fiche
```

Le stock n'est **jamais** modifiable ici : il ne bouge que par un mouvement
tracé. Corriger un stock par ce chemin contournerait la traçabilité.

### Supprimer un médicament

1. Il touche « Supprimer ce médicament » - `MedicineDetailScreen.kt:239`.
2. `DeleteMedicineDialog` (:296) rappelle **en rouge le nombre d'unités
   restantes** si le stock n'est pas nul. La décision se prend en connaissance
   de cause.
3. Il valide → :113 → `deleteMedicine` (`MedicineViewModel.kt:172`) →
   `MedicineRepositoryImpl.kt:249` : suppression et trace dans la même
   transaction. **L'historique survit au médicament supprimé** - c'est
   l'opération que le service qualité a le plus besoin de retrouver.
4. `onDeleted` renvoie à la liste par `navigateUp`.
```mermaid
sequenceDiagram
    autonumber
    actor U as Opérateur
    participant UI as MedicineDetailScreen
    participant Dlg as DeleteMedicineDialog
    participant VM as MedicineViewModel
    participant Repo as MedicineRepositoryImpl
    participant FS as Firestore

    U->>UI: « Supprimer ce médicament » (:239)
    UI->>Dlg: Ouvre la confirmation (:296)
    Note over Dlg: Rappelle EN ROUGE le nombre d'unités restantes<br/>si le stock n'est pas nul
    U->>Dlg: Valide
    Dlg->>VM: deleteMedicine(id) (:172)
    VM->>Repo: deleteMedicine(id, e-mail) (:249)
    Repo->>FS: runTransaction : delete(medicine) + set(entrée DELETE)
    Note over FS: L'historique survit au médicament supprimé.<br/>C'est l'opération que le service qualité a<br/>le plus besoin de retrouver
    Repo-->>VM: suppression confirmée
    VM-->>U: onDeleted, retour à la liste par navigateUp
```

---

## Ce que ces parcours ont en commun

Trois traits reviennent dans les trois parcours, et ce sont des choix, pas des
hasards :

**Les écrans ne décident de rien.** Ils affichent un état et remontent des
gestes. Toute la logique est dans les ViewModels et les dépôts - c'est ce qui
permet de tester ces parcours sans émulateur.

**Les écritures sensibles passent par une transaction**, avec leur trace dans la
même opération. Un stock modifié sans trace serait exactement l'incohérence
signalée par le service qualité.

**Un refus se valide, une réussite s'annonce.** Les échecs ouvrent une fenêtre
qu'il faut acquitter ; les confirmations passent par un message éphémère. Voir
les [décisions d'architecture](decisions.md).
