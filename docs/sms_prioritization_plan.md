# SMS Prioritization Optimization Plan

## Overview
Goal: Prioritize SMS notifications to the most active button pressers, improving real-time engagement while respecting Twilio rate limits.

## PR 1: Integration Test Framework
- Add a robust integration test framework using Testcontainers.
- Launch a Postgres container for tests.
- Run all existing migrations in the test setup.
- Validate that the database is ready for further integration tests.

## PR 2: Database Foundation
- Design and add a new table to store materialized per-contact press counts (e.g., `contact_press_counts`).
- Write and run a Liquibase migration for the new table.
- Implement JDBI entities and DAOs for the new table.
- Add integration tests for the new table using the framework from PR 1.

## PR 3: Daily Materialization Task
- Implement a scheduled task to update the materialized press counts for each contact (e.g., last 90 days).
- Ensure the task is idempotent and efficient.
- Add tests for the task logic.

## PR 4: ContactNotifier Optimization
- Remove the 1000ms delay between SMS sends.
- Update ContactNotifier to sort active contacts by recent press count before sending SMS.
- Add/adjust tests to confirm prioritization and correct notification order.

## Notes
- Each PR should be independently reviewable and testable.
- Update documentation and diagrams as needed with each PR.
- Consider edge cases (e.g., new contacts, contacts with 0 presses, press count ties).
