# EasyCook

Application Android de gestion de recettes de cuisine, écrite en Kotlin avec Jetpack Compose.
Elle permet de créer ses propres recettes, de les consulter et de les suivre étape par étape en cuisinant.
Le stockage est local et ne nécessite aucune connexion réseau.

## Outils et logiciels
- Android Studio
- Claude Opus 5.5
## Fonctionnalités

- **Liste des recettes** : toutes les recettes enregistrées, avec leur temps de cuisson, et la suppression après confirmation.
- **Création d'une recette** : nom, image (URL), temps de cuisson, description, ingrédients et étapes de préparation (facultatives).
- **Détail et modification** : consultation complète d'une recette et modification de tous ses champs.
- **Mode étape par étape** : chaque étape se coche une fois réalisée, avec une barre de progression. On peut aussi ajouter, modifier ou supprimer des étapes directement depuis cet écran.
- **Validation des saisies** : nom obligatoire, temps de cuisson positif, au moins un ingrédient complet et aucun ingrédient à moitié rempli.
- **Stockage local** : les recettes sont enregistrées sur l'appareil (base Room) et conservées entre deux lancements.
- **Thème clair et sombre** : la charte graphique EasyCook (orange, vert basilic, fond crème) suit le réglage du téléphone.

## Prérequis

- Android Studio (version récente, compatible avec Android Gradle Plugin 9.3)
- JDK 11 ou plus récent
- Un appareil ou un émulateur sous **Android 7.0 (API 24)** ou plus

## Installation et lancement

```bash
git clone https://github.com/JerryChan1881/EasyCook.git
```

Ouvrez ensuite le dossier dans Android Studio, attendez la synchronisation Gradle, puis lancez la configuration **app** sur un émulateur ou un appareil.

En ligne de commande, pour compiler l'APK de debug :

```bash
./gradlew assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/`.

## Tests

Les tests unitaires tournent sur la JVM, sans émulateur :

```bash
./gradlew testDebugUnitTest
```

Ils couvrent :
- `RecipeValidatorTest` : les règles de validation et le nettoyage des saisies ;
- `StateViewModelTest` : la lecture, l'ajout, la modification et la suppression des recettes et des étapes, avec un faux DAO en mémoire (`FakeRecipeDao`).

## Architecture

L'application suit une architecture simple en couches : **UI Compose → ViewModel → DAO Room**.

```
app/src/main/java/com/example/easycook/
├── MainActivity.kt          Point d'entrée, applique le thème et crée le ViewModel
├── NavHostcontroller.kt     Graphe de navigation entre les écrans
├── Routes.kt                Routes de navigation centralisées
├── StateViewModel.kt        État de l'application et actions (recettes, étapes)
├── data/
│   ├── RecipeDatabase.kt    Base Room : entités, DAO et migrations
│   ├── RecipeValidator.kt   Validation des saisies (sans dépendance Android)
│   └── Recipe.kt, Ingredient.kt, RecipeStep.kt   Modèles utilisés par l'UI
└── ui/
    ├── screens/             Écrans : accueil, ajout, détail, étape par étape
    ├── composable/          Composants réutilisables (carte, pastille, bouton…)
    ├── theme/               Couleurs, typographie et formes de la charte
    └── RecipeErrorMessages.kt   Traduction des erreurs de validation en texte
```

### Base de données

La base `easycook.db` contient trois tables liées par `recipeId`, avec suppression en cascade :

| Table         | Contenu                                                       |
| ------------- | ------------------------------------------------------------- |
| `recipes`     | titre, image, temps de cuisson, description                   |
| `ingredients` | quantité et nom de chaque ingrédient                          |
| `steps`       | instruction, position dans la recette et état « terminé »     |

Le schéma est en version 3, et les migrations depuis les versions précédentes sont fournies.

### Textes et ressources

Aucun texte affiché n'est écrit en dur dans le code : ils sont tous dans `res/values/strings.xml`.
Pour traduire l'application, il suffit d'ajouter un dossier `values-<langue>/strings.xml`
(par exemple `values-en/` pour l'anglais).


## Stack technique

| Domaine        | Bibliothèque                         |
| -------------- | ------------------------------------ |
| Langage        | Kotlin 2.2                           |
| Interface      | Jetpack Compose, Material 3          |
| Navigation     | Navigation Compose                   |
| Persistance    | Room (avec KSP)                      |
| Asynchrone     | Coroutines Kotlin, Flow              |
| Tests          | JUnit 4, kotlinx-coroutines-test     |

## Pistes d'évolution

- Afficher les vraies images des recettes à partir de leur URL (le bandeau est pour l'instant une illustration).
- Brancher la recherche et les favoris (les boutons sont déjà présents sur l'accueil).
- Ajouter une traduction anglaise.
- Ajouter une fonction de recherche
- Ajouter des catégories
