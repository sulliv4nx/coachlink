# CoachLink V0.3 — plan de test sur smartphone Android

## Prérequis

- Un **APK debug compilé** avec Android Studio ou via le workflow GitHub Actions fourni. Cette archive est un projet source : elle ne contient pas d'APK déjà compilé.
- Android 8+ et accès Internet pour Open Food Facts ; le backend photo IA doit être configuré séparément.
- Ne pas enregistrer de vraies données clients : il s'agit d'un prototype local non synchronisé.

## Vérification de l'interface

1. Installer puis ouvrir l'application, aller dans **Tableau**.
2. Cliquer **Afficher un exemple de tableau rempli** : les 7 colonnes de calories doivent apparaître (certaines avec `—`), avec les moyennes de calories/macros, les jours sportifs, la charge externe et deux pesées. Les données sont **fictives et ne sont pas enregistrées**.
3. Passer en **mode coach** avec le bouton en haut. Le tableau hebdomadaire doit être présent également et ses données d'aperçu doivent rester fictives.
4. Cliquer **Quitter l'aperçu fictif** : aucune donnée fictive ne doit avoir été sauvegardée dans Nutrition, Sport ou Progression.
5. Naviguer vers la semaine précédente et revenir ; les dates vont toujours du **lundi au dimanche**.

## Vérification avec données réelles saisies sur l'appareil

6. Dans **Nutrition**, ajouter deux aliments aujourd'hui : vérifier que le tableau affiche la somme des calories et des macros, et un seul **jour avec alimentation renseignée**.
7. Dans le tableau, comparer la moyenne calculée pour ce jour aux objectifs journaliers ; les six autres jours non renseignés ne doivent pas être pris pour des journées à zéro calorie.
8. En mode coach, créer un programme avec exercices ; en mode athlète, noter deux exercices le même jour. Le tableau doit annoncer **1 jour** de sport et **2 exercices consignés**, pas « 2 séances ».
9. Dans **Progression**, noter une pesée ; le tableau affiche son poids. Une variation de poids doit apparaître uniquement après **une deuxième pesée sur la même semaine**.
10. Facultatif : avec le backend IA sécurisé configuré, analyser une photo et **valider** ses aliments. Le tableau hebdomadaire doit ensuite afficher le nombre d'ingrédients photo validés, pas un faux nombre de repas.

## Points d'acceptation / bugs à signaler

- Si les chiffres restent à zéro après un ajout, vérifier la journée affichée et le retour à la semaine courante.
- Les résultats photo doivent rester identifiés comme estimations ; ils peuvent être imprécis.
- Les objectifs des semaines passées sont les objectifs **actuels**, pas des objectifs historiques.
- Les données ne sont pas partagées entre deux téléphones. Le bouton Coach change seulement la vue sur **un dossier de démonstration local**.
- Capture utile pour le diagnostic : écran, description de l'action, Android version, trace d'erreur Android Studio si disponible (sans partager de données de santé personnelles).
