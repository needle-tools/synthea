package org.mitre.synthea.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Reproducible breast oncology workflow scenarios for testing longitudinal case views.
 * This deliberately sampled scenario model is separate from population prevalence modeling.
 * Timings and branches are test parameters, not treatment recommendations or calibrated rates.
 */
public final class OncologyScenario {
  public enum Scenario {
    UNCOMPLICATED, AWAITING_PATHOLOGY, DELAYED_MOLECULAR, TOXICITY,
    RECURRENCE, DISCORDANT_MARKERS, DECLINED_TREATMENT, LOST_FOLLOWUP, PARTIAL_DATES,
    EQUIVOCAL_HER2, NODE_POSITIVE_TCHP, RESIDUAL_TNBC, RESIDUAL_HER2,
    LATE_LIVER_LESION, PULMONARY_RELAPSE, HEREDITARY_REVIEW, ER_LOW_REVIEW, DELAYED_RESTART
  }

  public static final class Event {
    private final String key;
    private final String kind;
    private final LocalDate date;
    private final String status;
    private final String description;
    private final String sample;
    private final String value;
    private final String cause;

    /** An immutable recorded event, also usable for clinical counterfactual tests. */
    public Event(String key, String kind, LocalDate date, String status,
        String description, String sample, String value, String cause) {
      this.key = key;
      this.kind = kind;
      this.date = date;
      this.status = status;
      this.description = description;
      this.sample = sample;
      this.value = value;
      this.cause = cause;
    }

    public String key() {
      return key;
    }

    public String kind() {
      return kind;
    }

    public LocalDate date() {
      return date;
    }

    public String status() {
      return status;
    }

    public String description() {
      return description;
    }

    public String sample() {
      return sample;
    }

    public String value() {
      return value;
    }

    public String cause() {
      return cause;
    }
  }

  public static final class Journey {
    private final long seed;
    private final int index;
    private final Scenario scenario;
    private final LocalDate asOf;
    private final LocalDate birthDate;
    private final List<Event> events;

    Journey(long seed, int index, Scenario scenario,
        LocalDate asOf, LocalDate birthDate, List<Event> events) {
      this.seed = seed;
      this.index = index;
      this.scenario = scenario;
      this.asOf = asOf;
      this.birthDate = birthDate;
      this.events = events;
    }

    public long seed() {
      return seed;
    }

    public int index() {
      return index;
    }

    public Scenario scenario() {
      return scenario;
    }

    public LocalDate asOf() {
      return asOf;
    }

    public LocalDate birthDate() {
      return birthDate;
    }

    public List<Event> events() {
      return events;
    }
  }

  private OncologyScenario() { }

  /** Sample an independent patient. Ordinary follow-up is intentionally common in view tests. */
  public static Journey simulate(long seed, int index, LocalDate asOf) {
    var selection = new java.util.SplittableRandom(seed ^ (0x9e3779b97f4a7c15L * (index + 1)));
    Scenario[] pathways = Scenario.values();
    Scenario selected = selection.nextInt(4) == 0 ? Scenario.UNCOMPLICATED
        : pathways[selection.nextInt(pathways.length)];
    return simulate(seed, index, asOf, selected);
  }

