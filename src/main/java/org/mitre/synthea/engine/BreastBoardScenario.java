package org.mitre.synthea.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.mitre.synthea.engine.OncologyScenario.Complexity;
import org.mitre.synthea.engine.OncologyScenario.Event;
import org.mitre.synthea.engine.OncologyScenario.Journey;
import org.mitre.synthea.engine.OncologyScenario.Scenario;

/** Sampled points in a breast tumor board journey. Offsets are days before the snapshot. */
final class BreastBoardScenario {
  private final List<Event> events = new ArrayList<>();
  private final LocalDate snapshot;
  private final Random random;

  BreastBoardScenario(LocalDate snapshot, Random random) {
    this.snapshot = snapshot;
    this.random = random;
  }

  private int daysFromPresentation(Scenario scenario) {
    switch (scenario) {
      case EQUIVOCAL_HER2:
      case ER_LOW_REVIEW:
        return 18;
      case NODE_POSITIVE_TCHP:
        return 40;
      case RESIDUAL_TNBC:
        return 211;
      case RESIDUAL_HER2:
        return 181;
      case LATE_LIVER_LESION:
        return 7 * 365 + random.nextInt(240);
      case PULMONARY_RELAPSE:
        return 5 * 365 + random.nextInt(200);
      case HEREDITARY_REVIEW:
        return 720 + random.nextInt(180);
      default:
        throw new IllegalArgumentException(scenario.name());
    }
  }

