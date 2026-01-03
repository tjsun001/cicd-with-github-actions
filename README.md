End-to-End Recommendation System (MLOps Proof of Concept)

Frontend
↓
Spring Boot (product-service)
↓ REST
Inference Service (FastAPI)
↓
Model (local / S3)


This project demonstrates a production-style MLOps architecture focused on reliability, graceful degradation, and operational correctness, rather than model experimentation.

The system integrates a frontend application, a backend API layer, and an ML inference service.
A core design goal is ensuring the system always returns a valid, explainable response, even when machine-learning predictions are unavailable or unreliable.

System Architecture

Components

Frontend (Next.js) – user interface that requests recommendations

Backend API (Spring Boot) – system boundary that owns API contracts, validation, and fallback logic

Inference Service (FastAPI) – isolated ML service responsible for loading models and returning predictions

Database (PostgreSQL) – product data and (optionally) user/event data

Key Principle

The backend acts as the reliability boundary: ML is used when available, but never allowed to violate system guarantees.

(Architecture diagram included in this repository.)

Key Behaviors

Cold-start handling: If ML returns empty results, the backend falls back to deterministic popular items

Graceful degradation: If inference errors or times out, the system still returns a valid response

Explainability: Responses include metadata (source, reason) indicating whether ML or fallback was used

Separation of concerns: UI, backend, and inference services are independently deployable

Repositories

Frontend (Next.js)
UI for requesting and displaying recommendations
👉 link to frontend repo

Backend API (Spring Boot)
Owns API contracts, integrates ML inference, and enforces fallback logic
👉 link to backend repo

Inference Service (FastAPI)
Loads trained model artifacts and serves predictions via REST
👉 link to inference repo

Technology Stack

Java, Spring Boot

Python, FastAPI

Next.js

PostgreSQL

Docker

AWS (deployment & monitoring)

REST APIs

Why This Project Exists

This project was built to explore how ML systems behave in real production environments, including:

sparse data

cold-start users

model uncertainty

downstream service failures

The focus is operational ML, not offline model accuracy.



## Kill-Switch Demo (Inference Resilience)

This service integrates with an external ML inference service for product recommendations.
To demonstrate graceful degradation and service isolation, you can simulate an inference outage.

### Normal operation (ML enabled)
```bash
curl http://localhost:5050/recommendations/1
{
  "user_id": 1,
  "recommendations": [1,2,3,101,102],
  "source": "ml"
}
docker stop inference
curl http://localhost:5050/recommendations/1
{
  "user_id": 1,
  "recommendations": [1,2,3,101,102],
  "source": "fallback_popular"
}
docker start inference
