# Oncology case scenarios

Generate deterministic, longitudinal FHIR R4 case files for testing oncology
views and workflows:

```sh
./gradlew oncologyCases -Poutput=output/oncology -Pcount=27 -Pseed=42 -PasOf=2025-06-30
```

Use an empty output directory. Each patient has one FHIR R4 collection Bundle.
The manifest records the seed, snapshot date, clinical scenario and resource count.
The same inputs reproduce the same files. Increasing the count preserves the
first patients byte for byte.

The default mixed cohort samples 18 clinical situations, including routine
adjuvant care, diagnostic uncertainty, interrupted therapy and recurrence.
The sampling frequencies are chosen for testing patient views. Each patient has
one clinical course. For example, the margin finding after surgery determines
whether a second operation and pathology report appear before radiotherapy.
To focus on one situation, pass a name such as `-Pscenario=equivocal-her2`.

The [clinical scenario contract](doc/oncology-scenarios.md) describes the modeled
pathways, dosing, evidence sources and clinical checks.

```sh
./gradlew test --tests '*Oncology*'
```
