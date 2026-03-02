# AI Handoff Notes (FHIR Workflow Export)

Purpose: fast context for the next AI agent without re-discovery.

## Current State

Feature work is centered in:
- `src/main/java/org/mitre/synthea/export/FhirR4.java`

Primary docs:
- `FHIR-ADDITIONS.md` (detailed functional documentation)
- `TODO.md` (current status + guardrails)

## What Was Modified and Why

- Added operationally useful FHIR resources and links for real workflow UIs:
  - nurse calendars/queues
  - patient scheduling + reminders
  - discharge coordination
  - tumorboard pre/post planning
- Improved import/replay behavior for standalone-ish history bundles and deterministic updates.
- Fixed validator/runtime issues encountered during generation and targeted validation.

## What Worked

- Compile path is stable: `./gradlew compileJava --console=plain`
- Targeted generation + data inspection reliably validates new resources.
- Small sample + larger sample strategy works for common + rare pathways.

## What Did Not Work / Be Careful

- Full `./gradlew test` is often too slow for this flow and can fail from environment/file locks.
- Some test failures observed were environmental/infra lock related rather than feature logic.
- Temporary validation artifacts can pollute git status if not cleaned.

## Recommended Agent Flow (Do This)

1. Read `TODO.md` and `FHIR-ADDITIONS.md`.
2. Make minimal targeted code edits in `FhirR4.java`.
3. Run compile only:
   - `./gradlew compileJava --console=plain`
4. Generate validation data:
   - small: `./run_synthea.bat -p 3 -s 456 --exporter.fhir.export=true --exporter.fhir.history_export=true --exporter.baseDirectory=./output_validation`
   - larger (if needed): `./run_synthea.bat -p 80 -s 987 --exporter.fhir.export=true --exporter.fhir.history_export=false --exporter.baseDirectory=./output_validation_large`
5. Validate generated resources and references.
6. Clean artifacts before commit.

## Do Not Do (Unless User Explicitly Asks)

- Do not run full long test suite.
- Do not commit generated outputs or temp scripts/logs.
- Do not flip persistent defaults in `synthea.properties` unless requested.

## Open/Deferred Items

- deeper ISiK profile completion and strict profile-level conformance pass
- richer referral lifecycle state machine
- stronger oncology/tumorboard candidate detection logic

## Cleanup Commands (Before Commit)

- Remove temporary outputs and scripts:
  - `Remove-Item -Recurse -Force .\output_validation* -ErrorAction SilentlyContinue`
  - `Remove-Item .\tmp_*.py,.\test_output*.txt -ErrorAction SilentlyContinue`

## Quick Sanity Checks

- `git status --short` contains only intended source/doc files.
- compile succeeds.
- generated bundles contain expected resource types for the scenario you changed.