  Journey simulate(long seed, int index, Scenario scenario, Complexity depth) {
    boolean young = scenario == Scenario.HEREDITARY_REVIEW || scenario == Scenario.RESIDUAL_TNBC;
    final int age = young ? 34 + random.nextInt(8) : 60 + random.nextInt(13);
    int days = daysFromPresentation(scenario);
    boolean lowEr = scenario == Scenario.ER_LOW_REVIEW;
    boolean hr = scenario == Scenario.EQUIVOCAL_HER2 || scenario == Scenario.LATE_LIVER_LESION;
    String her2 = scenario == Scenario.EQUIVOCAL_HER2 ? "IHC 2+"
        : young ? "IHC 0" : hr || lowEr ? "IHC 1+" : "IHC 3+";
    String side = random.nextBoolean() ? "Left" : "Right";
    int size = hr ? 12 + random.nextInt(8) : 30 + random.nextInt(15);
    int ki67 = hr ? 10 + random.nextInt(11) : 40 + random.nextInt(36);
    put("presentation", "assessment", days, "completed",
        (young ? "Premenopausal" : "Postmenopausal") + " at presentation", null, null, null);
    put("mammogram", "imaging", days, "final", side + " upper outer quadrant mass on mammography,"
        + (hr ? " BI-RADS 4C" : " BI-RADS 5"), null, null, null);
    put("imaging", "imaging", days, "final", side + " breast mass " + size + " mm; "
        + (hr ? "axilla unremarkable" : "suspicious ipsilateral axillary node"), null, null, null);
    tissue("biopsy", "primary", days - 2, side + " breast");
    report("pathology", "primary", days - 6,
        "Invasive breast carcinoma NST, " + (hr ? "G2" : "G3"));
    boolean priorDisease = scenario == Scenario.HEREDITARY_REVIEW
        || scenario == Scenario.LATE_LIVER_LESION;
    put("diagnosis", "condition", days - 6, priorDisease ? "remission" : "active",
        "Malignant neoplasm of breast", null, null, null);
    receptors("primary", days - 6, hr, lowEr, her2);
    if (lowEr) {
      put("controls", "assessment", days - 6, "completed",
          "Weak nuclear ER staining; internal and external controls adequate", null, null, null);
    }
    put("ki67", "percent", days - 6, "final", "Ki-67 proliferation index", "primary",
        Integer.toString(ki67), null);
    if (!hr) {
      tissue("node-biopsy", "axilla", days - 2, side + " axillary lymph node");
      report("node-pathology", "axilla", days - 6, "Metastatic breast carcinoma in clipped node");
      put("systemic-staging", "imaging", days - 7, "final",
          "Baseline systemic staging shows no distant metastasis", null, null, null);
    }
    put("stage", "stage", days - 7, "final", "Clinical TNM", null,
        hr ? "cT1c cN0 cM0" : "cT2 cN1 cM0", null);
    put("ecog", "ecog", days - 7, "final", "ECOG performance status", null, "0", null);
    String question;
    String evidence;
    switch (scenario) {
      case ER_LOW_REVIEW:
        put("er-review", "investigation", 6, "active",
            "Pathology second read of low ER expression before systemic treatment selection",
            "primary", null, null);
        question = "Review ER-low biology and chemotherapy options; discuss the uncertain"
            + " magnitude of endocrine benefit with the patient";
        evidence = "er-review";
        break;
      case EQUIVOCAL_HER2:
        put("ish", "molecular", 7, "active", "Reflex HER2 ISH on equivocal core biopsy",
            "primary", null, null);
        question = "Review the ISH addendum before assigning HER2 status and choosing therapy";
        evidence = "ish";
        break;
      case LATE_LIVER_LESION:
        breastSurgery(days - 35, "18 mm NST, negative margins, 0/3 sentinel nodes", "pT1c pN0(sn)");
        course("rt", days - 68, days - 90, "Adjuvant whole-breast radiotherapy");
        course("endocrine", days - 95, days - 95 - 1826,
            "Completed five years of adjuvant letrozole");
        put("surveillance", "assessment", 30, "completed",
            "New mild elevation of alkaline phosphatase and GGT during follow-up",
            null, null, null);
        put("ct", "imaging", 20, "final", "CT shows a new lesion in liver segment VI;"
            + " no suspicious lung lesions", null, null, null);
        put("liver-imaging", "imaging", 12, "final", "New solitary indeterminate "
            + (18 + random.nextInt(9)) + " mm liver lesion on contrast MRI", null, null, null);
        put("liver-order", "investigation", 8, "active", "Image-guided biopsy of liver lesion",
            null, null, null);
        question = "Arrange liver biopsy and repeat receptors if recurrence is confirmed";
        evidence = "liver-order";
        break;
      default:
        boolean tnbc = young;
        String regimen = tnbc ? "Paclitaxel, carboplatin and pembrolizumab" : "TCHP";
        put("baseline-echo", "lvef", days - 10, "final", "Baseline LVEF", null, "63", null);
        if (scenario == Scenario.NODE_POSITIVE_TCHP) {
          put("initial-board", "board", 11, "completed", "Six cycles of neoadjuvant docetaxel,"
              + " carboplatin, trastuzumab and pertuzumab before surgery",
              null, null, "node-pathology");
          course("first-cycle", 8, 8, "First TCHP cycle administered of six planned");
          put("interim", "investigation", 5, "active",
              "Interval breast and axillary ultrasound after cycle four", null, null, null);
          task("clip-record", 4, "ODOK", "Record location of clipped node for surgical planning",
              "node-pathology");
          task("echo", 3, "CASE_MANAGER", "Schedule surveillance echocardiography",
              "baseline-echo");
          question = "Review response and clipped axillary node before definitive surgery";
          evidence = "interim";
          break;
        }
        if (tnbc) {
          course("taxane", days - 15, days - 92, regimen + " with 12 weekly paclitaxel doses");
          course("ec", days - 99, days - 162, "Four three-weekly cycles of epirubicin,"
              + " cyclophosphamide and pembrolizumab");
        } else {
          course("tchp", days - 20, days - 125, "Six neoadjuvant TCHP cycles");
        }
        boolean residual = scenario == Scenario.RESIDUAL_TNBC || scenario == Scenario.RESIDUAL_HER2;
        int op = days - (tnbc ? 190 : 160);
        put("response", "imaging", op + 9, "final", residual
            ? "Preoperative ultrasound shows partial response, residual mass 25 mm"
            : "Preoperative ultrasound shows no measurable breast mass", null, null, null);
        breastSurgery(op, residual
            ? "Residual invasive carcinoma 24 mm, margins clear, 1/10 positive nodes"
            : "Complete pathological response, margins clear, clipped node negative",
            residual ? "ypT2 ypN1a" : "ypT0 ypN0");
        if (residual) {
          task("radiation-consult", op - 11, "CASE_MANAGER",
              "Coordinate postoperative breast and regional nodal radiation consultation",
              "resection");
          if (tnbc) {
            put("gbrca", "assessment", op - 9, "completed",
                "Germline BRCA1/2 testing reported no pathogenic variant", null, null, null);
            put("rcb", "investigation", op - 10, "active",
                "Request residual cancer burden addendum", "surgical", null, null);
            evidence = "rcb";
            question = "Review residual burden, pembrolizumab continuation, the individual"
                + " role of capecitabine and coordination with radiation oncology";
          } else {
            evidence = "resection";
            question = "Discuss adjuvant trastuzumab emtansine for residual HER2-positive"
                + " invasive disease";
          }
        } else {
          course("rt", op - 30, op - 55, "Adjuvant breast and regional nodal radiotherapy");
          if (tnbc) {
            course("continuation", op - 20, op - 190, "Adjuvant pembrolizumab completed");
            put("counselling", "assessment", 18, "completed",
                "Testing was missed during initial external treatment. Patient reports maternal"
                    + " breast cancer around age 43; genetic counselling and consent documented",
                null, null, null);
            tissue("blood", "germline", 14, "Peripheral blood");
            put("panel", "molecular", 14, "active", "Germline hereditary breast cancer panel",
                "germline", null, null);
            evidence = "panel";
            question = "Review germline result with genetics before discussing family testing";
          } else {
            course("continuation", op - 12, days - 380,
                "Trastuzumab and pertuzumab to complete one year");
            put("lung-imaging", "imaging", 25, "final", "New bilateral pulmonary nodules up to"
                + " 18 mm on chest CT; no additional lesions on abdominal or skeletal staging",
                null, null, null);
            tissue("lung-biopsy", "lung", 15, "Lung");
            report("lung-report", "lung", 9, "Metastatic breast carcinoma in lung biopsy");
            receptors("lung", 9, false, false, "IHC 3+");
            put("m1", "stage", 8, "final", "Metastatic recurrence", null, "M1 (lung)", null);
            put("metastatic-board", "board", 5, "completed",
                "Board proposes trastuzumab, pertuzumab and docetaxel"
                    + " after late HER2-positive relapse",
                null, null, "lung-report");
            task("payer", 4, "CASE_MANAGER", "Confirm funding for transport to infusions",
                "metastatic-board");
            evidence = "payer";
            question = "Coordinate transport and confirm consent before scheduling first infusion";
          }
        }
    }
    put("next-board", "board", 0, "draft", question, null, null, evidence);
    task("decision", 0, "PHYSICIAN", question, evidence);
    if (depth != Complexity.SIMPLE) {
      task("track", 0, "CASE_MANAGER", "Coordinate evidence review with the responsible physician",
          evidence);
    }
    if (depth == Complexity.COMPLEX) {
      String additionalWork = outstandingEvidence(scenario);
      put("source-query", "investigation", 0, "active", additionalWork, null, null, null);
      task("reconcile", 0, "ODOK", "Track the outstanding evidence: " + additionalWork,
          "source-query");
      task("second-read", 0, "PHYSICIAN", "Review the requested evidence before final sign-off",
          "reconcile");
      task("reboard", 0, "CASE_MANAGER",
          "Reserve repeat board review once both reports are available", "second-read");
    }
    String summary = side + " breast NST, " + size + " mm at diagnosis, "
        + (hr ? "cN0, ER-positive" : lowEr ? "biopsy-proven cN1, ER-low positive"
            : "biopsy-proven cN1, ER-negative")
        + ", HER2 " + her2 + ", Ki-67 " + ki67 + "%. " + question + ".";
    put("board-summary", "assessment", 0, "completed", summary, null, null, evidence);
    events.sort(java.util.Comparator.comparing(Event::date));
    Journey result = new Journey(seed, index, scenario, depth, snapshot,
        snapshot.minusDays(days).minusYears(age).minusDays(random.nextInt(365)),
        List.copyOf(events));
    OncologyClinicalValidator.requireValid(result);
    return result;
  }

