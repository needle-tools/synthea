# Breast oncology scenario contract

`oncologyCases` produces deliberately sampled longitudinal breast-cancer cases.
Every simulation and export must pass `OncologyClinicalValidator`; a contradiction
aborts generation. The manifest identifies `oncology-v2` and `breast-scenarios-v2`.
Use an empty output directory when regenerating a cohort.

The clinical scope is two explicit postmenopausal archetypes: ER-positive,
HER2-negative disease treated with surgery and adjuvant endocrine therapy, and
ER-negative, HER2-positive cT2N0 disease receiving neoadjuvant TCHP. These are
selected plausible pathways, not an exhaustive breast-cancer guideline or a
population prevalence model. Triple-negative, ER-low, residual HER2-positive
disease, dose calculation and dose-modification algorithms are not modeled.
Drug administrations name the actual ingredients but do not invent doses.

## Clinical basis

- [AGO early breast cancer recommendations, 2025](https://pmc.ncbi.nlm.nih.gov/articles/PMC11996201/):
  receptor testing informs systemic treatment; an aromatase inhibitor is an
  adjuvant option after menopause. The low-risk ER-positive pathway records
  complete excision, node-negative pathology, radiotherapy, five-year endocrine
  intent, and a bone-health assessment. A larger ER-positive/HER2-negative tumor
  with uncertain chemotherapy benefit has a pending genomic risk assessment;
  it does not receive invented chemotherapy while that decision is pending.
- [eviQ protocol 3736, version 4](https://www.eviq.org.au/medical-oncology/breast/neoadjuvant-adjuvant/3736-breast-neoadjuvant-tchp-docetaxel-carboplati):
  TCHP combines docetaxel, carboplatin, trastuzumab and pertuzumab over six
  three-weekly cycles. The selected responder pathway has surgery four weeks
  after the final cytotoxic cycle, complete pathological response, and continuing
  trastuzumab toward one year total. Cardiac surveillance, blood counts and
  treatment-readiness assessments accompany treatment. Persistent sensory
  neuropathy causes a documented hold and review before further treatment.
- [NICE NG101](https://www.nice.org.uk/guidance/ng101/chapter/Recommendations):
  local and systemic treatment depend on the tumor and patient context;
  breast-conserving surgery is followed by adjuvant radiotherapy in these cases.
- [NICE CG81 pathological reassessment](https://www.nice.org.uk/guidance/cg81/chapter/Diagnosis-and-assessment):
  recurrence can warrant renewed receptor assessment. Liver metastases in these
  cases are biopsy-confirmed, carry M1 staging, and retain specimen-specific
  receptor results. The recurrent cases stop at the metastatic treatment decision,
  rather than administering an unspecified regimen across an unresolved result.

## Executable checks

The gate checks specimen/result chronology, diagnostic prerequisites, regimen
ingredients and intervals, completed versus interrupted courses, treatment holds,
cardiac and hematologic prerequisites, endocrine eligibility, adjuvant treatment,
metastatic staging and reassessment, refusal and pending-result boundaries.
These constraints apply to these archetypes; they are not universal rules forbidding
legitimate individualized care in imported patient records.

Complexity changes additional observations and comorbidity detail, never the
number of cycles required to complete a course. Partial dates reduce recorded
precision without changing the underlying clinical sequence. Prescriptions do
not imply adherence: the lost-follow-up case explicitly records uncertainty.

Tests sweep seeds across all nine scenarios and three depths, mutate otherwise
valid cases to ensure clinically contradictory controls are rejected, and parse
exported FHIR with resolved references. FHIR import validation and browser tests
are separate checks of the representation and its consumers. The clinical checks
are source-based automated verification, not a claim of independent physician
review or a prescribing system.

```sh
./gradlew test --tests '*Oncology*'
./gradlew oncologyCases -Poutput=output/breast-cases -Pcount=27 -Pseed=42 -PasOf=2025-06-30
```
