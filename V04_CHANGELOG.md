# CoachLink V0.4 — Coach adaptatif, suivi nutrition et courses

## Livré dans le code

### Sport
- Déclaration d'un **exercice hors programme** (date, nom, groupe musculaire principal, séries, répétitions, difficulté RPE et éventuelle douleur).
- Convergence des activités libres et des performances d'exercices déjà programmés.
- Proposition de séance à chaque changement de l'historique, du point faible, de l'objectif sportif ou des données nutritionnelles.
- Moteur **local explicable** : pénalités de charge récente (notamment RPE élevé), prise en compte des zones impliquées indirectement et des douleurs, priorisation d'un point faible s'il a récupéré, complément tronc/cardio selon besoin, modulation prudente des séries.
- Accepter la proposition l'ajoute à la liste des programmes existants, sans remplacer le coach ni fixer une charge maximale.
- Un programme manuel peut être saisi en mode coach humain, même sur le profil athlète local.

### Accompagnement
- Modes **indépendants** par domaine : sport coach humain / coach IA, nutrition coach humain / coach IA.
- Objectif Force, Hypertrophie, Endurance, Forme générale ou Recomposition ; 1 à 7 séances par semaine ; point faible prioritaire.
- Évolution calculée à la demande à partir des nouveaux journaux d'entraînement / nutrition / pesées. **Ce n'est pas un modèle qui apprend par fine-tuning sur l'utilisateur.**
- Analyse **optionnelle par vrai modèle de langage** : `/v1/coach/review`, via backend HTTPS configuré et consentement avant l'envoi d'un résumé. Sans serveur, le moteur local est utilisable.
- Indicateurs de groupes musculaires sur 28 jours et tendance des deux dernières pesées. Attention, ils ne prouvent pas une faiblesse ou une amélioration.

### Nutrition et courses
- Synthèse des calories et protéines journalières consignées (sur jours renseignés, sans assimiler les journées vides à 0 kcal).
- Conseils prudents selon l'objectif alimentaire, et conservation des objectifs imposés par un coach humain.
- Menu hebdomadaire par aliment, jour et repas ; quantités et portions modifiables via suppression/ajout.
- Bouton **Reprendre mes aliments des 7 derniers jours** : après confirmation, convertit les consommations enregistrées en menu récurrent (les portions d'une analyse photo restent approximatives).
- Liste de courses **semaine calendaire ou mois civil**, quantités agrégées par aliments (en grammes/kilos), coche-paniers éphémères.

### Stockage
- Room V2 -> V3 : tables `athlete_settings`, `extra_workouts`, `meal_plan`, et nouvelle colonne `exercises.muscleGroup`.
- Migration conservatrice 1->2->3. **Ne pas utiliser `fallbackToDestructiveMigration()`**.

## Limites actuelles
- Le moteur local est un système de règles, pas une IA entraînée ; les réponses en langage naturel nécessitent un backend configuré.
- Les menus ne sont pas inventés à partir des macros : l'athlète saisit les aliments ou reprend son historique. La création de recettes adaptatives, la connaissance des allergies, budgets et stocks nécessitent une V0.5.
- Le mode coach / athlète n'est qu'un commutateur de démonstration sur un appareil ; il n'y a ni comptes réels, ni partage sécurisé des informations.
- La liste de courses ne tient pas compte des quantités disponibles à domicile ; les coches ne sont pas sauvegardées.
- Les exercices complexes ou polyarticulaires sont classés selon un muscle principal et des chevauchements simplifiés : revoir manuellement le classement.
- Les résultats de la photo IA demeurent des estimations ; une photo ne mesure pas les grammages.
- Le backend de référence n'a PAS d'authentification ni de quotas : ne pas l'exposer à Internet ni à de vrais clients avant sécurisation.
- Pas de build APK ou de tests d'intégration Android réalisés dans cet environnement.
