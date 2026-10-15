import os
from unittest.mock import patch
from fastapi.testclient import TestClient
from backend.app import app, CoachingReview

client = TestClient(app)
VALID = {
    "objective": "Force", "weak_muscle": "Dos", "sessions_per_week": 4,
    "calories_target": 2450, "protein_target": 160, "recent_nutrition_days": 4,
    "average_protein": 130,
    "workouts": [{"name": "Squat", "muscle": "Jambes", "days_ago": 1,
                  "sets": 4, "reps": 6, "rpe": 8, "pain": False}],
}


def test_coach_requires_key():
    with patch.dict(os.environ, {}, clear=True):
        assert client.post("/v1/coach/review", json=VALID).status_code == 503


def test_coach_rejects_invalid_rpe():
    invalid = {**VALID, "workouts": [{**VALID["workouts"][0], "rpe": 25}]}
    with patch.dict(os.environ, {"OPENAI_API_KEY": "dummy"}):
        assert client.post("/v1/coach/review", json=invalid).status_code == 422


def test_coach_review_mocked():
    output = CoachingReview(overview="Reprise progressive", training="Haut du corps léger",
                            nutrition="Vérifier les repas manquants", follow_up="Renseigner le RPE")
    with patch.dict(os.environ, {"OPENAI_API_KEY": "dummy"}), patch(
        "backend.app.generate_coach_review", return_value=output
    ):
        response = client.post("/v1/coach/review", json=VALID)
        assert response.status_code == 200
        assert response.json()["training"] == "Haut du corps léger"


def test_coach_rejects_too_many_workouts():
    invalid = {**VALID, "workouts": VALID["workouts"] * 36}
    with patch.dict(os.environ, {"OPENAI_API_KEY": "dummy"}):
        assert client.post("/v1/coach/review", json=invalid).status_code == 422
