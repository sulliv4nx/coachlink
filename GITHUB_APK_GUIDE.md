# Générer un APK de test CoachLink V0.4 avec GitHub Actions

## Option A — Le projet est déjà sur GitHub

1. Placer le fichier `CoachLink_android_debug_apk.yml` dans le dépôt sous le nom exact :
   `.github/workflows/android-debug-apk.yml`.
2. Vérifier qu'à la racine du dépôt figurent `settings.gradle.kts`, `build.gradle.kts` et le dossier `app/`.
3. Valider les modifications (`Commit changes`) sur la branche `main`.
4. Ouvrir l'onglet **Actions**, puis choisir **CoachLink V0.4 - APK Android de test**.
5. Cliquer sur **Run workflow** et sélectionner la branche `main`. Le workflow se lance également automatiquement après un push sur `main`.
6. Si toutes les étapes réussissent, ouvrir l'exécution puis **Artifacts** → `CoachLink-V04-APK-debug`.
7. Télécharger le ZIP de l'artefact, le décompresser et installer `app-debug.apk` sur un téléphone Android compatible (Android 8 ou plus récent).

## Option B — Le projet n'est pas encore sur GitHub

1. Télécharger `CoachLink_V04_Pret_Pour_GitHub.zip`.
2. Décompresser ce ZIP, puis publier **son contenu** à la racine d'un dépôt GitHub. Ne pas ajouter un dossier parent `CoachAthleteAndroid` : `.github/`, `app/`, `backend/` et les fichiers Gradle doivent être à la racine.
3. Suivre les étapes 4 à 7 de l'option A.

## Ce que le workflow fait réellement

- Installe Java 17, Android SDK API 36 et Gradle 8.13.
- Exécute `:app:testDebugUnitTest` (tests Android locaux).
- Construit l'APK de test avec `:app:assembleDebug`.
- Vérifie l'existence du fichier `app/build/outputs/apk/debug/app-debug.apk` et le publie comme artefact pour 14 jours.
- Ne signe pas une version destinée au Play Store ; Android fournit la signature debug de développement.

**Le fichier `gradle-wrapper.jar` n'est pas inclus dans la V0.4.** C'est pourquoi le workflow installe directement Gradle 8.13 et n'utilise pas `./gradlew`.

## Points importants

- Aucun APK n'est inclus dans l'archive du projet : il est produit seulement **après une exécution GitHub réussie**.
- Le workflow lui-même a été vérifié syntaxiquement et les tests locaux de logique/serveur existants passent, mais **une compilation Android de bout en bout n'a pas encore été vérifiée**. Si la compilation échoue, ouvrir l'étape en rouge et relever le message d'erreur exact.
- Le scanner photo/coach IA distant demande un backend HTTPS configuré et protégé ; cette configuration n'est **pas** automatiquement activée dans un APK de test.
- Ne jamais placer de clé API fournisseur dans les fichiers Android ou les variables de compilation exportées vers l'APK.
- Les comptes coach/athlètes synchronisés entre appareils ne font pas encore partie du prototype V0.4.
