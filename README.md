# CoachLink One (design 2027) — Android beta

Application Android personnelle. Branche isolée `coachlink-one-2027` afin de préserver CoachLink V0.5.

Fonctions V1 : accueil local par PIN à six chiffres, profil/objectif et décompte, IMC, courbe des poids, mensurations, nutrition (repas, encas, shakers) et calcul des besoins, programmation sportive et chronomètre de repos.

**Partage entre amis** : partage manuel via Android Sharesheet et import d'un code dans l'application du destinataire. Pas de comptes cloud ni de synchronisation temps réel. Les données ne sont pas envoyées à un serveur ; le code exporte uniquement prénom, type d'objectif, poids cible et date cible.

**Vie privée** : les mesures et repas restent dans la base locale Room. PIN dérivé par PBKDF2+sel et protégé contre des tentatives répétées en session. Ce n'est pas une authentification cloud et les données ne sont pas chiffrées individuellement dans la base. Désinstaller l'application efface les données locales. Sauvegarde Android désactivée.

Les besoins énergétiques utilisent Mifflin–St Jeor et des facteurs d'activité déclaratifs, réservés aux adultes pour cette version. Ils constituent des estimations. L'IMC ne sépare pas muscle et masse grasse. Aucune recommandation médicale ni prescription sportive clinique.

Build : `gradle :app:testDebugUnitTest :app:assembleDebug`. Workflow GitHub Actions `android-one-debug.yml` sur la branche indiquée. Package : `com.coachlink.one` — s'installe indépendamment de l'ancien CoachLink.
