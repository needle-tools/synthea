package org.mitre.synthea.engine;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mitre.synthea.engine.OncologyScenario.Event;
import org.mitre.synthea.engine.OncologyScenario.Journey;
import org.mitre.synthea.engine.OncologyScenario.Scenario;

/** Clinical acceptance contract for the supported archetypes; see doc/oncology-scenarios.md. */
public final class OncologyClinicalValidator {
  private OncologyClinicalValidator() { }

  /** Reject a scenario before any clinical record is exported. */
  public static void requireValid(Journey journey) {
    List<String> issues = validate(journey, journey.events());
    if (!issues.isEmpty()) {
      throw new IllegalStateException("Clinical scenario rejected: " + String.join("; ", issues));
    }
  }

  /** Also accepts candidate events so negative clinical controls can exercise the same gate. */
  public static List<String> validate(Journey journey, List<Event> events) {
    List<String> issues = new ArrayList<>();
    Map<String, Event> byKey = new HashMap<>();
    for (Event event : events) {
      check(byKey.put(event.key(), event) == null, "duplicate-event", issues);
      check(!event.date().isAfter(journey.asOf())
          && event.date().isAfter(journey.birthDate()), "event-date", issues);
    }
    for (Event event : events) {
      if (event.cause() != null) {
        Event cause = byKey.get(event.cause());
        check(cause != null && !cause.date().isAfter(event.date()), "causal-order", issues);
      }
      if (event.sample() != null && !event.kind().equals("specimen")) {
        check(events.stream().anyMatch(sample -> sample.kind().equals("specimen")
            && event.sample().equals(sample.sample()) && !sample.date().isAfter(event.date())),
            "specimen-before-result", issues);
      }
      if (event.kind().equals("medication")) {
        Event order = byKey.get(event.cause());
        check(order != null && order.kind().equals("medication-order")
            && event.value().equals(order.value()) && !order.status().equals("cancelled"),
            "medication-matches-order", issues);
        Event hold = byKey.get("interruption");
        check(hold == null || event.date().isBefore(hold.date()), "toxicity-hold", issues);
      }
      if (event.kind().equals("course")) {
        java.time.LocalDate end = java.time.LocalDate.parse(event.value());
        check(!end.isBefore(event.date()) && !end.isAfter(journey.asOf()), "course-period", issues);
      }
      if (Set.of("medication", "medication-order", "procedure", "course").contains(event.kind())) {
        before(byKey, "pathology", event.key(), "histology-before-treatment", issues);
        before(byKey, "primary-er", event.key(), "receptors-before-treatment", issues);
        before(byKey, "primary-her2", event.key(), "receptors-before-treatment", issues);
      }
      if (event.kind().equals("medication-order") && event.value().equals("letrozole")) {
        check(value(byKey, "primary-er", "Positive") && byKey.containsKey("menopause"),
            "endocrine-eligibility", issues);
        before(byKey, "surgical-pathology", event.key(), "adjuvant-pathology", issues);
      }
      if (event.kind().equals("er") || event.kind().equals("pr")) {
        Event percent = byKey.get(event.key() + "-percent");
        double fraction = percent == null ? Double.NaN : Double.parseDouble(percent.value());
        check(event.value().equals("Low positive")
            ? event.kind().equals("er") && fraction >= 1 && fraction <= 10
            : event.value().equals("Positive")
                ? (event.kind().equals("er") ? fraction > 10 : fraction >= 1) && fraction <= 100
            : event.value().equals("Negative") && fraction >= 0 && fraction < 1,
            "receptor-interpretation", issues);
      }
      if (event.kind().equals("medication") && event.value().equals("trastuzumab")) {
        check(value(byKey, "primary-her2", "IHC 3+"), "her2-eligibility", issues);
        check(recentResult(events, "lvef", event, 92, 50), "cardiac-monitoring", issues);
      }
      if (event.kind().equals("medication") && event.value().equals("docetaxel")) {
        for (String kind : List.of("anc", "platelets")) {
          check(recentResult(events, kind, event, 3, kind.equals("anc") ? 1.5 : 100),
              "pretreatment-counts", issues);
        }
      }
    }
    if (byKey.containsKey("therapy-order")
        && !byKey.get("therapy-order").status().equals("revoked")) {
      List<Event> cycles = events.stream().filter(e -> e.kind().equals("medication")
          && e.value().equals("docetaxel")).sorted(java.util.Comparator.comparing(Event::date))
          .collect(java.util.stream.Collectors.toList());
      boolean held = byKey.containsKey("interruption");
      boolean restarted = journey.scenario() == Scenario.DELAYED_RESTART;
      check(cycles.size() == (held ? 2 : restarted ? 3 : 6), "tchp-course-length", issues);
      check(byKey.get("therapy-order").status().equals(held ? "on-hold"
          : restarted ? "active" : "completed"),
          "course-status", issues);
      for (int i = 0; i < cycles.size(); i++) {
        Event cycle = cycles.get(i);
        if (i > 0) {
          int interval = restarted && i == 2 ? 42 : 21;
          check(ChronoUnit.DAYS.between(cycles.get(i - 1).date(), cycle.date()) == interval,
              "tchp-interval", issues);
        }
        for (String drug : List.of("carboplatin", "trastuzumab", "pertuzumab")) {
          check(events.stream().anyMatch(e -> e.kind().equals("medication")
              && drug.equals(e.value()) && e.date().equals(cycle.date())),
              "tchp-combination", issues);
        }
      }
      if (restarted) {
        before(byKey, "restart-echo", "restart-clearance", "restart-reviewed", issues);
        before(byKey, "restart-clearance", "cycle-3-docetaxel", "restart-reviewed", issues);
        for (Event cycle : cycles) {
          check(events.stream().anyMatch(e -> e.kind().equals("weight")
              && e.date().equals(cycle.date()) && Double.parseDouble(e.value()) > 0),
              "weight-for-infusion", issues);
        }
      } else if (!held && !cycles.isEmpty()) {
        before(byKey, cycles.get(cycles.size() - 1).key(), "surgery",
            "neoadjuvant-before-surgery", issues);
        check(byKey.containsKey("maintenance-order"), "her2-continuation", issues);
      }
    }
    if (byKey.containsKey("surgery")) {
      before(byKey, "surgery", "surgical-pathology", "surgical-pathology-order", issues);
      check(byKey.containsKey("stage-surgery"), "pathological-stage", issues);
      check(byKey.containsKey("diagnosis") && byKey.get("diagnosis").status().equals(
          byKey.containsKey("metastasis") ? "active" : "remission"), "disease-status", issues);
      if (journey.scenario() != Scenario.DELAYED_MOLECULAR) {
        before(byKey, "surgical-pathology", "radiotherapy", "local-adjuvant-treatment", issues);
        check(byKey.containsKey(value(byKey, "primary-er", "Positive")
            ? "endocrine-order" : "maintenance-order"), "systemic-adjuvant-treatment", issues);
      }
    }
    if (byKey.containsKey("metastasis")) {
      before(byKey, "surgery", "recurrence", "recurrence-after-primary-treatment", issues);
      before(byKey, "metastatic-pathology", "metastasis", "metastasis-confirmation", issues);
      check(value(byKey, "stage-recurrence", "M1 (liver)"), "metastatic-stage", issues);
      check(byKey.containsKey("metastatic-er") && byKey.containsKey("metastatic-her2"),
          "metastatic-reassessment", issues);
      check(byKey.containsKey("endocrine-order")
          && byKey.get("endocrine-order").status().equals("stopped"),
          "progression-stops-adjuvant-therapy", issues);
      check(byKey.containsKey("current-review") && events.stream().noneMatch(e ->
          e.kind().equals("medication") && !e.date().isBefore(byKey.get("metastasis").date())),
          "metastatic-decision-pending", issues);
    }
    if (journey.scenario() == Scenario.DISCORDANT_MARKERS) {
      completedBefore(byKey, "adjuvant-ec", "adjuvant-taxane", issues);
      completedBefore(byKey, "adjuvant-taxane", "radiotherapy-start", issues);
      check(value(byKey, "primary-her2", "IHC 1+") && value(byKey, "metastatic-her2", "IHC 3+")
          && pending(byKey, "reference-pathology"), "discordant-pathology-review", issues);
    }
    if (journey.scenario() == Scenario.AWAITING_PATHOLOGY) {
      check(!byKey.containsKey("diagnosis") && events.stream().noneMatch(e ->
          Set.of("medication", "medication-order", "procedure").contains(e.kind())),
          "pending-histology", issues);
    }
    if (journey.scenario() == Scenario.DELAYED_MOLECULAR) {
      check(value(byKey, "primary-er", "Positive") && value(byKey, "primary-her2", "IHC 1+"),
          "genomic-test-eligibility", issues);
      before(byKey, "surgical-pathology", "molecular-order", "genomic-test-context", issues);
      check(events.stream().noneMatch(e -> e.kind().equals("medication")),
          "genomic-decision-pending", issues);
    }
    if (journey.scenario() == Scenario.DECLINED_TREATMENT) {
      check(events.stream().noneMatch(e -> e.kind().equals("medication")),
          "patient-refusal", issues);
    }
    if (journey.scenario() == Scenario.LOST_FOLLOWUP) {
      check(byKey.containsKey("endocrine-order") && byKey.containsKey("followup"),
          "followup-context", issues);
      if (byKey.containsKey("followup")) {
        check(events.stream().noneMatch(e -> e.kind().equals("medication")
            && !e.date().isBefore(byKey.get("followup").date())), "followup-uncertainty", issues);
      }
    }
    validateBoardSnapshot(journey, byKey, issues);
    return List.copyOf(issues);
  }

