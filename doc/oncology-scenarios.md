# Breast oncology scenario contract

`oncologyCases` produces deliberately sampled longitudinal breast-cancer cases.
Every simulation and export must pass `OncologyClinicalValidator`; a contradiction
aborts generation. The manifest identifies `oncology-v3` and `breast-scenarios-v3`.
Use an empty output directory when regenerating a cohort.

Each file stops at a specific board decision. The model deliberately samples
these situations for interface testing. Scenario frequencies have no population
interpretation. The clinical basis is the 2025 treatment context.

| Board situation | Evidence in the file |
| --- | --- |
| Equivocal primary HER2 | IHC 2+ on a specified core and an outstanding reflex ISH order |
| Node-positive HER2-positive disease during TCHP | Clipped biopsy-positive node, baseline cardiac function, first cycle and planned interim ultrasound |
| Postoperative residual triple-negative disease | Weekly taxane and carboplatin with pembrolizumab, then EC with pembrolizumab, preoperative response, residual nodal disease and pending RCB addendum |
| Postoperative residual HER2-positive disease | Six preoperative TCHP cycles, residual invasive disease, radiotherapy referral and adjuvant trastuzumab emtansine discussion |
| Suspected late hepatic recurrence | Five completed years of endocrine therapy, abnormal surveillance labs, imaging and an outstanding biopsy |
| Confirmed pulmonary relapse | Prior pCR, completed HER2-directed course, new lung biopsy with receptors, board recommendation and transport funding request |
| Receptor discordance | Primary HR-positive, HER2-negative tumor and HER2-positive liver biopsy after adjuvant chemotherapy, irradiation and endocrine therapy. Reference pathology review is pending. |
| Young triple-negative survivor | Prior pCR, postoperative treatment, family history, genetic counselling and a pending blood panel |
| ER-low positive disease | A core with 1–10% ER-positive nuclei, PR negativity, valid controls and a pending pathology review before treatment selection |
| Interrupted care during transfer | Three documented TCHP cycles, a six-week interval between the second and third, reassessment and reloading of both IV antibodies with measured weight |

Other sampled decisions cover pending primary pathology, delayed genomic risk
assessment, neuropathy interrupting TCHP, refusal of treatment, lost follow-up,
partial historical dates, concordant metastatic recurrence and routine care.

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
  confirmed metastases retain specimen-specific receptor results. The separate
  unconfirmed liver lesion stops before metastatic staging and treatment.
- [AGO metastatic breast cancer recommendations, 2025](https://pmc.ncbi.nlm.nih.gov/articles/PMC12052354/).
  The lung-relapse scenario uses a long treatment-free interval and repeat
  biopsy before the first-line discussion. The remaining administrative task
  concerns transport to treatment.
- [Femara prescribing information](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=82b77d74-085f-45ac-a7dd-1f5c038bf406)
  and [Perjeta product information](https://www.ema.europa.eu/en/documents/product-information/perjeta-epar-product-information_en.pdf).
  Letrozole orders carry a structured oral dose and frequency. IV pertuzumab
  administrations distinguish the loading dose and subsequent doses. In the
  transfer case, a six-week interval triggers IV reloading for both antibodies.
  The trastuzumab amount uses the weight documented on the infusion day.
  Historical courses carry drug names, intervals or cycle counts when known.
  Individual cytotoxic doses and renal-function-based carboplatin calculations
  are outside the model.
- [ASCO/CAP ER and PgR testing recommendations](https://documents.cap.org/documents/erpgr_summaryrecommendations.pdf).
  The ER-low case reports the staining percentage and controls, retains the
  low-positive interpretation and keeps endocrine benefit open for discussion.

## Executable checks

The gate checks specimen/result chronology, diagnostic prerequisites, regimen
ingredients and intervals, completed versus interrupted courses, treatment holds,
cardiac and hematologic prerequisites, endocrine eligibility, adjuvant treatment,
metastatic staging and reassessment, refusal and pending-result boundaries.
These constraints apply to these archetypes; they are not universal rules forbidding
legitimate individualized care in imported patient records.

Complexity adds observations, comorbidity context or linked review work. In a
complex postoperative case, pathology documentation feeds physician review and
repeat board scheduling. A completed course retains its length. Partial dates reduce recorded
precision without changing the underlying clinical sequence. Prescriptions do
not imply adherence: the lost-follow-up case explicitly records uncertainty.

Tests sweep seeds across all 18 scenarios and three depths, mutate otherwise
valid cases to ensure clinically contradictory controls are rejected, and parse
exported FHIR with resolved references. FHIR import validation and browser tests
are separate checks of the representation and its consumers. The clinical checks
are source-based automated verification, not a claim of independent physician
review or a prescribing system.

```sh
./gradlew test --tests '*Oncology*'
./gradlew oncologyCases -Poutput=output/breast-cases -Pcount=54 -Pseed=42 -PasOf=2025-06-30
```
