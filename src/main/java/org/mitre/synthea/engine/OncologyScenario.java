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
    RECURRENCE, DISCORDANT_MARKERS, DECLINED_TREATMENT, LOST_FOLLOWUP, PARTIAL_DATES
  }

  public enum Complexity { SIMPLE, STANDARD, COMPLEX }

  public static final class Event {
    private final String key;
    private final String kind;
    private final LocalDate date;
    private final String status;
    private final String description;
    private final String sample;
    private final String value;
    private final String cause;

    private Event(String key, String kind, LocalDate date, String status,
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
    private final Complexity complexity;
    private final LocalDate asOf;
    private final LocalDate birthDate;
    private final List<Event> events;

    private Journey(long seed, int index, Scenario scenario, Complexity complexity,
        LocalDate asOf, LocalDate birthDate, List<Event> events) {
      this.seed = seed;
      this.index = index;
      this.scenario = scenario;
      this.complexity = complexity;
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

    public Complexity complexity() {
      return complexity;
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

  /** Simulate an ordered journey, stopping at a clinically meaningful snapshot. */
  public static Journey simulate(long seed, int index, LocalDate asOf, Scenario scenario,
      Complexity complexity) {
    if (index < 0 || asOf == null || scenario == null || complexity == null) {
      throw new IllegalArgumentException("Index, snapshot, scenario and complexity are required");
    }
    Random random = new Random(seed ^ (0x9e3779b97f4a7c15L * (index + 1)));
    List<Event> events = new ArrayList<>();
    boolean relapsed = scenario == Scenario.RECURRENCE
        || scenario == Scenario.DISCORDANT_MARKERS;
    int horizon = relapsed ? 900 + random.nextInt(400) : 200 + random.nextInt(180);
    if (scenario == Scenario.AWAITING_PATHOLOGY) {
      horizon = 8 + random.nextInt(6);
    } else if (scenario == Scenario.DELAYED_MOLECULAR
        || scenario == Scenario.DECLINED_TREATMENT) {
      horizon = 45 + random.nextInt(10);
    } else if (scenario == Scenario.TOXICITY) {
      horizon = 100 + random.nextInt(20);
    }
    LocalDate onset = asOf.minusDays(horizon);
    LocalDate biopsy = onset.plusDays(5);
    add(events, "presentation", "assessment", onset, "completed",
        "New breast finding; diagnostic assessment requested", null, null, null);
    add(events, "biopsy", "specimen", biopsy, "available",
        "Primary breast biopsy", "primary", null, null);
    add(events, "pathology-order", "request", biopsy, "active",
        "Histology and receptor assessment", "primary", null, null);
    if (scenario == Scenario.AWAITING_PATHOLOGY) {
      add(events, "pathology", "report", biopsy, "registered",
          "Histology requested; report not yet available", "primary", null, "pathology-order");
      add(events, "review-pathology", "task", asOf, "on-hold",
          "Obtain pathology before treatment planning", null, null, "pathology-order");
      return journey(seed, index, asOf, scenario, complexity, random, events);
    }
    LocalDate diagnosis = biopsy.plusDays(4 + random.nextInt(5));
    add(events, "diagnosis", "condition", diagnosis, "active",
        "Malignant neoplasm of breast", null, null, null);
    add(events, "pathology", "report", diagnosis, "final",
        "Invasive breast carcinoma; primary specimen", "primary", null, "pathology-order");
    boolean erPositive = random.nextBoolean();
    String her2 = scenario == Scenario.DISCORDANT_MARKERS ? "IHC 1+"
        : random.nextBoolean() ? "IHC 3+" : "IHC 1+";
    markers(events, "primary", diagnosis, erPositive, her2);
    add(events, "primary-histology", "histology", diagnosis, "final", "histology_type",
        "primary", "Invasive breast carcinoma NST", null);
    add(events, "primary-grade", "histology", diagnosis, "final", "grade",
        "primary", "G" + (1 + random.nextInt(3)), null);
    add(events, "stage", "stage", diagnosis.plusDays(3), "final",
        "Clinical TNM assessment", null,
        random.nextBoolean() ? "cT2 cN0 cM0" : "cT2 cN1 cM0", null);
    add(events, "ecog-baseline", "ecog", diagnosis, "final",
        "ECOG performance status", null, Integer.toString(random.nextInt(2)), null);
    add(events, "staging-imaging", "imaging", diagnosis.plusDays(3), "final",
        "Baseline staging: no distant metastasis identified", null, null, null);
    add(events, "board-initial", "board", diagnosis.plusDays(7), "completed",
        "Multidisciplinary review of diagnosis and patient preferences", null, null, "pathology");
    add(events, "review-pathology", "task", diagnosis.plusDays(7), "completed",
        "Pathology reviewed for initial tumorboard", null, null, "pathology");
    if (scenario == Scenario.DELAYED_MOLECULAR) {
      add(events, "molecular-order", "molecular", diagnosis.plusDays(8), "active",
          "Additional molecular analysis requested; external result delayed",
          "primary", null, null);
      add(events, "review-molecular", "task", asOf.minusDays(7), "on-hold",
          "Contact laboratory and review the outstanding molecular result",
          null, null, "molecular-order");
      add(events, "board-pending", "board", asOf, "draft",
          "Reassess plan once the outstanding result is available", null, null, "molecular-order");
      return journey(seed, index, asOf, scenario, complexity, random, events);
    }
    if (scenario == Scenario.DECLINED_TREATMENT) {
      add(events, "therapy-order", "therapy-request", diagnosis.plusDays(10), "revoked",
          "Proposed systemic treatment declined after discussion", null, null, "board-initial");
      add(events, "preference-review", "task", asOf, "requested",
          "Arrange another discussion of preferences and supportive care",
          null, null, "therapy-order");
      return journey(seed, index, asOf, scenario, complexity, random, events);
    }
    LocalDate treatment = diagnosis.plusDays(21);
    int cycles = complexity == Complexity.SIMPLE ? 2 : complexity == Complexity.STANDARD ? 4 : 6;
    int completedCycles = scenario == Scenario.TOXICITY ? 2 : cycles;
    add(events, "therapy-order", "therapy-request", treatment.minusDays(3),
        scenario == Scenario.TOXICITY ? "on-hold" : "completed",
        "Systemic therapy course", null, null, "board-initial");
    for (int cycle = 0; cycle < completedCycles; cycle++) {
      LocalDate day = treatment.plusDays(cycle * 21L);
      add(events, "cycle-" + (cycle + 1), "therapy", day, "completed",
          "Systemic therapy administration, cycle " + (cycle + 1), null, null, "therapy-order");
      if (complexity != Complexity.SIMPLE) {
        add(events, "ecog-cycle-" + cycle, "ecog", day, "final",
            "ECOG performance status", null, Integer.toString(random.nextInt(2)), null);
        add(events, "hemoglobin-cycle-" + cycle, "hemoglobin", day, "final",
            "Hemoglobin", null, Double.toString(10.0 + random.nextInt(41) / 10.0), null);
      }
    }
    if (scenario == Scenario.TOXICITY) {
      LocalDate interruption = treatment.plusDays(30);
      add(events, "toxicity", "problem", interruption, "active",
          "Treatment-associated peripheral neuropathy", null, null, "cycle-2");
      add(events, "interruption", "assessment", interruption, "completed",
          "Treatment interrupted after neuropathy; reassessment required", null, null, "toxicity");
      add(events, "toxicity-review", "task", asOf.minusDays(2), "on-hold",
          "Review toxicity and treatment continuation before next cycle", null, null, "toxicity");
      add(events, "ecog-current", "ecog", asOf, "final",
          "ECOG performance status", null, "2", null);
      return journey(seed, index, asOf, scenario, complexity, random, events);
    }
    LocalDate surgery = treatment.plusDays(cycles * 21L + 21);
    add(events, "surgery", "procedure", surgery, "completed",
        "Breast surgery", null, null, null);
    add(events, "surgical-sample", "specimen", surgery, "available",
        "Surgical breast specimen", "surgical", null, null);
    add(events, "surgical-pathology", "report", surgery.plusDays(7), "final",
        "Resection specimen reviewed; margins documented", "surgical", null, null);
    add(events, "board-postop", "board", surgery.plusDays(14), "completed",
        "Postoperative review and follow-up planning", null, null, "surgical-pathology");
    if (complexity == Complexity.COMPLEX) {
      add(events, "comorbidity", "problem", onset.minusDays(60), "active",
          "Type 2 diabetes considered during treatment planning", null, null, null);
      add(events, "supportive-review", "task", surgery.plusDays(15), "completed",
          "Supportive care and comorbidity review documented", null, null, "board-postop");
    }
    if (relapsed) {
      LocalDate recurrence = asOf.minusDays(80 + random.nextInt(30));
      add(events, "recurrence", "assessment", recurrence, "completed",
          "New liver lesion during follow-up; recurrence assessment", null, null, null);
      add(events, "recurrence-imaging", "imaging", recurrence, "final",
          "New liver lesion on follow-up imaging; tissue confirmation requested", null, null, null);
      add(events, "metastatic-sample", "specimen", recurrence.plusDays(7), "available",
          "Liver lesion biopsy", "metastatic", null, null);
      add(events, "metastatic-pathology", "report", recurrence.plusDays(14), "final",
          "Metastatic breast carcinoma in liver biopsy", "metastatic", null, null);
      markers(events, "metastatic", recurrence.plusDays(14), erPositive,
          scenario == Scenario.DISCORDANT_MARKERS ? "IHC 3+" : her2);
      add(events, "metastasis", "problem", recurrence.plusDays(14), "active",
          "Liver metastasis from breast carcinoma", null, null, "metastatic-pathology");
      add(events, "stage-recurrence", "stage", recurrence.plusDays(14), "final",
          "Distant metastasis assessment", null, "M1 (liver)", null);
      add(events, "board-recurrence", "board", recurrence.plusDays(21), "completed",
          "Metastatic disease reviewed with current specimen findings",
          null, null, "metastatic-pathology");
      add(events, "second-line-order", "therapy-request", recurrence.plusDays(25), "active",
          "Systemic treatment for metastatic disease", null, null, "board-recurrence");
      for (int cycle = 0; cycle < (complexity == Complexity.COMPLEX ? 3 : 2); cycle++) {
        add(events, "second-line-cycle-" + cycle, "therapy", recurrence.plusDays(28 + cycle * 21L),
            "completed", "Metastatic treatment administration, cycle " + (cycle + 1),
            null, null, "second-line-order");
      }
      add(events, "current-review", "task", asOf, "requested",
          scenario == Scenario.DISCORDANT_MARKERS
              ? "Review discordant HER2 findings across primary and metastatic specimens"
              : "Assess response and tolerability of current therapy",
          null, null, "metastatic-pathology");
    } else {
      add(events, "followup", "task", asOf.minusDays(scenario == Scenario.LOST_FOLLOWUP ? 30 : 0),
          scenario == Scenario.LOST_FOLLOWUP ? "on-hold" : "requested",
          scenario == Scenario.LOST_FOLLOWUP
              ? "Follow-up appointment missed; contact attempts unsuccessful"
              : "Arrange follow-up assessment", null, null, "board-postop");
    }
    return journey(seed, index, asOf, scenario, complexity, random, events);
  }

  private static void markers(List<Event> events, String sample, LocalDate date,
      boolean erPositive, String her2) {
    add(events, sample + "-er", "er", date, "final", "ER", sample,
        erPositive ? "Positive" : "Negative", null);
    add(events, sample + "-pr", "pr", date, "final", "PR", sample,
        erPositive ? "Positive" : "Negative", null);
    add(events, sample + "-her2", "her2", date, "final", "HER2", sample, her2, null);
  }

  private static void add(List<Event> events, String key, String kind, LocalDate date,
      String status, String text, String sample, String value, String cause) {
    events.add(new Event(key, kind, date, status, text, sample, value, cause));
  }

  private static Journey journey(long seed, int index, LocalDate asOf, Scenario scenario,
      Complexity complexity, Random random, List<Event> events) {
    // Discard future events, but retain outstanding orders and tasks at the snapshot.
    events.removeIf(event -> event.date().isAfter(asOf));
    events.sort(java.util.Comparator.comparing(Event::date));
    return new Journey(seed, index, scenario, complexity, asOf,
        asOf.minusYears(40 + random.nextInt(41)).minusDays(random.nextInt(365)),
        Collections.unmodifiableList(events));
  }
}