  /** Generate a patient within an explicitly requested clinical situation. */
  public static Journey simulate(long seed, int index, LocalDate asOf, Scenario scenario) {
    if (index < 0 || asOf == null || scenario == null) {
      throw new IllegalArgumentException("A nonnegative index, snapshot and scenario are required");
    }
    Random random = new Random(seed ^ (0x9e3779b97f4a7c15L * (index + 1)));
    switch (scenario) {
      case EQUIVOCAL_HER2:
      case NODE_POSITIVE_TCHP:
      case RESIDUAL_TNBC:
      case RESIDUAL_HER2:
      case LATE_LIVER_LESION:
      case PULMONARY_RELAPSE:
      case HEREDITARY_REVIEW:
      case ER_LOW_REVIEW:
        return new BreastBoardScenario(asOf, random).simulate(seed, index, scenario);
      default:
        break;
    }
    List<Event> events = new ArrayList<>();
    boolean relapsed = scenario == Scenario.RECURRENCE
        || scenario == Scenario.DISCORDANT_MARKERS;
    final boolean discordant = scenario == Scenario.DISCORDANT_MARKERS;
    int horizon = relapsed ? 900 + random.nextInt(400) : 330 + random.nextInt(40);
    if (scenario == Scenario.AWAITING_PATHOLOGY) {
      horizon = 8 + random.nextInt(6);
    } else if (scenario == Scenario.DELAYED_MOLECULAR) {
      horizon = 75 + random.nextInt(5);
    } else if (scenario == Scenario.DECLINED_TREATMENT) {
      horizon = 45 + random.nextInt(10);
    } else if (scenario == Scenario.TOXICITY) {
      horizon = 100 + random.nextInt(20);
    } else if (scenario == Scenario.DELAYED_RESTART) {
      horizon = 110 + random.nextInt(5);
    }
    LocalDate onset = asOf.minusDays(horizon);
    LocalDate biopsy = onset.plusDays(5);
    // Two explicit treatment archetypes. Receptor status is never sampled independently of care.
    boolean restarting = scenario == Scenario.DELAYED_RESTART;
    boolean her2Positive = scenario == Scenario.TOXICITY || restarting
        || scenario == Scenario.DECLINED_TREATMENT
        || (!relapsed && scenario != Scenario.DELAYED_MOLECULAR
            && scenario != Scenario.LOST_FOLLOWUP && random.nextBoolean());
    boolean erPositive = !her2Positive;
    boolean t2 = her2Positive || discordant || scenario == Scenario.DELAYED_MOLECULAR;
    int lesionSize = t2 ? 22 + random.nextInt(23) : 12 + random.nextInt(8);
    String breast = random.nextBoolean() ? "left" : "right";
    String site = breast + " breast at " + (1 + random.nextInt(12)) + " o'clock";
    add(events, "presentation", "assessment", onset, "completed",
        random.nextBoolean() ? "Recalled after abnormal screening mammogram"
            : "Patient noticed a new palpable breast lump and sought assessment", null, null, null);
    add(events, "menopause", "assessment", onset, "completed",
        "Postmenopausal; natural menopause more than five years before presentation",
        null, null, null);
    boolean diabetes = random.nextInt(5) == 0;
    if (diabetes) {
      add(events, "comorbidity", "problem", onset.minusDays(60), "active",
          "Type 2 diabetes considered during treatment planning", null, null, null);
    }
    add(events, "breast-imaging", "imaging", onset.plusDays(2), "final",
        "Suspicious " + lesionSize + " mm mass in the " + site
            + " on mammography and ultrasound; axilla clinically negative",
        null, null, null);
    add(events, "biopsy", "specimen", biopsy, "available",
        "Core biopsy of suspicious breast lesion", "primary", site, null);
    add(events, "pathology-order", "request", biopsy, "active",
        "Histology and receptor assessment", "primary", null, null);
    if (scenario == Scenario.AWAITING_PATHOLOGY) {
      add(events, "pathology", "report", biopsy, "registered",
          "Histology requested; report not yet available", "primary", null, "pathology-order");
      add(events, "review-pathology", "task", asOf, "on-hold",
          "Obtain pathology before treatment planning", null, null, "pathology-order");
      return journey(seed, index, asOf, scenario, random, events);
    }
    LocalDate diagnosis = biopsy.plusDays(4 + random.nextInt(5));
    add(events, "diagnosis", "condition", diagnosis, "active",
        "Malignant neoplasm of breast", null, null, null);
    add(events, "pathology", "report", diagnosis, "final",
        "Invasive breast carcinoma; primary specimen", "primary", null, "pathology-order");
    String her2 = her2Positive ? "IHC 3+" : "IHC 1+";
    markers(events, "primary", diagnosis, erPositive, her2);
    add(events, "primary-histology", "histology", diagnosis, "final", "histology_type",
        "primary", "Invasive breast carcinoma NST", null);
    add(events, "primary-grade", "histology", diagnosis, "final", "grade",
        "primary", her2Positive || discordant ? "G3" : "G2", null);
    add(events, "stage", "stage", diagnosis.plusDays(3), "final",
        "Clinical TNM assessment", null,
        t2 ? "cT2 cN0 cM0" : "cT1c cN0 cM0", null);
    add(events, "ecog-baseline", "ecog", diagnosis, "final",
        "ECOG performance status", null, Integer.toString(random.nextInt(2)), null);
    add(events, "staging-imaging", her2Positive ? "imaging" : "assessment",
        diagnosis.plusDays(3), her2Positive ? "final" : "completed",
        her2Positive ? "Baseline staging before chemotherapy: no distant metastasis identified"
            : "Clinical staging: no symptoms or signs of distant metastasis; cM0",
        null, null, null);
    add(events, "board-initial", "board", diagnosis.plusDays(7), "completed",
        her2Positive ? "Recommend neoadjuvant TCHP for HER2-positive cT2 disease"
            : "Recommend breast-conserving surgery and sentinel node staging"
                + " for ER-positive disease",
        null, null, "pathology");
    add(events, "review-pathology", "task", diagnosis.plusDays(7), "completed",
        "Pathology reviewed for initial tumorboard", null, null, "pathology");
    if (scenario == Scenario.DECLINED_TREATMENT) {
      add(events, "therapy-order", "therapy-request", diagnosis.plusDays(10), "revoked",
          "Neoadjuvant TCHP declined after informed discussion; no treatment administered",
          null, null, "board-initial");
      add(events, "preference-review", "task", asOf, "requested",
          "Arrange another discussion of preferences and supportive care",
          null, null, "therapy-order");
      return journey(seed, index, asOf, scenario, random, events);
    }
    LocalDate treatment = diagnosis.plusDays(21);
    int cycles = 6;
    int completedCycles = scenario == Scenario.TOXICITY ? 2 : restarting ? 3 : cycles;
    String treatmentStatus = scenario == Scenario.TOXICITY ? "on-hold"
        : restarting ? "active" : "completed";
    if (her2Positive) {
      add(events, "therapy-order", "therapy-request", treatment.minusDays(3),
          treatmentStatus,
          "Neoadjuvant TCHP: six cycles every 21 days", null, null, "board-initial");
      for (String drug : List.of("docetaxel", "carboplatin", "trastuzumab", "pertuzumab")) {
        add(events, "order-" + drug, "medication-order", treatment.minusDays(3),
            treatmentStatus,
            "Neoadjuvant TCHP", null, drug, "board-initial");
      }
      add(events, "lvef-baseline", "lvef", treatment.minusDays(5), "final",
          "Left ventricular ejection fraction", null, "63", null);
      int baselineWeight = restarting ? 55 + random.nextInt(37) : 0;
      for (int cycle = 0; cycle < completedCycles; cycle++) {
        LocalDate day = treatment.plusDays(cycle * 21L + (restarting && cycle == 2 ? 21 : 0));
        if (restarting) {
          add(events, "weight-" + cycle, "weight", day, "final", "Measured body weight",
              null, Integer.toString(baselineWeight - cycle), null);
          if (cycle == 2) {
            add(events, "missed-infusion", "assessment", treatment.plusDays(42), "completed",
                "Cycle three missed during relocation; transfer between treatment centres",
                null, null, null);
            add(events, "restart-clearance", "assessment", day.minusDays(1), "completed",
                "New team reviewed prior infusion dates, clinical status and repeat cardiac"
                    + " assessment; resume TCHP after six weeks since cycle two",
                null, null, "missed-infusion");
            add(events, "restart-echo", "lvef", day.minusDays(2), "final",
                "LVEF before restarting HER2 therapy", null, "61", null);
          }
        }
        add(events, "cycle-" + (cycle + 1), "assessment", day, "completed",
            "TCHP cycle " + (cycle + 1) + ": blood count, renal and liver function acceptable;"
                + " premedication, antiemetics and growth-factor prophylaxis arranged",
            null, null, "therapy-order");
        add(events, "anc-" + cycle, "anc", day.minusDays(1), "final",
            "Absolute neutrophil count", null,
            Double.toString((20 + random.nextInt(36)) / 10.0), null);
        add(events, "platelets-" + cycle, "platelets", day.minusDays(1), "final",
            "Platelet count", null, Integer.toString(150 + random.nextInt(151)), null);
        for (String drug : List.of("docetaxel", "carboplatin", "trastuzumab", "pertuzumab")) {
          add(events, "cycle-" + (cycle + 1) + "-" + drug, "medication", day, "completed",
              "TCHP cycle " + (cycle + 1), null, drug, "order-" + drug);
        }
        if (cycle == 4) {
          add(events, "lvef-cycle-4", "lvef", day.minusDays(2), "final",
              "Left ventricular ejection fraction", null, "61", null);
        }
        add(events, "ecog-cycle-" + cycle, "ecog", day, "final",
            "ECOG performance status", null, cycle < 2 ? "0" : "1", null);
        add(events, "hemoglobin-cycle-" + cycle, "hemoglobin", day, "final",
            "Hemoglobin", null, Double.toString((132 - cycle * 4) / 10.0), null);
      }
    }
    if (restarting) {
      add(events, "restart-review", "task", asOf, "requested",
          "Review tolerance of the reloading infusions and coordinate cycle four",
          null, "PHYSICIAN", "cycle-3-pertuzumab");
      return journey(seed, index, asOf, scenario, random, events);
    }
    if (scenario == Scenario.TOXICITY) {
      LocalDate interruption = treatment.plusDays(30);
      add(events, "toxicity", "problem", interruption, "active",
          "Persistent grade 2 sensory peripheral neuropathy after docetaxel",
          null, null, "cycle-2-docetaxel");
      add(events, "interruption", "assessment", interruption, "completed",
          "Treatment interrupted after neuropathy; reassessment required", null, null, "toxicity");
      add(events, "toxicity-review", "task", asOf.minusDays(2), "on-hold",
          "Review toxicity and treatment continuation before next cycle", null, null, "toxicity");
      add(events, "ecog-current", "ecog", asOf, "final",
          "ECOG performance status", null, "2", null);
      return journey(seed, index, asOf, scenario, random, events);
    }
    LocalDate surgery = her2Positive ? treatment.plusDays((cycles - 1) * 21L + 28)
        : diagnosis.plusDays(21);
    add(events, "surgery", "procedure", surgery, "completed",
        "Breast-conserving surgery with sentinel node biopsy, " + breast + " breast",
        null, null, "board-initial");
    add(events, "surgical-sample", "specimen", surgery, "available",
        "Surgical breast specimen", "surgical", breast + " breast", null);
    boolean involvedMargin = (scenario == Scenario.LOST_FOLLOWUP || scenario == Scenario.RECURRENCE
        || (scenario == Scenario.PARTIAL_DATES && erPositive)) && random.nextBoolean();
    String margin = involvedMargin ? "invasive carcinoma at inked lateral radial margin"
        : "negative margins";
    add(events, "surgical-pathology", "report", surgery.plusDays(7), "final",
        her2Positive ? "No residual invasive or in situ carcinoma;"
            + " negative margins; 0/3 sentinel nodes"
            : (lesionSize + random.nextInt(3) - 1) + " mm invasive carcinoma NST, "
                + (discordant ? "G3; clear margins; 2/3 positive sentinel nodes"
                    : "G2; " + margin + "; 0/3 sentinel nodes"),
        "surgical", null, null);
    add(events, "initial-margin", "histology", surgery.plusDays(7), "final",
        "Radial resection margin", "surgical", involvedMargin ? "Involved" : "Clear", null);
    add(events, "stage-surgery", "stage", surgery.plusDays(7), "final",
        "Postoperative pathological TNM", "surgical", her2Positive ? "ypT0 ypN0 cM0"
            : discordant ? "pT2 pN1a(sn) cM0"
            : scenario == Scenario.DELAYED_MOLECULAR ? "pT2 pN0(sn) cM0" : "pT1c pN0(sn) cM0",
        null);
    if (scenario == Scenario.DELAYED_MOLECULAR) {
      add(events, "molecular-order", "molecular", surgery.plusDays(8), "active",
          "21-gene recurrence score requested to resolve uncertain adjuvant chemotherapy benefit",
          "surgical", null, null);
      add(events, "review-molecular", "task", asOf, "on-hold",
          "Obtain recurrence score before final adjuvant chemotherapy decision",
          null, null, "molecular-order");
      add(events, "board-pending", "board", asOf, "draft",
          "Adjuvant chemotherapy decision pending;"
              + " endocrine therapy and breast radiotherapy planned",
          null, null, "molecular-order");
      return journey(seed, index, asOf, scenario, random, events);
    }
    LocalDate definitiveSurgery = surgery;
    String definitivePathology = "surgical-pathology";
    if (involvedMargin) {
      definitiveSurgery = surgery.plusDays(17 + random.nextInt(8));
      add(events, "margin-review", "board", surgery.plusDays(10), "completed",
          "Discussed involved lateral margin; patient elected re-excision",
          null, null, "surgical-pathology");
      add(events, "re-excision", "procedure", definitiveSurgery, "completed",
          "Re-excision of lateral cavity margin, " + breast + " breast", null, null, "margin-review");
      add(events, "margin-sample", "specimen", definitiveSurgery, "available",
          "Oriented lateral margin re-excision specimen", "margin", breast + " breast", "re-excision");
      definitivePathology = "margin-pathology";
      add(events, definitivePathology, "report", definitiveSurgery.plusDays(5), "final",
          "No residual carcinoma in the re-excised lateral margin; final margins clear",
          "margin", null, null);
      add(events, "final-margin", "histology", definitiveSurgery.plusDays(5), "final",
          "Radial resection margin", "margin", "Clear", null);
    }
    add(events, "board-postop", "board", definitiveSurgery.plusDays(14), "completed",
        her2Positive ? "Pathological complete response; continue trastuzumab to one year total;"
            + " adjuvant breast radiotherapy"
            : discordant ? "Node-positive high-grade disease: adjuvant EC then weekly paclitaxel,"
                + " breast and regional nodal irradiation, followed by letrozole"
            : "Low clinical recurrence risk: no chemotherapy; adjuvant letrozole for five years"
            + " and breast radiotherapy; bone health assessment and antiresorptive discussion",
        null, null, definitivePathology);
    LocalDate radiation = definitiveSurgery.plusDays(discordant ? 214 : 35);
    if (discordant) {
      add(events, "adjuvant-echo", "lvef", surgery.plusDays(23), "final",
          "LVEF before anthracycline treatment", null, "64", null);
      add(events, "adjuvant-ec", "course", surgery.plusDays(28), "completed",
          "Four three-weekly cycles of epirubicin and cyclophosphamide",
          null, surgery.plusDays(91).toString(), "board-postop");
      add(events, "adjuvant-taxane", "course", surgery.plusDays(112), "completed",
          "Twelve weekly doses of paclitaxel", null, surgery.plusDays(189).toString(),
          "adjuvant-ec");
    }
    String field = breast + (discordant ? " breast and regional nodal" : " whole-breast");
    add(events, "radiotherapy-start", "assessment", radiation, "completed",
        "Adjuvant " + field + " radiotherapy commenced with cardiac-sparing technique",
        null, null, "board-postop");
    add(events, "radiotherapy", "procedure", radiation.plusDays(21), "completed",
        "Adjuvant " + field + " radiotherapy course completed", null, null, "board-postop");
    if (erPositive) {
      LocalDate endocrine = discordant ? radiation.plusDays(22) : definitiveSurgery.plusDays(21);
      add(events, "endocrine-order", "medication-order", endocrine,
          relapsed ? "stopped" : "active",
          "Adjuvant endocrine therapy; planned duration five years",
          null, "letrozole", "board-postop");
      add(events, "bone-health", "assessment", endocrine, "completed",
          "Baseline DXA normal; exercise, calcium/vitamin D and antiresorptive options discussed",
          null, null, "endocrine-order");
    } else {
      add(events, "maintenance-order", "medication-order", treatment.plusDays(123), "active",
          "Trastuzumab continuation every 21 days to complete one year"
              + " including neoadjuvant treatment",
          null, "trastuzumab", "board-initial");
      for (LocalDate day = treatment.plusDays(126); !day.isAfter(asOf); day = day.plusDays(21)) {
        add(events, "maintenance-" + day, "medication", day, "completed",
            "Trastuzumab continuation", null, "trastuzumab", "maintenance-order");
      }
      for (LocalDate day = treatment.plusDays(168); !day.isAfter(asOf); day = day.plusDays(84)) {
        add(events, "lvef-" + day, "lvef", day, "final",
            "Left ventricular ejection fraction", null, "60", null);
      }
    }
    if (diabetes) {
      add(events, "supportive-review", "task", definitiveSurgery.plusDays(15), "completed",
          "Supportive care and comorbidity review documented", null, null, "board-postop");
    }
    if (relapsed) {
      LocalDate recurrence = asOf.minusDays(21);
      add(events, "recurrence", "assessment", recurrence, "completed",
          "New right upper-quadrant discomfort; diagnostic liver imaging requested",
          null, null, null);
      add(events, "recurrence-imaging", "imaging", recurrence, "final",
          "New liver lesion on follow-up imaging; tissue confirmation requested", null, null, null);
      add(events, "metastatic-sample", "specimen", recurrence.plusDays(7), "available",
          "Liver lesion biopsy", "metastatic", null, null);
      add(events, "metastatic-pathology", "report", recurrence.plusDays(14), "final",
          "Metastatic breast carcinoma in liver biopsy", "metastatic", null, null);
      markers(events, "metastatic", recurrence.plusDays(14), erPositive,
          scenario == Scenario.DISCORDANT_MARKERS ? "IHC 3+" : her2);
      if (discordant) {
        add(events, "reference-pathology", "investigation", recurrence.plusDays(15), "active",
            "Request reference pathology review of HER2 conversion in liver biopsy",
            "metastatic", null, null);
      }
      add(events, "metastasis", "problem", recurrence.plusDays(14), "active",
          "Liver metastasis from breast carcinoma", null, null, "metastatic-pathology");
      add(events, "stage-recurrence", "stage", recurrence.plusDays(14), "final",
          "Distant metastasis assessment", null, "M1 (liver)", null);
      add(events, "board-recurrence", "board", recurrence.plusDays(21), "completed",
          "Biopsy-confirmed metastatic disease; stop adjuvant letrozole after progression;"
              + " review current receptors and molecular testing"
              + " before selecting metastatic therapy",
          null, null, "metastatic-pathology");
      add(events, "current-review", "task", asOf, "requested",
          scenario == Scenario.DISCORDANT_MARKERS
              ? "Review discordant HER2 findings across primary and metastatic specimens"
              : "Review molecular findings and choose first metastatic treatment"
                  + " after AI progression",
          null, "PHYSICIAN", discordant ? "reference-pathology" : "metastatic-pathology");
    } else {
      add(events, "followup", "task", asOf.minusDays(scenario == Scenario.LOST_FOLLOWUP ? 30 : 0),
          scenario == Scenario.LOST_FOLLOWUP ? "on-hold" : "requested",
          scenario == Scenario.LOST_FOLLOWUP
              ? "Follow-up appointment missed; contact attempts unsuccessful;"
                  + " endocrine adherence unknown"
              : "Arrange follow-up assessment", null, null, "board-postop");
    }
    return journey(seed, index, asOf, scenario, random, events);
  }

