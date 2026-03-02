# TODO / Continuation Guide (FHIR Scheduling + Workflow Export)

This file is the **current handoff state** for future AI/dev work in this repo.

## Scope Implemented

Main implementation is in `src/main/java/org/mitre/synthea/export/FhirR4.java`.

### Completed
- Scheduling resources: `Schedule`, `Slot`, `Appointment`
- Clinical order workflow: `ServiceRequest`, `Task`
- Appointment lifecycle simulation with outcomes (`fulfilled`, `cancelled`, `noshow`, `booked`, `waitlist`)
- Appointment reason linking (`Appointment.reasonReference` -> `Condition`)
- History export (transaction bundle sequence + manifest)
- Standalone-friendly reference behavior improvements for clinician references
- Location hierarchy generation: Hospital -> Department -> Ward -> Room -> Bed
- Reminder resources: `CommunicationRequest` for appointment + medication reminders
- Medication operations:
  - scheduled `MedicationAdministration` dose events
  - medication schedule `CarePlan`
- Discharge workflow:
  - discharge `CareTeam`
  - discharge `CarePlan`
  - post-discharge follow-up `Task`
  - post-discharge follow-up `CommunicationRequest`
- Nurse operations view support:
  - handover / medication round / lab review tasks
- Tumorboard support:
  - `CarePlan` v1 (pre-board prep)
  - `CarePlan` v2 (post-board plan)

### Fixed During Iteration
- Pre-1970 booking timestamp issue in history generation (ordering bug)
- Invalid `CommunicationRequest.occurrence[x]` usage (switched to valid type)
- Invalid coding system URI for participation mode (ValueSet -> CodeSystem)
- Invalid `timeOfDay` format (`HH:mm` -> `HH:mm:ss`)
- Duplicate location fullUrl collisions under parallel generation (cache made thread-local)

## Deferred / Postponed

- Full ISiK profile binding completeness via Flexporter (beyond basic compatibility)
- Additional referral lifecycle depth (accepted/rejected/escalated as explicit stages)
- More deterministic tumorboard triggering beyond heuristic condition/procedure text matching
- Full long-running test suite triage (see testing policy below)

## Testing Policy (Important)

## Do this
- `./gradlew compileJava --console=plain`
- generate small samples and validate exported JSON content

## Avoid by default
- full `./gradlew test` (slow and can be flaky/locked in this environment)

Only run full tests if explicitly requested by the user.

## Build / Run / Validate Quick Commands

From repo root (`e:/git/synthea`):

- Compile
  - `./gradlew compileJava --console=plain`

- Generate small validation dataset
  - `./run_synthea.bat -p 3 -s 456 --exporter.fhir.export=true --exporter.fhir.history_export=true --exporter.baseDirectory=./output_validation`

- Generate larger dataset (for rare pathways like tumorboard)
  - `./run_synthea.bat -p 80 -s 987 --exporter.fhir.export=true --exporter.fhir.history_export=false --exporter.baseDirectory=./output_validation_large`

## Validation Checklist

After generation, verify:
- no runtime export exceptions in terminal output
- workflow resources exist in patient bundles:
  - `Task` (handover/med/lab/discharge follow-up)
  - `CommunicationRequest` (reminders/follow-up)
  - `CarePlan` (`Medikationsplan`, discharge plan, tumorboard v1/v2)
- appointment history sequence files are generated when history export is true
- references resolve per intended model:
  - main bundles: no broken internal references
  - history event bundles: may reference patient/shared dependencies from initial event depending on replay mode

## Files to Read First (Future AI)

1. `src/main/java/org/mitre/synthea/export/FhirR4.java`
2. `FHIR-ADDITIONS.md`
3. `README.md` (run options)

## Config Notes

`src/main/resources/synthea.properties`
- `exporter.fhir.history_export` is usually kept `false` by default in source
- prefer enabling via CLI `--exporter.fhir.history_export=true` for ad hoc validation

## Known Environment Gotchas

- PowerShell/Gradle can leave file locks in `build/test-results`; if needed, clean that folder before reruns.
- Large test output files should not be committed.
- Validation helper scripts should be temporary and removed before committing.

## Commit Hygiene

When committing this feature area, include only:
- source changes (`src/main/java/...`)
- relevant documentation (`FHIR-ADDITIONS.md`, this file, handoff docs)

Do not commit:
- generated output folders (`output_validation*`)
- temporary scripts/log files (`tmp_*.py`, `test_output*.txt`)
