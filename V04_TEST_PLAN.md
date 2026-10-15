# CoachLink V0.4 — Recette Android

1. Dans Android Studio, ouvrir `CoachAthleteAndroid`, synchroniser et lancer un appareil Android 8+.
2. Ouvrir **Coach IA**, choisir **Sport : Coach IA**, **Nutrition : coach humain** ; choisir *Hypertrophie*, point faible *Pectoraux*, enregistrer.
3. Sans entraînement, vérifier que les pectoraux peuvent être proposés. Cliquer **+ J'ai fait une séance hors programme**, saisir `Développé couché`, pectoraux, 5 séries, 8 reps, RPE 9, hier.
4. Vérifier que **la séance suivante n'a pas les pectoraux** comme groupe principal et affiche la prise en compte de l'effort récent.
5. Supprimer la séance libre : vérifier le recalcul. Enregistrer un nouvel exercice avec douleur pour vérifier l'avertissement et l'exclusion de la zone.
6. Valider la proposition : vérifier son apparition dans **Sport**, pouvoir la noter après entraînement.
7. Passer **Sport : coach humain** : depuis l'espace athlète, créer un programme et un exercice, puis enregistrer des performances. Le coach IA ne doit pas appliquer automatiquement ses propres propositions.
8. Passer **Nutrition : coach humain** : vérifier les objectifs nutritionnels modifiables et la conservation des valeurs.
9. Renseigner plusieurs aliments dans **Nutrition**, puis aller dans **Courses**. Cliquer *Reprendre mes aliments des 7 derniers jours* et valider : vérifier le jour, le poids par portion et la liste hebdomadaire.
10. Sélectionner **Mois** et vérifier le total d'un aliment présent chaque lundi en le multipliant par le nombre de lundis réels du mois ; vérifier que les quantités de deux repas homonymes sont additionnées.
11. Ajouter manuellement un aliment au menu, puis le supprimer ; vérifier la régénération.
12. Dans le tableau de bord, vérifier les graphiques conservés de la V0.3.
13. Si le backend **sécurisé** est configuré : vérifier que l'analyse IA en ligne nécessite une coche explicite, et que la photo IA est toujours disponible.
14. Tester la mise à jour depuis V0.3 sans désinstaller l'appli : repas, programmes, séances et pesées doivent rester présents (migration Room).

Tests automatisés hors Android : `bash tools/verify_adaptive_coach.sh`, `bash tools/verify_weekly_logic.sh`, `python -m pytest -q backend/tests`.
