package org.mitre.synthea.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.Test;
import org.mitre.synthea.engine.OncologyScenario.Complexity;
import org.mitre.synthea.engine.OncologyScenario.Event;
import org.mitre.synthea.engine.OncologyScenario.Journey;
import org.mitre.synthea.engine.OncologyScenario.Scenario;

public class OncologyClinicalValidatorTest {
  private static final LocalDate SNAPSHOT = LocalDate.of(2025, 6, 30);

  @Test
  public void seededCohortsMeetTheClinicalContract() {
    for (int seed = 0; seed < 100; seed++) {
      for (Scenario scenario : Scenario.values()) {
        for (Complexity complexity : Complexity.values()) {
          Journey journey = OncologyScenario.simulate(seed, seed % 13, SNAPSHOT,
              scenario, complexity);
          assertEquals(List.of(), OncologyClinicalValidator.validate(journey, journey.events()));
          if (scenario == Scenario.TOXICITY) {
            assertEquals(2, journey.events().stream().filter(e ->
                e.kind().equals("medication") && e.value().equals("docetaxel")).count());
          }
        }
      }
    }
  }

  private Journey journey(Scenario scenario) {
    return OncologyScenario.simulate(42, 0, SNAPSHOT, scenario, Complexity.COMPLEX);
  }

  private void rejects(Journey journey, String rule, UnaryOperator<List<Event>> mutate) {
    List<String> issues = OncologyClinicalValidator.validate(journey,
        mutate.apply(new ArrayList<>(journey.events())));
    assertTrue(rule + ": " + issues, issues.contains(rule));
  }

  private Event change(Event event, LocalDate date, String value) {
    return new Event(event.key(), event.kind(), date, event.status(), event.description(),
        event.sample(), value, event.cause());
  }

  @Test
  public void rejectsBiologicallyIncompatibleTreatment() {
    rejects(journey(Scenario.TOXICITY), "her2-eligibility", events -> {
      events.replaceAll(e -> e.key().equals("primary-her2")
          ? change(e, e.date(), "IHC 1+") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "endocrine-eligibility", events -> {
      events.replaceAll(e -> e.key().equals("primary-er")
          ? change(e, e.date(), "Negative") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "endocrine-eligibility", events -> {
      events.removeIf(e -> e.key().equals("menopause"));
      return events;
    });
  }

  @Test
  public void rejectsUnsafeTreatmentAndInventedCompletion() {
    Journey toxicity = journey(Scenario.TOXICITY);
    rejects(toxicity, "toxicity-hold", events -> {
      events.replaceAll(e -> e.key().equals("cycle-2-docetaxel")
          ? change(e, SNAPSHOT, e.value()) : e);
      return events;
    });
    rejects(toxicity, "cardiac-monitoring", events -> {
      events.removeIf(e -> e.kind().equals("lvef"));
      return events;
    });
    rejects(toxicity, "cardiac-monitoring", events -> {
      Event cycle = events.stream().filter(e -> e.key().equals("cycle-2-trastuzumab"))
          .findFirst().orElseThrow();
      events.add(new Event("new-echo", "lvef", cycle.date().minusDays(1), "final",
          "Left ventricular ejection fraction", null, "35", null));
      return events;
    });
    rejects(toxicity, "pretreatment-counts", events -> {
      events.replaceAll(e -> e.kind().equals("anc") ? change(e, e.date(), "0.5") : e);
      return events;
    });
    rejects(toxicity, "tchp-combination", events -> {
      events.removeIf(e -> e.kind().equals("medication") && e.value().equals("carboplatin"));
      return events;
    });
    rejects(toxicity, "course-status", events -> {
      events.replaceAll(e -> e.key().equals("therapy-order")
          ? new Event(e.key(), e.kind(), e.date(), "completed", e.description(),
              e.sample(), e.value(), e.cause()) : e);
      return events;
    });
  }

  @Test
  public void rejectsPrematureOrContradictoryFindings() {
    rejects(journey(Scenario.TOXICITY), "event-date", events -> {
      events.replaceAll(e -> e.key().equals("cycle-2-docetaxel")
          ? change(e, SNAPSHOT.plusDays(1), e.value()) : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "receptor-interpretation", events -> {
      events.replaceAll(e -> e.key().equals("primary-er-percent")
          ? change(e, e.date(), "150") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "metastatic-stage", events -> {
      events.replaceAll(e -> e.key().equals("stage-recurrence")
          ? change(e, e.date(), "cM0") : e);
      return events;
    });
    rejects(journey(Scenario.RECURRENCE), "specimen-before-result", events -> {
      events.removeIf(e -> e.key().equals("metastatic-sample"));
      return events;
    });
    rejects(journey(Scenario.TOXICITY), "histology-before-treatment", events -> {
      events.replaceAll(e -> e.key().equals("pathology")
          ? change(e, SNAPSHOT, e.value()) : e);
      return events;
    });
    rejects(journey(Scenario.DELAYED_MOLECULAR), "genomic-test-eligibility", events -> {
      events.replaceAll(e -> e.key().equals("primary-her2")
          ? change(e, e.date(), "IHC 3+") : e);
      return events;
    });
  }

  @Test
  public void rejectsACompletedCourseWithMissingCycles() {
    Journey complete = java.util.stream.IntStream.range(0, 100)
        .mapToObj(seed -> OncologyScenario.simulate(seed, 0, SNAPSHOT,
            Scenario.UNCOMPLICATED, Complexity.SIMPLE))
        .filter(j -> j.events().stream().anyMatch(e -> e.key().equals("cycle-6-docetaxel")))
        .findFirst().orElseThrow();
    rejects(complete, "tchp-course-length", events -> {
      events.removeIf(e -> e.key().equals("cycle-6-docetaxel"));
      return events;
    });
    rejects(complete, "tchp-interval", events -> {
      events.replaceAll(e -> e.key().equals("cycle-6-docetaxel")
          ? change(e, e.date().plusDays(2), e.value()) : e);
      return events;
    });
  }

  @Test
  public void displayComplexityPreservesTheClinicalCourse() {
    for (Scenario scenario : Scenario.values()) {
      List<String> simple = course(OncologyScenario.simulate(42, 0, SNAPSHOT,
          scenario, Complexity.SIMPLE));
      for (Complexity complexity : Complexity.values()) {
        assertEquals(simple, course(OncologyScenario.simulate(42, 0, SNAPSHOT,
            scenario, complexity)));
      }
    }
  }

  private List<String> course(Journey journey) {
    return journey.events().stream().filter(e -> List.of("medication", "medication-order",
        "procedure", "stage").contains(e.kind())).map(e -> e.kind() + ":" + e.date()
            + ":" + e.status() + ":" + e.value() + ":" + e.description()).toList();
  }
}
