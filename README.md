# CoachLink One (design 2027) — Android beta

Application Android personnelle. Branche isolée `coachlink-one-2027` afin de préserver CoachLink V0.5.

Fonctions V1 : accueil local par PIN à six chiffres, profil/objectif et décompte, IMC, courbe des poids, mensurations, nutrition (repas, encas, shakers) et calcul des besoins, programmation sportive et chronomètre de repos.

**Partage entre amis** : partage manuel via Android Sharesheet et import d'un code dans l'application du destinataire. Pas de comptes cloud ni de synchronisation temps réel. Les données ne sont pas envoyées à un serveur ; le code exporte uniquement prénom, type d'objectif, poids cible et date cible.

**Vie privée** : les mesures et repas restent dans la base locale Room. PIN dérivé par PBKDF2+sel et protégé contre des tentatives répétées en session. Ce n'est pas une authentification cloud et les données ne sont pas chiffrées individuellement dans la base. Désinstaller l'application efface les données locales. Sauvegarde Android désactivée.

Les besoins énergétiques utilisent Mifflin–St Jeor et des facteurs d'activité déclaratifs, réservés aux adultes pour cette version. Ils constituent des estimations. L'IMC ne sépare pas muscle et masse grasse. Aucune recommandation médicale ni prescription sportive clinique.

Build : `gradle :app:testDebugUnitTest :app:assembleDebug`. Workflow GitHub Actions `android-one-debug.yml` sur la branche indiquée. Package : `com.coachlink.one` — s'installe indépendamment de l'ancien CoachLink.

## V1.1 — Nutrition & objectifs sportifs multiples
- Recherche de produits emballés par nom via Open Food Facts (réseau requis), choix d'un produit puis recalcul automatique des kcal/macros dès modification des grammes. Les valeurs restent vérifiables et éditables. Les aliments déjà saisis sont réutilisables hors ligne.
- Limitation conservatrice des appels au catalogue à un appel toutes les 7 secondes sur cet appareil.
- Objectifs sportifs multiples, hebdomadaires ou mensuels, basés sur les séances enregistrées, les minutes et les kcal déclarées.
- Après séance: saisie de la durée, des kcal relevées (montre/appareil) ou estimation facultative à partir du poids et de MET. Les estimations sont approximatives et ne sont pas automatiquement déduites des calories consommées.
- Migration Room 1 vers 2: conserve les anciens repas, mesures, programmes, performances et amis.
- Recherche Open Food Facts sous sa licence ODbL, attribution et conditions sur openfoodfacts.org. Pas d'identification de produits maison fiable à partir d'un seul nom.


## CoachLink One V1.2 — Planning et entraînement en direct

- Construis plusieurs séances datées à l'avance, puis duplique-les pour un autre jour.
- Chaque séance comporte des blocs Force/Échauffement/AMRAP/EMOM/For Time/Tabata.
- Chaque bloc contient des mouvements (séries, répétitions, charges, repos).
- Démarre une séance, marque les séries réellement réalisées ou passées, avance bloc par bloc.
- Le chronomètre général et le compteur de bloc sont enregistrés en base Room et reprennent après fermeture de l'interface.
- La pause gèle les temps et le repos ; un retour en cours de séance conserve les saisies.
- Le bilan final enregistre les calories de la montre si indiquées, sinon « non renseignées » ; aucune déduction automatique du journal nutritionnel.
- Anciennes pesées, repas, entraînements V1.1 conservés par migration de base Room 2→3.
- Cette version debug « Preview » reste séparée d'une installation de production, avec une base distincte.

**Limites du prototype** : timers mis à jour visuellement lorsque l'app est au premier plan (horloge persistante si elle est quittée) ; notifications sur écran verrouillé et intégration montre non encore implémentées. On ne déduit pas automatiquement des records estimés ou des calories d'une séance.
