# Testing Standards

*This document outlines the testing requirements and methodologies for the project.*

## Unit Testing
- Write unit tests for all business logic (Service layer).
- Use JUnit 5 and Mockito for Java components.

## Integration Testing
- Use Spring Boot Test for testing REST controllers and repository interactions.
- Ensure database interactions are tested against an in-memory database or testcontainers.

## General
- Run tests before any major commit or handoff.
- Aim for high test coverage on critical paths.
