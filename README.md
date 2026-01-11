End-to-End Recommendation System (MLOps Proof of Concept)
Frontend (Next.js)
¯
Spring Boot Backend (product-service)
¯ REST (ALB)
FastAPI Inference Service (ECS)
¯
Model Artifacts (S3)
This project demonstrates a production-style MLOps architecture focused on reliability, graceful
degradation, observability, and cloud-native deployment using ECS, Application Load Balancer,
and CloudWatch.
System Architecture
Components:
- Frontend (Next.js)
- Backend API (Spring Boot)
- Inference Service (FastAPI on ECS)
- Database (PostgreSQL)
- Model Storage (S3)
- Monitoring (CloudWatch)
Key Metrics:
- RequestCountPerTarget
- TargetResponseTime
- HealthyHostCount
- HTTPCode_Target_4XX_Count
Key Principle:
The backend is the reliability boundary. ML is optional; system correctness is not.
Kill-Switch Demo:
curl http://localhost:5050/recommendations/1 -> source: ml
docker stop inference
curl http://localhost:5050/recommendations/1 -> source: fallback_popular