  private static void validateBoardSnapshot(Journey journey, Map<String, Event> events,
      List<String> issues) {
    String requiredEvidence;
    switch (journey.scenario()) {
      case ER_LOW_REVIEW:
        check(value(events, "primary-er", "Low positive") && events.containsKey("controls")
            && pending(events, "er-review") && value(events, "primary-pr", "Negative")
            && value(events, "primary-her2", "IHC 1+"), "er-low-pathology", issues);
        requiredEvidence = "er-review";
        break;
      case EQUIVOCAL_HER2:
        check(value(events, "primary-her2", "IHC 2+") && pending(events, "ish")
            && events.values().stream().noneMatch(e -> Set.of("course", "medication",
                "medication-order").contains(e.kind())
                || e.kind().equals("her2") && !e.key().equals("primary-her2")),
            "equivocal-awaits-ish", issues);
        requiredEvidence = "ish";
        break;
      case RESIDUAL_TNBC:
      case HEREDITARY_REVIEW:
        check(value(events, "primary-er", "Negative") && value(events, "primary-pr", "Negative")
            && value(events, "primary-her2", "IHC 0"), "triple-negative-pathology", issues);
        completedBefore(events, "taxane", "ec", issues);
        completedBefore(events, "ec", "operation", issues);
        if (journey.scenario() == Scenario.HEREDITARY_REVIEW) {
          check(pending(events, "panel") && events.containsKey("blood")
              && "germline".equals(events.get("panel").sample())
              && events.values().stream().noneMatch(e -> e.kind().equals("report")
                  && "germline".equals(e.sample()) && e.status().equals("final"))
              && value(events, "post-stage", "ypT0 ypN0 cM0"), "hereditary-workup", issues);
          requiredEvidence = "panel";
        } else {
          requiredEvidence = "rcb";
          check(pending(events, "rcb"), "residual-burden-pending", issues);
        }
        break;
      case RESIDUAL_HER2:
      case PULMONARY_RELAPSE:
        check(value(events, "primary-her2", "IHC 3+"), "primary-her2-positive", issues);
        completedBefore(events, "tchp", "operation", issues);
        requiredEvidence = "resection";
        if (journey.scenario() == Scenario.PULMONARY_RELAPSE) {
          completedBefore(events, "continuation", "lung-imaging", issues);
          before(events, "lung-report", "metastatic-board", "biopsy-before-board", issues);
          check(value(events, "lung-her2", "IHC 3+") && value(events, "m1", "M1 (lung)"),
              "lung-recurrence-confirmed", issues);
          check(events.containsKey("payer") && events.containsKey("lung-report")
              && events.values().stream().noneMatch(e -> Set.of("medication", "course")
                  .contains(e.kind()) && !e.date().isBefore(events.get("lung-report").date())),
              "relapse-treatment-proposed", issues);
          requiredEvidence = "payer";
        }
        break;
      case LATE_LIVER_LESION:
        completedBefore(events, "endocrine", "liver-imaging", issues);
        check(value(events, "primary-er", "Positive") && events.containsKey("endocrine")
            && ChronoUnit.DAYS.between(events.get("endocrine").date(),
                java.time.LocalDate.parse(events.get("endocrine").value())) >= 1825,
            "completed-five-year-endocrine-course", issues);
        check(pending(events, "liver-order") && events.values().stream()
            .noneMatch(e -> e.kind().equals("stage") && e.value().contains("M1")
                || e.kind().equals("report") && "liver".equals(e.sample())),
            "suspicious-lesion-unconfirmed", issues);
        requiredEvidence = "liver-order";
        break;
      case NODE_POSITIVE_TCHP:
        check(value(events, "primary-her2", "IHC 3+") && pending(events, "interim"),
            "interim-restaging", issues);
        before(events, "baseline-echo", "first-cycle", "pretreatment-echo", issues);
        requiredEvidence = "interim";
        break;
      default:
        return;
    }
    if (journey.scenario() == Scenario.RESIDUAL_HER2
        || journey.scenario() == Scenario.RESIDUAL_TNBC) {
      check(value(events, "post-stage", "ypT2 ypN1a cM0"), "residual-stage", issues);
      before(events, "response", "operation", "response-before-surgery", issues);
    }
    if (value(events, "stage", "cT2 cN1 cM0")) {
      check(events.containsKey("node-pathology")
          && "axilla".equals(events.get("node-pathology").sample()),
          "node-positive-evidence", issues);
    }
    if (events.containsKey("operation")) {
      before(events, "operation", "resection", "surgical-pathology-order", issues);
      before(events, "resection", "post-stage", "postoperative-staging", issues);
    }
    check(events.containsKey("decision") && requiredEvidence.equals(events.get("decision").cause())
        && "PHYSICIAN".equals(events.get("decision").value()), "board-dependency", issues);
  }

