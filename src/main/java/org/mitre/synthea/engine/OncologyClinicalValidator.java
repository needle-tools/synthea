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
      if (Set.of("medication", "medication-order", "procedure").contains(event.kind())) {
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
        check(percent != null && (event.value().equals("Positive")
            ? Double.parseDouble(percent.value()) > 10
                && Double.parseDouble(percent.value()) <= 100 : percent.value().equals("0")),
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
      check(cycles.size() == (held ? 2 : 6), "tchp-course-length", issues);
      check(byKey.get("therapy-order").status().equals(held ? "on-hold" : "completed"),
          "course-status", issues);
      for (int i = 0; i < cycles.size(); i++) {
        Event cycle = cycles.get(i);
        if (i > 0) {
          check(ChronoUnit.DAYS.between(cycles.get(i - 1).date(), cycle.date()) == 21,
              "tchp-interval", issues);
        }
        for (String drug : List.of("carboplatin", "trastuzumab", "pertuzumab")) {
          check(events.stream().anyMatch(e -> e.kind().equals("medication")
              && drug.equals(e.value()) && e.date().equals(cycle.date())),
              "tchp-combination", issues);
        }
      }
      if (!held && !cycles.isEmpty()) {
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
    return List.copyOf(issues);
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