  private String outstandingEvidence(Scenario scenario) {
    switch (scenario) {
      case HEREDITARY_REVIEW:
        return "Verify the reported age at diagnosis of the affected first-degree relative";
      case LATE_LIVER_LESION:
        return "Obtain older abdominal imaging to compare the indeterminate liver lesion";
      case PULMONARY_RELAPSE:
      case NODE_POSITIVE_TCHP:
      case RESIDUAL_HER2:
        return "Obtain updated cardiac function assessment for the next HER2 therapy review";
      case RESIDUAL_TNBC:
        return "Obtain residual tumor bed dimensions and cellularity from surgical pathology";
      case EQUIVOCAL_HER2:
        return "Confirm tissue adequacy with the reference laboratory for the ISH addendum";
      case ER_LOW_REVIEW:
        return "Retrieve the original stained slides and control images for joint review";
      default:
        throw new IllegalArgumentException(scenario.name());
    }
  }

  private void receptors(String sample, int day, boolean positive, boolean lowEr, String her2) {
    for (String marker : List.of("er", "pr")) {
      String key = sample + "-" + marker;
      boolean low = lowEr && marker.equals("er");
      put(key, marker, day, "final", marker.toUpperCase(java.util.Locale.ROOT), sample,
          low ? "Low positive" : positive ? "Positive" : "Negative", null);
      put(key + "-percent", marker + "-percent", day, "final", marker + " positive nuclei",
          sample, Integer.toString(low ? 1 + random.nextInt(10)
              : positive ? 50 + random.nextInt(48) : 0), null);
    }
    put(sample + "-her2", "her2", day, "final", "HER2", sample, her2, null);
  }

  private void breastSurgery(int day, String finding, String stage) {
    boolean sentinel = stage.startsWith("pT");
    course("operation", day, day, sentinel ? "Breast conservation and sentinel node biopsy"
        : "Breast conservation and targeted axillary surgery including clipped node"
            + (stage.contains("ypN1") ? " with completion axillary dissection" : ""));
    tissue("excision", "surgical", day, "Breast resection and ipsilateral axillary nodes");
    report("resection", "surgical", day - 6, finding);
    put("post-stage", "stage", day - 6, "final", "Resection TNM", "surgical", stage + " cM0", null);
  }

  private void course(String key, int start, int end, String label) {
    put(key, "course", start, "completed", label, null, snapshot.minusDays(end).toString(), null);
  }

  private void tissue(String key, String sample, int day, String site) {
    put(key, "specimen", day, "available", site, sample, site, null);
  }

  private void report(String key, String sample, int day, String conclusion) {
    put(key, "report", day, "final", conclusion, sample, null, null);
  }

  private void task(String key, int day, String role, String description, String cause) {
    put(key, "task", day, "requested", description, null, role, cause);
  }

  private void put(String key, String kind, int day, String status, String description,
      String sample, String value, String cause) {
    events.add(new Event(key, kind, snapshot.minusDays(day), status, description,
        sample, value, cause));
  }
}