  private static boolean pending(Map<String, Event> events, String key) {
    return events.containsKey(key) && events.get(key).status().equals("active");
  }

  private static void completedBefore(Map<String, Event> events, String course, String next,
      List<String> issues) {
    check(events.containsKey(course) && events.containsKey(next)
        && !java.time.LocalDate.parse(events.get(course).value()).isAfter(events.get(next).date()),
        "completed-course-sequence", issues);
  }

  private static boolean value(Map<String, Event> events, String key, String value) {
    return events.containsKey(key) && value.equals(events.get(key).value());
  }

  private static boolean recentResult(List<Event> events, String kind, Event treatment,
      int maximumAge, double minimumValue) {
    return events.stream().filter(e -> e.kind().equals(kind)
        && !e.date().isAfter(treatment.date()))
        .max(java.util.Comparator.comparing(Event::date))
        .filter(e -> ChronoUnit.DAYS.between(e.date(), treatment.date()) <= maximumAge
            && Double.parseDouble(e.value()) >= minimumValue).isPresent();
  }

  private static void before(Map<String, Event> events, String first, String second,
      String rule, List<String> issues) {
    check(events.containsKey(first) && events.containsKey(second)
        && !events.get(first).date().isAfter(events.get(second).date()), rule, issues);
  }

  private static void check(boolean valid, String rule, List<String> issues) {
    if (!valid && !issues.contains(rule)) {
      issues.add(rule);
    }
  }
}
