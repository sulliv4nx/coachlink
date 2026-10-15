# CoachLink Android — V0.4 : coach adaptatif, courses, nutrition, sport et photo IA

**Statut : code source d'un prototype Android local V0.4, non compilé ni testé sur appareil dans cet environnement.**
Le mode coach et le mode athlète sont des démonstrations sur **un seul téléphone**. Aucune synchronisation de comptes réels n'est encore livrée.

## Nouveauté V0.3 — tableau de bord hebdomadaire (coach et athlète)

L'onglet **Tableau** regroupe maintenant deux vues, sur la même base locale :

- **Semaine** : calendrier lundi–dimanche, déplacement sur les semaines passées (jusqu'à 52), barres des calories consommées par jour, progression moyenne des calories et macros **sur les seuls jours enregistrés**, et comparaison à l'objectif courant. Un bouton **Afficher un exemple de tableau rempli** permet de tester l'affichage sans ajouter de données dans le journal.
- **Sport** : jours avec au moins une performance enregistrée, nombre de performances **par exercice** et volume externe (séries × répétitions × charge). Ce nombre ne doit pas être confondu avec le nombre de séances.
- **Corps** : dernière pesée connue, pesées de la semaine, et variation si deux pesées sont disponibles sur cette période.
- **Photos** : nombre d'ingrédients saisis et validés via l'IA photo, identifiés comme **estimations**.

**Important :** une absence de repas dans le journal est une absence de données, jamais une preuve d'une consommation de 0 kcal. Les objectifs courants sont utilisés pour toutes les semaines historiques dans ce prototype, faute de versionnement des objectifs.

### Vérification rapide des calculs hebdomadaires (sans Android)

Avec Kotlin installé : `bash tools/verify_weekly_logic.sh`. Un jeu de tests JUnit se trouve dans `app/src/test/java/com/coachlink/app/domain/WeeklyDashboardTest.kt`.

Voir `V03_CHANGELOG.md` pour le détail et les limites.

## Nouveauté V0.2 — analyser un repas en photo

Le menu **Nutrition > Analyser mon repas en photo (IA)** permet de :
1. Photographier l'assiette via l'application appareil photo ou sélectionner une image dans la galerie (Photo Picker Android).
2. Prévisualiser l'image, puis **autoriser explicitement** son envoi à un service d'analyse IA.
3. Obtenir une **estimation** d'aliments / ingrédients, des masses en grammes, des calories et macros.
4. Corriger les noms, quantités et, au besoin, les valeurs pour 100 g ; retirer un ingrédient non présent.
5. Voir le total estimé, choisir le type de repas, puis **valider** avant enregistrement dans le journal local.

L'entrée dans le journal conserve la provenance `photo_ai_confirmed`. Chaque ingrédient est sauvegardé en tant qu'entrée alimentaire distincte, afin que la correction des portions soit explicite.

**Précision :** une photo ne pèse pas les portions et ne révèle pas l'huile, le beurre, les sauces ou les ingrédients cachés. L'estimation peut être sensiblement fausse. Cette fonctionnalité n'établit pas des prescriptions médicales et n'ajuste pas automatiquement les objectifs fixés par le coach.

### Conditions d'activation IA

**Par défaut, le service IA n'est PAS configuré.** La photo et la galerie fonctionnent, mais le bouton d'analyse reste désactivé jusqu'à l'installation d'un backend.

- Un backend de référence est fourni dans `backend/app.py` avec `requirements.txt` et des tests.
- La clé API du fournisseur IA doit être configurée **uniquement sur le serveur**, jamais dans le code Android ni dans le build Gradle.
- Configurer dans le serveur la variable `OPENAI_API_KEY`. Optionnel : `OPENAI_VISION_MODEL` (par défaut `gpt-5.5`, modifier selon les modèles autorisés sur votre compte).
- Lancer localement (à la racine de ce projet) :

```bash
python -m pip install -r backend/requirements.txt
export OPENAI_API_KEY="YOUR_KEY_ON_SERVER_ONLY"
python -m uvicorn backend.app:app --host 127.0.0.1 --port 8000
```

- Publier le service derrière un **reverse-proxy HTTPS**, avec authentification, protections contre les abus et limites de débit avant usage avec de vrais clients. **Le backend de référence n'implémente pas l'authentification** : ne pas exposer ce service tel quel sur l'Internet public.
- Puis construire l'application avec l'URL HTTPS du backend :

```bash
gradle assembleDebug -PcoachlinkPhotoApiUrl=https://votre-serveur-securise.example
```

Vous pouvez aussi ajouter `coachlinkPhotoApiUrl=https://...` à votre fichier **local** `~/.gradle/gradle.properties` (et non au dépôt). L'URL est intégrée au build : un changement impose une recompilation. Seules les URL HTTPS sont acceptées.

**Flux réseau :** l'application redimensionne puis réencode la photo en JPEG (sans métadonnées EXIF), envoie un `POST image/jpeg` à `/v1/photo-meals/analyze`, puis reçoit `foods[]` (noms, poids estimés, kcal/macros pour 100 g, confiance) et `notes`. Le serveur ne sauvegarde ni ne journalise les photos dans l'implémentation fournie, mais le fournisseur IA les traite suivant ses propres conditions. Le consentement et la politique de confidentialité du futur service devront détailler les traitements et les durées de conservation.

## Ouverture et compilation Android
1. Installer Android Studio récent, JDK 17, SDK Android 36, Google Play Services pour le scanner de codes-barres et Gradle 8.13.
2. Ouvrir le dossier `CoachAthleteAndroid` dans Android Studio.
3. Synchroniser le projet Gradle. Le fichier binaire `gradle-wrapper.jar` n'est **pas fourni** : sélectionner une installation locale de Gradle 8.13, ou générer un wrapper avec cette version.
4. Lancer sur Android 8 ou supérieur. Autoriser les interactions avec la caméra système et la galerie lorsque demandées.
5. La recherche produit Open Food Facts et l'analyse IA exigent une connexion Internet ; le journal déjà saisi reste stocké sur l'appareil.

**Important :** le compilateur Android et un émulateur n'étaient pas disponibles pour cette livraison. Le build n'a donc pas été validé, et aucun APK n'est fourni. Les tests Kotlin du tableau hebdomadaire et les tests Python du backend ont été exécutés ; voir ci-dessous.

### Produire un APK de test avec GitHub Actions (alternative à Android Studio)

Le fichier `.github/workflows/android-debug-apk.yml` construit une version debug après publication du projet **dans un dépôt GitHub de votre choix**. Dans GitHub, ouvrir **Actions > Build CoachLink Android debug APK > Run workflow**. Après une exécution réussie, télécharger l'artefact `CoachLink-V04-APK-debug`, le décompresser et transférer `app-debug.apk` sur le smartphone Android. La compilation par cette méthode n'a pas encore été validée ; aucune exécution GitHub Actions n'a été déclenchée ici.


## Démonstration
- **Nutrition** : saisir un aliment, scanner un code-barres, ou analyser un repas en photo lorsque le serveur est configuré.
- **Sport** : en mode coach, créer un programme et ajouter des exercices. En mode athlète, enregistrer les performances.
- **Tableau / Progression** : consulter les indicateurs de la semaine et du jour, séances et mesures.
- **Coach** : modifier les objectifs et voir le même dossier local que l'athlète.

## Tests backend

```bash
python -m pip install -r backend/requirements.txt pytest httpx
python -m pytest -q backend/tests
```

Ils vérifient les restrictions JPEG, la taille limite, la configuration du service et le contrat de réponse avec une IA simulée ; **ils ne testent ni un véritable modèle IA ni un appareil Android**.

## Risques et limites avant un déploiement réel
- Ajouter connexion coach/athlètes, règles d'accès serveur, traçabilité, chiffrement, flux d'export et suppression RGPD, antispam et quota par utilisateur.
- Ajouter validation par nutritionniste ou références alimentaires fiables : les macros proposées par un modèle visuel restent approximatives.
- Prévoir des tests Android (appareil/émulateur), migrations Room et tests fonctionnels du scanner / caméra / photo picker.
- Ne jamais confondre calories **estimées à partir d'une photo** et calories **mesurées**. Prévoir des garde-fous supplémentaires pour les mineurs et les profils ayant des besoins médicaux spécifiques.
- Le User-Agent Open Food Facts est encore provisoire et devra être correctement identifié pour une diffusion publique.

Voir `ARCHITECTURE.md` pour les entités et les flux complets.

---

## V0.4 — Coaching adaptatif + listes de courses

La version 0.4 ajoute 2 onglets : **Coach IA** et **Courses**, et conserve Tableau, Nutrition, Sport et Photo IA.

- **Coach IA** : sélection indépendante du coach humain ou du coach IA pour le sport et la nutrition ; objectifs / points faibles / fréquence ; saisie des exercices libres, y compris leur RPE et les douleurs éventuelles. Le moteur local propose une séance réévaluée selon le journal sportif et nutritionnel. La proposition doit être confirmée avant insertion dans les programmes.
- **Nutrition** : analyse de la régularité des saisies et comparaison prudente calories/protéines, sans assimilation des données manquantes à 0 ni changement automatique des macros.
- **Courses** : planification de la semaine par ingrédient, jour, type de repas et poids. Conversion optionnelle des aliments consommés les 7 derniers jours en menu récurrent, puis liste pour la semaine ou le mois. Les aliments sont regroupés par nom normalisé. Stock à la maison et gaspillage non intégrés.
- **IA distante, optionnelle** : bouton « Demander mon bilan IA », uniquement après consentement pour l'envoi d'un résumé de l'historique. Le serveur de référence propose `POST /v1/coach/review` et exige `OPENAI_API_KEY`, sous les mêmes réserves de sécurité que le module photo. Sans serveur configuré, le coach local reste opérationnel. Aucune clé de fournisseur ne figure dans l'APK.
- **Données** : migration Room V2->V3 sans remise à zéro. Il n'existe pas encore de comptes distincts ni de connexion multi-athlètes.

Tests de logique hors Android :

```bash
bash tools/verify_adaptive_coach.sh
bash tools/verify_weekly_logic.sh
python -m pytest -q backend/tests
```

Lire `V04_CHANGELOG.md` et `V04_TEST_PLAN.md` pour les fonctionnalités exactes, la recette et les limites. L'archive contient le **code source**, pas un APK déjà compilé ; pour obtenir l'APK, exécuter `gradle :app:assembleDebug` avec l'environnement Android requis ou lancer le workflow GitHub Actions inclus.

**Le coach local est un moteur de règles adaptatives et non une IA générative.** Le modèle génératif distant, lorsqu'il est activé sur un backend protégé, produit un bilan additionnel uniquement : toutes les modifications de programmation et objectifs nécessitent encore validation humaine. Pour un véritable usage commercial avec des clients, il faudra l'authentification, les autorisations, une politique de confidentialité, la gestion des incidents et un encadrement des recommandations nutritionnelles et sportives.
