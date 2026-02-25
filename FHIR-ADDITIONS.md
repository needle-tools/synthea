# FHIR Scheduling & History Export Additions

This document describes the FHIR R4 resource additions implemented in Synthea for appointment scheduling simulation and temporal history replay.

## Table of Contents

- [Overview](#overview)
- [New FHIR Resources](#new-fhir-resources)
  - [Schedule](#schedule)
  - [Slot](#slot)
  - [Appointment](#appointment)
  - [ServiceRequest](#servicerequest)
  - [Task](#task)
  - [PractitionerRole](#practitionerrole)
- [Appointment Lifecycle Simulation](#appointment-lifecycle-simulation)
- [History Export (Sequential Transaction Bundles)](#history-export-sequential-transaction-bundles)
  - [What Is Standard FHIR vs. Custom](#what-is-standard-fhir-vs-custom)
  - [File Structure](#file-structure)
  - [Bundle Format](#bundle-format)
  - [Manifest](#manifest)
  - [Playback / Replay](#playback--replay)
- [Configuration](#configuration)
- [German (ISiK) Considerations](#german-isik-considerations)
- [Modified Files](#modified-files)

---

## Overview

These additions generate a realistic scheduling workflow around each patient encounter. Instead of only exporting a snapshot of the current state, Synthea can now also export the **full temporal history** of appointment lifecycle changes as a series of FHIR transaction bundles suitable for replaying against a FHIR server.

## New FHIR Resources

All new resources are **standard FHIR R4** with no custom extensions (except Synthea's existing identifier system).

### Schedule

**Resource**: `Schedule` ([FHIR R4 spec](https://hl7.org/fhir/R4/schedule.html))

Created per encounter. Represents the availability window of a practitioner at a location.

| Field | Value |
|-------|-------|
| `active` | `true` |
| `serviceType` | SNOMED code from encounter, or `185349003` (Encounter for check up) |
| `actor` | References to Practitioner and Location |
| `planningHorizon` | Encounter start → end |

### Slot

**Resource**: `Slot` ([FHIR R4 spec](https://hl7.org/fhir/R4/slot.html))

One Slot per encounter, representing the booked time block within a Schedule.

| Field | Value |
|-------|-------|
| `schedule` | Reference to parent Schedule |
| `status` | `busy` |
| `serviceType` | Same as Schedule |
| `start` / `end` | Encounter start / end times |

### Appointment

**Resource**: `Appointment` ([FHIR R4 spec](https://hl7.org/fhir/R4/appointment.html))

The core scheduling resource. Each non-emergency, non-urgent encounter gets an Appointment with a realistic lifecycle status and rich participant information.

| Field | Value |
|-------|-------|
| `status` | One of: `fulfilled`, `cancelled`, `noshow`, `booked`, `waitlist` |
| `serviceType` | SNOMED code from encounter |
| `reasonCode` | From encounter reason (if present) |
| `start` / `end` | Encounter times |
| `minutesDuration` | Computed from encounter duration |
| `slot` | Reference to Slot |
| `created` | 1–30 days before appointment (simulated booking time) |
| `priority` | `3` for inpatient, `5` for routine |
| `cancelationReason` | `pat` (patient request) or `prov` (provider request) — only for cancelled |
| `description` | Service type text |

**Participants** (up to 5 per appointment):

| # | Role (`v3-ParticipationType`) | Actor | Status | Required |
|---|------|-------|--------|----------|
| 1 | `SBJ` (subject) | Patient | `accepted` or `declined` (noshow) | `required` |
| 2 | `PPRF` (primary performer) | Encounter Clinician (Practitioner) | `accepted` | `required` |
| 3 | `ATND` (attender) | Scheduling nurse/admin staff | `accepted` | `information-only` |
| 4 | `REF` (referrer) | Referring practitioner (if applicable) | `accepted` | `information-only` |
| 5 | *(none)* | Location | `accepted` | `required` |

**Cancelled** appointments automatically generate a **rescheduled replacement** appointment shifted 1–14 days later with status `fulfilled`.

### ServiceRequest

**Resource**: `ServiceRequest` ([FHIR R4 spec](https://hl7.org/fhir/R4/servicerequest.html))

Created for Procedures and ImagingStudies — represents the clinical order that was fulfilled.

| Field | Value |
|-------|-------|
| `status` | `completed` |
| `intent` | `order` |
| `code` | From procedure/study code |
| `subject` | Patient |
| `encounter` | Encounter reference |
| `requester` | Encounter clinician |
| `reasonCode` | Clinical reason (if present) |
| `authoredOn` | Time order was placed |

### Task

**Resource**: `Task` ([FHIR R4 spec](https://hl7.org/fhir/R4/task.html))

Represents the workflow fulfilment of a ServiceRequest.

| Field | Value |
|-------|-------|
| `status` | `completed` |
| `intent` | `order` |
| `code` | `fulfill` (from task-code CodeSystem) |
| `focus` | Reference to ServiceRequest |
| `for` | Patient |
| `encounter` | Encounter reference |
| `authoredOn` | Same as ServiceRequest |
| `lastModified` | Encounter end time |
| `requester` | Encounter clinician |
| `owner` | Encounter clinician |

### PractitionerRole

**Resource**: `PractitionerRole` ([FHIR R4 spec](https://hl7.org/fhir/R4/practitionerrole.html))

Created for each clinician involved in scheduling (performer, scheduler, referrer). Maps Synthea specialty strings to SNOMED codes.

| Field | Value |
|-------|-------|
| `active` | `true` |
| `practitioner` | Reference to Practitioner |
| `specialty` | SNOMED-coded specialty from clinician's assigned specialty |

Specialty mapping covers 30+ specialties (e.g., `394802001` General Medicine, `394612005` Urology, `394585009` Obstetrics/Gynecology). If no SNOMED code is found, falls back to text-only.

---

## Appointment Lifecycle Simulation

Each schedulable encounter gets a probabilistic lifecycle outcome:

| Outcome | Probability | Status Flow |
|---------|------------|-------------|
| Fulfilled | 80% | `booked` → `arrived` → `fulfilled` |
| Cancelled + Rescheduled | 8% | `booked` → `cancelled` → new `booked` (rescheduled) |
| No-show | 5% | `booked` → `noshow` |
| Still booked (future) | 5% | `booked` (remains) |
| Waitlisted | 2% | `waitlist` (remains) |

**Excluded encounter types**: Emergency (`EncounterType.EMERGENCY`) and Urgent Care (`EncounterType.URGENTCARE`) are considered unscheduled walk-ins and do not generate scheduling resources.

The lifecycle outcome in the **main bundle** uses `person.rand()` (Synthea's patient RNG stream). The **history export** uses `deterministicRandom()` — a UUID-based hash function — to ensure lifecycle decisions are deterministic for any given encounter UUID, independent of RNG call order. This means the history export may show different specific lifecycle outcomes than the main bundle for the same encounter, but both follow the same statistical distribution. This is by design: the history must be independently reproducible.

---

## History Export (Sequential Transaction Bundles)

When enabled via `exporter.fhir.history_export = true`, Synthea generates a folder of numbered JSON files per patient. Each file is a valid FHIR R4 **transaction bundle** (`Bundle.type = "transaction"`) that can be POSTed to a FHIR server in sequence to reconstruct the patient's full journey over time.

### What Is Standard FHIR vs. Custom

| Aspect | Standard FHIR? | Notes |
|--------|----------------|-------|
| Transaction Bundles | **Yes** | Standard `Bundle.type = "transaction"` with `request.method` POST/PUT |
| Provenance resources | **Yes** | [FHIR R4 Provenance](https://hl7.org/fhir/R4/provenance.html) with `v3-DataOperation` activity codes |
| `meta.versionId` / `meta.lastUpdated` | **Yes** | Standard FHIR resource versioning metadata |
| `request.method` POST/PUT | **Yes** | Standard FHIR transaction semantics |
| `response.etag` / `response.lastModified` | **Yes** | Standard FHIR bundle entry response metadata |
| Appointment status codes | **Yes** | Standard FHIR `AppointmentStatus` value set |
| `cancelationReason` | **Yes** | Standard FHIR R4 field with `appointment-cancellation-reason` CodeSystem |
| Numbered file sequence pattern | **No** | Custom convention (`000_timestamp_label.json`) |
| Per-patient folder structure | **No** | Custom convention (`output/fhir_history/{name}_{id}/`) |
| Manifest bundle | **No** | Custom collection bundle using `Basic` resources with Synthea extensions |
| Synthea extension URLs | **No** | `http://synthetichealth.github.io/synthea/` namespace |

**In summary**: Every individual resource and element is standard FHIR R4. The **composition pattern** (numbered files + manifest + folder structure) is custom. This was a deliberate choice: FHIR's built-in `_history` interaction returns all versions in a single Bundle, but it's designed for server-side retrieval, not for constructing history from scratch. Our pattern allows sequential replay against any FHIR server using standard POST/PUT operations.

### File Structure

```
output/fhir_history/
└── Gladys682_Medhurst46_d256ed5a-.../
    ├── 000_1960-05-15T07-00-00Z_initial.json          # Patient, Org, Practitioner, etc.
    ├── 001_1960-06-10T08-00-00Z_appointment-booked.json
    ├── 002_1960-07-15T08-00-00Z_patient-arrived.json
    ├── 003_1960-07-15T09-00-00Z_appointment-fulfilled.json
    ├── 004_1973-03-01T08-00-00Z_appointment-booked.json
    ├── 005_1973-03-10T08-00-00Z_appointment-cancelled.json
    ├── 006_1973-03-10T09-00-00Z_appointment-rescheduled.json
    ├── ...
    └── manifest.json
```

**File naming**: `{sequence}_{timestamp}_{label}.json`

- `sequence`: Zero-padded 3-digit index (e.g., `000`, `001`, `042`)
- `timestamp`: ISO 8601 with hyphens instead of colons (filesystem-safe)
- `label`: One of `initial`, `appointment-booked`, `patient-arrived`, `appointment-fulfilled`, `appointment-noshow`, `appointment-cancelled`, `appointment-rescheduled`

### Bundle Format

#### Initial Bundle (000)

The first bundle contains all non-scheduling resources for the patient — the full output of `convertToFHIR()` including Patient, Conditions, Observations, Encounters, Practitioners, Organizations, etc. This is a complete patient record at the simulation's "initial" point.

- `Bundle.type`: `transaction`
- `Bundle.timestamp`: 30 days before the first encounter
- Entry `request.method`: `POST` for all resources

#### Lifecycle Event Bundles (001+)

Each subsequent bundle contains exactly 2 entries:

1. **Appointment** — with updated status, version, and metadata
2. **Provenance** — recording who changed what and why

**Example: Appointment Booked (POST)**
```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "timestamp": "1985-03-01T08:00:00.000+01:00",
  "entry": [
    {
      "resource": {
        "resourceType": "Appointment",
        "id": "3fc047c1-b104-3e7e-...",
        "meta": { "versionId": "1", "lastUpdated": "1985-03-01T08:00:00.000+01:00" },
        "status": "booked",
        "serviceType": [{ "coding": [{ "system": "http://snomed.info/sct", "code": "..." }] }],
        "start": "1985-04-01T08:00:00.000+01:00",
        "end": "1985-04-01T09:00:00.000+01:00",
        "participant": [
          { "type": [{"coding": [{"code": "SBJ"}]}], "actor": {"reference": "urn:uuid:..."}, "status": "needs-action" },
          { "type": [{"coding": [{"code": "PPRF"}]}], "actor": {"reference": "urn:uuid:..."}, "status": "accepted" }
        ]
      },
      "request": { "method": "POST", "url": "Appointment" },
      "response": { "etag": "W/\"1\"", "lastModified": "1985-03-01T08:00:00.000+01:00" }
    },
    {
      "resource": {
        "resourceType": "Provenance",
        "target": [{ "reference": "urn:uuid:Appointment/3fc047c1-..." }],
        "recorded": "1985-03-01T08:00:00.000+01:00",
        "activity": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/v3-DataOperation", "code": "CREATE" }] },
        "agent": [{ "who": { "reference": "urn:uuid:..." } }],
        "reason": [{ "text": "Appointment booked" }]
      },
      "request": { "method": "POST", "url": "Provenance" }
    }
  ]
}
```

**Version progression for a fulfilled appointment:**

| Event | `meta.versionId` | `request.method` | `response.etag` | `Provenance.activity` |
|-------|-------------------|-------------------|-------------------|------------------------|
| Booked | `1` | `POST` | `W/"1"` | `CREATE` |
| Arrived | `2` | `PUT` | `W/"2"` | `UPDATE` |
| Fulfilled | `3` | `PUT` | `W/"3"` | `UPDATE` |

**Version progression for a no-show:**

| Event | `meta.versionId` | `Provenance.reason` |
|-------|-------------------|-----------------------|
| Booked | `1` | "Appointment booked" |
| No-show | `2` | "Patient did not show up" |

**Version progression for a cancellation + reschedule:**

| Event | Resource ID | `meta.versionId` | `Provenance.reason` |
|-------|-------------|-------------------|-----------------------|
| Booked | `{appt-id}` | `1` | "Appointment booked" |
| Cancelled | `{appt-id}` | `2` | "Appointment cancelled: Patient request" |
| Rescheduled | `{new-appt-id}` | `1` | "Rescheduled appointment after cancellation" |

### Manifest

The `manifest.json` is a FHIR `Bundle` of type `collection` that indexes all event files. It provides metadata for consumers to understand the playback sequence without parsing every file.

```json
{
  "resourceType": "Bundle",
  "type": "collection",
  "identifier": { "system": "https://github.com/synthetichealth/synthea", "value": "history-{patient-id}" },
  "meta": {
    "tag": [{ "system": "http://synthetichealth.github.io/synthea/tags", "code": "transaction-sequence" }]
  },
  "total": 85,
  "entry": [
    {
      "resource": {
        "resourceType": "Basic",
        "code": { "text": "Transaction sequence manifest entry" },
        "extension": [
          { "url": "http://synthetichealth.github.io/synthea/sequence-index", "valueInteger": 0 },
          { "url": "http://synthetichealth.github.io/synthea/event-timestamp", "valueInstant": "1960-05-15T..." },
          { "url": "http://synthetichealth.github.io/synthea/event-label", "valueString": "initial" }
        ]
      }
    }
  ]
}
```

**Custom extension URLs** (all under `http://synthetichealth.github.io/synthea/`):

| Extension | Type | Description |
|-----------|------|-------------|
| `sequence-index` | `valueInteger` | Zero-based event index (matches filename prefix) |
| `event-timestamp` | `valueInstant` | When the event occurred |
| `event-label` | `valueString` | Human-readable label (e.g., `appointment-booked`) |

### Playback / Replay

To replay a patient's history against a FHIR server:

1. Read `manifest.json` to understand the sequence
2. POST each numbered bundle file in order (`000`, `001`, `002`, ...) as a FHIR transaction
3. Each POST/PUT creates or updates resources on the server
4. After replaying all bundles, the server will have the full versioned history

```bash
# Example replay script (pseudocode)
for file in output/fhir_history/Patient_*/[0-9]*.json; do
  curl -X POST "$FHIR_SERVER/" \
    -H "Content-Type: application/fhir+json" \
    -d @"$file"
done
```

**Important notes for replay**:
- Bundle 000 (initial) must be posted first — it creates the Patient, Organization, and Practitioner resources referenced by later bundles
- The `urn:uuid:` references in later bundles must be resolved against the server's assigned IDs from the initial bundle
- In practice, a replay tool should map `urn:uuid:` → server-assigned IDs from the initial bundle's response

### Determinism

The history export uses `deterministicRandom(encounterUUID, salt)` — a pure hash function based on `UUID.nameUUIDFromBytes()` — to determine lifecycle outcomes. This means:

- **Same encounter** → **same lifecycle outcome**, always
- Independent of Synthea's RNG stream (no `person.rand()` calls)
- Independent of thread scheduling or generation order
- The hash only depends on the encounter's UUID and a salt string

Note: Synthea's overall patient generation is not fully deterministic across runs (due to `HashMap` iteration order, `UUID.randomUUID()` in provider constructors, and thread scheduling). However, given the same patient data, the history export layer will always produce identical results.

---

## Configuration

Add to `synthea.properties`:

```properties
# Export sequential transaction bundles for appointment lifecycle replay/simulation
# Creates per-patient folders under output/fhir_history/ with numbered JSON files
# Default: false
exporter.fhir.history_export = false
```

**Prerequisites**: The history export requires `exporter.fhir.export = true` (the standard FHIR R4 exporter must be active).

Existing scheduling-related settings:

```properties
# These control whether individual resource types are exported in the main bundle
# (independent of history export):
exporter.fhir.export = true
```

---

## German (ISiK) Considerations

These resources are designed to be compatible with the German **ISiK** (Informationstechnische Systeme im Krankenhaus) profiles:

- **ISiKTermin** (Appointment): Our Appointment includes all required ISiK fields — `status`, `serviceType`, `start`, `end`, `participant` with proper role coding
- **ISiKTerminblock** (Slot): Standard Slot with schedule reference and service type
- **ISiKTerminplan** (Schedule): Standard Schedule with actor references

The Provenance resources use the standard `http://terminology.hl7.org/CodeSystem/v3-DataOperation` system, which is compatible with ISiK's provenance requirements.

### German Provider Data (Sachsen-Anhalt)

The following real-world data was added to the synthea-international `de/` module:

**Hospitals** (in `providers/hospitals.csv`):
- Universitätsklinikum Halle (Saale) — academic tertiary care
- BG Klinikum Bergmannstrost — trauma center
- Diakoniekrankenhaus Halle — community hospital
- Krankenhaus Martha-Maria Halle-Dölau
- Krankenhaus St. Barbara
- Krankenhaus St. Elisabeth und St. Barbara
- HELIOS Klinik Sangerhausen
- HELIOS Klinik Lutherstadt Eisleben
- HELIOS Klinik Hettstedt (newly added)

**Primary Care Specialties** (in `providers/primary_care_facilities.csv`):
- All Halle/Mansfeld hospitals updated with `hasSpecialties=True`
- Realistic clinician counts by specialty (e.g., UKH has 50 nurse practitioners, 25 internal medicine, 20 anesthesiology)

**Insurance (Krankenkassen)** (in `payers/`):
- AOK Sachsen-Anhalt, TK, BARMER, DAK-Gesundheit, IKK gesund plus, Knappschaft, AOK PLUS, Debeka PKV
- Proper GKV plans (income-based, no OOP costs) and one PKV plan (premium-based with deductible)

**Demographics** (in `geography/demographics.csv`):
- Halle (Saale) — population 236,991
- Lutherstadt Eisleben — population 24,198

---

## Additional Workflow Views (Clinical Operations)

The exporter now also emits additional workflow-friendly resources for UI use-cases beyond basic scheduling.

### 1) Nurse Handover View

Added Task-based nursing workflow artifacts per encounter:

- Medication round / adherence task (`Task.code = Drug administration`)
- Lab follow-up task when reports exist (`Task.code = Laboratory data interpretation`)
- Shift handover task (`Task.code = Nursing handover`)

These tasks are linked to `Patient`, `Encounter`, and where applicable to `MedicationRequest` via `Task.basedOn`.

### 2) Discharge Follow-up View

For `INPATIENT` / `SNF` / `HOSPICE` encounters:

- Existing discharge planning `CarePlan` and discharge `CareTeam`
- New post-discharge outreach `Task` (48h follow-up)
- New patient-facing `CommunicationRequest` (48h follow-up instruction)

This supports dashboards tracking pending post-discharge contact and completion state.

### 3) Tumorboard v1/v2 (Pre/Post)

For oncology-like encounters, two linked `CarePlan` resources are emitted:

- `Tumorboard v1 - Vorbereitung`
  - diagnostic prep + case summary activities
- `Tumorboard v2 - Nachbesprechung`
  - treatment decision + therapy preparation activities
  - references v1 via `CarePlan.basedOn`

This provides explicit pre-board and post-board planning objects for MDT/tumorboard workflows.

### 4) Technical Notes

- Location hierarchy cache is now thread-local to avoid duplicate `Location.fullUrl` collisions in parallel generation.
- `CommunicationRequest.medium` uses `CodeSystem/v3-ParticipationMode` (not ValueSet URI).
- `CarePlan.activity.detail.scheduledTiming.repeat.timeOfDay` uses valid FHIR time format (`HH:mm:ss`).

### Validation Snapshot

Validated with compile + generation + data inspection (without running full long test suites):

- Small sample (`-p 3`): nurse handover tasks + discharge follow-up task/communication present
- Larger sample (`-p 80`): tumorboard plans present (`tumorboard_v1: 9`, `tumorboard_v2: 9`)

---

## Modified Files

### Synthea Core (`src/main/java/org/mitre/synthea/export/`)

| File | Changes |
|------|---------|
| `FhirR4.java` | Added scheduling + history export + workflow extensions (location hierarchy, reminders, medication schedule artifacts, discharge workflow, nurse handover tasks, tumorboard v1/v2 careplans), plus FHIR validation fixes and standalone reference improvements |
| `Exporter.java` | Added history export branch (~30 lines) after standard FHIR export |

### Synthea Configuration

| File | Changes |
|------|---------|
| `synthea.properties` | Added `exporter.fhir.history_export = false` |

### synthea-international (`de/src/main/resources/`)

| File | Changes |
|------|---------|
| `geography/demographics.csv` | Added Halle (Saale) and Lutherstadt Eisleben |
| `providers/hospitals.csv` | Updated 8 hospitals, added HELIOS Hettstedt |
| `providers/primary_care_facilities.csv` | Updated 8 facilities with specialties, added HELIOS Hettstedt |
| `payers/insurance_companies.csv` | Expanded from 1 to 8 real Krankenkassen |
| `payers/insurance_plans.csv` | Expanded from 1 to 8 plans (7 GKV + 1 PKV) |
