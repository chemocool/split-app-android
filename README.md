# Split

Application Android pour partager les dépenses à deux (Guillaume et Alexandra), inspirée de Splitwise.

- Connexion avec le compte Google (Firebase Auth + Credential Manager)
- Données partagées en temps réel entre les deux téléphones (Cloud Firestore)
- Interface en français, montants en dollars canadiens (« 46,25 $ »)
- Partage d'une dépense : **Égal**, **En $** (montant précis dû par l'autre) ou **Totalité** (l'autre rembourse tout)
- Remboursement toujours par **virement Interac**, enregistré dans l'app
- Thème clair, sombre ou système ; export CSV

Maquettes Figma : https://www.figma.com/design/YteBxcHc08gkrjoRnOsadO

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, DataStore, Firebase (Auth, Firestore). minSdk 26, targetSdk 35.

```
app/src/main/java/com/chemocool/split/
├── domain/   modèles, calcul des parts et des soldes, format des montants, export CSV (testés)
├── data/     connexion Google, dépôt Firestore, préférences locales
└── ui/       thème, navigation, écrans (Connexion, Accueil, Activité, Nouvelle dépense, Soldes, Régler le solde, Paramètres)
```

## Mise en route

### 1. Firebase

1. Sur https://console.firebase.google.com, créez un projet (Google Analytics n'est pas nécessaire).
2. Ajoutez une application **Android** avec le nom de package `com.chemocool.split`.
3. Téléchargez `google-services.json` et placez-le dans le dossier `app/`.
   Ce fichier est ignoré par git : ne le versionnez pas.
4. **Authentication** > Sign-in method > activez **Google**.
5. **Firestore Database** > créez la base (région `northamerica-northeast1` ou `northeast2`, mode production),
   puis dans l'onglet **Règles**, collez le contenu de [`firestore.rules`](firestore.rules) et publiez.
6. Ajoutez l'empreinte **SHA-1** de votre clé de signature dans les paramètres de l'app Android Firebase
   (Paramètres du projet > Vos applications), puis retéléchargez `google-services.json`.
   Pour la clé de debug, dans Android Studio : onglet Gradle > `app` > Tasks > android > `signingReport`,
   ou en ligne de commande : `./gradlew signingReport`.
   Sans SHA-1, la connexion Google échoue.

### 2. Android Studio

1. Installez Android Studio (https://developer.android.com/studio).
2. File > Open > ce dossier. Laissez Gradle synchroniser.
3. Branchez votre téléphone (Options pour les développeurs > Débogage USB) ou lancez un émulateur avec Google Play.
4. Cliquez ▶ Run.

### 3. Premier lancement

1. Guillaume se connecte avec Google, puis **Créer le foyer**. Un code d'invitation à 6 caractères s'affiche.
2. Alexandra installe l'app, se connecte, puis saisit le code dans **Rejoindre le foyer**.
3. C'est prêt : chaque dépense ajoutée apparaît sur les deux téléphones.

## Tests

```
./gradlew testDebugUnitTest
```

Les tests couvrent le format des montants, le calcul des parts, les soldes et l'export CSV.

## Pas encore fait

- Les interrupteurs de notifications sont enregistrés mais n'envoient encore rien (il faudra Firebase Cloud Messaging et une Cloud Function).
- Modifier une dépense existante (pour l'instant : supprimer puis recréer).
