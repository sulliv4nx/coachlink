# CoachLink V0.3 — Tableau de bord hebdomadaire

## Fonctionnalités ajoutées

- Nouveau **tableau de bord hebdomadaire** dans le premier onglet, en mode **athlète** et **coach**. Bouton pour consulter un **exemple fictif** sans écrire dans la base locale.
- Navigation entre semaine en cours et les 52 semaines précédentes, du lundi au dimanche.
- Histogramme quotidien des calories, avec valeur exacte sur chaque jour, repère graphique à 150 % de l'objectif actuel.
- Moyennes **calories, protéines, glucides, lipides uniquement sur les jours renseignés**, comparées aux objectifs du coach.
- Indication claire des journées sans journal : les considérer comme 0 kcal serait trompeur.
- Indication du nombre d'ingrédients issus de photographies IA **validées** par l'athlète.
- Sport : jours avec au moins une activité saisie, nombre de **logs d'exercice** (pas de faux « nombre de séances »), volume externe kg = séries × répétitions × charge.
- Suivi corporel : dernière pesée connue avant la fin de la semaine, nombre de pesées hebdomadaires, différence de poids uniquement si deux mesures existent sur la semaine.
- Test unitaire pour les bornes des semaines, moyennes sur jours renseignés, volume et pesées.

## Technique et intégration

- Nouveau module de calcul **pur Kotlin** `app/src/main/java/com/coachlink/app/domain/WeeklyDashboard.kt`, testable sans Android.
- Nouveau composant Compose `app/src/main/java/com/coachlink/app/ui/WeeklyDashboardScreen.kt`.
- Réutilisation de la base Room V0.2 **sans modifier le schéma**. Pas de migration additionnelle ; compatibilité conservée avec une installation V0.2 du même `applicationId`.
- BOM Compose réajusté à `2026.05.01` pour rester compatible avec `compileSdk 36` / AGP 8.13.2. Plugin Kotlin 2.3.10, KSP 2.3.10, Gradle 8.13, JDK 17.
- Workflow `.github/workflows/android-debug-apk.yml` : produit un APK debug comme artefact GitHub Actions si le dépôt a été publié et que la compilation réussit.

## Vérifications réalisées

- `bash tools/verify_weekly_logic.sh` : **OK** (tests Kotlin purs, exécutés localement).
- `python -m pytest -q backend/tests` : **5 passed** (serveur photo IA simulé).
- Tests JUnit ajoutés au projet ; ils seront exécutés par Gradle lors du build Android.

## Limitations actuelles

- **Pas d'APK généré ni de test réel sur téléphone** : SDK Android/Gradle Android absents de l'environnement de livraison.
- Comptes coach/athlète toujours simulés **sur un seul appareil**, sans vrai multi-athlètes ni partage cloud.
- Objectifs nutritionnels actuels utilisés dans toutes les semaines consultées, car pas encore d'historique d'objectifs.
- La fonction d'estimation photo ne fonctionne qu'après déploiement et configuration d'un serveur IA HTTPS sécurisé. Elle ne garantit pas des calories exactes.
- Les calories dépensées durant le sport ne sont pas soustraites automatiquement.