  private static void markers(List<Event> events, String sample, LocalDate date,
      boolean erPositive, String her2) {
    add(events, sample + "-er", "er", date, "final", "ER", sample,
        erPositive ? "Positive" : "Negative", null);
    add(events, sample + "-pr", "pr", date, "final", "PR", sample,
        erPositive ? "Positive" : "Negative", null);
    add(events, sample + "-er-percent", "er-percent", date, "final",
        "ER positive tumor nuclei", sample, erPositive ? (sample.equals("metastatic") ? "70" : "90")
            : "0", null);
    add(events, sample + "-pr-percent", "pr-percent", date, "final",
        "PR positive tumor nuclei", sample, erPositive ? (sample.equals("metastatic") ? "20" : "70")
            : "0", null);
    add(events, sample + "-her2", "her2", date, "final", "HER2", sample, her2, null);
  }

  private static void add(List<Event> events, String key, String kind, LocalDate date,
      String status, String text, String sample, String value, String cause) {
    events.add(new Event(key, kind, date, status, text, sample, value, cause));
  }

  private static Journey journey(long seed, int index, LocalDate asOf, Scenario scenario,
      Random random, List<Event> events) {
    if (events.stream().anyMatch(e -> e.key().equals("surgical-pathology"))
        && events.stream().noneMatch(e -> e.key().equals("metastasis"))) {
      events.replaceAll(e -> e.key().equals("diagnosis")
          ? new Event(e.key(), e.kind(), e.date(), "remission", e.description(),
              e.sample(), e.value(), e.cause()) : e);
    }
    events.sort(java.util.Comparator.comparing(Event::date));
    Journey result = new Journey(seed, index, scenario, asOf,
        asOf.minusYears(60 + random.nextInt(16)).minusDays(random.nextInt(365)),
        Collections.unmodifiableList(events));
    OncologyClinicalValidator.requireValid(result);
    return result;
  }
}
