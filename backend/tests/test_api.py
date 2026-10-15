import os
from unittest.mock import patch
from fastapi.testclient import TestClient
from backend.app import app, MealAnalysis

client = TestClient(app)
FAKE_JPEG = b"\xff\xd8\xff" + b"x" * 200


def test_health():
    assert client.get("/health").json() == {"status": "ok"}


def test_requires_key():
    with patch.dict(os.environ, {}, clear=True):
        assert client.post("/v1/photo-meals/analyze", content=FAKE_JPEG,
                           headers={"content-type": "image/jpeg"}).status_code == 503


def test_rejects_non_jpeg():
    with patch.dict(os.environ, {"OPENAI_API_KEY": "test"}):
        assert client.post("/v1/photo-meals/analyze", content=b"123456", headers={
            "content-type": "image/png"}).status_code == 415


def test_valid_prediction_with_mocked_ai():
    prediction = MealAnalysis.model_validate({
        "foods": [{"name": "Riz blanc", "grams": 150,
                   "calories100": 130, "protein100": 2.5,
                   "carbs100": 28, "fat100": 0.3, "confidence": "low"}],
        "notes": "Photo seule : quantité approximative."
    })
    with patch.dict(os.environ, {"OPENAI_API_KEY": "test"}), patch(
        "backend.app.analyze_image", return_value=prediction
    ):
        response = client.post("/v1/photo-meals/analyze", content=FAKE_JPEG,
                               headers={"content-type": "image/jpeg"})
        assert response.status_code == 200
        assert response.json()["foods"][0]["grams"] == 150


def test_rejects_large_image():
    with patch.dict(os.environ, {"OPENAI_API_KEY": "test"}):
        response = client.post("/v1/photo-meals/analyze", content=FAKE_JPEG,
                               headers={"content-type": "image/jpeg", "content-length": "999999999"})
        assert response.status_code == 413
