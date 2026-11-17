# Oncology case scenarios

Generate deterministic, longitudinal FHIR R4 case files for testing oncology
views and workflows:

```sh
./gradlew oncologyCases -Poutput=output/oncology -Pcount=27 -Pseed=42 -PasOf=2025-06-30
```

The output directory must be empty. Each patient has a separate collection
Bundle; `manifest.json` records the model, seed, snapshot, scenario, complexity
and resource count. Identical inputs reproduce identical bytes and identifiers.
Changing the scenario, complexity or snapshot creates a separate case identity.

`-Pscenario` accepts `mixed`, `uncomplicated`, `awaiting-pathology`,
`delayed-molecular`, `toxicity`, `recurrence`, `discordant-markers`,
`declined-treatment`, `lost-followup` and `partial-dates`.
`-Pcomplexity` accepts `mixed`, `simple`, `standard` and `complex`.
The first 27 mixed cases cover every scenario/complexity combination; larger
cohorts vary demographics, intervals, receptor findings and measurements by seed.

This mode simulates breast oncology case workflows. It is separate from the
population generator and its prevalence model: it intentionally samples rare
and incomplete cases rather than estimating their frequency. It does not add
contradictory histories to an existing population record. Other gynecological
tumor entities and guideline-specific treatment selection are not modeled here.

Journeys include diagnosis, specimen-specific pathology and receptors, staging,
imaging, ECOG, tumorboards, treatment courses, cycle-level observations,
follow-up and tasks with dependencies or completion evidence. Complex journeys
include comorbidity and additional cycles; recurrence has a new biopsy and
another treatment course. Toxicity interrupts the course. Pending pathology
does not produce a confirmed diagnosis or treatment. Partial historical dates
remain partial. No event occurs after the selected snapshot.

The scenarios are software test models, not clinically reviewed treatment
protocols. Procedures describe treatment events without inventing prescriptions
or doses. Breast cancer and receptor codes reuse the existing breast cancer
modules; the workflow uses standard FHIR resources and the application
CodeSystems supplied with the workbench, without claiming conformance to an oncology profile.

Run the model/export tests with:

```sh
./gradlew test --tests org.mitre.synthea.export.OncologyCaseExporterTest
```

These tests check reproducibility, causal ordering, snapshot boundaries, distinct
specimens, treatment interruptions and complete references across all scenario
and complexity combinations. Validate exported files against FHIR R4 separately.
