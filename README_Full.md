EndntonEnd Recommendation System

ProductionnStyle MLOps Architecture (ECS • ALB • CloudWatch)
A portfolio project demonstrating reliability, observability, and cloudnnative ML serving.

System Architecture

Frontend (Next.js) ® Spring Boot Backend ® Application Load Balancer ® FastAPI Inference
Service (ECS) ® Model Artifacts (S3)
The backend acts as the reliability boundary. Machine learning is optional; system correctness is
not.

Reliability & Resilience
• Graceful degradation with deterministic fallback when ML is unavailable
• Killnswitch demo proving service isolation
• Explainable responses indicating ML vs fallback source

CloudnNative MLOps
• Inference service deployed on Amazon ECS
• Application Load Balancer with health checks and target groups
• Models versioned in Amazon S3 and loaded at container startup

Observability (CloudWatch)
Production monitoring using ALB Target Group metrics:
• RequestCountPerTarget (traffic per task)
• TargetResponseTime (latency)
• HealthyHostCount (service health)
• HTTPCode_Target_4XX_Count (error signals)

Why This Project Matters
This project demonstrates how to operate machine learning systems as production services:
faultntolerant, observable, and safely integrated behind stable APIs. It reflects realnworld MLOps
and platform engineering concerns rather than offline model accuracy alone.
