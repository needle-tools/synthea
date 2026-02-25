# Patient Journey FHIR Resources – Implementation Plan

## Goal
Extend Synthea (fork) to generate **Schedule, Slot, Appointment, ServiceRequest, and Task** FHIR R4 resources so we can build a full calendar and work-planning solution. Output must be compatible with **German ISiK Terminplanung v4** profiles from gematik.

## Design Decisions
| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Generate scheduling resources in **FhirR4.java export phase only** – do NOT modify HealthRecord.java simulation model | Scheduling is a FHIR representation concern, not a simulation concern. Keeps diff small and mergeable. |
| 2 | Map EncounterType → scheduling behaviour: **EMERGENCY/URGENTCARE = unscheduled** (no prior Appointment); everything else = **scheduled** | Real-world: ER visits are walk-in; ambulatory/wellness/outpatient are pre-booked. |
| 3 | One **Schedule per Provider×ServiceType**, one **Slot per Encounter time-range**, one **Appointment per non-emergency Encounter** | Simple 1:1 mapping; no overbooking simulation needed. |
| 4 | **ServiceRequest** per Procedure and per ImagingStudy in an Encounter | These are the orderable items in a breast-cancer pathway. |
| 5 | **Task** per CarePlan activity → tracks fulfilment of ServiceRequests | Gives downstream systems a work-item to schedule/complete. |
| 6 | Use **Flexporter YAML** for ISiK profile decoration (meta.profile, German value-set bindings) | Keeps core generation profile-agnostic; ISiK layer is opt-in. |
| 7 | Use `exporter.fhir.included_resources` / `excluded_resources` for new resource types | Same mechanism Synthea already uses – zero surprise. |

## Encounter-Type Scheduling Matrix
| EncounterType | FHIR class | Scheduled? | Appointment created? |
|---------------|-----------|------------|---------------------|
| WELLNESS      | AMB       | Yes        | ✅ |
| AMBULATORY    | AMB       | Yes        | ✅ |
| OUTPATIENT    | AMB       | Yes        | ✅ |
| INPATIENT     | IMP       | Yes        | ✅ |
| EMERGENCY     | EMER      | No         | ❌ |
| URGENTCARE    | AMB       | No         | ❌ |
| HOSPICE       | HH        | Yes        | ✅ |
| HOME          | HH        | Yes        | ✅ |
| SNF           | IMP       | Yes        | ✅ |
| VIRTUAL       | VR        | Yes        | ✅ |

## Implementation Tasks

### Phase 1 – Configuration & Wiring
- [x] Create this TODO.md
- [x] Register `Schedule`, `Slot`, `Appointment`, `ServiceRequest`, `Task` in the `shouldExport()` mechanism (uses HAPI class resolution, no additional config needed)

### Phase 2 – FHIR Resource Generation (FhirR4.java)
- [x] `schedule()` – create a Schedule resource per Provider referenced in the encounter
- [x] `slot()` – create a Slot linked to the Schedule, covering the encounter time window
- [x] `appointment()` – create an Appointment linked to Slot + Patient + Practitioner; skip for EMERGENCY/URGENTCARE
- [x] `serviceRequest()` – create standalone ServiceRequest per Procedure / ImagingStudy, referencing the Encounter
- [x] `task()` – create Task per CarePlan activity, referencing the ServiceRequest it fulfils
- [x] Wire all new methods into the `convertToFHIR()` encounter loop (after existing resource generation)

### Phase 3 – ISiK Profile Decoration (Flexporter)
- [x] Create `isik_terminplanung.yaml` Flexporter mapping
  - Appointment → `https://gematik.de/fhir/isik/StructureDefinition/ISiKTermin`
  - Schedule   → `https://gematik.de/fhir/isik/StructureDefinition/ISiKKalender`
  - Slot       → `https://gematik.de/fhir/isik/StructureDefinition/ISiKTerminblock`
- [ ] Map ISiK required extensions and value-set bindings (deferred – needs profile-specific testing)

### Phase 4 – Testing
- [x] Verify build succeeds (BUILD SUCCESSFUL, checkstyle passes)
- [x] Verify 11/12 existing FhirR4 tests pass (1 pre-existing failure unrelated to changes)
- [x] Run generation and validate Schedule, Slot, Appointment, ServiceRequest, Task resources present
- [x] Verify cross-references (Appointment→Slot→Schedule, ServiceRequest→Encounter, Task→ServiceRequest)
- [x] Verify EMERGENCY/URGENTCARE encounters have no Appointment (confirmed: 121 encounters, 108 appointments, 13 unscheduled correctly filtered)
- [x] Verify ServiceRequest+Task counts match Procedure+ImagingStudy counts (509 = 479 + 30)
- [ ] Full German data run with synthea-international data files overlaid

## Key File Locations
| File | Purpose |
|------|---------|
| `src/main/java/org/mitre/synthea/export/FhirR4.java` | Main exporter – all new methods go here |
| `src/main/java/org/mitre/synthea/world/concepts/HealthRecord.java` | Encounter model (read-only for us) |
| `src/main/resources/synthea.properties` | Config defaults |
| `synthea-international/de/src/main/resources/synthea.properties` | German overrides |
| `src/main/resources/modules/breast_cancer.json` | Test module (all ambulatory encounters) |

## ISiK Profile URLs (gematik Terminplanung v4)
- `https://gematik.de/fhir/isik/StructureDefinition/ISiKTermin` (Appointment)
- `https://gematik.de/fhir/isik/StructureDefinition/ISiKKalender` (Schedule)
- `https://gematik.de/fhir/isik/StructureDefinition/ISiKTerminblock` (Slot)
- `https://gematik.de/fhir/isik/StructureDefinition/ISiKNachricht` (Communication)
- `https://gematik.de/fhir/isik/StructureDefinition/ISiKMedizinischeBehandlungseinheit` (HealthcareService)
